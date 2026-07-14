---
comet_change: stabilize-nebula-jdbc
role: technical-design
canonical_spec: openspec
---

# stabilize-nebula-jdbc 技术设计

## 1. 背景与目标

`fw-plus-nebula-jdbc` 当前将 NebulaGraph Java client 包装为 JDBC 风格接口，已经具备 `Driver`、`Connection`、`Statement`、`PreparedStatement`、`ResultSet` 与 metadata 基础实现。本 change 的目标是在后续 SQL 转 nGQL 与 MySQL wire protocol 接入前，先稳定现有 JDBC/nGQL 执行基础。

本设计采用“全面 façade + 生命周期管理器”方案，在保持公开 JDBC API 与现有包入口兼容的前提下，把连接配置、Nebula client 访问、资源生命周期、语句分类和结果集游标边界拆分为可测试的内部组件。

## 2. 范围与非目标

### 范围

- 仅修改 `fw-plus-core/fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc` 及其测试代码。
- 保持 `com.vesoft.nebula.jdbc` 对外入口、`java.sql.*` 接口实现方法签名和 `jdbc:nebula://...` URL 格式兼容。
- 统一 JDBC URL 与输入 `Properties` 合并后的配置来源。
- 提供连接池整数参数的集中、安全解析。
- 改善 `Connection`、`Statement`、`ResultSet` 的关闭和边界行为。
- 改善 `executeQuery` / `executeUpdate` 对常见 nGQL 格式的分类稳定性。
- 增加不依赖真实 NebulaGraph 服务的本地单元测试。

### 非目标

- 不实现 SQL 转 nGQL。
- 不实现 MySQL wire protocol 或独立网关服务。
- 不修改 Boot、Cloud、AI、Product 层。
- 不要求真实 NebulaGraph 集成测试。
- 不升级 Nebula Java client 大版本。
- 不追求完整 JDBC 规范覆盖，只修复当前模块基础稳定性问题。

## 3. 设计原则

1. **公开兼容，内部重构**：对外保持现有 JDBC 使用方式；内部通过清晰组件边界降低耦合。
2. **配置单一事实源**：连接初始化只使用解析合并后的配置对象，不再混用原始 `Properties`。
3. **边界显式失败**：非法配置、关闭后访问、游标越界等场景统一转为明确 `SQLException`。
4. **轻量分类，不做解析器**：语句分类只服务现有 nGQL query/update 入口，不承担 SQL 或完整 nGQL 语法解析。
5. **可隔离测试优先**：把易出错逻辑移入纯逻辑或可 mock 的内部组件，避免测试依赖真实 NebulaGraph。

## 4. 内部组件设计

### 4.1 `NebulaConnectionConfig`

职责：承载 JDBC URL 与输入 `Properties` 合并后的类型化连接配置。

建议字段包括：

- graphspace
- username
- password
- graph addresses
- minConnsSize
- maxConnsSize
- timeout
- idleTime
- intervalIdle
- waitTime
- 原始合并属性视需要保留为只读副本，用于兼容已有扩展点

设计要点：

- 该对象是连接初始化后的唯一配置输入。
- 数值字段在构造前完成解析，使用方不再直接从 `Properties` 做类型转换。
- 对必需字段缺失或非法值，解析阶段抛出带字段名的 `SQLException`。

### 4.2 `NebulaConnectionConfigParser`

职责：包装和复用现有 `NebulaJdbcUrlParser.parse(url, properties)`，生成 `NebulaConnectionConfig`。

处理流程：

1. 调用现有 URL parser 得到合并后的 `Properties`。
2. 从合并属性中读取 graphspace、认证信息、地址和连接池参数。
3. 对连接池整数参数执行集中解析：
   - 值为空时使用默认值。
   - 支持 `Number`。
   - 支持可 `trim()` 的字符串。
   - 字符串为空或非数字时抛出明确 `SQLException`。
4. 返回不可变或约定不可变的 `NebulaConnectionConfig`。

