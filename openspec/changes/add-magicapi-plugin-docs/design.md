## Context

两个 AI Magic-API 插件已经在代码中形成清晰边界：

- model 插件依赖 `LangChain4jModelRegistry`、`LangChain4jModelFactory` 和 `LangChain4jModelProviderAdapter`，向 Magic-API 暴露 `model` 模块和 `model-datasource` 动态资源。
- vector 插件依赖 `EmbeddingStoreRegistry`、`EmbeddingStoreFactory` 和 `EmbeddingStoreProviderAdapter`，向 Magic-API 暴露 `vector` 模块和 `vector-datasource` 动态资源。

当前缺口是模块目录没有 README。用户需要从自动配置、控制器、动态资源模型、前端表单和 starter 文档中拼接使用方式。

## Goals / Non-Goals

**Goals:**

- 为 model/vector 两个 Magic-API AI 插件提供模块级接入文档。
- 文档覆盖依赖、自动配置开关、动态资源 JSON、Magic 脚本访问、控制器接口、前端构建和注意事项。
- 明确 model 插件中的默认模型、多模型名和单模型 properties 覆盖规则。

**Non-Goals:**

- 不调整 Java 自动配置、注册表、动态资源和 Magic 脚本模块行为。
- 不新增 provider adapter 或扩展脚本快捷操作 API。
- 不调整 Maven 依赖版本、前端构建链路或 Magic-API 插件注册文件名。

## Decisions

- README 按模块拆分，而不是只写在 starter 聚合目录：使用者进入具体插件 artifact 时能直接看到该插件如何接入。
- 只描述当前源码已实现的脚本方法：model 文档展示 `getModel()`、`getModelObject()` 和 `model['key']`，vector 文档展示 `getEmbeddingStore()` 和 `vector['key']`。
- model 多模型配置以 `model-name` 列表为中心说明，不在同一个模型 key 下再嵌套一层 `models`，避免与顶层 `models` 混淆。
- 文档保留 LangChain4j 0.35.0 边界：Chat 使用 `ChatLanguageModel` / `StreamingChatLanguageModel`；`AUDIO`、`VIDEO`、`PDF` 是 fw-plus 扩展 modelType，不声明当前 LangChain4j 0.35.0 存在独立 `AudioModel` / `VideoModel` / `PdfModel` 统一接口。

## Risks / Trade-offs

- [Risk] 文档示例被误认为会自动引入所有 provider SDK。→ 在 README 中说明具体 provider 依赖由业务应用按需引入。
- [Risk] Magic 脚本示例过度承诺高级操作。→ 只写当前对象透传和已有 getter，不写未实现的检索、上传、推理封装。
- [Risk] 动态资源 JSON 与 Spring Boot YAML 配置语义混淆。→ 分章节区分 Spring Boot registry 配置和 Magic-API 动态资源配置。
