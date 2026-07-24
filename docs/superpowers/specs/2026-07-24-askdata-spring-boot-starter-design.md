---
comet_change: add-askdata-spring-boot-starter
role: technical-design
canonical_spec: openspec
---

# AskData Spring Boot Starter 技术设计

## 背景

`fw-plus-ai` 目前已经包含 AI BOM、父 POM、自动配置、LangChain4j starter、OpenAI starter，以及 LangChain4j、Milvus、Qdrant、Weaviate 等 Magic-API AI 插件。AskData 的目标不是另起一套 AI 平台，而是在现有 `fw-plus-ai` 体系内增加一个 Java 8 兼容的智能问数核心 starter。

参考项目分工如下：SQLBot 参考智能问数产品能力和 Text-to-SQL 链路；Aiflowy 参考 Java 企业级 AI 应用平台、模型管理、工作流和知识库/RAG 边界；LangGraph4j 参考 Java 版 LangGraph 图编排、状态和检查点能力；Agents-Flex 与 Tinyflow 参考 Agent/流程编排思想。实现不直接搬入这些项目的运行时或品牌资源。

## 设计目标

- 新增 `fw-plus-ai-askdata-spring-boot-starter`，归入 `fw-plus-ai-boot-starters`。
- 提供 `AskDataService`，支持自然语言问题生成 SQL、可选执行 SQL、返回结构化结果和步骤日志。
- 支持多个模型配置、一个默认模型、请求级 `modelKey` 覆盖。
- 模型调用主要使用 LangChain4j，向量召回优先复用现有 `fw-plus-ai` 能力；兜底适配顺序为 Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其它实现。当前 Java 8 Starter 不引入 Spring AI 运行时依赖；图编排可评估 LangGraph4j JDK8 版本作为可选适配，但不引入 Java 17+ 的 LangGraph4j 主线模块。
- 提供轻量多 Agent 与子 Agent 流程抽象，但第一版不强依赖 Agents-Flex/Tinyflow。
- 支持自动上下文压缩和可恢复执行，应用异常退出或重启后可以从最近稳定检查点继续。
- 默认执行保守 SQL 安全策略：只读单语句、危险关键字拒绝、默认行数限制、可关闭执行。

## 非目标

- 不逐行翻译 SQLBot Python/FastAPI 代码。
- 不使用 SQLBot 品牌、Logo、路径或配置前缀。
- 不实现 SQLBot UI、用户体系、安装器、MCP Server 或 xpack 能力。
- 不实现 Magic-API `import askdata`，该能力属于后续 `add-askdata-magicapi-plugin`。
- 不做项目管理模块；后续插件只做向量管理模块。

## 模块布局

新增模块：

```text
fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/
  fw-plus-ai-askdata-spring-boot-starter/
    pom.xml
    src/main/java/com/xiongx9527/fw/plus/ai/askdata/
      AskDataService.java
      AskDataRequest.java
      AskDataResult.java
      AskDataProperties.java
      autoconfigure/AskDataAutoConfiguration.java
      model/
      metadata/
      prompt/
      vector/
      sql/
      flow/
      conversation/
    src/main/resources/META-INF/spring.factories
    src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
```

包名使用 `com.xiongx9527.fw.plus.ai.askdata`，配置前缀使用 `fw.plus.ai.askdata`。POM 继承 `fw-plus-ai-boot-starters`，依赖版本由 `fw-plus-ai-boot-dependencies` 管理。

## 核心接口

### AskDataService

```java
public interface AskDataService {
    AskDataResult ask(AskDataRequest request);
}
```

`AskDataRequest` 包含：

- `question`：自然语言问题，必填。
- `datasourceKey`：数据源标识，可由上层系统映射。
- `dialect`：数据库方言，可由元数据提供器补齐。
- `modelKey`：可选模型 key；为空时使用默认模型。
- `conversationId`：可选会话 ID，用于多轮问数、上下文压缩和崩溃恢复。
- `resumeToken`：可选恢复 token，用于从最近稳定检查点继续执行。
- `execute`：是否执行 SQL，默认受全局配置控制。
- `maxRows`：本次请求最大行数，可选。
- `options`：扩展参数，用于后续 Magic-API、向量召回或 Agent 流程传参。

