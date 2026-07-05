# Standard MCP Server Plugin Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 新增独立 `fw-plus-magicapi-plugin-mcp`，提供标准 MCP Server 能力、Sa-Token OAuth2 保护、tools/resources/prompts/provider 扩展，并且不依赖 SSE 插件或 datasource 模型。

**Architecture:** 新插件由 HTTP transport、MCP session、JSON-RPC dispatcher、feature handlers、Provider SPI、Sa-Token OAuth2 auth service 组成。Controller 只处理 HTTP/MCP transport，协议层处理生命周期和方法分发，Provider 负责业务内容。

**Tech Stack:** Java 1.8、Spring Boot 2.7.18、MagicAPI 2.2.2、Sa-Token OAuth2、Jackson/Fastjson2、JUnit/Spring Boot Test、Maven Wrapper。

## Global Constraints

- 始终使用简体中文沟通。
- 当前 change：`add-standard-mcp-server-plugin`。
- 设计文档：`docs/superpowers/specs/2026-07-04-standard-mcp-server-plugin-design.md`。
- base-ref：`f1a347ab0199fc836daf93b4d52f08091d1b2b86`。
- JDK 保持 1.8；不得使用 Java 9+ API。
- 不恢复 `fw-plus-magicapi-plugin-sse`。
- 不修改或删除 `ssehttp`。
- 不引入 `mcp-datasource`、`resources-datasource`、`prompts-datasource` 或任何 `*-datasource` MCP 内容模型。
- OAuth 使用 Sa-Token OAuth2；MCP 插件不实现 Authorization Server。
- 不新增 Spring Security 作为必需依赖。
- 新生产逻辑优先先写失败测试，再实现。
- 每个任务完成后运行对应测试，并只勾选已验证通过的任务。

---

### Task 1: Maven Module And Auto-Configuration Wiring

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-dependencies/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/MagicApiPluginAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfigurationTest.java`

**Interfaces:**
- Produces: `MagicAPIMcpProperties` with prefix `magic-api.plugin.mcp`.
- Produces: `MagicAPIMcpConfiguration implements MagicPluginConfiguration`.
- Produces: plugin id `mcp`, display name `MagicMcp`, no required console bundle in first implementation.

- [x] **Step 1: Write auto-configuration test**

Create `MagicAPIMcpConfigurationTest.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;

import static org.assertj.core.api.Assertions.assertThat;

class MagicAPIMcpConfigurationTest {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withUserConfiguration(MagicAPIMcpConfiguration.class);

	@Test
	void shouldCreateMcpPluginConfigurationByDefault() {
		contextRunner.run(context -> {
			assertThat(context).hasSingleBean(MagicAPIMcpConfiguration.class);
			MagicPluginConfiguration configuration = context.getBean(MagicAPIMcpConfiguration.class);
			assertThat(configuration.plugin().getName()).isEqualTo("mcp");
		});
	}

	@Test
	void shouldDisableMcpPluginWhenPropertyIsFalse() {
		contextRunner.withPropertyValues("magic-api.plugin.mcp.enabled=false")
				.run(context -> assertThat(context).doesNotHaveBean(MagicAPIMcpConfiguration.class));
	}

	@Test
	void shouldUseDefaultEndpoint() {
		contextRunner.run(context -> {
			MagicAPIMcpProperties properties = context.getBean(MagicAPIMcpProperties.class);
			assertThat(properties.getEndpoint()).isEqualTo("/mcp");
		});
	}
}
```

- [x] **Step 2: Run test and confirm failure**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIMcpConfigurationTest -DfailIfNoTests=false test
```

Expected: FAIL because MCP classes do not exist.

- [x] **Step 3: Create starter POM**

Create `fw-plus-magicapi-plugin-mcp/pom.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
		 xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
		 xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
	<modelVersion>4.0.0</modelVersion>
	<parent>
		<groupId>com.xiongx9527.fw.plus.boot</groupId>
		<artifactId>fw-plus-magicapi-plugin</artifactId>
		<version>${revision}</version>
	</parent>

	<artifactId>fw-plus-magicapi-plugin-mcp</artifactId>

	<properties>
		<main.basedir>${basedir}/../../..</main.basedir>
	</properties>

	<dependencies>
		<dependency>
			<groupId>cn.dev33</groupId>
			<artifactId>sa-token-oauth2</artifactId>
		</dependency>
	</dependencies>
</project>
```

