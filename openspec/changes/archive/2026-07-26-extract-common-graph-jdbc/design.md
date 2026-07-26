## Context

`fw-plus-core/fw-plus-project/fw-plus-graph` 当前聚合 `fw-plus-nebula-jdbc` 和 `fw-plus-neo4j-jdbc` 两个具体 JDBC 驱动模块。Neo4j 模块中已有 `Neo4jJdbcProxies` 负责 `Statement`、`PreparedStatement`、`DatabaseMetaData` 和 `ResultSet` 的部分通用代理形状；Nebula 模块则以完整 JDBC 类实现为主，并已有稳定性、框架集成、SQL AST、MySQL 协议等多项规格能力。

本次变更的核心不是新增图数据库实现，而是把公共 JDBC 行为沉淀为一个抽象标准层。已有具体能力依赖公共能力，具体驱动负责实现标准并保留协议差异。

## Goals / Non-Goals

**Goals:**

- 新增公共图数据库 JDBC 模块，作为 Nebula、Neo4j 等具体驱动的抽象标准和基础工具层。
- 让两个现有驱动依赖公共模块，复用标准 JDBC 行为、默认异常处理、参数绑定、基础元数据结果集和资源状态管理。
- 保持 Nebula 与 Neo4j 的连接配置、驱动注册、查询执行、协议转换和原生结果映射位于各自模块。
- 在规格层表达依赖关系：已有具体能力依赖 `common-graph-jdbc-foundation`，公共能力不依赖任何具体图数据库。
- 保持 Java 1.8、现有 Maven 聚合结构和对外 JDBC 入口兼容。

**Non-Goals:**

- 不新增第三种图数据库 JDBC 驱动。
- 不重写 Nebula SQL AST、MySQL 协议网关、binlog、trigger、routine 等已归档能力。
- 不改变已有 JDBC URL 前缀、Driver 类名、Maven artifact 坐标或用户侧调用方式。
- 不要求本次验证连接真实 NebulaGraph 或 Neo4j 服务。

## Decisions

### Decision: 新增公共模块而不是放入某个具体驱动

在 `fw-plus-graph` 聚合下新增公共子模块，例如 `fw-plus-graph-jdbc-common` 或同等语义名称。Nebula 和 Neo4j 都依赖该模块。

理由：公共层是抽象标准，不应从属于 Nebula 或 Neo4j 任一具体实现；独立 Maven 模块可以防止具体驱动之间相互依赖，也便于后续新增图数据库驱动复用。

备选方案：把 Neo4j 的 `Neo4jJdbcProxies` 移入 Nebula 模块复用。该方案会让 Nebula 依赖 Neo4j 命名和语义，破坏抽象边界，因此不采用。

### Decision: 公共层定义 JDBC 标准行为，具体驱动提供执行适配

公共层提供可复用的 JDBC 基础抽象，例如：

- 默认 `unsupported` 和 `defaultValue` 处理。
- 基础 `Statement`/`PreparedStatement` 状态、参数绑定、batch 形状和关闭语义。
- 基础只读 `ResultSet` 或 `CachedRowSet` 构造工具。
- 基础 `DatabaseMetaData` 结果集列形状和 table/schema/catalog 空结果集构造。
- 公共 `GraphJdbcExecutor`、`GraphJdbcResult`、`GraphJdbcMetadata` 等抽象契约，用于连接具体驱动执行器和 JDBC 对象。

具体驱动只实现协议相关部分：Nebula 的 nGQL/SQL 转换和原生结果包装，Neo4j 的 Cypher 执行和 SQL-to-Cypher 转换。

备选方案：公共层直接定义统一图查询语言或统一 SQL 转换器。该方案会扩大范围并触碰已有 Nebula SQL AST 能力，不符合本次抽象标准边界，因此不采用。

### Decision: 以兼容迁移方式落地

先新增公共模块和测试，再逐步让 Neo4j 与 Nebula 迁移可复用部分。每迁移一类行为，都必须保留原有 public 入口和已归档规格要求。

迁移顺序建议：

1. 抽出无协议依赖的异常、默认值、JDBC 类型/元数据结果集工具。
2. 抽出参数绑定和基础 `Statement`/`PreparedStatement` 代理骨架。
3. 将 Neo4j 代理实现改为公共骨架适配器。
4. 将 Nebula 可复用的资源状态、参数绑定、元数据形状和只读结果集行为接入公共层。
5. 补齐公共层单元测试，以及 Nebula/Neo4j 不依赖真实服务的适配回归测试。

## Risks / Trade-offs

- [Risk] 过度抽象会把 Nebula 和 Neo4j 的协议差异隐藏到公共层，导致公共 API 复杂化。→ Mitigation：公共层只定义 JDBC 标准和无协议行为，查询语言转换、原生连接、结果映射留在具体模块。
- [Risk] 迁移公共 Statement/ResultSet 行为时可能影响已有框架集成。→ Mitigation：以现有规格为回归边界，保留 DriverManager、JdbcTemplate、MyBatis 常见读取路径测试。
- [Risk] Maven 模块顺序错误会导致两个具体驱动无法解析公共模块。→ Mitigation：在 `fw-plus-graph/pom.xml` 中先声明公共模块，再声明具体驱动，并在两个具体驱动 POM 中显式依赖公共模块。
- [Risk] Java 8 与当前依赖约束限制公共层 API 设计。→ Mitigation：公共模块使用 Java 1.8 API，不引入 Java 9+ 集合工厂、模块系统或新外部依赖。

## Migration Plan

1. 新增公共图 JDBC Maven 模块并加入 `fw-plus-graph` 聚合。
2. 将无协议依赖的 JDBC 工具和抽象契约放入公共模块。
3. 让 `fw-plus-neo4j-jdbc` 依赖公共模块并迁移代理/元数据/结果集工具。
4. 让 `fw-plus-nebula-jdbc` 依赖公共模块并迁移可共享行为，同时保留 Nebula 专属 SQL/nGQL、原生 SDK 和历史规格能力。
5. 运行聚合或指定模块测试验证公共层和两个驱动适配。

回滚策略：公共模块迁移应以小步提交完成；若某个具体驱动适配失败，可先保留公共模块和另一个驱动迁移结果，回退失败驱动的依赖接入，不改变对外 JDBC 入口。

## Open Questions

- 公共模块最终 artifactId 使用 `fw-plus-graph-jdbc-common`、`fw-plus-common-graph-jdbc` 还是项目已有命名风格下的其他名称，需要在设计阶段最终确认。
- Nebula 当前完整 `ResultSet` 类中哪些方法优先迁移到公共只读结果集基础类，需要在实现前结合测试覆盖逐项确认。
