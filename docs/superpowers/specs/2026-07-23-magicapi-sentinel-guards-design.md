---
comet_change: add-magicapi-sentinel-guards
role: technical-design
canonical_spec: openspec
---

# Magic-API Sentinel Guards 技术设计

## 背景

当前 Magic-API `Options` 已提供 `HOT_DATA_*` 和 `FALLBACK_*` 配置项，但请求执行链路没有对应运行时保护。Akali 参考项目使用 Sentinel `ParamFlowRule` 做热点参数保护，使用 `FlowRule` 做 fallback 阈值保护；本项目需要把这个思路映射到 Magic-API HTTP `api` 请求，而不是照搬 Java 方法代理。

现有重复提交能力已经证明了几个可复用边界：`ParamConfig` 负责接口 option 与外部配置优先级，`RequestInterceptor` 可在脚本执行前短路并在完成后清理上下文，`ResultProvider` 可构造统一失败响应，`MagicScriptContext` 可保存请求级状态。

## 目标

- 为 Magic-API HTTP `api` 接口实现热点数据保护和熔断降级保护。
- 支持接口 options 与 `magic.api.<path>.options.*` 外部配置，外部配置优先。
- 使用 Sentinel `ParamFlowRule` 处理热点 key，使用 Sentinel `FlowRule` 处理接口资源阈值。
- 被 Sentinel 阻断时不执行 Magic 脚本，直接返回 Magic-API 统一失败响应。
- 正常执行、异常执行和阻断路径都不泄漏 Sentinel `Entry`。
- 保持 JDK 1.8 兼容，遵循当前 autoconfigure 模块风格。

## 非目标

- 不实现 Akali 的 Java Bean 方法代理。
- 不查找或调用 `${methodName}Fallback` Java 方法。
- 不新增 fallback script/function 配置机制。
- 不覆盖 task、subscribe、SSE/WebSocket subscribe 等非 HTTP API 入口。
- 不改 Magic-API core，不新增前端配置项，不改变现有 option key。

## 推荐架构

采用独立 Magic-API `RequestInterceptor` + 通用请求 key resolver + 合并式 Sentinel rule manager。

请求流：

```text
Magic-API HTTP request
        |
        v
MagicApiSentinelGuardRequestInterceptor.preHandle
        |
        +-- 读取 ParamConfig: hot_data_* / fallback_*
        |
        +-- count <= 0 或 enable=false: 放行
        |
        +-- rule manager 合并注册 FlowRule / ParamFlowRule
        |
        +-- fallback: SphU.entry(resource)
        |
        +-- hot data: SphU.entry(resource, EntryType.IN, 1, hotKey)
        |
        +-- BlockException: 返回 ResultProvider 失败响应，不执行脚本
        |
        v
Magic script execution
        |
        v
afterCompletion: 释放本请求成功进入的 Sentinel Entry
```

## 组件设计

### 1. 通用请求 key resolver

现有 `RepeatSubmitKeyResolver` 已包含 SpEL 上下文、接口标识、fallback fingerprint 和长片段摘要能力。实现时建议抽出通用组件，例如 `MagicApiRequestKeyResolver` 或内部 helper，让 repeat submit 和 hot data 共享：

- SpEL 上下文变量：`request`、`response`、`headers`、`params`、`pathVariables`、`body`、`api`、`requestEntity`。
- 字面值配置：非 SpEL 直接作为 key 主体。
- 空值 fallback：按接口 id/path/method、请求参数、path variables、body 生成稳定 SHA-256 指纹。
- 长片段归一化：超过固定长度后摘要，避免 Sentinel 参数维度过长。

热点数据 key 使用 `Options.HOT_DATA_KEY`，不需要 repeat submit 的 suffix bean 语义。repeat submit 可继续保留 suffix provider 作为自身组合逻辑。

### 2. Sentinel 资源命名

资源名应稳定且可读，建议格式：

```text
magicapi:{HTTP_METHOD}:{PATH_OR_ID}
```

优先使用 `ApiInfo.getId()` 可避免 path 变更引起资源迁移；如果 id 为空，则用 method + path。实现需要统一封装，所有 rule manager 和 interceptor 使用同一方法生成资源名。

### 3. 配置解析

配置来自 `ParamConfig.get(apiInfo, optionKey)`，继承现有优先级：

```text
magic.api.<path>.options.<key> > ApiInfo options
```

解析规则：

- `hot_data_enable=true && hot_data_count>0` 才启用热点保护。
- `fallback_enable=true && fallback_count>0` 才启用 fallback 保护。
- `hot_data_grade` 支持 `QPS` 和 `THREAD`，分别映射 Sentinel `RuleConstant.FLOW_GRADE_QPS` 与 `RuleConstant.FLOW_GRADE_THREAD`。
- `hot_data_duration` 作为 `ParamFlowRule.durationInSec`，非法值使用默认值。
- count 小于等于 0 视为未形成有效规则，放行请求，避免当前默认值 `0` 导致接口全部阻断。

