# Comet Design Handoff

- Change: add-nebula-sql-parser-core
- Phase: design
- Mode: compact
- Context hash: e2c775ba25ac17fe9813b3c0bedb339877f981ef532215927a0983ce21f2ce8a

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-nebula-sql-parser-core/proposal.md

- Source: openspec/changes/add-nebula-sql-parser-core/proposal.md
- Lines: 1-26
- SHA256: 42030c3a77c1cb82a50faed02a7482fba97f73a8caff06c0b140230feb383b52

```md
## Why

当前 Nebula JDBC 只稳定了 nGQL 直连基础能力，尚不能接受用户熟悉的 SQL 语句。要支持后续 SQL→nGQL 转换和 MySQL 协议接入，必须先建立独立、可测试的 SQL 解析核心。

## What Changes

- 新增 SQL parser 核心能力，输出稳定的 SQL AST 或中间表示。
- 覆盖 SELECT、INSERT、UPDATE、DELETE、基础 DDL 以及复杂 SQL 构造（多表 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名）的语法识别框架。
- 提供方言边界、标识符、字面量、表达式、表/列引用、参数占位符、复杂查询结构和嵌套查询解析。
- 对非法 SQL 或暂不支持的语法返回明确错误，不直接执行 Nebula 查询。

## Capabilities

### New Capabilities
- `nebula-sql-parser-core`: SQL 解析核心，覆盖本 change 的需求、边界和验收场景。

### Modified Capabilities

## Impact

- 依赖前置 change `define-nebula-sql-ast-coverage-matrix` 提供 Druid AST 覆盖矩阵和验收边界。

- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 影响 `fw-plus-nebula-jdbc` 或新建相邻 SQL 子包。
- 可能引入 SQL parser 依赖或自研轻量 parser，需要在 design 阶段决策。
- 为后续 SQL→nGQL 和 MySQL 协议 change 提供基础能力。

```

## openspec/changes/add-nebula-sql-parser-core/design.md

- Source: openspec/changes/add-nebula-sql-parser-core/design.md
- Lines: 1-47
- SHA256: 6bdd946d37103bf872a9f709e63262b08716abbdc45a1df388540a0a955b6070

```md
## Context

本 change 属于 Nebula SQL / MySQL 协议支持路线的一部分。当前 `fw-plus-nebula-jdbc` 已具备稳定的 nGQL JDBC 基础能力，但完整 SQL 与 MySQL 协议能力需要分阶段建设。

前置依赖：`define-nebula-sql-ast-coverage-matrix`。本 change 按该矩阵实现 Druid MySQL AST 到 Nebula AST 的完整 adapter。

## Goals / Non-Goals

**Goals:**

- 新增 SQL parser 核心能力，输出稳定的 SQL AST 或中间表示，并能表达复杂 SQL 查询结构。
- 覆盖 SELECT、INSERT、UPDATE、DELETE、基础 DDL 以及 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式、别名等复杂 SQL 构造的语法识别框架。
- 提供方言边界、标识符、字面量、表达式、表/列引用和参数占位符解析。
- 对非法 SQL 或暂不支持的语法返回明确错误，不直接执行 Nebula 查询。

**Non-Goals:**


- MySQL binlog、复制协议、存储过程和触发器不在本 change 内实现；这些能力由后续独立 changes 承接。
- 不扩大到本拆分项之外的后续能力。
- 不破坏现有 `jdbc:nebula://...` nGQL 直连路径。
- 不追求 100% MySQL Server 或 SQL 标准兼容。

## Decisions

- 按 `define-nebula-sql-ast-coverage-matrix` 的覆盖矩阵实现完整 adapter，parser 输出为与执行层解耦的中间表示，避免 MySQL 协议、JDBC 和转换器直接依赖 Druid AST。
- SQL 方言能力采用分层白名单：parser 尽量完整表达复杂 SQL 结构，转换层再决定哪些语义可映射到 nGQL，不能映射时返回明确错误。
- 错误模型使用 JDBC 可传播的异常类型或内部解析异常，并保留原始 SQL 与错误位置。

## Risks / Trade-offs

- **[Risk] 复杂 SQL 与图模型语义存在天然差异。** → Mitigation：parser 保留复杂结构，转换层采用明确白名单和可测试映射，不支持时返回清晰错误。
- **[Risk] 客户端兼容范围可能快速扩大。** → Mitigation：按 change 分阶段交付，每阶段定义验收客户端和协议子集。
- **[Risk] 引入第三方 parser 或协议库可能带来许可证和 Java 8 兼容风险。** → Mitigation：在 design/build 阶段先做依赖评估，再落地实现。

## Migration Plan

