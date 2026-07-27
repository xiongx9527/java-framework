# Magic 数据源 properties

## 目标

Magic-API 数据源插件以 `properties` JSON 对象承载各客户端的扩展配置，使前端编辑、资源持久化、后端绑定和运行时消费使用同一种结构。

## 适用范围

本能力适用于 LangChain4j、Debezium、DTP、Elasticsearch、InfluxDB、Kafka、MongoDB、MQTT、MQTT Client、Pulsar、RabbitMQ、Redis、RocketMQ、S3 和 WebSocket 数据源。

订阅资源不属于本能力；其现有 `other` 字段和行为保持不变。

## 数据契约

- 每个适用的数据源实体提供 `properties: Map<String, Object>`，默认值为空的有序可变 Map。
- `properties` 对应的 JSON 值必须是对象；空值按空对象处理，数组、标量和无效 JSON 不得作为有效数据源扩展配置。
- Map 中允许字符串、数字、布尔值、列表、嵌套对象和 null，并在前端、持久化与运行时消费之间保持等价 JSON 类型。
- 数据源的 `copy()` 创建独立 Map 副本；每个实体的 `simple()` 是否包含扩展配置保持迁移前的现有边界，包含时同样使用独立 Map，避免共享可变引用。
- 新建或更新资源时只写出 `properties`，不得写出前端临时文本字段。
- 不提供 `other` 的读取别名或自动迁移：只含 `other` 的旧资源和配置中心值不会作为扩展配置生效，使用方必须显式迁移到 `properties`。

## 前端行为

- 数据源表单使用 JSON 编辑器编辑 `properties`，打开时把对象格式化为 JSON 文本。
- 保存和连接测试前把编辑文本解析为 JSON 对象，并删除临时文本字段。
- JSON 无效或解析结果不是对象时，阻止请求并显示校验错误。
- 插件源码、中文/英文文案和仓库提交的 IIFE 构建产物使用 `properties` 命名与对象语义。

## 后端行为

- 校验逻辑直接验证 `properties` 是可用 Map，不再把扩展配置作为 JSON 字符串重复解析。
- 动态客户端、消费者和配置构建逻辑直接从 Map 读取配置值，并保留原有键名、默认值、类型转换和错误处理语义。
- 配置中心覆盖项与实体字段统一使用 `properties` 命名，不再查询 `other` 键。

## 验收场景

1. 保存包含嵌套对象、列表、数字和布尔值的 `properties`，重新读取后得到等价对象，运行时构建使用对应值。
2. 前端输入 `{}` 或留空时，请求发送空对象；输入 `[]`、`"text"` 或不完整 JSON 时不发送请求。
3. 复制数据源后修改副本的 `properties` 不会改变原对象。
4. 15 个目标数据源的新资源不包含 `other` 或临时文本字段。
5. 订阅资源继续使用 `other`，现有保存与执行路径不受影响。
6. 目标数据源只提供 `other` 而未提供 `properties` 时，旧扩展配置不生效；迁移为等价 `properties` 对象后生效。
