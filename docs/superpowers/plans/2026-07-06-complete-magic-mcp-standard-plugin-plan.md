---
change: complete-magic-mcp-standard-plugin
design-doc: docs/superpowers/specs/2026-07-06-complete-magic-mcp-standard-plugin-design.md
base-ref: 341b7e34334ac65db182499f658e5ff2b83e3d9e
---

# Complete Magic MCP Standard Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Bring Magic MCP plugin to a practical first-complete MCP 2025-11-25 compatible implementation.

**Architecture:** Keep existing MCP packages and add focused protocol/session/capability extensions. Backend protocol behavior is implemented first with tests, then console diagnostics are layered through protected backend endpoints and existing console plugin services.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Magic-API 2.2.2, JUnit, Vue3/Vite console plugin.

## Global Constraints

- Java source must remain Java 8 compatible; do not use Java 9+ APIs.
- Do not introduce `mcp-datasource` or any new datasource storage model.
- Preserve existing endpoint default `/mcp` and configurable header names.
- Use Maven command: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mcp*Test,MagicMcpControllerTest,MagicMcpControllerAuthTest' test -q`.
- Keep Magic API tool exposure disabled by default for APIs.

---

### Task 1: Protocol Negotiation And POST Session Semantics

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/core/McpLifecycleHandlers.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/McpHttpTransportService.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/core/McpLifecycleHandlersTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/MagicMcpControllerTest.java`

**Interfaces:**
- Produces: `MagicAPIMcpProperties.Protocol#getSupportedVersions()`, `getDefaultVersion()`, and lifecycle negotiation using those values.
- Produces: transport behavior that rejects invalid non-initialize session headers before dispatcher execution.

- [x] **Step 1: Write failing protocol negotiation tests**

Add tests asserting default version `2025-11-25`, supported version accepted, unsupported version returns `-32602`, and no usable session is created for unsupported version.

- [x] **Step 2: Write failing POST session tests**

Add controller tests asserting non-initialize POST with unknown/deleted session returns client error before handler side effects.

- [x] **Step 3: Implement protocol properties and lifecycle validation**

Add nested `Protocol` config with supported versions default `2025-11-25, 2025-06-18` and default version `2025-11-25`; validate initialize params before `createSession`.

- [x] **Step 4: Implement POST session validation and protocol response header**

Before dispatch, if method is not initialize and session header is present or required, call `sessionService.requireSession`; return client errors for invalid states. Add protocol version response header from context/session.

- [x] **Step 5: Run task tests**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='McpLifecycleHandlersTest,MagicMcpControllerTest' test -q`

### Task 2: Accurate Capabilities And SSE Message Events

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/core/McpLifecycleHandlers.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/McpSseStreamService.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/core/McpLifecycleHandlersTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/MagicMcpControllerTest.java`

**Interfaces:**
- Produces: capabilities map with `tools.listChanged`, `resources.listChanged`, `resources.subscribe`, `prompts.listChanged`, `logging`, and `completion` when available.
- Produces: SSE sends `SseEmitter.event().name("message").data(message)`.

- [x] **Step 1: Write failing capability tests**

Assert initialize result contains precise capabilities for tools/resources/prompts/logging/completion based on constructor inputs.

- [x] **Step 2: Write failing SSE event test**

Use existing emitter test seam to assert send receives an event object named `message` or equivalent Spring SSE builder payload.

- [x] **Step 3: Implement capability builder**

Inject availability flags/counts into lifecycle handler from configuration and providers; omit unavailable optional capabilities.

- [x] **Step 4: Implement SSE message event sending**

Change `McpSseStreamService.send` to use named message event while keeping existing removal-on-error behavior.

- [x] **Step 5: Run task tests**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='McpLifecycleHandlersTest,MagicMcpControllerTest,McpNotificationServiceTest' test -q`

### Task 3: Server-to-client Request Lifecycle

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/session/McpSession.java`
- Create/Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/client/`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/protocol/McpJsonRpcDispatcher.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/client/McpClientRequestServiceTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/protocol/McpJsonRpcDispatcherTest.java`

