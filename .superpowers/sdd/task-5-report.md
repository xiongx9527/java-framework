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
