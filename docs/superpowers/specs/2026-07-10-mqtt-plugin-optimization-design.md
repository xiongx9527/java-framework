# Magic-API MQTT 插件收敛与客户端优化设计

## 背景

当前 Boot 自动配置层同时存在两个 Magic-API MQTT 插件：

- `magicapi/plugin/mqttclient`：作为 MQTT 客户端连接外部 Broker，面向 Magic-API 脚本提供发布与订阅能力。
- `magicapi/plugin/mqtt`：作为内置 MQTT 服务端/Broker，支持在应用内动态启动 MQTT Server。

从项目通用性和运维成本看，常规业务更需要连接外部 MQTT Broker，而不是让应用自身承担 Broker 职责。因此本次优化将 `mqttclient` 明确为主推能力，将 `mqtt` 服务端插件降级为高级可选能力。

已有 `docs/superpowers/specs/2026-07-08-mqtt-autoconfigure-optimization-design.md` 聚焦基础 MQTT 自动配置包 `autoconfigure/mqtt` 的标准化；本设计聚焦 Magic-API 插件层 `autoconfigure/magicapi/plugin/mqtt*`，两者范围不同。

## 目标

1. 服务端插件 `magicapi/plugin/mqtt` 默认不启用，避免普通项目误启动 MQTT 服务端口。
2. 系统检查并优化 `magicapi/plugin/mqttclient`，提升客户端发布、订阅、配置解析、资源释放和异常处理的稳定性。
3. 修正明显不准确的命名、注释和复制粘贴遗留问题。
4. 保持现有资源目录、配置字段和脚本调用方式兼容。
5. 新增或调整关键逻辑时使用中文注释，说明行为边界和风险点。

## 非目标

本次不做以下事情：

- 不删除 `magicapi/plugin/mqtt` 服务端插件源码。
- 不实现完整标准 MQTT Broker。
- 不重命名 Magic-API 资源目录。
- 不引入新的重型依赖。
- 不大规模改造前端控制台插件。
- 不破坏已有 `mqttclient` 的 `key`、`ip`、`port`、`clientId`、`username`、`password`、`other` 配置格式。

## 设计方案

### 1. 服务端插件默认关闭

将 `MagicAPIMqttConfiguration` 中的服务端插件启用条件从默认启用调整为显式启用：

```java
@ConditionalOnProperty(
    prefix = "magic-api.plugin.mqtt",
    name = "enabled",
    havingValue = "true",
    matchIfMissing = false
)
```

用户如果确实需要内置 MQTT Broker，需要显式配置：

```yaml
magic-api:
  plugin:
    mqtt:
      enabled: true
```

这里会添加中文注释，明确该插件是“内置 MQTT 服务端/Broker 高级能力”，普通连接外部 Broker 的场景应使用 `mqttclient`。

### 2. 客户端插件作为主推路径

`mqttclient` 继续作为 Magic-API MQTT 集成的主入口，职责边界为：

- 动态配置外部 MQTT Broker 连接。
- 在 MagicScript 中通过 `mqttClient` 模块发布消息。
- 通过 `mqttclient-subscribe` 资源订阅外部 Broker Topic。
- 收到消息后交给 Magic-API 脚本处理。

不会把客户端插件改造成服务端，也不会让它承担 Broker 职责。

### 3. 发布端优化

重点检查 `MqttClientProducer`：

- 保持 `String`、`byte[]`、普通对象三类 payload 的现有兼容行为。
- 检查 `topic` 参数校验是否清晰。
- 检查 `other` 配置解析是否和消费者侧保持一致。
- 如果 SSL 配置尚未真正生效，要通过中文注释明确当前不是完整 SSL 支持，避免误导。
- 检查 `MqttClientTemplate` 资源关闭逻辑，避免动态刷新数据源后泄漏连接。

### 4. 订阅端优化

重点检查 `MqttClientConsumer`：

- 订阅重建时先安全取消旧订阅并销毁旧模板。
- 检查 topic 为空、enabled=false、QoS 非法值等边界处理。
- 检查动态配置覆盖逻辑，确保配置中心值优先级清晰。
- 检查 `clientId` 使用方式，避免同一数据源多个 topic 订阅时复用相同 clientId 导致连接互踢；如需调整，应兼容原有 clientId，并通过派生 clientId 区分订阅连接。
- 异常日志保留 topic、数据源 key 等关键信息，便于排查。

### 5. 动态数据源优化

重点检查 `MagicDynamicMqttClientDataSource`：

- 替换数据源时关闭旧 producer。
- 删除数据源时关闭 producer 和 consumer。
- 应用关闭时释放所有 producer/consumer。
- 检查默认 Spring `MqttClientTemplate` 和动态数据源并存时的行为，避免默认数据源和动态 key 行为混淆。

### 6. 命名和注释清理

修正明显误导的命名与注释，例如：

- 服务端插件中从其他模块复制遗留的变量名，如 `magicDynamicWeaviateDataSource`。
- 注释中提到的条件类型与实际 `@ConditionalOnBean` / `@ConditionalOnMissingBean` 不一致的问题。
- `mqtt` 与 `mqttclient` 的边界说明不清的问题。

新增注释只放在关键行为处，不给简单 getter/setter 添加噪音注释。

## 兼容性

- `mqttclient-datasource` 与 `mqttclient-subscribe` 资源目录保持不变。
- `mqttClient.publish(...)` 调用方式保持不变。
- `mqttClient['key'].publish(...)` 调用方式保持不变。
- 已显式配置 `magic-api.plugin.mqtt.enabled=true` 的服务端用户仍可启用服务端插件。
- 未显式配置服务端插件的普通用户将不再默认启用内置 MQTT Broker，这是本次唯一有意改变的默认行为。

## 测试计划

至少验证以下内容：

1. Maven 编译通过。
2. 未配置 `magic-api.plugin.mqtt.enabled` 时，`MagicAPIMqttConfiguration` 不加载。
3. 配置 `magic-api.plugin.mqtt.enabled=true` 时，服务端插件仍可加载。
4. `mqttclient` 自动配置可正常加载。
5. `MqttClientModule` 默认数据源和指定 key 数据源行为保持兼容。
6. 发布端 topic 为空、payload 为字符串/字节数组/对象时行为符合预期。
7. 订阅端 QoS、enabled、topic 空值、旧订阅重建等边界行为可被测试覆盖。

## 风险与缓解

### 风险：服务端插件默认关闭影响已有隐式依赖用户

缓解：这是有意的安全默认值调整。文档和注释中明确说明，如果需要内置 Broker，配置 `magic-api.plugin.mqtt.enabled=true` 即可恢复。

### 风险：订阅端 clientId 派生改变连接标识

缓解：如果实现派生 clientId，应仅在多 topic 订阅连接需要区分时使用，并保持基础 clientId 可识别，避免完全改变用户配置语义。

### 风险：SSL 配置被误认为完整支持

缓解：若本轮不完整实现 SSL，则通过中文注释和文档说明当前状态；如实现 SSL，则补充对应测试。

## 后续实施顺序

1. 调整服务端插件默认启用条件。
2. 清理服务端插件明显错误命名和注释。
3. 系统检查并优化 `mqttclient` 发布端。
4. 系统检查并优化 `mqttclient` 订阅端。
5. 补充或调整测试。
6. 运行 Maven 编译和相关测试。
