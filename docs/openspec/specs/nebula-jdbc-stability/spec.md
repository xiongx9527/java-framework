# nebula-jdbc-stability Specification

## Purpose
TBD - created by archiving change stabilize-nebula-jdbc. Update Purpose after archive.
## Requirements
### Requirement: Parsed JDBC configuration is used consistently
The Nebula JDBC implementation SHALL use the merged configuration produced from the JDBC URL and input `Properties` as the single source of truth for graph space, authentication, host addresses, and connection pool parameters.

#### Scenario: URL query parameters configure the connection
- **WHEN** a JDBC URL includes graph space and connection pool query parameters
- **THEN** the parsed graph space and pool parameters are available to the connection initialization logic

#### Scenario: Input properties and URL properties are merged
- **WHEN** connection properties are supplied both through the JDBC URL and the input `Properties`
- **THEN** the connection uses the parser-defined merged configuration consistently instead of mixing merged and raw property sources

### Requirement: Connection pool numeric parameters are parsed safely
The Nebula JDBC implementation SHALL parse connection pool numeric parameters from supported property value types and SHALL report invalid numeric values with a clear `SQLException`.

#### Scenario: Numeric parameters are strings
- **WHEN** connection pool parameters such as `minConnsSize`, `maxConnsSize`, `timeout`, `idleTime`, `intervalIdle`, or `waitTime` are provided as strings
- **THEN** the driver converts them to integer values without a `ClassCastException`

#### Scenario: Numeric parameter is invalid
- **WHEN** a connection pool numeric parameter contains a non-numeric value
- **THEN** the driver fails with a `SQLException` that identifies the invalid parameter

### Requirement: JDBC resources close reliably
The Nebula JDBC implementation SHALL make `Connection`, `Statement`, and `ResultSet` close operations stable for common JDBC cleanup patterns, including repeated close calls.

#### Scenario: Connection is closed more than once
- **WHEN** a caller invokes `Connection.close()` repeatedly
- **THEN** the driver does not throw a non-essential exception after the resource is already closed

#### Scenario: Statement and ResultSet are closed more than once
- **WHEN** a caller invokes `Statement.close()` or `ResultSet.close()` repeatedly
- **THEN** the operation remains stable and the resource reports itself as closed

### Requirement: Statement execution type classification handles common formatting
The Nebula JDBC implementation SHALL classify supported nGQL query and update statements after normalizing common whitespace and case variations.

#### Scenario: Query statement contains mixed case and line breaks
- **WHEN** `executeQuery` receives a supported query nGQL statement with mixed case, line breaks, or repeated spaces
- **THEN** the statement is accepted as a query statement

#### Scenario: Update statement contains mixed case and line breaks
- **WHEN** `executeUpdate` receives a supported update nGQL statement with mixed case, line breaks, or repeated spaces
- **THEN** the statement is accepted as an update statement

#### Scenario: Unsupported statement type is provided
- **WHEN** `executeQuery` or `executeUpdate` receives a statement that does not match the supported category
- **THEN** the driver raises a clear `SQLException` describing the expected statement type

### Requirement: ResultSet boundary behavior avoids unexpected null pointer failures
The Nebula JDBC implementation SHALL handle ResultSet cursor and null-state boundary operations with explicit JDBC-style outcomes instead of unexpected null pointer failures.

#### Scenario: wasNull is called before reading a column
- **WHEN** a caller invokes `wasNull()` before any column value has been read
- **THEN** the driver returns a stable JDBC-style result or raises a clear `SQLException` instead of throwing a `NullPointerException`

#### Scenario: Cursor is outside a readable row
- **WHEN** a caller reads a column while the cursor is before the first row or after the last row
- **THEN** the driver raises a clear `SQLException` describing the invalid cursor position

### Requirement: Stability behavior is covered by local tests
The Nebula JDBC module SHALL include local automated tests for the stabilized behavior without requiring a live NebulaGraph service.

#### Scenario: Unit tests run without NebulaGraph
- **WHEN** the `fw-plus-nebula-jdbc` module test suite is executed in a local development environment without a NebulaGraph server
- **THEN** tests for URL/config parsing, numeric conversion, statement classification, resource closing, and ResultSet boundaries can complete successfully

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

