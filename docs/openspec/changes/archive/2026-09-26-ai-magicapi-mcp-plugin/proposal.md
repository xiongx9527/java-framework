# Proposal

## Why

Boot 层 `fw-plus-boot-autoconfigure` 维护着一套**自研 JSON-RPC** 的 Magic-API MCP 插件（`magicapi/plugin/mcp/`，75 个 Java + 25 个测试），与 AI 层已基于 `aj-mcp` 内核实现的 `fw-plus-ai-mcp-spring-boot-starter` 功能重叠、双轨并存。用户希望收敛到 AI 层统一的 MCP 内核：删除 Boot 层自研实现，在 `fw-plus-ai-boot-autoconfigure` 内基于 mcp starter（aj-mcp）实现功能对齐的 Magic-API MCP 插件，能力对齐现有 README 描述的全功能。

## What Changes

- **BREAKING**: 删除 Boot 层自研 MCP 插件全部实现——`fw-plus-boot-autoconfigure` 中 `magicapi/plugin/mcp/` 包（75 个 Java 文件）及其测试（25 个）。
- **BREAKING**: 更新 Boot 层 `MagicApiPluginAutoConfiguration`，移除对 `MagicAPIMcpConfiguration.class` 的 import（第 16 行）与注册（第 60 行），不再装配 MCP 插件。
- **BREAKING**: 移除 Boot 层 `fw-plus-magicapi-plugin-mcp` 模块（含 `fw-plus-magicapi-plugin/pom.xml` 聚合条目）；其 console 前端资源与 README 按用户确认事项迁移/处置。
- 在 `fw-plus-ai-boot-autoconfigure` 中新增 `magicapi/plugin/mcp/` 实现，基于 aj-mcp 内核（`fw-plus-ai-mcp` / mcp starter）：
  - 将 Magic-API 函数 / API / 任务自动注册为 MCP 工具，支持 groups/paths/methods/opt-in 过滤与 `mcpInputSchema` 回退。
  - 新增 `mcp-resource`、`mcp-prompt` 两类代码类 MagicAPI 资源类型及其 storage 校验（URI/MIME/name/arguments/messages）。
  - Streamable HTTP 与 SSE 端点、协议版本协商（2025-11-25 / 2025-06-18）、`Mcp-Session-Id`/header 语义。
  - Sa-Token OAuth2 token 校验 + capability scope（tools/resources/prompts/…）接入 mcp starter 认证 SPI（`McpAuthService`），保留自定义 verifier 扩展点。
  - SSE 变更通知（tools/resources/prompts list_changed、resources/updated，经 aj-mcp `publishXxxChanged`），Redis-only 集群跨节点分发与回环抑制。
  - Server-to-client 请求（roots/list、sampling/createMessage、elicitation/create）与 pending request 生命周期，复用 aj-mcp 内核能力。
  - 控制台诊断入口（`diagnostics/execute`）与 mcp-resource/mcp-prompt 预览。
  - 可扩展 provider：`McpToolProvider` / `McpResourceProvider` / `McpPromptProvider` / `McpCompletionProvider`。
- 新增配置绑定：MCP + Magic-API 插件开关与过滤规则，配置前缀与 Boot 层既有 README 的兼容性由设计阶段确定（候选 `magic-api.plugin.mcp.*` 或 `fw-plus.ai.mcp.*`）。
- 更新/新增 OpenSpec capability：`mcp-server-starter` 之「与 Magic-API 集成」requirement 行为变更，新增 `magicapi-mcp-plugin` capability 覆盖工具暴露、资源/提示词类型、通知、认证、集群与诊断行为。

## Capabilities

### New Capabilities

- `magicapi-mcp-plugin`: 基于 aj-mcp 内核的 Magic-API MCP 插件——工具暴露（函数/API/任务与过滤规则）、mcp-resource/mcp-prompt 资源类型与 storage、SSE 变更通知、Redis 集群分发、Sa-Token OAuth2 认证与 scope、diagnostics、可扩展 provider 的全部行为。

### Modified Capabilities

- `mcp-server-starter`: 「与 Magic-API 集成」requirement 由「将接口/函数/任务注册为工具」的简单承诺扩展为「由 AI 层 magicapi mcp 插件提供完整工具暴露（含过滤/opt-in/inputSchema）、认证、通知与集群语义」，其交付主体从 Boot 层自研实现迁移至 AI 层 autoconfigure。

## Impact

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure`：删除 `magicapi/plugin/mcp/` 包（75 Java）、相关测试（25）、更新 `MagicApiPluginAutoConfiguration`。
- `fw-plus-boot/.../fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-mcp`：模块移除，聚合 pom `fw-plus-magicapi-plugin/pom.xml` 更新；console 前端资源处置按迁移决策执行。
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure`：新增 `magicapi/plugin/mcp/` 实现、spring.factories / AutoConfiguration.imports 注册、`McpAuthService` 的 Sa-Token OAuth2 实现；pom 需引入 magic-api 插件与 sa-token-oauth2 相关 optional 依赖。
- `docs/openspec/specs/mcp-server-starter/spec.md`：delta spec 修改「与 Magic-API 集成」requirement。
- 依赖层面：`sa-token-oauth2`、Magic-API plugin 依赖的传递关系从 Boot 层模块迁移至 AI 层 autoconfigure（保持 optional，`org.ssssssss` 版本受约束不动）。