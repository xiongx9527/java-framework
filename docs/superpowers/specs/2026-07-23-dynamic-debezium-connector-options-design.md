---
comet_change: dynamic-debezium-connector-options
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-23-dynamic-debezium-connector-options
status: final
---

# Debezium 连接器选项动态化设计

## 背景

Debezium 数据源编辑组件 `magic-debezium-datasource-info.vue` 当前直接硬编码 `connectorOptions`，其中包含 MySQL、PostgreSQL、SQL Server、Oracle、MongoDB 五类 connector class。这个列表和运行时依赖并不天然一致：Debezium 插件 POM 默认只启用 `debezium-connector-mysql`，其他 connector 依赖目前是按需启用的注释项。

因此，前端硬编码列表会造成两个问题：新建数据源时可能展示运行时不可用的 connector；后端支持范围变化时必须重新修改和打包前端源码。目标是把 connector 选项改成运行时动态来源，同时兼容已有数据源配置。

## 目标

- Debezium 数据源表单从后端动态加载 `connectorClass` 下拉选项。
- 后端集中维护默认 connector class 候选列表，并按运行时 classpath 过滤可用项。
- 已有数据源的当前 `connectorClass` 即使不在动态列表里，也能在编辑表单中展示并保留。
- 不改变数据源 JSON 结构、测试接口、引擎启动逻辑、Kafka 配置和故障恢复逻辑。

## 非目标

- 不新增新的 Debezium connector Maven 依赖。
- 不实现 connector jar 的运行时安装、热加载或 classpath 扫描发现机制。
- 不重构 Magic-API 资源管理或 Debezium 引擎创建流程。
- 不改变 `includeTables`、`excludeTables`、Kafka topic、server id 等现有字段行为。

## 方案概览

采用“后端候选列表 + classpath 过滤 + 前端当前值补齐”的方案。

数据流如下：

```text
MagicDebeziumController
  └─ connectorOptions()
       ├─ 默认候选 connector class 列表
       ├─ ClassUtils.isPresent / Class.forName 过滤运行时可加载项
       └─ JsonBean<List<Option>> 返回 [{ text, value }]

magic-debezium-datasource.js
  └─ loadConnectorOptions(success, failure)
       └─ request 调用 /debezium/datasource/connector-options

magic-debezium-datasource-info.vue
  ├─ onMounted 加载选项
  ├─ connectorOptions 使用 ref([])
  ├─ mergeCurrentConnectorOption 补齐 info.connectorClass
  └─ magic-select 渲染动态选项
```

## 后端设计

在 `MagicDebeziumController` 中新增只读接口：

```text
GET 或 RequestMapping /debezium/datasource/connector-options
返回：JsonBean<List<ConnectorOption>>
```

`ConnectorOption` 只需要两个字段：

```text
text: 展示文本，当前直接使用 connector class 全限定名
value: 实际保存值，connector class 全限定名
```

默认候选列表放在后端集中维护，覆盖当前前端已有值：

```text
io.debezium.connector.mysql.MySqlConnector
io.debezium.connector.postgresql.PostgresConnector
io.debezium.connector.sqlserver.SqlServerConnector
io.debezium.connector.oracle.OracleConnector
io.debezium.connector.mongodb.MongoDbConnector
```

返回前进行 classpath 可用性过滤。实现上保持 JDK 1.8 兼容，可以使用 `ClassUtils.isPresent(className, classLoader)` 或 `Class.forName(className, false, classLoader)`。过滤失败时不抛出业务异常，只是不返回该候选项。

不建议做全 classpath 扫描。扫描成本高、结果不可控，还可能把测试类或非预期实现暴露给控制台。显式候选列表更符合当前“按需启用 connector 依赖”的 POM 结构。

## 前端服务设计

在 `magic-debezium-datasource.js` 返回对象中新增方法，例如：

```text
loadConnectorOptions: (success, failure) => { ... }
```

该方法负责调用后端接口，并把返回结果交给组件。具体调用方式应贴合现有 `request` 封装；如果该封装没有直接的 GET helper，可使用项目中等价的 `sendGet`、`sendPost` 或 `sendJson` 风格，保持和其他 Magic-API 插件控制台一致。

服务层不做复杂合并逻辑。选项补齐依赖当前组件的 `info.connectorClass`，放在组件层更清晰。

## Vue 组件设计

`magic-debezium-datasource-info.vue` 中将 `connectorOptions` 从常量改为响应式状态：

```text
const connectorOptions = ref([])
```

组件挂载时调用服务加载后端选项。加载成功后先规范化返回值，只接受有 `value` 的项；再调用 `mergeCurrentConnectorOption`。该函数检查当前 `props.info.connectorClass`：

- 当前值为空：不追加。
- 当前值已在返回列表：不重复追加。
- 当前值不在返回列表：追加 `{ text: currentValue, value: currentValue }`。

组件还应监听 `props.info.connectorClass` 的变化，避免用户切换或外层编辑对象异步加载后，当前值没有被补回。

接口失败时，组件不弹出强阻断错误，不清空现有选项；只调用补齐逻辑，让当前值能继续展示。这样后端接口短暂不可用时，编辑已有数据源仍可继续。

## 错误处理

- 后端 classpath 检测失败：忽略该候选项，不影响其他候选项返回。
- 后端接口异常：按现有 MagicController/MagicExceptionHandler 机制返回错误。
- 前端请求失败：保留当前 `connectorClass`，表单继续可编辑。
- 返回值格式异常：前端过滤无效项，保留当前值。

## 测试策略

后端测试重点：

- 当前运行时存在 MySQL connector class 时，接口返回 MySQL connector 选项。
- 未引入依赖的候选 connector 不返回。
- 返回对象包含 `text` 和 `value`，且二者使用 connector class 全限定名。

前端测试或手工验证重点：

- 打开新建/编辑 Debezium 数据源表单时，下拉选项来自服务返回结果。
- 后端返回列表缺少当前 `info.connectorClass` 时，表单仍显示当前值。
- 服务请求失败时，当前 `info.connectorClass` 不被清空。
- Debezium console `npm run build` 通过。

构建验证：

```bash
cd fw-plus-boot && ./mvnw clean install -pl fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium -DskipTests
cd fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-debezium/src/console && npm run build
openspec validate dynamic-debezium-connector-options
```

## 实施顺序

1. 在后端新增选项结构、默认候选列表、classpath 过滤方法和只读接口。
2. 在前端服务中新增加载 connector options 的方法。
3. 在 Vue 组件中接入响应式选项、加载逻辑、当前值补齐和失败兜底。
4. 执行后端编译、前端构建和 OpenSpec 校验。

## 设计结论

该方案把 connector 选项从前端硬编码迁移到后端运行时事实源，同时不改变 Debezium 数据源存储和引擎行为。classpath 过滤与当前 POM 的按需 connector 依赖模式一致，前端当前值补齐则保证已有配置编辑体验不回退。
