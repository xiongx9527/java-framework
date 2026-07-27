## 1. 依赖与兼容性基线

- [ ] 1.1 读取四层父 POM 和有效 POM，确认当前 Java、Spring Boot、Spring AI、Spring AI Alibaba 与 AgentScope 2 Java 的版本约束。
- [ ] 1.2 为 Spring AI、Spring AI Alibaba 和 AgentScope 2 Java 分别执行最小 Maven 依赖/编译探针，记录 JDK、Spring 版本和传递依赖冲突结果。
- [ ] 1.3 根据探针结果确定各 adapter 的可发布基线、可选依赖边界和不支持能力，并更新 BOM 版本管理。

## 2. 中立 Core 模型与向量模块

- [ ] 2.1 在根 AI 聚合和 Core BOM 中新增 `fw-plus-ai-model` 与 `fw-plus-ai-store-embedding` 模块及 artifact 管理。
- [ ] 2.2 将模型定义、能力类型、门面、工厂、registry、异常和 adapter SPI 迁移到中立模型模块，确保公开签名不含框架类型。
- [ ] 2.3 将向量文档、向量、查询、匹配、store 门面、工厂、registry 和 adapter SPI 迁移到中立向量模块，确保公开签名不含框架类型。
- [ ] 2.4 为中立模型/store 增加重复 key、默认 key、adapter 匹配、能力不支持与异常上下文的单元契约测试。

## 3. LangChain4j 默认适配与兼容入口

- [ ] 3.1 将 `fw-plus-ai-langchain4j-model` 改造为基于中立模型 SPI 的 LangChain4j adapter，保留既有 provider、模型别名和默认 key 行为。
- [ ] 3.2 将 `fw-plus-ai-langchain4j-store-embeding` 改造为基于中立向量 SPI 的 LangChain4j adapter，并把 `EmbeddingStore<TextSegment>` 限制在该模块内部。
- [ ] 3.3 调整原 LangChain4j artifact、starter、聚合和 BOM，使既有下游依赖仍可解析且不引入其他框架 SDK。
- [ ] 3.4 为 LangChain4j adapter 增加模型/store 行为、旧配置映射和原有测试回归覆盖。

## 4. Java 17 Spring AI、Spring AI Alibaba 与 AgentScope 2 骨架

- [ ] 4.1 创建 Java 17 / Spring Boot 3 独立聚合、parent 和 Spring AI starter 骨架，确保不进入 Java 8 默认依赖图。
- [ ] 4.2 创建 Spring AI Alibaba 与 AgentScope 2 Java 的独立 starter 骨架，复用中立 SPI 而不向公共模块泄漏目标 SDK 类型。
- [ ] 4.3 为三个 Java 17 starter 提供未实现原生 model/store adapter 时的明确条件装配诊断和迁移文档入口。
- [ ] 4.4 验证 Java 17 模块编译、Boot 3 条件装配和 Java 8 依赖隔离；真实 SDK adapter 契约测试移至后续 change。

## 5. AI Boot、starter 与 Magic-API 集成

- [ ] 5.1 新增中立 model/store starter 与属性绑定，并实现中立配置优先、旧 LangChain4j 配置兼容映射和敏感字段脱敏。
- [ ] 5.2 改造 AI Boot 自动配置，使其仅依赖中立 registry/factory，并覆盖多 framework、默认 key 和不支持能力的启动期错误。
- [ ] 5.3 改造 Magic-API model/vector 插件和动态资源注册器，使其返回中立门面并保持 `model[...]`、`vector[...]`、资源 key 和默认访问兼容。
- [ ] 5.4 为 Magic-API model/vector 插件增加动态资源、按 key/default 访问、adapter 缺失和错误脱敏的集成测试。

## 6. 迁移文档与端到端验证

- [ ] 6.1 编写从 LangChain4j 切换到 Spring AI 的迁移指南，包含依赖、配置、Java API、Magic-API、测试与回滚示例。
- [ ] 6.2 编写从 LangChain4j 切换到 Spring AI Alibaba 和 AgentScope 2 Java 的迁移指南，并逐项声明运行基线和能力差异。
- [ ] 6.3 为旧配置、中立配置和同 key 冲突配置补齐自动配置回归测试，验证中立配置优先且旧路径可回滚。
- [ ] 6.4 按 Core → AI Boot → sample 的依赖顺序执行 Maven 编译与相关测试，记录有效 POM、依赖树和测试结果。
