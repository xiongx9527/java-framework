# Task 4 报告：Neo4j JDBC 适配公共模块

## 状态

DONE

## RED

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-neo4j-jdbc -am -Dtest=Neo4jDriverTest -DfailIfNoTests=false test
```

结果：BUILD FAILURE，`Neo4jDriverTest` 共 21 个测试，1 个失败、0 个错误。

失败摘要：新增 `shouldAdaptCommonBindingsToNeo4jNamedParameters` 失败，断言信息为 `Neo4jGraphJdbcExecutor should adapt common bindings`。失败原因是生产代码中尚未存在 `Neo4jGraphJdbcExecutor`，证明新增测试覆盖了待实现的公共执行器适配行为。

说明：首次携带 `-Dtest=Neo4jDriverTest` 的反应器执行被公共模块的 Surefire `No tests were executed` 提前拦截；补充 `-DfailIfNoTests=false` 后得到上述有效 RED。

## GREEN

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-neo4j-jdbc -am test
```

结果：BUILD SUCCESS。

- `fw-plus-graph-jdbc`：18 个测试通过，0 失败，0 错误。
- `fw-plus-neo4j-jdbc`：24 个测试通过，0 失败，0 错误；3 个 `Neo4jLiveJdbcSmokeTest` 因无真实服务条件跳过。
- `Neo4jDriverTest`：21 个测试通过，验证 fake executor 下的 Statement、PreparedStatement、`p1/p2` 命名参数、batch、JDBC 对象参数物化、metadata 形状和 Neo4j `NODE`/`RELATIONSHIP` 表类型。

附加验证：

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/resources/META-INF/services/java.sql.Driver
```

无输出，`java.sql.Driver` 服务声明未变化。

## 提交

`2220c07b fw-plus-core: 适配 Neo4j JDBC 公共层`

## 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jGraphJdbcExecutor.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jJdbcProxies.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/FakeNeo4jQueryExecutorFactory.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jDriverTest.java`

## 风险信号自评

- 低风险：未连接真实 Neo4j，符合本任务 fake executor 测试约束；3 个真实服务 smoke 测试保持跳过。
- 低风险：Maven 编译仍提示既有 `Neo4jSQLXML` 的 unchecked 警告，本任务未修改该文件。
- 低风险：Neo4j facade 仅为生成键列形状增加一层轻量代理，标准 Statement/PreparedStatement 行为继续由公共 `GraphJdbcStatementSupport` 提供。
- 已确认：未修改 Nebula、POM、Driver URL/类、Neo4j Java Driver 依赖或 `META-INF/services`。

## Reviewer 反馈

### Important
- `Neo4jJdbcProxies` 对 `getGeneratedKeys()` 的外层包装绕过内层公共 Statement 生命周期校验；关闭 Statement/Connection 后调用 `getGeneratedKeys()` 不再抛出关闭态 `SQLException`。同一包装还需处理 `equals/hashCode/toString`，避免 `statement.equals(statement)` 为 false。
- `Neo4jGraphJdbcExecutor.normalize` 已不知道原始 setter 名称与长度参数；公共层仅保留 `args[1]` 后，`setAsciiStream`/`setUnicodeStream` 从 String 退化为 byte[]，`setBlob`/`setClob`/binary/character stream 的指定长度被忽略。需要在公共绑定中保留 setter/长度信息，或在 Neo4j adapter 中恢复等价物化规则，并补回归测试。

### Minor
- 新增 batch 测试只覆盖单条空值批次；建议补两次 setXxx/addBatch 的不同值断言，锁定参数快照策略。

## Reviewer 修复（2026-07-26）

### RED

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-neo4j-jdbc -am -Dtest=Neo4jDriverTest -DfailIfNoTests=false test
```

结果：BUILD FAILURE，`Neo4jDriverTest` 共 24 个测试，2 个失败、0 个错误。

- `shouldKeepGeneratedKeysStatementLifecycleAndObjectContracts`：外层代理使 `statement.equals(statement)` 返回 false，且绕过关闭态校验。
- `shouldMaterializeStreamParametersUsingTheirSetterAndLength`：`setBlob(..., 4)` 实际读取 `blob-rest`，未尊重长度。

### GREEN

命令：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-neo4j-jdbc -am test
```

结果：BUILD SUCCESS。

- `fw-plus-graph-jdbc`：18 个测试通过，0 失败，0 错误。
- `fw-plus-neo4j-jdbc`：27 个测试通过，0 失败，0 错误；3 个真实 Neo4j smoke 测试按环境条件跳过。
- 新增回归覆盖生成键关闭态与对象方法、`setBlob`/`setClob`/binary/character/ascii/unicode stream 长度物化、两条 PreparedStatement batch 参数快照。

### 提交

`34e823b3 fix: preserve Neo4j JDBC adapter behavior`

### 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterBindings.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcStatementSupport.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jGraphJdbcExecutor.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jJdbcProxies.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/FakeNeo4jQueryExecutorFactory.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jDriverTest.java`

## Reviewer 复审（2026-07-26）

状态：APPROVED。

- 已确认 `getGeneratedKeys()` 不再绕过 Statement/Connection 关闭态校验。
- 已确认外层代理的 `equals/hashCode/toString` 有合理对象方法行为和回归断言。
- 已确认 stream、Blob、Clob 参数物化保留 setter 与 length 语义。
- 已确认 batch 参数快照覆盖两次 `setXxx/addBatch` 的不同值。
- 已确认 Neo4j `p1/p2` named params 仍仅在 Neo4j adapter 内处理。
- 已确认 Neo4j `NODE`/`RELATIONSHIP` table types 保留。
- 已确认未修改 Nebula、POM、服务声明或引入真实服务依赖。

问题：Critical 无，Important 无，Minor 无。Task 4 不阻塞勾选。
