# Comet Design Handoff

- Change: stabilize-nebula-jdbc
- Phase: design
- Mode: compact
- Context hash: cc59e56318bd82296647282f6b0402c55444a1c8eb6fdc6dffc9f6c214341f6e

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/stabilize-nebula-jdbc/proposal.md

- Source: openspec/changes/stabilize-nebula-jdbc/proposal.md
- Lines: 1-29
- SHA256: ae7edbbe0f320f3913280e24824327154dd3ff321c3960b8ce509eb56ede2f51

```md
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

```

## openspec/changes/stabilize-nebula-jdbc/design.md

- Source: openspec/changes/stabilize-nebula-jdbc/design.md
- Lines: 1-96
- SHA256: d5ce634ca418b2fbc2199c0b96a92fb93968a7eba09d1139830767ef0a74c9ba

[TRUNCATED]

```md
## Context

`fw-plus-nebula-jdbc` 是 `fw-plus-nebula` 模块当前唯一子模块，用于将 NebulaGraph Java client 包装为 JDBC 风格接口。现有实现已经提供 `Driver`、`Connection`、`Statement`、`PreparedStatement`、`ResultSet` 和 metadata 基础类，但存在以下稳定性问题：

- `NebulaConnection` 解析 JDBC URL 后保存到 `this.properties`，但部分后续逻辑仍使用传入的原始 `Properties`，可能导致 URL 中解析出的 `graphspace`、认证信息或连接池参数未生效。
- 连接池参数通过强制 `(int)` 转换读取，而 `Properties` 的值通常是字符串，容易在运行时触发类型转换异常。
- 资源关闭语义不够稳定，重复关闭连接可能抛出异常，不利于常见 JDBC 调用模式和 try-with-resources 场景。
- `Statement` 通过简单空格拆分判断查询或更新语句，对换行、多空白和大小写混合语句不够稳健。
- `ResultSet` 游标和 `wasNull()` 边界行为可能产生非预期异常或 NPE。
- 关键行为缺少不依赖真实 NebulaGraph 服务的单元测试。

本 change 只稳定现有 JDBC 基础层，为后续 SQL 转 nGQL 和 MySQL 协议网关提供可验证的基础。

## Goals / Non-Goals

**Goals:**

- 保持现有 `com.vesoft.nebula.jdbc` 包名和主要 JDBC API 兼容。
- 统一使用解析后的连接配置，确保 JDBC URL、query 参数和传入 `Properties` 的合并结果进入连接逻辑。
- 为连接池整数参数提供集中、安全的解析逻辑，并对非法值给出明确 `SQLException`。
- 改善资源关闭和边界行为，使其更接近 JDBC 常见预期并避免非预期 NPE。
- 改善 nGQL 语句分类逻辑，使 `executeQuery` / `executeUpdate` 对常见格式更稳定。
- 补充不依赖真实 NebulaGraph 服务的单元测试覆盖关键稳定性场景。

**Non-Goals:**

- 不实现 SQL 转 nGQL。
- 不实现 MySQL wire protocol 或独立服务端。
- 不改动 Boot、Cloud、AI、Product 层。
- 不要求连接真实 NebulaGraph 服务进行集成测试。
- 不默认升级 Nebula Java client 大版本。
- 不追求完整 JDBC 规范覆盖，只修复本模块基础稳定性和后续扩展所需的关键行为。

## Decisions

### 1. 以解析后的连接配置作为唯一连接输入

连接初始化应先调用 `NebulaJdbcUrlParser.parse(url, properties)` 得到合并配置，然后后续 `graphspace`、用户名、密码、连接池参数等均从该合并配置读取。

**理由：** 当前代码中解析结果和原始参数混用，容易造成 URL query 参数被忽略。统一配置来源可以减少歧义，也便于单元测试。

**备选方案：** 保持原逻辑，只修复单个字段读取。该方案改动小，但未来新增参数时仍容易重复出现混用问题，因此不采用。

### 2. 为连接池数值参数提供集中解析方法

连接池参数应通过一个小型工具方法从 `Properties` 中读取，支持 `Number` 和 `String` 两类常见值；为空时使用默认值；无法解析时抛出带参数名的 `SQLException`。

**理由：** `Properties` 常以字符串存储值，直接 `(int)` 转换不安全。集中解析可以统一默认值、错误消息和测试覆盖。

**备选方案：** 在每个参数读取处分别处理。该方案重复代码多，错误信息不一致，因此不采用。

### 3. 资源关闭保持稳定且尽量幂等

`Connection.close()`、`Statement.close()`、`ResultSet.close()` 应允许重复调用不产生非预期失败；关闭过程中对底层 session/pool 的释放需保留必要保护。

**理由：** JDBC 使用者常通过 try-with-resources 或 finally 重复触发关闭。重复关闭抛错会放大资源清理路径的风险。

**备选方案：** 严格保持当前第二次关闭抛异常。该行为与常见 JDBC 经验不符，也不利于上层协议网关复用，因此不采用。

### 4. 语句分类只做轻量规范化，不引入 SQL/nGQL parser

`executeQuery` / `executeUpdate` 的分类逻辑应在现有关键字集合基础上，对输入做 trim、小写化和连续空白规范化，支持换行、多空白和大小写混合；暂不引入完整 parser。

**理由：** 本 change 的目标是稳定现有 nGQL 执行层，而 SQL 转 nGQL 会在后续独立 change 中处理。轻量规范化能解决当前明显问题，避免扩大范围。

**备选方案：** 引入完整 nGQL parser 或 SQL parser。该方案超出本 change 范围，也会与后续 SQL 转换 change 重叠，因此不采用。

### 5. 单元测试优先覆盖纯逻辑和可隔离边界

测试应优先覆盖 URL parser、配置转换、语句分类、资源关闭和 ResultSet 边界行为；对真实 Nebula session/pool 的交互不作为本 change 的强制集成测试。

**理由：** 当前未知真实 NebulaGraph 测试环境，纯逻辑测试更稳定，也能作为后续重构安全网。

**备选方案：** 只做人工验证或要求真实 NebulaGraph 环境。该方案无法稳定在本地和 CI 复现，因此不采用。

## Risks / Trade-offs

- **[Risk] 资源关闭语义调整可能改变少数调用方对重复关闭异常的依赖。** → Mitigation：重复关闭幂等是更常见的 JDBC 预期，且不改变首次关闭释放资源的行为。
- **[Risk] 语句分类仍不是完整 nGQL 语法识别。** → Mitigation：明确限制为轻量规范化；复杂 SQL/nGQL 转换留给后续 `add-sql-to-ngql-translator`。
- **[Risk] 不做真实 NebulaGraph 集成测试可能遗漏 client 交互问题。** → Mitigation：本 change 聚焦可本地验证的稳定性问题；后续可在 verify 阶段或单独 change 增加集成测试 profile。

```

