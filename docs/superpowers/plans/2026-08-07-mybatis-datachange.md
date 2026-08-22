# MyBatis DataChange Rebuild Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 重建数据变更监听，使原生 MyBatis、MyBatis-Plus 和 MyBatis-Flex 通过同一事件、快照、Diff 与 Handler 内核获得逐行逐字段审计能力。

**Architecture:** 新增 Spring 无关的 `fw-plus-mybatis-datachange`，以最终 `BoundSql` 和实际 JDBC Connection 为事实来源，使用方言 SPI 分析 SQL、读取更新前后快照并生成 Event → Statement → Table → Row → Field 事件。Plus/Flex 模块仅贡献框架元数据适配器，Boot 自动配置负责属性绑定、拦截器排序和全部 `SqlSessionFactory` 注册。

**Tech Stack:** Java 8、MyBatis 3.5.19、JSqlParser 4.6、MyBatis-Plus 3.3.2、MyBatis-Flex 1.11.4、Spring Boot 2.7.18、JDBC、H2、JUnit 5、AssertJ、Mockito。

## Global Constraints

- 生产代码严格兼容 Java 8，不使用 `List.of()`、`Map.of()`、`Map.copyOf()`、新版 `Optional.orElseThrow()` 等 Java 9+ API。
- 旧 `fw-plus-mp-datachange` 仅作为需求参考；不兼容旧 Handler、事件、配置前缀、实体基类或 Mapper 方法名白名单。
- 公共模块不得引用 MyBatis-Plus、MyBatis-Flex、Spring 或项目自定义 `MpBaseEntity` / `MpBaseMapper` 类型。
- 配置前缀固定为 `fw.plus.mybatis-datachange`，只监听显式配置的表；默认快照上限为 `1000` 行。
- 事务可选：有事务时 Handler 失败回滚；无事务时标记 `BEST_EFFORT` 并交给 `DataChangeErrorHandler`。
- `CALL`、存储过程、SELECT 和 DDL 不审计；无法识别目标表的其他 DML 采用 fail-closed。
- Batch 按一次实际 flush 的初始值和最终值聚合，不记录批次内中间状态。
- SQL、异常和日志不得渲染绑定参数；快照参数必须通过 MyBatis TypeHandler/JDBC 参数绑定。
- 目标数据库通过 `DataChangeDialect` 扩展；内置 MySQL/MariaDB、PostgreSQL、Oracle、SQL Server 和 H2。
- 当前 `dev` 工作区包含用户的 README、MagicAPI Plus/Flex 模块、Tinyflow 和 Comet 等未提交改动；执行时不得修改、暂存或提交这些无关文件。
- 实施前按 `using-git-worktrees` 检测是否需要隔离工作区；用户未明确授权前不得暂存、提交或推送本计划产生的文件。
- 设计依据：`docs/superpowers/specs/2026-08-07-mybatis-datachange-design.md`。

---

## File Map

### 公共模块

- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/pom.xml`：公共模块依赖和方言集成测试 Profile。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/`：事件、行身份、Handler、错误码和失败上下文。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/config/`：全局配置、表配置、逻辑删除配置和白名单索引。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/`：框架适配契约、原生 MyBatis 兜底和确定性注册表。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/`：SQL 分析模型、方言 SPI、JSqlParser 基类和内置方言。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/`：元数据、参数绑定、前后快照和生成主键解析。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/diff/`：行、字段和操作类型差异计算。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dispatch/`：Handler 顺序、事务/非事务异常策略。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/`：作用域、执行上下文、普通执行和 Batch 生命周期。

### ORM 适配模块

- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/`：删除旧实现，改为 MyBatis-Plus 元数据适配器。
- `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf/fw-plus-mf-datachange/`：新增 MyBatis-Flex 元数据适配器。

### Boot 与样例

- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/MybatisDataChangeAutoConfiguration.java`：公共配置和全部 `SqlSessionFactory` 注册。
- `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mp/MpDataChangeAdapterAutoConfiguration.java` 与 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mf/MfDataChangeAdapterAutoConfiguration.java`：条件贡献 ORM Adapter。
- `fw-plus-sample/fw-plus-boot-samples/fw-plus-mp-spring-boot-starter-sample/`：新 Handler 和新配置格式示例。
- `docs/mybatis-datachange.md`：用户文档、支持矩阵和扩展说明。

---

### Task 1: Maven 拓扑、配置和公共事件契约

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/config/DataChangeConfiguration.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/config/TableMonitor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/config/LogicalDeleteConfiguration.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/ConsistencyLevel.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeOperation.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/TableIdentifier.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/RowIdentity.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeEvent.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/StatementChange.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/TableChange.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/RowChange.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/FieldChange.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeHandler.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeErrorHandler.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeErrorCode.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeFailure.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeException.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/config/DataChangeConfigurationTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/api/DataChangeModelTest.java`

**Interfaces:**
- Produces: `DataChangeConfiguration#isEnabled()`、`getTables()`、`getMaxSnapshotRows()`；`enabled` 默认 `false`，快照上限默认 `1000`。
- Produces: `TableIdentifier(catalog, schema, table)` 的值语义和 `RowIdentity(Map<String,Object>)` 的稳定顺序值语义。
- Produces: `DataChangeOperation` 仅包含最终行语义 `INSERT`、`UPDATE`、`PHYSICAL_DELETE`、`LOGICAL_DELETE`；不把 UPSERT/MERGE 作为行结果。
- Produces: `DataChangeHandler#supports(DataChangeEvent)`、`handle(DataChangeEvent) throws Exception`、`getOrder()`。
- Produces: `DataChangeException(DataChangeErrorCode, String)`、`DataChangeException(DataChangeErrorCode, String, Throwable)` 和 `getCode()`，供所有 fail-closed 路径统一携带错误码。
- Produces: Event → Statement → Table → Row → Field 公共模型，供后续所有任务使用。

- [ ] **Step 1: 创建失败的配置和事件模型测试**

```java
@Test
void defaultsToOneThousandSnapshotRowsAndAllColumns() {
	DataChangeConfiguration configuration = new DataChangeConfiguration();
	TableMonitor table = new TableMonitor();
	table.setName("sys_user");
	configuration.setTables(Collections.singletonList(table));

	assertThat(configuration.getMaxSnapshotRows()).isEqualTo(1000);
	assertThat(table.getIncludeColumns()).isEmpty();
	assertThat(table.getExcludeColumns()).isEmpty();
}

@Test
void rowIdentitySupportsCompositeKeysWithoutStringConversion() {
	Map<String, Object> keys = new LinkedHashMap<String, Object>();
	keys.put("tenant_id", 7L);
	keys.put("order_no", "A-1");
	RowIdentity identity = new RowIdentity(keys);

	assertThat(identity.getKeyValues()).containsExactly(
			entry("tenant_id", 7L), entry("order_no", "A-1"));
}
```

- [ ] **Step 2: 运行测试并确认 RED**

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange -am \
  -Dtest=DataChangeConfigurationTest,DataChangeModelTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: Maven 先识别新模块；测试编译失败，错误指向尚未创建的公共类型。

- [ ] **Step 3: 创建模块 POM 和公共类型**

新模块 POM 依赖必须收敛为：

```xml
<dependencies>
	<dependency>
		<groupId>org.mybatis</groupId>
		<artifactId>mybatis</artifactId>
	</dependency>
	<dependency>
		<groupId>com.github.jsqlparser</groupId>
		<artifactId>jsqlparser</artifactId>
	</dependency>
	<dependency>
		<groupId>org.springframework.boot</groupId>
		<artifactId>spring-boot-starter-test</artifactId>
		<scope>test</scope>
	</dependency>
	<dependency>
		<groupId>com.h2database</groupId>
		<artifactId>h2</artifactId>
		<scope>test</scope>
	</dependency>
</dependencies>
```

在 `fw-plus-mybatis/pom.xml` 添加 `<module>fw-plus-mybatis-datachange</module>`，并在 Core BOM 添加 `fw-plus-mybatis-datachange:${project.version}`。所有集合在构造/设置时防御复制；`TableIdentifier`、`RowIdentity` 实现与值一致的 `equals/hashCode`。

- [ ] **Step 4: 实现配置校验入口**

`DataChangeConfiguration#validate()` 必须拒绝空表名、重复 `catalog/schema/table`、非正数上限、include/exclude 重叠和重复唯一键列，错误消息带规范化表标识：

