# MyBatis DataChange 重建设计

## 背景

现有 `fw-plus-mp-datachange` 以 MyBatis-Plus 的实体、Mapper、Wrapper 和方法名约定为核心，无法作为原生 MyBatis 与 MyBatis-Flex 共用的数据变更能力。新实现不保留旧 API、配置格式或内部结构；旧代码只用于核对已有业务需求，不作为兼容边界。

本次从 MyBatis 执行模型重新设计数据变更监听：原生 MyBatis 提供公共执行、快照、差异计算和事件派发能力，MyBatis-Plus 与 MyBatis-Flex 通过独立适配器提供各自框架才能可靠识别的实体元数据和操作语义。

## 目标

- 新建可独立使用的 `fw-plus-mybatis-datachange` 模块。
- 支持原生 MyBatis、MyBatis-Plus 和 MyBatis-Flex。
- 支持单表、多表、单语句、多语句和 JDBC Batch 的数据变更审计。
- 支持 `INSERT`、`UPDATE`、物理删除、逻辑删除以及 UPSERT / MERGE 类 DML。
- 为监听表生成逐行、逐字段的更新前后值。
- 支持联合主键、配置唯一键、自动生成主键和多 schema 同名表。
- 事务可选：有事务时提供强一致语义，无事务时提供明确标记的尽力而为语义。
- 数据库能力通过方言 SPI 扩展，核心架构不绑定单一数据库。
- 只监听显式配置的表，并限制单次快照规模。

## 非目标

- 不兼容旧 `DataChangeHandler`、`ChangeDataRecord`、配置前缀或方法名白名单。
- 不复用 `MpBaseEntity`、`MpBaseMapper` 或旧 Wrapper 转换实现。
- 不分析 `CALL procedure_name(...)` 或存储过程内部的数据变化；`CALL` 明确跳过。
- 不监听 `SELECT`、DDL 或数据库外部发生的数据变化。
- 不通过 DataChange 核心直接向 MQ、HTTP 等不可事务回滚的外部系统发送事件。
- 不承诺一个固定版本内穷举所有数据库厂商语法；未内置的数据库通过方言 SPI 接入。

## 模块结构

```text
fw-plus-core/fw-plus-project/fw-plus-mybatis/
├── fw-plus-mybatis-datachange
├── fw-plus-mp/
│   └── fw-plus-mp-datachange
└── fw-plus-mf/
    └── fw-plus-mf-datachange
```

依赖方向固定为：

```text
fw-plus-mp-datachange ──┐
                       ├──▶ fw-plus-mybatis-datachange
fw-plus-mf-datachange ──┘
```

`fw-plus-mybatis-datachange` 只依赖 MyBatis、SQL 解析能力和必要的通用库，不依赖 MyBatis-Plus、MyBatis-Flex 或 Spring 容器。Spring Boot 属性绑定和拦截器注册继续放在 `fw-plus-boot-autoconfigure`。

建议包结构：

```text
com.xiongx9527.fw.plus.mybatis.datachange
├── api
├── adapter
├── dialect
├── interceptor
├── snapshot
├── diff
├── dispatch
└── config
```

Plus 与 Flex 适配模块分别使用现有 `com.xiongx9527.fw.plus.mp.datachange` 和 `com.xiongx9527.fw.plus.mf.datachange` 包。

## 总体架构

```text
Mapper DML
    │
    ▼
DataChangeInterceptor
    │  捕获 MappedStatement、参数、最终 BoundSql、连接和执行结果
    ▼
FrameworkAdapterRegistry
    │  Plus → Flex → 原生 MyBatis
    ▼
DialectRegistry
    │  分析语句、目标表、SET、WHERE、UPSERT/MERGE 语义
    ▼
SnapshotService
    │  更新前快照 → 原始 DML → 更新后快照
    ▼
ChangeDiffEngine
    │  Event → Statement → Table → Row → Field
    ▼
DataChangeDispatcher
    │  调用所有匹配的 Handler
    ▼
事务提交或 Best-Effort 错误处理
```

框架适配器只提供 ORM 语义；数据库方言只提供 SQL 和数据库语义；快照、Diff、事件和 Handler 调度保持唯一实现。

## 领域模型

### 事件层级

```text
DataChangeEvent       一次 Mapper 调用或一次 Batch flush
└── StatementChange  一条 DML
    └── TableChange  一个目标表
        └── RowChange    一行数据
            └── FieldChange
```

