---
change: standardize-sse-mcp-transport
design-doc: docs/superpowers/specs/2026-07-03-standardize-sse-mcp-transport-design.md
base-ref: f1a347ab0199fc836daf93b4d52f08091d1b2b86
archived-with: 2026-07-04-standardize-sse-mcp-transport
---

# Standardize SSE MCP Transport Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add standards-compliant MCP Streamable HTTP transport to the MagicAPI SSE plugin while preserving existing ordinary SSE behavior and legacy endpoint-event compatibility.

**Architecture:** Add a dedicated MCP transport controller, a session service, a JSON-RPC processor, and a MagicAPI tool adapter under the existing SSE plugin package. Keep standard HTTP transport concerns out of `BaseDynamicSseHandler`, and leave ordinary SSE broadcast/session behavior in the existing handler path.

**Tech Stack:** JDK 1.8, Spring Boot 2.7.18, Spring MVC, `SseEmitter`, Fastjson2, MagicAPI 2.2.2, JUnit/Spring test support.

## Global Constraints

- Canonical requirements live in `openspec/changes/standardize-sse-mcp-transport/specs/mcp-transport/spec.md`.
- Do not change `fw-plus-auth`.
- Do not restore `fw-plus-javaparser` or the MagicAPI Java plugin.
- Do not break `SseModule.connect`, `broadcastMessageToClient`, or `broadcastDistributedMessageToAllClient`.
- Standard MCP endpoint default path is `/mcp`; implementation must allow configuration override.
- Standard mode must use `Mcp-Session-Id` header for stateful sessions.
- Legacy endpoint-event compatibility may use query `sessionId`, but it must target a real POST route.
- Use TDD when implementing production behavior: write a failing test, verify it fails for the intended reason, implement minimal code, then verify it passes.
- Focused verification command for implementation tasks: `cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests=false -DfailIfNoTests=false test`.

archived-with: 2026-07-04-standardize-sse-mcp-transport
---

## File Structure

- Create `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpConstants.java`: header names, protocol versions, default endpoint names, JSON-RPC error codes.
- Create `.../sse/mcp/McpTransportProperties.java`: typed MCP configuration resolved from `MagicAPISseConfig`.
- Create `.../sse/mcp/McpSession.java`: session id, datasource key, protocol version, lifecycle state, optional emitter, timestamps.
- Create `.../sse/mcp/McpSessionService.java`: create, find, validate, mark initialized, attach stream, close, cleanup sessions.
- Create `.../sse/mcp/McpJsonRpcMessage.java`: JSON-RPC request/response DTO currently embedded in `BaseDynamicSseHandler`.
- Create `.../sse/mcp/McpJsonRpcProcessor.java`: lifecycle-aware JSON-RPC dispatcher for `initialize`, `notifications/initialized`, `tools/list`, and `tools/call`.
- Create `.../sse/mcp/MagicApiMcpToolAdapter.java`: MagicAPI API listing and script execution adapter.
- Create `.../sse/mcp/McpTransportController.java`: Spring MVC controller for standard and legacy MCP HTTP endpoints.
- Modify `.../sse/handler/BaseDynamicSseHandler.java`: remove or delegate MCP-specific JSON-RPC processing; keep ordinary SSE behavior and legacy endpoint event emission through the new controller/session service.
- Modify `.../sse/module/SseModule.java`: keep existing methods, delegate `message(...)` legacy requests to the new service/controller-level processor where practical.
- Modify `.../sse/starter/MagicAPISseConfiguration.java`: register MCP properties, session service, processor, adapter, and controller beans.
- Add tests under `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/`.

archived-with: 2026-07-04-standardize-sse-mcp-transport
---

