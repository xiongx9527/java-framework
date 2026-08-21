# Magic-API Options 使用说明

本文说明 `fw-plus` 当前暴露给 Magic-API 接口的所有 `Options` 配置项、配置方式和运行时行为。

选项来源：

- `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.enums.Options`
- Magic-API 原生同名枚举：`org.ssssssss.magicapi.core.model.Options`

`fw-plus` 在原生选项基础上扩展了防重复提交、Sentinel 热点/熔断、请求响应日志等能力。控制台通过 `/magic-api/web/optionList` 返回枚举列表，每项格式为 `[key, name, defaultValue]`。

## 配置方式

### 接口 JSON 头部

在 `resources/api/*.ms` 的 JSON 头部配置 `options`：

```json
{
  "name": "创建订单",
  "path": "/order/create",
  "method": "POST",
  "options": [
    { "name": "avoid_repeat_submit_enable", "value": "true" },
    { "name": "avoid_repeat_submit_timeout", "value": "3000" },
    { "name": "avoid_repeat_submit_key", "value": "#{#params['orderNo']}" },
    { "name": "hot_data_enable", "value": "true" },
    { "name": "hot_data_key", "value": "#{#params['tenantId']}" },
    { "name": "hot_data_count", "value": "20" },
    { "name": "fallback_enable", "value": "true" },
    { "name": "fallback_count", "value": "100" }
  ]
}
================================
return { success: true }
```

### 分组选项

Magic-API 的 `ApiInfo.getOptionValue` 会先读取接口自身 `options`，找不到时再读取接口所在分组的 `options`。因此，原生选项和 `fw-plus` 扩展选项都可以通过分组设置作为一批接口的默认策略。

### 外部配置覆盖

`fw-plus` 扩展能力通过 `ParamConfig` 读取配置，支持外部配置覆盖接口/分组选项：

```yaml
magic:
  api:
    /order/create:
      options:
        avoid_repeat_submit_enable: false
        print_request_parameters: false
        hot_data_count: 50
```

等价 properties：

```properties
magic.api./order/create.options.avoid_repeat_submit_enable=false
magic.api./order/create.options.print_request_parameters=false
magic.api./order/create.options.hot_data_count=50
```

优先级：

1. 外部配置：`magic.api./{接口路径}.options.{key}`
2. 接口自身 `options`
3. 分组 `options`

注意：外部配置路径会按接口 `path` 匹配，`ParamConfig.getPath` 会确保路径以 `/` 开头。

## 默认值规则

`Options.defaultValue` 主要用于控制台展示和新增选项时提供默认值。运行时是否自动使用默认值取决于具体处理器：

- Magic-API 原生处理器通常直接读取 `ApiInfo.getOptionValue`，没有配置时多为“不启用”。
- `fw-plus` 扩展开关通过 `ParamConfig.isEnabled` 判断，只有值等于 `true`（忽略大小写）才启用；未配置时不会因为枚举默认值为 `true` 而自动启用。
- 部分数值/消息类扩展项在为空或非法时会回退到枚举默认值，例如 `avoid_repeat_submit_timeout`、`avoid_repeat_submit_msg`、`hot_data_count`、`hot_data_duration`、`fallback_count`。

建议：需要明确启用的能力，在接口或分组选项中显式配置 `*_enable=true`。

## 原生 Magic-API 选项

| key | 名称 | 默认值 | 用法 |
| --- | --- | --- | --- |
| `wrap_request_parameter` | 包装请求参数到一个变量中 | 无 | 配置后，`RequestHandler` 会把请求参数 Map 绑定到该变量名。例如值为 `query`，脚本中可通过 `query` 访问全部请求参数。 |
| `default_data_source` | 配置默认数据源的 key | 无 | 请求进入脚本上下文时写入 `default_data_source`。动态数据源模块会读取它作为默认数据源 key。 |
| `permission` | 允许拥有该权限的访问 | 无 | 访问权限配置项，交给项目自定义 `AuthorizationInterceptor` 或 Magic-API 权限体系判断。 |
| `permission_visible` | 允许拥有该权限的查看 | 无 | 控制台查看/列表可见性配置项，资源树过滤时会经 `AuthorizationInterceptor.allowVisit(..., Authorization.VIEW, entity)` 判断。 |
| `role` | 允许拥有该角色的访问 | 无 | 访问角色配置项，具体含义由认证授权实现解释。 |
| `role_visible` | 允许拥有该角色的查看 | 无 | 控制台查看/列表可见性角色配置项，具体含义由认证授权实现解释。 |
| `require_login` | 该接口需要登录才允许访问 | `true` | 登录控制配置项。Magic-API 控制台接口的登录校验由 `@Valid(requireLogin=...)` 和 `AuthorizationInterceptor.requireLogin()` 处理；接口级该选项供权限实现读取。 |
| `anonymous` | 该接口不需要登录也可访问 | `true` | 匿名访问配置项，通常与登录/权限实现配合使用。 |
| `disabled_unknown_parameter` | 不接收未经定义的参数 | `true` | 值为 `true` 时，参数/path/header/requestBody 校验会移除未定义字段，并只保留接口定义中的字段。 |
| `disabled_validate_request_body` | 禁止验证 RequestBody | `false` | 值为 `true` 时，跳过 requestBody definition 的结构校验。 |

