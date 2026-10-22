---
change: mcp-websocket-custom-auth
design-doc: docs/openspec/changes/mcp-websocket-custom-auth/design.md
base-ref: ce4970fe010e97b3874c3caa7779ea2c3b8a5b17
---

<!-- comet-task-authority: docs/openspec/changes/mcp-websocket-custom-auth/tasks.md -->

# 实施计划：mcp-websocket-custom-auth

本计划实现「mcp server 与 websocket 授权验证支持业务自定义扩展点」：在 auth-core 新增 `CustomAuthService` SPI，扩展 `InstanceAuthRegistry` 按自定义 key 注册/解析业务 `AuthService`，并在 mcp / ws 自动配置中装配。执行策略：autonomous + standard 审查。

## 任务与验收

### T1 — 定义 CustomAuthService 并扩展 InstanceAuthRegistry <!-- comet-task-ref:eb62f9fa-7c9f-42e4-bd52-8d5659f165d3 -->
- 范围：在 `fw-plus-auth-core` 新增 `CustomAuthService extends AuthService`（含 `getProviderKey()`）；在 `InstanceAuthRegistry` 新增 `registerProvider(key, service)`、`customProviders` 查找，并让 `resolve` 支持自定义 key（未命中内置与自定义时抛 `IllegalArgumentException`，fail-closed）；`availableProviders()` 实例方法追加自定义 key。
- 验收：`InstanceAuthRegistry` 单元测试覆盖：注册后 resolve 返回业务实现；未注册自定义 key 抛异常；内置 provider 行为不变。

### T2 — mcp 自动配置装配自定义 provider <!-- comet-task-ref:6d533758-4b43-49e9-8a29-996cb58cf81f -->
- 范围：修改 `McpAutoConfiguration.instanceAuthRegistry` Bean，接收 `ObjectProvider<CustomAuthService>` 并遍历 `registerProvider`；mcp 请求认证仍经 `McpAuthService.adapt(registry.resolve(...))`，无需改调用链路。
- 验收：`McpAutoConfigurationBeanSelectionTest` 仍通过；新增测试证明配置 `auth.provider=自定义key` 时走业务实现。

### T3 — websocket 自动配置装配自定义 provider <!-- comet-task-ref:e9308ec9-b7b2-414e-b65d-49b0944c61c1 -->
- 范围：修改 `WsAutoConfiguration.instanceAuthRegistry` Bean，接收 `ObjectProvider<CustomAuthService>` 并遍历 `registerProvider`；`WsAuthSupport` 调用链路不变。
- 验收：ws 握手在 `auth.provider=自定义key` 时走业务实现；`validateProviders` 对未注册自定义 key 启动失败（与现状未知 provider 一致）。

### T4 — 内置 provider 不变 + fail-closed 一致性 <!-- comet-task-ref:99f191bb-ade4-47dd-8dac-80cec72bf9b4 -->
- 范围：回归内置 SaToken provider 解析与 `PermitAllAuthService`/`RejectingAuthService` 行为；确认自定义缺失/未知 key 拒绝而非放行。
- 验收：既有 `McpAuthServiceRegistryTest` 等全绿；新增断言覆盖 fail-closed。

### T5 — 示例/测试覆盖 <!-- comet-task-ref:a9f8e881-59d5-47af-9141-ab0cb3308c4e -->
- 范围：为自定义 provider 在 mcp 与 ws 两端生效提供测试（含一个 `CustomAuthService` 实现 Bean 的上下文测试），并验证内置 provider 行为不变。
- 验收：对应测试全绿，覆盖 mcp + ws 两条入口。

## 实施顺序

T1（核心 SPI + registry）→ T2（mcp 装配）→ T3（ws 装配）→ T4（一致性回归）→ T5（测试覆盖）。

## 风险与回退

- 低风险：仅新增 SPI 与装配，不改动内置 SaToken provider 实现与 `McpAuthService`/`WsAuthSupport` 公共签名。
- fail-closed：自定义 key 未注册即抛异常，绝不误放行；与现状未知 provider 语义一致。
- 回退：若业务误用同 key 覆盖内置 provider，registry 在 `registerProvider` 时跳过并 warn，内置优先。
