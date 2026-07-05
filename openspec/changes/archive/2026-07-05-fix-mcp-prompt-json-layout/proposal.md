## Why

当前 MCP Prompt 控制台信息区将 `arguments` 和 `messages` 分别用普通 textarea 编辑，参数和消息布局与 API 信息区常见的 JSON/结构化配置体验不一致，也容易生成不符合 MCP Prompt 协议的消息结构。用户明确要求参考 API 信息，并将组件类型改为 JSON 组件，以修正参数、消息布局。

## What Changes

- 将 MCP Prompt 信息面板中的参数与消息编辑改为 JSON 组件/JSON 编辑体验，不再使用普通 textarea 作为主要编辑方式。
- 调整默认 Prompt 脚本/数据布局，使 `arguments`、`messages` 与 MCP Prompt API 信息结构一致。
- 保持后端 `McpPromptInfo`、`MagicApiMcpPromptProvider` 对 `argumentsJson` / `messagesJson` 的兼容解析能力。
- 增加测试覆盖，确保参数、消息布局为 JSON 对象/数组时能正确 list/get/render。
- 更新 MCP 插件文档，说明 Prompt 参数与消息的 JSON 格式。

## Capabilities

### New Capabilities
- `mcp-prompt-json-layout`: 规范 MCP Prompt 控制台参数和消息的 JSON 组件布局、默认结构和后端解析兼容行为。

### Modified Capabilities

无。

## Impact

- 控制台前端：`fw-plus-magicapi-plugin-mcp/src/console/src/components/magic-mcp-prompt-info.vue`、`service/magic-mcp-prompt.js`、必要 i18n 文案。
- 后端提供者：`MagicApiMcpPromptProvider` 及相关测试。
- 文档：`fw-plus-magicapi-plugin-mcp/README.md`。
- 不改 MCP JSON-RPC 协议方法名，不改 `mcp-prompt` 资源类型。