JDBC 返回的影响行数属于整条语句，因此保存在 `StatementChange`。一条多表 DML 在同一个 `StatementChange` 中生成多个 `TableChange`，不虚构每个目标表的独立影响行数。

```java
public class DataChangeEvent {
	private String eventId;
	private String statementId;
	private Date occurredAt;
	private String databaseProduct;
	private ConsistencyLevel consistencyLevel;
	private List<StatementChange> statements;
}

public class StatementChange {
	private int statementIndex;
	private String sqlTemplate;
	private int affectedRows;
	private List<TableChange> tables;
}

public class TableChange {
	private TableIdentifier table;
	private List<RowChange> rows;
}

public class RowChange {
	private DataChangeOperation operation;
	private RowIdentity identity;
	private Map<String, Object> before;
	private Map<String, Object> after;
	private List<FieldChange> fields;
}

public class FieldChange {
	private String columnName;
	private String propertyName;
	private Object beforeValue;
	private Object afterValue;
}
```

`sqlTemplate` 保留占位符，不将绑定参数拼接进 SQL。`propertyName` 在原生 MyBatis 场景可以为空，由 Plus/Flex 适配器补充。操作类型放在 `RowChange`，因为同一条 UPSERT/MERGE 可能同时产生 `INSERT` 行和 `UPDATE` 行。

### 表与行身份

```java
public class TableIdentifier {
	private String catalog;
	private String schema;
	private String table;
}

public class RowIdentity {
	private Map<String, Object> keyValues;
}
```

表名大小写、catalog/schema 默认值和引用符由数据库方言标准化。`RowIdentity` 使用有序映射保存一个或多个唯一键列，不假设存在名为 `id` 的单主键。

唯一键解析优先级：

1. 表配置显式指定的唯一键。
2. Plus/Flex 框架元数据。
3. JDBC `DatabaseMetaData` 主键元数据。

监听表无法获得稳定唯一键时，阻止其 `UPDATE`、`DELETE`、UPSERT/MERGE 审计，避免生成无法可靠回查的事件。

## 配置模型

配置前缀为 `fw.plus.mybatis-datachange`。表使用列表而不是以表名为 key 的 Map，以支持不同 catalog/schema 下的同名表。

```yaml
fw:
  plus:
    mybatis-datachange:
      enabled: true
      max-snapshot-rows: 1000
      tables:
        - catalog:
          schema: public
          name: sys_user
          key-columns:
            - id
          include-columns: []
          exclude-columns:
            - update_time
            - update_user
          logical-delete:
            column: deleted
            deleted-values:
              - 1
          max-snapshot-rows: 500
```

规则：

- 只有显式配置的表进入快照和事件流程。
- `catalog`、`schema` 可选，`name` 必填。
- `key-columns` 为空时按唯一键解析优先级自动解析。
- `include-columns` 为空表示监听全部列；非空时只生成指定列的 `FieldChange`。
- `exclude-columns` 在 include 结果上继续排除列。
- 主键始终用于行定位，但主键值未变化时不生成 `FieldChange`。
- `logical-delete` 是原生 MyBatis 的可选语义配置；Plus/Flex 优先使用各自元数据，显式表配置可以覆盖。
- 全局 `max-snapshot-rows` 默认 `1000`，表级配置可以覆盖。
- 快照查询读取上限加一行，用于可靠判断是否超限；超限时在执行原始 DML 前失败。
- 配置和事件中的核心字段统一使用数据库列名，不使用 Java 属性名作为持久化事实。

核心配置对象保持普通 Java Bean。Boot 自动配置通过 Bean 方法绑定属性，不把 Spring 注解带入公共模块。

## 框架适配 SPI

```java
public interface DataChangeFrameworkAdapter {

	boolean supports(DataChangeInvocation invocation);

	FrameworkChangeMetadata resolve(DataChangeInvocation invocation);

	int getOrder();
}
```

```java
public class FrameworkChangeMetadata {
	private Class<?> entityType;
	private TableIdentifier table;
	private List<String> keyColumns;
	private Map<String, String> propertyColumnMappings;
	private Set<String> ignoredColumns;
	private LogicalDeleteMetadata logicalDelete;
	private GeneratedKeyAccessor generatedKeyAccessor;
}
```

适配器按确定性顺序匹配：Plus、Flex、原生 MyBatis。不能仅按 Classpath 选择；必须结合 `MappedStatement`、Mapper 类型和参数类型。若相同优先级的多个适配器同时声明支持同一调用，立即抛出配置异常。

### 原生 MyBatis 适配器