```java
public void validate() {
	if (maxSnapshotRows <= 0) {
		throw new IllegalArgumentException("maxSnapshotRows must be greater than zero");
	}
	Set<TableIdentifier> identifiers = new HashSet<TableIdentifier>();
	for (TableMonitor table : tables) {
		if (table == null || table.getName() == null || table.getName().trim().isEmpty()) {
			throw new IllegalArgumentException("table name must not be blank");
		}
		TableIdentifier identifier = new TableIdentifier(
				table.getCatalog(), table.getSchema(), table.getName());
		if (!identifiers.add(identifier)) {
			throw new IllegalArgumentException("duplicate monitored table: " + identifier);
		}
		if (table.getMaxSnapshotRows() != null && table.getMaxSnapshotRows().intValue() <= 0) {
			throw new IllegalArgumentException(
					"maxSnapshotRows must be greater than zero for " + identifier);
		}
		Set<String> keyColumns = normalizedColumns(table.getKeyColumns());
		if (keyColumns.size() != table.getKeyColumns().size()) {
			throw new IllegalArgumentException("duplicate key column for " + identifier);
		}
		Set<String> included = normalizedColumns(table.getIncludeColumns());
		Set<String> excluded = normalizedColumns(table.getExcludeColumns());
		included.retainAll(excluded);
		if (!included.isEmpty()) {
			throw new IllegalArgumentException("include/exclude overlap for " + identifier + ": " + included);
		}
	}
}

private Set<String> normalizedColumns(List<String> columns) {
	Set<String> normalized = new LinkedHashSet<String>();
	for (String column : columns) {
		if (column == null || column.trim().isEmpty()) {
			throw new IllegalArgumentException("column name must not be blank");
		}
		normalized.add(column.trim().toLowerCase(Locale.ENGLISH));
	}
	return normalized;
}
```

测试增加重复表、`maxSnapshotRows=0` 和 include/exclude 冲突断言。

- [ ] **Step 5: 运行 Task 1 测试和模块边界检查**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange -am test
rg -n "com\.baomidou|com\.mybatisflex|org\.springframework|MpBase" \
  fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java
```

Expected: Maven `BUILD SUCCESS`；`rg` 无输出。

- [ ] **Step 6: 检查 Task 1 变更范围（不提交）**

```bash
git diff -- \
  fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml \
  fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

---

### Task 2: 框架适配、方言选择和表元数据注册表

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/DataChangeInvocation.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/FrameworkChangeMetadata.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/LogicalDeleteMetadata.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/GeneratedKeyAccessor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/DataChangeFrameworkAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/DataChangeFrameworkAdapterRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/MybatisDataChangeFrameworkAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/DatabaseMetadata.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/DataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/DataChangeDialectRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/config/TableMonitorRegistry.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/JdbcTableMetadata.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/JdbcTableMetadataResolver.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/adapter/DataChangeFrameworkAdapterRegistryTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/DataChangeDialectRegistryTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/config/TableMonitorRegistryTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/JdbcTableMetadataResolverTest.java`

**Interfaces:**
- Consumes: `TableIdentifier`, `RowIdentity`, `DataChangeConfiguration` from Task 1。
- Produces: `DataChangeFrameworkAdapterRegistry#select(DataChangeInvocation)`，按 order 降序选择且拒绝同优先级冲突。
- Produces: `GeneratedKeyAccessor#readKeyValues(Object, List<String>)` 返回以数据库列名为 key 的有序主键值。
- Produces: `DataChangeDialectRegistry#select(Connection)`，按数据库产品和版本缓存方言。
- Produces: `TableMonitorRegistry#mightMatch(TableIdentifier)` 做方言选择前的保守白名单判定，`find(TableIdentifier, DataChangeDialect)` 使用方言规范化后做精确匹配。
- Produces: `JdbcTableMetadataResolver#resolve(Connection, TableIdentifier)`，缓存主键和列信息。

- [ ] **Step 1: 为确定性选择和冲突写失败测试**

```java
@Test
void selectsHighestOrderedSupportingAdapter() {
	DataChangeFrameworkAdapter fallback = adapter("mybatis", 0, true);
	DataChangeFrameworkAdapter plus = adapter("plus", 100, true);
	DataChangeFrameworkAdapterRegistry registry =
			new DataChangeFrameworkAdapterRegistry(Arrays.asList(fallback, plus));

	assertThat(registry.select(invocation())).isSameAs(plus);
}

@Test
void rejectsAmbiguousAdaptersAtSameOrder() {
	DataChangeFrameworkAdapterRegistry registry = new DataChangeFrameworkAdapterRegistry(
			Arrays.asList(adapter("a", 100, true), adapter("b", 100, true)));

	assertThatThrownBy(() -> registry.select(invocation()))
			.isInstanceOf(DataChangeException.class)
			.hasMessageContaining("AMBIGUOUS_FRAMEWORK_ADAPTER");
}
```

为方言注册表写同样的最高优先级和冲突测试；为 TableMonitorRegistry 写 schema 同名表与大小写规范化测试。

- [ ] **Step 2: 运行注册表测试并确认 RED**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeFrameworkAdapterRegistryTest,DataChangeDialectRegistryTest,TableMonitorRegistryTest test
```

Expected: FAIL，缺少 Registry 和 SPI 类型。

- [ ] **Step 3: 实现 SPI、注册表和原生 MyBatis 兜底**

接口签名固定为：

```java
public interface DataChangeFrameworkAdapter {
	boolean supports(DataChangeInvocation invocation);
	FrameworkChangeMetadata resolve(DataChangeInvocation invocation);
	int getOrder();
}

public interface DataChangeDialect {
	boolean supports(DatabaseMetadata metadata);
	TableIdentifier normalize(TableIdentifier table);
	int getOrder();
}
```

Task 2 先建立方言选择所需的最小接口；Task 4 在同一接口增加 SQL 分析和快照构建方法。原生适配器 order 为 `0` 且支持所有 MyBatis DML；它只合并显式配置、BoundSql 和 JDBC 元数据，不按 Mapper 方法名猜测语义。

- [ ] **Step 4: 实现 JDBC 元数据解析与缓存键**

缓存键必须包含数据源身份、数据库产品、catalog、schema、table。主键顺序使用 `DatabaseMetaData#getPrimaryKeys` 返回的 `KEY_SEQ`，列信息使用 `getColumns`。测试用 Mockito 返回乱序主键行，断言最终按 `KEY_SEQ` 排序。

```java
JdbcTableMetadata metadata = resolver.resolve(connection,
		new TableIdentifier(null, "PUBLIC", "ORDERS"));
assertThat(metadata.getKeyColumns()).containsExactly("TENANT_ID", "ORDER_NO");
```

- [ ] **Step 5: 运行 Task 2 测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest='*RegistryTest,JdbcTableMetadataResolverTest' test
```

Expected: PASS，冲突错误码分别为 `AMBIGUOUS_FRAMEWORK_ADAPTER` 和 `AMBIGUOUS_DIALECT`。

- [ ] **Step 6: 检查 Task 2 变更范围（不提交）**

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

---

### Task 3: Diff、Handler 调度和递归抑制

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/diff/ChangeDiffEngine.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/RowSnapshot.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dispatch/DataChangeDispatcher.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeScope.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeScopeHolder.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/diff/ChangeDiffEngineTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dispatch/DataChangeDispatcherTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeScopeHolderTest.java`

**Interfaces:**
- Produces: `ChangeDiffEngine#diff(TableIdentifier, TableMonitor, Map<String,String>, List<RowSnapshot>, List<RowSnapshot>)`，根据行在 before/after 中的存在性和逻辑删除列变化推导最终行操作。
- Produces: `DataChangeDispatcher#dispatch(DataChangeEvent)`，直接读取事件的 `ConsistencyLevel` 决定异常策略。
- Produces: `DataChangeScope.suppress()` 返回可嵌套、幂等关闭的 `AutoCloseable` 作用域。

- [ ] **Step 1: 为字段过滤、操作分类和空事件写失败测试**

