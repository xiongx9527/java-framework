# Comet Design Handoff

- Change: extract-common-graph-jdbc
- Phase: design
- Mode: compact
- Context hash: 845303960383f83f363b5e2f9a1f9d8439189d54114162c622f1964d0dfcbdd3

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/extract-common-graph-jdbc/proposal.md

- Source: openspec/changes/extract-common-graph-jdbc/proposal.md
- Lines: 1-31
- SHA256: bbf94072daac9b5db805cda9c98b74c5e549e52f18453990cde14a36102cf57c

```md
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

```

## openspec/changes/extract-common-graph-jdbc/design.md

- Source: openspec/changes/extract-common-graph-jdbc/design.md
- Lines: 1-80
- SHA256: 5a854120993d8b8e6e1307d6ea4c96c8c623ca93948dc068e6e24f1e4de79c5b

```md
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

```

## openspec/changes/extract-common-graph-jdbc/tasks.md

- Source: openspec/changes/extract-common-graph-jdbc/tasks.md
- Lines: 1-32
- SHA256: 3bb95c02406d49b3b707940b67589fe98a8ed8d58205af55ff3ae6962ce8c747

```md
## 1. 模块与依赖结构

- [ ] 1.1 在 `fw-plus-graph` 聚合下新增公共图 JDBC Maven 子模块，并确认模块顺序位于具体驱动之前
- [ ] 1.2 为公共模块配置 Java 1.8 兼容的 POM、包名和测试依赖，不引入新的外部运行时依赖
- [ ] 1.3 调整 `fw-plus-nebula-jdbc` 与 `fw-plus-neo4j-jdbc` POM，使两个具体驱动依赖公共图 JDBC 模块

## 2. 公共抽象标准层

- [ ] 2.1 定义公共图 JDBC 执行抽象和结果抽象，用于连接具体驱动执行器与标准 JDBC 对象
- [ ] 2.2 抽出默认 unsupported/default-value、unwrap/isWrapperFor、资源关闭状态和 JDBC 类型辅助逻辑
- [ ] 2.3 抽出基础参数绑定、PreparedStatement 参数元数据和 batch 结果形状
- [ ] 2.4 抽出只读 ResultSet 或 RowSet 构造工具，支持列名、列标签、列类型和空结果集
- [ ] 2.5 抽出基础 DatabaseMetaData 结果集形状，包括 schemas、catalogs、table types、columns、primary keys 和 index info

## 3. Neo4j JDBC 适配

- [ ] 3.1 将 `Neo4jJdbcProxies` 中无 Neo4j 协议依赖的代理、元数据和结果集逻辑迁移到公共模块
- [ ] 3.2 保留 Neo4j 连接配置、SQL-to-Cypher 转换、Cypher 执行器和 Java Driver 适配在 `fw-plus-neo4j-jdbc`
- [ ] 3.3 补充或更新 Neo4j 本地测试，验证 DriverManager、Statement、PreparedStatement、参数绑定和元数据行为不变

## 4. Nebula JDBC 适配

- [ ] 4.1 将 Nebula JDBC 中可复用的资源状态、异常、参数绑定和元数据结果集形状接入公共模块
- [ ] 4.2 保留 Nebula 连接池、nGQL/SQL 转换、原生 SDK 结果映射、SQL AST 和 MySQL 协议相关能力在 `fw-plus-nebula-jdbc`
- [ ] 4.3 补充或更新 Nebula 本地稳定性测试，覆盖重复 close、ResultSet 边界、wasNull 和不支持能力异常
- [ ] 4.4 补充或更新 Nebula 框架集成测试，覆盖 DriverManager、JdbcTemplate、MyBatis 常见读取路径和元数据映射

## 5. 验证与文档

- [ ] 5.1 运行公共模块和两个具体驱动模块的单元测试，确保不依赖真实 NebulaGraph 或 Neo4j 服务
- [ ] 5.2 运行 `fw-plus-core` 中 `fw-plus-graph` 相关 Maven 构建命令，确认模块依赖顺序和 Java 1.8 编译通过
- [ ] 5.3 检查 OpenSpec 规格关系，确认已有 Nebula 能力依赖公共抽象标准能力且未扩大到第三方图数据库实现

```

## openspec/changes/extract-common-graph-jdbc/specs/common-graph-jdbc-foundation/spec.md

- Source: openspec/changes/extract-common-graph-jdbc/specs/common-graph-jdbc-foundation/spec.md
- Lines: 1-41
- SHA256: 4315a46d88e3d3ac619aa8ba8bf60fc879e58f167914c00f4d47d2c8b6104f86

