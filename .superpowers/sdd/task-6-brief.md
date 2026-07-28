### Task 6: 全量验证、依赖边界检查与 OpenSpec 对齐

**Files:**
- Verify only: `fw-plus-core/fw-plus-project/fw-plus-graph/**`
- Verify only: `openspec/changes/extract-common-graph-jdbc/tasks.md`
- Verify only: `docs/superpowers/specs/2026-07-26-extract-common-graph-jdbc-design.md`

**Interfaces:**
- Consumes: Task 1-5 的所有产出。
- Produces: 可交付状态：公共模块存在、两个具体驱动依赖公共模块、测试通过、公共模块无具体 SDK 依赖、OpenSpec tasks 可勾选。

- [ ] **Step 1: 跑公共模块测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 2: 跑 Neo4j 本地测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc -am -Dtest=Neo4jDriverTest test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 3: 跑 Nebula 本地核心回归**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc -am -Dtest=NebulaResourceLifecycleTest,NebulaPreparedStatementBindingsTest,NebulaDatabaseMetaDataTest,NebulaUnsupportedJdbcFeatureTest,NebulaResultSetCursorMovementTest,NebulaJdbcTemplateContractTest,NebulaMyBatisContractTest test
```

Expected: `BUILD SUCCESS`。

- [ ] **Step 4: 跑 `fw-plus-graph` 聚合测试**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework/fw-plus-core
./mvnw -pl fw-plus-project/fw-plus-graph -am test
```

Expected: `BUILD SUCCESS`。如果 `Neo4jLiveJdbcSmokeTest` 或 `NebulaLiveJdbcSmokeTest` 在当前 Surefire 配置中误运行并要求真实服务，先确认现有测试约定，再将 live smoke 测试限定到显式 profile 或按已有跳过机制处理；不要用真实服务可用性作为本 change 的成功条件。

- [ ] **Step 5: 检查公共模块依赖边界**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework
rg -n "com\\.vesoft|org\\.neo4j|druid|springframework|mybatis|nebula|neo4j" fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-graph-jdbc
```

Expected: 无输出。若测试类中字符串包含 `neo4j` 或 `nebula`，改为中性示例词，例如 `NODE`、`EDGE`、`graph`，保持公共模块无具体驱动命名。

- [ ] **Step 6: 检查服务声明和对外入口未变**

Run:

```bash
cd /Users/xiongxiao/Workspace/Idea/codes/java-framework
git diff -- fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-nebula-jdbc/src/main/resources/META-INF/services/java.sql.Driver \
  fw-plus-core/fw-plus-project/fw-plus-graph/fw-plus-neo4j-jdbc/src/main/resources/META-INF/services/java.sql.Driver
```

Expected: 无输出。

- [ ] **Step 7: 检查 OpenSpec tasks 可逐项勾选**

对照 `openspec/changes/extract-common-graph-jdbc/tasks.md`：

```text
1.1-1.3 由 Task 1 覆盖
2.1-2.5 由 Task 2 和 Task 3 覆盖
3.1-3.3 由 Task 4 覆盖
4.1-4.4 由 Task 5 覆盖
5.1-5.3 由 Task 6 覆盖
```

Expected: 每个 OpenSpec 条目都能映射到一个已执行任务；没有新增第三种图数据库驱动；没有把 Nebula SQL AST、MySQL 协议、binlog、trigger、routine 迁入公共模块。

- [ ] **Step 8: 最终提交验证收口**

```bash
git status --short
git log --oneline --max-count=6
```

Expected: 工作区只有本 change 相关文件改动；最近提交包含 Task 1-5 的提交。若团队偏好单提交，在最终集成前用非交互式方式整理提交，并保留清晰提交信息：

```bash
git commit -m "fw-plus-core: 抽象公共图数据库 JDBC 标准层"
```

## 自检清单

- [ ] 公共模块 `fw-plus-graph-jdbc` 位于 `fw-plus-graph` 聚合中，且构建顺序早于 Nebula/Neo4j。
- [ ] 公共模块没有具体图数据库 SDK、Druid、Spring、MyBatis 运行时依赖。
- [ ] Neo4j 保留 URL、Driver、连接配置、SQL-to-Cypher、Java Driver 执行器。
- [ ] Nebula 保留连接池、nGQL/SQL 转换、原生结果映射、SQL AST、MySQL 协议、binlog、trigger、routine。
- [ ] 公共测试、Neo4j 本地测试、Nebula 本地稳定性与框架集成测试都通过。
- [ ] 所有 Java 代码兼容 Java 1.8。
- [ ] `META-INF/services/java.sql.Driver` 未变化。