```java
@Test
void emitsOnlyChangedIncludedColumns() {
	RowSnapshot before = row(keys("id", 1L), values("display_name", "A", "update_time", 10L));
	RowSnapshot after = row(keys("id", 1L), values("display_name", "B", "update_time", 11L));
	TableMonitor monitor = monitorIncluding("display_name", "update_time");
	monitor.setExcludeColumns(Collections.singletonList("update_time"));

	TableChange change = engine.diff(table("sys_user"), monitor,
			Collections.singletonMap("name", "display_name"),
			Collections.singletonList(before), Collections.singletonList(after));

	assertThat(change.getRows()).hasSize(1);
	assertThat(change.getRows().get(0).getFields())
			.extracting(FieldChange::getColumnName, FieldChange::getPropertyName)
			.containsExactly(tuple("display_name", "name"));
}

@Test
void omitsRowsWithoutMonitoredChanges() {
	assertThat(engine.diff(table("sys_user"), monitor(),
			Collections.<String, String>emptyMap(), rows(1, "A"), rows(1, "A"))
			.getRows()).isEmpty();
}
```

另写 INSERT 的 `null → value`、物理 DELETE 的 `value → null`、逻辑删除列进入 deleted-values 后生成 `LOGICAL_DELETE`、UPSERT 同时生成 INSERT/UPDATE 行、联合主键匹配测试。

- [ ] **Step 2: 为事务和非事务 Handler 失败写失败测试**

```java
@Test
void transactionalFailurePropagates() {
	DataChangeDispatcher dispatcher = dispatcher(failingHandler(), errorHandler);
	assertThatThrownBy(() -> dispatcher.dispatch(event(ConsistencyLevel.TRANSACTIONAL)))
			.isInstanceOf(DataChangeException.class)
			.hasMessageContaining("HANDLER_FAILED");
}

@Test
void bestEffortFailureContinuesAndReports() {
	DataChangeDispatcher dispatcher = dispatcher(failingHandler(0), recordingHandler(10), errorHandler);
	dispatcher.dispatch(event(ConsistencyLevel.BEST_EFFORT));
	assertThat(errorHandler.failures()).hasSize(1);
	assertThat(recordingHandler.events()).hasSize(1);
}
```

- [ ] **Step 3: 实现 Diff 和有序 Handler 调度**

Diff 使用 `RowIdentity` 建索引，不把主键转为 String。`before/after` Map 保留唯一键和监听列；`FieldChange` 只包含实际变化的监听列。Handler 按 `getOrder()` 升序执行，并在 `DataChangeScope.suppress()` 内调用，避免 Handler 写审计表时递归进入 DataChange；事务失败包装后抛出，非事务失败调用 ErrorHandler 并继续。

- [ ] **Step 4: 实现可嵌套递归抑制**

```java
try (DataChangeScope ignored = DataChangeScope.suppress()) {
	assertThat(DataChangeScopeHolder.isSuppressed()).isTrue();
}
assertThat(DataChangeScopeHolder.isSuppressed()).isFalse();
```

测试覆盖嵌套恢复、重复 close、异常退出和线程隔离。`close()` 不声明受检异常。

- [ ] **Step 5: 运行 Task 3 测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=ChangeDiffEngineTest,DataChangeDispatcherTest,DataChangeScopeHolderTest test
```

Expected: PASS。

- [ ] **Step 6: 检查 Task 3 变更范围（不提交）**

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

---

### Task 4: SQL 分析模型、H2 方言和 fail-closed 判定

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/DataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/BoundSqlContext.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/SqlTargetDetection.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/JSqlParserTargetDetector.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/StatementOperation.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/AnalyzedStatement.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/ChangeTarget.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/AssignedColumn.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/SnapshotQuery.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/SnapshotParameter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/AbstractJSqlParserDataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/H2DataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/SqlAnalysisException.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/H2DataChangeDialectTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/SqlTargetDetectionTest.java`

**Interfaces:**
- Consumes: `DataChangeDialect` from Task 2。
- Produces: `DataChangeDialect#analyze(BoundSqlContext)`、`buildBeforeQuery(ChangeTarget)`、`buildAfterQuery(ChangeTarget, List<RowIdentity>)`。
- Produces: `JSqlParserTargetDetector#detect(String)` 返回 `SqlTargetDetection`，在选择数据库方言前区分非 DML、CALL、可信目标表和无法判定目标的 DML。
- Produces: `List<AnalyzedStatement>`，每个语句包含 `statementIndex` 和零到多个 `ChangeTarget`。
- Produces: `StatementOperation` 包含 `INSERT`、`UPDATE`、`PHYSICAL_DELETE`、`MERGE`；`ChangeTarget` 保存该语句操作、目标表、WHERE/JOIN AST、赋值列、参数映射和唯一键提示。
- Produces: H2 快照 SQL，始终选择唯一键和监听列，并使用 `maxRows + 1`。

- [ ] **Step 1: 为 DML 分类、多语句和 CALL 跳过写失败测试**

```java
@ParameterizedTest
@CsvSource({
	"UPDATE sys_user SET name = ? WHERE id = ?,UPDATE,sys_user",
	"DELETE FROM sys_user WHERE id = ?,PHYSICAL_DELETE,sys_user",
	"INSERT INTO sys_user(id,name) VALUES (?,?),INSERT,sys_user",
	"MERGE INTO sys_user KEY(id) VALUES (?,?),MERGE,sys_user"
})
void analyzesSupportedDml(String sql, String operation, String table) {
	AnalyzedStatement statement = dialect.analyze(context(sql)).get(0);
	assertThat(statement.getTargets()).singleElement()
			.satisfies(target -> {
				assertThat(target.getOperation().name()).isEqualTo(operation);
				assertThat(target.getTable().getTable()).isEqualToIgnoringCase(table);
			});
}

@Test
void skipsCallButKeepsFollowingDml() {
	List<AnalyzedStatement> statements = dialect.analyze(
			context("CALL refresh_cache(); UPDATE sys_user SET name=? WHERE id=?"));
	assertThat(statements).hasSize(2);
	assertThat(statements.get(0).isAuditable()).isFalse();
	assertThat(statements.get(1).isAuditable()).isTrue();
}
```

同一测试类再加入 `UPDATE sys_user SET name=? WHERE id=? AND note='?'; DELETE FROM sys_user WHERE id=?`，断言两个 `AnalyzedStatement` 的 `statementIndex` 分别为 `0/1`，参数映射切片分别为前两个和最后一个，且字符串字面量中的 `?` 不计入参数位置；`SELECT * FROM sys_user` 与 `ALTER TABLE sys_user ADD COLUMN note varchar(20)` 各返回一个 `auditable=false` 结果。

- [ ] **Step 2: 运行方言测试并确认 RED**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=H2DataChangeDialectTest,SqlTargetDetectionTest test
```

Expected: FAIL，缺少分析模型和 H2 方言。

- [ ] **Step 3: 实现 JSqlParser AST 分析，不使用正则拆 SQL**

使用 `CCJSqlParserUtil.parseStatements(sql)`；针对 `Insert`、`Update`、`Delete`、`Merge` 建立 `ChangeTarget`。SELECT/DDL 返回 `auditable=false`；`Execute`/CALL 返回 `auditable=false`。解析异常时抛出 `DataChangeException(UNSUPPORTED_SQL)`，不能降级为字符串切割。

- [ ] **Step 4: 实现两阶段表判定和 fail-closed**

先由 `JSqlParserTargetDetector` 提取目标表，再用 `TableMonitorRegistry#mightMatch` 做不依赖方言的保守过滤；只有可能命中监听表后才按实际 Connection 选择 `DataChangeDialect`，并用 `find` 做规范化精确匹配和完整分析。AST 无法建立任何可信目标且 SQL 命令属于 DML 时抛 `UNSUPPORTED_SQL`；成功识别且可证明全部未配置时直接返回空计划，即使数据库产品没有内置方言也不应失败。测试断言未知数据库上的 `UPDATE other_table ...` 直接放行、未知数据库上的 `UPDATE monitored_table ...` 抛 `UNSUPPORTED_DIALECT`、非法 `UPDATE ???` 在原 DML 前失败。

- [ ] **Step 5: 实现 H2 更新前后快照 SQL**

更新前查询保留最终 WHERE/JOIN，投影为唯一键与监听列；事务模式追加 H2 支持的 `FOR UPDATE`。更新后按 `RowIdentity` 生成参数化联合键条件：

```sql
SELECT "ID", "NAME" FROM "SYS_USER"
WHERE ("ID" = ?)
FETCH FIRST 1001 ROWS ONLY
```

联合主键使用 `(k1 = ? AND k2 = ?) OR (...)`，所有值进入 `SnapshotParameter`。

- [ ] **Step 6: 运行 Task 4 测试并检查变更范围（不提交）**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=H2DataChangeDialectTest,SqlTargetDetectionTest test
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

Expected: 测试 PASS；差异只包含 Task 1-4 的计划内文件。

---

