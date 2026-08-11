# Task 10 MyBatis-Flex DataChange 适配器报告

## 状态

- 新增 `fw-plus-mf-datachange` 模块并接入 `fw-plus-mf` 聚合器与 Core BOM。
- 新增 order `100` 的 MyBatis-Flex 适配器、实体类型解析器和生成键读取器。
- 生产模块直接依赖仅为 `fw-plus-mybatis-datachange` 与 `mybatis-flex-core`；Plus 适配器仅以 test scope 用于跨适配选择测试。

## Flex 1.11.4 API 核验

使用本地 1.11.4 artifact 执行 `javap` 核实：

- `TableInfoFactory.ofEntityClass(Class<?>)`、`TableInfoFactory.ofTableName(String)` 均存在。
- `TableInfo` 提供 `getPrimaryColumns()`、`getPrimaryKeyList()`、`getPropertyColumnMapping()`、`getLogicDeleteColumn()`、`getVersionColumn()`、`getOnInsertColumns()`、`getOnUpdateColumns()` 与 `getColumnInfoList()`。
- 1.11.4 的 `QueryWrapper` 不提供 brief 中所述的 `entityClass`。实际 `from(Class<?>)` 将实体表写入受保护的 `queryTables`；解析器因此通过 MyBatis `MetaObject` 读取 `queryTables`，再用 `TableInfoFactory.ofTableName(...)` 解析实体。保留了 `entityClass` getter 探测，以兼容可能提供该属性的扩展 wrapper。
- 主键 `IdInfo` 不在 `ColumnInfoList` 内；实现通过 `getPrimaryKeyList()` 补齐主键，并按 `TableInfoFactory.getColumnFields(...)` 顺序结合 property-column 映射输出完整、有序元数据。
- Flex BaseMapper 的真实参数名为 `$$entity` 与 `$$query`。

## 行为覆盖

- 仅在 MappedStatement namespace 可加载且属于真实 Flex `BaseMapper` 继承链时支持 DML。
- 直接实体、标准参数包装、唯一且无歧义的 QueryWrapper 表信息和多级 Mapper 泛型按顺序解析；无效候选继续回退。
- 普通 MyBatis Mapper 与 Plus Mapper 即使传入已初始化 Flex TableInfo 的实体，也不会被 Flex 适配器抢占。
- 映射主键、property-column、ignore、逻辑删除、version、onInsert/onUpdate，并保持声明顺序；公共 `FrameworkChangeMetadata` 提供不可变快照。
- 动态 schema/table 仅保留实体静态元数据；运行目标仍由公共层分析最终 BoundSql。
- 生成键读取覆盖实体、Map、`$$entity` 参数包装与请求列顺序；只有真实 MyBatis `ParamMap` 才按包装别名拆包，避免普通业务 Map 的 `entity/record/param1/arg0` 字段冲突。
- 同一测试 classpath 下，Registry 对 Plus/Flex/原生 Mapper 分别选择 order 200/100/0 的适配器。

## TDD 与验证

RED：

```text
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mf/fw-plus-mf-datachange -am \
  -Dtest=MybatisFlexDataChangeAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

COMPILATION ERROR: 找不到符号 MybatisFlexDataChangeAdapter
```

首次测试夹具使用嵌套 `@Table` 类时，Flex annotation processor 生成了与测试类同名的子包；将实体移到顶层测试夹具后，RED 收敛为上述预期缺失类错误。

聚焦 GREEN：

```text
Tests run: 12, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

双适配器与公共层完整验证：

```text
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mf/fw-plus-mf-datachange,fw-plus-mp/fw-plus-mp-datachange \
  -am clean test

fw-plus-mybatis-datachange: 181 tests
fw-plus-mp-datachange:       13 tests
fw-plus-mf-datachange:       12 tests
合计:                        206 tests
Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

补充检查：生产类 class-file major version 为 `52`；`git diff --check` 通过；生产源码/POM 未引用 Spring、mf-base 或 Plus，Plus 仅存在于 test scope。

## 修复轮 1/5

### 修复内容

- 实体和 QueryWrapper 别名仅对真实 `MapperMethod.ParamMap` 拆包；普通业务 `Map` 一律回退 Mapper 泛型。
- QueryWrapper 的 `queryTables` 仅在严格单表、schema 全名与简单表名解析出的有效实体候选唯一时采用；多表、已知/未知混合以及名称冲突均回退 Mapper 泛型。
- fill 元数据改为按 `TableInfoFactory.getColumnFields(...)` 声明顺序统一解析 `IdInfo` 与普通 `ColumnInfo`，主键同时声明 `@Id` 和 `@Column(onInsertValue/onUpdateValue)` 时会进入 fill 列表，且 property/column 均不重复。
- 从 `fw-plus-mf` 聚合父 POM 删除 `fw-plus-common` 继承依赖，不向子模块下移。

### RED

```text
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mf/fw-plus-mf-datachange -am \
  -Dtest=MybatisFlexDataChangeAdapterTest \
  -Dsurefire.failIfNoSpecifiedTests=false test

Tests run: 17, Failures: 6, Errors: 0, Skipped: 0
BUILD FAILURE
```

6 个失败分别覆盖普通 Map 实体别名、普通 Map Wrapper 别名、QueryWrapper 多表、已知/未知表混合、schema/simple-name 实体歧义以及主键 fill 缺失，均为本轮预期缺陷。

### GREEN 与完整验证

聚焦 GREEN：

```text
Tests run: 17, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

公共层、mf-base、Plus 与 Flex 完整验证：

```text
mvn -f fw-plus-core/fw-plus-project/fw-plus-mybatis/pom.xml \
  -pl fw-plus-mf/fw-plus-mf-base,fw-plus-mf/fw-plus-mf-datachange,fw-plus-mp/fw-plus-mp-datachange \
  -am clean test

fw-plus-mybatis-datachange: 181 tests
fw-plus-mf-base:             no sources / no tests
fw-plus-mp-datachange:       13 tests
fw-plus-mf-datachange:       17 tests
合计:                        211 tests
Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

补充检查：compile `dependency:tree` 显示 mf-base 无依赖，mf-datachange 仅依赖公共 DataChange 模块和 `mybatis-flex-core`；两个模块 effective POM 的项目级 `fw-plus-common` 依赖数均为 `0`。生产源码无 Spring、Plus、mf-base、common 引用；三个生产 class-file major version 均为 `52`；`git diff --check` 通过。

## 非阻塞提示

- Maven 全局/用户 settings 均报告既有的 `Unrecognised tag: repository` 警告。
- 测试 classpath 未配置 SLF4J binder，Flex/Plus 测试输出 NOP logger 提示；不影响测试结果。
