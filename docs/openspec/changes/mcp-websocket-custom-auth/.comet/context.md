# Comet Design Handoff

- Change: mcp-websocket-custom-auth
- Phase: design
- Mode: compact
- Context hash: a50bc502a57a36b5fd6c007aa90a43b9b8385692c9b8c99c8329c3b7ebb15faf

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/mcp-websocket-custom-auth/proposal.md

- Source: docs/openspec/changes/mcp-websocket-custom-auth/proposal.md
- Lines: 1-27
- SHA256: e48673a676ffc5d7773a38180ca6f388833faed8d9d03e260f785b36da4fb76a

```md
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

```

## docs/openspec/changes/mcp-websocket-custom-auth/design.md

- Source: docs/openspec/changes/mcp-websocket-custom-auth/design.md
- Lines: 1-92
- SHA256: 79ee5f6aec3a5a43c3d11bff2d633f33935b3ba2786efe5724a5230f5a8d2588

[TRUNCATED]

```md
---
comet_change: mcp-websocket-custom-auth
role: technical-design
canonical_spec: openspec
---

# Design: mcp-websocket-custom-auth

## 背景

mcp server 与 websocket 的授权验证都收敛到 `InstanceAuthRegistry.resolve(instanceKey, auth)` 解析**内置** `AuthService`（SaToken 系列）。业务需要自有鉴权（自有网关、内部 token 服务、第三方 OIDC）时，没有可扩展入口。本 change 让 `InstanceAuthRegistry` 支持业务注册自定义 `AuthService` provider，mcp 与 ws 两条入口经既有链路自动复用。

## 关键决策

### 1. 扩展点形态：CustomAuthService SPI（复用现有 provider 配置项）

在 `fw-plus-auth-core` 新增接口：

```java
package com.xiongx9527.fw.plus.auth.core;

/** 业务自定义授权实现：声明一个 Bean 即可接入 mcp 与 ws 两套认证入口。 */
public interface CustomAuthService extends AuthService {
    /** 自定义 provider key；业务在 mcp/ws 的 auth.provider 配置中引用此值。 */
    String getProviderKey();
}
```

- 业务只需声明一个 `@Bean CustomAuthService`（返回自身 key + 实现 `authenticate`）。
- **不新增任何配置项**：业务在既有 `auth.provider` 字段里直接填自定义 key（例如 `my-company-oidc`），与内置 provider 同一字段、不同值即可。mcp 配置 `fw-plus.ai.mcp.auth.provider` / `servers[].auth.provider`，ws 配置 `fw.plus.ws.servers[].auth.provider`，均原样透传。

### 2. InstanceAuthRegistry 扩展

- 新增字段 `Map<String, AuthService> customProviders`（仅在启动装配期写入，运行期只读）。
- 新增 `void registerProvider(String key, AuthService service)`（重复 key 以首次注册为准并 warn；与内置 key 冲突时内置优先，register 时跳过并 warn）。
- `resolve(instanceKey, auth)`：
  1. provider 为空 → 返回 `null`（未认证，请求 401/握手拒绝），与现状一致；
  2. 命中内置 key → 原逻辑；
  3. 命中 `customProviders` → 返回业务 `AuthService`；
  4. 其它（含未注册的自定义 key / 历史 `custom`）→ 抛 `IllegalArgumentException`（fail-closed）。
- `availableProviders()` 实例方法：在内置清单基础上追加 `customProviders.keySet()`，使控制台/校验可选项包含自定义 key。静态 `availableProviders(ClassLoader)` 保持内置清单不变；新增重载 `availableProviders(ClassLoader, Collection<String> extra)` 供需要展示自定义 key 的端点调用（magic-api 控制台端点可选升级，不强制）。

### 3. 自动配置装配（唯一集成点）

`InstanceAuthRegistry` 在 `McpAutoConfiguration` 与 `WsAutoConfiguration` 中均以 `@Bean @ConditionalOnMissingBean` 声明，二者互斥只生成一个 Bean。两个 `@Bean` 方法都改为接收 `ObjectProvider<CustomAuthService>` 并在构造后遍历 `registerProvider(key, service)`。

```java
@Bean
@ConditionalOnMissingBean
public InstanceAuthRegistry instanceAuthRegistry(ObjectProvider<CustomAuthService> customProviders) {
    InstanceAuthRegistry registry = new InstanceAuthRegistry();
    customProviders.orderedStream().forEach(s -> registry.registerProvider(s.getProviderKey(), s));
    return registry;
}
```

### 4. 与既有 McpAuthService SPI 的关系

保留 `McpAuthService` SPI 不变。自定义 provider 经 `McpAuthService.adapt(registry.resolve(...))` 自动成为 mcp 认证；业务也可继续直接用 `McpAuthService` 适配，两者并存、不冲突。

### 5. fail-closed 一致性

- 内置 SaToken provider、permit-all、RejectingAuthService 行为**完全不变**。
- 自定义 key 已注册 → 使用业务实现。
- 自定义 key 配置但未注册（业务漏配 Bean）/ 命中未知值 → `resolve` 抛 `IllegalArgumentException`；`WsAuthSupport.validateProviders` 在启动期即抛该异常使启动失败（与现状未知 provider 一致），运行期解析同样拒绝。绝不误放行。

### 6. 范围边界

- 新增：`CustomAuthService` 接口、`InstanceAuthRegistry.registerProvider` + 自定义 lookup、`availableProviders` 追加、两个自动配置 Bean 装配 `ObjectProvider<CustomAuthService>`。
- 不改：内置 SaToken provider 实现与行为、`McpAuthService` / `WsAuthSupport` 公共方法签名、mcp/ws 既有调用链路（仍经 `resolve`）。
- 不实现任何具体业务鉴权逻辑（由业务侧提供）。

## 数据流

```
业务 @Bean CustomAuthService(key="my-company-oidc", authenticate=...)
  └─> 自动配置遍历 ObjectProvider<CustomAuthService> 注册到 InstanceAuthRegistry
        ├─> mcp: 配置 auth.provider=my-company-oidc
        │       McpAuthService.adapt(registry.resolve(key, auth)) -> 业务实现
        └─> ws:  配置 servers[].auth.provider=my-company-oidc