### Task 5: 快照执行、参数绑定和生成主键解析

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/SnapshotService.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/JdbcSnapshotExecutor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/SnapshotParameterBinder.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/RowIdentityResolver.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/GeneratedKeyResolver.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/JdbcSnapshotExecutorTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/RowIdentityResolverTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/snapshot/GeneratedKeyResolverTest.java`
- Test Resource: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/resources/datachange/h2-schema.sql`

**Interfaces:**
- Produces: `SnapshotService#readBefore(Connection, DataChangeDialect, ChangeTarget, JdbcTableMetadata, Configuration)` 和 `readAfter(Connection, DataChangeDialect, ChangeTarget, List<RowIdentity>, JdbcTableMetadata, Configuration)`，返回 `List<RowSnapshot>`。
- Produces: `SnapshotParameterBinder#bind(PreparedStatement, List<SnapshotParameter>, Configuration)`。
- Produces: `RowIdentityResolver#resolve(TableMonitor, FrameworkChangeMetadata, JdbcTableMetadata)`。
- Produces: `GeneratedKeyResolver#resolve(MappedStatement, Object, Statement, FrameworkChangeMetadata, List<String>)` 返回与插入参数顺序一致的 `List<RowIdentity>`；最后一个参数是已解析唯一键列。

- [ ] **Step 1: 为参数绑定、超限和联合主键写失败测试**

```java
@Test
void bindsSnapshotParametersThroughConfiguredTypeHandler() throws Exception {
	SnapshotParameter parameter = new SnapshotParameter(1, Status.ACTIVE, StatusTypeHandler.class);
	binder.bind(statement, Collections.singletonList(parameter), mybatisConfiguration);
	verify(statement).setString(1, "ACTIVE");
}

@Test
void rejectsSnapshotAtLimitPlusOne() {
	SnapshotQuery query = queryWithMaxRows(2);
	assertThatThrownBy(() -> executor.query(connection, queryReturningRows(3), metadata))
			.isInstanceOf(DataChangeException.class)
			.hasMessageContaining("SNAPSHOT_LIMIT_EXCEEDED");
}
```

- [ ] **Step 2: 运行快照测试并确认 RED**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=JdbcSnapshotExecutorTest,RowIdentityResolverTest,GeneratedKeyResolverTest test
```

Expected: FAIL，缺少快照实现。

- [ ] **Step 3: 实现 JDBC 快照执行器**

执行器使用传入 Connection 创建 `PreparedStatement`，只读取查询定义中的列；在 `DataChangeScope.suppress()` 中执行。读取到 `maxRows + 1` 时立即关闭 ResultSet/Statement 并抛超限异常。列值使用 ResultSet/Object 与配置 TypeHandler，不做字符串格式化。

- [ ] **Step 4: 实现唯一键和插入主键解析顺序**

严格按显式 `key-columns` → ORM metadata → JDBC PK 顺序。插入主键按参数已有值 → Adapter GeneratedKeyAccessor → JDBC generated keys/RETURNING → 方言回查顺序；没有身份时返回 `GENERATED_KEY_UNAVAILABLE`，不生成伪快照。

- [ ] **Step 5: 运行 Task 5 测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest='*Snapshot*Test,*IdentityResolverTest,*GeneratedKeyResolverTest' test
```

Expected: PASS，Mockito 验证 PreparedStatement 参数使用绑定 API，未调用拼接后的 SQL。

- [ ] **Step 6: 检查 Task 5 变更范围（不提交）**

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

---

### Task 6: 普通 DML 拦截器和 H2 纵向闭环

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeExecutionContext.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeExecutionContextHolder.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/StatementChangePlan.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeEngine.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeInterceptor.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/JdbcTransactionStateResolver.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeInterceptorH2Test.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeExecutionContextHolderTest.java`
- Test Fixture: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/fixture/TestUserMapper.java`
- Test Resource: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/resources/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/fixture/TestUserMapper.xml`

**Interfaces:**
- Consumes: Registry、Dialect、Snapshot、Diff、Dispatcher from Tasks 2-5。
- Produces: `DataChangeInterceptor`，同时协调 `Executor.update` 与 `StatementHandler.prepare`。
- Produces: `DataChangeEngine#beforeStatement(DataChangeExecutionContext, BoundSqlContext)` 返回 `List<StatementChangePlan>`，`afterStatement(DataChangeExecutionContext, List<StatementChangePlan>, List<Integer>)` 返回 `List<StatementChange>`，同时覆盖单 SQL 和一个 BoundSql 内的多语句。

- [ ] **Step 1: 写原生 MyBatis H2 失败集成测试**

测试使用真实 `SqlSessionFactory`、H2 表 `sys_user(id bigint primary key, name varchar, deleted int)` 和记录型 Handler：

```java
@Test
void recordsUpdateBeforeAndAfterValues() {
	mapper.insert(new TestUser(1L, "before", 0));
	recordingHandler.clear();

	assertThat(mapper.rename(1L, "after")).isEqualTo(1);

	RowChange row = onlyRow(recordingHandler.singleEvent());
	assertThat(row.getOperation()).isEqualTo(DataChangeOperation.UPDATE);
	assertThat(row.getBefore()).containsEntry("NAME", "before");
	assertThat(row.getAfter()).containsEntry("NAME", "after");
}
```

同文件增加以下具名用例：`recordsInsertIncludingDatabaseDefaults`、`recordsPhysicalDeleteFromBeforeSnapshot`、`classifiesConfiguredLogicalDelete`、`omitsZeroRowUpdate`、`bypassesUnmonitoredTable`、`readsAfterByCapturedIdentityWhenRowLeavesOriginalWhere`、`readsGeneratedKey`、`rejectsLimitPlusOneBeforeDml`。每个用例都断言 Handler 次数、RowChange 操作、RowIdentity 和 before/after；超限用例还要断言数据库内容未变化。

- [ ] **Step 2: 运行 H2 集成测试并确认 RED**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeInterceptorH2Test test
```

Expected: FAIL，缺少拦截器/Engine；数据库夹具本身可初始化。

- [ ] **Step 3: 实现双层拦截和最终 BoundSql 捕获**

拦截签名至少包含：

```java
@Intercepts({
	@Signature(type = Executor.class, method = "update",
			args = {MappedStatement.class, Object.class}),
	@Signature(type = StatementHandler.class, method = "prepare",
			args = {Connection.class, Integer.class}),
	@Signature(type = StatementHandler.class, method = "update",
			args = {Statement.class})
})
```

`Executor.update` 建立/关闭调用上下文并在 proceed 返回后生成事件。`StatementHandler.prepare` 必须先调用 `proceed()`，让外层调用进入已注册在 DataChange 之前的 ORM 插件并完成 SQL 改写；prepare 返回、业务 DML 尚未执行时，再读取最终 `BoundSql`、实际 Connection 和更新前快照。`StatementHandler.update` 在 `proceed()` 后捕获 JDBC update counts 和 generated keys，写回当前 `List<StatementChangePlan>`。所有 push/pop 置于 `try/finally`。

- [ ] **Step 4: 实现事务自动识别和 Handler 语义**

`JdbcTransactionStateResolver` 以实际 Connection 的 `getAutoCommit()==false` 判定 `TRANSACTIONAL`，否则 `BEST_EFFORT`。事务场景更新前快照使用方言行锁；非事务不加锁。原 DML 前的目标识别、方言、唯一键和 before 快照失败在两种模式下都直接抛出并阻止 DML。原 DML 后的 generated key、after 快照、Diff 或 Handler 失败：`TRANSACTIONAL` 向 Mapper 调用抛出以允许回滚；`BEST_EFFORT` 只调用 ErrorHandler、保留业务返回值且不生成伪造的成功事件。

- [ ] **Step 5: 增加事务回滚和非事务错误测试**

```java
@Test
void transactionalHandlerFailureRollsBackBusinessUpdate() {
	withManualTransaction(session -> {
		failingHandler.enable();
		assertThatThrownBy(() -> session.getMapper(TestUserMapper.class).rename(1L, "after"));
		session.rollback();
	});
	assertThat(selectName(1L)).isEqualTo("before");
}

