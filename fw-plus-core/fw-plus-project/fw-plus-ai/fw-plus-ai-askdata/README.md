# fw-plus-ai-askdata

面向「数据问答 / 自然语言转 SQL（NL2SQL）」的 AI 能力模块，封装在 `fw-plus-ai-base` 的通用端口之上。

## 定位

- 纯能力封装，**不绑定**任何 LLM / 向量库 / 数据库实现（这些通过 `fw-plus-ai-base`、`fw-plus-ai-support` 的端口注入）。
- 既可作为独立能力内联调用，也可作为 `AiServices` 的 `ToolProvider` 暴露给模型。

## 业务侧需要注入什么

`fw-plus-ai-askdata` 提供通用编排和默认实现，不负责选择业务数据源、识别当前用户权限或替业务注册具体模型。业务侧按使用场景提供以下依赖：

| 依赖 | 是否必需 | 业务侧职责 / 模块提供的实现 |
| --- | --- | --- |
| `LlmPort` | SQL 生成必需 | 接到业务选择的模型；通常由 `fw-plus-ai-langchain4j-*` 等适配模块提供，也可自行实现 `fw-plus-ai-base` 端口。 |
| `SqlExecutorPort` 或 `DataSource` | SQL 执行必需 | 选择可查询的数据源并落实租户路由。固定数据源可直接使用 `new JdbcSqlExecutor(dataSource)`；动态数据源/非 JDBC 后端由业务提供 `SqlExecutorPort` 实现。 |
| `SchemaCandidateProvider` | 端到端问数必需 | 从业务元数据中构造 `TableSchema`，并且每次根据可信的当前用户、租户、权限和数据源上下文，只返回允许访问的表及字段。`DefaultAskDataCapability` 和 `AskDataRetriever` 共用该授权入口。 |
| `Retriever<Document>`（向量、词法）及可选 `Reranker<Document>` | 采用检索式 schema 召回时按策略需要 | 复用 `fw-plus-ai-base` / `fw-plus-ai-support` 检索端口及相应存储适配；经济型需要词法检索器，质量型可组合向量、词法和重排。 |
| `ToolArgumentCodec` | 通过 `ToolProvider` 暴露工具时必需 | 可注入 `fw-plus-ai-support` 的 `JacksonToolArgumentCodec`；也可按应用的 JSON 组件自行实现。 |
| 图表生成调用 | 仅需要图表时 | 不参与 `DefaultAskDataCapability` 主流程；调用方在拿到 `QueryResult` 后独立调用 `ChartGenerationCapability`。 |

最小的 SQL 问答装配如下（省略具体模型适配和 `DataSource` 配置）：

```java
SqlGenerationCapability generation = SqlGenerationCapabilityBuilder.builder()
        .llm(llmPort)
        .build();

SqlExecutionCapability execution = SqlExecutionCapabilityBuilder.builder()
        .executor(new JdbcSqlExecutor(dataSource))
        .build();

// 每次调用都从可信业务上下文中解析当前用户可访问的 schema。
SchemaCandidateProvider candidateProvider = query -> schemaService.findVisibleTables(query.getText());

// 不启用检索召回时，授权候选表直接交给 SQL 生成。
AskDataCapability askData = new DefaultAskDataCapability(candidateProvider, null, generation, execution);

// 启用 schema 检索时，先由 candidateProvider 限定授权范围，再在该范围内召回。
SchemaRecallCapability recall = EngineSchemaRecallCapability.forStrategy(
        IndexMode.ECONOMICAL, null, lexicalRetriever, null, 10, 8, 0.0);
AskDataCapability askDataWithRecall = new DefaultAskDataCapability(candidateProvider, recall, generation, execution);
```

若应用只需业务代码直接调用 `askData.ask(input)`，不需要创建 `ToolArgumentCodec` 或 `AskDataToolProvider`。若只需 SQL 生成或执行，也可分别装配 `SqlGenerationCapability` / `SqlExecutionCapability`，无需创建端到端 `AskDataCapability`。

