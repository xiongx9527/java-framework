# Comet Design Handoff

- Change: add-magicapi-repeat-submit-lock
- Phase: design
- Mode: compact
- Context hash: 0e288614f6960dabbfc74671b3467f924dff551ab4d9aec9df46ba0738e4dc66

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-magicapi-repeat-submit-lock/proposal.md

- Source: openspec/changes/add-magicapi-repeat-submit-lock/proposal.md
- Lines: 1-27
- SHA256: eade7a33c353438839e4daf90ad305cb1f1922714102369c82a4950063f6ea8e

```md
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

```

## openspec/changes/add-magicapi-repeat-submit-lock/design.md

- Source: openspec/changes/add-magicapi-repeat-submit-lock/design.md
- Lines: 1-78
- SHA256: c47993d4f48b4850024bf28bbba05c821240e426c37b14d918f2f30729533c84

```md
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

```

## openspec/changes/add-magicapi-repeat-submit-lock/tasks.md

- Source: openspec/changes/add-magicapi-repeat-submit-lock/tasks.md
- Lines: 1-32
- SHA256: 4682799cb7e6a5a1ff291f7728774493b08f5f5d500574f01ea2f9166ff3df1f

```md
## 1. 测试准备

- [ ] 1.1 为重复提交拦截器设计可单测的锁实现 stub，覆盖获取锁成功、获取锁失败、释放锁场景
- [ ] 1.2 补充配置读取测试，验证接口 options 与 `magic.api.<path>.options.*` 覆盖顺序
- [ ] 1.3 补充 key 生成测试，覆盖 SpEL、空 key fallback、不同请求 key 隔离、业务后缀 Bean 和默认后缀实现

## 2. 核心实现

- [ ] 2.1 新增专用 Magic-API 重复提交 `RequestInterceptor`，在 `preHandle` 中按开关决定是否启用校验
- [ ] 2.2 使用现有 `Lock` Bean 调用 `lock(key, expire, acquireTimeout)`，获取成功后把锁信息绑定到本次请求上下文
- [ ] 2.3 获取锁失败或超时时使用 Magic-API `ResultProvider` 返回 `avoid_repeat_submit_msg`，并阻止接口脚本执行
- [ ] 2.4 在 `afterCompletion` 中仅释放本请求成功获取的锁，并记录释放失败日志

## 3. 配置与 Key 生成

- [ ] 3.1 复用 `Options.AVOID_REPEAT_SUBMIT_*` 与 `ParamConfig` 读取接口级配置
- [ ] 3.2 实现 `avoid_repeat_submit_key` SpEL 解析，暴露 request、headers、params、pathVariables、body、api、requestEntity 等必要上下文
- [ ] 3.3 实现空 key fallback，基于接口路径、HTTP 方法、请求参数和 request body 生成稳定摘要
- [ ] 3.4 定义 `RepeatSubmitKeySuffixProvider` 接口和 `DefaultRepeatSubmitKeySuffixProvider` 默认实现，并实现 `avoid_repeat_submit_key_subfix_spring_bean` 按 beanName 选择业务实现或回退默认实现
- [ ] 3.5 将 `avoid_repeat_submit_timeout` 按毫秒解释，并修正代码注释或文档中的单位说明

## 4. 自动配置接入

- [ ] 4.1 在 `MagicAPIBaseAutoConfiguration` 中注册重复提交拦截器 Bean，并保持与现有 `ParamProvider`、`ResultProvider` 协作
- [ ] 4.2 确认存在 `LockTemplate` 时使用分布式 `LockModule`，不存在时使用 `LocalLockModule` 兜底
- [ ] 4.3 确认控制台 `optionList` 继续暴露已有重复提交配置项且键名不变

## 5. 验证

- [ ] 5.1 运行新增单元测试，确认同 key 重复请求短路、不同 key 放行、异常后释放锁
- [ ] 5.2 编译 `fw-plus-boot-autoconfigure` 指定模块，确保 JDK 1.8 与 Magic-API 2.2.2 兼容
- [ ] 5.3 对照 `openspec/changes/add-magicapi-repeat-submit-lock/specs/magicapi-repeat-submit-lock/spec.md` 验证所有场景均有实现或测试证据

```

## openspec/changes/add-magicapi-repeat-submit-lock/specs/magicapi-repeat-submit-lock/spec.md

- Source: openspec/changes/add-magicapi-repeat-submit-lock/specs/magicapi-repeat-submit-lock/spec.md
- Lines: 1-76
- SHA256: f41e8ed51fa5290745ee6fdb708a5fa429c89ffefe3ce288fb0cd63e6e908f04

