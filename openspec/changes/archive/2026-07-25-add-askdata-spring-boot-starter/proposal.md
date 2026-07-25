## Why

fw-plus 目前已有 LangChain4j、Magic-API AI 插件和多种数据库扩展，但缺少一个面向业务系统复用的 Java 版智能问数核心能力。用户希望参考 SQLBot 的智能问数产品能力，并结合 Aiflowy、Agents-Flex、Tinyflow 的 Java AI/流程编排思路，在 Java 8 和 Spring Boot 2.7.18 约束下形成 fw-plus 自己的 AskData Starter。

本 change 先交付独立的 `fw-plus-ai-askdata-spring-boot-starter`，为后续 Magic-API AI 插件提供稳定核心服务，避免把脚本插件和问数引擎耦合在一起。

## What Changes

- 新增 `fw-plus-ai-askdata-spring-boot-starter` 模块，归入 `fw-plus-ai-boot-starters` 聚合。
- 新增 AskData 自动配置与配置属性，使用 `fw.plus.ai.askdata` 作为配置前缀。
- 提供 Java 服务接口，支持自然语言问题生成只读 SQL、执行查询并返回结构化结果。
- 改造或适配现有模型配置能力，使 AskData 支持多个模型配置、默认模型和按请求指定模型。
- 模型调用技术顺序为 LangChain4j、Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其他实现；当前 Java 8 Starter 主路径只落地 LangChain4j，Spring AI 不作为本模块运行时依赖。Agent 图编排可评估 LangGraph4j JDK8 版本作为可选适配，但不得引入要求 Java 17+ 的 LangGraph4j 1.8.x 或 Spring AI 相关模块。
- 提供数据源元数据抽象，用于描述表、字段、注释、样例值和数据库方言。
- 提供 Prompt 模板与数据库方言规则扩展点，支持不同数据库的标识符引用、limit 语法和只读查询约束。
- 提供 SQL 安全校验，拒绝非查询语句和危险语句，并支持默认查询行数限制。
- 提供多 agent 与子 agent 协作扩展点，使选表、生成 SQL、SQL 校验、执行和分析等步骤可以由主 Agent、独立 Agent 或子 Agent 协同完成。
- 提供可压缩、可持久化的问数上下文，使长流程可以保存关键状态、压缩历史上下文，并在应用异常退出或重启后继续操作。
- 提供执行日志/步骤结果模型，便于上层插件记录选表、生成 SQL、校验、执行等过程。
- 不使用 `SQLBot` 品牌名，不迁移 SQLBot UI、安装器、用户体系或受品牌约束的资源。

## Capabilities

### New Capabilities

- `askdata-query-engine`: Java 8 版 AskData 智能问数核心引擎，覆盖配置、多模型选择、数据源元数据、Text-to-SQL、多 agent/子 agent 协作扩展、上下文压缩与崩溃恢复、SQL 安全校验、只读执行和结构化结果返回。

### Modified Capabilities

- 无。

## Impact

- 影响 Maven 模块：
  - `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/pom.xml`
  - 新增 `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter/`
  - 视现有 BOM 管理方式更新 `fw-plus-ai-boot-dependencies` 或相关父 POM。
- 影响 Spring Boot 自动配置：
  - 新增 AskData 自动配置类和 `spring.factories` 注册。
  - 生成配置元数据。
- 影响依赖：
  - 优先复用仓库已有 LangChain4j 依赖、模型配置能力和 JDBC 基础能力；当前只能配置单模型时，需要扩展为多模型配置。
  - 技术兜底顺序为 Spring AI（仅未来 Spring Boot 3 / Java 17 版本）、Agents-Flex、其他实现；当前 Java 8 Starter 不引入 Spring AI 运行时依赖。LangGraph4j 仅可评估 JDK8 兼容版本作为可选图编排适配，不能使用 Java 17+ 主线模块。
  - 如需 SQL 解析库，必须选择 Java 8 兼容且许可证可接受的依赖，或先实现保守只读校验。
- 后续 change `add-askdata-magicapi-plugin` 将依赖本 change 暴露的服务接口。
