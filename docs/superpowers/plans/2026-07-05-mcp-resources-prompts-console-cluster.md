---
change: add-mcp-resources-prompts-console-cluster
design-doc: docs/superpowers/specs/2026-07-05-mcp-resources-prompts-console-cluster-design.md
base-ref: 4b1e2ad0b7a9d654cb076328d4f80eb7da5b666b
---

# MCP Resources Prompts Console Cluster Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add console-managed MCP resources/prompts, scoped API tools, configurable headers, real client notifications, Redis-only cluster support, and a detailed MCP integration README.

**Architecture:** Follow the existing MagicAPI `config` plugin pattern for console-managed resources. Keep MCP provider SPI additive, route outbound JSON-RPC notifications through MCP-owned SSE emitters, and activate cross-node behavior only when Redis-backed MagicAPI cluster support is enabled.

**Tech Stack:** Java 8, Spring Boot 2.7.18, Magic-API 2.2.2, Sa-Token OAuth2, Redis-backed MagicAPI cluster plugin, Vue 3/Vite console bundle, JUnit.

## Global Constraints

- 输出和文档使用简体中文，代码命名保持现有英文风格。
- Java 源码保持 JDK 1.8 兼容，不使用 Java 9+ API。
- 不新增 `mcp-datasource`、`resources-datasource`、`prompts-datasource` 或任何 MCP datasource 模型。
- 不恢复已删除的 SSE 插件，不复用业务 `ssehttp` 作为 MCP transport。
- Redis + `magic-api.plugin.cluster.enabled=true` 才是集群模式；无 Redis cluster 时保持单节点本地行为。
- 保留现有 MCP provider SPI 和默认 header 名称，避免破坏现有客户端。
- 使用 `apply_patch` 做手工编辑；不要回滚用户未要求回滚的改动。

---

## File Structure

Backend new/modified files:

- Modify `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java`: add header, tool scope, resource/prompt, and cluster-related configuration properties.
- Modify `fw-plus-boot/.../plugin/mcp/starter/MagicAPIMcpConfiguration.java`: register new storages, registries, providers, session repository, notification services, and console plugin bundle name.
- Modify `fw-plus-boot/.../plugin/mcp/web/MagicMcpController.java`: read configured header names from `HttpServletRequest`.
- Modify `fw-plus-boot/.../plugin/mcp/web/McpSseStreamService.java`: register and manage real SSE emitters.
- Modify `fw-plus-boot/.../plugin/mcp/session/McpSessionService.java`: delegate session state to a repository abstraction.
- Create `fw-plus-boot/.../plugin/mcp/session/McpSessionRepository.java`: session repository interface.
- Create `fw-plus-boot/.../plugin/mcp/session/InMemoryMcpSessionRepository.java`: current map-backed implementation.
- Create `fw-plus-boot/.../plugin/mcp/model/McpResourceInfo.java`: MagicAPI resource entity.
- Create `fw-plus-boot/.../plugin/mcp/model/McpPromptInfo.java`: MagicAPI prompt entity.
- Create `fw-plus-boot/.../plugin/mcp/service/McpResourceMagicResourceStorage.java`: `mcp-resource` storage and validation.
- Create `fw-plus-boot/.../plugin/mcp/service/McpPromptMagicResourceStorage.java`: `mcp-prompt` storage and validation.
- Create `fw-plus-boot/.../plugin/mcp/service/McpResourceMagicDynamicRegistry.java`: resource change listener.
- Create `fw-plus-boot/.../plugin/mcp/service/McpPromptMagicDynamicRegistry.java`: prompt change listener.
- Create `fw-plus-boot/.../plugin/mcp/provider/MagicApiMcpResourceProvider.java`: console-backed resource provider.
- Create `fw-plus-boot/.../plugin/mcp/provider/MagicApiMcpPromptProvider.java`: console-backed prompt provider.
- Create `fw-plus-boot/.../plugin/mcp/tools/McpToolScopeMatcher.java`: path/group/method/opt-in matching.
- Modify `fw-plus-boot/.../plugin/mcp/tools/MagicApiMcpToolProvider.java`: add scoped API tools and function tool scope.
- Create `fw-plus-boot/.../plugin/mcp/notification/McpNotificationService.java`: send JSON-RPC messages to local session emitters.
- Create `fw-plus-boot/.../plugin/mcp/notification/McpChangeEvent.java`: local/cluster event payload.
- Create `fw-plus-boot/.../plugin/mcp/notification/McpChangeEventPublisher.java`: abstraction for local vs Redis-backed broadcasting.
- Create tests under `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/**`.

