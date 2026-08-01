## ADDED Requirements

### Requirement: Nebula JDBC framework integration depends on common graph JDBC foundation
Nebula JDBC 框架集成能力 SHALL 依赖 `common-graph-jdbc-foundation` 提供的公共 JDBC 抽象标准，并在复用公共层后继续满足标准 JDBC、JdbcTemplate 和 MyBatis 常见读取路径。

#### Scenario: DriverManager 和 Statement 路径保持兼容
- **WHEN** 应用通过 DriverManager 获取 Nebula JDBC 连接并创建 Statement 或 PreparedStatement
- **THEN** 公共层接入 SHALL 不改变已有连接、执行、读取 ResultSet 和关闭资源的外部行为

#### Scenario: 框架元数据读取保持兼容
- **WHEN** JdbcTemplate 或 MyBatis 读取 ResultSetMetaData、列标签、列类型或基础 DatabaseMetaData 行为
- **THEN** Nebula JDBC SHALL 在依赖公共层后继续提供框架映射所需的稳定元数据

#### Scenario: 不支持能力仍有明确异常
- **WHEN** 框架调用当前不支持的 batch、generated keys、CallableStatement、多结果集或复杂事务能力
- **THEN** Nebula JDBC SHALL 继续返回明确的 SQLFeatureNotSupportedException 或 SQLException，而不是静默失败
