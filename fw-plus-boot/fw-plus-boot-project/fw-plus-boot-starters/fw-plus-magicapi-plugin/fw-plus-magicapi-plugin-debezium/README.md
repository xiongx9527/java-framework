# Magic-API Debezium 插件使用说明

## 插件能力

Debezium 插件用于在 Magic-API 控制台维护 CDC 数据源配置，并在数据源启用后启动内嵌 Debezium Engine 监听数据库变更。捕获到的变更事件会批量转发到配置的 Kafka Topic。

数据源资源目录：

```text
debezium-datasource/
```

资源文件格式为 JSON，保存的是连接配置，不是 magic-script 脚本。

## 启用条件

插件自动配置开关：

```yaml
magic-api:
  plugin:
    debezium:
      enabled: true
```

默认 `matchIfMissing=true`，不显式关闭时会尝试启用。实际生效还要求 classpath 中存在：

- `io.debezium.engine.DebeziumEngine`
- `org.springframework.kafka.core.ProducerFactory`

## 连接器下拉

Debezium datasource 表单的连接器下拉会在 Magic-API 控制台登录后从后端动态加载。控制台请求路径：

```text
POST /debezium/datasource/connector-options
```

后端只返回当前 classpath 中真实存在的 Debezium Connector 类。默认候选包括：

- `io.debezium.connector.mysql.MySqlConnector`
- `io.debezium.connector.postgresql.PostgresConnector`
- `io.debezium.connector.sqlserver.SqlServerConnector`
- `io.debezium.connector.oracle.OracleConnector`
- `io.debezium.connector.mongodb.MongoDbConnector`

当前插件 POM 默认引入 `debezium-embedded`、`spring-kafka` 和 `debezium-connector-mysql`，所以默认情况下通常只会显示 MySQL Connector。PostgreSQL、SQL Server、Oracle、MongoDB 等 Connector 需要在 POM 中按需启用对应依赖后才会出现在下拉中。

如果接口请求成功但返回空列表，前端会显示为空；这表示当前 classpath 没有可用的默认 Connector 类。如果编辑已有 datasource 时当前 `connectorClass` 不在动态候选中，前端会临时追加当前值，保证历史配置能继续显示和保存。

## 数据源配置示例

```json
{
  "name": "本地 MySQL CDC",
  "key": "local_mysql",
  "enabled": true,
  "connectorClass": "io.debezium.connector.mysql.MySqlConnector",
  "hostname": "127.0.0.1",
  "port": "3306",
  "username": "root",
  "password": "root",
  "database": "demo",
  "includeTables": "user,order",
  "excludeTables": "",
  "kafkaServers": "127.0.0.1:9092",
  "kafkaTopic": "demo_cdc",
  "serverIdType": "random",
  "other": "{\"snapshot.mode\":\"no_data\"}"
}
```

## 字段说明

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `name` | 是 | 控制台显示名称。 |
| `key` | 是 | 数据源唯一标识，保存后不可修改。 |
| `enabled` | 否 | 是否启动该 CDC 数据源，默认 `false`。 |
| `connectorClass` | 是 | Debezium Connector 类名。 |
| `hostname` | 是 | 数据库主机。 |
| `port` | 是 | 数据库端口，必须是整数。 |
| `username` | 是 | 数据库用户名。 |
| `password` | 是 | 数据库密码。 |
| `database` | 是 | 数据库名，会写入 `database.include.list`。 |
| `includeTables` | 否 | 逗号分隔的包含表。可写 `user,order`，后端会补成 `database.user,database.order`。 |
| `excludeTables` | 否 | 逗号分隔的排除表。仅当 `includeTables` 为空时生效。 |
| `kafkaServers` | 是 | Kafka bootstrap servers。 |
| `kafkaTopic` | 是 | 业务变更事件输出 Topic。 |
| `serverIdType` | 否 | MySQL `database.server.id` 生成策略：`random` 或 `fixed`，默认 `random`。 |
| `other` | 否 | JSON 对象字符串，用于覆盖 Debezium 或 Kafka Producer 扩展参数。 |

