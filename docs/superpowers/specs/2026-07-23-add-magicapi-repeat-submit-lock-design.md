---
comet_change: add-magicapi-repeat-submit-lock
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-23-add-magicapi-repeat-submit-lock
status: final
---

# Magic-API 接口重复提交锁技术设计

## 背景

`fw-plus-boot-autoconfigure` 已经在 `Options` 中定义了重复提交相关配置，包括开关、锁等待时间、key 表达式、key 后缀 Spring Bean 和提示信息。这些配置目前仅暴露给 Magic-API 控制台，普通 `api` 接口执行链路还没有使用它们。

Magic-API 2.2.2 的 `RequestHandler` 会在接口脚本执行前调用所有 `RequestInterceptor.preHandle`，返回非 `null` 时短路脚本执行；请求结束时调用 `afterCompletion`。项目中也已经有 `@MagicModule("lock")` 的 `Lock` 接口，并通过 `LockModuleConfiguration` 在存在 `LockTemplate` 时使用分布式 `LockModule`，否则使用本地 `LocalLockModule`。因此，本功能应作为 Magic-API 请求拦截器能力接入，而不是修改 Magic-API core 或把逻辑写进脚本。

## 目标

- 对 Magic-API `api` 类型 HTTP 请求增加重复提交校验。
- 复用现有 `Options.AVOID_REPEAT_SUBMIT_*` 配置键和 `ParamConfig` 配置读取链路。
- 使用现有 `Lock` Bean 获取和释放锁，不新增外部锁依赖。
- 支持 SpEL 或字面值 repeat key，并支持 Spring Bean 后缀扩展。
- 框架提供默认 key 后缀实现，业务方可通过 Spring Bean 替换。
- 重复提交时返回 Magic-API 统一响应，不执行接口脚本。
- 请求成功、失败或异常退出后释放当前请求持有的锁。

## 非目标

- 不实现幂等结果缓存或重复请求返回第一次执行结果。
- 不改变 Magic-API 控制台、资源管理接口、插件管理接口或静态资源行为。
- 不覆盖 task、kafka-subscribe、pulsar-subscribe、ws-subscribe、sse-subscribe 等非普通 HTTP API 触发链路。
- 不调整 Magic-API 2.2.2 依赖版本，不修改现有配置键名。

## 方案概览

采用独立拦截器方案：

```text
RequestHandler.invoke
  ├─ 参数、path、header、body 校验
  ├─ RequestContext.setRequestEntity
  ├─ RepeatSubmitRequestInterceptor.preHandle
  │    ├─ 读取 avoid_repeat_submit_enable
  │    ├─ 构造 repeat key
  │    ├─ Lock.lock(key, expire, acquireTimeout)
  │    ├─ 成功：保存 lockInfo 到本次 request context，返回 null 放行
  │    └─ 失败：返回失败态 JsonBean，脚本不执行
  ├─ ScriptManager.executeScript
  └─ RepeatSubmitRequestInterceptor.afterCompletion
       └─ 仅释放本请求成功获取的 lockInfo
```

新增类建议放在 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.provider` 或其子包中，保持与现有 `ParamProvider`、`ParamConfig`、`ResultProvider` 同域。

## 组件设计

### RepeatSubmitRequestInterceptor

实现 `org.ssssssss.magicapi.core.interceptor.RequestInterceptor`。

依赖：

- `Lock lock`
- `org.ssssssss.magicapi.core.interceptor.ResultProvider resultProvider`
- `RepeatSubmitKeyResolver keyResolver`

`preHandle` 逻辑：

1. 读取 `avoid_repeat_submit_enable`，关闭时返回 `null`。
2. 读取 `avoid_repeat_submit_timeout`，按毫秒解析；无效值使用默认值。
3. 调用 `RepeatSubmitKeyResolver` 生成最终锁 key。
4. 调用 `lock.lock(key, timeout, timeout)` 或按实现阶段确认的 expire/acquireTimeout 参数组合获取锁。
5. 获取成功后把 `lockInfo` 绑定到本次请求上下文，返回 `null` 放行。
6. 获取失败或超时时，返回 `avoid_repeat_submit_msg` 对应的 Magic-API 失败响应，不执行脚本。

`afterCompletion` 逻辑：

1. 从本次请求上下文读取 `lockInfo`。
2. 只有当前请求成功获取过锁时才调用 `lock.release(lockInfo)`。
3. 捕获释放异常并记录日志，不能覆盖原接口返回值或异常处理。

上下文绑定优先使用 `MagicScriptContext` 中的内部 key，避免静态 Map 带来的清理风险。内部 key 使用私有常量，避免与用户脚本变量冲突。

### RepeatSubmitKeyResolver

负责把配置和请求上下文解析为最终锁 key。

输入：`ApiInfo`、`RequestEntity`、`MagicScriptContext`。

读取配置：

- `avoid_repeat_submit_key`
- `avoid_repeat_submit_key_subfix_spring_bean`

生成规则：

1. 固定前缀：例如 `magicapi:repeat:submit`。
2. 接口维度：优先 `ApiInfo.getId()`，并加入 HTTP method 和 path 兜底。
3. 主 key 片段：
   - `#{...}` 且能作为 SpEL 解析时，使用表达式结果。
   - 默认值 `#{magicapi:repeat:submit}` 这类历史字面值需要兼容为普通文本，不应导致默认配置报错。
   - 空值时使用请求指纹 fallback。
4. 后缀片段：调用 `RepeatSubmitKeySuffixProvider`。
5. 对复杂对象或长字符串做稳定序列化和摘要，避免锁 key 过长。