- [x] **Step 4: Wire Maven aggregation and BOM**

Modify plugin aggregator POM to add:

```xml
<module>fw-plus-magicapi-plugin-mcp</module>
```

Modify Boot dependency management near other MagicAPI plugins:

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.boot</groupId>
	<artifactId>fw-plus-magicapi-plugin-mcp</artifactId>
	<version>${project.version}</version>
</dependency>
```

- [x] **Step 5: Implement properties and configuration**

Create `MagicAPIMcpProperties.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "magic-api.plugin.mcp")
public class MagicAPIMcpProperties {

	private boolean enabled = true;

	private String endpoint = "/mcp";

	private long sessionTimeoutSeconds = 1800;

	private boolean sseStreamEnabled = true;

	private int maxRequestBytes = 1024 * 1024;

	private List<String> trustedOrigins = new ArrayList<>();

	private Auth auth = new Auth();

	@Data
	public static class Auth {
		private boolean enabled = true;
		private String resource = "fw-plus-mcp";
		private String authorizationServer;
		private String toolsScope = "mcp:tools";
		private String resourcesScope = "mcp:resources";
		private String promptsScope = "mcp:prompts";
		private String loggingScope = "mcp:logging";
		private String rootsScope = "mcp:roots";
		private String samplingScope = "mcp:sampling";
		private String elicitationScope = "mcp:elicitation";
	}
}
```

Create minimal `MagicAPIMcpConfiguration.java`:

```java
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.starter;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;

