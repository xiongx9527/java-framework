## 1. Maven 模块接入

- [x] 1.1 在 `fw-plus-ai-boot-starters` 下新增 `fw-plus-ai-askdata-spring-boot-starter` 模块。
- [x] 1.2 将新模块接入 AI starter 聚合 POM，并接入 `fw-plus-ai` 使用的依赖管理。
- [x] 1.3 确认新模块继承现有 Java 8、Spring Boot 2.7.18、`${revision}` 和 flatten 约定。

## 2. 核心 API 与模型

- [x] 2.1 定义 `AskDataService` 作为主要 Java API。
- [x] 2.2 定义请求和结果模型，覆盖问题输入、数据源上下文、生成 SQL、执行数据、步骤日志和错误信息。
- [x] 2.3 定义数据源元数据模型，覆盖方言、表、字段、注释、样例值、术语和自定义 Prompt 片段。

## 3. Prompt 与模型集成

- [x] 3.1 实现 Prompt 模板提供器，参考相关文档中的 SQL 生成规则，但统一使用 AskData 命名。
- [x] 3.2 增加数据库方言规则抽象，覆盖标识符引用和查询行数限制语法。
- [x] 3.3 梳理现有 `fw-plus-ai` 模型管理与 LangChain4j 模块，定义 AskData 所需的最小可复用模型调用适配器。
- [x] 3.4 扩展或适配当前模型配置，使其可以表达多个模型条目和一个默认模型。
- [x] 3.5 增加请求级模型 key 选择，并对指定模型缺失或默认模型缺失返回明确错误。
- [x] 3.6 梳理现有 Milvus、Qdrant、Weaviate 和 embedding 模块，定义 AskData 如何复用它们进行表结构、术语或训练样例召回。
- [x] 3.7 在现有 `fw-plus-ai` 能力足够时，实现基于现有能力的模型调用抽象和默认适配器。

## 4. SQL 安全与执行

- [x] 4.1 实现保守的 SQL 安全校验器，只允许只读单语句查询。
- [x] 4.2 实现可配置的行数限制策略和执行启用/禁用行为。
- [x] 4.3 实现只读 SQL 执行器抽象和默认 JDBC 结果映射。

## 5. 多 Agent 与子 Agent 流程

- [x] 5.1 定义 `AskDataAgent`、`AskDataSubAgent`、`AskDataAgentDispatcher`、步骤上下文/结果和流程执行器接口。
- [x] 5.2 实现默认串行流程，并拆分元数据、SQL 生成、校验和执行步骤。
- [x] 5.3 实现默认子 Agent 派发器，支持同步执行、结果归并、超时记录和失败策略。
- [x] 5.4 允许应用通过 Spring Bean 替换单个 Agent 或子 Agent，默认运行时不强制依赖 Agents-Flex 或 Tinyflow。

## 6. 上下文压缩与恢复

- [x] 6.1 定义 `AskDataConversationStore`、`AskDataContextCompressor`、`AskDataCheckpoint` 和 `AskDataResumeToken`。
- [x] 6.2 在流程关键步骤完成后写入检查点，记录会话 ID、请求摘要、模型 key、数据源 key、步骤状态、生成 SQL、压缩摘要和错误信息。
- [x] 6.3 实现默认上下文压缩策略，确保结构化字段不被摘要替代。
- [x] 6.4 实现恢复入口，通过 `conversationId` 或 `resumeToken` 从最近稳定检查点继续执行。
- [x] 6.5 对恢复失败、检查点缺失、版本不兼容和不安全 SQL 场景返回明确错误。

## 7. Spring Boot 自动配置

- [x] 7.1 增加 `AskDataProperties`，配置前缀为 `fw.plus.ai.askdata`。
- [x] 7.2 增加自动配置，为默认提供器、校验器、执行器、上下文存储、压缩器和服务注册条件化 Bean。
- [x] 7.3 使用 Spring Boot 2.7 兼容的元数据注册自动配置。

## 8. 测试与验证

- [x] 8.1 增加 SQL 校验器单元测试，覆盖危险语句拒绝和查询限制行为。
- [x] 8.2 增加 Prompt 构造和方言规则选择单元测试。
- [x] 8.3 增加默认模型选择、指定模型选择和模型缺失错误的单元测试。
- [x] 8.4 增加 Spring 上下文测试，覆盖自动配置默认 Bean 和 Bean 替换。
- [x] 8.5 增加子 Agent 结果归并、超时、失败策略测试。
- [x] 8.6 增加上下文压缩、检查点写入、恢复继续和恢复失败测试。
- [ ] 8.7 使用相关 Maven Wrapper 构建新模块和聚合模块。
