---
comet_change: stabilize-fw-plus-nebula-jdbc
role: technical-design
canonical_spec: openspec
---

# stabilize-fw-plus-nebula-jdbc 技术设计

## 背景与目标

`fw-plus-nebula-jdbc` 提供 `com.vesoft.nebula.jdbc` 包下的 JDBC Driver、Connection、Statement、PreparedStatement、ResultSet 和 MetaData 表面。当前 change 的目标不是扩展 Nebula Graph 查询能力，而是稳定已有 JDBC 契约：让 URL/属性解析、连接生命周期、执行状态边界、ResultSet/MetaData 边界和 unsupported 方法异常语义变得可预测、可测试。

本设计以 OpenSpec delta spec `nebula-jdbc-stability` 为事实源，保持 Java 1.8、现有包名、Maven 坐标和公共 JDBC 类型不变，不升级 `com.vesoft:client`，也不要求真实 Nebula 服务参与本次单元测试。

## 推荐方案

采用“契约测试优先 + 小范围测试接缝”的方案。

实施时先补充无外部服务依赖的契约测试，固定解析、生命周期、异常和边界行为；再按测试驱动收紧实现。对必须触达 Nebula client 的路径，通过小范围内部接缝隔离 `NebulaPool` / `Session` 创建与结果返回，使初始化失败清理、关闭幂等和关闭后访问能在单元测试中验证。

允许的接缝仅限内部实现形态，例如：

- 受保护工厂方法，用于创建 `NebulaPool`、获取 `Session` 或构造 `NebulaPoolConfig`。
- 包内可见构造器或轻量 adapter，用于测试替身注入。
- 小型 helper 方法，用于统一属性读取、数字转换和关闭状态校验。

这些接缝不得改变 public JDBC API、包名、Maven 坐标或调用方可见类型。

## 备选方案与取舍

### 方案 A：契约测试优先 + 小范围测试接缝（采用）

优点是能覆盖 OpenSpec 要求的多数失败路径和边界行为，同时保持改动范围集中。缺点是会在少数类中引入为测试和隔离外部 client 服务的内部接缝，需要控制命名和可见性，避免演化成新的抽象层。

### 方案 B：最小修复 + 手工验证

只修复明显的属性混用、关闭状态和异常问题，新增少量测试。优点是速度快；缺点是初始化失败清理、query/update 分类、ResultSet 边界和 unsupported 方法审计仍缺少保护，后续回归风险较高。

### 方案 C：重构驱动内部架构

抽出完整的 ConnectionFactory、SessionAdapter、ResultAdapter 等内部抽象。长期可测试性最好，但会显著扩大修改面，不符合本 change “稳定已有 JDBC 表面”的范围。

## 设计细节

### 1. URL 与连接属性解析

`NebulaJdbcUrlParser.parse(url, properties)` 的返回值作为连接有效配置的单一事实源。`NebulaConnection` 初始化完成解析后，后续 graph space、用户名、密码、host 列表和连接池数值配置均从解析后的配置读取，不再混用原始入参。

解析层或连接创建边界需要统一处理以下输入：

- `null`、非 `jdbc:nebula:` 前缀或不符合 `jdbc:nebula://host:port[,host:port]/space` 结构的 URL。
- 缺失必要 host/port、端口非数字、地址段为空。
- 用户、密码、graph space 等必需属性缺失或空值。
- `minConnsSize`、`maxConnsSize`、`timeout`、`idleTime`、`intervalIdle`、`waitTime` 等数字型 pool 参数非数字或超出可接受范围。

非法输入应在创建或初始化连接池前失败，并向 `Driver.connect` / `NebulaConnection` 调用方表现为清晰的 `SQLException` 或 OpenSpec 已允许的解析异常，不应留下已创建 pool。

### 2. 连接生命周期

`NebulaConnection` 采用“成功创建则负责关闭”的规则。初始化步骤中，pool 创建成功后如果 session 获取或 `use <space>` 失败，必须释放已创建的 session 和 pool，再抛出 `SQLException`。

`close()` 必须幂等：重复调用不会因为重复关闭本身抛异常。关闭后，对已支持的连接操作，例如 `createStatement()`、`prepareStatement(String)`、`execute(String)` 和状态读取中需要连接有效性的操作，应先通过统一 `checkClosed()` 失败为 `SQLException`。

对于原本明确 unsupported 的 JDBC 功能，例如 savepoint、callable statement、复杂事务控制等，应继续抛出 `SQLFeatureNotSupportedException`，不要为了关闭状态校验而把 unsupported 行为误报为其他异常，除非 JDBC 契约或现有实现已经明确支持该方法。

### 3. Statement 与 PreparedStatement

`NebulaStatementImpl` 和 `NebulaPreparedStatementImpl` 需要统一以下状态边界：

- 关闭后执行 `execute`、`executeQuery`、`executeUpdate`、`getResultSet` 等受支持方法时抛 `SQLException`。
- 空 SQL、纯空白 SQL 或 PreparedStatement 未设置必要参数时，不进入 Nebula 执行路径。
- `executeQuery` 不应静默接受 update-only nGQL；`executeUpdate` 不应静默接受 query-only nGQL。
- `execute` 成功后维护当前结果状态；只有成功产生结果的执行才能通过 `getResultSet()` 取到当前结果。

