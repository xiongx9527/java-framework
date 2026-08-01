## Context

当前 `fw-plus-boot-autoconfigure` 已经在 `Options` 中定义了避免重复提交相关配置，并通过 `MagicAPIController.optionList` 暴露给 Magic-API 控制台。请求执行侧目前只有 `ParamProvider` 作为 `RequestInterceptor` Bean 注册进 Magic-API 生命周期，用于打印请求和响应参数。

Magic-API 2.2.2 的 `RequestHandler` 会在脚本执行前调用所有 `RequestInterceptor.preHandle`，当返回非 `null` 时直接短路后续脚本执行；接口结束时会调用 `afterCompletion`。这提供了天然的重复提交拦截点。

项目中已经存在 `@MagicModule("lock")` 的 `Lock` 能力：有 `LockModule` 包装 `com.baomidou.lock.LockTemplate`，也有无外部依赖时的 `LocalLockModule` 兜底。两者都实现 `lock(key, expire, acquireTimeout)` 与 `release(lockInfo)`，适合在请求生命周期中复用。

## Goals / Non-Goals

**Goals:**

- 在 Magic-API `api` 请求进入脚本执行前完成重复提交校验。
- 使用当前 Spring 容器中已经注册的 Magic `lock` 模块实现，不新增锁依赖。
- 复用现有 `Options.AVOID_REPEAT_SUBMIT_*` 配置键，支持接口 options 和 `magic.api.<path>.options.*` 覆盖。
- 在锁获取失败、锁获取超时或重复提交时返回配置提示，并阻止脚本执行。
- 无论脚本成功、失败、校验异常或后置处理异常，已经获取的锁都必须释放。

**Non-Goals:**

- 不实现接口响应结果缓存或幂等结果复用，只阻止同一 repeat key 的重复执行。
- 不改变 Magic-API 控制台资源管理接口、插件管理接口或静态资源行为。
- 不覆盖 task、kafka-subscribe、pulsar-subscribe、ws-subscribe、sse-subscribe 等非普通 HTTP API 触发链路。
- 不调整 Magic-API 2.2.2 依赖版本，也不改变现有配置键名。

## Decisions

### 决策 1：使用独立 RequestInterceptor 承载重复提交逻辑

新增专用拦截器，例如 `RepeatSubmitRequestInterceptor`，由 `MagicAPIBaseAutoConfiguration` 注册为 Bean。拦截器注入 `Lock` 与 Magic-API `ResultProvider`，在 `preHandle` 中读取配置、生成 key、尝试获取锁；在 `afterCompletion` 中释放锁。

备选方案是直接增强 `ParamProvider`。该方案改动更少，但会把参数日志和并发控制混在一起，后续测试和维护边界不清晰。独立拦截器更符合单一职责，也能通过 Spring Bean 顺序或显式注册控制执行位置。

### 决策 2：通过现有配置读取链路解析接口级配置

重复提交配置读取沿用 `ParamConfig.get(info, optionKey)`，保持优先级为接口 options 或分组 options，再由 `magic.api.<path>.options.<key>` 覆盖默认值。默认值仍来自 `Options`。

这样不会改变控制台已保存的 options 结构，也与现有请求/响应参数日志配置保持一致。

### 决策 3：repeat key 由 SpEL 或字面值生成，并追加稳定上下文

`avoid_repeat_submit_key` 支持 SpEL 表达式。表达式上下文至少提供 `request`、`response`、`headers`、`params`、`pathVariables`、`body`、`api`、`requestEntity` 和 MagicScriptContext 中已放入的变量。表达式解析失败时应按错误处理返回可诊断异常，不静默降级为同一个全局 key。

为降低默认配置导致全局串行化的风险，最终锁 key 需要包含固定前缀、接口标识和表达式结果；当配置的 key 为空时，退化为基于接口路径、HTTP 方法、请求参数和 request body 的稳定摘要。

