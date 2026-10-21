# Proposal

## Why

当前 mcp server 与 websocket 的授权验证都经由 `InstanceAuthRegistry` 按 `provider` 解析**内置** `AuthService`（SaToken 系列：apikey/login/sso/jwt）。当业务需要自有鉴权逻辑（如对接自有网关、内部 token 服务、第三方 OIDC）时，没有可扩展的入口，只能改框架或硬编码。本 change 提供统一的可自定义授权扩展点，让业务注册自有校验实现，mcp server 与 websocket 两套入口均可接入。

## What Changes

- 新增业务可实现的自定义授权扩展接口（如 `CustomAuthService` / `AuthService` 自定义 provider 注册机制），业务只需提供一个 Bean 即可接入自有校验逻辑。
- 扩展 `InstanceAuthRegistry`，支持注册业务自定义的 `AuthService`（按 provider key 解析），使 mcp server（经 `McpAuthService.adapt`）与 websocket（经 `WsAuthSupport`）在配置 `provider` 指向自定义 key 时走业务实现。
- mcp server 的 `McpAuthService` SPI 保持兼容：业务既可用现有 `McpAuthService` 适配，也可走统一自定义 provider。
- **不**实现任何具体业务鉴权逻辑；**不**改动内置 SaToken provider 行为。

## Capabilities

### New Capabilities
- `auth-customization`: 框架授权验证的可自定义扩展点——业务可注册自有 `AuthService`/校验实现，对 mcp server 与 websocket 两套握手/请求认证入口统一生效。

### Modified Capabilities
<!-- mcp-server-starter 的授权解析入口复用本 capability 的扩展点，但既有 requirement 行为不变，故不另列 delta -->

## Impact

- 受影响代码：`fw-plus-auth-core` 的 `InstanceAuthRegistry`；`fw-plus-ai-boot-autoconfigure` 的 mcp 授权装配；`fw-plus-boot-autoconfigure` 的 `WsAuthSupport` 装配。
- 受影响 API：新增自定义授权注册接口/配置项；既有 `McpAuthService`、`WsAuthSupport` 公共方法签名保持不变。
- 依赖：无新增第三方依赖。
- 风险：自定义 provider 解析失败须 fail-closed（与现状一致），避免误放行。