- 使用表配置、`MappedStatement`、`BoundSql`、参数映射和 JDBC 元数据。
- 不假设 Mapper 方法名、实体基类或固定参数名。
- SQL 已能完整表达的操作不要求实体类型。
- 无法从 SQL、配置和元数据确认的信息不进行猜测。

### MyBatis-Plus 适配器

- 使用 Plus 表、主键、属性列映射和忽略字段元数据。
- 识别 Plus Wrapper、逻辑删除、乐观锁、自动填充和批量 CRUD 语义。
- 不要求实体继承 `MpBaseEntity`，不要求 Mapper 继承项目自定义 Mapper。
- 最终 SQL 仍是快照范围的事实来源，框架元数据不能绕过租户或数据权限条件。

### MyBatis-Flex 适配器

- 使用 Flex 表、主键、属性列映射和忽略字段元数据。
- 识别 Flex Wrapper、逻辑删除、乐观锁和批量 CRUD 语义。
- 与 Plus 适配器输出同一个 `FrameworkChangeMetadata`，不复制快照或 Diff 实现。

## 数据库方言 SPI

```java
public interface DataChangeDialect {

	boolean supports(DatabaseMetadata metadata);

	List<AnalyzedStatement> analyze(BoundSqlContext context);

	SnapshotQuery buildBeforeQuery(ChangeTarget target);

	SnapshotQuery buildAfterQuery(
			ChangeTarget target,
			List<RowIdentity> identities);

	int getOrder();
}
```

方言负责：

- 数据库产品识别和标识符标准化。
- 多语句拆分和 DML AST 分析。
- 单表、多表、UPSERT/MERGE 的目标表、赋值和条件提取。
- 方言特有的行数限制、行锁和联合主键查询语法。
- 生成更新前和更新后快照 SQL。
- 保留原 SQL 参数映射并生成额外快照参数。

`SnapshotQuery` 必须保存 SQL 模板、参数值和类型信息，并使用 MyBatis `TypeHandlerRegistry` 绑定参数；禁止字符串拼接参数。

内置方言目标：

- MySQL / MariaDB
- PostgreSQL
- Oracle
- SQL Server
- H2

达梦、人大金仓、瀚高以及其他数据库通过同一 SPI 增加方言。系统使用 `DatabaseMetaData.getDatabaseProductName()` 等元数据自动选择方言。未知数据库或无法分析的监听 DML 采用 fail-closed 策略，不静默漏记。

## DML 处理语义

| 操作 | 更新前快照 | 更新后快照 | 事件语义 |
| --- | --- | --- | --- |
| INSERT | 普通插入为空；UPSERT 按唯一键查询 | 按生成主键或唯一键查询 | INSERT 或实际 UPSERT 结果 |
| UPDATE | 按最终 WHERE 和 JOIN 查询 | 按更新前捕获的行身份查询 | UPDATE |
| DELETE | 按最终 WHERE 和 JOIN 查询 | 不查询 | PHYSICAL_DELETE |
| 逻辑删除 | 按 UPDATE 查询 | 按捕获的行身份查询 | LOGICAL_DELETE |
| UPSERT/MERGE | 按冲突键/匹配条件查询 | 按最终行身份查询 | 分行为 INSERT 或 UPDATE |
| 多表 DML | 分目标表查询 | 分目标表回查 | 一个 StatementChange 包含多个 TableChange |

更新后查询不得复用原始 WHERE，因为更新后的数据可能已经不再满足该条件。更新前必须捕获每行身份，更新后只按这些身份回查。

插入行身份按以下顺序解析：

1. SQL 参数已有唯一键。
2. Plus/Flex 已生成主键。
3. JDBC generated keys 或数据库 `RETURNING` 能力。
4. 方言或表配置提供的主键回查策略。

有事务时，无法取得监听表插入行身份会抛出异常并回滚。无事务时，业务 SQL 可能已经提交，错误交给 `DataChangeErrorHandler`，不伪造最终快照或成功事件。

## 拦截器与执行上下文

拦截器需要协调 `Executor` 与 `StatementHandler` 层：

- `Executor` 层维护一次 Mapper 调用、事务状态、返回结果和 Batch flush 边界。
- `StatementHandler` 层捕获经过租户、数据权限、逻辑删除、乐观锁及 ORM 插件改写后的最终 `BoundSql`。
- 快照使用执行该 DML 的同一 JDBC Connection。
- Boot 注册顺序必须保证 DataChange 分析的是最终 SQL，而不是 ORM 改写前 SQL。

执行上下文使用线程栈，不使用单值 `ThreadLocal`，以支持 Mapper 嵌套调用：

