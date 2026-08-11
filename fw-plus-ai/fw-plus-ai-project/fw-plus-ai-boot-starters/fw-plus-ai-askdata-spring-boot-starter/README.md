# fw-plus-ai-askdata-spring-boot-starter

## 当前支持状态

本 starter 提供 AskData 的 Spring Boot 2.7.x / Java 8 自动配置。业务配置使用 `fw.plus.ai.askdata`，模型和向量能力统一解析中立注册表：聊天/embedding 模型 key 解析到 `AiModelRegistry`（来自 `fw-plus-ai-model`），向量 store key 解析到 `AiEmbeddingStoreRegistry`（来自 `fw-plus-ai-store-embedding`）。AskData 业务代码不应把 LangChain4j 原生模型或 store 当作主接口。

## Maven 依赖

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-askdata-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

当前 starter 不默认带入任何具体 AI 框架 Adapter。AskData 的 key、业务流程和覆盖点保持中立；业务应用按所选 AI 框架显式组合对应的 Adapter Starter 与 provider SDK。

## 中立模型与向量配置

`model.default-chat-model`、`model.default-embedding-model` 是默认注册表 key；候选列表会优先选择已在 `AiModelRegistry` 中注册且类型匹配的 key。`vector.store` 解析到 `AiEmbeddingStoreRegistry` 的 store key。

默认模型桥接只接受同时实现 `AiModel` 与 `AskDataChatModel` / `AskDataEmbeddingModel` 的中立实现，不反射或转换具体 SDK 类型。LangChain4j 模型的转换由应用显式引入 Adapter Starter 后启用的可选适配配置负责。

```yaml
fw:
  plus:
    ai:
      askdata:
        enabled: true
        model:
          default-chat-model: chat-default
          default-embedding-model: embedding-default
          chat-candidates:
            - chat-default
          embedding-candidates:
            - embedding-default
          fallback-enabled: true
          request-timeout-millis: 30000
          infrastructure-failure-timeout-millis: 30000
        vector:
          store: knowledge
          default-top-k: 5
        sql:
          execute-enabled: true
          read-only-connection-required: true
          default-max-rows: 100
          query-timeout-seconds: 30
        flow:
          multi-question-max-concurrency: 4
          multi-question-timeout-millis: 30000
```

`request-timeout-millis` 是传给 Core Chat/Embedding Adapter 的单次调用超时；`infrastructure-failure-timeout-millis` 只用于旧 LangChain4j 兼容层按调用耗时识别基础设施故障，不能替代 provider HTTP 客户端超时。两者默认均为 30000 毫秒，但语义独立。

默认 Flow 的 Chat 与 Embedding 都通过 Core Router 选择中立模型。请求需要独立指定 Embedding 模型时，在 `AskDataRequest.options` 中设置 `embeddingModelKey`；未设置时按 `embedding-candidates` 和 `default-embedding-model` 路由。Flow 会向每次向量检索加入 `projectId`、`operatorId`、`datasourceKey` 和 `authorizationVersion` 过滤条件，因此所选 `AiEmbeddingStore` 必须返回 `supportsFilters() == true` 并实际应用 filters。无法应用 filters 的 Store 会 fail-closed；旧 LangChain4j `EmbeddingStore` 兼容适配器也会明确拒绝带授权过滤的查询。

Chat 与 Embedding 只有可重试基础设施故障允许切换候选。返回结果和检查点分别通过 `actualModelKey` / `modelAttempts`、`actualEmbeddingModelKey` / `embeddingModelAttempts` 保存最终实际 key 与尝试顺序；监控事件使用 `modelType=CHAT|EMBEDDING`，并记录路由层级、失败码、切换原因和最终状态。自定义 `AskDataVectorRetriever` 可继续只实现 `retrieve`；需要提供 Embedding 路由审计时覆盖兼容的 `retrieveWithResult`。

## 多问题 DAG 与恢复

`AskDataRequest.options.questions` 可以是字符串列表，也可以是结构化子问题列表。字符串列表按声明顺序生成 `question-1`、`question-2` 等稳定 ID；结构化列表显式提供 `id`、`question`、`dependsOn`、`failurePolicy` 和可选的 `timeoutMillis`：

```java
Map<String, Object> first = new LinkedHashMap<String, Object>();
first.put("id", "orders");
first.put("question", "订单数量");
first.put("dependsOn", Collections.emptyList());

Map<String, Object> summary = new LinkedHashMap<String, Object>();
summary.put("id", "summary");
summary.put("question", "汇总订单结论");
summary.put("dependsOn", Collections.singletonList("orders"));
summary.put("failurePolicy", "SKIP");
summary.put("timeoutMillis", Long.valueOf(5000L));

request.setOptions(Collections.<String, Object>singletonMap(
        "questions", Arrays.asList(first, summary)));
```