SpEL 上下文只暴露必要变量：`request`、`response`、`headers`、`params`、`pathVariables`、`body`、`api`、`requestEntity`。不开放任意 BeanResolver，降低表达式访问面。

### RepeatSubmitKeySuffixProvider

定义框架扩展接口：

```java
public interface RepeatSubmitKeySuffixProvider {
    String getSuffix(RequestEntity requestEntity);
}
```

业务方可以实现该接口并注册为 Spring Bean，然后在 `avoid_repeat_submit_key_subfix_spring_bean` 中配置 beanName。框架只按 beanName 查找并校验接口类型，不猜测方法名。

### DefaultRepeatSubmitKeySuffixProvider

框架必须提供默认实现：

```java
public class DefaultRepeatSubmitKeySuffixProvider implements RepeatSubmitKeySuffixProvider {
    @Override
    public String getSuffix(RequestEntity requestEntity) {
        // 按通用上下文生成后缀
    }
}
```

建议默认顺序：

1. 从当前请求上下文或认证上下文获取登录用户标识，如果可用则返回用户标识。
2. 从 session 获取 id，如果可用则返回 session id。
3. 使用客户端地址或请求头中的常见真实 IP 值。
4. 仍不可用时返回空字符串。

选择逻辑：配置了 beanName 且对应 Bean 存在并实现 `RepeatSubmitKeySuffixProvider` 时使用业务实现；否则使用 `DefaultRepeatSubmitKeySuffixProvider`。这保证没有业务 Bean 时仍有通用隔离能力。

## 响应设计

重复提交是预期内的业务拒绝，不应抛出异常让 Magic-API 记录系统错误日志。推荐使用现有 `ResultProvider.buildResult(requestEntity, Constants.RESPONSE_CODE_INVALID, msg, null)` 构造响应，并在返回对象是 `JsonBean` 时设置 `success=false`。

`msg` 来自 `avoid_repeat_submit_msg`，空值时使用 `Options.AVOID_REPEAT_SUBMIT_MSG` 默认值。

## 配置语义

- `avoid_repeat_submit_enable`：开启/关闭重复提交校验。
- `avoid_repeat_submit_timeout`：锁获取等待时间，按毫秒解释；需要同步修正注释中的“秒”描述。
- `avoid_repeat_submit_key`：SpEL 或字面值主 key 片段。
- `avoid_repeat_submit_key_subfix_spring_bean`：业务后缀 Provider 的 Spring beanName；不可用时走 `DefaultRepeatSubmitKeySuffixProvider`。
- `avoid_repeat_submit_msg`：重复提交提示信息。

配置读取沿用 `ParamConfig.get(info, key)`，保持与现有参数日志 options 一致的覆盖链路。

## 错误处理

- 开关配置非法：按默认值处理并记录 debug/warn。
- timeout 配置非法：按默认毫秒值处理并记录 debug/warn。
- SpEL 表达式非法：返回可诊断的 Magic-API 失败响应，不静默降级为全局 key。
- 后缀 beanName 不存在或类型不匹配：使用默认后缀实现，不中断请求。
- 获取锁超时或失败：返回重复提交提示，不执行脚本。
- release 失败：记录日志，不覆盖原始响应。

## 测试策略

### Key Resolver 单测

- 默认 key 字面兼容，不因 `#{magicapi:repeat:submit}` 报错。
- 合法 SpEL 能读取 header、params、pathVariables、body 和 api 信息。
- 空 key 使用 method、path、params、body 生成稳定 fallback。
- 不同请求上下文生成不同 key，同一上下文生成稳定 key。
- 配置业务后缀 Bean 时调用业务实现。
- 未配置、Bean 不存在或类型不匹配时调用 `DefaultRepeatSubmitKeySuffixProvider`。

### 拦截器单测

- 开关关闭时不调用 lock，返回 `null` 放行。
- 首个请求获取锁成功，保存 lockInfo，脚本可执行。
- 相同 key 重复请求获取锁失败，返回失败态 JsonBean，脚本不执行。
- 不同 key 请求互不阻塞。
- 脚本成功后释放锁。
- 脚本异常后释放锁。
- 未成功获取锁的重复请求不释放其他请求的 lockInfo。

### 自动配置和构建验证

- `MagicAPIBaseAutoConfiguration` 注册 `RepeatSubmitRequestInterceptor`。
- 存在 `LockTemplate` 时 `Lock` Bean 使用分布式 `LockModule`；不存在时使用 `LocalLockModule`。
- `MagicAPIController.optionList` 继续暴露已有重复提交配置项，键名不变。
- 使用 `fw-plus-boot` 下 Maven Wrapper 编译目标模块，保持 JDK 1.8 兼容。

## 实施顺序

1. 新增 `RepeatSubmitKeySuffixProvider`、`DefaultRepeatSubmitKeySuffixProvider` 和 `RepeatSubmitKeyResolver` 测试。
2. 实现 key 解析、默认后缀和业务后缀选择。
3. 新增 `RepeatSubmitRequestInterceptor` 测试。
4. 实现拦截器拿锁、短路响应和释放锁。
5. 在 `MagicAPIBaseAutoConfiguration` 注册 Bean。
6. 修正 timeout 单位注释，并运行目标测试和编译。

## 设计结论

该方案把重复提交校验放在 Magic-API 已有请求拦截器扩展点上，复用现有 lock 模块和 options 体系，避免改动 Magic-API core。通过明确的 `RepeatSubmitKeySuffixProvider` 接口和 `DefaultRepeatSubmitKeySuffixProvider` 默认实现，既支持业务自定义 key 后缀，也保证没有业务 Bean 时仍能获得通用隔离能力。