通过 AiServices 同时提供 schema 检索上下文和问数工具：

```java
AskDataRetriever retriever = AskDataRetriever.forStrategy(
        candidateProvider, indexMode, vectorRetriever, lexicalRetriever, reranker);
AskDataToolProvider askDataTool = new AskDataToolProvider(askDataWithRecall, toolArgumentCodec);

Assistant assistant = AiServices.builder(Assistant.class)
        .llm(llmPort)
        .retriever(retriever)
        .toolProvider(askDataTool)
        .build();
```

`Retriever` 注入的是模型可见的 schema 上下文；工具执行时 `DefaultAskDataCapability` 会再次调用 `SchemaCandidateProvider` 并在授权候选范围内召回，不能依赖检索结果自动传入工具。

### 权限与安全边界

- 用户身份、租户、数据权限和动态数据源路由必须由业务侧可信上下文决定，不能由模型参数决定。`SchemaCandidateProvider` 是问数编排和 Retriever 共用的表结构授权边界；它不替代数据库本身的行级/列级权限。
- `readOnly` 和 SQL 校验是通用防护，不等于完整 SQL 安全沙箱。应使用只读数据库账号、设置正数的行数/超时上限，并在业务侧限制可访问的数据源。
- `AskDataToolProvider` 仅接受自然语言问题，不接受模型生成的 schema、授权信息、只读开关、最大行数或超时策略。执行策略由业务装配的能力及可信请求上下文控制。
- `AiServices` 中注册检索器与注册工具是两件事：前者为模型补充 schema 上下文，后者触发完整问数能力。工具执行会独立重新取得授权候选表。

## 核心能力

| 能力 | 端口 / 实现 | 说明 |
| --- | --- | --- |
| SQL 生成 | `SqlGenerationCapability` / `DefaultSqlGenerationCapability` | 含本地校验、LLM 校验、置信度评估、自动修复（`SqlRepairPolicy`）与 fail-closed |
| SQL 执行 | `SqlExecutionCapability` / `DefaultSqlExecutionCapability` | 提供 `JdbcSqlExecutor`（默认 JDBC），含只读保护、超时、结果自检（`DefaultResultVerifier`） |
| 表结构召回 | `SchemaRecallCapability` / `EngineSchemaRecallCapability` | 基于通用检索契约，支持**经济型 / 质量型**两套索引策略 |
| 图表生成 | `ChartGenerationCapability` / `DefaultChartGenerationCapability` | 类型推断、配置校验、降级策略 |
| 方言适配 | `DialectHelper` / `DialectSqlChecker` | 方言感知的 SQL 检查与改写 |
| 后处理 | `ConfidenceEstimator` / `SqlExtractor` / `SqlValidator` / `SqlRepairPolicy` | 抽取、校验、修复、置信度 |
| LLM 韧性 | `ResilientLlmPort` / `StaticLlmPort` | 重试 / 降级 |

## 端到端编排

`DefaultAskDataCapability.ask(AskDataInput)` 串起 **授权候选表 → 可选召回 → 生成 SQL → 执行**。候选表始终由 `SchemaCandidateProvider` 获取；图表生成不在此调用链内：

```java
AskDataResult result = askData.ask(AskDataInput.builder()
        .question("上个月销售额最高的 5 个商品")
        .recallIndexMode(IndexMode.ECONOMICAL)    // 经济型 / 质量型召回
        .build());
```

`AskDataResult` 携带 `sql`、`confidence`、`queryResult`、`valid`、`warnings`、`alternatives` 等，由调用方决定展示或让模型追问。

## 三个工具（ToolProvider）

