# Brainstorm Summary

- Change: add-tiny-engine-ai-starter
- Date: 2026-07-30

## 确认的技术方案

TinyEngine AI starter 使用最新 `abstract-ai-model-and-embedding` 结果作为主路径：模型能力通过 `fw-plus-ai-model` 的 `AiModelRegistry` / 中立模型门面获取，向量存储能力通过 `fw-plus-ai-store-embedding` 的 `AiEmbeddingStoreRegistry` / 中立 store 门面获取。LangChain4j 保持默认完整 adapter/provider，但不得成为 TinyEngine AI starter 的业务契约类型。

用户追加确认：AskData 也要和 `add-tiny-engine-ai-starter-design` 一起纳入当前设计。AskData 默认模型注册和向量检索主路径也要对齐当前中立 AI 模块；不另开 change。

## 关键取舍与风险

- 取舍：TinyEngine AI 和 AskData 业务对象保持中立，Provider/adapter 负责转换为 LangChain4j 或未来其他运行时类型。
- 风险：AskData 仍有 LangChain4j 原生兼容入口，直接删除可能破坏业务 Bean；设计要求默认主路径切中立 registry，旧原生入口仅作为可选兼容桥并加 classpath/bean 条件隔离。
- 风险：若中立模型/store 门面不能完整表达 TinyEngine Chat/RAG 或 AskData 检索参数，当前 change 在业务 SPI 层保留参数并定义 unsupported/ignore 策略。
- 风险：RAG 文档切分、Embedding、向量写入如果直接暴露 LangChain4j 文档类型会破坏边界；设计要求使用中立文档、metadata、query、match result 类型。

## 测试策略

- POM 边界测试：TinyEngine AI starter 和 AskData starter 不依赖旧 LangChain4j 专用 model/store artifact，不传递 Spring AI、Spring AI Alibaba、AgentScope2 Java 版必需依赖。
- TinyEngine 自动配置测试：总开关关闭、仅 Chat 开启、RAG 缺模型/store fail-fast、显式 degrade。
- AskData 边界测试：默认 `AskDataModelRegistry` / `AskDataVectorRetriever` 从 `AiModelRegistry` / `AiEmbeddingStoreRegistry` 获得能力；adapter 缺失和 key 缺失错误可诊断；业务 Bean 覆盖有效。
- Provider 测试：默认 LangChain4j adapter 经中立 registry 提供 Chat/Streaming/Embedding/Vector 能力，自定义 Provider 可替换，unsupported capability 有明确错误。
- RAG 测试：文档过滤、metadata、按集合/跨集合检索、缺 store key 错误。

## Spec Patch

- 将 proposal/design/spec/tasks 中的“复用现有 LangChain4j 模型 registry / 多向量 store registry”改为“使用 `fw-plus-ai-model` 的 `AiModelRegistry` 与 `fw-plus-ai-store-embedding` 的 `AiEmbeddingStoreRegistry`”。
- 明确 LangChain4j 是默认 adapter/provider，不是 TinyEngine AI 或 AskData 的业务契约类型。
- 增加 AskData Requirement：AskData starter、模型注册和向量检索主路径使用当前中立 AI 模块。
- 增加依赖边界验证任务：禁止旧 `fw-plus-ai-langchain4j-model`、旧 `fw-plus-ai-langchain4j-store-embeding`、旧 LangChain4j model/store starter 作为 TinyEngine AI 或 AskData 主路径依赖。
