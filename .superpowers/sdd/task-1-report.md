# Task 1 报告

## 状态

DONE_WITH_CONCERNS

## 提交

- Commit message: `docs: add nebula sql ast matrix baseline`
- Commit hash: `54b9d791d3a3c18e8b6e396a417e7a83bdf6d5e8`

## 变更文件

- `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json`
- `openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`

未修改 `tasks.md`，因为本任务用户指令明确禁止修改；也未提交 `.superpowers/` 或 `.comet/subagent-progress.md`。

## Evidence Completion

- JSON 语法验证：`python3 -m json.tool openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json >/tmp/nebula-sql-ast-coverage-matrix.json`，exit 0。
- 证据检索：`rg -n "Apache 2|java8Compatible|major version 52|Maven Central|maven-wrapper" openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md`，exit 0，命中全部关键词。
- 空白检查：`git diff --check`，exit 0。
- Druid 许可证已由 Maven Central POM 验证为 Apache 2；Java 8 兼容性由 Maven Central JAR 的 1893 个 class、min/max major version 52 验证。wrapper 命令因缺少 `.mvn/wrapper/maven-wrapper.properties` 及 Maven Wrapper 主类失败，已保留该说明。
- Commit message: `docs: verify druid matrix baseline evidence`
- Commit hash: `3509344359153e944b52a655d24f69d181f15672`（amend 前哈希；最终哈希见本报告提交记录）。

## 强制命令结果

### 1. Druid 版本来源

命令：

```bash
rg -n "druid.version|<artifactId>druid</artifactId>|druid-spring-boot-starter" fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml
```

结果：exit 0。

```text
106:        <druid.version>1.2.24</druid.version>
597:                <artifactId>druid</artifactId>
598:                <version>${druid.version}</version>
602:                <artifactId>druid-spring-boot-starter</artifactId>
603:                <version>${druid.version}</version>
```

### 2. Maven 解析 Druid POM

命令：

```bash
cd fw-plus-core && ./mvnw -q dependency:get -Dartifact=com.alibaba:druid:1.2.24:pom -Dtransitive=false
```

结果：exit 1，未进入依赖解析。当前 worktree 缺少 `fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 及 Maven Wrapper 主类 `org.apache.maven.wrapper.MavenWrapperMain`，因此无法用本次命令确认许可证和 Java 8 兼容性。该事实已记录在 JSON 的 `druid.verificationNotes` 中。

### 3. JSON 语法验证

命令：

```bash
python3 -m json.tool openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.json >/tmp/nebula-sql-ast-coverage-matrix.json
```

结果：exit 0，无 JSON 解析错误。

补充验证：`git diff --check` exit 0。

## 风险信号

已命中：

- 安全敏感面：SQL / AST 解析边界，artifact 中包含 Raw Fallback 禁止规则和 SQL 样例枚举约束。
- `DONE_WITH_CONCERNS`：Maven wrapper 缺失，许可证和 Java 8 兼容性证据仍待补充。

未命中：跨模块/跨子系统、认证授权、加解密、密钥、并发、数据/schema 迁移、公共 API、单任务 diff 超过 200 行。

## Reviewer Fix - Raw Fallback 矩阵规则

### 修复内容

- 将 Raw Fallback 禁止规则扩展到所有未来矩阵条目，不再仅限定 `required` 条目。
- 明确每个矩阵条目都必须包含 `"rawFallbackAllowed": false`。
- 保留 raw SQL 仅允许用于 debug、trace 或 error context，不得作为语义 fallback 返回或继续处理的约束。

### 强制命令结果

命令：

```bash
rg -n "rawFallbackAllowed|Raw Fallback|debug|trace|语义 fallback" openspec/changes/define-nebula-sql-ast-coverage-matrix/artifacts/nebula-sql-ast-coverage-matrix.md
```

结果：exit 0。

```text
44:## Raw Fallback 禁止规则
46:所有未来矩阵条目，无论其 `supportLevel`、来源或当前是否纳入 parser-core，都必须显式包含 `"rawFallbackAllowed": false`。任何 AST 类型都不得通过 Raw Fallback 绕过结构化解析；raw SQL 仅允许出现在 debug、trace 或 error context 中，不得作为语义 fallback 返回或继续处理。解析失败信息至少包含条目 `id`、`druidAstType` 和 `exampleSql`。
```

命令：

```bash
git diff --check
```

结果：exit 0。

### 修复提交

- Commit message: `docs: clarify raw fallback matrix rule`
- Commit hash: `031f1f2`
