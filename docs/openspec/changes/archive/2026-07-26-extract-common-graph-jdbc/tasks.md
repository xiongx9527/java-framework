## 1. 模块与依赖结构

- [x] 1.1 在 `fw-plus-graph` 聚合下新增公共图 JDBC Maven 子模块，并确认模块顺序位于具体驱动之前
- [x] 1.2 为公共模块配置 Java 1.8 兼容的 POM、包名和测试依赖，不引入新的外部运行时依赖
- [x] 1.3 调整 `fw-plus-nebula-jdbc` 与 `fw-plus-neo4j-jdbc` POM，使两个具体驱动依赖公共图 JDBC 模块

## 2. 公共抽象标准层

- [x] 2.1 定义公共图 JDBC 执行抽象和结果抽象，用于连接具体驱动执行器与标准 JDBC 对象
- [x] 2.2 抽出默认 unsupported/default-value、unwrap/isWrapperFor、资源关闭状态和 JDBC 类型辅助逻辑
- [x] 2.3 抽出基础参数绑定、PreparedStatement 参数元数据和 batch 结果形状
- [x] 2.4 抽出只读 ResultSet 或 RowSet 构造工具，支持列名、列标签、列类型和空结果集
- [x] 2.5 抽出基础 DatabaseMetaData 结果集形状，包括 schemas、catalogs、table types、columns、primary keys 和 index info

## 3. Neo4j JDBC 适配

- [x] 3.1 将 `Neo4jJdbcProxies` 中无 Neo4j 协议依赖的代理、元数据和结果集逻辑迁移到公共模块
- [x] 3.2 保留 Neo4j 连接配置、SQL-to-Cypher 转换、Cypher 执行器和 Java Driver 适配在 `fw-plus-neo4j-jdbc`
- [x] 3.3 补充或更新 Neo4j 本地测试，验证 DriverManager、Statement、PreparedStatement、参数绑定和元数据行为不变

## 4. Nebula JDBC 适配

- [x] 4.1 将 Nebula JDBC 中可复用的资源状态、异常、参数绑定和元数据结果集形状接入公共模块
- [x] 4.2 保留 Nebula 连接池、nGQL/SQL 转换、原生 SDK 结果映射、SQL AST 和 MySQL 协议相关能力在 `fw-plus-nebula-jdbc`
- [x] 4.3 补充或更新 Nebula 本地稳定性测试，覆盖重复 close、ResultSet 边界、wasNull 和不支持能力异常
- [x] 4.4 补充或更新 Nebula 框架集成测试，覆盖 DriverManager、JdbcTemplate、MyBatis 常见读取路径和元数据映射

## 5. 验证与文档

- [x] 5.1 运行公共模块和两个具体驱动模块的单元测试，确保不依赖真实 NebulaGraph 或 Neo4j 服务
- [x] 5.2 运行 `fw-plus-core` 中 `fw-plus-graph` 相关 Maven 构建命令，确认模块依赖顺序和 Java 1.8 编译通过
- [x] 5.3 检查 OpenSpec 规格关系，确认已有 Nebula 能力依赖公共抽象标准能力且未扩大到第三方图数据库实现
