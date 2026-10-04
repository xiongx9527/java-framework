# 提案：跨提供商流式思考事件

## Why

`SimpleAssistant` 已将 LangChain4j 的独立思考回调映射为 SSE `thinking` / `thinking-done`，但 Ollama 样例流只收到检索、工具和回答事件：Ollama 流式模型没有启用生成思考及回传思考片段所需的选项。Magic-API 动态模型也由独立 provider 工厂构建，缺少对应配置；其他 provider 的思考能力尚未逐一确认，调用方无法判断哪些模型可以向前端提供真实思考事件。

## What Changes

- 修复 Ollama 样例与 Magic-API 动态模型的 thinking 配置透传；在样例流式配置中显式启用 Ollama 的生成与回传选项。
- 审核所有已注册的流式聊天 provider，只有当 LangChain4j 适配器能提供独立思考增量时，才暴露其真实 provider-specific 配置并纳入回归测试；无法映射独立思考内容的 provider 明确记录限制。
- 为 Magic-API 流式模型控制台增加与后端工厂一致的思考选项元数据和文案。
- 验证 SSE 思考回调以现有 `thinking` / `thinking-done` 事件发出；保持 `token`、`tool-call`、`tool-result` 等事件名和数据形状不变。
- 不从普通回答文本推测思考内容，不为所有 provider 添加虚假的统一开关。

## Capabilities

### New Capabilities

- `langchain4j-streaming-thinking-events`：定义 SimpleAssistant 流式对话对独立思考片段的 SSE 契约、事件顺序以及 provider 不支持时的行为。

### Modified Capabilities

- `langchain4j-model-datasource-plugins`：扩展 Ollama 及经能力核实支持思考回调的 streaming-chat-model provider options 与控制台字段契约；配置缺省时保持 SDK 默认值。

## Impact

- AI autoconfigure：`SimpleAssistant` 的 SSE 回调测试、全部 18 个流式 provider 工厂与能力测试。
- Magic-API 流式聊天模型插件：provider 字段元数据、中英文文案、元数据一致性测试及 README 限制说明。
- AI chat sample：Ollama 配置属性、流式模型构建、YAML 与配置测试。
- OpenSpec：新增流式思考事件规格，更新 `langchain4j-model-datasource-plugins` delta spec。
- 不更改 LangChain4j 依赖、第三方实现或 core 层 provider 源码；不更改现有 SSE 消费协议。
