## Context

当前 `Options` 已包含 `hot_data_enable`、`hot_data_key`、`hot_data_grade`、`hot_data_count`、`hot_data_duration`、`fallback_enable`、`fallback_count`，但 Magic-API 请求链路中只有刚完成的重复提交拦截器实际接入了运行时校验。Akali 参考项目通过 Sentinel 的 `FlowRule` 和 `ParamFlowRule` 实现方法级 fallback 与热点参数保护；当前项目需要把这个思路映射到 Magic-API HTTP `api` 请求，而不是 Java 方法代理。

现有可复用模式包括 `ParamConfig` 的接口 options / 外部配置优先级、`RequestInterceptor` 的 `preHandle` / `afterCompletion` 生命周期、`ResultProvider.buildResult(...)` 的统一失败响应，以及 `MagicScriptContext` 存放请求级状态。

## Goals / Non-Goals

**Goals:**

- 按 Magic-API 接口维度启用热点数据保护和熔断降级保护。
- 参照 Akali 使用 Sentinel `ParamFlowRule` 处理热点 key，使用 Sentinel `FlowRule` 处理 fallback 资源阈值。
- 支持接口 options 与 `magic.api.<path>.options.*` 外部配置，外部配置优先。
- 在请求被 Sentinel 阻断时不执行 Magic 脚本，并返回 Magic-API 统一失败响应。
- 规则注册、entry 进入与释放必须适配多接口、多请求并发，并保持 JDK 1.8 兼容。

**Non-Goals:**

- 不实现 Akali 的 Java Bean 方法代理，也不调用 `${methodName}Fallback` Java 方法。
- 不新增 fallback script/function 配置机制。
- 不覆盖 task、subscribe、SSE/WebSocket 等非 HTTP API 脚本。
- 不修改 Magic-API core，不新增前端配置项，不改变现有 option key。

## Decisions

1. 使用独立 Magic-API `RequestInterceptor` 接入保护逻辑。

   备选方案是在 Magic-API core 或 controller 层扩展，但会扩大影响面。拦截器与当前重复提交实现一致，可以在脚本执行前短路，在 `afterCompletion` 释放 Sentinel `Entry`，并保持对 Magic-API HTTP `api` 请求的范围收敛。

2. Sentinel 资源维度使用接口稳定标识，热点参数值作为 `ParamFlowRule` 的第 0 个参数。

   fallback 资源建议使用 `magicapi:{method}:{path}` 这样的接口级资源名。热点规则使用同一资源名，`hot_data_key` 解析结果作为 `SphU.entry(resource, EntryType.IN, 1, hotKey)` 的参数。这样映射接近 Akali 的 `methodStr + args hash`，但更符合 Magic-API 的接口模型。

3. 配置启用条件采用 `enable=true && count>0`。

   当前 `hot_data_count` 与 `fallback_count` 默认值都是 `0`，如果直接注册 Sentinel count=0，默认开启会导致请求被全部阻断。因此运行时应把 count 小于等于 0 视为该保护未形成有效规则，记录调试日志后放行。

4. 热点 key 解析采用请求上下文表达式优先、空值 fallback 指纹兜底。

   `hot_data_key` 支持 SpEL 或字面值，表达式上下文应暴露 request、response、apiInfo、path、method、headers、parameters、pathVariables、body 等请求数据。解析为空时，使用接口路径、HTTP 方法、请求参数和请求体生成稳定 key，避免所有请求落到同一个默认字面值。

5. 规则管理采用合并更新，不照搬 Akali 的单规则覆盖写法。

   Sentinel `FlowRuleManager.loadRules(...)` 和 `ParamFlowRuleManager.loadRules(...)` 会整体替换规则列表。实现时应维护框架自身的资源规则缓存，并与当前 Sentinel 规则合并后加载；更新同一资源时替换旧规则，避免一个接口请求覆盖其他接口或外部 Sentinel 规则。

6. 阻断响应使用 Magic-API 统一失败结果。

   Magic-API 没有 Akali 的 Java fallback 方法映射。本阶段将 fallback 定义为请求准入保护：触发 Sentinel 阻断时不执行脚本，使用 `ResultProvider.buildResult(requestEntity, Constants.RESPONSE_CODE_INVALID, message, null)` 返回失败结果，并保证 `JsonBean.success=false`。默认消息可先使用固定文案，后续如需要可另开 change 增加可配置消息。

## Risks / Trade-offs

- [Risk] Sentinel 规则管理是 JVM 全局状态，加载规则可能影响应用内其他 Sentinel 使用方。→ Mitigation: 合并当前规则列表并仅按框架资源名替换自身规则，同时用单元测试覆盖不覆盖已有规则。
- [Risk] `hot_data_key` 表达式解析失败可能导致运行时异常。→ Mitigation: 表达式错误应短路返回失败结果或按设计记录明确错误，避免静默使用错误 key；具体策略在实现时与重复提交 key resolver 保持一致。
- [Risk] 多个 `RequestInterceptor` 同时短路时响应优先级受注册顺序影响。→ Mitigation: 自动配置中显式注册独立 Bean，测试覆盖重复提交与 Sentinel guard 都开启时的主要路径。
- [Risk] Sentinel 依赖引入可能与用户应用已有 Sentinel 版本冲突。→ Mitigation: 在 BOM/依赖管理中固定版本，优先参考 Akali 的 1.8.6；若已有用户侧 Sentinel，允许 Maven 依赖解析按上层覆盖。

## Migration Plan

1. 添加 Sentinel 依赖管理与 autoconfigure 模块依赖。
2. 新增 Magic-API Sentinel guard 相关 resolver、rule manager、request interceptor，并在 `MagicAPIBaseAutoConfiguration` 注册 Bean。
3. 添加单元测试覆盖配置解析、规则注册、热点阻断、fallback 阻断、entry 释放、count<=0 放行和外部配置优先级。
4. 使用 `fw-plus-boot` 模块编译/测试验证。回滚时移除新增 Bean 和 Sentinel 依赖即可恢复旧行为。

## Open Questions

- 本 change 默认不新增 `fallback_msg` / `hot_data_msg` 配置项；如果需要可配置提示信息，应在后续设计中补充新的 `Options`。