### `default_data_source` 生效模块

脚本上下文存在 `default_data_source` 时，以下动态模块会读取它作为默认数据源：

- JDBC/Magic-API 原生数据库模块
- MyBatis-Flex：`mybatisflex`
- Redis：`redis`
- MongoDB：`mongo`
- Elasticsearch：`elasticsearch`
- InfluxDB：`influx`
- Kafka：`kafka`
- Pulsar：`pulsar`
- RocketMQ：`rocketmq`
- RabbitMQ：`rabbitmq`
- MQTT/MQTT Client：`mqtt`、`mqttclient`
- S3：`s3`
- DTP：`dtp`
- WebSocket：`ws`

示例：

```json
{
  "path": "/demo/query",
  "method": "GET",
  "options": [
    { "name": "default_data_source", "value": "evaluation" }
  ]
}
================================
return db.select('select 1')
```

等价于在脚本中优先使用 `evaluation` 数据源；没有配置时使用模块自身默认数据源。

## 防重复提交选项

防重复提交由 `RepeatSubmitRequestInterceptor` 处理，底层使用 Magic-API `lock` 模块。拦截器在请求进入脚本前尝试加锁，在请求完成后释放锁。

| key | 名称 | 默认值 | 用法 |
| --- | --- | --- | --- |
| `avoid_repeat_submit_enable` | 启动避免重复提交 | `true` | 值为 `true` 时启用防重复提交。未显式配置时不启用。 |
| `avoid_repeat_submit_timeout` | 避免重复提交超时时间（毫秒） | `1000` | 加锁等待时间和锁过期时间，单位毫秒。非法或为空时回退到 `1000`。 |
| `avoid_repeat_submit_key` | 避免重复提交的 Key（支持 SpEL 表达式） | `#{magicapi:repeat:submit}` | 生成防重复提交主 key。支持 `#{#...}` 形式 SpEL；非 SpEL 时按普通字符串处理。为空时使用请求指纹兜底。 |
| `avoid_repeat_submit_key_subfix_spring_bean` | 避免重复提交的 Key 后缀（Spring Bean 名称） | `springbean` | 指定实现 `RepeatSubmitKeySuffixProvider` 的 Spring Bean 名称，用于给 key 追加后缀。找不到或类型不匹配时使用默认实现。 |
| `avoid_repeat_submit_msg` | 避免重复提交提示信息 | `重复提交，请稍后再试。` | 加锁失败时返回给调用方的提示信息。为空时回退到默认值。 |

最终锁 key 结构：

```text
magicapi:repeat:submit:{接口标识}:{主key}:{后缀}
```

接口标识优先使用 `ApiInfo.id`，没有 id 时使用 `{method}:{path}`。

### Key SpEL 上下文

`avoid_repeat_submit_key` 支持的变量：

| 变量 | 含义 |
| --- | --- |
| `#request` | 当前 HTTP request |
| `#response` | 当前 HTTP response |
| `#headers` | 请求头 Map |
| `#params` | 请求参数 Map |
| `#pathVariables` | 路径变量 Map |
| `#body` | requestBody |
| `#api` | 当前 `ApiInfo` |
| `#requestEntity` | 当前 `RequestEntity` |

示例：

```json
{
  "options": [
    { "name": "avoid_repeat_submit_enable", "value": "true" },
    { "name": "avoid_repeat_submit_timeout", "value": "5000" },
    { "name": "avoid_repeat_submit_key", "value": "#{#headers['X-Request-ID']}" },
    { "name": "avoid_repeat_submit_msg", "value": "请求处理中，请勿重复提交" }
  ]
}
```

## Sentinel 热点数据选项

热点数据由 `MagicApiSentinelGuardRequestInterceptor` 和 `MagicApiSentinelRuleManager` 处理。启用后会为当前接口创建 Sentinel `ParamFlowRule`，并以 `hot_data_key` 解析出的值作为热点参数。

