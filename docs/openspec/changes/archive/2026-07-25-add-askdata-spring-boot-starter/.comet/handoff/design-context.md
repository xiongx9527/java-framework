# Comet Design Handoff

- Change: add-askdata-spring-boot-starter
- Phase: design
- Mode: compact
- Context hash: 0f8f0d25e7ebba62e59588dd584e4980b600a638abad33fc7edda0e519450072

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-askdata-spring-boot-starter/proposal.md

- Source: openspec/changes/add-askdata-spring-boot-starter/proposal.md
- Lines: 1-43
- SHA256: 143575daee36a65aeecbf63d46c73a4297fff1535f81cbac7be5dd9e97f79902

```md
## Why

fw-plus 目前已有 LangChain4j、Magic-API AI 插件和多种数据库扩展，但缺少一个面向业务系统复用的 Java 版智能问数核心能力。用户希望参考 SQLBot 的智能问数产品能力，并结合 Aiflowy、Agents-Flex、Tinyflow 的 Java AI/流程编排思路，在 Java 8 和 Spring Boot 2.7.18 约束下形成 fw-plus 自己的 AskData Starter。

本 change 先交付独立的 `fw-plus-ai-askdata-spring-boot-starter`，为后续 Magic-API AI 插件提供稳定核心服务，避免把脚本插件和问数引擎耦合在一起。

## What Changes

- 新增 `fw-plus-ai-askdata-spring-boot-starter` 模块，归入 `fw-plus-ai-boot-starters` 聚合。
- 新增 AskData 自动配置与配置属性，使用 `fw.plus.ai.askdata` 作为配置前缀。
- 提供 Java 服务接口，支持自然语言问题生成只读 SQL、执行查询并返回结构化结果。
- 改造或适配现有模型配置能力，使 AskData 支持多个模型配置、默认模型和按请求指定模型。
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
  - 如需 SQL 解析库，必须选择 Java 8 兼容且许可证可接受的依赖，或先实现保守只读校验。
- 后续 change `add-askdata-magicapi-plugin` 将依赖本 change 暴露的服务接口。

```

## openspec/changes/add-askdata-spring-boot-starter/design.md

- Source: openspec/changes/add-askdata-spring-boot-starter/design.md
- Lines: 1-133
- SHA256: ea1d1ee70c61b9cef7ada933a6482783f1805d7053a0cfa36c3759fed1648ef7

[TRUNCATED]

```md
## Context

当前 `fw-plus-ai` 已包含 `fw-plus-ai-boot-dependencies`、`fw-plus-ai-boot-parent`、`fw-plus-ai-boot-autoconfigure`、`fw-plus-ai-boot-starters` 以及 LangChain4j/OpenAI/Magic-API AI 相关 starter。新能力必须作为这些模块的增量扩展，而不是另起一套不受 BOM 和自动配置体系管理的 AI 栈。

SQLBot v1 提供了智能问数产品形态，核心包括数据源选择、表结构检索、Prompt 模板、Text-to-SQL、SQL 执行、图表配置和日志。Aiflowy、Agents-Flex、Tinyflow 提供 Java AI 应用平台、Agent 和流程编排参考，尤其适合作为多 agent 协同、基础 AI 工作流和流程节点扩展的设计参考；但它们的运行时版本和架构目标与本仓库不同。本设计只借鉴能力边界和扩展点，落地必须遵守 fw-plus 的 Java 8、Spring Boot 2.7.18、Maven Wrapper 和现有模块风格。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-ai-boot-starters` 下新增 `fw-plus-ai-askdata-spring-boot-starter`。
- 结合现有 `fw-plus-ai` BOM、父 POM 和 LangChain4j 依赖管理，提供 AskData 核心服务。
- 提供 Java 8 兼容的 API：自然语言问题输入，输出 SQL、查询数据、执行步骤和错误信息。
- 提供可替换的数据源元数据提供器、SQL 执行器、模型调用器、Prompt 模板和 SQL 校验器。
- 模型管理和向量能力优先复用现有 `fw-plus-ai` 模块；如果现有模型/向量接口无法满足 AskData，需要在设计内识别并改造现有模块，而不是绕开它们新增平行体系。
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

