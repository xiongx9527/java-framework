# Brainstorm Summary

- Change: add-nebula-sql-to-ngql-query-support
- Date: 2026-07-17

## 确认的技术方案

- 本 change 只处理 query-only SQL SELECT 到 nGQL 的转换边界，不进入 INSERT、UPDATE、DELETE、DDL、事务、MySQL 协议、binlog、routine、trigger 或完整 JDBC 框架兼容。
- 本 change 消费 `add-nebula-sql-parser-core` 产出的稳定 AST/IR，不直接解析 SQL 字符串，不依赖 Druid AST 作为转换层公共契约。
- 现有 `fw-plus-nebula-jdbc` 查询链路以 `NebulaStatementImpl` 接收 nGQL，并通过 `NebulaConnection` / `NebulaClientFacade.executeNgql` 执行。SQL 查询转换应作为独立转换能力接入，不破坏 `jdbc:nebula://...` nGQL 直连路径。
- `add-nebula-sql-write-and-ddl-support` 可复用本 change 定义的 schema 映射模型，但写入、DDL、安全回滚和事务边界由该 change 单独实现。
- `add-nebula-jdbc-client-framework-integration` 负责 PreparedStatement 参数绑定、ResultSet/DatabaseMetaData、JdbcTemplate/MyBatis 验收和标准 JDBC 异常语义；本 change 只暴露转换结果和查询列描述所需的核心元数据。
- `add-nebula-mysql-protocol-gateway-readonly` 负责 wire protocol、认证、COM_QUERY 和 MySQL 文本协议结果集；复杂只读 SQL 是否可执行由本 change 的 query converter 判定。
- 采用方案 A：独立 Query Converter 核心。

核心边界：

- 输入：parser-core 的 SELECT AST/IR、可选参数占位符绑定视图、`NebulaSchemaMappingRegistry`。
- 输出：`NgqlQueryPlan`，包含 nGQL、输出列描述、语义保真级别、转换 trace 和不支持结构的稳定错误码。
- 执行：本 change 可定义通过现有 `NebulaClientFacade` 执行转换后 nGQL 的接入点，但不负责完整 JDBC framework 行为；底层仍复用 `NebulaConnection.execute(nGql)` / `NebulaClientFacade.executeNgql`。
- schema mapping：独立建模 table-to-tag、table-to-edge、id/src/dst/rank 字段、property 字段、方向、默认 vertex id 列名、别名解析和输出列命名。
- 支持策略：显式白名单。点查询、边查询、投影、WHERE、ORDER BY、LIMIT/OFFSET 是第一优先级；JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数表达式只在 schema mapping 与 nGQL 表达能力明确时转换，否则 fail fast。

## 关键取舍与风险

- 不直接解析 SQL 字符串，避免与 parser-core 重叠。
- 不在 JDBC 层绑定 converter 细节，避免覆盖 framework integration。
- 不把 SHOW/DESCRIBE 作为本 change 核心范围；只读 MySQL gateway 可通过 metadata/gateway change 处理。
- 对复杂 SQL 使用可映射白名单，不做关系数据库语义模拟。
- `JOIN` 只支持能映射为 tag-edge-tag 关系的结构；任意关系型 join、笛卡尔积、多跳推断、非等值 join 默认为不支持。
- 子查询、UNION、聚合和函数必须有明确 nGQL 等价或受控等价；不能通过客户端后处理伪装支持，除非 Design Doc 明确限定为 converter 输出的一部分且可测试。

## 测试策略

- 转换器单元测试不依赖真实 NebulaGraph，使用 parser-core AST/IR fixture 和 schema mapping fixture。
- golden tests 覆盖 SQL 输入、IR、mapping、期望 nGQL、输出列描述和 trace。
- negative tests 覆盖不支持 JOIN、写入/DDL、事务、不可映射函数、复杂子查询、UNION 不兼容列形态等。
- 执行路径测试只验证转换后的 nGQL 会交给现有 Nebula 执行链路；不在本 change 覆盖 JdbcTemplate/MyBatis 或 MySQL 客户端协议。

## Spec Patch

- 补充 parser-core AST/IR 消费边界：转换器 SHALL 只消费 parser-core 的稳定 IR，不直接解析 SQL 字符串或依赖 Druid AST 作为公共契约。
- 补充 query-only 边界：转换器 SHALL 拒绝 INSERT、UPDATE、DELETE、DDL 和事务语句，并把写入/DDL 交由 `add-nebula-sql-write-and-ddl-support`。
- 补充 schema mapping 要求：系统 SHALL 使用显式 schema mapping 区分 tag、edge、vertex id、edge src/dst/rank 和 property 字段。
- 补充跨 change 依赖：JDBC framework integration 和 MySQL protocol gateway SHALL 调用本 change 的 query converter，不在各自 change 中重复定义 SQL 到 nGQL 查询语义。
- 补充错误验收：不可映射 SQL SHALL 返回包含不支持结构、原因和稳定错误码的明确错误。