@Configuration
@EnableConfigurationProperties(MagicAPIMcpProperties.class)
@ConditionalOnProperty(prefix = "magic-api.plugin.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
public class MagicAPIMcpConfiguration implements MagicPluginConfiguration {

	@Override
	public Plugin plugin() {
		return new Plugin("mcp", "MagicMcp");
	}
}
```

Add import and `@Import` entry in `MagicApiPluginAutoConfiguration`.

- [x] **Step 6: Run focused test**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicAPIMcpConfigurationTest -DfailIfNoTests=false test
```

Expected: PASS.

---

### Task 2: Protocol Model And JSON-RPC Dispatcher

**Files:**
- Create package: `.../magicapi/plugin/mcp/protocol`
- Create: `McpJsonRpcMessage.java`
- Create: `McpJsonRpcError.java`
- Create: `McpJsonRpcResponse.java`
- Create: `McpMethodHandler.java`
- Create: `McpJsonRpcDispatcher.java`
- Test: `.../plugin/mcp/protocol/McpJsonRpcDispatcherTest.java`

**Interfaces:**
- Produces: `Object dispatch(McpRequestContext context, Object payload)`.
- Produces: standard error codes `-32700`, `-32600`, `-32601`, `-32602`, `-32603`.

- [x] **Step 1: Write dispatcher tests**

Create tests covering unknown method, invalid params from handler, notification without body, and ping handler registration.

- [x] **Step 2: Run test and confirm failure**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpJsonRpcDispatcherTest -DfailIfNoTests=false test
```

Expected: FAIL because protocol classes do not exist.

- [x] **Step 3: Implement minimal JSON-RPC model**

Use Java 8-compatible POJOs with Jackson/Fastjson-compatible getters/setters. Include `jsonrpc`, `id`, `method`, `params`, `result`, `error`.

- [x] **Step 4: Implement dispatcher**

Implement handler map lookup:

```java
public interface McpMethodHandler {
	Object handle(McpRequestContext context, Object params);
}
```

Unknown method returns `-32601`; invalid handler params map to `-32602`; uncaught exceptions map to `-32603`.

- [x] **Step 5: Run dispatcher tests**

Run the same Maven command. Expected: PASS.

---

### Task 3: Session Service And Base Lifecycle

**Files:**
- Create package: `.../magicapi/plugin/mcp/session`
- Create: `McpSession.java`
- Create: `McpSessionService.java`
- Create package: `.../magicapi/plugin/mcp/core`
- Create: `McpRequestContext.java`
- Create: `McpLifecycleHandlers.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: `.../plugin/mcp/session/McpSessionServiceTest.java`
- Test: `.../plugin/mcp/core/McpLifecycleHandlersTest.java`

**Interfaces:**
- Produces: `McpSession createSession(McpPrincipal principal, Map<String, Object> clientCapabilities, String protocolVersion)`.
- Produces: `McpSession requireSession(String sessionId)`.
- Produces handlers for `initialize`, `notifications/initialized`, `ping`, `notifications/cancelled`.

- [x] **Step 1: Write session and lifecycle tests**

Tests assert session creation, unknown session rejection, expiration cleanup, initialized lifecycle transition, ping empty result, and tool request before initialized rejection.

- [x] **Step 2: Run tests and confirm failure**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=McpSessionServiceTest,McpLifecycleHandlersTest -DfailIfNoTests=false test
```

Expected: FAIL.

- [x] **Step 3: Implement session POJO and service**

Use `ConcurrentHashMap<String, McpSession>`, UUID session ids, `lastActivityTime`, and explicit lifecycle enum values `NEW`, `INITIALIZED`, `READY`, `DELETED`.

- [x] **Step 4: Implement base handlers**

`initialize` creates or updates session and returns `protocolVersion`, `serverInfo`, `capabilities`. `notifications/initialized` marks session `READY`. `ping` returns empty map.

- [x] **Step 5: Register beans**

Register `McpSessionService`, `McpJsonRpcDispatcher`, and lifecycle handlers in `MagicAPIMcpConfiguration`.

- [x] **Step 6: Run tests**

Expected: PASS.

---

### Task 4: HTTP Transport Controller

**Files:**
- Create package: `.../magicapi/plugin/mcp/web`
- Create: `MagicMcpController.java`
- Create: `McpHttpTransportService.java`
- Create: `McpSseStreamService.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: `.../plugin/mcp/web/MagicMcpControllerTest.java`

**Interfaces:**
- Produces POST `/mcp` JSON-RPC endpoint.
- Produces GET `/mcp` SSE stream when enabled.
- Produces DELETE `/mcp` session termination.
- Produces OAuth protected resource metadata endpoint.

- [x] **Step 1: Write controller tests**

Use Spring MVC test or `ApplicationContextRunner` with mocked dispatcher/session services. Test default path `/mcp`, configured path, `Accept` rejection, POST success, notification `202`, GET SSE, DELETE session, and metadata response.

- [x] **Step 2: Run tests and confirm failure**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=MagicMcpControllerTest -DfailIfNoTests=false test
```

Expected: FAIL.

- [x] **Step 3: Implement transport service**

Validate `Accept`, `MCP-Protocol-Version`, `Mcp-Session-Id`, body size, and method support before dispatching JSON-RPC.

- [x] **Step 4: Implement controller registration**

Use `MagicPluginConfiguration#controllerRegister()` to register `MagicMcpController`, following existing plugin controller patterns.

- [x] **Step 5: Run controller tests**

Expected: PASS.

---

### Task 5: Sa-Token OAuth2 Auth Service

**Files:**
- Create package: `.../magicapi/plugin/mcp/auth`
- Create: `McpPrincipal.java`
- Create: `McpAuthService.java`
- Create: `SaTokenMcpAuthService.java`
- Create: `McpAuthorizationException.java`
- Modify: `McpHttpTransportService.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: `.../plugin/mcp/auth/SaTokenMcpAuthServiceTest.java`
- Test: `.../plugin/mcp/web/MagicMcpControllerAuthTest.java`

**Interfaces:**
- Produces: `McpPrincipal authenticate(HttpServletRequest request)`.
- Produces: `void requireScope(McpPrincipal principal, String scope)`.
- Produces `401` and `403` transport responses.

- [x] **Step 1: Write auth tests**

Tests cover missing Bearer token, invalid token, insufficient scope, disabled auth, trusted Origin rejection, and metadata `WWW-Authenticate`.

- [x] **Step 2: Run tests and confirm failure**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -Dtest=SaTokenMcpAuthServiceTest,MagicMcpControllerAuthTest -DfailIfNoTests=false test
```

Expected: FAIL.

- [x] **Step 3: Implement auth abstraction and Sa-Token adapter**

Use Sa-Token APIs behind `@ConditionalOnClass` and MCP properties. Keep Authorization Server behavior outside this plugin.

- [x] **Step 4: Wire auth into transport**

Authenticate before JSON-RPC dispatch, then pass `McpPrincipal` into `McpRequestContext`.

- [x] **Step 5: Run auth tests**

Expected: PASS.

---

### Task 6: Provider SPI And Tools

**Files:**
- Create package: `.../magicapi/plugin/mcp/provider`
- Create: `McpProviderContext.java`
- Create: `McpPage.java`
- Create: `McpPageRequest.java`
- Create: `McpToolProvider.java`
- Create: `McpToolDefinition.java`
- Create: `McpToolCallRequest.java`
- Create: `McpToolResult.java`
- Create package: `.../magicapi/plugin/mcp/tools`
- Create: `McpToolsHandler.java`
- Create: `MagicApiMcpToolProvider.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: `.../plugin/mcp/tools/McpToolsHandlerTest.java`

**Interfaces:**
- Produces `tools/list`, `tools/call`, `notifications/tools/list_changed`.
- Produces opaque cursor pagination.

- [x] **Step 1: Write tools tests**

Tests cover list pagination, known call, unknown call, structured content, output schema, lifecycle gating, scope gating, and list change notification gating.

- [x] **Step 2: Run tests and confirm failure**

Run focused tools tests. Expected: FAIL.

- [x] **Step 3: Implement Provider SPI basics**

Implement page request/page response and tool provider interfaces.

- [x] **Step 4: Implement tools handler**

Aggregate all `McpToolProvider` beans. Enforce scope `mcp:tools`. Return standard MCP tool definitions and tool call results.

- [x] **Step 5: Implement MagicAPI-backed provider**

Use existing MagicAPI services only where available from current application context. If MagicAPI execution integration is too broad for this task, provide provider interface and a minimal no-op/default provider, then add MagicAPI execution in a follow-up task before marking OpenSpec task 4 complete.

- [x] **Step 6: Run tools tests**

Expected: PASS.

---

### Task 7: Resources, Templates, And Subscriptions

**Files:**
- Create: `McpResourceProvider.java`
- Create: `McpResourceDefinition.java`
- Create: `McpResourceReadRequest.java`
- Create: `McpResourceReadResult.java`
- Create: `McpResourceTemplate.java`
- Create package: `.../magicapi/plugin/mcp/resources`
- Create: `McpResourcesHandler.java`
- Create: `McpResourceSubscriptionService.java`
- Create: `StaticMcpResourceProvider.java`
- Modify: `MagicAPIMcpProperties.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: `.../plugin/mcp/resources/McpResourcesHandlerTest.java`

**Interfaces:**
- Produces `resources/list`, `resources/read`, `resources/templates/list`, `resources/subscribe`, `resources/unsubscribe`, `notifications/resources/updated`, `notifications/resources/list_changed`.

- [x] **Step 1: Write resources tests**

Tests cover listing, reading text/blob, template listing, URI rejection, pagination, subscribe/unsubscribe, updated notification, list changed notification, and scope checks.

- [x] **Step 2: Run tests and confirm failure**

Expected: FAIL.

- [x] **Step 3: Implement resource provider SPI**

Resources are not datasource. Use provider beans and optional static configuration under `magic-api.plugin.mcp.resources`.

- [x] **Step 4: Implement resources handler**

Aggregate provider results, validate URI scheme/path, apply output limits, and gate by `mcp:resources`.

- [x] **Step 5: Implement subscription service**

Track subscriptions by MCP session id and URI. Send update notifications only to sessions that subscribed and negotiated resources support.

- [x] **Step 6: Run resources tests**

Expected: PASS.

---

### Task 8: Prompts And Completion

**Files:**
- Create: `McpPromptProvider.java`
- Create: `McpPromptDefinition.java`
- Create: `McpPromptGetRequest.java`
- Create: `McpPromptResult.java`
- Create: `McpCompletionProvider.java`
- Create package: `.../magicapi/plugin/mcp/prompts`
- Create: `McpPromptsHandler.java`
- Create: `StaticMcpPromptProvider.java`
- Create package: `.../magicapi/plugin/mcp/completion`
- Create: `McpCompletionHandler.java`
- Modify: `MagicAPIMcpProperties.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: `.../plugin/mcp/prompts/McpPromptsHandlerTest.java`
- Test: `.../plugin/mcp/completion/McpCompletionHandlerTest.java`

**Interfaces:**
- Produces `prompts/list`, `prompts/get`, `notifications/prompts/list_changed`.
- Produces `completion/complete` for prompt arguments and resource template arguments.

- [x] **Step 1: Write prompts and completion tests**

Tests cover prompt list pagination, required argument validation, rendered multi-message output, list changed notification gating, completion bounded results, and unsupported completion target errors.

- [x] **Step 2: Run tests and confirm failure**

Expected: FAIL.

- [x] **Step 3: Implement prompt provider SPI and handler**

Prompts are separate from resources in the public MCP contract. Enforce `mcp:prompts`.

- [x] **Step 4: Implement completion provider SPI and handler**

Delegate completion by reference type and provider support. Return bounded values and `hasMore` if provider indicates more results.

- [x] **Step 5: Run prompts and completion tests**

Expected: PASS.

---

### Task 9: Logging, Client Capability Calls, Cancellation, And Progress

**Files:**
- Create package: `.../magicapi/plugin/mcp/logging`
- Create: `McpLoggingHandler.java`
- Create: `McpLogService.java`
- Create package: `.../magicapi/plugin/mcp/client`
- Create: `McpClientRequestService.java`
- Create: `McpClientCapabilityException.java`
- Modify: `McpSession.java`
- Modify: `McpSessionService.java`
- Modify: `McpLifecycleHandlers.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: `.../plugin/mcp/logging/McpLoggingHandlerTest.java`
- Test: `.../plugin/mcp/client/McpClientRequestServiceTest.java`

**Interfaces:**
- Produces `logging/setLevel`, `notifications/message`.
- Produces server-initiated `roots/list`, `sampling/createMessage`, `elicitation/create`.
- Produces in-flight request cancellation and progress notification support.

- [x] **Step 1: Write logging and client capability tests**

Tests cover per-session logging level, log notification gating, roots request correlation, sampling/elicitation unsupported capability errors, cancellation propagation marker, and progress notification routing.

- [x] **Step 2: Run tests and confirm failure**

Expected: FAIL.

- [x] **Step 3: Implement logging handler and service**

Store per-session logging level. Only send `notifications/message` when negotiated and authorized.

- [x] **Step 4: Implement client request service**

Generate JSON-RPC request ids, send requests over active SSE emitter, track pending requests, correlate responses, and reject unsupported capabilities.

- [x] **Step 5: Implement cancellation/progress hooks**

Track in-flight request ids in session. Mark cancellation and route progress notification by progress token/session.

- [x] **Step 6: Run tests**

Expected: PASS.

---

### Task 10: Sample, Documentation, And Verification

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/README.md`
- Modify: `openspec/changes/add-standard-mcp-server-plugin/tasks.md`

**Interfaces:**
- Produces sample dependency for `fw-plus-magicapi-plugin-mcp`.
- Produces README with endpoint, Sa-Token OAuth2, scopes, provider SPI, and no-datasource guidance.

- [x] **Step 1: Add sample dependency**

Add:

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus.boot</groupId>
	<artifactId>fw-plus-magicapi-plugin-mcp</artifactId>
</dependency>
```

- [x] **Step 2: Write README**

Document:

```md
# MagicAPI MCP Plugin

- Default endpoint: `/mcp`
- Config prefix: `magic-api.plugin.mcp`
- Auth: Sa-Token OAuth2 Bearer token
- Content model: MCP tools, resources, prompts
- Not supported: mcp-datasource or SSE plugin coupling
```

- [x] **Step 3: Run no-SSE/no-datasource scans**

Run:

```bash
rg -n "plugin\\.sse|fw-plus-magicapi-plugin-sse|mcp-datasource|resources-datasource|prompts-datasource|sse-subscribe|sse-datasource" fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp
```

Expected: no matches except README text that explicitly says these are not supported.

- [x] **Step 4: Run OpenSpec validation**

Run:

```bash
openspec validate add-standard-mcp-server-plugin --strict
openspec validate --specs --strict
```

Expected: PASS.

- [x] **Step 5: Run Maven verification**

Run:

```bash
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DfailIfNoTests=false test
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin -DskipTests -DfailIfNoTests=false test
cd fw-plus-boot && ./mvnw -pl fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample -am -DskipTests -DfailIfNoTests=false test
```

Expected: PASS.

- [x] **Step 6: Run diff check**

Run:

```bash
git -C fw-plus-boot diff --check
git diff --check
```

Expected: no whitespace errors.

- [x] **Step 7: Update OpenSpec task checkboxes**

Only after the corresponding implementation and tests pass, check off completed items in `openspec/changes/add-standard-mcp-server-plugin/tasks.md`.
