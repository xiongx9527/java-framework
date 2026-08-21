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

- TinyEngine core 模块 artifactId 最终命名待实现阶段结合 fw-plus-core 命名习惯确认，建议优先考虑 `fw-plus-tiny-engine-core` 或 `fw-plus-lowcode-tiny-engine`。
- 初始化 SQL 是否需要在后续 starter 中提供自动建表开关，不在本 change 决定。