无依赖节点在 `multi-question-max-concurrency` 上限内并发执行，默认值为 `4`；整个计划受 `multi-question-timeout-millis` 约束，默认值为 `30000` 毫秒。`failurePolicy=SKIP` 会在任一依赖失败时跳过当前节点，`CONTINUE` 则允许节点在依赖已终态后继续。每个 `childResults` 条目携带 `questionId`、`dependsOn` 和独立 `questionState`；终态固定为 `SUCCEEDED`、`FAILED`、`TIMED_OUT`、`CANCELLED` 或 `SKIPPED`，失败结果使用对应的稳定错误码。

配置 `AskDataConversationStore` 后，Flow 会在计划开始及每个子问题进入终态时保存父检查点，并使用 `<parentConversationId>:<questionId>` 作为稳定子会话 ID。恢复请求可传父 `resumeToken` 或 `conversationId`；已终态节点直接从父检查点恢复，未终态节点从其最新子检查点继续，因此已完成的 SQL 不会重放。恢复时请求身份、检查点版本和完整 DAG 快照必须一致，否则 fail-closed。集群部署必须提供共享会话存储；默认内存实现只适用于单实例，进程退出后不可恢复。

## SQL 阶段化插件

Core SQL 管线按 `ASSEMBLE`、`BIND_PARAMETERS`、`REWRITE`、`PREPARE` 的固定阶段顺序处理不可变 `AskDataSqlCommand`；同一阶段按 `order` 升序、能力 key 字典序执行。旧 `AskDataSqlInterceptor` 默认属于 `REWRITE`，无需修改即可继续使用。命令插件结束后，Flow 会重新注入治理行级谓词，再执行 Boot 方言/元数据校验和 Core 最终安全校验；校验成功后才进入只读执行。`RESULT_MAP` 只在执行完成且执行检查点已保存后运行。

SQL 插件同时是 `SQL_PLUGIN` 能力 Provider。Spring Bean 会被能力注册表检查重复 key 和 Core 版本，并被自动加入插件链。下面的插件在参数绑定阶段增加组织条件和具名参数：

```java
@Bean
public AskDataSqlInterceptor organizationScopePlugin() {
    return new AskDataSqlInterceptor() {
        public String getKey() {
            return "organization-scope";
        }

        public int getOrder() {
            return 100;
        }

        public AskDataSqlPhase getPhase() {
            return AskDataSqlPhase.BIND_PARAMETERS;
        }

        public AskDataSqlCommand intercept(AskDataSqlCommand command) {
            return command.withSql(command.getSql() + " AND organization_id = :organizationId")
                    .withParameter(AskDataSqlParameter.of("organizationId", "organization-1"));
        }
    };
}
```

插件可以新增命名参数；已有参数必须完整保留。同名同值追加是幂等操作，同名不同值、删除参数、SQL 中存在未绑定参数、扩大授权对象或弱化治理摘要都会立即拒绝。最终 JDBC 执行器只从校验后的 `AskDataSqlCommand` 绑定参数，不从插件取得连接。`AskDataSqlAudit` 记录能力 key、阶段、成功/失败、SQL 是否发生改写、参数名和参数数量，不记录 SQL 参数值。

结果映射实现 `AskDataSqlResultMapper`：

```java
@Bean
public AskDataSqlResultMapper businessResultMapper() {
    return new AskDataSqlResultMapper() {
        public String getKey() {
            return "business-result";
        }

        public int getOrder() {
            return 100;
        }

        public AskDataSqlResult map(AskDataSqlCommand command, AskDataSqlResult result) {
            return result.withRows(mapRows(result.getRows()));
        }
    };
}
```

映射器只能替换结构化列和行，不能改变执行终态、实际执行行数、`default-max-rows` 行数上限或 `query-timeout-seconds` 超时事实，也不能扩充超过实际读取行数的结果。违反约束时审计记录插件 key 与 `RESULT_MAP` 失败阶段，问数结果返回失败，但已完成 SQL 不会重放。

## 业务 Agent 步骤替换

业务可以注册 Core `AskDataAgent` Bean，按稳定步骤 key 替换默认 Flow 的单个业务步骤：

| 步骤 key | 类型化输出 | 未注册时的默认行为 |
| --- | --- | --- |
| `select-tables` | `selectedTables(...)` | 使用治理过滤后的全部授权表 |
| `generate-sql` | `generatedSql(...)` | 通过 Core Router 调用聊天模型并解析 SQL |
| `validate-sql` | `validatedSql(...)` | 使用默认 SQL 校验管线 |
| `execute-sql` | `executedSql(...)` | 使用默认只读 JDBC 执行器 |
| `interpret-result` | `interpreted(...)` | 保留模型返回的默认 `brief` |

