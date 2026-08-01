# Comet Design Handoff

- Change: integrate-tiny-engine-core
- Phase: design
- Mode: compact
- Context hash: 1397aac6720b6e6732d687637aaf489f1aaf8b22bd41d1230d4d99e846d3c068

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/integrate-tiny-engine-core/proposal.md

- Source: openspec/changes/integrate-tiny-engine-core/proposal.md
- Lines: 1-30
- SHA256: f9d54e9e68e1046d4f4346fc55816d9196c4f5b6ce6250673417326ac338f009

```md
## Why

TinyEngine 后端当前作为独立应用存在于 `tiny-engine-backend-java`，其父工程使用 Spring Boot 3.1.9 和 Java 17，并包含 `base` 与 `app` 两个模块。fw-plus 需要先在 `fw-plus-core` 提炼可复用的低代码核心领域能力，让后续 boot starter、自动配置或样例可以复用这些模型、持久化和服务契约，同时满足 fw-plus-core 的 Java 8 与 Spring Boot 2.7.18 约束。

## What Changes

- 在 `fw-plus-core` 新增或扩展 TinyEngine 核心模块，承载低代码平台的基础实体、DTO、公共返回、异常、枚举、上下文、TypeHandler 和工具类。
- 迁移 TinyEngine 主要领域模型与 Mapper/XML，包括应用、页面、页面历史、页面模板、画布、区块、区块组、物料、物料历史、组件、组件库、资源、资源组、业务分类、国际化、平台、租户、用户、扩展、任务记录、模型、数据源和关联关系等核心对象。
- 提炼动态模型能力，包括动态模型元数据、动态 CRUD DTO、动态表创建/变更/删除、动态查询/写入服务接口与实现，以及 SQL 标识符安全校验。
- 纳入 TinyEngine 初始化 SQL 资源，保留 MySQL、H2、PostgreSQL 等初始化脚本的可分发资源能力，并按 fw-plus-core 资源布局组织。
- 适配 fw-plus-core 现有依赖与版本体系：使用 `${revision}` / `0.1.1-SNAPSHOT`，兼容 Java 8，避免 Spring Boot 3、Java 17、Jakarta EE 9+、LangChain4j 1.x、RAG、AI Chat 和启动应用依赖进入本次核心模块。
- 不新增 Spring Boot 自动配置、starter、Web Controller、应用启动类或运行态部署能力；这些留给上层模块后续 change 处理。

## Capabilities

### New Capabilities

- `tiny-engine-core-domain`: 提供 TinyEngine 低代码核心领域模型、DTO、Mapper/XML、SQL 资源、动态模型和核心服务契约，使上层 fw-plus 模块可以在 Java 8 / Spring Boot 2.7.18 环境中引用并复用。

### Modified Capabilities

- 无。本次不修改既有 OpenSpec capability 的行为契约，只新增 TinyEngine core 领域能力。

## Impact

- 影响范围限定在 `fw-plus-core` 及其 Maven 模块、资源目录和必要的 BOM/父 POM 注册。
- 上层 `fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai`、`fw-plus-sample` 不在本次实现范围内，但后续可依赖本次新增的 core artifact。
- 需要从 `tiny-engine-backend-java/base` 参考迁移 Java 源码、Mapper XML 和 SQL 资源，并排除 `tiny-engine-backend-java/app` 的启动、Web 配置、定时清理和部署相关能力。
- 需要重点处理兼容性差异：`jakarta.*` 改为 Spring Boot 2.7 可用的 `javax.*` 或 Spring 事务注解，Java 17 写法改为 Java 8 可编译写法，依赖版本收敛到 fw-plus-core 现有 BOM 或可兼容版本。
- 需要保留或增强动态 SQL 安全边界，拒绝非法表名、字段名、排序字段和危险动态输入，避免把参考实现中的字符串拼接风险原样带入核心模块。

```

## openspec/changes/integrate-tiny-engine-core/design.md

- Source: openspec/changes/integrate-tiny-engine-core/design.md
- Lines: 1-82
- SHA256: 8ab74a172e2d603a28745436c31e39d052d07c2b13430947169b68856bb3bf30

[TRUNCATED]

