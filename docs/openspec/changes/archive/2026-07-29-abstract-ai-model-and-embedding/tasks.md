## 1. Java 8 依赖与兼容性基线

- [x] 1.1 读取 Core 与 AI Boot 的父 POM 和有效 POM，确认 Java 8 / Spring Boot 2.7.18 默认路径的实际约束。
- [x] 1.2 在 Core BOM 中仅登记 Java 8 中立 model/store artifact；确认默认依赖图不引入 Spring Boot 3、Spring AI、Spring AI Alibaba 或 AgentScope 2 Java。
- [x] 1.3 在兼容矩阵中记录未来 Java 17 / Boot 3.x 实施门槛：目标 SDK 必须由后续独立 change 的 parent/BOM 管理，并先通过 SDK 版本探针、依赖树和编译验证；本次不执行探针、不创建 BOM 或 parent。

## 2. 中立 Core 模型与向量模块

- [x] 2.1 在根 AI 聚合和 Core BOM 中新增 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 模块及 artifact 管理。
- [x] 2.2 将模型定义、能力类型、门面、工厂、registry、异常和 adapter SPI 迁移到中立模型模块，确保公开签名不含框架类型。
- [x] 2.3 将向量文档、向量、查询、匹配、store 门面、工厂、registry 和 adapter SPI 迁移到中立向量模块，确保公开签名不含框架类型。
- [x] 2.4 为中立模型/store 增加重复 key、默认 key、adapter 匹配、能力不支持与异常上下文的单元契约测试。

## 3. LangChain4j 默认适配与兼容入口

- [x] 3.1 将 `fw-plus-ai-langchain4j-model` 改造为基于中立模型 SPI 的 LangChain4j adapter，保留既有 provider、模型别名和默认 key 行为。
- [x] 3.2 将 `fw-plus-ai-langchain4j-store-embeding` 改造为基于中立向量 SPI 的 LangChain4j adapter，并把 `EmbeddingStore<TextSegment>` 限制在该模块内部。
- [x] 3.3 调整原 LangChain4j artifact、starter、聚合和 BOM，使既有下游依赖仍可解析且不引入其他框架 SDK。
- [x] 3.4 为 LangChain4j adapter 增加模型/store 行为、旧配置映射和原有测试回归覆盖。

## 4. Java 17 / Boot 3.x 后续实施蓝图（本次不实现）

- [x] 4.1 文档定义后续独立 Java 17 / Spring Boot 3.x reactor、parent、Spring AI、Spring AI Alibaba 和 AgentScope 2 Java starter 的目标布局；本次不得创建对应目录、POM、artifact、starter 或自动配置。
- [x] 4.2 文档定义后续 adapter 必须复用 Java 8 中立 SPI、保持目标 SDK 类型不进入公共签名，并通过能力声明和启动期诊断处理不支持能力。
- [x] 4.3 文档明确 AgentScope 2 Java 按进程内 SDK 适配，禁止以 HTTP、MCP 或 A2A 桥接替代原生 adapter。
- [x] 4.4 文档列出后续 change 的前置门槛：SDK 版本探针、独立 Java 17 编译、Boot 3 条件装配、adapter 契约和 Java 8 依赖隔离；本次只验证 Java 8 路径与文档完整性。

## 5. AI Boot、starter 与 Magic-API 集成

- [x] 5.1 新增中立 model/store starter 与属性绑定，并实现中立配置优先、旧 LangChain4j 配置兼容映射和敏感字段脱敏。
- [x] 5.2 改造 AI Boot 自动配置，使其仅依赖中立 registry/factory，并覆盖多 framework、默认 key 和不支持能力的启动期错误。
- [x] 5.3 改造 Magic-API model/vector 插件和动态资源注册器，使其返回中立门面并保持 `model[...]`、`vector[...]`、资源 key 和默认访问兼容。
- [x] 5.4 为 Magic-API model/vector 插件增加动态资源、按 key/default 访问、adapter 缺失和错误脱敏的集成测试。

## 6. 迁移文档与当前交付验证

- [x] 6.1 编写从 LangChain4j 切换到 Spring AI 的迁移指南，包含依赖、配置、Java API、Magic-API、测试与回滚示例。
- [x] 6.2 编写从 LangChain4j 切换到 Spring AI Alibaba 和 AgentScope 2 Java 的迁移指南，并逐项声明运行基线和能力差异。
- [x] 6.3 为旧配置、中立配置和同 key 冲突配置补齐自动配置回归测试，验证中立配置优先且旧路径可回滚。
- [x] 6.4 按 Java 8 Core → AI Boot → sample 的依赖顺序执行 Maven 编译与相关测试，并运行迁移文档校验；后续 Java 17 实施命令只作为蓝图记录，不在本次执行。