AskData 的模型调用优先接入现有 LangChain4j 相关 starter 和模型配置能力；表结构召回、术语召回、训练样例召回等向量能力优先复用现有 Milvus、Qdrant、Weaviate 和 embedding 模块。若现有模块只面向 Magic-API 脚本，无法被 Java 服务层复用，则本 change 的后续设计/实现任务需要把公共接口上移或补充适配，而不是在 AskData 内重新维护一套模型和向量客户端。

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

Starter 先定义 `AskDataAgent`、`AskDataSubAgent`、`AskDataAgentDispatcher`、`AskDataStepContext`、`AskDataStepResult` 和 `AskDataFlowExecutor` 等轻量接口。默认实现使用普通 Java 服务串行执行；子 agent 默认通过同步 dispatcher 顺序执行并归并结果，后续可适配线程池、Agents-Flex、Tinyflow 或自研流程编排，但本 change 不强依赖任何一个外部 Agent 框架。

```

Full source: openspec/changes/add-askdata-spring-boot-starter/design.md

## openspec/changes/add-askdata-spring-boot-starter/tasks.md

- Source: openspec/changes/add-askdata-spring-boot-starter/tasks.md
- Lines: 1-58
- SHA256: 226e20dbbb0fdbab5f6a373de0c88cbca5759ec5c2e40f9dfb97d538a601bd3f

```md
## 1. Maven 模块接入

- [ ] 1.1 在 `fw-plus-ai-boot-starters` 下新增 `fw-plus-ai-askdata-spring-boot-starter` 模块。
- [ ] 1.2 将新模块接入 AI starter 聚合 POM，并接入 `fw-plus-ai` 使用的依赖管理。
- [ ] 1.3 确认新模块继承现有 Java 8、Spring Boot 2.7.18、`${revision}` 和 flatten 约定。

## 2. 核心 API 与模型

- [ ] 2.1 定义 `AskDataService` 作为主要 Java API。
- [ ] 2.2 定义请求和结果模型，覆盖问题输入、数据源上下文、生成 SQL、执行数据、步骤日志和错误信息。
- [ ] 2.3 定义数据源元数据模型，覆盖方言、表、字段、注释、样例值、术语和自定义 Prompt 片段。

## 3. Prompt 与模型集成

- [ ] 3.1 实现 Prompt 模板提供器，参考相关文档中的 SQL 生成规则，但统一使用 AskData 命名。
- [ ] 3.2 增加数据库方言规则抽象，覆盖标识符引用和查询行数限制语法。
- [ ] 3.3 梳理现有 `fw-plus-ai` 模型管理与 LangChain4j 模块，定义 AskData 所需的最小可复用模型调用适配器。
- [ ] 3.4 扩展或适配当前模型配置，使其可以表达多个模型条目和一个默认模型。
- [ ] 3.5 增加请求级模型 key 选择，并对指定模型缺失或默认模型缺失返回明确错误。
- [ ] 3.6 梳理现有 Milvus、Qdrant、Weaviate 和 embedding 模块，定义 AskData 如何复用它们进行表结构、术语或训练样例召回。
- [ ] 3.7 在现有 `fw-plus-ai` 能力足够时，实现基于现有能力的模型调用抽象和默认适配器。

## 4. SQL 安全与执行

- [ ] 4.1 实现保守的 SQL 安全校验器，只允许只读单语句查询。
- [ ] 4.2 实现可配置的行数限制策略和执行启用/禁用行为。
- [ ] 4.3 实现只读 SQL 执行器抽象和默认 JDBC 结果映射。

