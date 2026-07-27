---
comet_change: abstract-ai-model-and-embedding
role: technical-design
canonical_spec: openspec
---

# AI 模型与向量存储框架无关抽象设计

## 1. 目标与发布边界

本设计把现有 LangChain4j 专属模型和向量存储公共能力下沉为 Java 8 可用的中立核心。LangChain4j 保持完整默认实现，确保既有 Spring Boot 2.7 应用、配置、Magic-API 资源和脚本无需立即迁移。

Spring AI、Spring AI Alibaba、AgentScope 2 Java 的首版不实现原生 model/store adapter。它们以同仓 Java 17+ / Spring Boot 3+ 独立 artifact 交付，完成 parent、BOM、starter、条件装配、诊断和迁移文档边界。真实 SDK adapter、能力声明和 SDK 契约测试属于后续 OpenSpec change，不能被首版文档描述为已经支持运行。

## 2. 模块布局与依赖方向

```text
fw-plus-core/fw-plus-project/fw-plus-ai/             # Java 8
├── fw-plus-ai-model/                                # 中立模型 API/SPI
├── fw-plus-ai-store-embedding/                      # 中立向量存储 API/SPI
└── fw-plus-ai-langchain4j/
    ├── fw-plus-ai-langchain4j-model/                # 完整默认 adapter + 兼容入口
    └── fw-plus-ai-langchain4j-store-embeding/       # 完整默认 adapter + 兼容入口

fw-plus-ai-j17/                                      # Java 17+，独立 Maven 聚合
├── fw-plus-ai-j17-parent/                           # Spring Boot 3+ parent/BOM
├── fw-plus-ai-spring-ai-j17-starter/                # 首版骨架
├── fw-plus-ai-spring-ai-alibaba-j17-starter/        # 首版骨架
└── fw-plus-ai-agentscope-j17-starter/               # 首版骨架
```

`fw-plus-ai-j17` 不进入 Java 8 Core reactor。它只依赖已发布到本地或远程 Maven 仓库的中立 core artifact，不反向依赖 LangChain4j adapter。这样 Java 8 构建不会解析 Java 17 字节码、Boot 3 或目标 SDK；Java 17 构建则复用稳定中立 API。

## 3. 中立 API

### 3.1 模型

`fw-plus-ai-model` 公开 `AiModelDefinition`、`AiModelType`、`AiModel`、`AiModelRegistry`、`AiModelFactory`、`AiModelAdapter` 与受控异常类型。定义包含稳定 key、framework、provider、能力类型、启用状态和扩展 properties。

调用方通过 `AiModelRegistry` 取得 key 或默认模型，通过 `AiModel` 使用共同能力。原生对象只能由对应 adapter 在受控扩展点处理，不能出现在 Core API、AI Boot 自动配置参数、Magic-API 模块签名或资源协议中。adapter 必须能够报告支持的 framework/provider/能力组合；没有匹配项时抛出包含 key、framework、provider 和能力类型的错误。

### 3.2 向量存储

`fw-plus-ai-store-embedding` 公开中立文档、metadata、向量、查询、匹配结果、`AiEmbeddingStore`、registry、factory 和 adapter SPI。LangChain4j 的 `EmbeddingStore<TextSegment>` 是 `fw-plus-ai-langchain4j-store-embeding` 的实现细节，Spring AI 的 `Document` 等未来原生对象也同样不得跨越中立边界。

registry 负责按 key/default 查找，factory 负责通过 adapter 构造；重复 key、无效默认 key、缺失 adapter 和未支持 provider 都在启动期失败并保持凭证脱敏。

## 4. 默认实现、配置与 Magic-API

LangChain4j adapter 是唯一首版完整 adapter。原 artifact 保留并改为依赖中立 core，原 provider、模型别名、多模型和默认 key 行为必须回归验证。

