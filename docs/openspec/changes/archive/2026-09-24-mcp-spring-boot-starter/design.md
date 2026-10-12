---
comet_change: mcp-spring-boot-starter
change_ref: docs/openspec/changes/mcp-spring-boot-starter
role: technical-design
canonical_spec: openspec
archived-with: 2026-09-24-mcp-spring-boot-starter
status: final
---

# Design

## Context

当前 `fw-plus-ai-boot-autoconfigure` 中已有基于 aj-mcp 的 MCP Server 基础自动配置（`McpAutoConfiguration`），但需要对外提供一个轻量的 Starter 依赖包，方便用户快速接入。`fw-plus-ai-mcp` 核心模块已封装了 SSE 和 Streamable HTTP 传输的 Registry 能力，支持跨节点消息发送和广播。

## Goals / Non-Goals

**Goals：**
- 创建 `fw-plus-ai-mcp-spring-boot-starter` 纯依赖模块，不包含任何 Java 代码
- 该 Starter 仅聚合 `fw-plus-ai-boot-autoconfigure`，由后者提供全部自动配置和 Bean 注入
- 支持 HTTP/SSE、Streamable HTTP 两类传输切换
- 提供完整的 `fw-plus.ai.mcp.*` 配置属性
- 提供可选的集群 Redis 广播支持，根据环境动态配置

**Non-Goals：**
- 不在 Starter 中编写任何自动配置代码
- 不重新实现 MCP 协议，基于 aj-mcp 库
- 不支持 JSON-RPC batch 批量消息（与 aj-mcp 保持一致）
- 暂不实现 MCP Client 能力

## Decisions

### D1: Starter 为纯依赖 POM

`fw-plus-ai-mcp-spring-boot-starter` 仅包含 `pom.xml`，依赖 `fw-plus-ai-boot-autoconfigure`，不包含任何 Java 代码或 `spring.factories`。

**Rationale**：所有自动配置和 Bean 注入已在 `fw-plus-ai-boot-autoconfigure` 中实现，Starter 只需做依赖聚合，用户引入 Starter 即可获得完整能力。

**Alternatives considered**：
- Starter 包含自动配置代码：与 `fw-plus-ai-boot-autoconfigure` 职责重叠，维护两份配置容易不一致

### D2: 传输方式实现

基于 `fw-plus-ai-mcp` 核心模块已有的 `ServerSseRegistry`、`ServerStreamableHttpRegistry`，在 `fw-plus-ai-boot-autoconfigure` 中通过 Spring 条件化 Bean 选择激活。

**Rationale**：核心模块已封装传输能力，Boot 层只需条件化注册，无需重复实现。

### D3: 集群 Redis 广播

通过可选依赖 `spring-boot-starter-data-redis` + `RedisTemplate` 实现集群事件广播；当环境中存在 Redis 时自动启用，否则仅使用本地广播。

**Rationale**：满足"根据环境动态配置"的要求，单机部署无需 Redis 也能正常工作。

## Risks / Trade-offs

[Starter 零代码的认知门槛] → 在 README 中明确说明 Starter 的依赖聚合角色，避免用户困惑

[Redis 可选依赖的 ClassNotFound] → 使用 `@ConditionalOnClass` 确保 Redis 类不存在时不影响启动

[传输方式互斥] → 通过 `@ConditionalOnProperty` 确保同一时间只激活一种传输
