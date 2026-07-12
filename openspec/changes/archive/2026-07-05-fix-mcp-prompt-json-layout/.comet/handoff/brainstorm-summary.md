# Brainstorm Summary

- Change: fix-mcp-prompt-json-layout
- Date: 2026-07-05

## 确认的技术方案

采用“前端 JSON 组件布局 + 后端结构化 JSON 兼容”的方案。

1. MCP Prompt 信息区不再以普通 textarea 作为参数和消息主要编辑控件，改为 JSON 组件/JSON 编辑器布局。
2. 默认 Prompt 数据结构调整为 MCP 标准 message content 对象：`content: { type: "text", text: "..." }`。
3. 后端 provider 继续解析 `argumentsJson` / `messagesJson`，保持旧资源兼容。
4. 增加测试覆盖嵌套 JSON placeholder 渲染和参数 required 解析。
5. 文档补充 arguments/messages JSON 示例。

## 关键取舍与风险

- 不引入新前端依赖，优先复用 MagicAPI 控制台已有 JSON 组件能力。
- 若运行时 JSON 组件名称与预期不同，需要采用当前控制台可识别的 JSON 编辑配置方式。
- 后端保持字段兼容，避免迁移旧资源。

## 测试策略

- Provider 单元测试覆盖 `prompts/list` 参数定义解析和 `prompts/get` 嵌套 message 渲染。
- Prompts handler 测试覆盖 required 参数校验不退化。
- Maven targeted test：`McpPromptsHandlerTest,MagicApiMcpPromptProviderTest`。
- 最终运行 autoconfigure 模块相关测试。

## Spec Patch

无。
