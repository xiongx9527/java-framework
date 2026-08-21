# Outcome

Magic-API 数据源插件的扩展配置统一使用 `properties` JSON 对象，不再以 `other` JSON 字符串作为当前数据模型；后端、前端编辑与持久化结构保持一致，并以 vector 数据源插件的 `Map<String, Object>` 模型为参照。

# Scope

- 将以下 15 个数据源实体中的 `String other` 替换为 `Map<String, Object> properties`：LangChain4j、Debezium、DTP、Elasticsearch、InfluxDB、Kafka、MongoDB、MQTT、MQTT Client、Pulsar、RabbitMQ、Redis、RocketMQ、S3、WebSocket。
- 将上述数据源的后端校验、动态客户端构建、配置中心覆盖读取、复制与简化对象逻辑从 `other` 字符串解析迁移到 `properties` Map。
- 将上述插件前端的字段绑定、JSON 编辑器、保存/测试请求、默认值、提示文案与已提交 IIFE 产物同步为 `properties` 对象语义。
- 必要时完善 Magic-API 数据源宿主的保存规范化入口，使编辑器中的 JSON 文本在发送前可靠转换成对象。
- 增补或更新覆盖字段绑定、对象校验、客户端配置消费和前端构建的测试。

# Non-goals

- 不修改 vector 数据源的业务能力；它只作为目标数据结构和交互参照。
- 不修改 Kafka、Pulsar、RabbitMQ、RocketMQ、MQTT Client 等订阅资源中的 `other` 字段。
- 不修改非 Magic-API 数据源插件、运行环境外置 `resources/` 内容或 Magic-API 2.2.2 依赖版本。
- 不改变各数据源已有的连接参数含义、默认值和启停行为。

# Acceptance examples

- 数据源 JSON 输入 `"properties":{"timeout":3000,"ssl":true}` 时，后端实体得到保留值类型的 Map，客户端构建逻辑读取同一 Map，并在复制/持久化后保持等价。
- 前端打开已有 `properties` 对象时，JSON 编辑器显示格式化对象；用户保存合法 JSON 对象后，请求体包含 `properties` 对象且不包含编辑器临时字段。
- 前端输入数组、标量或非法 JSON 时，保存和连接测试不会发送无效请求，并显示可理解的校验错误。
- 新建或更新的 15 类数据源资源使用 `properties` 字段，不再写出 `other` 字段。
- 订阅资源仍按原结构读写 `other`，行为不受影响。

# Constraints and invariants

- 保持 JDK 8 兼容，不使用 Java 9+ API。
- 保持 Spring Boot 2.7.18、Magic-API 2.2.2 及内部 Nexus 依赖版本不变。
- Map 实现与 vector 插件一致，使用有序、可变且非空的默认对象；复制时不得共享可变 Map 引用。
- `properties` 仅接受 JSON 对象，空值按空对象处理；保留字符串、数字、布尔、列表及嵌套对象的 JSON 值类型。
- 各实体 `simple()` 与 `copy()` 是否包含扩展配置保持当前行为边界，只把包含时的数据从 `other` 字符串替换为独立的 `properties` Map。
- 前端源码与仓库提交的插件 IIFE 资源必须同步，不能只改其中一份。

# Decisions

- 用户明确将范围限定为 datasource 插件；所有 subscribe 资源排除。
- 目标字段名和数据结构为 vector 插件采用的 `properties: Map<String, Object>`。
- 前后端同时迁移，不能仅做 Java 模型改名。
- 用户选择严格迁移：旧资源和配置中心中的 `other` 不兼容读取；使用方必须迁移到 `properties`，新版本只写出 `properties`。
- 用户已确认当前共享理解并授权进入实现。

# Open questions

- 无。

# Verification expectations

- 使用 CodeGraph 与精确搜索复核 15 个目标数据源实体及其消费链中不再存在 `getOther`、`setOther` 或 `String other`。
- 运行 fw-plus-boot 和 fw-plus-ai 受影响模块的定向单元/集成测试，并覆盖至少一个嵌套、混合类型的 `properties` 对象。
- 对所有受影响前端插件运行可用的构建命令，确认源码和 IIFE 产物生成成功。
- 检查订阅实体及其前端仍保留 `other`，且无意外改动。
