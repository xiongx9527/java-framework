# MCP Resources Prompts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有 SSE MCP 服务中，通过 `SseSubscribeInfo.mcpExposeType` 支持 `resources/list`、`resources/read`、`prompts/list`、`prompts/get`，并统一使用订阅资源的 `script` 作为 MCP 返回内容。

**Architecture:** `SseSubscribeInfo` 增加显式暴露类型，默认 `none`，避免普通 SSE 订阅自动暴露给 MCP。新增 `MagicApiMcpContentAdapter` 负责把启用的 SSE 订阅转换为 MCP resources/prompts，`McpJsonRpcProcessor` 继续负责 JSON-RPC 生命周期和方法分发。

**Tech Stack:** Java 1.8、Spring Boot 2.7.18、MagicAPI 2.2.2、JUnit 5、fastjson2。

## Global Constraints

- 始终使用简体中文沟通。
- 不创建分支，直接在当前分支修改。
- 版本保持 `0.1.1-SNAPSHOT`，JDK 保持 1.8。
- 不覆盖或修改 `fw-plus-auth`。
- 不恢复 `fw-plus-javaparser` 和 MagicAPI Java 插件。
- 生产代码改动先写失败测试，再实现。

---

### Task 1: SseSubscribeInfo MCP 暴露字段

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/model/SseSubscribeInfo.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/SseSubscribeInfoMcpExposeTypeTest.java`

**Interfaces:**
- Produces: `SseSubscribeInfo#getMcpExposeType()` 和 `setMcpExposeType(String)`，默认值 `none`，`copyTo/simple/copy` 保留该字段。

- [x] 写测试：断言默认 `mcpExposeType=none`，copy 后字段不丢失。
- [x] 运行测试确认失败。
- [x] 增加字段并在 `copyTo` 复制。
- [x] 运行测试确认通过。

### Task 2: MCP 内容适配器

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/MagicApiMcpContentAdapter.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/MagicApiMcpContentAdapterTest.java`

**Interfaces:**
- Consumes: `SseSubscribeInfo#getMcpExposeType()`。
- Produces: `listResources(String toolGroupKey)`, `readResource(String toolGroupKey, String uri)`, `listPrompts(String toolGroupKey)`, `getPrompt(String toolGroupKey, String name, JSONObject arguments)`。

- [x] 写测试：`resource/both` 出现在 resources，`prompt/both` 出现在 prompts，`none` 不暴露。
- [x] 写测试：`resources/read` 用 `sse://{key}/{businessId}` 读取订阅资源的 `script` 正文。
- [x] 写测试：`prompts/get` 返回标准 `messages` 内容，message content 使用订阅资源的 `script` 正文。
- [x] 运行测试确认失败。
- [x] 实现最小适配器。
- [x] 运行测试确认通过。

### Task 3: JSON-RPC 分发和文档

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpJsonRpcProcessor.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/starter/MagicAPISseConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpJsonRpcProcessorTest.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-sse/README.md`

**Interfaces:**
- Consumes: `MagicApiMcpContentAdapter`。
- Produces: MCP methods `resources/list`、`resources/read`、`prompts/list`、`prompts/get`。

- [x] 写 processor 测试：初始化 capabilities 包含 `resources` 和 `prompts`。
- [x] 写 processor 测试：READY 后四个新方法可分发，未 READY 时拒绝。
- [x] 运行测试确认失败。
- [x] 注入内容适配器并实现四个方法。
- [x] 更新 README，说明 `mcpExposeType` 取值和 Dify 用法。
- [x] 运行模块测试和 `diff --check`。

### Task 4: SSE 插件前端配置项

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-sse/src/console/src/components/magic-sse-subscribe-info.vue`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-sse/src/console/src/i18n/zh-cn.js`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-sse/src/console/src/i18n/en.js`

**Interfaces:**
- Produces: SSE 订阅编辑表单可配置 `mcpExposeType`，旧数据默认显示为 `none`。

- [x] 增加 `mcpExposeType` 下拉框。
- [x] 增加中英文文案。
- [x] 运行 `npm run build` 验证 SSE 插件前端构建通过。

### Task 5: MCP list_changed SSE 通知

**Files:**
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpSessionService.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpListChangedEventPublisher.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/starter/MagicAPISseConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpJsonRpcProcessor.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpListChangedEventPublisherTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/magicapi/plugin/sse/mcp/McpSessionServiceTest.java`

**Interfaces:**
- Produces: `notifications/tools/list_changed`、`notifications/resources/list_changed`、`notifications/prompts/list_changed` 通过 MCP SSE stream 推送给 READY 会话。

- [x] 写 session 通知测试：只给 READY 且有 emitter 的会话发送 JSON-RPC notification。
- [x] 写事件发布测试：`api` 变化发布 tools list changed，`sse-subscribe` 变化发布 resources/prompts list changed。
- [x] 实现 `notifyReadyStreams` 和 `McpListChangedEventPublisher`。
- [x] 注册 Spring Bean 并同步 capabilities。
- [x] 更新 README 和 OpenSpec。
- [x] 运行模块测试、OpenSpec 校验、前端构建和 `diff --check`。
