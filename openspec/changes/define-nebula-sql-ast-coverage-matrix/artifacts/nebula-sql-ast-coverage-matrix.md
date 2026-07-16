# Nebula SQL AST 覆盖矩阵

## 事实源

JSON 文件是后续 parser-core 完整性测试消费的机器可读事实源。本文档只解释矩阵字段、覆盖边界和维护流程。

## Druid 基线

- GroupId: `com.alibaba`
- ArtifactId: `druid`
- Version: `1.2.24`
- Version source: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- 许可证和 Java 8 兼容性：待 Maven 解析或构建结果补充证据。

## 字段说明

- `schemaVersion` 固定标识矩阵结构版本。
- `druid` 记录解析器依赖坐标、来源和验证状态。
- `artifacts` 记录双产物路径。
- `supportLevels` 定义条目的支持级别枚举。
- `categories`、`entries` 和 `reflectedCandidates` 由后续枚举任务填充。
- `enumeration` 描述样例 SQL 驱动枚举和反射缺口检查的范围。
- `parserCoreConsumption` 定义测试事实源和失败诊断字段。
- `upgradePolicy` 定义 Druid 升级后的重新验证动作。

## 分类

分类和具体 AST 条目由后续任务根据 Druid 样例 SQL 及反射结果补充。

## 样例 SQL 枚举

枚举应以可执行样例 SQL 为入口，覆盖 `com.alibaba.druid.sql.ast` 与 `com.alibaba.druid.sql.dialect.mysql.ast`，并记录 SQL 到 AST 类型的映射。

## 反射缺口检查

反射检查用于发现 Druid AST 包中未被样例 SQL 枚举覆盖的候选类型。候选类型进入 `reflectedCandidates`，再由维护者决定支持、明确不支持或延期。

## 支持级别

- `required`：parser-core 必须支持并纳入完整性测试。
- `explicitlyUnsupported`：明确不支持，并保留原因和诊断信息。
- `deferred`：暂不纳入当前实现，但必须可追踪。

## Raw Fallback 禁止规则

所有未来矩阵条目，无论其 `supportLevel`、来源或当前是否纳入 parser-core，都必须显式包含 `"rawFallbackAllowed": false`。任何 AST 类型都不得通过 Raw Fallback 绕过结构化解析；raw SQL 仅允许出现在 debug、trace 或 error context 中，不得作为语义 fallback 返回或继续处理。解析失败信息至少包含条目 `id`、`druidAstType` 和 `exampleSql`。

## Parser-Core 消费方式

parser-core 完整性测试只消费 JSON；Markdown 用于解释字段、边界和维护流程，不作为测试事实源。

## Druid 升级规则

Druid 版本升级后必须重新执行样例 SQL 枚举、反射缺口检查，并重新评估 parser-core 的接受边界；同时补充许可证和 Java 8 兼容性证据。