```text
DataChangeExecutionContext
├── statementId
├── connection
├── transactionState
├── finalBoundSql
├── frameworkAdapter
├── dialect
├── statementPlans
└── batchPlans
```

上下文必须在 `finally` 中清理。异常、回滚和 Batch 丢弃时不得遗留待派发事件。

### 普通执行

```text
建立执行上下文
→ 捕获最终 BoundSql
→ 分析并过滤监听表
→ 查询更新前快照
→ 执行原始 DML
→ 按实际影响行和行身份查询更新后快照
→ Diff 并派发事件
→ 清理上下文
```

原始 DML 抛出异常或影响零行时，不生成成功变更事件。

业务 DML 虽然报告影响行数，但监听字段没有任何实际差异时，不生成空的 `RowChange`；整个事件没有行变化时不调用 Handler。

### JDBC Batch

Batch 按一次实际 flush 聚合：

```text
收集本批次目标和初始快照
→ flush 整个 JDBC Batch
→ 查询批次最终快照
→ 生成一个 DataChangeEvent
```

同一批次内同一行经历 `10 → 20 → 30` 时，只记录 `10 → 30`。实现必须覆盖显式 `flushStatements()`、提交触发 flush 和查询前隐式 flush，不能仅处理显式 flush。

## 事务与一致性

事务不是使用 DataChange 的硬性要求。运行时自动识别一致性等级：

```java
public enum ConsistencyLevel {
	TRANSACTIONAL,
	BEST_EFFORT
}
```

### 有事务

- 快照、业务 DML 和 Handler 使用同一事务。
- 方言为更新前快照使用合适的行锁语法，避免并发穿插修改。
- 任一 Handler 失败时异常向上抛出，业务 DML 和审计写入一起回滚。
- 事务提交前完成同步 Handler；外部发送应通过同事务 Outbox 实现。

### 无事务

- 不要求调用方额外创建事务，也不自动改变连接事务模式。
- 更新前后快照属于尽力而为，事件标记为 `BEST_EFFORT`。
- 并发更新可能使前后值存在观察窗口，文档必须明确这一限制。
- Handler 失败不能撤销已自动提交的业务 SQL，不向业务层抛出造成“数据已提交但调用失败”的假象。
- Handler 错误交给 `DataChangeErrorHandler`；其他匹配 Handler 继续执行。

## Handler 与错误处理

```java
public interface DataChangeHandler {

	boolean supports(DataChangeEvent event);

	void handle(DataChangeEvent event) throws Exception;

	int getOrder();
}

public interface DataChangeErrorHandler {

	void handle(DataChangeFailure failure);
}
```

所有 `supports` 返回 `true` 的 Handler 按 `getOrder()` 依次执行。同一事件可同时写审计表、写 Outbox 和执行其他事务内处理。核心不直接启动异步线程，也不直接发送外部消息。

`DataChangeFailure` 至少包含事件/语句标识、目标表、一致性等级、错误代码和原始异常。默认 ErrorHandler 记录结构化错误日志，但不得输出绑定后的敏感参数。

错误代码：

```text
UNSUPPORTED_DIALECT
UNSUPPORTED_SQL
SNAPSHOT_LIMIT_EXCEEDED
ROW_IDENTITY_UNAVAILABLE
GENERATED_KEY_UNAVAILABLE
SNAPSHOT_QUERY_FAILED
HANDLER_FAILED
AMBIGUOUS_FRAMEWORK_ADAPTER
AMBIGUOUS_DIALECT
```

## 监听判定与失败策略

处理顺序为先分析目标表，再匹配表白名单：

- 非 DML 直接放行。
- `CALL` 直接放行且不生成事件。
- 成功识别且不包含监听表的 DML 直接放行。
- 包含监听表的 DML 必须完成方言分析、快照和身份解析，否则在原始 DML 前失败。
- 完全无法确定目标表的 DML 默认阻止，因为无法证明它没有修改监听表。
- 失败策略不允许静默生成缺少前后值的“半成功”事件。

该 fail-closed 行为只针对 DML；DDL 和查询不进入 DataChange。

## 递归保护

内部快照查询和 Handler 写入不得再次进入 DataChange：

```java
try (DataChangeScope ignored = DataChangeScope.suppress()) {
	// 快照查询或 Handler 管理的审计写入
}
```

抑制作用域必须可嵌套并在 `close()` 时恢复上一层状态。它只包围框架内部操作，不全局屏蔽业务代码在 Handler 前后的 Mapper 嵌套调用。