Console new/modified files:

- Create `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/src/console/**`: Vite plugin bundle, i18n, icons, services, Vue panels.
- Modify `fw-plus-boot/.../fw-plus-magicapi-plugin-mcp/pom.xml`: add existing exec/resources plugin hooks for console packaging.
- Rewrite `fw-plus-boot/.../fw-plus-magicapi-plugin-mcp/README.md`: detailed integration guide.

OpenSpec tracking:

- Update `openspec/changes/add-mcp-resources-prompts-console-cluster/tasks.md` as tasks complete.

## Task 1: Session Repository And Configurable Headers

**Files:**
- Modify: `MagicAPIMcpProperties.java`
- Modify: `MagicMcpController.java`
- Modify: `McpSessionService.java`
- Create: `McpSessionRepository.java`
- Create: `InMemoryMcpSessionRepository.java`
- Test: `MagicMcpControllerHeaderTest.java`
- Test: `McpSessionServiceTest.java`

**Interfaces:**
- Produces: `McpSessionRepository` with `save(McpSession)`, `findById(String)`, `delete(String)`, `touch(String, long)`, `findAll()`.
- Produces: `MagicAPIMcpProperties.Headers` with defaults for `accept`, `protocolVersion`, `sessionId`, `origin`.
- Consumes: existing `McpSession`, `McpSessionState`, `MagicAPIMcpProperties`.

- [ ] **Step 1: Add failing tests for custom headers and repository-backed sessions**

Add tests proving custom header names are read from `HttpServletRequest`, and `McpSessionService` no longer depends on a private map directly.

- [ ] **Step 2: Run focused tests and verify failure**

Run:

```bash
mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DfailIfNoTests=false -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.**.*Test' test
```

Expected: tests fail because header config/repository abstractions do not exist yet.

- [ ] **Step 3: Implement properties, controller header lookup, and in-memory repository**

Add nested properties and refactor controller methods to pass values read through configured names.

- [ ] **Step 4: Run focused tests and verify pass**

Run the same MCP-focused Maven test command. Expected: session/header tests pass.

- [ ] **Step 5: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/web/MagicMcpController.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/session \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp
git commit -m "feat: make MCP headers and sessions configurable"
```

## Task 2: Console-Backed MCP Resources And Prompts

**Files:**
- Create: `model/McpResourceInfo.java`
- Create: `model/McpPromptInfo.java`
- Create: `service/McpResourceMagicResourceStorage.java`
- Create: `service/McpPromptMagicResourceStorage.java`
- Create: `service/McpResourceMagicDynamicRegistry.java`
- Create: `service/McpPromptMagicDynamicRegistry.java`
- Create: `provider/MagicApiMcpResourceProvider.java`
- Create: `provider/MagicApiMcpPromptProvider.java`
- Modify: `MagicAPIMcpConfiguration.java`
- Test: resource/prompt storage and provider tests.

**Interfaces:**
- Produces: `mcp-resource` and `mcp-prompt` resource folders.
- Produces: provider beans implementing `McpResourceProvider` and `McpPromptProvider`.
- Consumes: MagicAPI `MagicResourceService`, `PathMagicEntity`, and existing MCP provider DTOs.

- [ ] **Step 1: Write failing storage/provider tests**

Tests must cover valid resource URI, invalid URI rejection, prompt required fields, list/read/get translation, and no datasource folder usage.

- [ ] **Step 2: Run tests and verify failure**

Run MCP-focused Maven tests. Expected: new classes missing.

- [ ] **Step 3: Implement entity, storage, registry, and provider classes**

Mirror the `config` plugin's storage/registry pattern, but keep MCP-specific validation and folder names.

- [ ] **Step 4: Register beans in auto-configuration**

Register storage, dynamic registries, and providers with `@ConditionalOnMissingBean` where appropriate.

- [ ] **Step 5: Run tests and verify pass**

Run MCP-focused Maven tests. Expected: resource/prompt tests pass.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp
git commit -m "feat: add console-backed MCP resources and prompts"
```