| key | 名称 | 默认值 | 用法 |
| --- | --- | --- | --- |
| `hot_data_enable` | 启动热数据 | `true` | 值为 `true` 且 `hot_data_count > 0` 时热点限流生效。未显式配置时不启用。 |
| `hot_data_key` | 热点数据的 Key（支持 SpEL 表达式） | `#{magicapi:hot:data}` | 生成热点参数值。支持的 SpEL 上下文与防重复提交 key 一致。为空时使用请求指纹兜底。 |
| `hot_data_grade` | 热点数据的等级（QPS、THREAD） | `QPS` | `QPS` 表示按每秒请求数限流；`THREAD` 表示按并发线程数限流。其他值按 `QPS` 处理。 |
| `hot_data_count` | 热点数据的计数 | `0` | 阈值。必须大于 `0` 才生效。非法或为空时回退到 `0`。 |
| `hot_data_duration` | 热点数据的时长（秒） | `1000` | Sentinel `ParamFlowRule.durationInSec`，单位秒。非法或为空时回退到 `1000`。 |

示例：

```json
{
  "options": [
    { "name": "hot_data_enable", "value": "true" },
    { "name": "hot_data_key", "value": "#{#params['userId']}" },
    { "name": "hot_data_grade", "value": "QPS" },
    { "name": "hot_data_count", "value": "10" },
    { "name": "hot_data_duration", "value": "60" }
  ]
}
```

触发热点限流时返回：

```text
热点数据访问过于频繁，请稍后再试。
```

## Sentinel 熔断降级选项

熔断降级同样由 `MagicApiSentinelGuardRequestInterceptor` 和 `MagicApiSentinelRuleManager` 处理。启用后会为当前接口创建 Sentinel `FlowRule`。

| key | 名称 | 默认值 | 用法 |
| --- | --- | --- | --- |
| `fallback_enable` | 启动熔断降级 | `true` | 值为 `true` 且 `fallback_count > 0` 时熔断降级生效。未显式配置时不启用。 |
| `fallback_count` | 熔断降级的计数 | `0` | Sentinel `FlowRule.count`，按 QPS 阈值处理。必须大于 `0` 才生效。非法或为空时回退到 `0`。 |

示例：

```json
{
  "options": [
    { "name": "fallback_enable", "value": "true" },
    { "name": "fallback_count", "value": "100" }
  ]
}
```

触发熔断降级时返回：

```text
接口触发熔断降级，请稍后再试。
```

## 请求响应日志选项

日志由 `ParamLogger` 处理。

| key | 名称 | 默认值 | 用法 |
| --- | --- | --- | --- |
| `print_request_parameters` | 打印请求入参（调试用） | `true` | 值为 `true` 时打印请求来源、URL、params、requestBody、headers。未显式配置时不打印。 |
| `print_response_parameters` | 打印响应出参（调试用） | `true` | 值为 `true` 时打印 URL、code、msg、耗时和 data。未显式配置时不打印。 |

示例：

```json
{
  "options": [
    { "name": "print_request_parameters", "value": "false" },
    { "name": "print_response_parameters", "value": "false" }
  ]
}
```

生产环境建议默认关闭请求/响应日志，尤其是接口可能包含密码、token、身份证号、手机号等敏感字段时。

## 推荐组合

### 普通写接口

```json
{
  "options": [
    { "name": "avoid_repeat_submit_enable", "value": "true" },
    { "name": "avoid_repeat_submit_timeout", "value": "3000" },
    { "name": "avoid_repeat_submit_key", "value": "#{#headers['X-Request-ID']}" },
    { "name": "print_request_parameters", "value": "false" },
    { "name": "print_response_parameters", "value": "false" }
  ]
}
```

### 高并发查询接口

```json
{
  "options": [
    { "name": "hot_data_enable", "value": "true" },
    { "name": "hot_data_key", "value": "#{#params['tenantId']}" },
    { "name": "hot_data_grade", "value": "QPS" },
    { "name": "hot_data_count", "value": "200" },
    { "name": "fallback_enable", "value": "true" },
    { "name": "fallback_count", "value": "500" }
  ]
}
```

### 指定默认数据源

```json
{
  "options": [
    { "name": "default_data_source", "value": "evaluation" }
  ]
}
```

## 排查清单

- 开关不生效：确认对应 `*_enable` 是否显式配置为字符串 `true`。
- 外部配置不生效：确认 key 是否为 `magic.api./{接口路径}.options.{optionKey}`，接口路径是否包含开头 `/`。
- Sentinel 不限流：确认 `hot_data_count` 或 `fallback_count` 是否大于 `0`。
- SpEL 报错：确认表达式是否为 `#{#...}` 格式，例如 `#{#params['id']}`。
- 防重复提交误伤：检查 `avoid_repeat_submit_key` 是否过粗；必要时加入租户、用户、业务单号等字段。
- Spring Bean 后缀不生效：确认 Bean 名称匹配 `avoid_repeat_submit_key_subfix_spring_bean`，且实现了 `RepeatSubmitKeySuffixProvider`。