该组件不改变 URL 格式，也不重新定义 `NebulaJdbcUrlParser` 的合并优先级；它只把 parser 结果转成更安全的内部配置模型。

### 4.3 `NebulaClientFacade`

职责：隔离 Nebula Java client 的 `NebulaPool`、`Session`、原生 `ResultSet` 调用。

建议能力：

- `open(NebulaConnectionConfig config)`：初始化 pool、获取 session、执行 `use <graphspace>`。
- `executeNgql(String ngql)`：执行 nGQL，并将 Nebula client 异常转换为 `SQLException`。
- `close()`：释放 session 并关闭 pool，具备幂等保护。
- `isClosed()`：暴露底层 client 访问边界。

设计要点：

- `NebulaConnection` 不再直接编排所有 `NebulaPoolConfig`、`NebulaPool`、`Session` 细节。
- 对 `IOErrorException`、`AuthFailedException`、`NotValidConnectionException`、`ClientServerIncompatibleException` 等异常统一转换为 `SQLException`。
- graphspace 切换失败时保留 Nebula error code 和 error message。
- 测试中可通过 mock façade 或对 façade 内部依赖进行隔离，避免真实 NebulaGraph。

### 4.4 `NebulaResourceLifecycle`

职责：为 connection、statement、resultSet 提供一致的关闭状态和访问检查语义。

建议能力：

- `close(Runnable closeAction)` 或等价封装：首次关闭执行底层释放，重复关闭直接返回。
- `checkOpen(String resourceName)`：关闭后访问抛出明确 `SQLException`。
- `isClosed()`：返回当前资源关闭状态。

设计要点：

- `Connection.close()`、`Statement.close()`、`ResultSet.close()` 的重复调用保持稳定，不抛非必要异常。
- 首次关闭时底层释放失败仍应以 `SQLException` 暴露。
- 不吞掉首次关闭的真实资源释放错误。
- 关闭状态设置要避免释放失败后状态不一致；可根据实际语义选择“释放失败仍视为关闭”或“释放失败保持可见失败”，但必须在实现和测试中保持一致。

### 4.5 `NebulaStatementClassifier`

职责：独立负责 nGQL query/update 分类。

分类策略：

1. 输入为 `null` 或规范化后为空时，抛出明确 `SQLException`。
2. 对输入执行 `trim()`、小写化、连续空白规范化。
3. 基于 token 边界匹配现有 query/update 关键字集合。
4. `executeQuery`：若包含 update 类关键字则拒绝；若包含 query 类关键字则接受；否则报 expected query。
5. `executeUpdate`：若包含 update 类关键字则接受；否则报 expected update。

现有关键字集合保持不扩展或只做小范围整理：

- update：`update`、`delete`、`insert`、`upsert`、`create`、`drop`、`alter`、`rebuild`
- query：`match`、`lookup`、`go`、`fetch`、`find`、`subgraph`、`show`、`desc`、`describe`

该组件不识别 SQL，不做 AST，不保证完整 nGQL 语法正确性；它只改进当前轻量分类逻辑对大小写、换行、多空白的容错。

### 4.6 `NebulaResultSetCursor`

职责：集中管理 `ResultSet` 当前行号、可读状态和 `wasNull()` 需要的 last-column 状态。

建议能力：

- `next()`：推进游标并返回是否存在可读行。
- `checkReadable()`：在 before first 或 after last 状态读取列时抛出明确 `SQLException`。
- `recordColumnAccess(ValueWrapper value)`：记录最近一次读取列值。
- `wasNull()`：在已有列访问记录时返回最近列是否为 null；在未读取任何列前返回稳定结果或抛出明确 `SQLException`。

设计选择：

- 优先选择“未读取列前调用 `wasNull()` 抛明确 `SQLException`”，因为该行为能暴露调用顺序错误，并满足 OpenSpec 中“不出现 NPE”的要求。
- 如果实现时发现现有调用方依赖默认 `false`，可改为返回 `false`，但必须在测试中固定该语义，并确保不会再出现 `NullPointerException`。

