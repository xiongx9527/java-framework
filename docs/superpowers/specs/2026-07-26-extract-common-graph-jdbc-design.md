---
comet_change: extract-common-graph-jdbc
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-26-extract-common-graph-jdbc
status: final
---

# 抽象公共图数据库 JDBC 标准层设计

## 背景

`fw-plus-core/fw-plus-project/fw-plus-graph` 当前包含 `fw-plus-nebula-jdbc` 和 `fw-plus-neo4j-jdbc` 两个具体 JDBC 驱动模块。两者都需要处理标准 JDBC 对象、默认不支持能力、参数绑定、元数据结果集形状、资源关闭状态和本地测试，但这些基础行为目前分散在具体驱动内部。

本设计将新增 `fw-plus-graph-jdbc` 公共模块。该模块不是第三个具体驱动，而是图数据库 JDBC 的抽象标准层。已有具体能力依赖它，具体驱动实现它。

## 目标

- 在 `fw-plus-graph` 聚合下新增 `fw-plus-graph-jdbc` 公共 Maven 模块。
- 让 `fw-plus-nebula-jdbc` 和 `fw-plus-neo4j-jdbc` 依赖公共模块。
- 在公共模块中沉淀无协议的标准 JDBC 基础能力。
- 保留 Nebula 与 Neo4j 的协议转换、连接 SDK、查询执行和原生结果映射在各自模块。
- 保持现有 JDBC URL、Driver 类名、Maven artifact 坐标和用户调用路径不变。
- 保持 Core 层 Java 1.8 编译约束。

## 非目标

- 不新增第三种图数据库 JDBC 驱动。
- 不重写 Nebula SQL AST、MySQL 协议网关、binlog、trigger、routine 等既有能力。
- 不抽象统一图查询语言，不把 nGQL、Cypher 或 SQL-to-Graph 转换放入公共层。
- 不要求本阶段验证真实 NebulaGraph 或 Neo4j 服务连接。

## 模块结构

`fw-plus-graph/pom.xml` 增加公共模块，并确保公共模块先于具体驱动构建：

```text
fw-plus-graph/
├── fw-plus-graph-jdbc/       # 公共图数据库 JDBC 抽象标准层
├── fw-plus-nebula-jdbc/      # Nebula 具体驱动，依赖公共层
└── fw-plus-neo4j-jdbc/       # Neo4j 具体驱动，依赖公共层
```

公共模块建议包名前缀为：

```text
com.xiongx9527.fw.plus.graph.jdbc
```

两个具体驱动继续使用现有包名：

```text
com.xiongx9527.fw.plus.graph.nebula.jdbc
com.xiongx9527.fw.plus.graph.neo4j.jdbc
```

## 公共层职责

公共层只定义标准 JDBC 行为和无协议基础实现。建议拆成以下几类组件：

- `GraphJdbcExecutor`：具体驱动执行器适配接口，接收最终查询文本和参数，返回公共结果抽象。
- `GraphJdbcResult`：公共结果抽象，表达列名、行数据、更新数量和是否存在 ResultSet。
- `GraphJdbcStatementSupport`：基础 Statement/PreparedStatement 状态、参数绑定、batch、queryTimeout、fetchSize、closeOnCompletion 等通用行为。
- `GraphJdbcRowSets`：只读 ResultSet 或 CachedRowSet 构造工具，处理列名、列标签、列类型、空结果集和 generated keys 形状。
- `GraphJdbcMetaDataSupport`：DatabaseMetaData 常见空结果集形状，包括 schemas、catalogs、table types、columns、primary keys 和 index info。
- `GraphJdbcDefaults`：默认值、unsupported 异常、unwrap/isWrapperFor 和 Java 8 兼容的 JDBC 类型辅助。

公共 API 应避免暴露 Nebula 或 Neo4j 名称，也不依赖 `com.vesoft`、`org.neo4j.driver`、Druid、Spring 或 MyBatis。

## 具体驱动适配

### Neo4j

Neo4j 当前已有 `Neo4jJdbcProxies`，其中一部分逻辑适合下沉到公共层：

- Statement/PreparedStatement 动态代理骨架。
- 参数绑定和参数元数据。
- CachedRowSet 结果构造。
- DatabaseMetaData 空结果集形状。
- defaultValue、unsupported、unwrap/isWrapperFor 等默认行为。

Neo4j 模块保留：

