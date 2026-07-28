# Brainstorm Summary

- Change: stabilize-nebula-jdbc
- Date: 2026-07-14
- Status: 已定稿，用户已确认最终技术设计方案

## 确认的技术方案

采用方案 B：全面 façade + 生命周期管理器。

本 change 只稳定 `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`，不实现 SQL 转 nGQL，不实现 MySQL wire protocol，不改动 Boot、Cloud、AI、Product 层，不要求真实 NebulaGraph 集成测试。

新增内部组件：
- `NebulaConnectionConfig`：承载 URL + `Properties` 合并后的类型化连接配置，包括 graphspace、认证、地址和连接池参数。
- `NebulaConnectionConfigParser`：包装/复用现有 `NebulaJdbcUrlParser`，负责生成配置对象并统一数值参数解析错误。
- `NebulaClientFacade`：隔离 Nebula Java client 的 `NebulaPool`、`Session`、`ResultSet` 调用，对外暴露 nGQL 执行边界。
- `NebulaResourceLifecycle`：管理 connection、statement、resultSet 的关闭状态和幂等语义。
- `NebulaStatementClassifier`：独立负责 nGQL query/update 分类和错误消息，不解析 SQL。
- `NebulaResultSetCursor`：明确游标边界和 `wasNull()` 状态语义。

公开 API 兼容约束：
- 不改 `java.sql.Driver` / `Connection` / `Statement` / `ResultSet` 公开方法签名。
- 不改变 `com.vesoft.nebula.jdbc` 包的外部入口。
- 不改变 `jdbc:nebula://...` URL 格式。
- 新增组件放在内部包，如 `config`、`client`、`lifecycle`、`statement` 等。
- 当前 change 不支持 SQL；SQL 转 nGQL 留给后续 `add-sql-to-ngql-translator`。

## 数据流与错误处理

- 连接创建：`NebulaDriver.connect` → `NebulaConnection` → `NebulaConnectionConfigParser` → `NebulaConnectionConfig` → `NebulaClientFacade.open` → `NebulaConnection ready`。
- 语句执行：`NebulaStatementImpl` 调用 `NebulaStatementClassifier` 校验 nGQL 类型，再通过 `NebulaClientFacade.executeNgql` 执行。
- ResultSet：通过 cursor 组件管理游标边界和 lastColumnAccess 状态。
- 配置错误、Nebula client 错误、生命周期错误统一转换为明确 `SQLException`。
- 重复 close 幂等；close 后访问抛明确 `SQLException`。

## 关键取舍与风险

- 选择 B 是为了最大化内部边界清晰度和测试覆盖，为后续 SQL 转 nGQL 与 MySQL 协议接入留下更稳定的内部基础。
- 风险是改动面比局部修复更大，必须严格限制在 `fw-plus-nebula-jdbc` 内部，不改变公开 API 和不引入后续 change 的功能。
- 新增内部组件会增加文件数量和设计复杂度，需要用清晰命名与包内可见边界控制复杂度。
- 当前不做真实 NebulaGraph 集成测试，可能遗漏 client 交互问题；本 change 通过 façade + mock 单元测试降低风险，真实集成测试可后续单独补充。

## 测试策略

- 允许新增测试依赖；使用 JUnit + Mockito，优先复用父 POM 版本管理。
- 不依赖真实 NebulaGraph 服务，不引入 Testcontainers。
- 测试分层：纯逻辑测试、mock client 测试、JDBC API 行为测试。
- 覆盖配置解析、数字参数解析、Nebula client façade 生命周期编排、Statement 分类、ResultSet 边界和 close 幂等性。
- 验证命令优先：`cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`，必要时加 `-am`。

## Spec Patch

无。现有 delta spec 已覆盖主要验收场景；方案 B 属于内部架构实现策略，不需要修改 OpenSpec 需求范围。
