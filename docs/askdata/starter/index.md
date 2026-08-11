# AskData Starter

AskData Starter 为 `fw-plus` 提供 Java 8、Spring Boot 2.7.18 基线下的自然语言问数装配。Core 仅暴露厂商无关的 API、上下文和值对象；Starter 负责中立能力注册和可替换 Bean 装配，不默认创建 LangChain4j、Ollama 或 Provider 模型。业务应用仍掌握模型框架选择、认证、项目成员授权、数据源连接与最终运行边界。

## 已实现功能

下表仅列出当前 Starter 与 Core 已有实现及其验证边界，不将接口预留表述为已交付业务功能。

| 能力 | 所属模块 | 公开配置/API | 单机/集群 | 最小验证证据 | 已知限制 |
| --- | --- | --- | --- | --- | --- |
| 中立 Chat/Embedding | `fw-plus-ai-model` | `AiChatModel`、`AiEmbeddingModel`、`AiModelRegistry`；`fw-plus.ai.model.*` | 注册表本身两者均可；实例线程安全与集群能力由 Adapter 声明 | `TypedAiModelContractTest`、`DefaultAiModelFactoryTest` | Core 不创建 Provider，未配置模型 key 时首次调用明确失败 |
| 中立向量存储 | `fw-plus-ai-store-embedding`、`fw-plus-ai-askdata` | `AiEmbeddingStore`、`AskDataVectorStore`；`fw.plus.ai.askdata.vector.*` | `memory` 仅单机；集群必须选择共享能力 | `DefaultAiEmbeddingStoreRegistryTest`、`InMemoryAskDataVectorStoreTest` | 授权 filters 无法证明生效时 fail-closed |
| LangChain4j Adapter | `fw-plus-ai-langchain4j-adapter`、Adapter Starter | `framework=langchain4j`、业务模型/store key | 取决于业务选择的 Provider 和 Store | `DefaultLangChain4jModelFactoryTest`、`LangChain4jAskDataContextCompressorTest` | 必须显式引入 Adapter 和 Provider SDK；没有默认 Ollama、模型或凭据 |
| 核心问数与模型路由 | `fw-plus-ai-askdata`、`fw-plus-ai-boot-autoconfigure` | Core/Boot `AskDataService`、`AskDataModelRouter`；`model.*` | Flow 可在两者运行；集群需共享状态 | `DefaultAskDataFlowExecutorTest`、`AskDataModelRouterTest` | 只有无副作用模型节点的可重试基础设施故障可切换候选 |
| 项目治理与权限 | `fw-plus-ai-askdata` | `AskDataGovernanceProvider`、`AskDataAuthorizationSummary` | 两者均可，真实授权源由业务部署 | `AskDataGovernanceProviderTest`、SQL 授权回归 | 默认治理桥拒绝受保护资源；Starter 不实现业务 IAM/JustAuth |
| SQL 安全与插件 | Core SQL、Boot JDBC 执行器 | `AskDataSqlInterceptor`、`AskDataSqlValidator`；`sql.*` | 单机/集群均可，连接池和审计需共享治理 | `AskDataSqlPipelineTest`、`JdbcAskDataSqlExecutorTest` | 插件无裸连接；改写后必须重新完成治理与最终安全校验 |
| 多问题与 Agent | Core Agent、Boot Flow | `AskDataQuestionPlanner`、`AskDataAgent`、`AskDataAgentGraphRuntime`；`flow.*` | 顺序/内存适合单机；集群需共享状态、租约、幂等 | `AskDataMultiQuestionExecutorTest`、`AskDataMultiQuestionFlowTest` | 已完成 SQL 或状态不确定的副作用不会自动重放 |
| 会话、检查点和运行状态 | Core Storage、Boot Adapter | 分类 Store SPI；`state.*`、`conversation.*` | 默认 `memory` 仅单机；JDBC/Redis/业务共享 Store 支持集群 | `AskDataSharedStorageTest`、`CoreAskDataConversationStoreAdapterTest` | Starter 不创建 JDBC/Redis 客户端、表结构、迁移或凭据 |
| 长期记忆与推荐 | Core Memory、Boot Flow | `AskDataLongTermMemory`、`AskDataRecommendationService`；`state.long-term-memory-store` | 默认内存仅单机；共享能力按 key 选择 | `AskDataMemoryAndRecommendationTest`、`AskDataRecommendationRankingTest` | 先按项目、操作人、数据源和授权对象过滤；不跨人复用画像 |
| 生命周期观测 | Core Observability、Boot Exporter | `AskDataEventPublisher`、`AskDataObservationTransport`；`observability.*` | 本地环形日志仅单机；共享 Store/Transport 可用于集群 | `AskDataObservabilityContractTest`、`VendorObservationExporterTest` | Transport、端点、凭据、重试由业务提供；导出失败不改变问数结果 |
| Starter 条件装配 | AskData Starter、Boot AutoConfigure | `fw.plus.ai.askdata.*`、Core SPI Bean、`AskDataCapabilityProvider` | 启动期校验部署模式和能力兼容性 | `AskDataAutoConfigurationTest`、Starter reactor 测试 | 不传递 Spring AI、Spring AI Alibaba、AgentScope 或未选择 Provider |

