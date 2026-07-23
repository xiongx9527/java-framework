## Why

Magic-API 接口已经暴露 `HOT_DATA_*` 和 `FALLBACK_*` options，但当前仅有配置项，没有运行时保护能力。参照 Akali 的 Sentinel 热点参数限流与熔断降级思路，需要让 Magic-API HTTP 接口能够按接口维度启用热点数据保护和熔断保护，避免高频热点请求或超阈值请求继续进入脚本执行。

## What Changes

- 为 Magic-API `api` 类型 HTTP 请求接入 Sentinel 风格保护能力，读取接口 options 与 `magic.api.<path>.options.*` 外部配置。
- 实现热点数据保护：按 `hot_data_key` 解析出的请求上下文 key 注册/进入 Sentinel 参数热点规则，触发阻断时短路返回统一失败响应。
- 实现熔断降级保护：按接口资源维度注册/进入 Sentinel FlowRule，触发阻断时短路返回统一失败响应。
- 将 `hot_data_grade` 的 `QPS` / `THREAD` 映射为 Sentinel grade，并读取 `hot_data_count`、`hot_data_duration`、`fallback_count` 参与规则。
- 新增必要的 Sentinel 依赖与自动配置 Bean，保持 JDK 1.8 兼容。
- 不拆分为多个 change：热点数据和熔断降级虽然是两个用户可见开关，但它们共用同一 Magic-API 请求拦截入口、Sentinel 规则管理、配置解析和失败响应路径，拆分会造成重复基础设施和互相等待。

## Capabilities

### New Capabilities
- `magicapi-sentinel-guards`: Magic-API 接口级热点数据保护与熔断降级保护能力。

### Modified Capabilities
- 无。

## Impact

- 影响模块：`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure`。
- 主要影响代码：Magic-API 自动配置、请求拦截器、options 配置解析、Sentinel 规则注册与释放逻辑。
- 依赖影响：需要引入 Sentinel 参数热点/流控相关依赖，版本参考 Akali 使用的 Sentinel 1.8.6，并纳入现有 Maven 依赖管理方式。
- 行为边界：本次仅覆盖 Magic-API `api` 类型 HTTP 请求；不覆盖 `task`、`kafka-subscribe`、`pulsar-subscribe`、SSE/WebSocket subscribe 等非 HTTP API 脚本。
- 非目标：不修改 Magic-API core，不新增前端配置页面，不实现 Akali Java 方法级 `${methodName}Fallback` 回退调用，也不新增 fallback script/function 机制。