## Spring Boot 自动配置

在 `fw-plus-boot-autoconfigure` 中新增 `MybatisDataChangeAutoConfiguration`：

- `fw.plus.mybatis-datachange.enabled=true` 时启用。
- 收集配置、全部 Adapter、Dialect、Handler 和 ErrorHandler。
- 至少存在一个监听表和一个 Handler 时才注册拦截器。
- 遍历并注册到全部 `SqlSessionFactory`，不假设只有默认数据源。
- Plus/Flex 模块分别通过条件自动配置贡献 Adapter。
- 启动时校验重复表配置、非法列配置、非法快照上限和相同优先级冲突。
- JDBC 主键元数据在相应数据库连接首次可用时延迟读取，并按数据源与表标识缓存。
- 保持 Spring Boot 2.7 的 `spring.factories` 注册方式和 Java 8 兼容性。

动态数据源场景以 DML 实际使用的 Connection 为准选择数据库方言和元数据缓存，不能在启动时把一个默认方言固定给全部数据源。

## 安全与性能

- 默认单次快照上限为 `1000` 行，允许按表降低或提高。
- 快照只查询唯一键、监听列和事件所需列，不执行 `SELECT *`。
- 元数据、方言选择、标准化表配置和属性列映射需要缓存。
- SQL 和日志保留占位符，不渲染绑定参数。
- 事件值来自显式监听字段；敏感字段是否落库由表配置和业务 Handler 决定。
- 更新前快照不得绕过最终 SQL 中的租户、数据权限和逻辑删除条件。
- 更新后只按已捕获行身份查询，不重新扩大业务条件范围。
- Handler 写外部系统时应使用 Outbox，避免在数据库事务中产生不可回滚副作用。

## 测试策略

### 公共模块测试

- 事件、行身份、联合主键和字段 Diff。
- 配置 include/exclude、schema 同名表和大小写标准化。
- Adapter/Dialect 确定性选择和冲突失败。
- 普通执行、嵌套执行、异常清理和递归抑制。
- 事务与无事务两种 Handler 失败语义。
- 快照 `limit + 1` 超限检测。
- INSERT、UPDATE、DELETE、逻辑删除、UPSERT/MERGE。
- 单表、多表、多语句和 Batch 聚合。
- 更新后不再满足原 WHERE 的场景。
- 自动生成主键、数据库默认值和触发器修改值。

### ORM 适配测试

- Plus/Flex 实体表名、主键、属性列和忽略字段映射。
- 原生 MyBatis 显式逻辑删除配置和 Plus/Flex 逻辑删除元数据覆盖。
- Wrapper 条件与更新字段识别。
- 逻辑删除、乐观锁、自动填充和批量 CRUD。
- 同时存在 Plus/Flex 依赖时的适配器选择。
- 不继承项目自定义基础实体和 Mapper 的普通实体。

### 数据库方言契约测试

每个内置方言执行同一套行为契约：

- 标识符、catalog/schema 和联合主键。
- 参数绑定和数据库类型处理。
- 方言特有 UPSERT/MERGE、多表 DML、限制和行锁语法。
- 事务回滚、无事务提交和并发快照。
- generated keys / RETURNING。
- 实际影响行数与事件行数一致。

H2 只验证公共流程，不能替代 MySQL/MariaDB、PostgreSQL、Oracle 和 SQL Server 的真实数据库集成测试。

### Boot 集成测试

- 启用/禁用条件和无 Handler 时不注册。
- 多个 `SqlSessionFactory` 和动态数据源。
- 与 Plus/Flex、租户、数据权限、逻辑删除和乐观锁插件的顺序。
- Handler 顺序、事务回滚、Best-Effort ErrorHandler。
- 不拦截未配置表、查询、DDL 和 `CALL`。

## 完成标准

- `fw-plus-mybatis-datachange` 不包含 Plus/Flex/Spring 类型引用。
- 原生 MyBatis、Plus 和 Flex 对相同数据库变化生成结构一致的 Event → Statement → Table → Row → Field 事件。
- 监听表的受支持 DML 能生成准确的行身份、前后快照和字段差异。
- 事务场景下 Handler 失败能够回滚；无事务场景明确标记为 `BEST_EFFORT` 并由 ErrorHandler 接管。
- Batch 保持批处理执行语义并生成批次前后聚合事件。
- 未知方言和无法分析的监听 DML 不会静默漏记。
- 所有内置方言通过公共契约与真实数据库集成测试。
- 旧 `fw-plus-mp-datachange` 实现不再决定新 API 或模块边界。
