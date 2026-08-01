## Why

`fw-plus-nebula-jdbc` 当前承担 NebulaGraph JDBC 适配职责，但连接配置解析、连接池参数转换、资源关闭语义、Statement 语句分类和 ResultSet 游标边界存在稳定性风险，且关键行为缺少测试覆盖。

在后续实现 SQL 转 nGQL 和 MySQL 协议接入前，需要先稳定现有 JDBC 基础层，避免上层能力建立在不可验证或易出错的执行基础之上。

## What Changes

- 修复 JDBC URL 与 query 参数解析后的配置使用路径，确保 `graphspace`、认证信息和连接池参数进入实际连接逻辑。
- 增强连接池数字参数转换，支持 `Properties` 中常见的字符串值，并对非法值给出明确异常。
- 调整 `Connection`、`Statement`、`ResultSet` 的关闭和边界行为，使其更符合 JDBC 使用预期并避免非预期 NPE。
- 改善 `executeQuery` / `executeUpdate` 的 nGQL 语句分类逻辑，提升对大小写、换行、多空白等常见格式的容错能力。
- 补充不依赖真实 NebulaGraph 服务的单元测试，覆盖 URL 解析、配置转换、语句分类、资源关闭和 ResultSet 边界场景。
- 不引入 SQL 转 nGQL 转换能力，不实现 MySQL wire protocol，不新增独立服务端。

## Capabilities

### New Capabilities
- `nebula-jdbc-stability`: 覆盖 Nebula JDBC 驱动的基础稳定性要求，包括配置解析、连接池参数转换、资源关闭语义、语句分类和结果集边界行为。

### Modified Capabilities

## Impact

- 影响模块：`fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc`。
- 影响代码：`NebulaConnection`、`NebulaDriver`、`NebulaStatementImpl`、`NebulaResultSet`、`NebulaJdbcUrlParser`、`NebulaPropertyKey` 等 JDBC 基础类及相关测试代码。
- 兼容性：保持现有 `com.vesoft.nebula.jdbc` 包名和主要 JDBC API 兼容；不做破坏性 API 调整。
- 依赖：允许补充测试依赖；不默认升级 Nebula Java client 大版本。
- 系统：不改动 Boot、Cloud、AI、Product 层，不要求接入真实 NebulaGraph 服务进行集成测试。
