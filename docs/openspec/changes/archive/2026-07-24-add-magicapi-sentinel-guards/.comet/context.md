# Comet Design Handoff

- Change: add-magicapi-sentinel-guards
- Phase: design
- Mode: compact
- Context hash: a89708e9e31fd668c30c4f76437dc8b4cbab77384a3876c372b12b6c6980e6ca

Generated-by: comet-handoff.sh

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## openspec/changes/add-magicapi-sentinel-guards/proposal.md

- Source: openspec/changes/add-magicapi-sentinel-guards/proposal.md
- Lines: 1-28
- SHA256: 23808e64f03336413e82626b813db79d76d7ff5fe25ee7782e2d82db11b6f054

```md
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

```

## openspec/changes/add-magicapi-sentinel-guards/design.md

- Source: openspec/changes/add-magicapi-sentinel-guards/design.md
- Lines: 1-66
- SHA256: 0764977f97af91e5e82d082b199232fcacb6c3733a42ed6990e4fba25d265683

```md
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

```

## openspec/changes/add-magicapi-sentinel-guards/tasks.md

- Source: openspec/changes/add-magicapi-sentinel-guards/tasks.md
- Lines: 1-21
- SHA256: 2143c79f31a455bf368a1d6a3ea99c98c8fee6e84e88a75ab8558810effcb7fc

```md
## 1. 依赖与配置入口

- [ ] 1.1 在 `fw-plus-boot` 依赖管理中加入 Sentinel 版本与 autoconfigure 模块所需依赖。
- [ ] 1.2 在 Magic-API 自动配置中注册 Sentinel guard 所需 Bean，并保持 `@ConditionalOnMissingBean` 可覆盖。
- [ ] 1.3 复用或抽取请求上下文 key 解析能力，支持 `hot_data_key` 的 SpEL / 字面值 / fallback 指纹。

## 2. Sentinel Guard 核心实现

- [ ] 2.1 实现 Magic-API Sentinel 资源命名、grade 解析、count/duration 解析与 `count<=0` 放行规则。
- [ ] 2.2 实现 Sentinel 规则管理器，按资源合并更新 `FlowRule` 与 `ParamFlowRule`，避免覆盖无关规则。
- [ ] 2.3 实现热点数据请求拦截逻辑，按解析出的 hot key 进入 Sentinel 参数热点规则并处理阻断响应。
- [ ] 2.4 实现 fallback 请求拦截逻辑，按接口资源进入 Sentinel FlowRule 并处理阻断响应。
- [ ] 2.5 在 `afterCompletion` 中释放本次成功进入的 Sentinel `Entry`，覆盖正常返回和异常退出。

## 3. 测试与验证

- [ ] 3.1 添加配置解析测试，覆盖接口 option、外部配置优先、grade 映射和无效 count/duration 兜底。
- [ ] 3.2 添加热点数据保护测试，覆盖不同 hot key 隔离、达到阈值阻断、阻断时不执行脚本。
- [ ] 3.3 添加 fallback 保护测试，覆盖接口资源阈值阻断、阻断时不执行脚本、不调用 Java fallback 方法。
- [ ] 3.4 添加规则合并测试，验证更新 Magic-API 规则时保留其他接口和外部 Sentinel 规则。
- [ ] 3.5 运行 `fw-plus-boot` autoconfigure 相关 Maven 编译/测试命令并记录结果。

```

## openspec/changes/add-magicapi-sentinel-guards/specs/magicapi-sentinel-guards/spec.md

- Source: openspec/changes/add-magicapi-sentinel-guards/specs/magicapi-sentinel-guards/spec.md
- Lines: 1-83
- SHA256: 020697c5f4339ed7ca7d3c6b337048bdcee86621dee9d57ecda978de922f9f38

[TRUNCATED]

