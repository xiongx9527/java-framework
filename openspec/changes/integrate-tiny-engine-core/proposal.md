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
