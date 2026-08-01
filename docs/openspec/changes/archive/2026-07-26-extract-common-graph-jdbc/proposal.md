## Why

`fw-plus-graph` 目前只有 `fw-plus-nebula-jdbc` 和 `fw-plus-neo4j-jdbc` 两个具体驱动模块，JDBC 基础对象、元数据结果集形状、默认 unsupported/default-value 行为和部分资源状态逻辑分散在两个模块中。随着图数据库 JDBC 能力继续扩展，这些重复实现会提高维护成本，也容易让两个驱动在标准 JDBC 行为上产生不一致。

本次变更将抽象一层通用的图数据库 JDBC 标准能力：公共层定义标准和抽象，Nebula、Neo4j 等已有具体能力依赖并实现该标准，同时保留各自协议、SQL/nGQL/Cypher 执行差异。

## What Changes

- 在 `fw-plus-core/fw-plus-project/fw-plus-graph` 下新增公共 Maven 子模块，提供通用图数据库 JDBC 抽象标准。
- 将可跨图数据库复用的 JDBC 支撑逻辑抽到公共模块，例如默认不支持能力处理、基础 `Statement`/`PreparedStatement` 代理骨架、基础 `DatabaseMetaData` 结果集形状、参数绑定辅助、只读结果集/行集构造和基础资源关闭状态。
- 调整 `fw-plus-nebula-jdbc` 与 `fw-plus-neo4j-jdbc` 依赖公共模块，具体驱动只保留连接配置、驱动注册、查询执行器、协议转换和原生结果映射。
- 让已有 Nebula JDBC 能力在规格层明确依赖公共图 JDBC 标准能力，避免后续继续以具体驱动为中心重复定义基础 JDBC 规则。
- 保持现有对外 JDBC URL、Driver 类名、Maven artifact 坐标和已有框架集成行为不变。
- 补充覆盖公共层与两个具体驱动适配路径的本地测试，不要求真实 NebulaGraph 或 Neo4j 服务。

## Capabilities

### New Capabilities
- `common-graph-jdbc-foundation`: 通用图数据库 JDBC 抽象标准层，定义公共模块、标准接口、默认 JDBC 行为和具体驱动依赖边界。

### Modified Capabilities
- `nebula-jdbc-stability`: 明确 Nebula JDBC 稳定性能力依赖 `common-graph-jdbc-foundation`，并在复用公共层后保持已归档稳定性要求。
- `nebula-jdbc-client-framework-integration`: 明确 Nebula JDBC 框架集成能力依赖 `common-graph-jdbc-foundation`，并在复用公共层后保持 DriverManager、JdbcTemplate、MyBatis 常见读取路径和元数据映射行为。

## Impact

- 影响代码范围：`fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml`、新增公共图 JDBC 模块、`fw-plus-nebula-jdbc`、`fw-plus-neo4j-jdbc` 及其本地测试。
- 影响构建：`fw-plus-graph` 聚合模块需要新增公共模块并让两个具体驱动声明依赖；仍遵循 Core 层 Java 1.8 与 Maven Wrapper 构建约束。
- 规格关系：已有具体 JDBC 能力依赖公共抽象标准能力；公共能力不替代具体驱动能力，也不承诺任何单一图数据库协议。
- 对外兼容性：不引入破坏性变更；不改变已有 JDBC URL 前缀、Driver 类、包名对外入口、SQL/nGQL/Cypher 语义或运行时服务依赖。
- 非目标：不新增第三种图数据库驱动，不重写 Nebula SQL AST/MySQL 协议相关能力，不要求接入真实图数据库做集成测试。
