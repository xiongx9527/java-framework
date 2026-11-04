# fw-plus-ai-askdata

面向「数据问答 / 自然语言转 SQL（NL2SQL）」的 AI 能力模块，封装在 `fw-plus-ai-base` 的通用端口之上。

## 定位

- 纯能力封装，**不绑定**任何 LLM / 向量库 / 数据库实现（这些通过 `fw-plus-ai-base`、`fw-plus-ai-support` 的端口注入）。
- 既可作为独立能力内联调用，也可作为 `AiServices` 的 `ToolProvider` 暴露给模型。

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

`DefaultAskDataCapability.ask(AskDataInput)` 串起 **召回 → 生成 SQL → 执行**：

```java
AskDataResult result = askData.ask(AskDataInput.builder()
        .question("上个月销售额最高的 5 个商品")
        .candidates(schemaContext.getTables())   // 可选：预筛选候选表
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
