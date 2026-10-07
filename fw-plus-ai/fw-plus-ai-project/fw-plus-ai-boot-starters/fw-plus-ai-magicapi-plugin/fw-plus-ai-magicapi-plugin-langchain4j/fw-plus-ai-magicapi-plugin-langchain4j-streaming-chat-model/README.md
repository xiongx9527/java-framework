# fw-plus-ai-magicapi-plugin-langchain4j-streaming-chat-model

Magic-API 的 LangChain4j **流式对话模型** 控制台前端，对应 datasource type `langchain4j-streaming-chat-model`。

- 后端实现：`fw-plus-ai-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j`。
- 前端产物：`magic-langchain4j-streaming-chat-model.1.0.0.iife.js`，Maven 在 `prepare-package` 阶段构建并复制到 `target/classes/magic-editor/plugins/`，并打进 jar；后端始终以「插件标识 + 全局名 + 产物名」三元组注册，控制台据此加载本插件面板，因此使用方必须把本模块声明为依赖（版本由 `fw-plus-ai-boot-dependencies` 纳管），否则控制台请求该产物会 404。
- `options` 表单字段按 provider 元数据渲染（`src/metadata/providers.js`，与后端工厂实际读取的 options 键一致）。
- 连接测试：`POST /langchain4j/streaming-chat-model/datasource/test`。

```bash
npm install && npm run build   # dist/magic-langchain4j-streaming-chat-model.1.0.0.iife.js
npm test                       # 元数据与后端工厂交叉校验
```

`dist/`、`node_modules/`、`package-lock.json` 不入库。

## 流式 thinking 能力

以下结论按本仓库锁定的 LangChain4j provider 构件与 `StreamingChatResponseHandler` 响应映射核对。表中“支持”表示适配器能把独立思考增量交给 LangChain4j；并不保证任意部署模型或服务端都会返回思考内容。

| Provider | LangChain4j 构件版本 | 独立 thinking 回调 | 原生选项或限制 |
|---|---|---|---|
| Anthropic | `langchain4j-anthropic:1.20.2` | 支持 | `thinking-type`、`thinking-budget-tokens`、`thinking-display`、`send-thinking`、`return-thinking` |
| Azure OpenAI | `langchain4j-azure-open-ai:1.20.2` | 不支持 | 当前 Azure streaming 适配器不映射独立 thinking 增量；不提供思考开关 |
| Bedrock | `langchain4j-bedrock:1.20.2` | 支持 | 适配器映射模型返回的思考内容块；builder 无额外 thinking 选项 |
| Cohere | `langchain4j-community-cohere:1.20.0-beta30` | 支持 | `thinking-type`（`ENABLED` / `DISABLED`）、`thinking-token-budget` |
| GitHub Models | `langchain4j-github-models:1.19.3-beta29` | 不支持 | 当前 streaming 适配器不映射独立 thinking 增量 |
| Google GenAI | `langchain4j-google-genai:1.20.2-beta30` | 支持 | `thinking-budget`、`thinking-level`、`include-thoughts`、`send-thinking`、`return-thinking` |
| LocalAI | `langchain4j-local-ai:1.20.2-beta30` | 不支持 | 当前 streaming 适配器不映射独立 thinking 增量 |
| Mistral AI | `langchain4j-mistral-ai:1.20.2` | 支持 | `send-thinking`、`return-thinking` |
| OCI GenAI | `langchain4j-community-oci-genai:1.20.0-beta30` | 不支持 | 当前 streaming 适配器不映射独立 thinking 增量 |
| Ollama | `langchain4j-ollama:1.20.2` | 支持 | `think` 控制生成，`return-thinking` 控制适配器回调映射 |
| OpenAI | `langchain4j-open-ai:1.20.2` | 支持 | `reasoning-effort`、`send-thinking`、`return-thinking`、`thinking-field-name` |
| OpenAI Official | `langchain4j-open-ai-official:1.20.2-beta30` | 当前工厂不支持 | SDK 的 Responses streaming 类可映射 reasoning summary，但本插件工厂构建的是 `OpenAiOfficialStreamingChatModel`，不映射独立 thinking |
| Qianfan | `langchain4j-community-qianfan:1.20.0-beta30` | 不支持 | 当前 streaming 适配器不映射独立 thinking 增量 |
| Qwen | `langchain4j-community-dashscope:1.20.0-beta30` | 支持 | 适配器映射服务端返回的 thinking 增量；当前 builder 无额外 thinking 选项 |
| Vertex AI Gemini | `langchain4j-vertex-ai-gemini:1.20.2-beta30` | 不支持 | 当前 Vertex streaming 适配器不映射独立 thinking 增量 |
| Watsonx | `langchain4j-watsonx:1.20.2-beta30` | 支持 | `thinking` 控制 Watsonx text-chat thinking 请求参数 |
| Xinference | `langchain4j-community-xinference:1.20.0-beta30` | 不支持 | 当前 streaming 适配器不映射独立 thinking 增量 |
| Zhipu AI | `langchain4j-community-zhipu-ai:1.20.0-beta30` | 支持 | `thinking-type` 与 `clear-thinking` 映射到 Zhipu 原生 thinking 配置 |

未配置 provider 专属选项时保持 LangChain4j SDK 默认行为。即使适配器支持，模型没有返回独立思考增量时也不会发送 `thinking` 事件；不支持的 provider 不会从普通回答文本推测思考内容。