## 5. 数据流

### 5.1 连接创建

```text
NebulaDriver.connect(url, properties)
  -> NebulaConnection(url, properties)
      -> NebulaConnectionConfigParser.parse(url, properties)
          -> NebulaJdbcUrlParser.parse(url, properties)
          -> NebulaConnectionConfig
      -> NebulaClientFacade.open(config)
          -> NebulaPool.init(addresses, poolConfig)
          -> NebulaPool.getSession(user, password, true)
          -> Session.execute("use " + graphspace)
      -> NebulaConnection ready
```

连接创建后的 `graphspace`、认证信息和连接池参数全部来自 `NebulaConnectionConfig`。

### 5.2 语句执行

```text
NebulaStatementImpl.executeQuery(ngql)
  -> NebulaStatementClassifier.checkQuery(ngql)
  -> NebulaClientFacade.executeNgql(ngql)
  -> NebulaResultSet

NebulaStatementImpl.executeUpdate(ngql)
  -> NebulaStatementClassifier.checkUpdate(ngql)
  -> NebulaClientFacade.executeNgql(ngql)
  -> update count / success handling
```

`PreparedStatement` 继续保持现有 nGQL 参数绑定能力边界；本 change 不把 SQL 文本转换为 nGQL。

### 5.3 ResultSet 访问

```text
ResultSet.next()
  -> NebulaResultSetCursor.next()

ResultSet.getObject(column)
  -> checkClosed()
  -> cursor.checkReadable()
  -> checkIndex(column)
  -> nativeResultSet.rowValues(currentRow)
  -> cursor.recordColumnAccess(value)
  -> return value

ResultSet.wasNull()
  -> checkClosed()
  -> cursor.wasNull()
```

## 6. 错误处理

- 配置解析错误：抛出包含参数名和非法值的 `SQLException`。
- Nebula client 初始化错误：保留原始异常作为 cause，并补充当前操作上下文。
- graphspace 切换失败：包含 graphspace、Nebula error code、error message。
- 语句类型错误：明确说明当前方法期望 query 或 update，并列出支持的关键字范围。
- 关闭后访问：抛出包含资源名的 `SQLException`。
- 游标越界：抛出包含当前游标位置和有效范围说明的 `SQLException`。
- `wasNull()` 调用顺序错误：抛出明确 `SQLException`，避免 `NullPointerException`。

## 7. 兼容性设计

- 不改变 `NebulaDriver` 的注册方式和 JDBC URL 前缀。
- 不改变 `NebulaConnection`、`NebulaStatementImpl`、`NebulaPreparedStatementImpl`、`NebulaResultSet` 对外实现的 JDBC 接口。
- 新增组件放在内部职责包下，例如：
  - `com.vesoft.nebula.jdbc.config`
  - `com.vesoft.nebula.jdbc.client`
  - `com.vesoft.nebula.jdbc.lifecycle`
  - `com.vesoft.nebula.jdbc.statement`
  - `com.vesoft.nebula.jdbc.result`
- 若因包可见性需要调整构造器或方法可见性，应优先使用 package-private，避免扩大公开 API。
- 错误消息可以变得更明确，但不应依赖精确英文文本作为外部兼容承诺。

## 8. 测试策略

测试不依赖真实 NebulaGraph，不引入 Testcontainers。允许新增 JUnit 与 Mockito，优先复用父 POM 或 BOM 中已有版本管理。

### 8.1 配置解析测试

覆盖：

- URL query 参数能进入合并配置。
- 输入 `Properties` 与 URL 参数合并后，连接初始化使用同一配置来源。
- graphspace、user、password、graph addresses 从 `NebulaConnectionConfig` 读取。
- 缺失或非法字段产生明确 `SQLException`。

### 8.2 数字参数解析测试

覆盖：

- `minConnsSize`、`maxConnsSize`、`timeout`、`idleTime`、`intervalIdle`、`waitTime` 的默认值。
- 字符串数字值。
- `Number` 类型值。
- 带前后空白的字符串。
- 非数字字符串和空字符串。
- 异常消息包含参数名。