### Task 1: Transport DTOs, Properties, and Session State

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpConstants.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpTransportProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpSession.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpSessionService.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpJsonRpcMessage.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpSessionServiceTest.java`

**Interfaces:**
- Produces: `McpSessionService#create(String dataSourceKey, String protocolVersion): McpSession`
- Produces: `McpSessionService#getRequired(String sessionId): McpSession`
- Produces: `McpSessionService#markInitialized(String sessionId): McpSession`
- Produces: `McpSessionService#markReady(String sessionId): McpSession`
- Produces: `McpSessionService#attachEmitter(String sessionId, SseEmitter emitter): McpSession`
- Produces: `McpSessionService#close(String sessionId): void`
- Produces: `McpSession.State` enum with `NEW`, `INITIALIZED`, `READY`, `CLOSED`

- [x] **Step 1: Write failing session lifecycle tests**

Create `McpSessionServiceTest` with tests:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.mcp;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.junit.jupiter.api.Assertions.*;

class McpSessionServiceTest {

    @Test
    void createInitializesSessionInNewState() {
        McpSessionService service = new McpSessionService();

        McpSession session = service.create("demo", McpConstants.PROTOCOL_VERSION_2025_06_18);

        assertNotNull(session.getSessionId());
        assertEquals("demo", session.getDataSourceKey());
        assertEquals(McpConstants.PROTOCOL_VERSION_2025_06_18, session.getProtocolVersion());
        assertEquals(McpSession.State.NEW, session.getState());
        assertSame(session, service.getRequired(session.getSessionId()));
    }

    @Test
    void lifecycleMovesFromNewToInitializedToReady() {
        McpSessionService service = new McpSessionService();
        McpSession session = service.create("demo", McpConstants.PROTOCOL_VERSION_2025_06_18);

        service.markInitialized(session.getSessionId());
        assertEquals(McpSession.State.INITIALIZED, service.getRequired(session.getSessionId()).getState());

        service.markReady(session.getSessionId());
        assertEquals(McpSession.State.READY, service.getRequired(session.getSessionId()).getState());
    }

    @Test
    void unknownSessionIsRejected() {
        McpSessionService service = new McpSessionService();

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> service.getRequired("missing"));

        assertTrue(ex.getMessage().contains("Unknown MCP session"));
    }

    @Test
    void attachEmitterStoresStreamOnSession() {
        McpSessionService service = new McpSessionService();
        McpSession session = service.create("demo", McpConstants.PROTOCOL_VERSION_2025_06_18);
        SseEmitter emitter = new SseEmitter(1000L);

        service.attachEmitter(session.getSessionId(), emitter);

        assertSame(emitter, service.getRequired(session.getSessionId()).getEmitter());
    }
}
```

- [x] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpSessionServiceTest test
```

Expected: compilation fails because `McpSessionService`, `McpSession`, and `McpConstants` do not exist.

- [x] **Step 3: Implement minimal session classes**

Implement `McpConstants`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.mcp;

public final class McpConstants {
    public static final String HEADER_SESSION_ID = "Mcp-Session-Id";
    public static final String HEADER_PROTOCOL_VERSION = "MCP-Protocol-Version";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_ORIGIN = "Origin";
    public static final String PROTOCOL_VERSION_2025_06_18 = "2025-06-18";
    public static final String PROTOCOL_VERSION_2024_11_05 = "2024-11-05";
    public static final String DEFAULT_ENDPOINT = "/mcp";
    public static final String DEFAULT_LEGACY_MESSAGE_ENDPOINT = "/mcp/message";

    private McpConstants() {
    }
}
```

Implement `McpSession` with JavaBean getters/setters and `State` enum.

Implement `McpSessionService` with a `ConcurrentHashMap<String, McpSession>`, UUID session ids, state transition methods, emitter attachment, and close removal.

Implement `McpJsonRpcMessage` as a public DTO equivalent to the current nested JSON-RPC message: `jsonrpc`, `id`, `method`, `JSONObject params`, `Object result`, `JSONObject error`.

Implement `McpTransportProperties` with fields: `endpoint`, `legacyMessageEndpoint`, `streamEnabled`, `strictOrigin`, `trustedOrigins`, `supportedProtocolVersions`; default endpoint `/mcp`, legacy endpoint `/mcp/message`, stream enabled `true`, strict origin `true`, supported versions containing `2025-06-18` and `2024-11-05`.

- [x] **Step 4: Run tests and verify GREEN**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpSessionServiceTest test
```