## Task 3: Scoped MagicAPI API Tools

**Files:**
- Modify: `MagicAPIMcpProperties.java`
- Create: `tools/McpToolScopeMatcher.java`
- Modify: `tools/MagicApiMcpToolProvider.java`
- Test: `MagicApiMcpToolProviderTest.java`
- Test: `McpToolScopeMatcherTest.java`

**Interfaces:**
- Produces: `MagicAPIMcpProperties.Tools`, `Tools.Api`, `Tools.Functions`.
- Produces: `McpToolScopeMatcher#matches(MagicEntity entity, String type)`.
- Consumes: `MagicResourceService.files("api")`, `MagicResourceService.files("function")`.

- [ ] **Step 1: Write failing tests for disabled API exposure and scoped inclusion/exclusion**

Cover default disabled behavior, path/method/group include, exclude precedence, and rejected `tools/call`.

- [ ] **Step 2: Run tests and verify failure**

Run MCP-focused Maven tests. Expected: scope classes/config missing.

- [ ] **Step 3: Implement tool scope properties and matcher**

Use simple wildcard/Ant-style matching consistent with available Spring utilities. Keep Java 8-compatible collection code.

- [ ] **Step 4: Extend `MagicApiMcpToolProvider`**

List and call API-backed tools only when matched. Preserve existing function behavior and existing provider SPI.

- [ ] **Step 5: Run tests and verify pass**

Run MCP-focused Maven tests. Expected: API tool scope tests pass.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpProperties.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/tools \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp
git commit -m "feat: scope MagicAPI APIs exposed as MCP tools"
```

## Task 4: MCP SSE Notifications

**Files:**
- Modify: `web/McpSseStreamService.java`
- Create: `notification/McpNotificationService.java`
- Create: `notification/McpChangeEvent.java`
- Create: `notification/McpChangeEventPublisher.java`
- Modify: `resources/McpResourceSubscriptionService.java`
- Modify: resource/prompt/tool registries as needed.
- Test: notification and SSE tests.

**Interfaces:**
- Produces: `McpNotificationService#send(String sessionId, Map<String,Object> message)`.
- Produces: `McpNotificationService#broadcastListChanged(String capability, Map<String,Object> message)`.
- Consumes: local session ids, client capabilities, `SseEmitter`.

- [ ] **Step 1: Write failing notification tests**

Tests must prove emitters receive JSON-RPC notifications, resource updates are subscription-filtered, and emitters are removed on completion/error/delete.

- [ ] **Step 2: Run tests and verify failure**

Run MCP-focused Maven tests. Expected: notification service missing or empty SSE behavior.

- [ ] **Step 3: Implement SSE emitter registry and notification service**

Register emitters on GET, send JSON-RPC notification maps, handle cleanup callbacks, and keep local-only behavior by default.

- [ ] **Step 4: Wire local file events to notifications**

Tool-related API/function changes send `tools/list_changed`; `mcp-resource` sends list/update notifications; `mcp-prompt` sends prompt list changes.

- [ ] **Step 5: Run tests and verify pass**

Run MCP-focused Maven tests. Expected: notification and SSE tests pass.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp
git commit -m "feat: send MCP change notifications over SSE"
```

## Task 5: Redis-Only Cluster Integration

**Files:**
- Modify/Create MCP notification/session cluster integration classes.
- Modify: `MagicAPIMcpConfiguration.java`
- Test: cluster mode tests with fake publisher/repository.

**Interfaces:**
- Produces: cluster-enabled beans only when Redis/MagicAPI cluster conditions are satisfied.
- Consumes: existing MagicAPI cluster infrastructure and `MagicNotifyService` where appropriate.

- [ ] **Step 1: Write failing tests for cluster boundary**

Tests must show cluster behavior is inactive without `magic-api.plugin.cluster.enabled=true`, and active only when Redis cluster dependencies/beans are present.

- [ ] **Step 2: Run tests and verify failure**

Run MCP-focused Maven tests. Expected: cluster-specific beans/conditions missing.

- [ ] **Step 3: Implement Redis-backed or Redis-coordinated session/event integration**

Use existing MagicAPI cluster infrastructure where practical. Keep local emitter sending on owning node only.

- [ ] **Step 4: Add duplicate/self-loop protection**

Cluster events must include origin id or equivalent information so a node does not duplicate its own local notification.

- [ ] **Step 5: Run tests and verify pass**

Run MCP-focused Maven tests. Expected: cluster boundary and event dispatch tests pass.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp
git commit -m "feat: support Redis-backed MCP cluster events"
```

