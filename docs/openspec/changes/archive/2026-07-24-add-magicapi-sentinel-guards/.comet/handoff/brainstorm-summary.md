# Brainstorm Summary

- Change: add-magicapi-sentinel-guards
- Date: 2026-07-23

## 确认的技术方案

待确认：采用独立 Magic-API `RequestInterceptor` 接入 Sentinel guard。拦截器在 `preHandle` 中按接口配置判断热点数据和熔断降级是否启用，按稳定接口资源名进入 Sentinel；在 `afterCompletion` 中释放本次成功进入的 `Entry`。

待确认：将现有 `RepeatSubmitKeyResolver` 中的 SpEL 上下文、接口指纹、请求 fallback fingerprint 能力抽取为通用请求 key 解析组件，再由 repeat submit 和 hot data 分别提供前缀、配置 key 与 fallback 策略，避免重复实现。

待确认：新增框架自有 Sentinel rule manager，维护 Magic-API guard 资源规则缓存。注册时合并现有 Sentinel 规则并只替换本框架资源，避免 `FlowRuleManager.loadRules(...)` 或 `ParamFlowRuleManager.loadRules(...)` 覆盖其他接口和外部 Sentinel 规则。

待确认：`fallback` 在本 change 中定义为请求准入保护，触发 Sentinel 阻断后返回 Magic-API 统一失败响应，不调用 Java `${methodName}Fallback`，也不新增 fallback script/function。

## 关键取舍与风险

- 推荐方案：独立拦截器 + 通用 key resolver + 合并式 Sentinel rule manager。优点是贴合现有 Magic-API 自动配置结构，复用 repeat submit 已验证的上下文能力，影响面可控；代价是需要处理 Sentinel 全局规则合并和并发更新。
- 备选方案 A：把热点数据和 fallback 拆成两个拦截器。优点是职责更细；缺点是 entry 顺序、短路优先级和上下文释放更容易分散，测试组合更多。
- 备选方案 B：照搬 Akali 的单规则 `loadRules(singleton)`。实现最少；但会覆盖多接口或外部 Sentinel 规则，不适合当前框架场景。
- 风险：Sentinel 是 JVM 全局状态，需要单元测试确保规则合并不会删除外部规则。
- 风险：多个 Magic-API `RequestInterceptor` 同时短路时顺序会影响最终响应，需要在自动配置测试中固定核心 Bean 存在和主路径行为。

## 测试策略

- key resolver 测试：SpEL、字面值、空值 fallback fingerprint、外部配置优先。
- rule manager 测试：新增/更新同一资源规则、保留其他 Magic-API 资源规则、保留外部 Sentinel 规则。
- interceptor 测试：`count<=0` 放行、热点 key 隔离、热点阻断不执行脚本、fallback 阻断不执行脚本、正常/异常完成释放 `Entry`。
- auto configuration 测试：默认 Bean 注册、用户自定义 Bean 覆盖、Sentinel 依赖下可启动。
- Maven 验证：运行 autoconfigure 相关编译/测试命令。

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖核心验收场景，不需要回写补丁。