Expected: `McpSessionServiceTest` passes.

archived-with: 2026-07-04-standardize-sse-mcp-transport
---

### Task 2: JSON-RPC Processor and MagicAPI Tool Adapter

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpJsonRpcProcessor.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpException.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/MagicApiMcpToolAdapter.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpJsonRpcProcessorTest.java`

**Interfaces:**
- Consumes: `McpSessionService`
- Produces: `McpJsonRpcProcessor#process(McpSession session, McpJsonRpcMessage request): McpJsonRpcMessage`
- Produces: `MagicApiMcpToolAdapter#listTools(String groupId): List<Map<String, Object>>`
- Produces: `MagicApiMcpToolAdapter#callTool(String name, JSONObject arguments): Map<String, Object>`

- [x] **Step 1: Write failing lifecycle and method dispatch tests**

Create `McpJsonRpcProcessorTest` with a fake adapter:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.mcp;

import com.alibaba.fastjson2.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class McpJsonRpcProcessorTest {

    @Test
    void initializeReturnsRequiredFieldsAndMarksSessionInitialized() {
        McpSessionService sessions = new McpSessionService();
        McpSession session = sessions.create("demo", McpConstants.PROTOCOL_VERSION_2025_06_18);
        McpJsonRpcProcessor processor = new McpJsonRpcProcessor(sessions, new FakeToolAdapter());
        McpJsonRpcMessage request = request("1", "initialize");

        McpJsonRpcMessage response = processor.process(session, request);

        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals(McpConstants.PROTOCOL_VERSION_2025_06_18, result.get("protocolVersion"));
        assertNotNull(result.get("capabilities"));
        assertNotNull(result.get("serverInfo"));
        assertEquals(McpSession.State.INITIALIZED, sessions.getRequired(session.getSessionId()).getState());
    }

    @Test
    void toolsListBeforeReadyIsRejected() {
        McpSessionService sessions = new McpSessionService();
        McpSession session = sessions.create("demo", McpConstants.PROTOCOL_VERSION_2025_06_18);
        McpJsonRpcProcessor processor = new McpJsonRpcProcessor(sessions, new FakeToolAdapter());

        McpException ex = assertThrows(McpException.class,
                () -> processor.process(session, request("2", "tools/list")));

        assertEquals(-32002, ex.getErrorCode());
    }

    @Test
    void initializedNotificationMarksSessionReadyAndReturnsNull() {
        McpSessionService sessions = new McpSessionService();
        McpSession session = sessions.create("demo", McpConstants.PROTOCOL_VERSION_2025_06_18);
        McpJsonRpcProcessor processor = new McpJsonRpcProcessor(sessions, new FakeToolAdapter());
        processor.process(session, request("1", "initialize"));

        McpJsonRpcMessage notification = request(null, "notifications/initialized");
        McpJsonRpcMessage response = processor.process(session, notification);

        assertNull(response);
        assertEquals(McpSession.State.READY, sessions.getRequired(session.getSessionId()).getState());
    }

    @Test
    void toolsCallDelegatesToAdapterWhenReady() {
        McpSessionService sessions = new McpSessionService();
        McpSession session = sessions.create("demo", McpConstants.PROTOCOL_VERSION_2025_06_18);
        McpJsonRpcProcessor processor = new McpJsonRpcProcessor(sessions, new FakeToolAdapter());
        processor.process(session, request("1", "initialize"));
        processor.process(session, request(null, "notifications/initialized"));

        McpJsonRpcMessage call = request("3", "tools/call");
        JSONObject params = new JSONObject();
        params.put("name", "known");
        params.put("arguments", new JSONObject());
        call.setParams(params);

        McpJsonRpcMessage response = processor.process(session, call);

        Map<?, ?> result = (Map<?, ?>) response.getResult();
        assertEquals(Boolean.FALSE, result.get("isError"));
        assertNotNull(result.get("content"));
    }

    private McpJsonRpcMessage request(String id, String method) {
        McpJsonRpcMessage message = new McpJsonRpcMessage();
        message.setId(id);
        message.setMethod(method);
        return message;
    }

    static class FakeToolAdapter extends MagicApiMcpToolAdapter {
        FakeToolAdapter() {
            super(null);
        }

        @Override
        public java.util.List<Map<String, Object>> listTools(String groupId) {
            return Collections.emptyList();
        }

        @Override
        public Map<String, Object> callTool(String name, JSONObject arguments) {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("content", Collections.singletonList(Collections.singletonMap("type", "text")));
            result.put("isError", false);
            return result;
        }
    }
}
```

- [x] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpJsonRpcProcessorTest test
```