### 4. 合并式 Sentinel rule manager

Akali 的 `FlowRuleManager.loadRules(ListUtil.toList(rule))` 在单方法 demo 中可行，但 Sentinel 的 loadRules 是全量替换。当前框架需要合并更新：

- 维护框架自有规则缓存：按资源名保存本功能创建的 `FlowRule` 与 `ParamFlowRule`。
- 更新某个资源时，只替换该资源对应的框架自有规则。
- 加载前读取当前 Sentinel 规则列表，保留不属于本功能资源前缀或不在自有缓存中的外部规则。
- 合并外部规则 + 自有缓存后再调用 `loadRules`。
- 更新过程需要同步，避免并发请求首次注册规则时互相覆盖。

### 5. RequestInterceptor 行为

拦截器职责：

- 在 `preHandle(RequestEntity)` 中读取配置并构建 guard context。
- 先处理 fallback resource entry，再处理 hot data param entry；如果任一步阻断，释放已进入的 entry 并返回失败响应。
- 成功进入的 entry 存入 `MagicScriptContext`，key 使用私有常量，避免与脚本变量冲突。
- 在 `afterCompletion` 中按后进先出释放所有成功进入的 entry。
- 对旧版接口签名 `preHandle(ApiInfo, MagicScriptContext, MagicHttpServletRequest, MagicHttpServletResponse)` 与 `afterCompletion(...)` 保持兼容，参考 `RepeatSubmitRequestInterceptor` 构造 `RequestEntity`。

失败响应：

- 使用 `ResultProvider.buildResult(requestEntity, Constants.RESPONSE_CODE_INVALID, message, null)`。
- 如果返回 `JsonBean`，显式设置 `success=false`。
- 本 change 不新增 `hot_data_msg` / `fallback_msg` option，默认文案可固定为热点访问过于频繁或接口触发熔断降级。

## 依赖设计

在 `fw-plus-boot` 依赖管理中加入 Sentinel 版本，优先参考 Akali 的 `1.8.6`：

- `com.alibaba.csp:sentinel-core`
- `com.alibaba.csp:sentinel-parameter-flow-control`

如果 `sentinel-parameter-flow-control` 已传递包含 core，可在 autoconfigure 模块只声明必要依赖，但 BOM 中仍建议集中管理版本。

## Bean 注册

在 `MagicAPIBaseAutoConfiguration` 中注册：

- 通用请求 key resolver 或 hot data key resolver。
- Sentinel rule manager。
- `MagicApiSentinelGuardRequestInterceptor`。

Bean 使用 `@ConditionalOnMissingBean`，允许业务侧替换默认实现。拦截器依赖 `ResultProvider`、rule manager、key resolver。

## 风险与缓解

- Sentinel 规则是 JVM 全局状态：通过合并式 rule manager 和单元测试保证不删除外部规则。
- count 默认值是 0：实现必须把 `count<=0` 作为放行，否则默认开启会变成全阻断。
- SpEL 表达式错误：保持与 repeat submit 一致，返回明确失败响应，不静默退化到错误 key。
- 多拦截器短路顺序：测试覆盖 repeat submit 与 Sentinel guard 同时存在时的主要 Bean 注册和短路路径。
- 用户应用已有 Sentinel 版本：通过 BOM 管理默认版本，同时允许上层依赖管理覆盖。

## 测试策略

- key resolver 单元测试：SpEL、字面值、空值 fallback fingerprint、长 key 摘要、外部配置优先。
- 配置解析测试：`enable=false`、`count<=0`、非法 grade、非法 duration。
- rule manager 测试：新增规则、更新同资源规则、保留其他 Magic-API 资源规则、保留外部 Sentinel 规则。
- interceptor 测试：热点 key 隔离、热点阻断不执行脚本、fallback 阻断不执行脚本、正常与异常完成释放 entry、阻断请求不释放其他请求 entry。
- auto configuration 测试：默认 Bean 注册、用户自定义 Bean 覆盖、依赖存在时上下文可启动。
- Maven 验证：运行 autoconfigure 相关编译/测试命令。

## 实施顺序

1. 添加 Sentinel 依赖管理和 autoconfigure 模块依赖。
2. 抽取/新增通用请求 key resolver，并保持 repeat submit 测试通过。
3. 实现 Sentinel 资源命名、配置解析和 rule manager。
4. 实现 Magic-API Sentinel guard 拦截器。
5. 在自动配置中注册 Bean。
6. 补齐单元测试和 Maven 验证。

## Spec Patch

无。当前 OpenSpec delta spec 已覆盖核心行为、边界和验收场景。