```md
## Context

`tiny-engine-backend-java` 当前是独立 Spring Boot 应用，根 POM 使用 Spring Boot 3.1.9、Java 17，模块包括 `base` 和 `app`。其中 `base` 承载主要低代码领域能力：实体、DTO、Mapper/XML、动态模型、核心服务、异常、上下文、TypeHandler 与工具；`app` 主要承载启动类、Web 配置、过滤器、定时清理和环境配置。

目标仓库 `fw-plus-core` 是 fw-plus 的底层库层，受 Java 8、Spring Boot 2.7.18、`${revision}` / `0.1.1-SNAPSHOT` 和现有 Maven 聚合结构约束。该 change 的设计重点是把 TinyEngine 低代码后端核心能力转化为 fw-plus-core 可复用库模块，而不是把原独立应用整体嵌入。

## Goals / Non-Goals

**Goals:**

- 在 `fw-plus-core` 中形成可被上层 starter 依赖的 TinyEngine core artifact。
- 保留 TinyEngine 的核心领域对象、DTO、Mapper XML、SQL 初始化资源和服务接口/实现。
- 让动态模型 CRUD、动态表结构管理和模型元数据能力在 Java 8 / Spring Boot 2.7.18 下可编译、可测试、可复用。
- 保留并增强动态 SQL 安全边界，所有表名、字段名、排序字段和动态条件键都必须经过标识符校验。
- 按 fw-plus-core 现有依赖、编码、包名和 Maven 模块风格落地，不修改 Magic-API 2.2.2 版本。

**Non-Goals:**

- 不新增 Spring Boot 自动配置、starter、Controller、应用启动类、Web 过滤器或部署配置。
- 不迁移 AI Chat、RAG、向量存储、模型运行时扩展和 LangChain4j 1.x 相关能力。
- 不直接把 `tiny-engine-backend-java` 作为嵌套 git 仓库整体并入。
- 不改动 `fw-plus-boot`、`fw-plus-cloud`、`fw-plus-ai` 或 `fw-plus-sample`。

## Decisions

### Decision: 在 fw-plus-core 新增独立 TinyEngine core 模块

将 TinyEngine 基础能力放入 `fw-plus-core/fw-plus-project` 下的独立模块，并在 core 聚合 POM 与 BOM 中按本仓库方式注册。

- 理由：TinyEngine 低代码领域能力需要被 boot starter 和后续上层集成复用，放在 core 层更符合依赖方向。
- 替代方案：直接放入 `fw-plus-boot` starter。该方案会把领域模型与 Spring Boot 自动配置耦合，后续 Cloud/AI/样例复用成本更高。

### Decision: 迁移 `base` 核心能力，排除 `app` 应用运行能力

实现时以 `tiny-engine-backend-java/base` 为主要参考源，迁移实体、DTO、Mapper/XML、SQL 资源、动态模型、核心 service 与公共工具；`app` 中的启动类、Web 配置、OpenAPI、过滤器、定时任务和 profile 配置不进入本 change。

- 理由：本次拆分项目标是 core 基础库，不是可运行应用或 starter。
- 替代方案：完整迁移 `base` + `app`。该方案会引入 Web 层、启动配置和 Spring Boot 3 运行约束，超出本 change 范围。

### Decision: 显式做 Java 8 / Spring Boot 2.7 兼容适配

迁移代码必须消除 Java 17、Spring Boot 3 和 Jakarta EE 9+ 专属 API：事务注解优先使用 Spring `org.springframework.transaction.annotation.Transactional` 或 Java 8 可用 API；如涉及 Servlet/Validation，必须适配 `javax.*` 或移出 core。

- 理由：fw-plus-core 明确要求 Java 8 与 Spring Boot 2.7.18，不能把参考项目运行时约束传导到 core。
- 替代方案：保持原依赖版本。该方案会导致 Java 8 编译失败，并破坏 fw-plus-core 的版本治理。

### Decision: 动态 SQL 使用集中式标识符校验与参数绑定

动态模型相关服务必须集中处理表名、列名、排序字段、方向和动态条件键校验；数据值必须通过 MyBatis/JdbcTemplate 参数绑定传入。参考实现中已有正则校验，但在迁移时需要补齐字段列表、排序字段、DDL 列定义和批量写入路径的统一校验。

- 理由：动态表与动态字段天然存在 SQL 注入风险，本能力进入 core 后会被多个上层入口调用，边界必须前置。
- 替代方案：保留参考实现中的局部校验。该方案容易遗漏 `ORDER BY`、字段选择、DDL/ALTER 和批量 INSERT 路径。

### Decision: SQL 初始化资源作为库资源分发

MySQL、H2、PostgreSQL 初始化脚本按 fw-plus-core 资源目录纳入模块，不在 core 自动执行。后续 starter 或样例可以选择加载这些资源。

- 理由：core 层应提供资源与契约，不负责应用生命周期和数据库初始化策略。
- 替代方案：在 core 中自动执行 SQL。该方案会引入运行时副作用，且与 starter 职责冲突。

## Risks / Trade-offs

- [Risk] TinyEngine 参考项目依赖 Spring Boot 3.1.9 / Java 17，直接迁移会出现编译不兼容。→ Mitigation：实现任务中逐项替换 Jakarta、Java 17 和不兼容依赖，并以 fw-plus-core Java 8 编译作为验收。
- [Risk] 动态 SQL 字符串拼接路径遗漏校验会带来注入风险。→ Mitigation：新增统一标识符校验工具和危险输入测试，覆盖 table、field、orderBy、orderType、DDL column、condition key。
- [Risk] 领域模型数量较多，容易遗漏 Mapper XML 或 SQL 初始化资源。→ Mitigation：按实体、Mapper、XML、SQL、Service 五类建立迁移清单，并在 tasks 中要求引用验证。
- [Risk] 原项目包含 AI/RAG 与应用运行能力，容易被误并入 core。→ Mitigation：tasks 明确排除 AI Chat、RAG、VectorStorage、OpenAPI、Controller、启动类和定时任务。
- [Risk] SQL 脚本跨 MySQL/H2/PostgreSQL 方言差异会影响后续 starter 使用。→ Mitigation：core 只分发资源并保留目录区分，后续运行模块负责选择和执行。

## Migration Plan

1. 新建或扩展 fw-plus-core TinyEngine core Maven 模块，完成父 POM、聚合 POM、BOM 和依赖收敛。
2. 分批迁移公共基础类、领域实体/DTO、Mapper 接口/XML、SQL 资源。
3. 迁移核心服务接口与实现，并把 Web/Controller/启动应用能力剥离到后续上层 change。
4. 迁移动态模型能力并集中加强 SQL 标识符校验。
5. 运行 fw-plus-core 相关 Maven 编译和安全边界测试，确认 Java 8 兼容。

Rollback 策略：本 change 应以新增模块和新增注册为主；如实现失败，可从聚合 POM/BOM 移除 TinyEngine core 模块并删除新增模块目录，不影响现有 fw-plus-core 其他模块。

## Open Questions


```

