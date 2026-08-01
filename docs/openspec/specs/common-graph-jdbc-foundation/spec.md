# common-graph-jdbc-foundation Specification

## Purpose
TBD - created by archiving change extract-common-graph-jdbc. Update Purpose after archive.
## Requirements
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

