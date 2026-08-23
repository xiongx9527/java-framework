# AskData Java 17 运行时 Adapter 规划

## 发布边界

当前分支基线是 Java 17 / Spring Boot 3.5.8。LangChain4j Adapter 已存在；`fw-plus-ai-spring-adapter` 也已实现 `spring-ai` 与 `spring-ai-alibaba` 的同步 CHAT/EMBEDDING。但 AskData Starter 不默认引入具体 adapter/provider，本 change 也未把 AskData 切换到 Spring AI。

Spring AI/Alibaba 首版没有 `AiEmbeddingStoreAdapter` 或 VectorStore bridge；AskData 的 Vector/RAG/Agent/Tool/Remote Runtime 仍是独立后续工作。所有实现继续复用中立契约，不允许把框架 SDK 类型带入以下模块：

- `fw-plus-ai-model`：类型化 Chat/批量 Embedding、用量、实际模型 key 与中立错误。
- `fw-plus-ai-store-embedding`：向量值、不可变 filters 与过滤能力诊断。
- `fw-plus-ai-askdata`：项目上下文、模型/向量/Agent/Tool/运行时 SPI、能力注册表、事件与 SQL 安全契约。

## 工程与部署边界

当前 Spring 模型 adapter 位于现有 Core AI 聚合：

```text
fw-plus-ai-spring/
└── fw-plus-ai-spring-adapter/
```

Boot 条件装配位于现有 `fw-plus-ai-boot-autoconfigure` 并通过 `AutoConfiguration.imports` 注册。应用显式引入 adapter 和官方 provider starter；中立 AskData/model/store starter 不传递它们。仓库没有 `fw-plus-ai-j17` 独立 reactor。

AgentScope、AskData Agent/Vector/RAG 或无法进程内复用中立对象的能力，仍可在后续通过专用 adapter 或 `AskDataRuntimeClient` 对接；远程服务不持有业务数据库裸连接。

无法进程内复用 Java 8 中立对象时，Java 17 服务通过 `AskDataRuntimeClient` 对接。Java 8 主进程继续负责治理授权、元数据过滤、模型路由决策、SQL 插件、最终 SQL 校验、只读执行、检查点、恢复授权与监控日志；远程服务不持有业务数据库裸连接。

## 共用映射

| 能力 | Java 17 Adapter 输入/输出 | Core 注册与约束 |
| --- | --- | --- |
| Chat | SDK Chat 请求、消息、响应、token 用量 | 适配为 `AskDataChatModel`/中立 `AiModel`；返回实际模型 key，SDK 类型不出 Adapter |
| Embedding | 批量文本与 SDK 向量结果 | 适配为 `AskDataEmbeddingModel`/中立 `AiModel`；保持批次顺序、维度与实际模型 key |
| Vector | SDK VectorStore/Document/filter | 适配为 `AskDataVectorStore` 或 `AiEmbeddingStore`；不支持授权 filters 时明确拒绝 |
| RAG | SDK 检索器、advisor 或 retrieval pipeline | 适配为 `AskDataRagRetriever`；必须携带项目、操作人、数据源和授权版本 filters |
| Agent | SDK Agent/workflow/graph | 适配为 `AskDataAgent` 或稳定步骤 key；不得扩大治理后的表、列、参数和 SQL 权限 |
| Tool | SDK tool/function callback | 适配为 `AskDataTool`；工具入参保留项目上下文，外部副作用声明幂等与重放策略 |
| State | SDK memory/session/checkpoint | 只保存运行时私有状态；AskData 会话和检查点仍由 Core Store SPI 管理 |
| Observability | SDK callback/span/event | 转换为 `AskDataEvent`；原始 Prompt、凭据、SQL 参数值、记忆和结果正文默认不导出 |

所有实现通过 `AskDataCapabilityProvider` 注册 `RUNTIME_ADAPTER`、`MODEL`、`VECTOR` 或 `OBSERVABILITY_EXPORTER`，声明稳定 key、实现类、优先级及 Core 契约版本范围。业务 key 由业务配置决定，建议使用 `<framework>.<provider>.<purpose>`，例如 `spring-ai.openai.askdata-chat`；同一 key 重复、未知 key 或版本不兼容必须在启动期失败，不能按 Bean 顺序选择。