query/update 分类可以延续当前 keyword 策略，但要集中化并覆盖大小写、前导空白和注释/空语句等边界。若 nGQL 类型无法可靠识别，优先选择清晰失败，而不是返回误导性默认值。

### 4. ResultSet 与 MetaData

`NebulaResultSet` 和 `NebulaResultSetMetaData` 应对受支持方法统一校验：

- `ResultSet` 关闭后，受支持读取方法失败为 `SQLException`。
- 游标未定位到有效行时读取列值，应失败为明确 `SQLException`。
- JDBC 列索引保持一基规则，索引 `0`、负数或大于列数时失败为 `SQLException`。
- 空结果如果没有列名元数据，`getColumnCount()` 返回 `0`，不抛 unrelated runtime exception。
- `getColumnName` / `getColumnLabel` / `getSchemaName` 等已支持 metadata 方法共享列索引校验。

暂不支持的 ResultSet / MetaData 方法继续通过 `ExceptionBuilder.buildUnsupportedOperationException()` 返回 `SQLFeatureNotSupportedException`。

### 5. Unsupported 方法审计

按类逐项梳理当前返回占位值、空实现或静默成功的 JDBC 方法。分类原则：

- 若方法代表当前驱动不支持的 JDBC 能力，改为 `SQLFeatureNotSupportedException`。
- 若方法是简单状态查询，且已有稳定含义，例如 closed 状态、schema/graph space、auto-commit 语义等，可以保留支持，但需返回稳定、文档化的值。
- 若方法是 setter 但当前驱动不真正支持对应行为，不能静默成功；应明确 unsupported 或保存为本地状态，二者只能择一并在测试中固定。

这一步应分批进行，避免一次性改变过多历史行为。实现计划中应先覆盖 OpenSpec 明确要求的 unsupported 示例，再扩展到明显的空实现/占位值。

## 测试策略

测试优先覆盖无需真实 Nebula 服务的路径：

1. `NebulaJdbcUrlParser`：合法 URL、畸形 URL、缺失 graph space、host/port 解析、凭据合并、数字型 pool 选项转换。
2. `NebulaDriver` / `NebulaConnection` 创建边界：非法 URL、缺失必需属性、非法数字参数在 pool 创建前失败。
3. 连接生命周期：close 幂等；初始化中 pool 已创建但 session 获取或 `use <space>` 失败时释放资源；关闭后 `createStatement()`、`prepareStatement()`、`execute()` 失败。
4. Statement / PreparedStatement：query/update 方法不匹配、空 SQL、关闭后执行、当前结果状态。
5. ResultSet / MetaData：非法列索引、空结果列数、关闭后读取、游标未定位读取。
6. Unsupported 方法：代表性 unsupported connection/statement/resultset 方法抛 `SQLFeatureNotSupportedException`；有意支持的简单状态方法返回稳定值。

如果父级依赖管理已提供 JUnit 能力，模块只添加必要 test scope 依赖；若未提供，再在 `fw-plus-nebula-jdbc` 或合适父级中补充最小测试依赖。测试替身应保持在 `src/test` 内，生产代码只保留必要内部接缝。

验证命令以 OpenSpec tasks 为准：

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc
cd fw-plus-core && ./mvnw install -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -DskipTests
```

## 风险与缓解

- **调用方依赖宽松行为**：通过测试固定 OpenSpec 关注的错误路径；对简单状态方法保留兼容返回值，只有真正 unsupported 或危险静默行为才收紧。
- **Nebula client 难以 mock**：使用受保护工厂方法、包内构造器或轻量 adapter 做内部隔离，不引入重量级 mock 框架或真实服务依赖。
- **URL 解析行为变化影响旧调用**：解析变更优先集中在非法输入和属性混用修复；对历史默认 graph space 行为如需改变，必须先通过 Spec Patch 明确。
- **范围膨胀**：不做完整驱动架构重构，不新增业务查询能力，不扩展真实集成测试。

## 实施顺序建议

1. 建立测试基础，新增 `NebulaJdbcUrlParser` 契约测试。
2. 统一有效配置读取与数字型 pool 参数转换，修复原始 `properties` 混用。
3. 引入最小连接创建测试接缝，覆盖 pool/session/use-space 失败清理。
4. 收紧 Connection close、关闭后访问和 unsupported 方法的代表性行为。
5. 收紧 Statement / PreparedStatement query/update 与当前结果状态。
6. 收紧 ResultSet / MetaData 关闭、游标和列索引边界。
7. 审计空实现和占位返回值，保留有意支持的简单状态方法。
8. 运行模块 test 与 install 验证。

## Spec Patch

暂无。当前 OpenSpec delta spec 已覆盖设计所需的验收场景；若实现前确认“缺失 graph space”必须从历史默认 `default` 改为非法输入，再回写 delta spec 增加明确验收场景。
