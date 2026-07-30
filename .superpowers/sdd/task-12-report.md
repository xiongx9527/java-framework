# Task 12 实现报告：SQL 插件链桥接与安全测试

## 范围

- Base commit：`6160e69e555aa78d1de4188679e68ee604940c64`
- 仅修改 AskData 自动配置中的 Core SQL 插件链桥接，以及 Task 12 新增测试。
- 未修改 Core AskData 模块、OpenSpec 进度文件或其他已有脏文件。

## TDD 记录

### RED

先新增 `AskDataSqlInterceptorAutoConfigurationTest`，覆盖以下行为：

1. Spring 自定义 `AskDataSqlInterceptor` 读取 `tenantId` 命名参数并增加租户谓词。
2. 恶意插件将 SQL 改写为 `delete` 时，最终校验器返回 `WRITE_SQL` 并拒绝执行。
3. 审计记录包含插件 key/`INTERCEPT` 及 `validator`/`FINAL_VALIDATE`。

首次执行指定命令时发现 Surefire 使用 JUnit Platform、类路径没有 JUnit Vintage，遗留 JUnit 4 测试被编译但未发现，命令返回 `Tests run: 0`。新增测试因此使用模块已有的 JUnit Jupiter 5 API，保证测试选择器实际执行 Task 12 测试。

随后执行：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest,AskDataSqlInterceptorAutoConfigurationTest test
```

结果：失败，`AskDataSqlInterceptorAutoConfigurationTest` 运行 3 项，均因 `AskDataSqlInterceptorChain` 尚未自动装配而断言失败（`expected: not <null>`）。

### GREEN

在 `AskDataAutoConfiguration` 中新增：

- 可替换的 `AskDataSqlAudit` Bean。
- 仅在存在 Core `AskDataSqlValidator` 时创建的 `AskDataSqlInterceptorChain` Bean。
- 通过 `ObjectProvider<AskDataSqlInterceptor>.orderedStream()` 收集 Spring 排序后的插件。

最终执行同一命令：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest,AskDataSqlInterceptorAutoConfigurationTest test
```

结果：`BUILD SUCCESS`，`AskDataSqlInterceptorAutoConfigurationTest` 共运行 3 项，`Failures: 0, Errors: 0`。

## 安全设计

- 自动配置不自行重写插件结果，直接委托 Core `AskDataSqlInterceptorChain`。
- Core 链只采纳插件返回的 SQL 文本，保留输入命令的项目/操作人上下文、命名参数和授权对象。
- 每个插件阶段写入审计记录；所有插件完成后链强制调用最终校验器，恶意 SQL 改写无法绕过校验。
- Core 校验器未配置时不创建插件链，避免以不明确的默认安全策略替代业务方明确的 Core SQL 安全策略。

## 变更文件

- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/autoconfigure/AskDataAutoConfiguration.java`
- `fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/ai/autoconfigure/askdata/sql/AskDataSqlInterceptorAutoConfigurationTest.java`
- `.superpowers/sdd/task-12-report.md`

## 环境信号

- Maven 在 JDK 24 下输出 Jansi/`sun.misc.Unsafe` 未来兼容性警告。
- 测试类路径存在多个 SLF4J binding 警告。
- 上述均为既有环境依赖信号；Task 12 的 3 项 JUnit Jupiter 测试已实际执行并通过。

## 第一轮审查修复：JUnit Jupiter 发现性

### RED

在未修改测试类时执行指定命令：

```bash
cd fw-plus-ai
./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -Dtest=DefaultAskDataSqlValidatorTest,JdbcAskDataSqlExecutorTest,AskDataSqlInterceptorAutoConfigurationTest test
```

构建成功但 Surefire 仅发现并执行 `AskDataSqlInterceptorAutoConfigurationTest` 的 3 项测试；
`DefaultAskDataSqlValidatorTest` 和 `JdbcAskDataSqlExecutorTest` 仍使用 JUnit 4，模块类路径没有
JUnit Vintage Engine，因而没有执行。该结果证明原安全回归证据不完整。

### GREEN

将上述两个遗留测试类的 `@Test` 和断言静态导入从 JUnit 4 迁移至模块已有的 JUnit Jupiter API，
不新增 Vintage 依赖，也不修改生产逻辑。再次执行同一命令，结果为 `BUILD SUCCESS`：

- `DefaultAskDataSqlValidatorTest`：23 项。
- `JdbcAskDataSqlExecutorTest`：2 项。
- `AskDataSqlInterceptorAutoConfigurationTest`：3 项。
- 合计：`Tests run: 28, Failures: 0, Errors: 0, Skipped: 0`。

因此，SQL 校验器、JDBC 执行器和 SQL 插件链自动配置三类回归测试均已被实际发现并执行。
