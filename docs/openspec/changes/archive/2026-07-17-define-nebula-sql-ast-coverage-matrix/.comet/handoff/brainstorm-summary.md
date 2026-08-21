# Brainstorm Summary

- Change: define-nebula-sql-ast-coverage-matrix
- Date: 2026-07-16

## 已确认上下文

- 本 change 使用完整 Comet workflow，当前处于 design 阶段。
- OpenSpec canonical spec 是 `openspec/changes/define-nebula-sql-ast-coverage-matrix/specs/nebula-sql-ast-coverage-matrix/spec.md`。
- 目标是交付 Druid MySQL AST 到 Nebula AST / 中间表示的覆盖矩阵、版本锁定规则和 adapter 完整性测试策略。
- 非目标是不实现 parser adapter、不实现 SQL 到 nGQL 转换、不实现 MySQL 协议、binlog、routine 或 trigger。
- 后续 `add-nebula-sql-parser-core` 明确依赖本 change 产出的覆盖矩阵。
- 当前 `fw-plus-nebula-jdbc` 主要是 nGQL JDBC 连接、Statement、PreparedStatement、ResultSet 等基础能力，未发现已落地的 SQL AST adapter 体系。
- 仓库已有 `com.alibaba:druid` / `druid-spring-boot-starter` 版本管理，core BOM 中当前 `druid.version` 为 `1.2.24`。
- 用户回复“继续”后，设计默认采用最贴近当前仓库的路径：覆盖矩阵基线锁定 `com.alibaba:druid:1.2.24`，并在 build 阶段通过依赖 POM / 构建验证补充许可证与 Java 8 兼容性证据。

## 候选设计方向

- 已确认：采用方案 A，矩阵以机器可读 JSON/YAML 资源作为事实源，Markdown 文档作为人类可读说明。
- 已采用默认假设：Druid SQL Parser 版本基线锁定为现有 BOM 管理的 `1.2.24`。
- 已确认：AST 类型枚举采用“样例 SQL 驱动 + 反射扫描兜底”的组合。

## 确认的技术方案

- 产物结构：以 `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json` 作为机器可读事实源，以同目录 Markdown 文档说明矩阵阅读、维护和后续 parser-core 消费方式。
- JSON 字段包含 Druid 版本、Java 8 兼容性验证、许可证验证、AST 分类、矩阵条目和 Druid 升级策略。
- Markdown 不作为测试事实源，只作为审阅与维护说明。
- AST 枚举策略：样例 SQL 覆盖主要 SQL 构造并记录 Druid MySQL parser 实际产出的 AST 类型；反射扫描 Druid AST / MySQL dialect 相关类作为缺口检查，反射命中不自动等于必须适配，但必须进入矩阵或记录排除理由。
- 矩阵条目字段至少包含 `id`、`category`、`druidAstType`、`sqlConstruct`、`exampleSql`、`nebulaAstKind`、`adapterRequirement`、`supportLevel`、`testCaseId`、`rawFallbackAllowed=false` 和 `notes`。
- 支持级别使用 `required`、`explicitlyUnsupported`、`deferred`：分别表示后续 parser-core 必须适配、必须返回结构化不支持错误、明确延期并记录原因。
- 后续 `add-nebula-sql-parser-core` 必须消费 JSON 矩阵作为 adapter 完整性测试输入；矩阵内节点缺 adapter 或 raw SQL 被当作语义 fallback 时测试失败。
- Druid 升级必须重新运行样例 SQL 枚举和反射缺口检查，并重新评估矩阵版本元信息或 hash 对 parser-core 验收边界的影响。

## 关键取舍与风险

- 已确认取舍：避免只写 Markdown 导致后续 parser-core 测试难以消费；同时避免只有 JSON/YAML 难以审阅，因此采用机器可读矩阵 + Markdown 说明的组合。
- 已确认风险：Druid AST 类型数量较多，设计需要避免承诺“所有内部类全量适配”，同时满足“不遗漏 Druid MySQL parser 已能解析的关键 AST 类型”的验收要求。
- 已确认测试策略：后续 parser-core 以 JSON 矩阵作为测试输入，矩阵内 `required` 条目缺 adapter 时测试失败；raw SQL 只允许作为 debug/trace 字段，不允许作为语义 fallback。
- 已确认支持级别：矩阵条目使用 `required`、`explicitlyUnsupported`、`deferred` 三类支持级别，分别表示必须适配、结构化不支持错误、明确延期且写明原因。

## 测试策略

- 已确认方向：后续 parser-core 应能读取矩阵或由矩阵生成测试输入，并在矩阵内 Druid AST 类型缺失 adapter、或 raw SQL 被当作语义 fallback 时失败。
- 失败信息应包含 `matrix id`、`druidAstType`、`exampleSql`，便于从测试直接定位矩阵条目。
- Druid 升级时必须重新运行样例 SQL 枚举和反射缺口检查；矩阵版本元信息或 hash 变化后，parser-core 验收边界必须重新评估。

## Spec Patch

- 将回写 delta spec：补充“矩阵 artifact 是机器可读事实源，并附带人类可读说明”的验收场景。
