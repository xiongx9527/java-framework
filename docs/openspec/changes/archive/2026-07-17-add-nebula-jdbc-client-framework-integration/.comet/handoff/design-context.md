# Comet Design Handoff

- Change: add-nebula-jdbc-client-framework-integration
- Phase: design
- Mode: compact
- Context hash: 936f82e37461f16b19bbfa8cdb4880283e4f8a455eae5afc236d53b24d7c47b0

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-jdbc-client-framework-integration/proposal.md

- Source: openspec/changes/add-nebula-jdbc-client-framework-integration/proposal.md
- Lines: 1-26
- SHA256: caf886b7a7eff026491fb9e6e393e915d8dcf0fb1669997f9198fa34b5abd990

```md
## Why

用户要求 Nebula JDBC Driver 能像正常 JDBC Driver 一样使用，并被 MyBatis、Spring JdbcTemplate 等常见 Java JDBC 上层框架自然调用。虽然 SQL parser 和 SQL→nGQL 转换提供核心语义能力，但标准 JDBC 使用还依赖 DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet、ResultSetMetaData、DatabaseMetaData、参数绑定、列别名、类型映射和标准异常语义。

## What Changes

- 支持标准 JDBC 使用路径：DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet 和 metadata。
- 支持 Spring JdbcTemplate 常见查询路径。
- 支持 MyBatis Mapper XML / 注解 SQL 常见查询路径。
- 支持 PreparedStatement `?` 参数占位符和参数绑定。
- 支持 ResultSetMetaData、基础 DatabaseMetaData、列名、列别名和 JDBC 类型映射。
- 支持 RowMapper、Bean 映射和 MyBatis resultMap 所需行为。
- 明确 batch、事务、generated keys、CallableStatement、多结果集和完整 DatabaseMetaData 的边界，并使用标准 JDBC 异常表达不支持能力。

## Capabilities

### New Capabilities
- `nebula-jdbc-client-framework-integration`: 覆盖标准 JDBC 兼容基础，以及 MyBatis、JdbcTemplate 等 JDBC 上层框架验收行为。

### Modified Capabilities

## Impact

- 依赖 SQL parser core、SQL→nGQL 查询支持，写入场景依赖 SQL 写入与 DDL 支持。
- 影响 `fw-plus-nebula-jdbc` 的 PreparedStatement、ResultSet、ResultSetMetaData、Statement 执行路径。
- 可能需要补充测试依赖以覆盖 Spring JdbcTemplate 和 MyBatis 最小集成测试。

```

## openspec/changes/add-nebula-jdbc-client-framework-integration/design.md

- Source: openspec/changes/add-nebula-jdbc-client-framework-integration/design.md
- Lines: 1-48
- SHA256: 07cefec4eebb5e591f7514bcb6093ddc1666d152e75dbc669cb5394e6112ed85

