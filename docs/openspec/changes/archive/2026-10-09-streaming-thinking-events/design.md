---
comet_change: streaming-thinking-events
role: technical-design
canonical_spec: openspec
archived-with: 2026-10-09-streaming-thinking-events
status: final
---

# 设计：跨提供商流式思考事件

## Context

参见 `proposal.md` 的 Why 和本 change 的两个规格文件。当前 SSE 汇聚在 `SimpleAssistant.streamTokenStreamToSse`：LangChain4j `TokenStream` 的思考回调映射为 `thinking` 与 `thinking-done`，回答和工具回调分别映射为 `token`、`tool-call`、`tool-result`。已知 Ollama 1.20.2 适配器在 `returnThinking` 启用后才把 Ollama thinking 字段交给 LangChain4j 思考回调；Ollama 的 `think` 参数控制服务端生成思考。

Magic-API 的 provider 配置经 `Langchain4jOptionsReader` 读取，前端 streaming-chat-model 子插件 `providers.js` 有后端工厂字段一致性测试。当前注册了 18 个显式实现流式模型构建方法的 provider：Anthropic、Azure OpenAI、Bedrock、Cohere、GitHub Models、Google GenAI、LocalAI、OCI GenAI、Ollama、OpenAI、OpenAI Official、Qianfan、Qwen、Mistral AI、Vertex AI Gemini、Watsonx、Xinference、Zhipu AI。provider SDK 能力和版本并不一致；工厂存在不等同于适配器提供独立思考回调。

## Goals / Non-Goals

**Goals:**

- 完成上述 18 个 provider 的逐项能力判定，并在实现和控制台元数据中保持一致。
- 保持 provider 配置缺省时原有 SDK 默认行为；只对 Ollama 样例显式启用 thinking。
- 保证思考回调结束时先发 `thinking-done` 再发 `done`，避免只返回思考内容的流遗漏思考轮结束事件。
- 通过工厂、控制台元数据和 SSE 回归测试锁定端到端契约。

**Non-Goals:**

- 不新增 provider 无关的 `thinking.enabled` 开关。
- 不在 LangChain4j 未提供独立思考回调时解析私有原始响应或回答文本。
- 不改动同步/非流式调用的用户可见行为，除非某个已有 provider 工厂必须共享同一配置对象；不得因此为同步请求启用思考。

## Decisions

### 1. 以 `PartialThinking` 回调能力作为支持标准

对 18 个 `buildStreamingChatModel` 工厂逐一核对当前 LangChain4j 依赖版本中的公开 builder 参数，以及 SDK 响应处理器是否把独立思考增量映射到 `StreamingChatResponseHandler.onPartialThinking`。结果归入：

| 分类 | 实施行为 |
|---|---|
| 有公开启用选项，且映射为独立 thinking 回调 | 工厂透传 provider 专属选项；streaming-chat-model 控制台只增加该 provider 实际选项；测试断言选项透传及其能力说明 |
| 无需启用选项，但能映射独立 thinking 回调 | 不增造开关；记录 SDK 默认/模型触发条件；用可验证的适配器行为测试 |
| 适配器不映射独立 thinking 回调，或没有公开配置能力 | 不解析私有原始响应，不显示通用思考开关；README 能力表注明限制及已审核的 SDK 版本 |

不能仅凭 provider 厂商具有 reasoning 模型，就认定对应 LangChain4j provider 实现支持独立思考回调。即使适配器支持，模型也可能不返回 thinking；此时 SSE 不产生思考事件是合法行为。

**替代方案：** 给全部工厂增加 `thinking.enabled`。拒绝：它隐藏 provider 参数差异，不能让缺少响应映射的 SDK 发出 `PartialThinking`，并会制造所有 provider 均支持的错误预期。

### 2. provider 配置遵循现有 `options` 与前端元数据边界

有原生配置能力的选项继续用各 SDK 的真实参数名、类型及默认值，经 `Langchain4jOptionsReader` 从现有 datasource `options` 读取。Ollama 使用 `think` 与 `return-thinking`；Ollama 样例属性与 YAML 绑定这两个字段，只在流式模型配置中将其设为 `true`。Magic-API 流式模型的 Ollama 元数据同步显示这两个字段。