| 工具 | 类 | 用途 |
| --- | --- | --- |
| `askdata.ask_data` | `AskDataToolProvider` → `DefaultAskDataCapability` | 端到端问答 |
| `askdata.sql_gen` | `SqlGenToolProvider` → `SqlGenerationCapability` | 仅生成 SQL |
| `askdata.sql_exec` | `SqlExecToolProvider` → `SqlExecutionCapability` | 仅执行 SQL |

工具描述保持英文（面向模型），参数校验失败会回灌英文错误，供模型自我纠正。

## 经济型 / 质量型召回（可配置）

召回策略由 `IndexMode` 决定，对齐 Dify：

- **HIGH_QUALITY（质量型）**：向量 + 倒排，支持语义 / 全文 / 混合检索与重排。
- **ECONOMICAL（经济型）**：只有全文索引，只走词法、不接重排。

两种装配方式：

1. **构造期固定**（适合全局统一策略）：

   ```java
   SchemaRecallCapability recall = EngineSchemaRecallCapability.forStrategy(
       IndexMode.ECONOMICAL, vector, lexical, reranker, 10, 8, 0.0);
   ```

2. **请求级可配置**（适合 per-ask 切换）：用组件式构造函数注入向量 / 词法检索器与重排器，
   由 `AskDataInput.recallIndexMode` 在每次 `ask()` 时选择链路：

   ```java
   SchemaRecallCapability recall = new EngineSchemaRecallCapability(
       vector, lexical, reranker, 10, 8, 0.0);
   // 调用时：AskDataInput.builder().recallIndexMode(IndexMode.ECONOMICAL).build()
   ```

   若 `recallIndexMode` 为 `null`，默认按 **HIGH_QUALITY** 处理。

## 工具参数校验（@P）与错误提示

`fw-plus-ai-base` 的 `@P` 同时声明「给模型的说明」与「执行期约束」：

| 属性 | 作用 |
| --- | --- |
| `required` | 缺参抛 `ToolArgumentException`（路由到参数错误处理器） |
| `pattern` | 字符串正则不匹配抛异常 |
| `minLength` / `maxLength` | 长度范围（含边界） |
| `min` / `max` | 数值范围（字符串化后解析为数字，含边界） |
| `jsonShape` | `object` / `array` 的结构校验（轻量括号配对扫描，跳过字符串内容） |
| `format` | 仅写入 schema 的 `format` 字段提示模型，执行期不强制 |
| `message` | **自定义错误提示**（面向模型，英文优先），非空时替代默认英文描述 |

校验失败时抛出 `ToolArgumentException`，它携带：

- `getMessage()`：面向模型的英文描述（回灌模型）；
- `getErrorKey()` + `getArgs()`：**稳定的 i18n key**（如 `tool.arg.required`、`tool.arg.pattern`、`tool.arg.minLength`、
  `tool.arg.max`、`tool.arg.jsonObject`/`jsonArray`、`tool.arg.decode`、`tool.arg.unknownTool`/`missingName`），
  供 UI 层通过 `I18nMessageResolver` 本地化，遵循「后端返回 key + 原始 reason，不硬编码本地化文案」约定。

## i18n key 注册

框架只返回 key，文案由**消费应用**的消息包解析。需在应用侧 bundle 注册下列 key（否则回退为 key 原文）：

- `tool.arg.*`（见 `ToolArgumentException` 中的 `KEY_*` 常量）
- `askdata.contract.*` / `askdata.input.*` / `askdata.exec.*`（已在 askdata 内部使用）

## 构建

```bash
# JDK 8（zulu-8）
export JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
mvn -Drevision=0.1.1-SNAPSHOT -pl fw-plus-ai-askdata -am clean package
```

> 模块级 `lombok.config` 将生成的 builder 命名为 `Builder`（而非默认的 `XxxBuilder`），以对齐
> `fw-plus-ai-base` 手写 `Builder` 的约定；该配置限定在本模块，避免影响 `fw-plus-ai-langchain4j-*` 等自带
> 嵌套 `Builder` 的源码拷贝。