## Spring AI Adapter

状态：**模型 adapter 已实现，AskData 专项能力未切换。**

- `framework=spring-ai` 支持同步 `ChatModel` 与批量 `EmbeddingModel`，映射 finish reason、token usage、实际 key，并校验 batch index/数量/向量。
- provider Bean 由 Spring 管理，通过 `properties.beanName` 或唯一 binding 选择；不反射构造 client，不暴露 native accessor。
- 首版不实现 `VectorStore`、Advisor/RAG、Function/Tool 或 Agent 适配；这些能力不能从模型支持状态推导。
- 异常按 infrastructure/request-rejected 稳定分类并脱敏；timeout 使用受控并发执行器。

详情见 [Spring AI 当前实现说明](../ai-framework-migration/spring-ai-java17.md)。

## Spring AI Alibaba Adapter

状态：**DashScope CHAT/EMBEDDING 已实现，AskData 专项能力未切换。**

- 与通用 Spring AI 共用 facade，但以独立 `framework=spring-ai-alibaba` adapter 实例精确选择。
- 显式 Bean 必须是已知 DashScope Chat/Embedding 类型且 provider 为 `dashscope`；不得跨 framework 认领或回退 LangChain4j。
- 首版同样不支持 Vector、Advisor、Tool、Agent/Workflow；SDK 原始错误体和敏感字段不能写入日志。

详情见 [Spring AI Alibaba 当前实现说明](../ai-framework-migration/spring-ai-alibaba-java17.md)。

## AgentScope Adapter

状态：**未实现，仅规划用于 AgentScope Java 2.x、Java 17 / Spring Boot 3。**

- 进程内模式包装 AgentScope model、agent、workflow、tool、memory/session；只把中立 DTO 和稳定状态暴露给 Core。
- 模型与 Embedding 能力仍注册到中立模型层；Agent/workflow 不因模型注册而隐式启动。
- Agent 按稳定步骤 key 接入选表、SQL 生成、校验、执行或解释；选表只能收窄，SQL 生成/改写必须回流 Java 8 管线，执行步骤不得取得裸连接。
- Tool 映射保留项目、操作人、会话、请求和审计关联；产生外部副作用的工具不可自动重放。
- AgentScope memory/session 不能代替 AskData 的共享会话、检查点、租约与授权版本校验。
- SDK 未验证的流式、Embedding、Vector、Tool 或 workflow 能力明确返回不支持，不能创建空代理。

详细 SDK 探针与模块蓝图见 [AgentScope Java 17 规划](../ai-framework-migration/agentscope-java17.md)。

## Remote Runtime 协议

当前 `AskDataRemoteRuntimeRequest` v1 已提供 `projectId`、`operatorId`、`conversationId`、`requestId`、能力 key、审计关联 ID、协议版本、认证主体和幂等键。它只适合受限的模型/运行时调用，不足以直接承载需要授权版本、数据源、超时预算或完整流式序号的受保护能力。

后续 Java 17 change 必须以向后兼容构造器/Builder 或协议 v2 增加：

- `authorizationVersion`、`datasourceKey`、语义版本和已授权对象摘要。
- 总超时、剩余超时、调用尝试、幂等/副作用分类。
- `eventId`、`parentEventId`、`subQuestionId`、`graphNodeId`、`agentKey`、`instanceId`。
- 流式 `sequence`、事件类型、完成/取消标志和可恢复游标。

在这些字段完成前，Remote Runtime 不得读取受保护元数据、向量、长期记忆或执行 SQL。更完整的部署与传输选择见 [Java 17 远程运行时](starter/java17-remote-runtime.md)。

## 鉴权签名与上下文

远程模式至少使用 mTLS 或服务身份令牌，并建议叠加 HMAC-SHA256 请求签名。推荐头部为 `X-AskData-Key-Id`、`X-AskData-Timestamp`、`X-AskData-Nonce`、`X-AskData-Content-SHA256`、`X-AskData-Signature`，签名原文固定为：

```text
METHOD\nPATH\nTIMESTAMP\nNONCE\nBODY_SHA256\nPROJECT_ID\nOPERATOR_ID\nAUTHORIZATION_VERSION\nREQUEST_ID
```