Full source: openspec/changes/integrate-tiny-engine-core/design.md

## openspec/changes/integrate-tiny-engine-core/tasks.md

- Source: openspec/changes/integrate-tiny-engine-core/tasks.md
- Lines: 1-44
- SHA256: d4d948abb5a21b1bfbb36f777f39e355e606e0da9ee81b8d16193ba7db361d73

```md
## 1. 模块与依赖准备

- [ ] 1.1 确认 `fw-plus-core/fw-plus-project` 现有模块命名、父 POM、聚合 POM、BOM 和 Java 8 编译配置。
- [ ] 1.2 新增 TinyEngine core Maven 模块，并接入 fw-plus-core 聚合 POM、父 POM和 BOM，版本遵循 `${revision}` / `0.1.1-SNAPSHOT`。
- [ ] 1.3 收敛 TinyEngine core 依赖到 Spring Boot 2.7.18 与 fw-plus-core 兼容版本，排除 Spring Boot 3、Java 17、Jakarta EE 9+、LangChain4j 1.x、AI/RAG 专属依赖。
- [ ] 1.4 确认没有修改 Magic-API 2.2.2 相关依赖版本。

## 2. 基础类型与工具迁移

- [ ] 2.1 从 `tiny-engine-backend-java/base` 迁移公共基础类型，包括 Result、BaseEntity、HistoryEntity、PageQueryVo、异常枚举、ServiceException 和必要上下文抽象。
- [ ] 2.2 迁移 TinyEngine 所需 TypeHandler、JSON/Schema/Hash/文件安全等工具类，并改造为 Java 8 可编译写法。
- [ ] 2.3 将 `jakarta.*`、Spring Boot 3-only API 和 Java 17 写法替换为 Spring Boot 2.7 / Java 8 兼容实现。
- [ ] 2.4 为迁移后的公共基础类型补充最小单元测试或编译验证覆盖。

## 3. 领域模型、DTO 与持久化资源

- [ ] 3.1 迁移 TinyEngine 主要实体和 DTO，覆盖 app、page、page history、page template、canvas、block、block group、material、material history、component、component library、resource、resource group、business category、i18n、platform、tenant、user、extension、task record、model、datasource 和关联关系。
- [ ] 3.2 迁移对应 Mapper 接口，并按 fw-plus-core 包名和 MyBatis/MyBatis-Plus 版本调整 import、泛型和注解。
- [ ] 3.3 迁移对应 Mapper XML 到 TinyEngine core 模块资源目录，确保 namespace、resultMap、typeHandler 和 SQL 片段与迁移后包名一致。
- [ ] 3.4 迁移 MySQL、H2、PostgreSQL 初始化 SQL 资源到模块 classpath 下的稳定位置，并记录资源路径，不在 core 中自动执行。
- [ ] 3.5 建立实体、Mapper、XML、SQL 资源迁移核对清单，避免主领域对象遗漏。

## 4. 核心服务迁移

- [ ] 4.1 迁移 app/page/material/resource/i18n/platform/tenant/user/task/model/datasource 等核心服务接口。
- [ ] 4.2 迁移对应服务实现，并剥离 Controller、OpenAPI、启动类、过滤器、定时任务和 profile 配置依赖。
- [ ] 4.3 保留必要的扩展点，例如保存后 hook 或上下文读取接口，并避免引入上层 starter 自动配置职责。
- [ ] 4.4 为核心服务编写可编译的单元测试或轻量集成测试，验证服务契约可被上层模块引用。

## 5. 动态模型与 SQL 安全边界

- [ ] 5.1 迁移动态模型 DTO、DAO、动态 CRUD 服务和动态表生命周期服务。
- [ ] 5.2 新增集中式 SQL 标识符校验工具，覆盖表名、模型英文名、字段名、条件键、排序字段、排序方向、DDL 列名和批量写入字段。
- [ ] 5.3 改造动态查询、插入、更新、删除、建表、改表、删表、初始化和批量插入路径，确保所有值使用参数绑定，所有 SQL 标识符先校验后拼接。
- [ ] 5.4 增加危险输入测试，覆盖分号、注释、空白注入、函数调用、表达式、关键字拼接和非法排序方向，并确认执行 SQL 前被拒绝。
- [ ] 5.5 增加合法动态模型操作测试，验证动态查询、分页、写入、表结构读取和表生命周期操作仍可工作。

## 6. 范围排除与验证

- [ ] 6.1 检查 TinyEngine core 模块不包含 AI Chat、RAG、VectorStorage、LangChain4j 1.x、Controller、OpenAPI、启动应用、Docker 和 Web 过滤器。
- [ ] 6.2 运行 TinyEngine core 模块 Maven 编译，确认 Java 8 source/target 下通过。
- [ ] 6.3 运行新增或受影响测试，确认主要领域映射、服务契约、SQL 资源引用和动态 SQL 安全边界通过验证。
- [ ] 6.4 运行必要的 fw-plus-core 聚合层验证，确认新增模块注册没有破坏现有 core 构建。
- [ ] 6.5 更新实现说明或模块 README，记录 TinyEngine core artifact、资源路径、范围排除项和后续 starter 接入边界。

```