新增 `fw-plus.ai.model` 和 `fw-plus.ai.embedding-store` 中立配置。旧 `fw-plus.ai.langchain4j.model`、`fw-plus.ai.langchain4j.embedding-store` 在没有同 key 中立定义时映射为 `framework=langchain4j`；同 key 时中立配置优先并输出不含密钥的迁移提示。

AI Boot 和 Magic-API model/vector 插件只注入中立 registry/factory。`model-datasource`、vector store key、`model[...]` 与 `vector[...]` 维持兼容。资源可选 `framework` 字段将留给后续真实 Java 17 adapter；首版遇到目标框架时必须说明“artifact 骨架已存在，但原生 adapter 尚未实现”，不能静默回退到 LangChain4j。

## 5. Java 17+ 对接文档

新增迁移指南目录，至少包含：

```text
docs/ai-framework-migration/
├── overview.md
├── spring-ai-java17.md
├── spring-ai-alibaba-java17.md
└── agentscope-java17.md
```

`overview.md` 提供兼容矩阵：Java 8 / Boot 2.7 中立核心与 LangChain4j 完整支持；Java 17 / Boot 3+ 目标 artifact 骨架；后续真实 adapter 的状态。每个框架文档必须按同一顺序给出：

1. JDK、Spring Boot、SDK 和 Maven parent/BOM 前置条件。
2. Java 17 aggregator 与 starter artifact 的引入方式，明确它不进入 Java 8 应用依赖图。
3. 从旧 LangChain4j 配置迁移到中立配置的同 key 示例，以及当前不可启用真实 adapter 的原因。
4. Java 代码从 `LangChain4jModelRegistry`/`EmbeddingStoreRegistry` 迁到中立 registry 的替换示例。
5. Magic-API 资源、`model[...]`、`vector[...]` 的不变部分与目标 framework 暂不可运行的边界。
6. Java 17 模块编译、条件装配、依赖隔离验证命令，以及恢复至旧 LangChain4j starter/configuration 的回滚步骤。
7. 后续真实 adapter change 需要补齐的 SDK 接入、能力声明、契约测试和远端集成测试清单。

## 6. 错误处理与安全性

- 对缺失 starter、SDK、adapter、provider 或能力的启动错误，统一输出 framework、provider、key 与能力，禁止输出 apiKey、secret、token 或完整连接字符串。
- Java 17 starter 骨架的条件装配必须避免产生半初始化 Bean；一旦用户尝试启用未实现 framework，错误必须指出当前发布状态和文档入口。
- 同 key 配置冲突只采用中立配置，旧配置只作为兼容映射来源；日志说明迁移动作但不回显 properties。

## 7. 测试策略

Java 8 线：中立 core fake adapter/store 契约测试、LangChain4j 模型/store 回归、旧配置映射、中立配置优先、AI Boot 自动配置、Magic-API 动态资源与脱敏集成测试。

Java 17 线：三个 artifact 的 Java 17 编译、Boot 3 parent/BOM 解析、starter 条件装配、对 Java 8 默认依赖图的隔离、未实现 adapter 的明确诊断，以及迁移文档中的依赖/配置片段校验。首版不运行真实模型服务、向量库或 SDK model/store 契约测试。

构建顺序是先发布/安装 Java 8 Core 中立 artifact，再使用 Java 17 构建 `fw-plus-ai-j17`，最后运行 AI Boot 和样例的相关回归测试。

## 8. 实施顺序与回滚

先完成兼容性探针和中立 Core，随后迁移 LangChain4j 默认 adapter，再改造 AI Boot/Magic-API，之后创建 Java 17 aggregate skeleton 和文档，最后补齐构建矩阵与回归测试。

若中立迁移出现问题，保留旧 LangChain4j artifact 和配置可回退；若 Java 17 artifact 出现问题，只移除对应 Java 17 starter，不影响 Java 8 默认路径。兼容层在一个明确的后续弃用周期内保留，移除必须通过新的破坏性 change。