@Test
void autoCommitHandlerFailureReportsWithoutPretendingBusinessFailure() {
	failingHandler.enable();
	assertThat(autoCommitMapper.rename(1L, "after")).isEqualTo(1);
	assertThat(selectName(1L)).isEqualTo("after");
	assertThat(errorHandler.failures()).singleElement()
			.extracting(DataChangeFailure::getCode).isEqualTo(DataChangeErrorCode.HANDLER_FAILED);
}
```

同一测试类再让 after 快照查询抛出 `SQLException`：手工事务断言 Mapper 抛 `SNAPSHOT_QUERY_FAILED` 且 rollback 后值未变；auto-commit 断言 Mapper 返回影响行数、业务值已更新、ErrorHandler 收到 `SNAPSHOT_QUERY_FAILED` 且 recording Handler 没有收到事件。再覆盖 DML 本身异常、影响零行和嵌套 Mapper 异常时 `DataChangeExecutionContextHolder` 最终为空。

- [ ] **Step 6: 运行纵向闭环测试并检查变更范围（不提交）**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeInterceptorH2Test,DataChangeExecutionContextHolderTest test
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

Expected: PASS；差异只包含 Task 1-6 的计划内文件。

---

### Task 7: JDBC Batch、多语句和多表聚合

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/BatchChangePlan.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeBatchCoordinator.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/JdbcUpdateCountCollector.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeInterceptor.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeEngine.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeBatchH2Test.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeMultiStatementTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/DataChangeMultiTableTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/JdbcUpdateCountCollectorTest.java`
- Modify Test Fixture: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/fixture/TestUserMapper.java`
- Modify Test Resource: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/resources/com/xiongx9527/fw/plus/mybatis/datachange/interceptor/fixture/TestUserMapper.xml`

**Interfaces:**
- Produces: `DataChangeBatchCoordinator#collect(DataChangeExecutionContext, List<StatementChangePlan>)`、`flush(DataChangeExecutionContext, List<BatchResult>)` 和 `discard(DataChangeExecutionContext)`。
- Produces: `JdbcUpdateCountCollector#collect(Statement, int)`，按 JDBC `getMoreResults()/getUpdateCount()` 收集多语句影响行数。
- Produces: 一次实际 flush 一个 `DataChangeEvent`，同一行保留批次第一次 before 和最后一次 after。
- Produces: 一个多表 SQL 的 `StatementChange` 包含多个 `TableChange`，`affectedRows` 只保存在 Statement 层。

- [ ] **Step 1: 为 Batch 初始/最终聚合写失败测试**

```java
@Test
void batchCollapsesIntermediateChangesForSameRow() {
	try (SqlSession session = factory.openSession(ExecutorType.BATCH, false)) {
		TestUserMapper mapper = session.getMapper(TestUserMapper.class);
		mapper.rename(1L, "middle");
		mapper.rename(1L, "final");
		session.flushStatements();
		session.commit();
	}

	RowChange row = onlyRow(recordingHandler.singleEvent());
	assertThat(row.getBefore()).containsEntry("NAME", "before");
	assertThat(row.getAfter()).containsEntry("NAME", "final");
}
```

增加 `flushesPendingBatchBeforeCommit`、`flushesPendingBatchBeforeQuery`、`discardsPendingBatchOnRollback`、`groupsCompositeKeyBatchByIdentity`、`transactionalHandlerFailurePreventsCommit`、`bestEffortHandlerFailureKeepsFlushedBusinessRows`。前五个断言事件数量、批次初始/最终值和数据库提交状态；最后一个断言 ErrorHandler 被调用但 auto-commit 批次结果保留。

- [ ] **Step 2: 为多语句与多表层级写失败测试**

```java
@Test
void multiTableStatementKeepsOneStatementAndTwoTables() {
	DataChangeEvent event = executeMultiTableUpdate();
	assertThat(event.getStatements()).hasSize(1);
	assertThat(event.getStatements().get(0).getTables())
			.extracting(change -> change.getTable().getTable())
			.containsExactlyInAnyOrder("account", "account_summary");
}
```

多语句测试断言两个 DML 的 `statementIndex` 为 `0,1` 且分别使用 JDBC 返回的 update count；若驱动返回 `Statement.SUCCESS_NO_INFO`，原值保留为 `-2`，不得猜测行数。若第一条是 CALL、第二条是 DML，事件只包含 `statementIndex=1` 的 DML，不为 CALL 创建空 `StatementChange`。

- [ ] **Step 3: 扩展拦截签名并实现 BatchCoordinator**

在 Task 6 的 `@Intercepts` 上精确增加以下签名：

```java
@Signature(type = StatementHandler.class, method = "batch",
		args = {Statement.class}),
@Signature(type = Executor.class, method = "flushStatements", args = {}),
@Signature(type = Executor.class, method = "commit", args = {boolean.class}),
@Signature(type = Executor.class, method = "rollback", args = {boolean.class}),
@Signature(type = Executor.class, method = "query",
		args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class}),
@Signature(type = Executor.class, method = "query",
		args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class,
				CacheKey.class, BoundSql.class})
```

`StatementHandler.batch` 在 `proceed()` 调用 `addBatch` 前，必须用该次 Handler 的最终 BoundSql/参数创建或合并 `StatementChangePlan` 并读取首次 before；同 SQL 的后续参数不能依赖只执行一次的 `prepare`。`BaseExecutor.commit()` 和 `BatchExecutor.doQuery()` 内部会直接调用自身的 flush 方法，不会再次经过代理，因此 commit/query 拦截器发现待处理批次时，先对 `invocation.getTarget()` 显式调用一次 `flushStatements()` 并保留返回的 `List<BatchResult>`，完成 after/Diff/派发后再 `proceed()`；原方法内部的第二次 flush 为空，不改变原有提交或查询前刷新语义。显式 flush 直接围绕 `proceed()` 收集结果；rollback 在 `proceed()` 前 discard，绝不读取 after 或派发事件。

- [ ] **Step 4: 保持 JDBC Batch 语义**

不得把每个 `addBatch` 改成普通 update。Coordinator 只围绕实际 `executeBatch` 聚合快照。相同 SQL 的不同参数分别保留 ChangeTarget，按 RowIdentity 去重；JDBC 返回的 `BatchResult#getUpdateCounts()` 汇总为 Statement 影响行数，`SUCCESS_NO_INFO` 保持未知标记，`EXECUTE_FAILED` 使该批次不生成成功事件。

- [ ] **Step 5: 运行 Batch/多语句/多表测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest=DataChangeBatchH2Test,DataChangeMultiStatementTest,DataChangeMultiTableTest,JdbcUpdateCountCollectorTest test
```

Expected: PASS，且原 `DataChangeInterceptorH2Test` 同时回归通过。

- [ ] **Step 6: 检查 Task 7 变更范围（不提交）**

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

---

### Task 8: 内置数据库方言和真实数据库契约

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/MySqlDataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/MariaDbDataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/PostgreSqlDataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/OracleDataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/SqlServerDataChangeDialect.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/StandardDialectSet.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/AbstractDataChangeDialectContract.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/MySqlDataChangeDialectTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/MariaDbDataChangeDialectTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/PostgreSqlDataChangeDialectTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/OracleDataChangeDialectTest.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/SqlServerDataChangeDialectTest.java`
- Integration Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/it/MySqlDataChangeDialectIT.java`
- Integration Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/it/MariaDbDataChangeDialectIT.java`
- Integration Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/it/PostgreSqlDataChangeDialectIT.java`
- Integration Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/it/OracleDataChangeDialectIT.java`
- Integration Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/test/java/com/xiongx9527/fw/plus/mybatis/datachange/dialect/it/SqlServerDataChangeDialectIT.java`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/pom.xml`

**Interfaces:**
- Consumes: `AbstractJSqlParserDataChangeDialect` and common contract from Task 4。
- Produces: built-in dialect list recognized from JDBC product name/version。
- Produces: per-dialect limit, row-lock, identifier quoting, UPSERT/MERGE and multi-table DML behavior。

- [ ] **Step 1: 建立公共方言契约失败测试**

每个方言单测调用同一契约方法，至少覆盖：

```java
protected void assertCompositeKeyAfterQuery(DataChangeDialect dialect) {
	SnapshotQuery query = dialect.buildAfterQuery(targetWithKeys("tenant_id", "order_no"),
			Arrays.asList(identity(7L, "A"), identity(8L, "B")));
	assertThat(query.getParameters()).extracting(SnapshotParameter::getValue)
			.containsExactly(7L, "A", 8L, "B");
	assertThat(query.getMaxRows()).isEqualTo(1001);
}
```

再分别断言 MySQL/MariaDB `ON DUPLICATE KEY`、PostgreSQL `ON CONFLICT ... RETURNING`、Oracle/SQL Server `MERGE`、各数据库锁语法和标识符引用。

