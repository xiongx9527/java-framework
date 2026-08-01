# Brainstorm Summary

- Change: integrate-tiny-engine-core
- Date: 2026-07-27

## 确认的技术方案

采用 `fw-plus-tiny-engine` 聚合模块 + `fw-plus-tiny-engine-core` 单一 core artifact 的中间方案。在 `fw-plus-core/fw-plus-project` 下新增 TinyEngine 聚合模块，第一版只包含一个稳定可依赖子模块 `fw-plus-tiny-engine-core`。包名统一迁移到 `com.xiongx9527.fw.plus.tinyengine.*`，内部按 `common`、`model`、`mapper`、`service`、`dynamic`、`support`、`resource` 分层。上层 Boot 和 AI change 只依赖 `fw-plus-tiny-engine-core`，不依赖 `tiny-engine-backend-java` 原仓库。

迁移边界：以 `tiny-engine-backend-java/base` 为主要参考源，迁移低代码领域实体、DTO、Mapper、Mapper XML、SQL 资源、核心服务和动态模型能力；排除 `app` 启动应用、Controller、OpenAPI、Servlet Filter、profile 配置、Docker、AI Chat、RAG、VectorStorage 和 LangChain4j 1.x。

## 关键取舍与风险

- 推荐单一 core artifact 起步，而不是首版拆成 common/domain/persistence/service/dynamic 多 artifact。优点是 Boot starter 依赖简单、迁移闭环快；风险是模块较大，需要用包边界、迁移清单和测试守住复杂度。
- `Result`、异常、公共类型进入 core 时应移除 Web/OpenAPI 注解依赖，HTTP 状态和 Controller 异常映射留给 Boot 层处理。
- 动态 SQL 是最高风险点。必须先抽出集中式 `SqlIdentifierValidator` 和动态 SQL 构建/校验边界，再迁移 `DynamicService` / `DynamicSqlProvider`，避免把字段、表名、排序表达式和 DDL 拼接风险原样带入。
- SQL 初始化资源只作为 classpath 资源分发，建议放到 `META-INF/tiny-engine/sql/{mysql,h2,postgresql}/` 并提供有序 manifest；core 不自动执行 SQL。
- 参考项目使用 Spring Boot 3.1.9 / Java 17 / Jakarta / MyBatis-Plus 3.5.7，目标必须收敛到 Spring Boot 2.7.18 / Java 8 / MyBatis-Plus 3.3.2。

## 测试策略

- Maven 编译：使用 core Maven Wrapper 编译新增模块及依赖链，确认 Java 8 source/target 兼容。
- 单元测试：覆盖 Result/异常、TypeHandler、Schema/Json/Hash/文件安全工具、集中式 SQL 标识符校验。
- 动态 SQL 安全测试：覆盖分号、注释、空白注入、函数调用、表达式、非法排序方向、非法 DDL 列名、非法批量写入字段。
- 轻量集成测试：用 H2 或 mock Mapper 验证 Mapper XML 可加载、主要服务契约可装配、SQL 资源 manifest 可发现。
- 范围排除测试/检查：确认新增 core 模块不包含 Controller、启动类、OpenAPI、Docker、AI/RAG/VectorStorage 和 Java 9+ API。

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖 core artifact、Java 8/Spring Boot 2.7 兼容、领域覆盖、动态 SQL 安全边界和 AI/RAG 排除；本阶段建议不回写 spec。