## 5. 多 Agent 与子 Agent 流程

- [ ] 5.1 定义 `AskDataAgent`、`AskDataSubAgent`、`AskDataAgentDispatcher`、步骤上下文/结果和流程执行器接口。
- [ ] 5.2 实现默认串行流程，并拆分元数据、SQL 生成、校验和执行步骤。
- [ ] 5.3 实现默认子 Agent 派发器，支持同步执行、结果归并、超时记录和失败策略。
- [ ] 5.4 允许应用通过 Spring Bean 替换单个 Agent 或子 Agent，默认运行时不强制依赖 Agents-Flex 或 Tinyflow。

## 6. 上下文压缩与恢复

- [ ] 6.1 定义 `AskDataConversationStore`、`AskDataContextCompressor`、`AskDataCheckpoint` 和 `AskDataResumeToken`。
- [ ] 6.2 在流程关键步骤完成后写入检查点，记录会话 ID、请求摘要、模型 key、数据源 key、步骤状态、生成 SQL、压缩摘要和错误信息。
- [ ] 6.3 实现默认上下文压缩策略，确保结构化字段不被摘要替代。
- [ ] 6.4 实现恢复入口，通过 `conversationId` 或 `resumeToken` 从最近稳定检查点继续执行。
- [ ] 6.5 对恢复失败、检查点缺失、版本不兼容和不安全 SQL 场景返回明确错误。

## 7. Spring Boot 自动配置

- [ ] 7.1 增加 `AskDataProperties`，配置前缀为 `fw.plus.ai.askdata`。
- [ ] 7.2 增加自动配置，为默认提供器、校验器、执行器、上下文存储、压缩器和服务注册条件化 Bean。
- [ ] 7.3 使用 Spring Boot 2.7 兼容的元数据注册自动配置。

## 8. 测试与验证

- [ ] 8.1 增加 SQL 校验器单元测试，覆盖危险语句拒绝和查询限制行为。
- [ ] 8.2 增加 Prompt 构造和方言规则选择单元测试。
- [ ] 8.3 增加默认模型选择、指定模型选择和模型缺失错误的单元测试。
- [ ] 8.4 增加 Spring 上下文测试，覆盖自动配置默认 Bean 和 Bean 替换。
- [ ] 8.5 增加子 Agent 结果归并、超时、失败策略测试。
- [ ] 8.6 增加上下文压缩、检查点写入、恢复继续和恢复失败测试。
- [ ] 8.7 使用相关 Maven Wrapper 构建新模块和聚合模块。

