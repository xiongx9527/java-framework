## Why

Magic-API 接口已经暴露了避免重复提交相关选项，但当前仅提供配置项，实际请求执行链路没有在脚本执行前进行重复提交校验。并发或短时间重复请求会多次进入同一 Magic-API 脚本，可能导致重复写入、重复发消息或重复调用外部系统。

本变更需要把这些现有选项接入 Magic-API 请求生命周期，并复用项目已有的 `lock` Magic 模块能力完成互斥控制。

## What Changes

- 为 Magic-API 接口请求增加重复提交校验，请求进入脚本执行前按配置生成 repeat key 并尝试获取锁。
- 复用现有 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.lock.Lock` 能力，优先使用当前容器中的 `lock` 模块实现，不新增外部锁依赖。
- 支持通过 Magic-API 接口 options 与 `magic.api.<path>.options.*` 配置控制开关、获取锁超时时间、repeat key、key 后缀 Spring Bean 和重复提交提示信息。
- 获取锁失败或超时时不执行接口脚本，并按现有 Magic-API 响应包装返回配置的提示信息。
- 请求完成或异常退出后释放本次获取的锁，避免锁泄漏。

## Capabilities

### New Capabilities
- `magicapi-repeat-submit-lock`: Magic-API 接口请求基于配置生成 repeat key，并使用 Magic 内置 lock 模块阻止同一 key 的重复提交进入脚本执行。

### Modified Capabilities
- 无。

## Impact

- 主要影响 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure` 中 Magic-API 自动配置、请求拦截器和 lock 模块相关代码。
- 影响 Magic-API `api` 类型资源的运行时请求，不影响控制台资源管理接口、插件管理接口、静态资源加载或非 HTTP 触发的订阅/任务脚本。
- 不调整 Magic-API 依赖版本，不改变现有 `Options` 的配置键名，保持已存在接口选项兼容。