## Task 6: MagicAPI MCP Console Bundle

**Files:**
- Create: `fw-plus-magicapi-plugin-mcp/src/console/package.json`
- Create: `fw-plus-magicapi-plugin-mcp/src/console/vite.config.js`
- Create: `fw-plus-magicapi-plugin-mcp/src/console/src/index.js`
- Create: `fw-plus-magicapi-plugin-mcp/src/console/src/i18n/zh-cn.js`
- Create: `fw-plus-magicapi-plugin-mcp/src/console/src/i18n/en.js`
- Create: `fw-plus-magicapi-plugin-mcp/src/console/src/service/*.js`
- Create: `fw-plus-magicapi-plugin-mcp/src/console/src/components/*.vue`
- Create: `fw-plus-magicapi-plugin-mcp/src/console/src/icons/*.svg`
- Modify: `fw-plus-magicapi-plugin-mcp/pom.xml`
- Modify: `MagicAPIMcpConfiguration.java`

**Interfaces:**
- Produces: `magic-mcp.1.0.0.iife.js` or matching package version bundle.
- Consumes: MagicAPI console plugin contract with `resource` and `toolbars`.

- [ ] **Step 1: Copy structure pattern from config plugin without copying business semantics**

Create MCP-specific console files for `mcp-resource` and `mcp-prompt`.

- [ ] **Step 2: Implement resource services**

Define icons, language/default script, `requirePath`, and metadata merge behavior for both resource types.

- [ ] **Step 3: Implement Vue editor panels**

Resource panel covers URI, name, MIME type, description, template flag, and content. Prompt panel covers name, description, arguments, and messages.

- [ ] **Step 4: Add console build packaging**

Mirror the config plugin POM plugin declarations and Vite output naming.

- [ ] **Step 5: Build console bundle**

Run the existing Maven/npm build path for the MCP plugin. Expected: IIFE bundle generated and packageable.

- [ ] **Step 6: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/mcp/starter/MagicAPIMcpConfiguration.java
git commit -m "feat: add MCP console resources and prompts"
```

## Task 7: Detailed README

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/README.md`

**Interfaces:**
- Produces: detailed integration guide for developers/operators.
- Consumes: final property names and resource folder semantics from earlier tasks.

- [ ] **Step 1: Rewrite README sections**

Include overview, dependencies, enablement, endpoint, OAuth/Sa-Token, headers, tools, resources, prompts, SSE notifications, Redis cluster, examples, and troubleshooting.

- [ ] **Step 2: Verify examples match final property names**

Cross-check YAML examples against `MagicAPIMcpProperties`.

- [ ] **Step 3: Commit**

```bash
git add fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp/README.md
git commit -m "docs: expand MCP plugin integration guide"
```

## Task 8: Final Verification And OpenSpec Tracking

**Files:**
- Modify: `openspec/changes/add-mcp-resources-prompts-console-cluster/tasks.md`

**Interfaces:**
- Consumes: all implemented tasks.
- Produces: checked OpenSpec task list and verification evidence.

- [ ] **Step 1: Run MCP-focused tests**

```bash
mvn -f fw-plus-boot/pom.xml -pl fw-plus-boot-project/fw-plus-boot-autoconfigure -DfailIfNoTests=false -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mcp.**.*Test' test
```

- [ ] **Step 2: Run relevant starter build**

Use the module build needed for `fw-plus-magicapi-plugin-mcp` and document the exact command/output.

- [ ] **Step 3: Run OpenSpec validation**

```bash
openspec validate add-mcp-resources-prompts-console-cluster --strict
```

- [ ] **Step 4: Run whitespace check**

```bash
git diff --check
```

- [ ] **Step 5: Mark tasks complete and commit verification updates**

```bash
git add openspec/changes/add-mcp-resources-prompts-console-cluster/tasks.md
git commit -m "chore: complete MCP resources prompts change tasks"
```