## openspec/changes/integrate-tiny-engine-core/specs/tiny-engine-core-domain/spec.md

- Source: openspec/changes/integrate-tiny-engine-core/specs/tiny-engine-core-domain/spec.md
- Lines: 1-61
- SHA256: c2129008e2f97c076606b44d5db4e82b7f0254942b7fe0fc7ebc9af3600fb435

```md
## ADDED Requirements

### Requirement: TinyEngine core module shall expose reusable domain artifacts

The system SHALL provide a fw-plus-core TinyEngine core module that exposes TinyEngine low-code domain entities, DTOs, common result and exception types, TypeHandlers, Mapper interfaces, Mapper XML resources, SQL initialization resources, and core service interfaces/implementations for upper fw-plus modules to reference.

#### Scenario: Upper module references core artifacts
- **WHEN** an upper fw-plus module adds a Maven dependency on the TinyEngine core artifact
- **THEN** the module can compile against TinyEngine core domain entities, DTOs, Mapper interfaces, Mapper XML resources, SQL resources, and core service contracts without depending on the original `tiny-engine-backend-java` repository

#### Scenario: Core excludes application runtime components
- **WHEN** the TinyEngine core module is built
- **THEN** it MUST NOT require TinyEngine application startup classes, Web Controllers, OpenAPI configuration, servlet filters, scheduled cleanup tasks, Docker deployment files, or Spring Boot starter auto-configuration

### Requirement: TinyEngine core shall be compatible with fw-plus-core runtime constraints

The system SHALL adapt migrated TinyEngine code to Java 8, Spring Boot 2.7.18, and the fw-plus-core Maven/version conventions.

#### Scenario: Java 8 compilation compatibility
- **WHEN** the fw-plus-core TinyEngine core module is compiled with the fw-plus-core Maven Wrapper and Java 8 source/target settings
- **THEN** compilation MUST succeed without Java 9+ APIs, Java 17 language features, Spring Boot 3-only APIs, or Jakarta EE 9+ imports

#### Scenario: Dependency governance compatibility
- **WHEN** the module declares dependencies and participates in fw-plus-core dependency management
- **THEN** it MUST use fw-plus-core compatible dependency versions, `${revision}` / `0.1.1-SNAPSHOT` versioning conventions, and MUST NOT modify the maintained Magic-API 2.2.2 dependency version

### Requirement: TinyEngine core shall preserve low-code domain coverage

The system SHALL preserve the main TinyEngine low-code backend domain capabilities from the reference `base` module, including application/page/material/resource/i18n/platform/tenant/user/task/model/datasource domains and their persistence mappings.

#### Scenario: Main domain mappings are available
- **WHEN** a caller uses TinyEngine core persistence components
- **THEN** Mapper interfaces and XML mappings MUST be available for the main TinyEngine domains, including app, page, page history, page template, canvas, block, block group, material, material history, component, component library, resource, resource group, business category, i18n entry/lang, platform, tenant, user, extension, task record, model, datasource, and relation tables

#### Scenario: SQL initialization resources are distributed
- **WHEN** a downstream module scans TinyEngine core classpath resources
- **THEN** MySQL, H2, and PostgreSQL TinyEngine initialization SQL resources MUST be present in a documented module resource location and MUST NOT be auto-executed by the core module itself

### Requirement: Dynamic model operations shall retain safe data access boundaries

The system SHALL provide dynamic model metadata, dynamic CRUD DTOs, dynamic table lifecycle services, dynamic query/write services, and a centralized SQL identifier validation boundary.

#### Scenario: Valid dynamic model operation
- **WHEN** a caller creates, updates, queries, or deletes dynamic model data using valid model names, field names, condition keys, sort fields, and allowed sort directions
- **THEN** the operation MUST use validated SQL identifiers and parameter-bound values while returning the expected dynamic result structure

#### Scenario: Dangerous table or field input is rejected
- **WHEN** a caller submits a table name, model name, field name, condition key, sort expression, DDL column, or order direction containing SQL operators, separators, comments, whitespace injection, function calls, or other non-identifier content
- **THEN** TinyEngine core MUST reject the operation before SQL execution with a clear validation error

#### Scenario: Dynamic table lifecycle is guarded
- **WHEN** dynamic table create, alter, drop, initialize, batch insert, or structure query operations are invoked
- **THEN** every generated table name and column identifier MUST pass the same centralized identifier validation rules before SQL is sent to JDBC or MyBatis

### Requirement: AI and RAG capabilities shall remain outside this core change

The system SHALL exclude TinyEngine AI Chat, RAG, vector storage, model runtime extension, and LangChain4j 1.x integration from this core capability.

#### Scenario: AI-related reference code is encountered during migration
- **WHEN** implementation reviews reference files such as `AiChatService`, `AiChatController`, `VectorStorageController`, `rag/*`, or LangChain4j 1.x dependencies
- **THEN** those files and dependencies MUST NOT be added to the TinyEngine core module as part of this change

```