`avoid_repeat_submit_key_subfix_spring_bean` 指定的 Spring Bean 作为 key 后缀提供者参与 key 生成。框架需要定义明确接口 `RepeatSubmitKeySuffixProvider`，业务方的 Spring Bean 必须实现该接口；配置值只作为 bean name 查找，不猜测方法名。同时框架必须提供默认实现 `DefaultRepeatSubmitKeySuffixProvider implements RepeatSubmitKeySuffixProvider`。未配置 bean、bean 不存在或 bean 未实现接口时，系统必须回退到该默认实现。默认实现以通用请求上下文生成后缀，例如优先使用认证用户或 session id，其次使用客户端地址，仍不可用时返回空后缀。

### 决策 4：锁获取超时时间按毫秒执行，并兼容现有文案

现有 `Options.AVOID_REPEAT_SUBMIT_TIMEOUT` 注释写“秒”，默认值是 `1000`；Magic lock 的 `acquireTimeout` 参数实际按毫秒传入。为保持现有默认值语义合理，本变更按毫秒解释该配置，并在代码注释或文档中修正说明，避免 `1000` 被误读成 1000 秒。

`expire` 参数传给 lock 时可使用同一个配置或固定为 0。`LocalLockModule` 当前不支持 expire，会忽略并告警；分布式 `LockModule` 支持 expire。实现应确保即使 expire 不生效，也依赖 `afterCompletion` 释放锁。

### 决策 5：重复提交返回统一 Magic-API 响应对象

`preHandle` 获取锁失败时不抛原始 lock 异常给脚本层，而是使用 Magic-API `ResultProvider.buildResult` 构造响应，消息使用 `avoid_repeat_submit_msg`。响应码应复用 Magic-API 非成功业务码或当前异常码策略，且 `success` 标记需要符合现有 `JsonBean` 语义。

这样调用方看到的响应结构与其他 Magic-API 结果一致，脚本不会被执行。

## Risks / Trade-offs

- [Risk] 默认 key 如果过于粗，会导致不同请求互相阻塞。→ Mitigation: 最终 key 必须包含接口方法和路径，并允许用户通过 SpEL 精确加入用户、业务 id、header 或 body 字段。
- [Risk] SpEL 表达式访问上下文过宽可能带来安全风险。→ Mitigation: 只暴露必要变量，不开放任意 BeanResolver；后缀扩展通过显式配置的 bean name 加接口契约调用，缺失时走 `DefaultRepeatSubmitKeySuffixProvider` 默认实现。
- [Risk] 本地锁只能保护单实例。→ Mitigation: 保持现有 `LockModuleConfiguration` 策略；存在 `LockTemplate` 时自动使用分布式锁，否则继续使用本地锁兜底，并在文档中说明差异。
- [Risk] 锁释放失败可能导致后续请求被长期阻塞。→ Mitigation: `afterCompletion` 必须捕获并记录 release 异常；分布式锁场景依赖 expire 兜底，本地锁依赖当前线程持有判断。
- [Risk] 拦截器顺序与日志、认证等逻辑冲突。→ Mitigation: 重复提交拦截器只处理脚本执行前的请求，保持与 Magic-API 已有认证机制独立；必要时通过 `@Order` 固定在日志之后、脚本之前。

## Migration Plan

1. 新增并注册重复提交拦截器，默认读取已有 options。
2. 补充单元测试或轻量集成测试，覆盖开关关闭、同 key 冲突、不同 key 放行、异常释放锁。
3. 编译 `fw-plus-boot-autoconfigure`，确保 Magic-API 2.2.2 与 JDK 1.8 兼容。

回滚方式为移除新增拦截器 Bean 或关闭 `avoid_repeat_submit_enable`。

## Open Questions

- 重复提交响应码应复用 Magic-API invalid code、exception code，还是引入框架已有业务码，需要实现阶段查看 `JsonBean` 使用约定后决定。