```md
## ADDED Requirements

### Requirement: Repeat submit validation can be configured per Magic-API interface
系统 SHALL 支持通过 Magic-API 接口 options 和 `magic.api.<path>.options.*` 配置每个 `api` 类型接口的重复提交校验行为。

#### Scenario: Repeat submit validation is disabled
- **WHEN** 某个 Magic-API 接口的 `avoid_repeat_submit_enable` 解析结果为 `false`
- **THEN** 系统 MUST 不尝试获取重复提交锁
- **AND** 系统 MUST 按原有流程执行该接口脚本

#### Scenario: Repeat submit validation is enabled
- **WHEN** 某个 Magic-API 接口的 `avoid_repeat_submit_enable` 解析结果为 `true`
- **THEN** 系统 MUST 在接口脚本执行前执行重复提交锁校验

#### Scenario: External config overrides interface option
- **WHEN** 接口 option 和 `magic.api.<path>.options.avoid_repeat_submit_enable` 同时存在
- **THEN** 系统 MUST 使用 `magic.api.<path>.options.avoid_repeat_submit_enable` 的配置值参与本次请求判断

### Requirement: Repeat submit key is generated from request context
系统 SHALL 为每次受保护的 Magic-API 请求生成稳定 repeat key，并允许通过配置表达式动态生成 key 片段。

#### Scenario: SpEL key expression uses request data
- **WHEN** `avoid_repeat_submit_key` 配置为可解析的 SpEL 表达式并引用请求 header、参数、path variable、body 或接口信息
- **THEN** 系统 MUST 使用表达式结果参与生成本次请求的重复提交锁 key

#### Scenario: Different keys do not block each other
- **WHEN** 两个并发请求解析出的重复提交锁 key 不相同
- **THEN** 系统 MUST 允许两个请求分别获取锁并进入各自接口脚本执行

#### Scenario: Empty key falls back to request fingerprint
- **WHEN** `avoid_repeat_submit_key` 未配置或解析结果为空
- **THEN** 系统 MUST 基于接口路径、HTTP 方法、请求参数和请求体生成稳定 fallback key

#### Scenario: Configured suffix bean contributes to key
- **WHEN** `avoid_repeat_submit_key_subfix_spring_bean` 指定的 Spring Bean 存在且实现框架定义的重复提交 key 后缀接口
- **THEN** 系统 MUST 调用该 Spring Bean 获取后缀
- **AND** 系统 MUST 将非空后缀纳入最终重复提交锁 key

#### Scenario: Default suffix provider is used when spring bean is unavailable
- **WHEN** `avoid_repeat_submit_key_subfix_spring_bean` 未配置、指定 Bean 不存在或指定 Bean 未实现框架定义的重复提交 key 后缀接口
- **THEN** 系统 MUST 使用框架默认后缀实现 `DefaultRepeatSubmitKeySuffixProvider` 生成后缀
- **AND** 系统 MUST NOT 因后缀 Bean 不可用而中断接口请求

### Requirement: Magic lock prevents duplicate script execution
系统 SHALL 使用 Magic `lock` 模块能力在接口脚本执行期间保护同一个 repeat key，阻止重复请求进入脚本。

#### Scenario: First request obtains lock and executes script
- **WHEN** 重复提交校验开启且当前 repeat key 未被占用
- **THEN** 系统 MUST 使用 Magic `lock` 模块获取该 key 的锁
- **AND** 系统 MUST 继续执行接口脚本

#### Scenario: Duplicate request fails to obtain lock
- **WHEN** 重复提交校验开启且另一个请求已持有相同 repeat key 的锁
- **THEN** 系统 MUST NOT 执行重复请求对应的接口脚本
- **AND** 系统 MUST 返回 `avoid_repeat_submit_msg` 配置的提示信息

#### Scenario: Acquire timeout is honored
- **WHEN** `avoid_repeat_submit_timeout` 配置为有效毫秒值
- **THEN** 系统 MUST 将该值作为获取锁等待超时时间传递给 Magic `lock` 模块

### Requirement: Repeat submit lock is released after request completion
系统 SHALL 在受保护请求完成时释放本次获取的重复提交锁。

#### Scenario: Successful script releases lock
- **WHEN** 受保护接口脚本正常执行完成
- **THEN** 系统 MUST 在请求完成后释放本次获取的锁
- **AND** 后续相同 repeat key 的请求 MUST 能再次进入接口脚本执行

#### Scenario: Failed script releases lock
- **WHEN** 受保护接口脚本执行过程中抛出异常
- **THEN** 系统 MUST 在异常处理完成后释放本次获取的锁
- **AND** 后续相同 repeat key 的请求 MUST 能再次进入接口脚本执行

#### Scenario: Duplicate request does not release another request lock
- **WHEN** 重复请求未成功获取相同 repeat key 的锁
- **THEN** 系统 MUST NOT 释放正在由其他请求持有的锁

```
