# Task 5 实施报告

## 实现内容

- 新增 SQL 校验接口、结果对象和默认实现：仅接受单条 `SELECT`/`WITH` 查询，忽略字符串字面量与注释中的关键字，拒绝危险关键字和多语句，并为无顶层 `LIMIT` 的查询追加行数限制。
- 新增 JDBC SQL 执行接口、结果对象和实现：通过 `JdbcTemplate` 提取列标签、SQL 类型、行数据和行数；缺少 `JdbcTemplate` 时返回 `SQL_EXECUTION_FAILED`。
- 在 `AskDataAutoConfiguration` 注册可覆盖的 SQL 校验器和执行器 bean；执行器通过 `ObjectProvider<JdbcTemplate>` 延迟获取依赖。
- 增加 H2 测试依赖与 SQL 校验、JDBC 执行测试。

## TDD 证据

### RED 1

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest test
```

结果：失败。测试编译提示 `DefaultAskDataSqlValidator` 不存在，证明新测试在实现前会失败。

### RED 2

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest test
```

结果：失败。`shouldAddLimitBeforeTrailingCommentAfterSemicolon` 期望 `select * from orders limit 100`，实际输出在分号和注释后追加 `limit`。

### GREEN

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest test
```

结果：通过，10 个测试、0 失败、0 错误。

## 全量验证

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter test
```

结果：通过，25 个测试、0 失败、0 错误。

## 风险信号

- 项目既有 `fw-plus-ai-boot-autoconfigure` POM 存在 3 条重复 LangChain4j 依赖声明警告，本任务未修改该文件。
- 测试运行时缺少 SLF4J 绑定，使用 NOP logger；不影响测试结果。
- 未发现 SQLBot 品牌、Spring AI 运行时依赖或 Java 9+ API。

## SQL 安全修复复核

### 修复内容

- 拒绝可写 `SELECT INTO`，包括 `select * into audit_copy from orders`。
- 保守拒绝 MySQL 可执行注释 `/*!...*/`，避免注释中包含的写操作绕过扫描。
- 为 `AskDataDialect` 增加 Java 8 兼容的 `applyLimit` 策略；MySQL、PostgreSQL、ClickHouse 仍注册为 `LIMIT` 语法。
- 识别无换行尾随 `--` 注释，在注释前通过方言追加行数限制。

### RED

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest test
```

结果：失败。新增方言委托测试在 `@Override applyLimit` 处无法编译，证明生产代码尚未提供行数限制策略契约。

### GREEN

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest test
```

结果：通过，13 个测试、0 失败、0 错误。

### Task 5 聚焦测试

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest test
```

结果：通过，14 个测试、0 失败、0 错误。

### 风险信号

- 构建仍报告 `fw-plus-ai-boot-autoconfigure` 的 3 条重复 LangChain4j 依赖声明警告，未在本任务范围内修改。
- JDBC 聚焦测试仍提示缺少 SLF4J 绑定并使用 NOP logger；不影响测试结果。

## 额外复审修复：方言限行检测

### 修复内容

- `AskDataDialect` 新增 Java 8 兼容的 `hasRowLimit(String sql)` 策略。默认实现忽略字符串、注释和子查询中的 `LIMIT`，仅识别顶层 `LIMIT`。
- `DefaultAskDataSqlValidator` 在 SQL 规范化后通过 `dialect.hasRowLimit(...)` 决定是否追加，不再自行以 `hasTopLevelLimit` 作出决策。
- 新增自定义 `FETCH FIRST ... ROWS ONLY` 方言测试：已有 `fetch first 10 rows only` 时保持原 SQL，缺失时追加 `fetch first 100 rows only`。

### RED

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest test
```

结果：失败。新增测试在 `@Override hasRowLimit` 处编译失败，证明生产方言契约尚未提供限行检测策略。

### GREEN

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest test
```

结果：通过，14 个测试、0 失败、0 错误。

### Task 5 聚焦测试

命令：

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/.worktrees/add-askdata-spring-boot-starter/fw-plus-ai && ./mvnw -f fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-askdata-spring-boot-starter -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest test
```

结果：通过，15 个测试、0 失败、0 错误。

### 风险信号

- 构建仍报告 `fw-plus-ai-boot-autoconfigure` 的 3 条重复 LangChain4j 依赖声明警告，未在本任务范围内修改。
- JDBC 聚焦测试仍提示缺少 SLF4J 绑定并使用 NOP logger；不影响测试结果。