```md
## ADDED Requirements

### Requirement: Common graph JDBC foundation defines an abstract standard
系统 SHALL 提供一个通用图数据库 JDBC 抽象标准能力，用于定义跨图数据库驱动复用的 JDBC 基础行为，并且该能力 MUST 不绑定 NebulaGraph、Neo4j 或任一具体图数据库协议。

#### Scenario: 公共能力作为标准被具体驱动依赖
- **WHEN** Nebula JDBC 或 Neo4j JDBC 实现标准 JDBC 基础行为
- **THEN** 具体驱动 SHALL 依赖公共图 JDBC 基础能力，而不是在各自模块重复定义同类基础规则

#### Scenario: 公共能力不承诺具体查询协议
- **WHEN** 公共模块定义 JDBC 抽象、默认异常、参数绑定、元数据结果集或资源状态行为
- **THEN** 公共模块 MUST 不包含 Nebula nGQL、Neo4j Cypher 或任一具体图数据库连接 SDK 的协议实现

### Requirement: Concrete graph JDBC drivers preserve public compatibility
系统 SHALL 在接入公共图 JDBC 基础能力后保持现有具体驱动的对外入口兼容，包括 JDBC URL、Driver 类、Maven artifact 坐标和常见 JDBC 调用路径。

#### Scenario: DriverManager 入口保持不变
- **WHEN** 应用使用现有 Nebula 或 Neo4j JDBC URL 通过 DriverManager 获取连接
- **THEN** 对应具体驱动 SHALL 继续被现有 Driver 类识别和处理

#### Scenario: Maven 坐标保持不变
- **WHEN** 应用继续依赖 `fw-plus-nebula-jdbc` 或 `fw-plus-neo4j-jdbc`
- **THEN** 应用 SHALL 不需要改为直接依赖公共图 JDBC 基础模块才能使用现有具体驱动

### Requirement: Common JDBC behavior is reusable by existing drivers
系统 SHALL 将可跨图数据库复用的 JDBC 基础行为放入公共层，包括默认不支持能力处理、参数绑定辅助、只读结果集构造、基础元数据结果集形状和资源关闭状态。

#### Scenario: 默认不支持行为一致
- **WHEN** 具体驱动调用当前不支持的 JDBC 能力
- **THEN** 驱动 SHALL 通过公共层返回明确的标准 JDBC 异常或标准默认值，而不是返回不一致或非预期异常

#### Scenario: 元数据空结果集形状一致
- **WHEN** 具体驱动需要返回 schemas、catalogs、table types、columns、primary keys 或 index info 等 DatabaseMetaData 结果集
- **THEN** 驱动 SHALL 复用公共层定义的标准列形状或扩展该标准形状

### Requirement: Common foundation remains Java 8 compatible
系统 SHALL 保持公共图 JDBC 基础模块与 Core 层 Java 1.8 编译约束兼容。

#### Scenario: Java 8 构建
- **WHEN** Core 层 Maven 构建公共图 JDBC 基础模块和两个具体驱动模块
- **THEN** 源码 SHALL 不依赖 Java 9+ API 或模块系统能力

```

## openspec/changes/extract-common-graph-jdbc/specs/nebula-jdbc-client-framework-integration/spec.md

- Source: openspec/changes/extract-common-graph-jdbc/specs/nebula-jdbc-client-framework-integration/spec.md
- Lines: 1-16
- SHA256: debac2903e7e27c760a3e493d79ecbf6d17cc568312d937328e51a218d25bb4b

```md
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

```

## openspec/changes/extract-common-graph-jdbc/specs/nebula-jdbc-stability/spec.md

- Source: openspec/changes/extract-common-graph-jdbc/specs/nebula-jdbc-stability/spec.md
- Lines: 1-16
- SHA256: b9e5a5e84db3ccf73f77e00d220dd4a3dba7fec837e7ce4c45855a3448a75174

```md
## ADDED Requirements

### Requirement: Nebula JDBC stability depends on common graph JDBC foundation
Nebula JDBC 稳定性能力 SHALL 依赖 `common-graph-jdbc-foundation` 提供的公共 JDBC 抽象标准，并在复用公共层后继续满足既有稳定性要求。

#### Scenario: 资源关闭稳定性通过公共层复用
- **WHEN** Nebula JDBC 的 Connection、Statement 或 ResultSet 接入公共资源状态或关闭辅助逻辑
- **THEN** 重复 close、关闭状态查询和异常语义 SHALL 继续满足 `nebula-jdbc-stability` 既有要求

#### Scenario: ResultSet 边界行为保持稳定
- **WHEN** Nebula JDBC 复用公共只读结果集、默认值或异常辅助逻辑
- **THEN** cursor 边界、wasNull 和列读取错误 SHALL 继续返回明确 JDBC 风格结果或异常，而不是非预期空指针失败

#### Scenario: 本地稳定性测试不依赖真实服务
- **WHEN** Nebula JDBC 稳定性测试验证公共层接入后的行为
- **THEN** 测试 SHALL 可以在没有真实 NebulaGraph 服务的本地环境中运行

```