Full source: openspec/changes/stabilize-nebula-jdbc/design.md

## openspec/changes/stabilize-nebula-jdbc/tasks.md

- Source: openspec/changes/stabilize-nebula-jdbc/tasks.md
- Lines: 1-25
- SHA256: 112bf72066e66b787c9325c9ac422a939cb04692d4b21168d84d0ab58fbdfd9a

```md
## 1. 测试基线与覆盖补强

- [ ] 1.1 确认 `fw-plus-nebula-jdbc` 当前 Maven 测试配置，补充必要的单元测试依赖但不引入真实 NebulaGraph 集成环境要求
- [ ] 1.2 为 JDBC URL 解析和连接配置合并行为补充单元测试
- [ ] 1.3 为连接池数字参数解析的字符串值、默认值和非法值场景补充单元测试
- [ ] 1.4 为 Statement 查询/更新语句分类的大小写、换行、多空白和非法类型场景补充单元测试
- [ ] 1.5 为资源重复关闭和 ResultSet 边界行为补充单元测试或可隔离验证

## 2. 连接配置与连接池参数稳定化

- [ ] 2.1 调整 `NebulaConnection` 初始化逻辑，统一使用 `NebulaJdbcUrlParser.parse` 后的合并配置
- [ ] 2.2 实现连接池整数参数的集中解析逻辑，支持字符串和数字类型并提供明确异常
- [ ] 2.3 确认 `graphspace`、用户名、密码和连接池参数均来自同一合并配置来源

## 3. JDBC 基础行为稳定化

- [ ] 3.1 调整 `Connection.close()`、`Statement.close()`、`ResultSet.close()` 的重复关闭行为，避免非必要异常
- [ ] 3.2 改善 `NebulaStatementImpl` 的 nGQL 类型分类逻辑，支持常见空白和大小写格式
- [ ] 3.3 改善 `NebulaResultSet` 游标边界和 `wasNull()` 的异常语义，避免非预期 NPE

## 4. 验证与收尾

- [ ] 4.1 运行 `fw-plus-nebula-jdbc` 模块测试并修复失败
- [ ] 4.2 检查变更不引入 SQL 转 nGQL、MySQL wire protocol 或跨层改动
- [ ] 4.3 更新任务勾选并准备进入 Comet design 阶段

```

## openspec/changes/stabilize-nebula-jdbc/specs/nebula-jdbc-stability/spec.md

- Source: openspec/changes/stabilize-nebula-jdbc/specs/nebula-jdbc-stability/spec.md
- Lines: 1-67
- SHA256: 955430d617f01585e858e4d147e2762c1258dcb82c2b311f86f632ce37b959e5

```md
## ADDED Requirements

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

```