## 连接测试

控制台测试连接调用：

```text
POST /debezium/datasource/test
```

测试逻辑包括：

- 校验必填字段和端口格式。
- 按 `connectorClass` 测试数据库连通性。
- 校验 `includeTables` / `excludeTables` 中的表是否存在。
- 测试 Kafka 是否可连接。

关系型数据库测试通过 JDBC 完成；MongoDB 测试通过 Mongo Client 完成。对应 JDBC Driver 或 MongoDB 客户端依赖需要在 classpath 中存在，否则测试会失败。

## Kafka 输出

插件启动 Debezium Engine 后，会把捕获到的变更事件发送到 `kafkaTopic`。每批事件会生成稳定的 `messageId` header，用于下游幂等处理。

插件还会使用 Kafka 保存 Debezium 内部状态，Topic 前缀格式为：

```text
debezium_{springProfile}_{key}
```

例如当前 profile 为 `dev`，数据源 `key=local_mysql` 时，内部 Topic 包括：

- `debezium_dev_local_mysql_offsets`
- `debezium_dev_local_mysql_configs`
- `debezium_dev_local_mysql_statuses`
- `debezium_dev_local_mysql_dbhistory`

内部 offsets/configs/statuses Topic 会使用 `compact` 策略，dbhistory Topic 使用 `delete` 策略。

## other 扩展配置

`other` 必须是 JSON 对象字符串。顶层 String 类型的键会写入 Debezium Engine properties：

```json
{
  "snapshot.mode": "initial",
  "decimal.handling.mode": "string",
  "filter.ddl.events": "false"
}
```

`other.kafka` 可以覆盖 Kafka Producer 参数：

```json
{
  "kafka": {
    "compression.type": "lz4",
    "linger.ms": "100"
  }
}
```

当前默认 `snapshot.mode` 为 `no_data`（Debezium 3.x 取值，旧名 `schema_only`），表示捕获表结构后从 offset 或当前 binlog 位置开始增量读取。可以通过 `other.snapshot.mode` 覆盖为 Debezium 支持的其他值，例如 `initial`、`when_needed`、`recovery`；旧值 `schema_only` / `schema_only_recovery` 会被自动归一化为 `no_data` / `recovery`。

## 配置中心覆盖

运行时字段会优先读取配置中心：

```text
magic.api.debezium.{key}.{field}
```

例如：

```yaml
magic:
  api:
    debezium:
      local_mysql:
        hostname: 127.0.0.1
        port: 3306
        username: root
        password: root
        database: demo
        kafkaServers: 127.0.0.1:9092
        kafkaTopic: demo_cdc
        includeTables: user,order
        serverIdType: fixed
        other: '{"snapshot.mode":"no_data"}'
```

配置中心值会覆盖 datasource JSON 中的同名字段。

## 依赖扩展

默认 POM 只启用 MySQL Connector：

```xml
<dependency>
    <groupId>io.debezium</groupId>
    <artifactId>debezium-connector-mysql</artifactId>
</dependency>
```

其他 Connector 已在 POM 中保留注释示例，按需打开：

```xml
<dependency>
    <groupId>io.debezium</groupId>
    <artifactId>debezium-connector-postgres</artifactId>
</dependency>
```

启用其他 Connector 时，也要确保连接测试需要的 JDBC Driver 或 MongoDB 客户端依赖存在。

## 注意事项

- 该插件是 CDC 采集插件，不提供 magic-script 侧的 `import debezium` 调用模块。
- 数据变更输出到 Kafka，业务侧需要从 `kafkaTopic` 消费。
- `includeTables` 优先级高于 `excludeTables`；两者同时配置时仅 `includeTables` 生效。
- `key` 用于内部 Topic、引擎实例和资源唯一性，保存后不要修改。
- 新增表加入 `includeTables` 后需要重启对应 Connector 才能触发新表 schema 捕获。
