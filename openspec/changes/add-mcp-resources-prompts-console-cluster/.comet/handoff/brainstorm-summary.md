# Brainstorm Summary

- Change: add-mcp-resources-prompts-console-cluster
- Date: 2026-07-05

## 确认的技术方案

本次 MCP 插件增强采用 `config` 插件同类模式：新增 `mcp-resource` 与 `mcp-prompt` 两类 MagicAPI 资源，控制台显示为 `resources` 与 `prompts`，由后端 `MagicResourceStorage`、动态注册器和 console-backed provider 接入 MCP `resources/list`、`resources/read`、`prompts/list`、`prompts/get`。这些资源不是 datasource，不新增任何 `*-datasource` MCP 模型。

API tool 暴露改为显式配置范围控制。MagicAPI `api` 只有在 `magic-api.plugin.mcp.tools.api.enabled=true` 且匹配 include groups、include paths、methods、显式 opt-in，并且未命中 exclude paths 时才作为 MCP tool 暴露。现有 function tool 保留，并补充开关/范围能力，避免业务接口被默认全量开放。

MCP transport 使用独立的 MCP SSE，不复用已删除的 SSE 插件，也不复用业务 `ssehttp`。`GET /mcp` 改为真正注册 session 对应的 `SseEmitter`，用于发送 `notifications/tools/list_changed`、`notifications/resources/list_changed`、`notifications/resources/updated`、`notifications/prompts/list_changed`，以及后续 server-to-client request。

`MagicMcpController` 不再通过固定 `@RequestHeader(value = "...")` 读取 MCP header，而是从 `HttpServletRequest` 按 `magic-api.plugin.mcp.headers` 配置读取，默认保持 `Accept`、`MCP-Protocol-Version`、`Mcp-Session-Id`、`Origin`。

集群边界按项目规则收紧：只有项目引入 Redis 且启用 `magic-api.plugin.cluster.enabled=true` 时才是集群。未启用 Redis cluster 时，MCP 使用本机内存 session 和本机 SSE 事件。Redis cluster 模式下，session 和事件广播通过 Redis/现有 MagicAPI cluster 基础设施协调，最终由持有本地 SSE 连接的节点发送客户端通知。

README 是交付范围的一部分，需要写成详细接入文档，覆盖依赖、配置、Sa-Token OAuth2、resources/prompts 控制台维护、API tool 范围、事件通知、Redis 集群、header 自定义、验证示例与排障。

## 关键取舍与风险

- 取舍：MCP resources/prompts 使用 MagicAPI 资源类型而不是 datasource。这样符合控制台添加资源的业务直觉，也避免污染连接配置模型。
- 取舍：API tool 默认不开放或严格 opt-in。安全性优先于便利性，防止业务 API 被 MCP 客户端无意调用。
- 取舍：SSE emitter 保持本机对象，集群只广播事件，不跨节点搬运 emitter。这样符合 Servlet/SSE 运行时边界。
- 取舍：集群仅在 Redis + `magic-api.plugin.cluster.enabled=true` 下启用。避免没有 Redis 的部署产生“伪集群”承诺。
- 风险：MagicAPI API 元数据里显式 opt-in 字段可能需要借助 options 扩展或约定字段实现。实现时应优先兼容现有 metadata，不强改核心结构。
- 风险：前端 console 构建依赖 Node/Vite。实现应复用现有插件 console 配置，不新增额外前端依赖。

## 测试策略

- 后端单元测试覆盖 MCP resource/prompt storage 校验、provider list/read/get、API tool scope matcher、custom header 读取、session repository、notification payload、subscription filtering、emitter cleanup。
- 集群测试使用 fake/mock Redis cluster 组件验证“未启用 cluster 时本地行为”和“启用 cluster 时广播但只由拥有本地 emitter 的节点发送”。
- 前端构建验证 MCP console bundle 能按现有插件模式输出并被 `MagicAPIMcpConfiguration.plugin()` 引用。
- 文档验证以 README 内容完整性和示例配置可读性为准。
- 最终运行 MCP 相关 Maven 测试、OpenSpec strict、`git diff --check`。

## Spec Patch

已回写 OpenSpec delta spec：

- 新增 console-managed MCP resources/prompts 要求。
- 新增 configurable MagicAPI API tool exposure 要求。
- 新增 configurable MCP transport header names 要求。
- 新增 Redis-backed MCP cluster mode 要求。
- 新增 detailed MCP integration README 要求。
- 修改 Streamable HTTP transport，明确 GET SSE 是 MCP server-to-client stream。
- 修改 MCP tools/resources/prompts，明确变更事件必须真实发送给客户端。