```

Full source: docs/openspec/changes/mcp-websocket-custom-auth/design.md

## docs/openspec/changes/mcp-websocket-custom-auth/tasks.md

- Source: docs/openspec/changes/mcp-websocket-custom-auth/tasks.md
- Lines: 1-7
- SHA256: 990a2cd1f28bd5bd1bc4bf0a6b525ad47895d3aff60a75477ef0afe94ffefa75

```md
# Tasks: mcp-websocket-custom-auth

- [ ] 1. 在 `fw-plus-auth-core` 定义业务可实现的自定义授权扩展接口（如 `CustomAuthService`/复用核心 `AuthService`），并扩展 `InstanceAuthRegistry` 支持按自定义 key 注册/解析业务 `AuthService` <!-- comet-task:eb62f9fa-7c9f-42e4-bd52-8d5659f165d3 -->
- [ ] 2. 在 mcp 自动配置中装配自定义 provider：配置 `auth.provider=<自定义key>` 时经 `McpAuthService.adapt(registry.resolve(...))` 使用业务实现 <!-- comet-task:6d533758-4b43-49e9-8a29-996cb58cf81f -->
- [ ] 3. 在 websocket 自动配置中装配自定义 provider：`WsAuthSupport` 配置 `fw.plus.ws.auth.provider=<自定义key>` 时走业务实现 <!-- comet-task:e9308ec9-b7b2-414e-b65d-49b0944c61c1 -->
- [ ] 4. 保证内置 SaToken provider 行为不变；自定义 provider 缺失/失败时 fail-closed（拒绝） <!-- comet-task:99f191bb-ade4-47dd-8dac-80cec72bf9b4 -->
- [ ] 5. 提供使用示例或测试：验证业务自定义实现在 mcp server 与 websocket 两端均生效，同时内置 provider 不变 <!-- comet-task:a9f8e881-59d5-47af-9141-ab0cb3308c4e -->

```

## docs/openspec/changes/mcp-websocket-custom-auth/specs/auth-customization/spec.md

- Source: docs/openspec/changes/mcp-websocket-custom-auth/specs/auth-customization/spec.md
- Lines: 1-31
- SHA256: 477a6ef0cd132a08a7ca460219c9e0dac8083c5501239e7e38e709badb035efd

```md
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

```
