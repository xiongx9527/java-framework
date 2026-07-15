# Task 2 实现报告

## Status
DONE

## 实现内容
- 将连接配置收敛为不可变 `NebulaConnectionConfig`：包含 graphspace、用户名、密码、地址列表、连接池参数和合并配置副本。
- `NebulaConnectionConfigParser.parse` 统一调用 `NebulaJdbcUrlParser.parse(url, defaults)` 与 `NebulaJdbcUrlParser.getAddresses(url)`，并使用扁平化后的合并配置作为单一事实源。
- 集中实现连接池整数参数解析：支持缺失默认值、`Number`、trim 后字符串；空串与非数字抛出包含参数名/非法值的 `SQLException`。
- 新增 `NebulaPoolFactory`，`NebulaClientFacade` 默认构造器通过 factory 创建 pool，并保留测试用构造入口。
- `NebulaClientFacade.open` 使用 `config.getGraphAddresses()`、配置化 pool 参数、登录 session、执行 `use graphspace`；失败时释放 session/pool 并抛出含 graphspace/error code/error message 的 `SQLException`。
- `NebulaConnection` 构造流程改为 `NebulaConnectionConfigParser.parse` -> 保存 merged properties -> 创建/open `NebulaClientFacade`；`execute` 委托 façade；`close` 幂等委托 façade。

## RED 命令与失败摘要
1. Wrapper 尝试：
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaClientFacadeTest
```
失败摘要：`./mvnw` 所需 `.mvn/wrapper/maven-wrapper.properties` 与 `maven-wrapper.jar` 缺失，无法加载 `org.apache.maven.wrapper.MavenWrapperMain`。后续按 brief 允许改用系统 Maven 3.6.3。

2. 初始 RED：
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaClientFacadeTest
```
失败摘要：testCompile 失败，缺少 `NebulaConnectionConfig.getGraphAddresses()`、`getMergedProperties()`、`NebulaPoolFactory`，且新构造器签名不存在。

3. 追加 RED（mergedProperties 扁平化边界）：
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest
```
失败摘要：`parseShouldMergeUrlQueryAndInputPropertiesIntoTypedConfig` 期望 `getMergedProperties().getProperty("user") == root`，实际为 `null`，证明 `Properties(defaults)` 默认链未被防御性副本扁平化。

## GREEN 命令与通过摘要
1. 目标测试：
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaClientFacadeTest
```
通过摘要：`Tests run: 16, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

2. Task 1 五类回归：
```bash
cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest
```
通过摘要：`Tests run: 30, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

## 变更文件
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfig.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParser.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaPoolFactory.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/NebulaConnection.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParserTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/.superpowers/sdd/task-2-report.md`

## 风险信号（Comet 风险清单自报）
- 范围漂移：未修改 Boot/Cloud/AI/Product 层；未修改 OpenSpec tasks；未修改 Comet 检查点。工作区存在 `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/openspec/changes/stabilize-nebula-jdbc/.comet/subagent-progress.md` 的既有/外部变更，未暂存未提交。
- 真实外部依赖：未使用真实 NebulaGraph 或 Testcontainers；测试使用 mock/factory 注入。
- TDD 证据：已记录 RED 与 GREEN；新增 mergedProperties 扁平化边界先 RED 后 GREEN。
- 兼容性：保留 `getGraphAddress()` 与旧构造器以降低 Task 1 测试/旧调用破坏风险；新主路径使用 `getGraphAddresses()`。
- 构建环境：`./mvnw` wrapper 缺失，按 brief 使用系统 Maven 3.6.3；Maven settings 存在既有 `Unrecognised tag: 'repository'` 警告、`sa-token-core` 重复依赖警告、SLF4J binder 缺失警告，未影响测试通过。
- 并发/生命周期：`NebulaClientFacade.close()` 和 `NebulaConnection.close()` 已保持重复调用稳定；更完整生命周期策略按 Task 3 继续收敛。
- 异常处理：`executeNgql` 对 `IOErrorException` 保留 cause 包装为 `SQLException`；`use graphspace` 失败信息包含 graphspace、error code、error message。


## Review Fix - Task 2 - 2026-07-15

### Status
DONE

### 修复内容
- 修复 `NebulaConnectionConfigParser` 在 URL query 存在时只 flatten parsed Properties 导致 input/defaults 链上的非 String pool 参数丢失的问题；现在先 flatten input/defaults 链，再用 URL query/current entries 覆盖，pool 参数继续从 merged properties 单一事实源读取。
- 修复 `NebulaClientFacade.open` 在 `pool.init(...)` 抛 `UnknownHostException` 等异常时未关闭已创建 pool 的问题；失败清理改为基于 pool 是否已创建，不依赖 init 返回 true。
- 修复空白数字参数的 `SQLException` 消息缺少实际非法值的问题；空白值消息现在包含字段名和原始空白值表示。

### RED 命令与失败摘要
命令：
`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaClientFacadeTest`

失败摘要：
- `parseShouldKeepNonStringPoolOptionsWhenUrlContainsQuery`: expected `<2>` but was `<0>`。
- `parseShouldKeepNonStringPoolOptionsFromPropertiesDefaultsWhenUrlContainsQuery`: expected `<2>` but was `<0>`。
- `parseShouldRejectBlankAndNonNumericPoolOptionsWithFieldName`: 空白值消息未包含原始非法值。
- `openShouldCloseCreatedPoolWhenPoolInitThrowsBeforeReturning`: `nebulaPool.close()` 未被调用。

### GREEN 命令与通过摘要
命令：
`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaClientFacadeTest`

通过摘要：`Tests run: 19, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

最终验证命令：
`cd /Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core && mvn test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -Dtest=NebulaConnectionConfigParserTest,NebulaStatementClassifierTest,NebulaResourceLifecycleTest,NebulaResultSetCursorTest,NebulaClientFacadeTest`

通过摘要：`Tests run: 33, Failures: 0, Errors: 0, Skipped: 0`，`BUILD SUCCESS`。

### 变更文件
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParser.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/main/java/com/vesoft/nebula/jdbc/client/NebulaClientFacade.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/config/NebulaConnectionConfigParserTest.java`
- `/Users/xiongxiao/Workspaces/Idea/codes/New/java-framework/fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc/src/test/java/com/vesoft/nebula/jdbc/client/NebulaClientFacadeTest.java`

### 风险信号
- Maven/settings 既有警告：settings.xml 存在无法识别的 `repository` 标签；`fw-plus-auth-satoken` 存在重复依赖声明。
- 测试运行时既有 SLF4J NOP logger 提示。
- `Properties.defaults` 需要通过反射 flatten；当前项目 Java 8 环境验证可访问，但未来强封装 JDK 需关注模块访问限制。
