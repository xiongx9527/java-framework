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
                WsAuthSupport.isAuthenticated -> registry.resolve(key, auth) -> 业务实现

内置 SaToken provider 仍按原 key 解析，行为不变
```

## 涉及文件

- 新增：`fw-plus-auth-core/.../auth/core/CustomAuthService.java`
- 修改：`fw-plus-auth-core/.../auth/core/InstanceAuthRegistry.java`（registerProvider + 自定义 lookup + availableProviders 追加）
- 修改：`fw-plus-ai-boot-autoconfigure/.../mcp/server/McpAutoConfiguration.java`（instanceAuthRegistry Bean 接收 ObjectProvider<CustomAuthService>）
- 修改：`fw-plus-boot-autoconfigure/.../web/ws/starter/WsAutoConfiguration.java`（同上）
- （可选）magic-api 控制台端点 `MagicMcpServerController` / `MagicMcpServerInfoMagicResourceStorage` 改用 `availableProviders(classLoader, customKeys)` 以展示自定义 key。
