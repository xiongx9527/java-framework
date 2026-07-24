## Context

当前 `fw-plus-ai` 已包含 `fw-plus-ai-boot-dependencies`、`fw-plus-ai-boot-parent`、`fw-plus-ai-boot-autoconfigure`、`fw-plus-ai-boot-starters` 以及 LangChain4j/OpenAI/Magic-API AI 相关 starter。新能力必须作为这些模块的增量扩展，而不是另起一套不受 BOM 和自动配置体系管理的 AI 栈。

SQLBot v1 提供了智能问数产品形态，核心包括数据源选择、表结构检索、Prompt 模板、Text-to-SQL、SQL 执行、图表配置和日志。Aiflowy、Agents-Flex、Tinyflow 提供 Java AI 应用平台、Agent 和流程编排参考，LangGraph4j 可作为 Java 版 LangGraph 的图编排参考，尤其适合作为多 agent 协同、基础 AI 工作流和流程节点扩展的设计参考；但它们的运行时版本和架构目标与本仓库不同。本设计只借鉴能力边界和扩展点，落地必须遵守 fw-plus 的 Java 8、Spring Boot 2.7.18、Maven Wrapper 和现有模块风格。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-ai-boot-starters` 下新增 `fw-plus-ai-askdata-spring-boot-starter`。
- 结合现有 `fw-plus-ai` BOM、父 POM 和 LangChain4j 依赖管理，提供 AskData 核心服务；模型调用主路径优先使用 LangChain4j。
- 提供 Java 8 兼容的 API：自然语言问题输入，输出 SQL、查询数据、执行步骤和错误信息。
- 提供可替换的数据源元数据提供器、SQL 执行器、模型调用器、Prompt 模板和 SQL 校验器。
- 模型管理和向量能力优先复用现有 `fw-plus-ai` 模块；模型调用主要使用 LangChain4j。技术兜底顺序为 Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其他实现。当前 Java 8 Starter 不得引入 Spring AI 运行时依赖，也不得绕开现有模块新增平行体系。图编排如需外部库，优先评估 LangGraph4j JDK8 兼容版本；不使用 Java 17+ 的 LangGraph4j 主线模块。
- 模型配置必须支持多个模型配置，并允许声明一个默认模型；请求可以显式指定模型 key，未指定时使用默认模型。
- 提供多 agent 与子 agent 协作抽象，使选表、生成 SQL、校验、执行、结果解释等步骤可以独立实现，并允许主 Agent 在步骤内部派生子 Agent 处理子任务。
- 提供可持久化的 AskData 会话上下文，支持自动压缩长上下文，并允许应用异常退出或重启后按会话恢复到最近稳定步骤继续操作。
- 默认只允许查询类 SQL，并通过配置控制默认行数限制、最大结果行数、危险关键字策略和是否执行 SQL。
- 为后续 Magic-API 插件提供稳定服务接口，插件只负责脚本侧封装。

**Non-Goals:**

- 不逐行翻译 SQLBot Python/FastAPI 代码。
- 不使用 SQLBot 品牌、Logo、路径、配置前缀或包名。
- 不迁移 SQLBot 完整 UI、用户体系、安装器、license/xpack、MCP Server 或运营后台。
- 不引入要求 Java 9+ 或 Spring Boot 3 的依赖。
- 不在本 change 中实现 Magic-API `import askdata` 脚本模块。

## Decisions

### Decision: 命名采用 AskData

模块命名为 `fw-plus-ai-askdata-spring-boot-starter`，包名使用 `com.xiongx9527.fw.plus.ai.askdata`，配置前缀使用 `fw.plus.ai.askdata`。

替代方案是沿用 SQLBot 命名。该方案识别度高，但存在品牌和许可证表达风险，也不符合 fw-plus 自有模块命名。AskData 更中性，能表达“问数据”的能力。

### Decision: Starter 内聚核心能力，插件依赖核心服务

AskData Starter 暴露 `AskDataService`、`AskDataRequest`、`AskDataResult` 等核心接口；Magic-API 插件后续只注入该服务并包装为脚本模块。

替代方案是在 Magic-API 插件中直接实现问数逻辑。该方案首屏见效快，但会导致 Java 业务项目无法复用核心问数能力，也让脚本插件承担过多职责。

### Decision: 复用现有 fw-plus-ai 依赖管理

新增模块挂在 `fw-plus-ai-boot-starters` 下，优先复用现有 LangChain4j 相关依赖和 AI BOM 管理。若需要新增 SQL 解析或 JDBC 辅助依赖，先放入 AI BOM 或父 POM 的依赖管理，并确认 Java 8 和许可证兼容。

替代方案是在新 starter POM 中直接写死版本。该方案局部简单，但会破坏 fw-plus 的统一版本管理。

### Decision: 模型管理和向量能力复用现有模块

AskData 的模型调用主路径接入现有 LangChain4j 相关 starter 和模型配置能力；表结构召回、术语召回、训练样例召回等向量能力优先复用现有 LangChain4j embedding、Milvus、Qdrant、Weaviate 模块。若现有模块只面向 Magic-API 脚本，无法被 Java 服务层复用，则本 change 的后续设计/实现任务需要把公共接口上移或补充适配，而不是在 AskData 内重新维护一套模型和向量客户端。

Spring AI、Agents-Flex、EasyAi 或其他实现只作为 `AskDataModelRegistry`、`AskDataChatModel`、`AskDataEmbeddingModel` 的可选适配来源，顺序为 Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其他实现。第一版 Java 8 默认自动配置不得因为缺少这些框架而失败，也不得直接依赖 Spring AI。

LangGraph4j 主线 `1.8.x` 适合作为 Spring Boot 3 / Java 17 版本的图编排候选；当前 Java 8 Starter 如需引入图编排库，只能评估 `langgraph4j-core-jdk8` 这类 JDK8 兼容构件，且必须保持 optional，不得替代 AskData 自有轻量 Agent 接口。

当前模型配置如果只能表达单个模型，需要改造成多模型注册表：每个模型有稳定 key、供应商/协议、模型名、端点、密钥和扩展参数；配置层提供 `default-model` 指向默认 key。AskData 请求可以携带 `modelKey` 覆盖默认模型。找不到请求指定模型时返回配置错误；未指定模型且默认模型缺失时启动或调用阶段返回明确错误。

替代方案是 AskData starter 内置独立模型管理和向量存储配置。该方案隔离度高，但会造成重复配置、重复依赖和运维复杂度，也不符合“结合目前已有 fw-plus-ai 模块”的目标。

### Decision: 核心流程拆为可替换组件

默认流程为：构建上下文 -> 获取数据源元数据 -> 生成 Prompt -> 调用模型生成 SQL JSON -> 解析与校验 SQL -> 可选执行 SQL -> 返回结构化结果。该流程既提供简单串行默认实现，也通过 Agent/Step 抽象允许拆成多 agent 协同执行，并允许某个 Agent 派生子 agent 完成表召回、术语召回、SQL 修复、结果解释等子任务。

组件边界：

- `AskDataService`：编排入口。
- `AskDataMetadataProvider`：按数据源和问题返回表结构、字段说明、样例值。
- `AskDataChatModel`：屏蔽 LangChain4j 或其他模型实现。
- `AskDataPromptTemplateProvider`：提供系统提示词、数据库方言规则和用户问题模板。
- `AskDataSqlValidator`：校验只读语句、危险关键字和行数限制。
- `AskDataSqlExecutor`：执行只读 SQL 并转换为结果集模型。
- `AskDataStepLogger`：记录选表、生成 SQL、校验、执行等步骤。
- `AskDataAgent` / `AskDataStep`：定义单个智能问数步骤的输入、输出、状态和错误，用于多 agent 协作。
- `AskDataSubAgent` / `AskDataAgentDispatcher`：定义子 agent 的派发、结果归并、超时和失败处理边界。
- `AskDataFlowExecutor`：按配置顺序串联多个步骤，默认提供单线程串行执行，预留后续并行和工作流编排适配。
- `AskDataConversationStore` / `AskDataContextCompressor`：定义上下文快照、摘要压缩、恢复 token 和步骤检查点的持久化边界，保证长会话和异常重启后可恢复。

替代方案是复刻 SQLBot 的单体 `LLMService`。该方案方便对照源项目，但 Java 版本会形成巨类，难以测试和给 Magic-API 复用。

### Decision: 多 agent 与子 agent 协作先抽象接口，不绑定具体工作流引擎

Starter 先定义 `AskDataAgent`、`AskDataSubAgent`、`AskDataAgentDispatcher`、`AskDataStepContext`、`AskDataStepResult` 和 `AskDataFlowExecutor` 等轻量接口。默认实现使用普通 Java 服务串行执行；子 agent 默认通过同步 dispatcher 顺序执行并归并结果，后续可适配线程池、LangGraph4j JDK8、Agents-Flex、Tinyflow 或自研流程编排，但本 change 不强依赖任何一个外部 Agent 框架。

替代方案是直接引入 Agents-Flex 或 Tinyflow 作为运行时依赖。该方案可以更快获得完整 Agent 能力，但需要额外确认 Java 8、Spring Boot 2.7.18、许可证和依赖体积，容易把 Starter 变成平台级组件。

### Decision: 上下文压缩和崩溃恢复作为核心运行时能力

AskData 流程需要支持长问题、多轮追问、多 agent 和子 agent 步骤，因此 Starter 定义 `AskDataConversationStore`、`AskDataContextCompressor`、`AskDataCheckpoint`、`AskDataResumeToken` 等轻量接口。默认实现可以使用内存存储，生产环境由应用接入 JDBC、Redis 或其他持久化实现。每个关键步骤完成后写入检查点，检查点至少包含会话 ID、请求摘要、已完成步骤、生成 SQL、模型 key、元数据引用、压缩摘要、步骤日志和失败信息。

上下文压缩默认只压缩历史对话、模型原始响应和中间推理摘要，不删除当前步骤必需的结构化字段。恢复时通过 `conversationId` 或 `resumeToken` 读取最近检查点，校验模型、数据源、执行开关和版本信息后继续执行。若检查点缺失、版本不兼容或上一步已产生不安全 SQL，则返回明确恢复错误，不静默重新执行危险步骤。

替代方案是只依赖调用方自己保存请求和结果。该方案实现简单，但无法支撑多 agent 长流程、应用崩溃后的继续操作，也不利于后续 Magic-API 插件展示执行轨迹。

### Decision: 为后续基础 AI 工作流保留演进接口

Aiflowy 的产品定位更接近“企业级 AI 应用开发平台”，后续 fw-plus-ai 如果要沉淀通用 AI 工作流，可以参考它的模型管理、工具节点、流程节点、应用编排和可观测性边界。本 change 只在 AskData 内部定义最小流程抽象，避免提前把 AskData 与未来通用工作流平台强耦合。

后续 Java 17 / Spring Boot 3 线路可以在独立扩展 starter 或 adapter 模块中接入 Spring AI、LangGraph4j 主线和其它新能力。当前 Java 8 Starter 的接口命名、请求/结果模型、Agent 抽象和检查点模型需要保持足够稳定，使 Java 17 模块可以复用契约并替换底层编排实现，而不是重写 AskData 调用方 API。

替代方案是在 AskData starter 中直接实现通用 AI workflow。该方案能一次覆盖更多未来能力，但会让智能问数 starter 承担平台职责，并阻塞当前 AskData MVP。

### Decision: MVP 先做保守 Text-to-SQL 和查询执行

MVP 覆盖 SQLBot 智能问数最核心链路，不包含图表自动生成、预测、推荐问题、训练样例向量检索和 MCP。接口模型保留扩展字段，后续可逐步补充图表配置和 RAG 召回。

替代方案是一次性覆盖 SQLBot 全量能力。该方案范围过大，且会把 UI、权限、训练、MCP 等独立能力混入 Starter，降低交付稳定性。

## Risks / Trade-offs

- [Risk] Text-to-SQL 输出不可控，可能生成危险或低质量 SQL -> 通过只读校验、关键字拒绝、默认 limit、可关闭执行和错误返回降低风险。
- [Risk] Java 8 可用的 SQL 解析库选择受限 -> MVP 可先采用保守规则校验，后续在确认依赖后替换为解析器实现。
- [Risk] 数据库方言差异大 -> Prompt 方言规则和 limit 处理做成扩展点，默认先覆盖 MySQL、PostgreSQL、ClickHouse 等常用方言。
- [Risk] 与现有 fw-plus-ai 模块重复模型适配 -> 模型调用接口主路径适配现有 LangChain4j starter；Spring AI 仅作为未来 Spring Boot 3 / Java 17 线路，Agents-Flex 和其他框架只走可选适配。
- [Risk] 现有模型管理或向量模块当前只适合 Magic-API 插件使用 -> 先抽象可复用 Java 服务接口或适配器，再让 AskData 依赖该接口。
- [Risk] 多模型配置改造影响既有单模型使用者 -> 保留默认模型语义，使旧配置可以迁移为只有一个条目的模型注册表。
- [Risk] SQLBot/Aiflowy/LangGraph4j/Agents-Flex/Tinyflow 参考项目版本栈不同 -> 只参考能力和边界，不把 JDK 17、Spring Boot 3 或完整平台架构带入本 change；LangGraph4j 主线 1.8.x 属于 Java 17+，Java 8 只能评估 JDK8 兼容构件。
- [Risk] 多 agent/子 agent 协作过早绑定具体框架会放大依赖风险 -> 第一版只定义 fw-plus 自有轻量抽象，具体框架适配作为后续扩展。
- [Risk] 自动压缩上下文可能丢失恢复所需信息 -> 结构化请求、SQL、模型 key、数据源 key、步骤状态和错误码不得被摘要替代，只允许压缩历史文本和可重放上下文。
- [Risk] 应用崩溃后重复执行 SQL 可能造成风险 -> 默认恢复只允许从已校验步骤继续，执行步骤需要幂等标记和只读校验记录；无法确认时返回恢复错误。
- [Risk] 后续基础 AI 工作流方向与 AskData 专用流程混淆 -> 本 change 只保留可迁移的流程接口，不把通用工作流平台职责写进 AskData starter。

## Migration Plan

1. 在 AI starter 聚合中新增 AskData starter 模块。
2. 补齐配置属性、自动配置和 `spring.factories` 注册。
3. 实现默认组件和核心服务接口。
4. 添加单元测试覆盖 Prompt 构造、SQL 校验、结果模型和自动配置装配。
5. 使用 `fw-plus-ai` Maven Wrapper 按模块编译验证。

回滚策略：删除新增 AskData starter 模块，并从聚合 POM、BOM、自动配置注册中移除引用。该 change 不修改既有行为，回滚不需要数据迁移。

## Open Questions

- 首批默认支持的数据库方言范围是否限定为 MySQL、PostgreSQL、ClickHouse，其他方言仅通过扩展点接入。
- SQL 解析库是否引入第三方依赖，还是第一版采用保守正则和语句边界校验。
- 现有模型配置对象的具体位置和字段需要在 build 阶段确认，改造时必须保持单默认模型配置的兼容迁移路径。
- 是否在本 starter 中直接提供简单 REST Controller，还是只提供 Java 服务接口；当前设计倾向只提供服务接口，由业务系统或 Magic-API 插件决定暴露方式。
