# LangChain4j 流式思考事件设计

- 日期：2026-10-04
- 状态：待评审

## 1. 背景与目标

`SimpleAssistant.streamTokenStreamToSse` 已将 LangChain4j 的流式回调转为 SSE：部分回答转成 `token`，思考片段转成 `thinking`，思考阶段结束时转成 `thinking-done`，工具调用转成 `tool-call` / `tool-result`。样例流已能收到工具和回答事件，但没有 `thinking`。

当前样例使用 Ollama 原生 `OllamaStreamingChatModel`。LangChain4j 1.20.2 的 Ollama 适配器仅在 `returnThinking` 启用时将 provider 响应的 thinking 字段交给 `onPartialThinking`；同时，Ollama 的 `think` 参数控制模型是否生成思考内容。Magic-API Ollama 工厂也独立构造流式模型，未透传这两个选项。

目标是在不改变前端 SSE 契约的前提下，盘点所有已注册的流式聊天 provider，让具备原生思考输出能力且 LangChain4j 适配器能提供独立思考片段的 provider 可以配置并发出事件；对不具备该能力的 provider 明确说明限制，不伪造或推测思考内容。

## 2. 范围

**包含**

- 审核 `Langchain4jModelFactoryRegistry` 中全部流式聊天模型工厂的思考输出能力。
- 对具备 LangChain4j 原生参数和独立 thinking 回调支持的 provider，透传其 provider-specific 参数并补回归测试。
- 更新 Magic-API streaming-chat-model 控制台字段元数据及中英文文案，使其与后端工厂实际读取的选项一致。
- 为 Ollama 样例开放 `think` / `return-thinking` 配置，并在样例流式配置中启用两者。
- 保持 `thinking`、`thinking-done`、`token`、`tool-call`、`tool-result` 等现有 SSE 事件名和数据形状。

**明确不做**

- 不从普通回答 token 中按标签、提示词或内容特征推测思考片段。
- 不增加跨 provider 的统一 `thinking.enabled` 选项；不同 SDK 的参数含义、类型和默认值并不统一。
- 不修改 LangChain4j 依赖、第三方模型实现或 core 层 provider 源码。
- 不承诺所有模型或模型版本都会产生思考内容；provider、模型能力和服务端配置仍决定实际输出。

## 3. Provider 覆盖清单

当前代码有 18 个显式实现 `buildStreamingChatModel` 的 provider 工厂。实现阶段须逐一核实对应 LangChain4j SDK 的请求参数、响应映射及 `PartialThinking` 支持，并将每个 provider 标记为“可配置且可发 thinking”“无需开关但可发 thinking”或“适配器/模型不支持独立 thinking”。

| Provider 工厂 | 能力审核要求 |
|---|---|
| Ollama | 验证 `think` 与 `return-thinking` 的请求和回调行为 |
| Bedrock | 核实模型专属 reasoning 参数及 LangChain4j 回调映射 |
| Google GenAI | 核实 thinking 配置和流式响应映射 |
| OCI GenAI | 核实 reasoning 配置和响应映射 |
| Vertex AI Gemini | 核实 thinking 配置和流式响应映射 |
| GitHub Models | 核实兼容模型是否暴露独立思考片段 |
| Qwen | 核实思考开关/预算及适配器映射 |
| Zhipu AI | 核实 reasoning 配置及适配器映射 |
| OpenAI | 核实 reasoning 模型选项及适配器映射 |
| Anthropic | 核实 extended-thinking 参数及适配器映射 |
| Mistral AI | 核实 reasoning 配置及适配器映射 |
| OpenAI Official | 核实 reasoning 配置及适配器映射 |
| Watsonx | 核实 reasoning 配置及适配器映射 |
| Qianfan | 核实思考参数及适配器映射 |
| Xinference | 核实模型/协议是否暴露独立思考片段 |
| LocalAI | 核实所选兼容协议及适配器映射 |
| Cohere | 核实 `thinking-type` 与回调映射 |
| Azure OpenAI | 核实 reasoning 模型选项及适配器映射 |