`AskDataResult` 包含：

- `success`、`errorCode`、`errorMessage`。
- `sql`、`tables`、`chartType`、`brief`。
- `columns`、`rows`、`rowCount`。
- `steps`：元数据、Prompt、模型、校验、执行等步骤日志。
- `conversationId`、`resumeToken`、`checkpointId`：用于上层继续会话或恢复流程。
- `rawModelResponse`：可配置保留，用于调试。

## 多模型设计

当前 `LangChain4jModelInfo` 已通过 Spring 注入多个模型类型的 Map，例如 chat、streaming chat、embedding、language、image。但它更偏 Magic-API 插件内部注册，缺少默认模型 key 和按用途选择模型的服务接口。

新增抽象：

```java
public interface AskDataModelRegistry {
    AskDataChatModel getChatModel(String modelKey);
    AskDataEmbeddingModel getEmbeddingModel(String modelKey);
    String getDefaultChatModelKey();
    String getDefaultEmbeddingModelKey();
}
```

默认实现优先适配现有 `LangChain4jModelInfo` 和 LangChain4j 模型 Bean。Spring AI、Agents-Flex、EasyAi 或其他实现只作为可选适配来源，技术顺序为 Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其它实现；当前 Java 8 Starter 不引入 Spring AI 运行时依赖。LangGraph4j 主线适合未来 Java 17+ 线路；当前只允许评估 `langgraph4j-core-jdk8` 这类兼容构件作为可选图编排适配。配置层支持：

```yaml
fw:
  plus:
    ai:
      askdata:
        model:
          default-chat-model: default
          default-embedding-model: embedding-default
```

规则：

- 请求指定 `modelKey` 时必须命中对应模型，否则返回模型配置错误。
- 请求未指定 `modelKey` 时使用默认模型。
- 默认模型缺失时返回明确错误。
- 旧单模型配置可以迁移为一个模型条目，并将该 key 配为默认模型。

## 向量复用设计

Milvus、Qdrant、Weaviate 现有模块已经能构造 LangChain4j `EmbeddingStore`，但主要以 Magic-API 动态模块形式暴露。AskData 不直接依赖脚本模块，而是新增统一 Facade，并优先通过 LangChain4j embedding 和 `EmbeddingStore` 生态完成召回：

```java
public interface AskDataVectorRetriever {
    List<AskDataRetrievedDocument> retrieve(AskDataVectorQuery query);
}
```

`AskDataVectorQuery` 包含召回类型、数据源 key、向量存储 key、问题文本、topK、过滤条件等。默认实现可以先为空实现或基于已存在的 `EmbeddingStore` Bean 适配；当 Milvus/Qdrant/Weaviate 插件只提供脚本模块时，构建阶段应将“构造 EmbeddingStore”能力上移为可复用 Java 服务。

召回结果只作为 Prompt 上下文片段输入，不直接改变 SQL 执行范围。第一版召回类型建议覆盖：表结构、术语、训练样例。

## Prompt 与方言

`AskDataPromptTemplateProvider` 负责构造系统提示词和用户提示词。模板参考 SQLBot 的规则，但统一使用 AskData 命名，并保持可配置。

`AskDataDialect` 负责：

- 标识符引用规则。
- limit/分页规则。
- 默认只读查询提示。
- 方言名和版本展示。

默认支持 MySQL、PostgreSQL、ClickHouse 三类方言；其他方言先通过扩展 Bean 接入。

## SQL 安全与执行

`AskDataSqlValidator` 校验：

- SQL 非空。
- 单语句。
- 只允许 `SELECT` 或配置允许的只读查询形态。
- 拒绝 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE`、`CREATE`、`MERGE` 等危险关键字。
- 启用默认 limit 时，确保 SQL 包含行数限制或由执行器包裹限制。

第一版可以采用保守字符串与语句边界校验，避免贸然引入不确定依赖。若后续引入 SQL 解析库，必须确认 Java 8 和许可证兼容，并纳入 BOM 管理。

`AskDataSqlExecutor` 默认基于 JDBC，只执行查询并返回列名、列类型、行数据和行数。配置禁用执行时，仅返回生成 SQL 和步骤日志。

## 多 Agent 流程

新增轻量流程接口：

```java
public interface AskDataAgent {
    String getName();
    AskDataStepResult execute(AskDataStepContext context);
}