**Interfaces:**
- Produces: pending request record with id, sessionId, method, capability, createdAt, timeoutAt, status, result, error.
- Consumes: dispatcher response messages with matching id.

- [x] **Step 1: Write failing lifecycle tests**

Assert roots/sampling/elicitation creates pending request, response resolves it, error marks failed, timeout cleanup marks timed out, cancelled marks cancelled.

- [x] **Step 2: Write failing dispatcher response routing test**

Assert a message with `jsonrpc`, `id`, and `result` but no `method` is routed to client request service and does not return method-not-found.

- [x] **Step 3: Implement pending request model**

Add Java 8 compatible POJO and session storage map keyed by request id.

- [x] **Step 4: Implement resolve/error/timeout/cancel operations**

Extend `McpClientRequestService` with methods to resolve, fail, timeout cleanup and cancel pending requests.

- [x] **Step 5: Route JSON-RPC responses in dispatcher**

Add optional client request service dependency; detect response/error messages before method dispatch.

- [x] **Step 6: Run task tests**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='McpClientRequestServiceTest,McpJsonRpcDispatcherTest' test -q`

### Task 4: Tool Schema Enhancement

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/tools/MagicApiMcpToolProvider.java`
- Create/Modify helpers under: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/tools/`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/tools/MagicApiMcpToolProviderTest.java`

**Interfaces:**
- Produces: valid JSON schema map with type object, properties, required when metadata exists; fallback schema remains valid.

- [x] **Step 1: Write failing schema tests**

Use Magic API/function test doubles with option metadata and assert schema includes known properties and required names.

- [x] **Step 2: Implement schema extraction helper**

Parse conservative metadata from Magic resource options/body; ignore malformed metadata and return fallback object schema.

- [x] **Step 3: Use helper in listTools**

Replace unconditional `objectSchema()` calls with extracted schema for input and safe fallback for output.

- [x] **Step 4: Run task tests**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='MagicApiMcpToolProviderTest,McpToolsHandlerTest' test -q`

### Task 5: Console MCP Diagnostics

**Files:**
- Create/Modify backend diagnostic service/controller under: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java`
- Modify console files under: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/src/console/src/`
- Test backend: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/`

**Interfaces:**
- Produces: protected diagnostic endpoint for tools list/call, resources read, prompts get using existing handlers and auth/session semantics.
- Produces: console service/UI entries to preview resource/prompt/tool results with JSON inputs.

- [ ] **Step 1: Write failing backend diagnostic tests**

Assert diagnostics call existing handlers, enforce auth scopes, return JSON-RPC-like success/error body.

- [ ] **Step 2: Implement diagnostic backend**

Add controller/service using existing handler methods and a diagnostic request context; do not bypass provider filtering.

- [ ] **Step 3: Add console services and UI controls**

Add actions for resource read preview, prompt get preview, and tools list/call preview with JSON editor inputs.

- [ ] **Step 4: Run backend tests and console build if available**

Run backend diagnostic tests. If `package.json` scripts are available, run console build command for MCP plugin.

### Task 6: Documentation And Final Verification

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/README.md`
- Modify: `openspec/changes/complete-magic-mcp-standard-plugin/tasks.md`

**Interfaces:**
- Produces: README sections for protocol version, capabilities, diagnostics, security, and examples.

- [ ] **Step 1: Update README**

Document configuration keys, supported protocol versions, diagnostics, client-request lifecycle and security defaults.

- [ ] **Step 2: Run full MCP verification**

Run: `mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest='*Mcp*Test,MagicMcpControllerTest,MagicMcpControllerAuthTest' test -q`

- [ ] **Step 3: Mark OpenSpec tasks complete**

Check off `openspec/changes/complete-magic-mcp-standard-plugin/tasks.md` only after corresponding implementation and verification pass.
