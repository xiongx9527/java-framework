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

#### Scenario: Updating Magic-API rules keeps external Sentinel rules
- **WHEN** 应用中已存在非本能力创建的 Sentinel 规则
- **THEN** 系统 MUST NOT 因注册 Magic-API Sentinel guard 规则而删除这些外部规则
