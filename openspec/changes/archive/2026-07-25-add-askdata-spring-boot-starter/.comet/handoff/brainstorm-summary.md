# Brainstorm Summary

- Change: add-askdata-spring-boot-starter
- Date: 2026-07-24

## 已确认事实

- 先推进 `fw-plus-ai-askdata-spring-boot-starter`，后续 `fw-plus-ai-magicapi-plugin-askdata` 依赖其核心 API。
- 目标 Java 版本为 Java 8，Spring Boot 版本为 2.7.18。
- 名称使用 AskData，不使用 SQLBot 品牌名、Logo、路径或配置前缀。
- 参考 SQLBot 的智能问数能力，参考 Aiflowy 的 Java 企业级 AI 应用平台、模型管理、工作流和知识库/RAG 边界，参考 Agents-Flex/Tinyflow 的 Agent/流程编排思想。
- 必须结合现有 `fw-plus-ai` 模块；模型管理和向量能力优先复用现有 LangChain4j、Milvus、Qdrant、Weaviate、embedding 相关模块。
- 模型技术顺序为 LangChain4j、Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其它实现；当前 Java 8 Starter 不引入 Spring AI 运行时依赖。
- 当前模型配置需要支持多个模型配置，并允许一个默认模型；请求可以通过 `modelKey` 指定模型。
- 支持主 Agent、独立 Agent 和子 Agent 协作；Agent 内可以派发子 Agent，子 Agent 需要结果归并、超时记录和失败策略。
- 支持自动压缩上下文和崩溃后继续操作；长会话或多 Agent 流程需要把关键结构化状态写入检查点，应用重启后可以通过会话 ID 或恢复 token 恢复。
- 不做项目管理模块；向量管理属于后续 Magic-API 插件 change。

## 当前仓库观察

- `LangChain4jModelInfo` 已通过 Spring 注入多个模型类型的 Map，包括 chat、streaming chat、embedding、language、image 等，但缺少统一默认模型 key、用途路由和面向普通 Java 服务的模型注册表接口。
- Milvus、Qdrant、Weaviate 插件模块可以构造 LangChain4j `EmbeddingStore`，但当前主要以 Magic-API 动态模块/数据源形式存在，缺少 AskData 可直接依赖的统一向量召回 Facade。
- AI BOM 已管理 LangChain4j、Agents-Flex、Milvus、Qdrant、Weaviate、Spring AI 等版本；新增依赖应进入现有依赖管理，而不是在 AskData 模块内写死版本。

## 候选技术方案

### 方案 A：薄 Starter，主要包装现有 Magic-API AI 模块

优点是实现快，复用多。缺点是普通 Spring Boot 项目会被 Magic-API 插件形态牵着走，API 边界不干净。

### 方案 B：推荐方案，新增 AskData 核心服务和可复用 Facade

新增 `AskDataService`、多模型注册/选择、向量召回 Facade、Prompt/SQL 安全/执行/Agent Flow 抽象。内部优先适配现有 `fw-plus-ai` 模块，必要时把现有模型/向量能力上移为公共 Java 服务接口。

### 方案 C：完整引入 Aiflowy/Agents-Flex/Tinyflow 风格工作流运行时

优点是 workflow 能力完整。缺点是范围过大，并且要处理 Java 8、Spring Boot 2.7.18、依赖体积和许可证兼容，容易阻塞 AskData MVP。

## 候选设计结论

推荐采用方案 B：AskData Starter 定义稳定核心 API 和轻量工作流抽象，默认实现保守可运行；模型、向量、Agent 工作流全部通过接口适配现有 `fw-plus-ai` 能力。Aiflowy 作为后续基础 AI 工作流设计参考，不直接引入为 Starter 运行时依赖。

## 关键取舍与风险

- 多模型配置要兼容现有单模型使用者：旧配置迁移为只有一个条目的模型注册表，并把该条目标记为默认模型。
- 现有向量模块偏脚本插件：需要抽象 `AskDataVectorRetriever` 或类似 Facade，避免 AskData 直接依赖 Magic-API 动态模块。
- SQL 生成安全风险高：默认只允许只读单语句查询，默认加 limit，可配置禁用 SQL 执行。
- 多 Agent 不强绑具体框架：先定义 `AskDataAgent`、`AskDataSubAgent`、`AskDataAgentDispatcher`、`AskDataStep`、`AskDataFlowExecutor`，未来再适配 Agents-Flex/Tinyflow。
- 上下文压缩必须保留可恢复的结构化字段：会话 ID、请求摘要、模型 key、数据源 key、已完成步骤、生成 SQL、校验状态、步骤日志和错误信息不能只存在于自然语言摘要中。
- 崩溃恢复需要避免重复执行风险：恢复只能从最近稳定检查点继续；无法证明 SQL 已通过安全校验或检查点版本不兼容时返回恢复错误。

## 测试策略

- 单元测试覆盖模型选择：默认模型、指定模型、指定模型不存在、默认模型缺失。
- 单元测试覆盖 SQL 安全：危险语句、多语句、空 SQL、缺少 limit 时的处理。
- 单元测试覆盖 Prompt 构建：方言规则、schema、术语和训练样例片段组合。
- Spring 上下文测试覆盖默认 Bean、用户自定义 Bean 替换、自动配置启停。
- 向量 Facade 使用 stub retriever 测试召回输入输出，避免依赖真实 Milvus/Qdrant/Weaviate。
- 子 Agent 测试覆盖结果归并、超时、失败继续/降级/终止策略。
- 上下文恢复测试覆盖自动压缩、检查点写入、重启后继续、检查点缺失和版本不兼容。

## Spec Patch

已回写 delta spec：补充上下文压缩、检查点持久化、应用重启后恢复、恢复失败保护等验收场景。
