# Brainstorm Summary

- Change: add-standard-mcp-server-plugin
- Date: 2026-07-04

## 已确认事实

- 新增独立 fw-plus-magicapi-plugin-mcp，不依赖已删除 SSE 插件，不修改 ssehttp。
- MCP 内容不使用 datasource 模型。
- MCP 自定义内容按标准分为 resources 和 prompts。
- OAuth 使用 Sa-Token OAuth2，MCP 插件只做 Resource Server/Protected Resource 侧适配，不自建 Authorization Server。
- 需要覆盖完整 MCP Server 能力：transport、lifecycle、tools、resources、prompts、completion、logging、subscriptions、notifications、client capability calls、cancellation、progress。

## 候选技术方案

1. 协议内核 + Provider SPI + Sa-Token Resource Server 适配（已确认采用）。
2. 基于 MagicAPI ResourceStorage 深度绑定实现。
3. 引入外部 MCP SDK/Server 框架再适配 MagicAPI。

## 确认的技术方案

- 采用协议内核 + Provider SPI + Sa-Token Resource Server 适配。
- 默认 endpoint 使用 `/mcp`，通过 `magic-api.plugin.mcp.endpoint` 配置覆盖。
- resources/prompts 对外仅暴露 MCP 标准能力，内部通过 Java Provider、静态配置、MagicAPI 脚本 Provider 扩展。
- MCP session 独立于 Sa-Token session；Sa-Token 负责认证授权，MCP session 负责协议态。
- 不使用 datasource，不恢复 SSE 插件，不修改 `ssehttp`。

## 关键取舍与风险

- 不直接绑定 MagicAPI ResourceStorage，避免形成 datasource 式耦合；代价是需要定义 Provider SPI。
- 不引入外部 MCP SDK，避免 Java 8 / Spring Boot 2.7 / 依赖冲突风险；代价是需要自研协议模型和测试覆盖。
- 使用 Sa-Token OAuth2 作为授权基础，避免重复 OAuth Server；代价是需要对 Sa-Token 配置差异做条件加载和错误兜底。

## 测试策略

- Controller 层测试 HTTP/header/status。
- Dispatcher 层测试 JSON-RPC 错误码、notification、batch、ping。
- Session 层测试 initialize、initialized、过期、DELETE。
- Auth 层测试 401、403、scope、Origin。
- Feature 层测试 tools/resources/prompts/completion/logging。
- Negotiation 层测试 list_changed、resources updated、roots/sampling/elicitation 能力 gating。
- Maven 层测试 autoconfigure、插件聚合、sample `-am`。

## Spec Patch

补充默认 endpoint 为 `/mcp` 且可通过配置覆盖。