`AskDataAgentStepContext` 携带项目与操作人、问题、数据源、会话、治理后元数据、已选表、Prompt、SQL 命令、结构化 SQL 结果、行数上限和超时。Agent 通过 `getPriority()` 声明优先级；同一步骤选择唯一最高优先级实现，最高优先级并列时应用启动失败并列出冲突 Agent key 与实现类。未声明步骤 key 的旧 `AskDataAgent` 标记实现不会替换默认步骤。

下面的 Java 8 示例只替换 SQL 生成，默认模型不会再次调用，后续默认校验和执行继续运行：

```java
@Bean
public AskDataAgent businessSqlGenerator() {
    return new AskDataAgent() {
        public String getKey() {
            return "business-sql-generator";
        }

        public String getStepKey() {
            return AskDataAgentStepKey.GENERATE_SQL;
        }

        public int getPriority() {
            return 100;
        }

        public AskDataAgentStepResult execute(AskDataAgentStepContext context) {
            return AskDataAgentStepResult.generatedSql("select count(*) from orders");
        }
    };
}
```

步骤替换不改变安全边界：`select-tables` 只能从治理后授权表中收窄；`validate-sql` 返回的命令仍须保留项目、操作人、参数和授权边界，并再次经过治理、方言/元数据及 Core 最终校验；`execute-sql` 必须返回与上下文相同的行数上限和超时事实，且结果不得超限；结果映射仍在执行完成检查点后运行。Agent 返回失败、空结果或越界结果时 Flow fail-closed。

需要 LangChain4j 时，由业务应用显式引入 Adapter Starter：

```xml
<dependency>
    <groupId>com.xiongx9527.fw.plus.ai</groupId>
    <artifactId>fw-plus-ai-langchain4j-adapter-spring-boot-starter</artifactId>
    <version>${fw-plus-ai.version}</version>
</dependency>
```

具体 LangChain4j provider SDK 也由业务按需显式加入。例如 OpenAI 兼容模型：

```xml
<dependency>
    <groupId>dev.langchain4j</groupId>
    <artifactId>langchain4j-open-ai</artifactId>
    <version>${langchain4j.version}</version>
</dependency>
```

然后通过 `fw.plus.ai.model` 和 `fw.plus.ai.embedding-store` 定义同名中立 key。AskData 只消费注册表，不要求业务 Bean 暴露 LangChain4j 原生类型。

## 业务 Bean 覆盖点

自动配置使用 `@ConditionalOnMissingBean`，业务可以按中立契约覆盖默认行为：

- 提供自定义 `AskDataModelRegistry`，覆盖模型 key 解析、聊天模型和 embedding 模型适配。
- 提供自定义 `AskDataVectorRetriever`，覆盖向量查询、召回和重排策略；默认实现通过 `AiEmbeddingStoreRegistry` 查找 `vector.store`。
- 提供自定义 `AskDataChatModel`、`AskDataEmbeddingModel` 或其他 AskData SPI Bean，覆盖具体业务模型能力。
- 直接提供 `AiModelRegistry` / `AiEmbeddingStoreRegistry`，让 AskData 的默认桥接从中立注册表读取能力。

示例：

```java
@Bean
public AskDataModelRegistry askDataModelRegistry(AiModelRegistry models) {
    return new MyNeutralAskDataModelRegistry(models, "chat-default", "embedding-default");
}

@Bean
public AskDataVectorRetriever askDataVectorRetriever(AiEmbeddingStoreRegistry stores,
        AskDataModelRegistry models) {
    return new MyNeutralVectorRetriever(stores, models, "knowledge");
}
```

覆盖 `AskDataModelRegistry` 或 `AskDataVectorRetriever` 后，应用负责保证 key、模型类型、向量维度和后端 schema 一致。缺少中立注册表或对应 key 时，默认向量检索不会伪造可用结果，应在部署检查中确认 adapter、provider SDK 和后端连接均已就绪。

## 旧坐标边界

当前 starter line 不使用旧的 `fw-plus-ai-langchain4j-model-spring-boot-starter` 或 `fw-plus-ai-langchain4j-store-embeding-spring-boot-starter` 坐标。它们不是当前主路径，也不应在新应用中与中立 starter 混用；请使用本 starter、`fw-plus-ai-model` / `fw-plus-ai-store-embedding` 中立注册表，并在应用侧按需组合 `fw-plus-ai-langchain4j-adapter-spring-boot-starter`。