服务端校验时间窗、nonce 防重放、body hash、调用方 audience、能力范围以及项目/操作人与 body 一致性。远程服务身份不能替代终端操作人授权；授权摘要由 Java 8 治理桥生成，远程端只能保持或收窄，不能重写。

## 流式、SQL 与错误语义

SSE/gRPC 流式事件必须单调递增 `sequence`，携带 `requestId` 与父子关联字段，并以唯一 completed/cancelled 事件终结。断线只恢复无副作用模型/Agent 节点；已执行工具或 SQL 状态不明时不自动重放。

远程运行时可以返回 SQL 候选和具名参数，但 Java 8 主进程必须按固定顺序执行组装、绑定、改写、治理谓词注入、方言/元数据校验、Core 最终安全校验、只读执行、结果映射和审计。远程端不能把“SDK 已校验”作为绕过依据。

计划稳定错误分类如下，最终常量由 Java 17 change 在契约测试中锁定：

| 分类 | 示例 | `retryable` |
| --- | --- | --- |
| 基础设施 | timeout、connection、gateway、rate-limit、provider-unavailable | 仅无副作用节点可为 `true` |
| 请求/协议 | invalid-request、protocol-incompatible、unsupported-capability、output-invalid | `false` |
| 安全/策略 | authentication-failed、authorization-denied、content-rejected、budget-exceeded | `false` |
| 状态不确定 | side-effect-unknown、sql-state-unknown、resume-conflict | `false`，转人工或业务恢复 |

## 观测与回滚

Adapter 将 SDK 事件转换为脱敏 `AskDataEvent`，保留项目、操作人、请求、子问题、会话、图节点、Agent、模型、实例和父事件关联。外部 exporter 异常、线程池拒绝或远程监控不可用只产生本地降级诊断，不改变模型、SQL 或检查点结果。

上线按新能力 key 灰度，不覆盖旧 key。回滚顺序为停止新流量、取消无副作用任务、等待有副作用任务进入确定终态、恢复上一已验证 key，并保留审计与检查点。禁止在一个应用内混装 Java 8 Boot 2 Starter 与未来 Java 17 Boot 3 Starter。

## 契约测试与版本矩阵

每个 Adapter 分别完成以下证据后才可从“规划”改为“已实现”：

1. SDK/BOM 探针：固定 JDK 17、Boot 3、Spring Framework、Jakarta、SDK 和 provider 版本，保存 dependency tree。
2. Chat/Embedding/Vector/Agent/Tool 契约：成功、空值、维度、流式顺序、取消、超时和稳定错误映射。
3. 能力注册：显式 key、唯一候选、未知/重复 key、契约版本不兼容和缺 SDK/配置时的启动失败。
4. 安全：跨项目/操作人、授权版本漂移、filter 不支持、签名重放、SQL 回流与敏感信息脱敏。
5. 恢复：无副作用重试、有副作用不重放、检查点/幂等键和跨实例关联。
6. 隔离：Java 8 Core/Starter dependency tree 中无 Spring AI、Spring AI Alibaba、AgentScope、Boot 3 或未选择 provider。
7. 集成：受控凭据下至少一个真实 Chat/Embedding/provider；声明 Vector 能力时必须验证一个真实后端。

| 运行时 | 当前发布状态 | 目标 Java / Boot | 目标接入形态 | 版本结论 |
| --- | --- | --- | --- | --- |
| LangChain4j | Adapter 已实现，业务显式引入 | Java 17 / Boot 3.5.8 | 进程内 | 1.18.0 |
| Spring AI | CHAT/EMBEDDING 已实现；AskData 专项未切换 | Java 17 / Boot 3.5.8 | 现有 Core/AI Boot reactor | 1.1.2 |
| Spring AI Alibaba | DashScope CHAT/EMBEDDING 已实现；AskData 专项未切换 | Java 17 / Boot 3.5.8 | 与 Spring AI 共用 adapter | 1.1.2.2 |
| AgentScope Java 2.x | 未实现 | Java 17 / Boot 3 | 进程内 Agent Adapter；必要时 Remote Runtime | 后续 SDK 探针决定 |