- `Neo4jDriver` 和 URL 识别。
- `Neo4jConnectionConfig`。
- `Neo4jSqlToCypherTranslator`。
- `Neo4jQueryExecutor` 与 Java Driver 适配。
- Neo4j 专属事务和 session 行为。

### Nebula

Nebula 模块当前实现更完整，迁移时以稳定性优先，不一次性大拆。优先接入这些公共能力：

- 默认 unsupported/default-value 和 unwrap/isWrapperFor 行为。
- 资源关闭状态与重复 close 语义。
- PreparedStatement 参数绑定辅助。
- DatabaseMetaData 空结果集列形状。
- 可由本地数据构造的只读结果集辅助。

Nebula 模块保留：

- `NebulaDriver` 和 URL 解析。
- Nebula connection pool 与 SDK 适配。
- nGQL/SQL 转换与 AST 相关能力。
- 原生 Nebula 结果映射。
- MySQL 协议网关、binlog、trigger、routine 等已有能力。

Nebula 的 `ResultSet` 面较广，迁移时不强行一次性替换为公共 ResultSet。可以先把无协议的辅助逻辑迁入公共模块，再逐步让 Nebula 复用。

## 数据流

标准 JDBC 调用进入具体驱动，再通过公共标准层处理基础 JDBC 行为，最后委托具体执行器：

```text
JDBC Client
  -> NebulaDriver / Neo4jDriver
  -> NebulaConnection / Neo4jConnection
  -> 公共 Statement/PreparedStatement 基础逻辑
  -> 具体驱动查询转换与执行器
  -> GraphJdbcResult / 具体原生结果适配
  -> 公共 RowSet/MetaData 或具体 ResultSet
```

这条路径保证公共层负责“标准 JDBC 形状”，具体驱动负责“图数据库协议事实”。

## 迁移顺序

1. 新增 `fw-plus-graph-jdbc` 模块，加入聚合 POM。
2. 在公共模块中实现无协议工具：默认异常、默认值、unwrap/isWrapperFor、JDBC 类型、基础 RowSet/MetaData 形状。
3. 引入公共执行/结果抽象和参数绑定支持。
4. 先迁移 Neo4j 的代理与 RowSet/MetaData 逻辑，因为其已有代理集中度更高，改动路径更清晰。
5. 再迁移 Nebula 的资源状态、异常、参数绑定和元数据形状，避免一次性重写大体量 ResultSet。
6. 更新两个具体驱动 POM 依赖和测试。

## 风险与缓解

- 抽象过度：公共层可能吸收具体协议细节。缓解方式是公共模块禁止依赖具体图数据库 SDK，公共接口只表达 JDBC 标准语义。
- 行为回归：迁移 Statement 或 ResultSet 可能影响框架集成。缓解方式是以已归档 OpenSpec 能力作为回归边界，重点跑 Nebula 稳定性与框架集成测试。
- 模块循环依赖：公共模块若引用具体驱动会形成反向依赖。缓解方式是公共模块只被具体驱动依赖，不依赖 Nebula/Neo4j。
- Java 8 兼容：公共层 API 容易误用 Java 9+ API。缓解方式是实现阶段禁止 `List.of`、`Map.copyOf`、模块系统等 Java 9+ 能力。

## 测试策略

公共模块：

- 默认 unsupported/default-value 行为。
- unwrap/isWrapperFor 行为。
- 参数绑定与 clearParameters。
- 空 ResultSet、table types、columns、primary keys、index info 形状。
- 资源 close 幂等性。

Neo4j 模块：

- DriverManager URL 接入。
- Statement/PreparedStatement 执行路径。
- 参数绑定、null 参数、batch 形状。
- DatabaseMetaData 基础行为。

Nebula 模块：

- 已有稳定性测试：重复 close、ResultSet 边界、wasNull、不支持能力异常。
- 已有框架集成测试：DriverManager、JdbcTemplate、MyBatis 常见读取路径和元数据映射。
- 不依赖真实 NebulaGraph 服务的本地测试优先。

## 验收标准

- `fw-plus-graph` 聚合中存在 `fw-plus-graph-jdbc`，且两个具体驱动都依赖它。
- 公共模块不依赖 Nebula 或 Neo4j SDK。
- 现有 Nebula/Neo4j 对外 JDBC 入口保持兼容。
- 公共层测试与两个具体驱动本地回归测试通过。
- OpenSpec 中已有 Nebula 具体能力明确依赖公共抽象标准能力，且公共能力不替代具体驱动能力。