```md
## Context

目标是让 Nebula JDBC Driver 像正常 JDBC Driver 一样使用。MyBatis 和 JdbcTemplate 都只是标准 JDBC 行为的上层消费者；只实现 SQL parser 或 MySQL 协议并不足以保证框架可用，还必须补齐 DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet、ResultSetMetaData、基础 DatabaseMetaData、列标签、JDBC 类型和异常语义。

## Goals / Non-Goals

**Goals:**

- 支持标准 JDBC 查询路径，包括 DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet 和 metadata。
- 支持 JdbcTemplate 常见查询和 RowMapper 映射。
- 支持 MyBatis XML / 注解 Mapper 常见查询和 resultMap 映射。
- 支持 PreparedStatement `?` 参数绑定。
- 支持列名、列别名、ResultSetMetaData、基础 DatabaseMetaData 和 JDBC 类型映射。
- 明确不支持能力的异常语义。

**Non-Goals:**

- 不做 MyBatis-Plus BaseMapper / Wrapper 全量适配。
- 不做 JPA/Hibernate。
- 不模拟完整关系型数据库事务。
- 不绕过 JDBC 直接接 MyBatis 插件。
- 不实现所有 DatabaseMetaData 方法，但常见探测方法必须可用或明确返回标准不支持异常。

## Decisions

- 主目标是标准 JDBC 兼容；MyBatis/JdbcTemplate 是验收框架，不做专用旁路。
- 框架 SQL 形态必须回补到 `define-nebula-sql-ast-coverage-matrix`，确保参数占位符和列别名能被 parser core 结构化。
- 查询兼容先于写入兼容；写入兼容依赖 `add-nebula-sql-write-and-ddl-support`。
- 不支持的 JDBC 能力必须明确失败，避免框架误判成功。

## Risks / Trade-offs

- **[Risk] 框架会调用大量 JDBC metadata 方法。** → Mitigation：先覆盖 MyBatis/JdbcTemplate 常见路径，其他方法明确返回不支持。
- **[Risk] 图模型与 POJO 映射存在差异。** → Mitigation：通过列别名、类型映射和 schema 映射约束明确结果形态。
- **[Risk] 动态 SQL 展开形态多样。** → Mitigation：parser 矩阵覆盖动态 SQL 展开后的常见 SQL，而不是 MyBatis XML 本身。

## Migration Plan

1. 等待 SQL parser core 和查询转换能力提供稳定 SQL 执行路径。
2. 补齐 PreparedStatement 参数绑定和 ResultSetMetaData。
3. 添加 JdbcTemplate 和 MyBatis 最小集成测试。
4. 根据测试发现补充 JDBC metadata 和类型映射。

## Open Questions

- 是否需要 Spring JDBC 和 MyBatis 作为 test scope 依赖？
- MyBatis 写入场景是否与 SQL 写入 change 同步验收？
- MyBatis-Plus 是否后续单独拆分 change？

```

## openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md

- Source: openspec/changes/add-nebula-jdbc-client-framework-integration/tasks.md
- Lines: 1-20
- SHA256: 6f636dbc4b9f7374d685f2fdd4d41fec833407643ac2b46ab4179c49bf5d5725

```md
## 1. 标准 JDBC 与框架兼容边界

- [ ] 1.0 梳理正常 JDBC 使用路径：DriverManager/DataSource、Connection、Statement、PreparedStatement、ResultSet、ResultSetMetaData 和 DatabaseMetaData。
- [ ] 1.1 梳理 JdbcTemplate 常见调用路径和所需 JDBC 方法。
- [ ] 1.2 梳理 MyBatis XML / 注解 Mapper 常见调用路径和所需 JDBC 方法。
- [ ] 1.3 定义不支持的 batch、generated keys、CallableStatement、事务、多结果集和完整 DatabaseMetaData 边界。

## 2. JDBC 行为补强

- [ ] 2.1 支持 PreparedStatement `?` 参数绑定和参数传递到 SQL parser / 转换层。
- [ ] 2.2 补齐 ResultSetMetaData、基础 DatabaseMetaData、列名、列别名和 JDBC 类型映射。
- [ ] 2.3 明确不支持 JDBC 能力的异常语义。

## 3. 标准 JDBC 与框架集成验证

- [ ] 3.0 补充标准 JDBC DriverManager/PreparedStatement/ResultSet/metadata 使用路径测试。
- [ ] 3.1 补充 JdbcTemplate queryForObject 和 RowMapper 测试。
- [ ] 3.2 补充 MyBatis XML Mapper 参数查询测试。
- [ ] 3.3 补充 MyBatis 注解 Mapper 和 resultMap / POJO 映射测试。
- [ ] 3.4 将 MyBatis/JdbcTemplate 常见 SQL 形态回补到 AST 覆盖矩阵。

```

## openspec/changes/add-nebula-jdbc-client-framework-integration/specs/nebula-jdbc-client-framework-integration/spec.md

- Source: openspec/changes/add-nebula-jdbc-client-framework-integration/specs/nebula-jdbc-client-framework-integration/spec.md
- Lines: 1-60
- SHA256: ea1255f9a874e7659d158f134ed60912e5b7cb794b3c83b54ff110312dfee390