## 快速配置

```yaml
fw:
  plus:
    ai:
      askdata:
        enabled: true
        model:
          default-chat-model: business-chat
          chat-candidates:
            - business-chat
        sql:
          execute-enabled: false
          default-max-rows: 100
          enforce-limit: true
```

`business-chat` 是示例业务 key，不是框架默认值。将 Starter 加入应用后，业务必须显式引入 LangChain4j Adapter 及所需 Provider SDK，或提供其他中立模型 Bean，并在中立注册表定义同名 key。SQL 执行默认关闭；业务仅在已配置只读数据源、项目权限与审计留存策略后开启它。

## 默认装配与覆盖

自动配置使用 `@ConditionalOnMissingBean` 提供默认实现。业务可按粒度覆盖模型注册、SQL 校验器、SQL 插件、会话/检查点/Agent 状态存储、监控日志存储、长期记忆、推荐、问题规划器，或直接实现 Core `AskDataService` 接管完整流程。覆盖后仍应保留 Core 的项目/操作人上下文、SQL 安全、审计和恢复边界。

## 阅读路径

- [配置参考](configuration.md)：配置项、默认值和单机/集群限制。
- [运行时替换](runtime-replacement.md)：将业务选择的 LangChain4j Adapter 替换为其他运行时的边界。
- [Java 17 Adapter 规划](../java17-runtime-adapters.md)：Spring AI、Spring AI Alibaba 与 AgentScope 的模块、协议和验收边界。
- [Java 17 远程运行时](java17-remote-runtime.md)：不兼容运行时的远程接入协议和治理要求。
- [SQL 安全](sql-safety.md)：插件链、最终校验、限行与审计。
- [存储矩阵](storage-matrix.md)：默认内存、共享存储和容量规划。
- [插件 SDK](plugin-sdk.md)：模型、向量、SQL、Agent、存储和观测扩展，以及 Java 8 可编译示例。
- [存储运维](storage-operations.md)：迁移、故障恢复、容量、脱敏和验证步骤。
- [数据库设计](database-design.md)：逻辑数据模型与持久化扩展点。
- [流程图](flow-diagrams.md)：业务、技术、故障、恢复、监控与外部集成流程。
- [SQLBot 外部系统](sqlbot-external-integrations.md)：模型、MCP、嵌入、企业平台和联调契约。
- [接口重实现示例](transport-examples.md)：HTTP、RPC、消息和 MCP 的业务侧实现方式。
- [问数调用示例](question-examples.md)：单问题、显式多问题、复合问题和部分失败。
- [验证记录](verification.md)：已运行验证、需要运行的环境验证和当前风险。

## 未包含与业务覆盖接口

当前 Starter 为项目、成员/角色授权、全量连接器和方言、MDL 语义治理、训练资产、持久化存储、REST/MCP 管理接口、嵌入式对接、企业身份平台、仪表板和控制台提供 Core 契约、能力注册、存储、SQL、观测、发布 SPI、数据库设计和联调用例。

默认实现覆盖单机核心问数；完整产品体验由业务模块接入这些契约，并在真实接入时补充权限、审计、容量、企业 IAM/JustAuth、外部系统和回滚验证。