```md
## ADDED Requirements

### Requirement: Magic-API Sentinel guards can be configured per interface
系统 SHALL 支持通过 Magic-API 接口 options 和 `magic.api.<path>.options.*` 外部配置为每个 `api` 类型 HTTP 接口配置热点数据保护与熔断降级保护。

#### Scenario: External config overrides interface option
- **WHEN** 接口 option 和 `magic.api.<path>.options.hot_data_enable` 或 `magic.api.<path>.options.fallback_enable` 同时存在
- **THEN** 系统 MUST 使用 `magic.api.<path>.options.*` 的配置值参与本次请求判断

#### Scenario: Non HTTP scripts are not guarded
- **WHEN** Magic-API 资源由 task、subscribe、SSE/WebSocket subscribe 或其他非 HTTP API 入口触发
- **THEN** 系统 MUST 不执行本能力的热点数据或熔断降级校验

#### Scenario: Non positive count disables effective guard
- **WHEN** `hot_data_enable` 或 `fallback_enable` 为 `true` 但对应 count 配置小于或等于 `0`
- **THEN** 系统 MUST 不注册有效 Sentinel 阻断规则
- **AND** 系统 MUST 放行请求继续执行接口脚本

### Requirement: Hot data guard limits requests by resolved request key
系统 SHALL 在 `hot_data_enable=true` 且 `hot_data_count>0` 时，按 `hot_data_key` 解析出的请求 key 对 Magic-API 接口执行 Sentinel 参数热点保护。

#### Scenario: Hot data request enters Sentinel with resolved key
- **WHEN** 受保护接口收到请求且 `hot_data_key` 可解析为非空值
- **THEN** 系统 MUST 使用接口资源名和解析出的 key 进入 Sentinel 参数热点规则
- **AND** 系统 MUST 将 `hot_data_grade`、`hot_data_count`、`hot_data_duration` 映射到 Sentinel `ParamFlowRule`

#### Scenario: Empty hot data key uses fallback fingerprint
- **WHEN** `hot_data_key` 未配置或解析结果为空
- **THEN** 系统 MUST 基于接口路径、HTTP 方法、请求参数和请求体生成稳定 fallback key
- **AND** 系统 MUST 使用该 fallback key 参与 Sentinel 参数热点判断

#### Scenario: Hot data request is blocked
- **WHEN** Sentinel 参数热点规则判定当前请求被阻断
- **THEN** 系统 MUST NOT 执行该 Magic-API 接口脚本
- **AND** 系统 MUST 返回 Magic-API 统一失败响应

#### Scenario: Different hot data keys are isolated
- **WHEN** 两个请求解析出的热点 key 不同
- **THEN** 系统 MUST 让两个 key 分别参与 Sentinel 参数热点统计
- **AND** 一个 key 被阻断 MUST NOT 导致另一个 key 因 key 相同而被阻断

### Requirement: Fallback guard limits requests by interface resource
系统 SHALL 在 `fallback_enable=true` 且 `fallback_count>0` 时，按 Magic-API 接口资源维度执行 Sentinel FlowRule 保护。

#### Scenario: Fallback request enters Sentinel resource
- **WHEN** 受保护接口收到请求
- **THEN** 系统 MUST 使用稳定的接口资源名进入 Sentinel FlowRule
- **AND** 系统 MUST 将 `fallback_count` 映射到该接口资源的 Sentinel 规则

#### Scenario: Fallback request is blocked
- **WHEN** Sentinel FlowRule 判定当前请求被阻断
- **THEN** 系统 MUST NOT 执行该 Magic-API 接口脚本
- **AND** 系统 MUST 返回 Magic-API 统一失败响应

#### Scenario: Fallback guard does not call Java fallback method
- **WHEN** Sentinel FlowRule 判定当前请求被阻断
- **THEN** 系统 MUST NOT 尝试查找或调用 Java Bean 上的 `${methodName}Fallback` 方法

### Requirement: Sentinel entries are released after request completion
系统 SHALL 在受保护请求完成或异常退出后释放本次成功进入的 Sentinel `Entry`。

#### Scenario: Successful request releases Sentinel entries
- **WHEN** 热点数据或熔断降级保护已成功进入 Sentinel 且接口脚本正常执行完成
- **THEN** 系统 MUST 在请求完成后释放本次 Sentinel `Entry`

#### Scenario: Failed request releases Sentinel entries
- **WHEN** 热点数据或熔断降级保护已成功进入 Sentinel 且接口脚本执行过程中抛出异常
- **THEN** 系统 MUST 在异常处理完成后释放本次 Sentinel `Entry`

#### Scenario: Blocked request does not release unrelated entry
- **WHEN** 当前请求在进入 Sentinel 时被阻断且没有成功持有对应 `Entry`
- **THEN** 系统 MUST NOT 释放其他请求持有的 Sentinel `Entry`

### Requirement: Sentinel rule loading preserves unrelated rules
系统 SHALL 在注册或更新 Magic-API Sentinel guard 规则时保留其他接口和外部系统已有的 Sentinel 规则。

#### Scenario: Updating one interface keeps another interface rule
- **WHEN** 系统为接口 A 注册或更新热点数据或熔断降级规则
- **THEN** 系统 MUST 保留接口 B 已注册的 Magic-API Sentinel guard 规则


```

Full source: openspec/changes/add-magicapi-sentinel-guards/specs/magicapi-sentinel-guards/spec.md