### 8.3 Nebula client façade 测试

通过 mock 或可注入依赖覆盖：

- pool 初始化参数来自 `NebulaConnectionConfig`。
- session 获取使用配置中的 user/password。
- 成功执行 `use <graphspace>` 后连接可用。
- `use <graphspace>` 返回失败时抛出 `SQLException`。
- `executeNgql` 将 Nebula client 异常转换为 `SQLException`。
- `close()` 重复调用稳定。

### 8.4 Statement 分类测试

覆盖：

- query 语句大小写混合。
- query 语句包含换行、多空白、制表符。
- update 语句大小写混合。
- update 语句包含换行、多空白、制表符。
- `executeQuery` 遇到 update 关键字时拒绝。
- `executeUpdate` 遇到非 update 语句时拒绝。
- 空字符串、纯空白、未知语句类型抛明确 `SQLException`。

### 8.5 生命周期与 ResultSet 边界测试

覆盖：

- `Connection.close()` 重复调用不抛非必要异常。
- `Statement.close()` 重复调用稳定。
- `ResultSet.close()` 重复调用稳定。
- 关闭后访问抛明确 `SQLException`。
- before first 或 after last 状态读取列抛明确 `SQLException`。
- 读取列后 `wasNull()` 返回最近列的 null 状态。
- 未读取列前 `wasNull()` 不出现 `NullPointerException`，并固定为明确异常或稳定返回值。

### 8.6 推荐验证命令

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc
```

如果测试依赖上游模块未安装或当前模块需要联动编译，可使用：

```bash
cd fw-plus-core && ./mvnw test -pl fw-plus-project/fw-plus-nebula/fw-plus-nebula-jdbc -am
```

## 9. 实施顺序建议

1. 补充或确认测试依赖，先建立可运行的测试基线。
2. 新增 `NebulaConnectionConfig` 与 `NebulaConnectionConfigParser`，覆盖配置和数字参数解析测试。
3. 新增 `NebulaClientFacade`，将 `NebulaConnection` 中的 pool/session 编排迁移进去。
4. 新增 `NebulaResourceLifecycle`，统一 connection、statement、resultSet close/check 语义。
5. 新增 `NebulaStatementClassifier`，替换 `NebulaStatementImpl` 中的字符串拆分分类逻辑。
6. 新增 `NebulaResultSetCursor`，调整 `NebulaResultSet` 游标和 `wasNull()` 边界。
7. 运行模块测试，修复失败。
8. 检查变更范围，确认未引入 SQL 转 nGQL、MySQL wire protocol 或跨层改动。

## 10. 风险与缓解

### 风险：内部重构改动面较大

缓解：只在 `fw-plus-nebula-jdbc` 内部新增组件，保持公开 API 和 URL 格式不变；按任务分批实现，每批配套测试。

### 风险：close 幂等改变少数调用方观察到的异常行为

缓解：重复 close 幂等符合常见 JDBC 使用预期；首次关闭仍保留真实释放错误，不吞掉底层异常。

### 风险：语句分类仍不完整

缓解：明确该组件只做轻量 nGQL 分类，不承担 SQL/nGQL parser 职责；复杂 SQL 转换由后续 `add-sql-to-ngql-translator` change 处理。

### 风险：不做真实 NebulaGraph 集成测试可能遗漏 client 交互问题

缓解：通过 façade 把 client 编排集中到少量边界并用 mock 覆盖；真实集成测试可在后续独立 change 或环境就绪后补充。

### 风险：新增内部组件增加理解成本

缓解：组件命名按职责划分，每个组件只承载单一边界；测试名称直接描述行为，帮助维护者理解设计意图。

## 11. Spec Patch 结论

无需回写 OpenSpec delta spec。现有 `nebula-jdbc-stability` 规格已经覆盖配置解析、数字参数解析、资源关闭、语句分类、ResultSet 边界和本地测试要求。本设计只细化内部实现策略，不改变需求范围。