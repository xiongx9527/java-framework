# Brainstorm Summary

- Change: streaming-thinking-events
- Date: 2026-10-04

## 已确认的需求理解

用户要求修复 LangChain4j 流式会话中的独立思考事件未到前端，并要求修复覆盖 Magic-API 插件及其他流式 provider。成功标准是：适配器真实提供独立 thinking 增量时，SSE 按现有事件协议发送；provider 不支持时说明限制，不从回答文本猜测思考内容。

## 确认的技术方案

- 用户已确认候选 A：保留 provider-specific 的 LangChain4j 原生配置；审核全部 18 个流式 provider，只为确实将独立思考映射到 `PartialThinking` 的 provider 增加配置和控制台字段。由共享 SSE tracker 维护 `thinking` 增量与 `thinking-done`，确保先于回答、工具事件或 `done` 完成思考轮。覆盖 `SimpleAssistant` 的 AiServices TokenStream 与直接 StreamingChatModel 两条流式路径。
- 不采用仅修复 AiServices 路径的候选 B；也不采用 provider-specific SSE 分支或从原始/回答文本解析 thinking。
- 已用本项目锁定的 `langchain4j-core:1.20.2` API 确认 `StreamingChatResponseHandler` 提供 `onPartialThinking` 重载，因此直接流式路径可使用公开回调，不需私有响应解析。

## 关键取舍与风险

- 通用桥接层保持 SSE 协议一致，provider 差异止于模型工厂；无独立回调映射的实现不展示伪造选项。DirectTokenStream 本身没有工具执行回调，扩展 thinking 不改变这一现有边界。
- Ollama 的 `think` 控制生成，`return-thinking` 控制 LangChain4j 回调映射；样例仅在流式配置启用，Magic-API 字段由后端选项驱动。
- 当前 `SimpleAssistant.DirectTokenStream` 的 `onPartialThinkingWithContext` 是 no-op，且其直接 `StreamingChatModel` handler 只处理回答；直接 handler 已确认具备公开 thinking 回调，但仍需用该版本 provider 的响应映射能力判定哪些模型会触发。
- provider 能力必须针对本仓库锁定的 SDK 版本逐一核实；模型实际是否返回 thinking 仍取决于服务端与模型。

## 测试策略

- 对支持的 provider 测试原生 options、默认值和构建器透传；对控制台跑前后端元数据一致性测试。
- 对共享 SSE tracker 测试思考增量、单次闭合、thinking-only 完成、思考转回答/工具、无思考输入和事件顺序；另覆盖 DirectTokenStream handler。
- 对 Ollama 样例验证配置属性绑定与流式 builder；不把真实在线模型响应作为必需单测。
- 最终运行受影响的 AI 模块、样例和插件控制台测试，并维护 18-provider 能力表。

## Spec Patch

已确认回写 `langchain4j-streaming-thinking-events`：新增直接 StreamingChatModel 路径将公开 thinking 回调转为既有 SSE thinking 事件的验收场景，并要求思考结束先于回答和 done；任务清单同步加入 DirectTokenStream 测试。