public interface AskDataFlowExecutor {
    AskDataResult execute(AskDataRequest request);
}
```

子 Agent 通过独立接口派发和归并：

```java
public interface AskDataSubAgent extends AskDataAgent {
    AskDataSubAgentResult executeSubTask(AskDataSubAgentRequest request);
}

public interface AskDataAgentDispatcher {
    AskDataDispatchResult dispatch(AskDataDispatchRequest request);
}
```

主 Agent 可以在步骤内部派发子 Agent，用于表召回、术语召回、训练样例召回、SQL 修复、结果解释等子任务。默认 dispatcher 使用同步顺序执行，记录每个子任务的耗时、超时、错误和输出摘要，并按子任务失败策略决定继续、降级或终止主流程。第一版不把并行调度作为默认行为，但接口保留超时和聚合结果字段，后续可接线程池、LangGraph4j JDK8、Agents-Flex、Tinyflow 或通用 AI 工作流引擎。

默认串行步骤：

1. `metadata`：获取表结构、术语和召回片段。
2. `prompt`：构造 Prompt。
3. `generate-sql`：调用模型生成 SQL JSON。
4. `validate-sql`：校验 SQL 安全和 limit。
5. `execute-sql`：按配置执行查询。
6. `format-result`：组装返回对象。

每个步骤都可以通过 Spring Bean 替换。Aiflowy、Agents-Flex、Tinyflow 的工作流思想作为后续扩展参考；本 Starter 第一版不强制依赖它们。

## 上下文压缩与恢复

多轮问数和多 Agent 流程会产生较长上下文。AskData Starter 需要把“可恢复状态”和“可压缩文本”分开处理：结构化字段必须持久化，历史对话、模型原始响应和中间摘要可以压缩。

新增接口：

```java
public interface AskDataConversationStore {
    AskDataCheckpoint save(AskDataCheckpoint checkpoint);
    AskDataCheckpoint loadLatest(String conversationId);
    AskDataCheckpoint loadByResumeToken(String resumeToken);
}