- [ ] **Step 2: 运行方言单测并确认 RED**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest='*DataChangeDialectTest' test
```

Expected: FAIL，缺少 MySQL、MariaDB、PostgreSQL、Oracle 和 SQL Server 五个新增方言类。

- [ ] **Step 3: 实现方言并注册 StandardDialectSet**

各方言共享 AST 提取，不复制事件/Diff。每个类只覆盖产品识别、quote、limit、lock、UPSERT/MERGE 和厂商多表语法。无法确定 MySQL `ON DUPLICATE KEY` 的具体冲突唯一索引时，使用 JDBC 唯一索引元数据；仍有多个可匹配索引时抛 `ROW_IDENTITY_UNAVAILABLE`。

- [ ] **Step 4: 增加真实数据库集成测试 Profile**

在 POM 添加 `datachange-dialect-it` Profile 和测试范围 JDBC 驱动。Profile 激活后，每个 IT 必须读取对应环境变量且缺失时失败，不静默 skip：

```xml
<dependency><groupId>mysql</groupId><artifactId>mysql-connector-java</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.mariadb.jdbc</groupId><artifactId>mariadb-java-client</artifactId><scope>test</scope></dependency>
<dependency><groupId>org.postgresql</groupId><artifactId>postgresql</artifactId><scope>test</scope></dependency>
<dependency><groupId>com.oracle.database.jdbc</groupId><artifactId>ojdbc8</artifactId><scope>test</scope></dependency>
<dependency><groupId>com.microsoft.sqlserver</groupId><artifactId>mssql-jdbc</artifactId><scope>test</scope></dependency>
```

这些驱动版本继续由项目现有 BOM 管理；Profile 配置 `maven-failsafe-plugin` 执行 `**/*IT.java`，不引入容器运行时。

```text
DATACHANGE_MYSQL_JDBC_URL / USERNAME / PASSWORD
DATACHANGE_MARIADB_JDBC_URL / USERNAME / PASSWORD
DATACHANGE_POSTGRES_JDBC_URL / USERNAME / PASSWORD
DATACHANGE_ORACLE_JDBC_URL / USERNAME / PASSWORD
DATACHANGE_SQLSERVER_JDBC_URL / USERNAME / PASSWORD
```

每个 IT 创建独立测试 schema/table，运行 insert/update/delete/upsert/merge、联合主键、generated keys/RETURNING、数据库默认值、触发器改值、事务回滚、无事务流程和并发更新前快照；逐条断言 JDBC update count 与事件行数。所有对象名带随机后缀，并在 finally 只删除本测试创建的对象。

- [ ] **Step 5: 运行方言单测和已配置的真实数据库契约**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Dtest='*DataChangeDialectTest' test

./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Pdatachange-dialect-it verify
```

Expected: 单测全部 PASS；在五套环境变量齐备的验证环境中，五个 IT 全部 PASS。若执行环境缺少任一数据库，任务不得宣称完整方言集成验证完成，必须在交付说明中列出缺失项。

- [ ] **Step 6: 检查 Task 8 变更范围（不提交）**

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange
```

---

### Task 9: 重写 MyBatis-Plus DataChange 适配器

**Files:**
- Modify: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/pom.xml`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/DataChangeInterceptor.java`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/DataChangeHandler.java`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/DataChangeHelper.java`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/properties/ChangeDataRecord.java`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/properties/DataChangeProperties.java`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/test/java/com/xiongx9527/fw/plus/mp/datachange/DataChangeHelperTest.java`
- Delete: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/test/java/com/xiongx9527/fw/plus/mp/datachange/properties/DataChangePropertiesTest.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/MybatisPlusDataChangeAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/MybatisPlusEntityTypeResolver.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/main/java/com/xiongx9527/fw/plus/mp/datachange/MybatisPlusGeneratedKeyAccessor.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange/src/test/java/com/xiongx9527/fw/plus/mp/datachange/MybatisPlusDataChangeAdapterTest.java`

**Interfaces:**
- Consumes: `DataChangeFrameworkAdapter` and metadata model from Task 2。
- Produces: Plus adapter order `200`，高于 Flex 和原生兜底。
- Uses: `TableInfoHelper.getTableInfo(Class<?>)`、`TableInfo#getKeyColumn/getKeyProperty/getFieldList/isLogicDelete/getVersionFieldInfo`。

- [ ] **Step 1: 写不依赖项目基类的 Plus 元数据失败测试**

```java
@TableName("audit_user")
static class PlusUser {
	@TableId("user_id") private Long id;
	@TableField("display_name") private String name;
	@TableField(exist = false) private String transientValue;
	@TableLogic private Integer deleted;
	@Version private Integer version;
}

@Test
void resolvesPlusMetadataWithoutMpBaseEntity() {
	FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(PlusUser.class));
	assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	assertThat(metadata.getKeyColumns()).containsExactly("user_id");
	assertThat(metadata.getPropertyColumnMappings())
			containsEntry("name", "display_name")
			.doesNotContainKey("transientValue");
	assertThat(metadata.getLogicalDelete().getColumn()).isEqualTo("deleted");
}
```

增加 `resolvesEntityFromBaseMapperGeneric`、`resolvesEntityFromEtParam`、`resolvesEntityFromEwParam`、`mapsLambdaUpdateWrapperColumns`、`mapsLogicalDeleteAndVersionFields`、`mapsInsertAndUpdateFillFields`、`readsGeneratedKeyAfterInsert`；每个测试分别断言实体类型、数据库列名、忽略列、逻辑删除元数据、版本/填充字段和有序主键值。

- [ ] **Step 2: 修改 POM 并运行测试确认 RED**

POM 删除 `fw-plus-mp-base` 和 Spring 依赖，新增：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-mybatis-datachange</artifactId>
</dependency>
<dependency>
	<groupId>com.baomidou</groupId>
	<artifactId>mybatis-plus-core</artifactId>
</dependency>
```

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mp/fw-plus-mp-datachange -am \
  -Dtest=MybatisPlusDataChangeAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，缺少新 Adapter。

- [ ] **Step 3: 实现实体类型解析和 TableInfo 映射**

解析顺序为直接实体/ParamMap 实体 → Wrapper entityClass → Mapper `BaseMapper<T>` 泛型。通过 `TableInfoHelper` 提取主键、property/column、逻辑删除、版本和 fill 字段；最终目标表和条件仍使用公共层捕获的最终 SQL。

- [ ] **Step 4: 删除旧实现并增加依赖边界测试**

删除旧类和旧测试后运行：

```bash
rg -n "MpBaseEntity|MpBaseMapper|DataChangeProperties|ChangeDataRecord" \
  fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange
```

Expected: 无输出。Adapter 测试必须覆盖 `@TableField(exist=false)` 不进入映射。

- [ ] **Step 5: 运行 MP 模块测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mp/fw-plus-mp-datachange -am test
```

Expected: BUILD SUCCESS。

- [ ] **Step 6: 检查 Task 9 变更范围（不提交）**

```bash
git diff -- fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange
```

---

### Task 10: 新增 MyBatis-Flex DataChange 适配器

**Files:**
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf/fw-plus-mf-datachange/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf/pom.xml`
- Modify: `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf/fw-plus-mf-datachange/src/main/java/com/xiongx9527/fw/plus/mf/datachange/MybatisFlexDataChangeAdapter.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf/fw-plus-mf-datachange/src/main/java/com/xiongx9527/fw/plus/mf/datachange/MybatisFlexEntityTypeResolver.java`
- Create: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf/fw-plus-mf-datachange/src/main/java/com/xiongx9527/fw/plus/mf/datachange/MybatisFlexGeneratedKeyAccessor.java`
- Test: `fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf/fw-plus-mf-datachange/src/test/java/com/xiongx9527/fw/plus/mf/datachange/MybatisFlexDataChangeAdapterTest.java`

**Interfaces:**
- Consumes: `DataChangeFrameworkAdapter` and metadata model from Task 2。
- Produces: Flex adapter order `100`，低于 Plus、高于原生 MyBatis。
- Uses: `TableInfoFactory.ofEntityClass(Class<?>)`、`TableInfo#getPrimaryColumns/getPropertyColumnMapping/getLogicDeleteColumn/getVersionColumn/getColumnInfoList`。

- [ ] **Step 1: 写 Flex 元数据失败测试**

```java
@Table("audit_user")
static class FlexUser {
	@Id private Long userId;
	@Column("display_name") private String name;
	@Column(ignore = true) private String transientValue;
	@Column(isLogicDelete = true) private Integer deleted;
	@Column(version = true) private Integer version;
}

@Test
void resolvesFlexMetadataAndIgnoredColumns() {
	FrameworkChangeMetadata metadata = adapter.resolve(invocationFor(FlexUser.class));
	assertThat(metadata.getTable().getTable()).isEqualTo("audit_user");
	assertThat(metadata.getKeyColumns()).containsExactly("user_id");
	assertThat(metadata.getPropertyColumnMappings())
			.containsEntry("name", "display_name")
			.doesNotContainKey("transientValue");
	assertThat(metadata.getLogicalDelete().getColumn()).isEqualTo("deleted");
}
```

增加 `resolvesEntityFromBaseMapperGeneric`、`resolvesEntityFromQueryWrapper`、`usesFinalSqlTableForDynamicSchema`、`mapsLogicalDeleteAndVersionColumns`、`mapsOnInsertAndOnUpdateColumns`、`readsGeneratedKeyAfterInsert`；分别断言 `TableInfoFactory` 元数据与最终 SQL 表名的职责边界。

- [ ] **Step 2: 创建模块 POM 并确认 RED**

模块依赖：

```xml
<dependency>
	<groupId>com.xiongx9527.fw.plus</groupId>
	<artifactId>fw-plus-mybatis-datachange</artifactId>
</dependency>
<dependency>
	<groupId>com.mybatis-flex</groupId>
	<artifactId>mybatis-flex-core</artifactId>
</dependency>
```

Run:

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mf/fw-plus-mf-datachange -am \
  -Dtest=MybatisFlexDataChangeAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test
```

Expected: FAIL，缺少 Flex Adapter。

- [ ] **Step 3: 实现 TableInfoFactory 映射和动态表语义**

通过 `TableInfoFactory.ofEntityClass` 提取映射；过滤 `ColumnInfo#isIgnore()`；使用 `getPrimaryColumns()`、`getLogicDeleteColumn()`、`getVersionColumn()`。动态 schema/table 只补充 FrameworkMetadata，最终运行表名以公共层捕获的 BoundSql 为准。

- [ ] **Step 4: 验证 Plus/Flex 同时存在时选择确定**

在测试 Classpath 同时加入 Plus Core 和 Flex Core，分别构造两类 Mapper invocation，断言 Registry 为 Plus Mapper 选择 order 200 Adapter、Flex Mapper 选择 order 100 Adapter、原生 Mapper 选择 order 0 Adapter。

- [ ] **Step 5: 运行 Flex 和跨适配测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mf/fw-plus-mf-datachange,fw-plus-mp/fw-plus-mp-datachange -am test
```

Expected: BUILD SUCCESS。

- [ ] **Step 6: 检查 Task 10 变更范围（不提交）**

```bash
git diff -- \
  fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mf \
  fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml
```

---

### Task 11: Spring Boot 自动配置、拦截器顺序和 Starter 接线

**Files:**
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/MybatisDataChangeAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/SqlSessionFactoryDataChangeRegistrar.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/DataChangeConfigurationValidator.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/DefaultDataChangeErrorHandler.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mp/MpDataChangeAdapterAutoConfiguration.java`
- Create: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mf/MfDataChangeAdapterAutoConfiguration.java`
- Delete: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mp/MpDataChangeAutoConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mp/MpAutoConfiguration.java`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring.factories`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`
- Modify: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-mp-spring-boot-starter/pom.xml`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/MybatisDataChangeAutoConfigurationTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/DataChangeConfigurationValidatorTest.java`
- Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis/datachange/SqlSessionFactoryDataChangeRegistrarTest.java`
- Modify Test: `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mp/MpAutoConfigurationTest.java`

**Interfaces:**
- Produces: `@ConfigurationProperties(prefix="fw.plus.mybatis-datachange") DataChangeConfiguration`。
- Produces: `DataChangeConfigurationValidator#afterPropertiesSet()`，在属性绑定完成后调用 `DataChangeConfiguration#validate()`。
- Produces: one `DataChangeInterceptor` bean and registration into every `SqlSessionFactory` exactly once。
- Produces: `SqlSessionFactoryDataChangeRegistrar#postProcessAfterInitialization(Object, String)`，对现有和后创建的 Factory 幂等注册并校验 DataChange 位于拦截器列表末尾。
- Produces: 原生 MyBatis fallback Adapter、内置方言 Registry 及 conditional Plus/Flex Adapter beans，不把任一 ORM 设为公共模块必需依赖。

- [ ] **Step 1: 写自动配置条件失败测试**

```java
@Test
void registersInterceptorOnlyWithConfiguredTableAndHandler() {
	contextRunner
			.withPropertyValues(
					"fw.plus.mybatis-datachange.enabled=true",
					"fw.plus.mybatis-datachange.tables[0].name=sys_user")
			.withUserConfiguration(HandlerConfiguration.class, TwoSqlSessionFactories.class)
			.run(context -> {
				assertThat(context).hasSingleBean(DataChangeInterceptor.class);
				assertThat(plugins(factoryOne(context))).containsExactlyOnce(interceptor(context));
				assertThat(plugins(factoryTwo(context))).containsExactlyOnce(interceptor(context));
			});
}
```

增加 `disabledDoesNotRegister`、`emptyTablesDoNotRegister`、`missingHandlerDoesNotRegister`、`registersEachFactoryExactlyOnce`、`providesDefaultErrorHandlerWithoutSensitiveValues`、`startsWithNativeMybatisOnly`、`startsWithPlusOnly`、`startsWithFlexOnly`、`selectsDialectFromEachFactoryActualConnection`；后三类 Classpath 场景使用 `FilteredClassLoader`，动态数据源用两个返回不同 productName 的 Connection 断言不会缓存成同一方言。

- [ ] **Step 2: 运行 Boot 测试并确认 RED**

先安装 Core 产物：

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/pom.xml \
  -pl fw-plus-mybatis/fw-plus-mybatis-datachange,fw-plus-mybatis/fw-plus-mp/fw-plus-mp-datachange,fw-plus-mybatis/fw-plus-mf/fw-plus-mf-datachange \
  -am install -DskipTests

./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=MybatisDataChangeAutoConfigurationTest,DataChangeConfigurationValidatorTest,SqlSessionFactoryDataChangeRegistrarTest test
```

Expected: FAIL，缺少新自动配置。

- [ ] **Step 3: 实现公共自动配置和多 Factory 注册**

`MybatisDataChangeAutoConfiguration` 必须：

```java
@Bean
@ConfigurationProperties(prefix = "fw.plus.mybatis-datachange")
@ConditionalOnMissingBean
public DataChangeConfiguration mybatisDataChangeConfiguration() {
	return new DataChangeConfiguration();
}
```

同一自动配置还要提供 `DataChangeConfigurationValidator`、`MybatisDataChangeFrameworkAdapter`、`DataChangeFrameworkAdapterRegistry`、`StandardDialectSet`、`DataChangeDialectRegistry`、Snapshot/Diff/Dispatcher/Engine 依赖和 `@ConditionalOnMissingBean(DataChangeErrorHandler.class)` 的默认错误处理器。Registry 构造时把内置方言与用户自定义 `DataChangeDialect` 合并，并通过 Task 2 的同优先级冲突检查拒绝歧义。默认 ErrorHandler 只记录 eventId、statementId、TableIdentifier、ConsistencyLevel、错误码和异常类型/消息，不输出 SQL 参数或 before/after 值。

仅在 enabled、至少一张表、至少一个 Handler 时创建 Interceptor。`SqlSessionFactoryDataChangeRegistrar` 实现 `BeanPostProcessor`，使用 `ObjectProvider<DataChangeInterceptor>` 避免提前实例化；`postProcessAfterInitialization` 遇到 `SqlSessionFactory` 时检查 `factory.getConfiguration().getInterceptors()`，不存在则 `addInterceptor`。DataChange Bean 使用 `@Order(Ordered.LOWEST_PRECEDENCE)` 保证被 MyBatis 自动收集时排在最后；若同类型实例已存在但不在列表末尾，启动失败并报告插件顺序，禁止通过反射修改 MyBatis 私有列表。

- [ ] **Step 4: 固定 ORM 适配和插件顺序**

Plus/Flex Adapter 自动配置各自 `@ConditionalOnClass`，并分别使用 `@ConditionalOnMissingBean(MybatisPlusDataChangeAdapter.class)`、`@ConditionalOnMissingBean(MybatisFlexDataChangeAdapter.class)`；不能按公共 `DataChangeFrameworkAdapter` 接口判空，否则一个 Adapter 会错误阻止另一个。DataChange Interceptor 必须在每个 `Configuration#getInterceptors()` 中位于最后，成为最外层代理；其 `prepare` 拦截先 `proceed()` 经过租户、数据权限、逻辑删除和乐观锁插件，再读取最终 `BoundSql` 并做前快照。测试断言 DataChange 位于列表末尾，并使用两个记录型 Interceptor 验证捕获 SQL 已包含追加条件。

- [ ] **Step 5: 更新注册资源、POM 和旧 MpAutoConfiguration 测试**

同时维护 `spring.factories` 与 `AutoConfiguration.imports`。删除旧 `MpDataChangeAutoConfiguration` 导入，MP Starter 继续依赖 `fw-plus-mp-datachange`，由其传递引入公共模块；Boot autoconfigure 增加公共、MP 和 MF datachange optional 依赖。

- [ ] **Step 6: 运行 Boot 自动配置测试**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=MybatisDataChangeAutoConfigurationTest,DataChangeConfigurationValidatorTest,SqlSessionFactoryDataChangeRegistrarTest,MpAutoConfigurationTest test
```

Expected: PASS；两个 SqlSessionFactory 都只注册一次；缺少 Handler 时不创建拦截器。

- [ ] **Step 7: 检查 Task 11 变更范围（不提交）**

```bash
git diff -- \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mp \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mf \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/resources/META-INF \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mybatis \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mp/MpAutoConfigurationTest.java \
  fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-mp-spring-boot-starter/pom.xml
```

---

### Task 12: 样例、文档和全链路验证

**Files:**
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-mp-spring-boot-starter-sample/src/main/java/com/xiongx9527/fw/plus/boot/mp/sample/handler/MpSampleDataChangeHandler.java`
- Modify: `fw-plus-sample/fw-plus-boot-samples/fw-plus-mp-spring-boot-starter-sample/src/main/resources/application.yml`
- Create: `docs/mybatis-datachange.md`
- Modify: `docs/superpowers/specs/2026-08-07-mybatis-datachange-design.md` only if implementation evidence requires correcting a proven design detail。

**Interfaces:**
- Consumes: final API and property prefix from Tasks 1-11。
- Produces: compilable Handler example and safe YAML table configuration。
- Produces: user documentation for native/Plus/Flex usage, dialect SPI, transaction semantics, Batch and unsupported CALL boundary。

- [ ] **Step 1: 迁移样例 Handler 和 YAML**

Handler 改为实现公共接口：

```java
@Component
public class MpSampleDataChangeHandler implements DataChangeHandler {
	@Override
	public boolean supports(DataChangeEvent event) {
		return true;
	}

	@Override
	public void handle(DataChangeEvent event) {
		log.info("dataChangeEvent:{}", JSON.toJSONString(event));
	}

	@Override
	public int getOrder() {
		return 0;
	}
}
```

MP 样例 YAML 删除 `fw.plus.mp-datachange`，替换为：

```yaml
fw:
  plus:
    mybatis-datachange:
      enabled: true
      max-snapshot-rows: 1000
      tables:
        - name: test_order
          key-columns:
            - id
          include-columns:
            - status
```

不得改动样例现有数据库地址、用户名或密码；本任务只改 DataChange 配置块。

- [ ] **Step 2: 编写完整用户文档**

`docs/mybatis-datachange.md` 必须包含：

- 模块依赖：原生、Plus、Flex 三种坐标。
- 原生 MyBatis 手工构造 Registry/Engine/Interceptor 并调用 `Configuration#addInterceptor` 的完整示例。
- 配置字段和 schema/catalog 同名表示例。
- Event → Statement → Table → Row → Field 模型。
- Handler 和 ErrorHandler 示例。
- TRANSACTIONAL 与 BEST_EFFORT 行为对比。
- 事务 Handler 写数据库审计/Outbox 的示例，以及不直接发送不可回滚外部消息的边界。
- Batch 聚合语义和 `1000` 行默认上限。
- 内置方言矩阵与 `DataChangeDialect` 扩展示例。
- `CALL`/存储过程、SELECT、DDL 非目标。
- 未知 DML fail-closed 和如何通过新增方言解决。

- [ ] **Step 3: 执行 Core 全量测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/pom.xml clean test
```

Expected: Core 全部模块 `BUILD SUCCESS`。若并发 Maven 污染 `target/classes`，停止并重新运行一次独占命令，不把受污染结果作为证据。

- [ ] **Step 4: 安装 Core 并执行 Boot 目标测试**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/pom.xml install -DskipTests

./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/pom.xml \
  -Dtest=MybatisDataChangeAutoConfigurationTest,DataChangeConfigurationValidatorTest,SqlSessionFactoryDataChangeRegistrarTest,MpAutoConfigurationTest test
```

Expected: 两个命令均 `BUILD SUCCESS`。

- [ ] **Step 5: 验证 Starter 和样例聚合器**

```bash
./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl fw-plus-boot-starters/fw-plus-mp-spring-boot-starter -am test

./fw-plus-boot/mvnw -f fw-plus-boot/fw-plus-boot-project/pom.xml \
  -pl fw-plus-boot-starters/fw-plus-mp-spring-boot-starter -am install -DskipTests

./fw-plus-core/mvnw -f fw-plus-sample/pom.xml \
  -pl fw-plus-boot-samples/fw-plus-mp-spring-boot-starter-sample -am package -DskipTests

./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
```

Expected: Starter 测试和安装成功；MP Sample 主代码完成编译打包；Sample 聚合 `validate` 成功。

- [ ] **Step 6: 运行方言真实数据库契约**

```bash
./fw-plus-core/mvnw -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mybatis-datachange \
  -Pdatachange-dialect-it verify
```

Expected: MySQL、MariaDB、PostgreSQL、Oracle、SQL Server 五个外部数据库 IT 全部 PASS；H2 公共流程测试已在前序任务通过。没有齐备环境时，明确记录未运行的数据库，不得用 H2 结果替代。

- [ ] **Step 7: 执行静态边界和工作区检查**

```bash
rg -n "com\.baomidou|com\.mybatisflex|org\.springframework|MpBase" \
  fw-plus-core/fw-plus-project/fw-plus-mybatis/fw-plus-mybatis-datachange/src/main/java

rg -n "fw\.plus\.mp-datachange|DataChangeProperties|ChangeDataRecord" \
  fw-plus-core fw-plus-boot fw-plus-sample docs/mybatis-datachange.md \
  --glob '!**/target/**'

git diff --check
git status --short
```

Expected: 两个 `rg` 均无旧依赖/旧 API 命中；`git diff --check` 无输出；`git status` 中只包含计划内文件和实施前已存在的用户改动。

- [ ] **Step 8: 检查 Task 12 变更范围（不提交）**

```bash
git diff -- \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-mp-spring-boot-starter-sample/src/main/java/com/xiongx9527/fw/plus/boot/mp/sample/handler/MpSampleDataChangeHandler.java \
  fw-plus-sample/fw-plus-boot-samples/fw-plus-mp-spring-boot-starter-sample/src/main/resources/application.yml \
  docs/mybatis-datachange.md \
  docs/superpowers/specs/2026-08-07-mybatis-datachange-design.md
```

---

## Final Review Checklist

- [ ] 公共模块生产代码不引用 Plus/Flex/Spring/项目 MP 基类。
- [ ] 旧 `fw-plus-mp-datachange` 只剩 Plus Adapter，不包含旧 Handler/事件/配置。
- [ ] 原生、Plus、Flex 对同一变更生成结构一致的事件。
- [ ] INSERT/UPDATE/DELETE/逻辑删除/UPSERT/MERGE、单表/多表/多语句/Batch 均有测试。
- [ ] 联合主键、生成主键、更新后脱离原 WHERE、默认值和触发器值有证据。
- [ ] 事务失败回滚，非事务失败进入 ErrorHandler 且不会伪装业务失败。
- [ ] 快照超过默认 `1000` 行时在原 DML 前失败。
- [ ] 未配置表、SELECT、DDL、CALL 正常放行且不生成事件。
- [ ] 未知方言/无法判断目标的 DML fail-closed。
- [ ] MySQL/MariaDB、PostgreSQL、Oracle、SQL Server、H2 方言契约通过。
- [ ] 多 `SqlSessionFactory` 和动态数据源按实际 Connection 选择方言。
- [ ] `git diff --check` 通过，未混入现有用户改动。