Expected: compilation fails because processor/adapter classes do not exist.

- [x] **Step 3: Implement processor and adapter**

Implement `McpException` with `errorCode`.

Implement `McpJsonRpcProcessor`:
- `initialize`: returns protocol version, `{ tools: { listChanged: true } }`, serverInfo, marks session initialized.
- `notifications/initialized`: requires `INITIALIZED`, marks ready, returns `null`.
- `tools/list`: requires `READY`, returns `{ tools: adapter.listTools(groupId) }`.
- `tools/call`: requires `READY`, validates `params.name`, delegates to adapter.
- unknown methods throw `McpException(-32601)`.

Implement `MagicApiMcpToolAdapter` by moving the current tool schema and tool call logic out of `BaseDynamicSseHandler` where possible. If constructor receives a null dependency in tests, methods may be overridden as shown; production path must use MagicAPI resource APIs.

- [x] **Step 4: Run tests and verify GREEN**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpJsonRpcProcessorTest test
```

Expected: test passes.

archived-with: 2026-07-04-standardize-sse-mcp-transport
---

### Task 3: Standard MCP HTTP Controller

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpTransportController.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/starter/MagicAPISseConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpTransportControllerTest.java`

**Interfaces:**
- Consumes: `McpSessionService`, `McpJsonRpcProcessor`, `McpTransportProperties`
- Produces: `POST /mcp` JSON-RPC endpoint
- Produces: `GET /mcp` SSE endpoint
- Produces: legacy `POST /mcp/message`

- [x] **Step 1: Write failing MockMvc tests**

Create controller tests with standalone MockMvc and fake processor:
- `POST /mcp` with valid Accept and initialize returns 200 JSON and `Mcp-Session-Id`.
- `POST /mcp` without acceptable Accept returns 406 or 400.
- `POST /mcp` with unknown session returns 400.
- `GET /mcp` with `Accept: text/event-stream` returns an SSE-compatible response.

- [x] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpTransportControllerTest test
```

Expected: compilation fails because controller does not exist.

- [x] **Step 3: Implement controller and bean registration**

Implement controller methods:
- `@PostMapping("${...}")` is not directly viable for dynamic path if using constructor properties; prefer registering under a stable default route first or use `@RequestMapping` with property placeholder.
- Validate `Origin` before JSON-RPC processing.
- Validate POST `Accept` contains `application/json` or `text/event-stream`.
- On initialize without session header, create session and return `Mcp-Session-Id`.
- On non-initialize with session header, lookup required session.
- Return JSON response for request messages; return 202/no body for notifications.
- Implement legacy `POST /mcp/message?sessionId=...` route that resolves legacy session and delegates to the same processor.

Register beans in `MagicAPISseConfiguration`.

- [x] **Step 4: Run tests and verify GREEN**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpTransportControllerTest test
```