其他 provider 只在能力审核通过后才新增工厂读取项和对应 UI 字段。每个 provider 的字段集合由现有 Node 一致性测试从后端 `buildStreamingChatModel` 中读取项反向校验，且增加中英文文案断言。未配置的布尔值传 `null`，保持 SDK builder 默认语义。

**替代方案：** 改 datasource schema 或引入统一嵌套 thinking 子对象。拒绝：现有 provider 差异参数已经由 `options` 表达，无需迁移持久化结构；不同 provider 的选项名与数值含义并不一致。

### 3. SSE 继续由单一 provider 无关的桥接器处理

保留 `SimpleAssistant` 当前的 LangChain4j 回调链与 SSE payload。`onPartialThinkingWithContext` 收到片段时发 `thinking` 并累积文本；思考阶段在转入回答、工具调用或完成时发一次 `thinking-done`。在 `onCompleteResponse` 的完成路径中先 flush 尚未结束的思考轮，再发 `done`。回答、工具或 retrieved 回调行为不因 provider 能力分类而改变。

此桥接覆盖 AiServices TokenStream 与直接 StreamingChatModel 两条路径。后者当前的 `DirectTokenStream.onPartialThinkingWithContext` 是 no-op；将它扩展为保存 thinking handler，并在 `StreamingChatResponseHandler.onPartialThinking` 到达时转发公开的 `PartialThinking` 增量。锁定的 `langchain4j-core:1.20.2` 提供该 handler 回调；直接路径在回答开始时沿用共享 tracker 闭合 thinking，在完成回调中也先闭合 thinking 再发送 `done`。直接路径不新增工具执行能力。

单测使用可控 TokenStream 和 StreamingChatModel 替身分别验证两条路径的思考增量、思考完成与 `token` / `tool-call` / `done` 的先后次序，以及无思考输入不产生思考事件。provider 工厂测试独立验证 SDK 配置，避免把实时模型和外网服务引入单测。

**替代方案：** 由 Controller 或前端区分 provider 并订阅厂商专有事件。拒绝：SSE 桥接已有稳定事件名，provider 专有协议应止于模型适配器。

### 4. 审核结果随 streaming-chat-model 控制台能力文档交付

逐 provider 审核记录到流式模型插件 README，至少包含 provider、当前 LangChain4j 实现/版本、独立思考回调状态、可配置选项（若有）和限制。控制台元数据测试继续保证显示字段恰好等于工厂实际消费字段；文档能力列表用于解释“字段存在”与“某个部署模型必然输出思考”之间的差异。

## Risks / Trade-offs

- **SDK 版本或模型端点升级改变映射行为** → 用本仓库锁定的 SDK 依赖核对公开 API 与适配器源/字节码，并将版本和分类写入 README；升级依赖时能力表测试与审核需要复核。
- **服务端或模型不支持 thinking 选项而返回错误** → 默认不设置选项；只有样例明确启用。沿用现有 SSE error 路径，不把失败伪装成正常回答。
- **能力检查只覆盖工厂参数，未证明真实 provider 响应会产出思考** → 区分“适配器能映射”与“模型必然生成”；离线测试验证配置及本地响应映射能力，真实模型输出作为可选集成检查，不作为跨 provider 必须可重复的单测条件。
- **结束时思考轮未闭合** → 在 `onCompleteResponse` 前 flush 活跃思考轮，并通过只含 thinking 的流测试确保 `thinking-done` 先于 `done`。

## Migration Plan

无需迁移 datasource JSON 结构。既有配置未设置新增 provider options 时仍采用 LangChain4j 默认值。Ollama 样例显式启用 thinking；使用不支持 thinking 的 Ollama 模型时，用户可将流式配置的 `think` / `return-thinking` 设为 `false` 或删除配置。回滚时移除新增 builder 透传及样例开关，旧 SSE 事件保持兼容。

## Open Questions

无。各 provider 的能力分类是实施前需要按固定判定标准完成的核实工作，不是等待用户决定的设计问题。