只有 SDK 存在公开配置能力、且 SDK 流式处理器可以发出独立思考回调时，才将 provider 分类为可支持。仅能在原始 HTTP 响应中观察到 provider 私有字段，但 LangChain4j 不将其映射到 `PartialThinking` 的，不在本变更中添加自定义协议解析；该限制应记录在插件文档中。

## 4. 架构与数据流

```text
provider options / sample properties
        ↓
对应 Langchain4j ModelFactory / 样例模型 Builder
        ↓
provider 流式响应 → LangChain4j PartialThinking（仅 provider 实际支持时）
        ↓
SimpleAssistant.onPartialThinkingWithContext
        ↓
SSE thinking（增量）/ thinking-done（本轮完成）
```

`SimpleAssistant` 的事件桥接是 provider 无关的，不改动其行为。配置缺失、模型不支持或服务端未返回思考内容时，不发送思考事件；回答、工具调用、检索及完成事件照常工作。

Magic-API 继续使用现有 datasource `options` map 表示 provider-specific 参数，不修改资源实体或存储格式。工厂从 `Langchain4jOptionsReader` 读取相应键；控制台 `streaming-chat-model` 的 `providers.js` 和中英文 i18n 字段随工厂变更同步更新。现有元数据交叉校验测试继续作为“前端字段与后端读取键一致”的守护。

Ollama 的样例配置增加 `think`、`return-thinking` 两个可空布尔值，并透传到同步及流式 Builder；样例 `application.yaml` 只为流式模型显式启用它们。Magic-API Ollama chat/streaming-chat 工厂也读取这两个选项，未配置时保持 LangChain4j 默认行为。

## 5. 错误处理与兼容性

- 保留现有 SSE 事件名和 payload，不需要前端迁移。
- 新 provider options 缺省时保持 SDK 默认值，不强制启用思考或改变现有 token/tool 事件。
- provider options 类型非法时沿用 `Langchain4jOptionsReader` 的带资源路径参数校验错误。
- 不把未映射的 reasoning 数据降级为普通 `token` 或静默拼接；没有 `PartialThinking` 时按无思考事件处理。
- Ollama 示例中 `think: true` 是显式选择。若模型或 Ollama 服务不支持 thinking，其他流式事件仍按既有行为运行；不通过伪造 `thinking` 事件兜底。

## 6. 测试与验收

1. **Provider 工厂测试**：对每个支持 thinking 的工厂，断言配置值到 provider Builder 的透传；对于不支持的 provider，确认没有虚假的通用选项或协议解析。
2. **控制台元数据测试**：现有 Node 测试必须验证各 provider 字段与后端工厂读取键完全一致，并覆盖新增中英文文案。
3. **样例配置测试**：断言 Ollama `think` 与 `return-thinking` 从配置绑定到流式模型 Builder；未配置时不强制打开。
4. **事件桥接测试**：用产生 `PartialThinking` 的 TokenStream 替身验证 SSE 收到 `thinking` 与 `thinking-done`，并确认 `token`、工具和完成事件行为不变。
5. **能力说明**：插件 README 按审核结果列出支持配置与不支持原因，避免将“工厂存在”误表述为“模型必然会输出 thinking”。

验收标准：18 个注册 provider 均有明确能力分类；每个可支持 provider 的公开配置、工厂读取、console 元数据和回归测试闭环一致；样例使用的 Ollama 流可以通过 provider 回调产生现有 `thinking` SSE 事件；不支持项有明确文档，不引入内容猜测。

## 7. 实施边界

主要涉及：

- `fw-plus-ai-boot-autoconfigure` 的各 provider `ModelFactory`、对应工厂测试和 `SimpleAssistantTest`。
- `fw-plus-ai-magicapi-plugin-langchain4j-streaming-chat-model` 的 provider 元数据、中英文 i18n、元数据测试与 README。
- `fw-plus-ai-chat-sample` 的 Ollama 配置属性、Builder、YAML、测试与 README。

实现不应改动调用方 SSE 接口签名，也不应把当前工作区已有的 core 层 staged 删除或其他用户修改纳入提交。
