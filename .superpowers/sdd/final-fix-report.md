# extract-common-graph-jdbc 最终审查修复报告

## 状态

DONE_WITH_CONCERNS

## 提交

`8d94a1c4` - `fix: resolve graph jdbc final review findings`

## RED

1. 公共代理与 PreparedStatement 重载：

   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc -Dtest=GraphJdbcRowSetsTest,GraphJdbcParameterBindingsTest,GraphJdbcStatementSupportTest test
   ```

   结果：预期失败。`GraphJdbcRowSetsTest.resultSetProxiesShouldKeepObjectContractsAndUseTheOuterWrapper` 失败，证明空 generated-keys 代理未保持对象自反性；`GraphJdbcStatementSupportTest.preparedStatementShouldBindParametersAndRejectStringExecute` 失败，证明 `PreparedStatement.addBatch(String)` 被接受。

2. Neo4j facade 与参数元数据：

   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-neo4j-jdbc -am -DfailIfNoTests=false -Dtest=Neo4jDriverTest test
   ```

   结果：预期失败。`shouldKeepGeneratedKeysStatementLifecycleAndObjectContracts` 失败，证明 `unwrap(Statement.class)` 未返回外层 facade；`shouldExposePreparedParameterMetadataAndGeneratedKeysShape` 失败，证明 Neo4j `ParameterMetaData.isSigned(1)` 从基线的 `true` 回归为 `false`。

## GREEN

1. 公共定向测试：同一公共模块命令通过，13 tests，0 failures，0 errors，0 skipped。
2. Neo4j 定向测试：同一 Neo4j 命令通过，24 tests，0 failures，0 errors，0 skipped。
3. 图 JDBC 聚合：

   ```bash
   JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml test
   ```

   通过。公共模块 21 tests，0 failures/errors/skipped；Nebula 244 tests，0 failures/errors，2 个既有 live smoke skipped；Neo4j 27 tests，0 failures/errors，3 个既有 live smoke skipped。

4. 边界扫描：公共模块的 `com.vesoft|org.neo4j|com.alibaba.druid|org.springframework|org.apache.ibatis` 扫描无命中；Java 9+ 集合工厂和无参 `orElseThrow()` 扫描无命中；`git diff --check` 通过。

## 修复内容

- `GraphJdbcRowSets` 的 ResultSet 和 ResultSetMetaData 动态代理优先处理 `equals`、`hashCode`、`toString`，并让 `unwrap/isWrapperFor` 以外层 ResultSet proxy 为准。
- Neo4j 生成键 facade 显式处理 `unwrap/isWrapperFor`，保证接口 unwrap 返回 facade 本身，保持单列 `GENERATED_KEY` 形状及底层关闭状态检查。
- `GraphJdbcParameterMetaData` 支持可配置的签名策略；Neo4j facade 使用 `true`，Nebula 既有 JDBC type 判断逻辑未修改。
- PreparedStatement 的 `addBatch(String)` 与其他 String 执行重载一致抛出 `SQLException`。

## 变更文件

- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcParameterMetaData.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSets.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcStatementSupport.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcRowSetsTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/jdbc/GraphJdbcStatementSupportTest.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jJdbcProxies.java`
- `fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/test/java/com/xiongx9527/fw/plus/graph/neo4j/jdbc/Neo4jDriverTest.java`
- `.superpowers/sdd/final-fix-report.md`

## 风险信号

- Maven Wrapper 文件仍缺失，本次按允许的 Zulu 8 系统 Maven 回退执行验证。
- 聚合中的 5 个真实服务 smoke 依既有条件跳过，未验证真实 NebulaGraph 或 Neo4j 连通性。
- Nebula 测试仍输出既有 SLF4J `StaticLoggerBinder` 缺失警告；不影响测试结果。
- `openspec/changes/extract-common-graph-jdbc/.comet/subagent-progress.md` 为已有用户改动，未修改且不会纳入提交。