public interface AskDataContextCompressor {
    AskDataCompressedContext compress(AskDataStepContext context);
}
```

检查点至少保存：`conversationId`、`checkpointId`、`resumeToken`、请求摘要、模型 key、数据源 key、方言、已完成步骤、当前步骤状态、生成 SQL、SQL 校验结果、压缩摘要、步骤日志、错误码、版本号和创建时间。默认内存存储只用于开发和测试；生产场景通过 Spring Bean 替换为 JDBC、Redis 或业务自己的持久化实现。

恢复规则：

- 每个稳定步骤完成后写入检查点，包括元数据获取、SQL 生成、SQL 校验、SQL 执行和子 Agent 结果归并。
- 当上下文超过配置阈值时触发压缩，但模型 key、数据源 key、SQL、校验状态、步骤状态、错误码等结构化字段不得被摘要替代。
- 应用重启后，调用方携带 `conversationId` 或 `resumeToken` 时，从最近稳定检查点恢复，并从未完成的后续步骤继续执行。
- 检查点缺失、版本不兼容、状态不完整、或上一步 SQL 未通过安全校验时，返回 `RESUME_FAILED`，不得静默重新执行不确定步骤。
- 已执行 SQL 的检查点需要记录执行完成标记和结果摘要；恢复时默认不重复执行已完成查询，只继续格式化或返回已有结果。

## Java 17 后续演进

当前 change 面向 Java 8 和 Spring Boot 2.7.18，先沉淀 AskData 的稳定核心契约。后续 Java 17 / Spring Boot 3 版本可以独立新增 adapter starter，例如 Spring AI adapter、LangGraph4j 主线图编排 adapter 或更完整的工作流 adapter。

演进原则：

- Java 17 模块复用 `AskDataRequest`、`AskDataResult`、模型注册、Agent 流程和检查点模型，不重写调用方 API。
- 当前 Java 8 模块不引入 Spring AI 或 Java 17+ LangGraph4j 主线依赖。
- 新能力通过 Spring Bean、条件化自动配置或 adapter 模块接入，使业务可以按运行时版本选择实现。
- 如果 Java 17 模块引入更强图编排或状态恢复能力，也必须保持 SQL 安全、只读执行和恢复安全边界一致。

## 自动配置

`AskDataAutoConfiguration` 注册条件化 Bean：

- `AskDataProperties`
- `AskDataModelRegistry`
- `AskDataVectorRetriever`
- `AskDataMetadataProvider`
- `AskDataPromptTemplateProvider`
- `AskDataSqlValidator`
- `AskDataSqlExecutor`
- `AskDataFlowExecutor`
- `AskDataConversationStore`
- `AskDataContextCompressor`
- `AskDataService`

注册方式同时覆盖 Spring Boot 2.7 的 `spring.factories` 和 `AutoConfiguration.imports`，与现有 `fw-plus-ai-boot-autoconfigure` 风格保持一致。

## 错误处理

所有可预期失败返回 `AskDataResult.success=false`：

- `MODEL_NOT_FOUND`
- `DEFAULT_MODEL_NOT_CONFIGURED`
- `METADATA_NOT_FOUND`
- `MODEL_RESPONSE_INVALID`
- `SQL_REJECTED`
- `SQL_EXECUTION_FAILED`
- `RESUME_FAILED`
- `CHECKPOINT_NOT_FOUND`
- `CONTEXT_COMPRESSION_FAILED`

不可恢复的配置错误可以在启动时失败，但请求级错误应尽量进入结构化结果，方便 Magic-API 插件消费。

## 测试策略

- 模型选择测试：默认模型、指定模型、指定模型不存在、默认模型缺失。
- SQL 校验测试：危险语句、多语句、空 SQL、缺少 limit、只读查询通过。
- Prompt 测试：方言规则、schema、术语、训练样例片段组合。
- 流程测试：默认串行流程的步骤顺序、失败短路、步骤日志。
- 子 Agent 测试：子任务结果归并、超时记录、失败继续/降级/终止策略。
- 上下文测试：压缩阈值触发、结构化字段保留、检查点写入、通过 `conversationId` 或 `resumeToken` 恢复继续、检查点缺失和版本不兼容错误。
- 自动配置测试：默认 Bean 装配、自定义 Bean 替换、配置禁用执行。
- 向量 Facade 测试：使用 stub retriever 验证召回内容进入 Prompt。

## 实施顺序

1. 接入 Maven 模块和自动配置注册。
2. 定义核心 API、结果模型和错误码。
3. 实现多模型注册表适配。
4. 实现向量召回 Facade 和默认空实现。
5. 实现 Prompt、方言和 SQL 安全。
6. 实现默认 Agent Flow、子 Agent dispatcher 和 `AskDataService`。
7. 实现上下文压缩、检查点存储和恢复入口。
8. 补齐测试和 Maven Wrapper 验证。
9. 在文档和接口注释中标明 Java 17 / Spring Boot 3 adapter 的扩展边界。

## 风险与缓解

- 现有模型/向量模块偏 Magic-API 插件：通过 Java Facade 上移复用能力，主路径对接 LangChain4j，避免 Starter 直接依赖脚本模块；Spring AI 仅保留给后续 Spring Boot 3 / Java 17 版本适配。
- 多模型改造影响旧用法：保留默认模型语义，单模型配置迁移为一个默认条目。
- Text-to-SQL 安全风险：默认不执行危险 SQL，提供关闭执行开关和行数限制。
- 工作流范围扩大：第一版只定义轻量接口，不引入完整工作流平台。
- 上下文压缩丢失关键状态：结构化字段独立持久化，压缩器只能处理历史文本和可重放上下文。
- 崩溃恢复重复执行风险：检查点记录步骤完成状态和 SQL 校验结果；无法确认安全状态时返回恢复错误。