1. 在本 change 内完成设计与测试边界确认。
2. 以不破坏现有 nGQL 直连能力为前提新增能力。
3. 每个后续 change 只依赖前置产物的稳定 API，不回头扩大前置范围。
4. 如发现范围过大，优先继续拆分而不是在单个 change 中堆叠。

## Open Questions

- 是否采用第三方 parser / protocol 库，或自研轻量实现？
- SQL schema 与 Nebula tag/edge/property/id 的最终映射配置放在哪一层？
- 是否需要 Spring Boot starter、Magic-API 插件或独立服务入口？

```

## openspec/changes/add-nebula-sql-parser-core/tasks.md

- Source: openspec/changes/add-nebula-sql-parser-core/tasks.md
- Lines: 1-18
- SHA256: 76876e48daad60399f20c7093caf0066db09f548d997aaab97d67ad527d18692

```md
## 1. 调研与架构决策

- [ ] 1.0 读取 `define-nebula-sql-ast-coverage-matrix` 产出的 Druid AST 覆盖矩阵，并确认实现范围。
- [ ] 1.1 评估自研 parser 与第三方 SQL parser 的兼容性、许可证、Java 8 支持和 AST 可控性。
- [ ] 1.2 定义 SQL AST / 中间表示和错误模型。

## 2. Parser 核心实现

- [ ] 2.1 实现 SELECT 基础语法解析。
- [ ] 2.2 实现 INSERT、UPDATE、DELETE 基础语法解析。
- [ ] 2.3 实现基础 DDL 语法解析。
- [ ] 2.4 实现 JOIN、子查询、聚合、GROUP BY、HAVING、UNION、函数、表达式和别名等复杂 SQL 构造解析。
- [ ] 2.5 实现非法语法和不支持语法的明确错误。

## 3. 测试与文档

- [ ] 3.1 补充 parser 单元测试，覆盖读写、DDL、复杂 SQL、非法 SQL 和暂不支持语法。
- [ ] 3.2 记录第一阶段 SQL 方言边界和后续转换层依赖。

```

## openspec/changes/add-nebula-sql-parser-core/specs/nebula-sql-parser-core/spec.md

- Source: openspec/changes/add-nebula-sql-parser-core/specs/nebula-sql-parser-core/spec.md
- Lines: 1-46
- SHA256: a794538590a7f402a0347276a00222e32f72aa64b54cd895cd0480fa5ba3575f

```md
## ADDED Requirements

### Requirement: SQL parser exposes a stable intermediate representation
系统 SHALL 将受支持 SQL 解析为稳定的 AST 或中间表示，供后续转换层消费。

#### Scenario: 解析 SELECT 语句
- **WHEN** 输入 `SELECT * FROM person WHERE id = 'p1'` 
- **THEN** 解析结果包含语句类型、目标表、投影列和过滤条件。

#### Scenario: 解析写入和 DDL 语句
- **WHEN** 输入基础 INSERT、UPDATE、DELETE 或 CREATE TABLE 语句
- **THEN** 解析结果标识语句类型和关键结构。

### Requirement: Complex SQL constructs are represented explicitly
系统 SHALL 解析复杂 SQL 构造，并在中间表示中显式表达 JOIN、子查询、聚合、分组、HAVING、UNION、函数、表达式和别名等结构。

#### Scenario: 解析多表 JOIN 与别名
- **WHEN** 输入包含多表 JOIN、表别名和列别名的 SELECT 语句
- **THEN** 解析结果包含 JOIN 类型、JOIN 条件、别名映射和投影结构

#### Scenario: 解析子查询和聚合
- **WHEN** 输入包含子查询、聚合函数、GROUP BY 或 HAVING 的 SELECT 语句
- **THEN** 解析结果保留嵌套查询、聚合表达式、分组字段和 HAVING 条件

#### Scenario: 解析 UNION 和函数表达式
- **WHEN** 输入包含 UNION、内置函数或复杂表达式
- **THEN** 解析结果能区分集合操作、函数调用和表达式树

### Requirement: Unsupported SQL reports clear errors
系统 SHALL 对语法错误或暂不支持的 SQL 返回明确、可定位的异常信息。

#### Scenario: 非法 SQL
- **WHEN** 输入无法解析的 SQL 
- **THEN** 返回包含错误位置或错误原因的异常。

#### Scenario: 暂不支持语法
- **WHEN** 输入 parser 识别但当前不支持的 SQL 特性
- **THEN** 返回明确的不支持原因。

### Requirement: Parser does not execute Nebula queries
SQL parser 核心 SHALL 只负责解析与基础语义校验，不直接访问 NebulaGraph。

#### Scenario: 纯解析调用
- **WHEN** 调用 parser API 
- **THEN** 不创建 Nebula 连接、不调用 `NebulaClientFacade`，只返回解析结果或错误。


```