```

## openspec/changes/add-askdata-spring-boot-starter/specs/askdata-query-engine/spec.md

- Source: openspec/changes/add-askdata-spring-boot-starter/specs/askdata-query-engine/spec.md
- Lines: 1-139
- SHA256: a55fec827bdd4aefff3c4b35e495c61bfb4f257a36f3f08eae336236325859e2

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: AskData starter module
系统 SHALL 在 `fw-plus-ai-boot-starters` 聚合下提供 `fw-plus-ai-askdata-spring-boot-starter` 模块，并遵循现有 `fw-plus-ai` 的 Maven 父 POM、BOM 和 Spring Boot 2.7.18 自动配置风格。

#### Scenario: Starter is part of AI aggregation
- **WHEN** 开发者在 `fw-plus-ai` 中构建 `fw-plus-ai-boot-starters`
- **THEN** `fw-plus-ai-askdata-spring-boot-starter` SHALL 作为聚合模块参与构建

#### Scenario: Java 8 compatibility
- **WHEN** 项目使用 Java 8 编译 AskData starter
- **THEN** 代码 SHALL 不依赖 Java 9+ API 或 Spring Boot 3 特性

### Requirement: AskData service API
系统 SHALL 提供 Java 服务接口，用于接收自然语言问题、数据源上下文和可选执行配置，并返回结构化 AskData 结果。

#### Scenario: Generate SQL result
- **WHEN** 调用方提交包含问题和数据源标识的 AskData 请求
- **THEN** 服务 SHALL 返回包含生成 SQL、使用表名、推荐展示类型、执行步骤和状态的结果对象

#### Scenario: Return model generation error
- **WHEN** 模型返回内容无法解析为有效 SQL 结果
- **THEN** 服务 SHALL 返回失败状态和可诊断错误信息，而不是抛出不可控异常给调用方

### Requirement: Metadata provider abstraction
系统 SHALL 提供数据源元数据提供器抽象，用于向 AskData 引擎提供数据库方言、表结构、字段说明、样例值和业务术语。

#### Scenario: Build prompt with metadata
- **WHEN** AskData 服务处理自然语言问题
- **THEN** 服务 SHALL 通过元数据提供器获取可用 schema，并将其用于 Prompt 构建

#### Scenario: Replace metadata provider
- **WHEN** 应用自定义 `AskDataMetadataProvider` Bean
- **THEN** 自动配置 SHALL 使用自定义实现替代默认实现

### Requirement: Prompt and dialect rules
系统 SHALL 提供可配置的 Prompt 模板和数据库方言规则，使生成 SQL 遵守目标数据库的标识符引用、分页限制和只读查询约束。

#### Scenario: Apply query limit policy
- **WHEN** 请求未显式指定返回数量且默认查询限制启用
- **THEN** Prompt 或 SQL 校验阶段 SHALL 要求生成 SQL 包含默认行数限制

#### Scenario: Dialect-specific identifier rules
- **WHEN** 数据源方言为 MySQL、PostgreSQL 或 ClickHouse
- **THEN** Prompt 构建 SHALL 使用该方言对应的标识符引用和 limit 规则

### Requirement: Multiple model configuration
系统 SHALL 支持多个模型配置，并允许配置一个默认模型；AskData 请求可以显式指定模型 key，未指定时使用默认模型。

#### Scenario: Use default model
- **WHEN** AskData 请求未指定模型 key 且系统已配置默认模型
- **THEN** AskData 服务 SHALL 使用默认模型生成 SQL

#### Scenario: Use requested model
- **WHEN** AskData 请求指定有效模型 key
- **THEN** AskData 服务 SHALL 使用该模型配置生成 SQL，而不是默认模型

#### Scenario: Requested model missing
- **WHEN** AskData 请求指定不存在的模型 key
- **THEN** AskData 服务 SHALL 返回模型配置错误，不得静默回退到默认模型

#### Scenario: No default model configured
- **WHEN** AskData 请求未指定模型 key 且系统没有默认模型
- **THEN** AskData 服务 SHALL 返回明确错误，说明缺少默认模型配置

### Requirement: SQL safety validation
系统 SHALL 在执行前校验生成 SQL，只允许只读查询语句，并拒绝包含危险操作或多语句执行风险的 SQL。

#### Scenario: Reject non-query SQL
- **WHEN** 模型生成 `INSERT`、`UPDATE`、`DELETE`、`DROP`、`ALTER`、`TRUNCATE` 或其他非查询语句
- **THEN** AskData 服务 SHALL 拒绝执行并返回安全校验失败结果

#### Scenario: Reject multi statement SQL
- **WHEN** 模型生成包含多个 SQL 语句的内容
- **THEN** AskData 服务 SHALL 拒绝执行并返回安全校验失败结果

### Requirement: Multi-agent flow extension
系统 SHALL 提供轻量的多 agent 与子 agent 协作抽象，使 AskData 流程中的选表、生成 SQL、SQL 校验、SQL 执行和结果解释可以由主 Agent、独立步骤、独立 Agent 或子 Agent 实现，并由流程执行器串联。

#### Scenario: Execute default flow through steps

```

Full source: openspec/changes/add-askdata-spring-boot-starter/specs/askdata-query-engine/spec.md