```md
## ADDED Requirements

### Requirement: Standard JDBC usage paths are supported
系统 SHALL 支持像正常 JDBC Driver 一样使用 Nebula JDBC Driver 的常见路径，包括 DriverManager 或 DataSource 获取连接、创建 Statement/PreparedStatement、执行查询、读取 ResultSet 和关闭资源。

#### Scenario: DriverManager 查询
- **WHEN** 应用使用 DriverManager 获取 Nebula JDBC 连接并通过 Statement 执行 SELECT
- **THEN** Driver 返回可遍历 ResultSet，资源关闭行为符合 JDBC 清理习惯

#### Scenario: PreparedStatement 参数绑定
- **WHEN** 应用使用 PreparedStatement 和 `?` 参数执行查询
- **THEN** Driver 正确绑定参数、执行 SQL 并返回结果

#### Scenario: 不支持 JDBC 能力
- **WHEN** 应用调用当前不支持的事务、batch、generated keys、CallableStatement 或多结果集能力
- **THEN** Driver 返回标准 JDBC 异常，而不是静默成功或抛出非预期异常

### Requirement: JdbcTemplate can execute parameterized queries
系统 SHALL 支持 Spring JdbcTemplate 通过 Nebula JDBC Driver 执行带 `?` 参数的查询，并正确读取标量和行映射结果。

#### Scenario: queryForObject 标量查询
- **WHEN** JdbcTemplate 执行带 `?` 参数的 SELECT 标量查询
- **THEN** Nebula JDBC Driver 完成参数绑定、SQL 解析和结果读取，并返回目标标量值

#### Scenario: RowMapper POJO 映射
- **WHEN** JdbcTemplate 使用 RowMapper 读取包含列别名的查询结果
- **THEN** ResultSet 和 ResultSetMetaData 提供稳定列名、列别名和类型信息

### Requirement: MyBatis mapper queries can execute through JDBC
系统 SHALL 支持 MyBatis Mapper XML 和注解 SQL 通过 Nebula JDBC Driver 执行常见查询，并映射到 Java 对象。

#### Scenario: XML Mapper 参数查询
- **WHEN** MyBatis XML Mapper 使用 `#{id}` 参数生成 JDBC PreparedStatement 查询
- **THEN** Nebula JDBC Driver 正确处理 `?` 占位符和参数绑定并返回映射结果

#### Scenario: 注解 Mapper 查询
- **WHEN** MyBatis 注解 Mapper 执行带列别名的 SELECT 查询
- **THEN** 查询结果可映射到 Java POJO 属性

#### Scenario: 当前框架非目标边界
- **WHEN** 应用尝试通过当前能力验收 MyBatis 写入、MyBatis-Plus BaseMapper/Wrapper 或完整事务语义
- **THEN** 这些路径不属于当前能力验收范围；若调用触达当前不支持的 JDBC 能力，Driver 返回明确的标准 JDBC 异常

### Requirement: JDBC metadata supports framework mapping
系统 SHALL 提供标准 JDBC、MyBatis 和 JdbcTemplate 常见映射所需的 ResultSetMetaData、列标签、列类型和基础 DatabaseMetaData 行为。

#### Scenario: 列别名映射
- **WHEN** SQL 使用 `AS` 定义列别名
- **THEN** ResultSetMetaData 和 ResultSet 读取方法返回标准 JDBC 客户端和框架可识别的列标签

#### Scenario: 不支持 JDBC 能力
- **WHEN** 框架调用当前不支持的 batch、generated keys、CallableStatement 或复杂事务能力
- **THEN** Driver 返回明确的 SQLFeatureNotSupportedException 或 SQLException，而不是静默失败

### Requirement: Framework SQL shapes are covered by parser matrix
系统 SHALL 确保 AST 覆盖矩阵包含 MyBatis 和 JdbcTemplate 常见 SQL 形态，例如参数占位符、列别名、IN 条件和动态 SQL 展开后的条件组合。

#### Scenario: 参数占位符被覆盖
- **WHEN** 后续 parser core 验证 AST 覆盖矩阵
- **THEN** `?` 参数、IN 参数列表和列别名相关 SQL 形态均有矩阵条目

```
