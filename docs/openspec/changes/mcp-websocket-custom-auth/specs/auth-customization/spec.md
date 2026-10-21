# Spec: auth-customization

框架授权验证的可自定义扩展点：业务可注册自有 `AuthService`/校验实现，对 mcp server 与 websocket 两套认证入口统一生效。

## Requirement: 业务可注册自定义授权实现

框架必须提供一个清晰的扩展接口/注册方式，使业务只需提供一个实现（Bean 或 provider 注册），即可将其自有校验逻辑接入 mcp server 与 websocket 的授权验证。

### Scenario: 业务注册自定义 provider 并被解析
- **WHEN** 业务实现自定义授权并注册到 `InstanceAuthRegistry` 的自定义 provider key
- **THEN** mcp server 与 websocket 在 auth 配置指向该 key 时，使用业务实现进行校验

### Scenario: 业务通过 McpAuthService SPI 适配自有逻辑
- **WHEN** 业务提供 `McpAuthService` 实现
- **THEN** 该实现可用于 mcp server 的请求认证，且与统一自定义 provider 机制不冲突

## Requirement: 自定义授权不影响内置 provider

内置 SaToken provider（apikey/login/sso/jwt 等）的解析与行为必须保持不变；自定义 provider 与内置 provider 通过不同的 provider key 区分。

### Scenario: 内置 provider 行为不变
- **WHEN** 配置使用内置 SaToken provider
- **THEN** 校验逻辑与引入自定义扩展前完全一致

## Requirement: 自定义 provider 解析失败必须 fail-closed

自定义 provider 缺失、模块不可用或校验失败时，认证必须拒绝（fail-closed），不得因扩展点而误放行。

### Scenario: 未配置/未解析自定义 provider
- **WHEN** 配置了自定义 provider key 但业务未注册对应实现
- **THEN** 请求被拒绝，行为与未知 provider 一致（拒绝而非放行）