Expected: controller tests pass.

archived-with: 2026-07-04-standardize-sse-mcp-transport
---

### Task 4: Legacy SSE Endpoint Event Integration

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/handler/BaseDynamicSseHandler.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/module/SseModule.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/LegacyMcpSseCompatibilityTest.java`

**Interfaces:**
- Consumes: `McpSessionService`
- Produces: legacy endpoint event data pointing to real `legacyMessageEndpoint`
- Preserves: `SseModule#connect`, `SseModule#message`

- [x] **Step 1: Write failing legacy compatibility tests**

Create tests that instantiate or subclass the SSE handler enough to verify:
- MCP-enabled datasource connection sends endpoint event with configured legacy endpoint path.
- `SseModule.message(sessionId, json)` delegates to the new processor path or remains compatible through `BaseDynamicSseHandler#message`.
- Ordinary broadcast method signatures remain unchanged.

- [x] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=LegacyMcpSseCompatibilityTest test
```

Expected: tests fail because endpoint integration still uses old hardcoded behavior or test seams are missing.

- [x] **Step 3: Integrate legacy flow**

Update `sendMcpEndpointEvent` so it uses `McpTransportProperties#getLegacyMessageEndpoint()` and creates/registers a compatible session in `McpSessionService`.

Keep `SseModule.message(String sessionId, String message)` for legacy callers. It may parse into `McpJsonRpcMessage` and delegate to a compatibility method that sends response through the legacy emitter.

Do not change ordinary broadcast methods.

- [x] **Step 4: Run tests and verify GREEN**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=LegacyMcpSseCompatibilityTest test
```

Expected: legacy compatibility tests pass.

archived-with: 2026-07-04-standardize-sse-mcp-transport
---

### Task 5: Security, Documentation, and Full Verification

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpTransportProperties.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpTransportController.java`
- Modify or Create: plugin documentation or sample notes near `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-sse/`
- Test: existing MCP tests plus any added origin/config tests

**Interfaces:**
- Consumes: controller/session/processor from earlier tasks
- Produces: documented configuration keys and tested security behavior

- [x] **Step 1: Write failing security/config tests**

Add tests proving:
- untrusted Origin is rejected before processor invocation.
- trusted Origin is accepted.
- unsupported `MCP-Protocol-Version` is rejected after initialization when strict protocol validation is enabled.

- [x] **Step 2: Run tests and verify RED**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mcp*Test,LegacyMcpSseCompatibilityTest' test
```

Expected: new security/config tests fail before implementation.

- [x] **Step 3: Implement security/config behavior and docs**

Implement exact config keys under `magic-api.plugin.sse.mcp`:
- `endpoint`
- `legacy-message-endpoint`
- `stream-enabled`
- `strict-origin`
- `trusted-origins`
- `strict-protocol-version`

Document default endpoint, required headers, session behavior, trusted origins, and legacy compatibility route.

- [x] **Step 4: Run focused verification**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mcp*Test,LegacyMcpSseCompatibilityTest' test
```

Expected: all MCP and legacy compatibility tests pass.

- [x] **Step 5: Run module verification**

Run:

```bash
cd fw-plus-boot && mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DskipTests=false -DfailIfNoTests=false test
```

Expected: module tests pass.

- [x] **Step 6: Update OpenSpec task checklist**

Review note: `review_mode` was `thorough`, but subagent dispatch was not used because the current tool policy only permits spawning subagents when explicitly requested by the user. A local review found and fixed the configurable mapping / `dataSourceKey` gaps before final verification.

Verify-fail fix note: local review found that `tools/call` could bypass the session datasource scope by calling a known API id outside the listed tool group. Added a failing test first, then changed tool invocation to validate the requested tool against the session `dataSourceKey` before executing MagicAPI scripts.

After all implementation verification passes, check off matching items in `openspec/changes/standardize-sse-mcp-transport/tasks.md`.
