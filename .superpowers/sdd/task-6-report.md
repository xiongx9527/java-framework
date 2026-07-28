# Task 6 验证报告：公共图 JDBC 抽象

## 状态

DONE_WITH_CONCERNS

## 环境与 Maven Wrapper

工作树的 `fw-plus-core/.mvn/wrapper/maven-wrapper.properties` 与 `maven-wrapper.jar` 缺失。按简报先在 `fw-plus-core` 执行：

```bash
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc test
```

结果：Maven 启动前失败，脚本报缺少 wrapper properties 和 jar，随后 `org.apache.maven.wrapper.MavenWrapperMain` 的 `ClassNotFoundException`。这属于工作树环境缺失，未进入项目编译或测试。

按照本任务确认的回退规则，以下 Maven 命令均显式使用：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home
```

运行时为 Zulu OpenJDK `1.8.0_402`，系统 Maven 为 `3.9.16`。

## Maven 验收

1. 公共模块：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-graph-jdbc test
```

结果：`BUILD SUCCESS`；20 tests，0 failures，0 errors，0 skipped。

2. Neo4j 本地回归：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-neo4j-jdbc -am -DfailIfNoTests=false -Dtest=Neo4jDriverTest test
```

结果：`BUILD SUCCESS`；`Neo4jDriverTest` 24 tests，0 failures，0 errors，0 skipped。Reactor 顺序为 `fw-plus-graph`、`fw-plus-graph-jdbc`、`fw-plus-neo4j-jdbc`。

3. Nebula 本地核心回归（含 Direct executor）：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml -pl fw-plus-nebula-jdbc -am -DfailIfNoTests=false -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaResultSetCursorMovementTest,DirectNebulaSqlExecutorTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test
```

结果：`BUILD SUCCESS`；58 tests，0 failures，0 errors，0 skipped；其中 `DirectNebulaSqlExecutorTest` 17 tests。

4. 图 JDBC 聚合测试：

```bash
JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-core/fw-plus-project/fw-plus-graph/pom.xml test
```

结果：`BUILD SUCCESS`。公共模块 20 tests（全通过）；Nebula 244 tests，0 failures，0 errors，2 skipped；Neo4j 27 tests，0 failures，0 errors，3 skipped。5 个跳过项全部为既有真实服务 smoke：`NebulaLiveJdbcSmokeTest` 2 项、`Neo4jLiveJdbcSmokeTest` 3 项。本次没有连接真实 NebulaGraph 或 Neo4j 服务，符合本变更的本地验收边界。

## 依赖与兼容边界

1. 执行：

```bash
rg -n "com\\.vesoft|org\\.neo4j|druid|springframework|mybatis|nebula|neo4j" fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc
```

结果：无输出（包含 `target/` 的完整模块扫描未命中），公共模块不存在具体 SDK、Druid、Spring、MyBatis 或具体图数据库命名。

2. 公共模块 POM 仅声明 `junit` 的 test scope 依赖；图聚合模块顺序为 `fw-plus-graph-jdbc`、`fw-plus-nebula-jdbc`、`fw-plus-neo4j-jdbc`。目录层面仅存在这三个 `*jdbc` 模块，未新增第三种图数据库驱动。

3. 执行：

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/resources/META-INF/services/java.sql.Driver \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/resources/META-INF/services/java.sql.Driver
```

结果：无输出；两个 Driver 服务声明无 diff。

4. 公共模块 `src` 的 SQL AST、MySQL、binlog、trigger、routine、parser、replication 关键词扫描无输出。相对 change base `97f4d9d0c96697bcfcb7446ee9b89e92ff47dcea`，Nebula 的 `mysql` 和 `sql/{parser,query,write,binlog,trigger,routine}` 路径无改动；上述协议与扩展能力仍位于 Nebula 模块。

5. Java 8 边界扫描：

```bash
rg -n "List\\.of\\(|Map\\.of\\(|Set\\.of\\(|copyOf\\(|Optional\\.orElseThrow\\(\\)|\\.isBlank\\(|\\bvar\\s+" fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc/src fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src
```

结果：未命中 Java 9+ 集合工厂、`Optional.orElseThrow()` 无参、`String.isBlank()` 或 `var`。`copyOf` 命中仅为公共类私有方法 `copyOf` 与 `Arrays.copyOf`，后者是 Java 8 API；无兼容性问题。

6. `git diff --check` 无输出。

## OpenSpec 映射

- 1.1-1.3：Task 1，公共模块、Java 8 POM 与两个具体驱动依赖。
- 2.1-2.5：Task 2/3，公共执行与结果抽象、默认行为/生命周期、参数与 batch、RowSet、MetaData 形状。
- 3.1-3.3：Task 4，Neo4j facade 保留协议与 Java Driver 边界，本地回归通过。
- 4.1-4.4：Task 5，Nebula 保守接入公共层，保留连接池、SQL/nGQL 转换、原生结果映射及协议能力。
- 5.1-5.3：Task 6，本报告中的定向测试、聚合构建、依赖与范围检查。

说明：`openspec/changes/extract-common-graph-jdbc/tasks.md` 的 1.x、2.x、5.x checkbox 当前仍未勾选；遵照 Task 6 职责边界，本任务未修改 OpenSpec tasks。

## 风险信号

- 工作树缺少 `.mvn/wrapper/*`，后续验证仍需显式使用 Zulu 8 系统 Maven，直到 Wrapper 文件被恢复。
- 聚合测试的 5 个真实服务 smoke 按既有条件跳过，未构成真实 NebulaGraph/Neo4j 连通性证明。
- Nebula 定向测试有 SLF4J `StaticLoggerBinder` 缺失警告，使用 NOP logger；测试结果未受影响。
- 工作区保留用户已有的 `openspec/changes/extract-common-graph-jdbc/.comet/subagent-progress.md` 修改，未由本任务触碰。
