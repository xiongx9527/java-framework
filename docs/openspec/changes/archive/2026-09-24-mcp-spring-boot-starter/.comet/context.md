# Comet Design Handoff

- Change: mcp-spring-boot-starter
- Phase: design
- Mode: compact
- Context hash: 7709f601d2047703a4bd8c4600d450bafd945ca32e591a14875c6d09b0b00c59

Generated-by: comet-handoff.sh
Task hash policy: task-content-v1. Read tasks.md for live completion; excerpts are design-time context.

OpenSpec remains the canonical capability spec. This handoff is a deterministic, source-traceable context pack, not an agent-authored summary.

## docs/openspec/changes/mcp-spring-boot-starter/proposal.md

- Source: docs/openspec/changes/mcp-spring-boot-starter/proposal.md
- Lines: 1-29
- SHA256: 2dbde76f0e29c06313268b24812b7f968cbb5fe9b7dcb676eb8b81366860e3e2

```md
# Proposal

## Why

项目当前已有基于 aj-mcp 的 MCP Server 基础能力（`fw-plus-ai-mcp` 核心模块 + `fw-plus-ai-boot-autoconfigure`），但用户需要一个轻量的 Spring Boot Starter 依赖包来快速接入。本项目创建一个无代码的依赖型 Starter，所有 Bean 注入和自动配置均在 `fw-plus-ai-boot-autoconfigure` 中实现，支持 HTTP/SSE 和 Streamable HTTP 传输，并可选择性地在集群环境下通过 Redis 广播向客户端发送通知。

## What Changes

- 新增 `fw-plus-ai-mcp-spring-boot-starter` 模块，作为纯依赖 POM 发布，不包含任何 Java 代码
- 该 Starter 依赖 `fw-plus-ai-boot-autoconfigure`，由后者提供全部自动配置和 Bean 注入
- 基于 `fw-plus-ai-mcp` 核心模块提供 HTTP/SSE、Streamable HTTP 两类传输
- 提供 `fw-plus.ai.mcp.*` 配置属性，支持服务器名称、版本、传输方式、基础包路径等
- 提供可选的集群 Redis 广播支持，根据环境动态配置（单机部署无需 Redis）
- 提供 `fw-plus-ai-mcp-server-sample` 样例应用

## Capabilities

### New Capabilities
- `mcp-server-starter`: 纯依赖型 Spring Boot Starter，聚合 `fw-plus-ai-boot-autoconfigure` 及其传递依赖，用户引入后自动获得 MCP Server 能力

### Modified Capabilities
（无）

## Impact

- 新增模块：`fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-mcp-spring-boot-starter/`
- 新增样例：`fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample/`
- 影响范围：fw-plus-ai 层，新增纯依赖 Starter 模块
- 用户只需添加 starter 依赖并配置属性即可启用 MCP Server，集群环境可选启用 Redis 广播

```

## docs/openspec/changes/mcp-spring-boot-starter/design.md

- Source: docs/openspec/changes/mcp-spring-boot-starter/design.md
- Lines: 1-58
- SHA256: 3492847016d101f018de2891b4e56584997f8f834b046126acfd248508b7dec9

```md
---
comet_change: mcp-spring-boot-starter
change_ref: docs/openspec/changes/mcp-spring-boot-starter
role: technical-design
canonical_spec: openspec
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

```

## docs/openspec/changes/mcp-spring-boot-starter/tasks.md

- Source: docs/openspec/changes/mcp-spring-boot-starter/tasks.md
- Lines: 1-23
- SHA256: 7cf880b8022de6908859d53f0e683c0f7a49726c26fedb138984ace765401d48

```md
# Tasks

## 1. Starter 模块创建

- [ ] 1.1 创建 `fw-plus-ai-mcp-spring-boot-starter` 模块目录结构和 pom.xml（纯依赖 POM，无 Java 代码），依赖 `fw-plus-ai-boot-autoconfigure`，验证 `mvn compile` 通过
- [ ] 1.2 在 `fw-plus-ai-boot-starters/pom.xml` 中注册新模块，验证聚合构建通过

## 2. 自动配置增强（fw-plus-ai-boot-autoconfigure）

- [ ] 2.1 扩展 `McpProperties` 配置类，新增 transport、base-package 等属性，验证配置绑定测试通过
- [ ] 2.2 扩展 `McpAutoConfiguration`，基于 `fw-plus-ai-mcp` 核心模块注册 HTTP/SSE、Streamable HTTP 传输 Bean，验证条件化装配正确
- [ ] 2.3 实现可选的集群 Redis 广播支持，通过 `@ConditionalOnClass(RedisTemplate.class)` 动态启用，验证 Redis 存在时集群广播生效、不存在时本地广播仍可用
- [ ] 2.4 创建 `spring.factories` 注册自动配置（如尚未存在），验证引入 starter 后自动生效

## 3. 样例应用

- [ ] 3.1 完善 `fw-plus-ai-mcp-server-sample`，添加 Tool、Resource、Prompt 示例，验证样例应用可启动并响应 MCP 请求

## 4. 测试与文档

- [ ] 4.1 编写单元测试（配置绑定、Bean 创建、传输切换），验证测试全部通过
- [ ] 4.2 编写集成测试（启动 Spring Boot 应用并验证 MCP Server 行为），验证集成测试通过
- [ ] 4.3 编写 README.md，包含依赖配置、属性说明、传输切换、集群广播说明，验证文档完整

```

## docs/openspec/changes/mcp-spring-boot-starter/.openspec.yaml

- Source: docs/openspec/changes/mcp-spring-boot-starter/.openspec.yaml
- Lines: 1-2
- SHA256: d04b6ad7c1b1e5eb9e8ec8cfdae94bc49d29216b59b73cea7a3cc1de9d508463

```md
schema: spec-driven
created: 2026-09-23

```

## docs/openspec/changes/mcp-spring-boot-starter/specs/mcp-server-starter/spec.md

- Source: docs/openspec/changes/mcp-spring-boot-starter/specs/mcp-server-starter/spec.md
- Lines: 1-83
- SHA256: ad334cef23aa21e9d9aebfa4134d44b214012911ec738e1301febb753e1091b5

[TRUNCATED]

```md
# Spec Delta

## Purpose

为 Spring Boot 应用提供轻量 MCP Server Starter 依赖包，用户只需添加 starter 依赖并配置属性，即可通过 `fw-plus-ai-boot-autoconfigure` 将 Java 业务能力通过 MCP 协议暴露给 AI 客户端，支持 HTTP/SSE 和 Streamable HTTP 传输，并可根据环境动态启用集群 Redis 广播。

## ADDED Requirements

### Requirement: 依赖型 Starter

`fw-plus-ai-mcp-spring-boot-starter`  SHALL 为纯依赖 POM，不包含任何 Java 代码，仅聚合 `fw-plus-ai-boot-autoconfigure` 及其传递依赖。

#### Scenario: 依赖聚合
- **WHEN** 用户添加 `fw-plus-ai-mcp-spring-boot-starter` 依赖到 Spring Boot 应用
- **THEN** 传递依赖自动引入 `fw-plus-ai-boot-autoconfigure`、`fw-plus-ai-mcp`、`aj-mcp-server` 等，无需额外配置

#### Scenario: Starter 无代码
- **WHEN** 用户查看 starter 模块内容
- **THEN** 仅存在 `pom.xml`，无 `src/main/java` 目录

### Requirement: 自动配置 MCP Server

Spring Boot 应用添加 starter 依赖后，SHALL 由 `fw-plus-ai-boot-autoconfigure` 自动配置 MCP Server 实例，无需用户手动编写配置类。

#### Scenario: 自动配置生效
- **WHEN** 用户添加 `fw-plus-ai-mcp-spring-boot-starter` 依赖到 Spring Boot 应用
- **THEN** 应用启动时自动创建 `McpServer` Bean 并注册 `@McpService` 注解的类

#### Scenario: 无配置也能运行
- **WHEN** 用户未配置任何 MCP 相关属性
- **THEN** MCP Server 使用默认配置启动，默认使用 HTTP/SSE 传输

### Requirement: 多传输方式支持

MCP Server SHALL 支持 HTTP/SSE、Streamable HTTP 两类传输方式，用户可通过配置属性选择启用哪种传输。

#### Scenario: HTTP/SSE 传输
- **WHEN** 用户配置 `fw-plus.ai.mcp.transport=http-sse` 并设置 `server.port`
- **THEN** MCP Server 通过 HTTP 端点提供 SSE 传输，支持事件推送

#### Scenario: Streamable HTTP 传输
- **WHEN** 用户配置 `fw-plus.ai.mcp.transport=streamable-http`
- **THEN** MCP Server 通过单个 HTTP 端点处理所有 JSON-RPC 消息

### Requirement: 注解扫描与注册

MCP Server SHALL 自动扫描指定包路径下标记了 `@McpService` 的类，并将其中的工具、资源、提示词方法注册到 MCP Server。

#### Scenario: 包扫描注册
- **WHEN** 用户配置 `fw-plus.ai.mcp.base-package=com.example.mcp`
- **THEN** 系统扫描该包下所有 `@McpService` 类并注册其公开方法

#### Scenario: 默认包扫描
- **WHEN** 用户未配置 `base-package`
- **THEN** 系统扫描启动类所在包及其子包

### Requirement: 配置属性

MCP Server SHALL 提供 `fw-plus.ai.mcp.*` 前缀的配置属性，支持配置服务器名称、版本、传输方式、基础包路径等。

#### Scenario: 自定义服务器信息
- **WHEN** 用户配置 `fw-plus.ai.mcp.server.name=my-server` 和 `fw-plus.ai.mcp.server.version=1.0.0`
- **THEN** MCP Server 在初始化时向客户端报告自定义的名称和版本

### Requirement: 集群 Redis 广播

MCP Server SHALL 支持可选的集群 Redis 广播能力。当环境中存在 `StringRedisTemplate` 时自动启用 Redis 广播，否则仅使用本地广播。

#### Scenario: Redis 广播启用
- **WHEN** 应用包含 `spring-boot-starter-data-redis` 依赖并配置了 Redis 连接
- **THEN** MCP Server 通过 Redis 向集群中其他节点的客户端广播通知

#### Scenario: 无 Redis 降级
- **WHEN** 应用未引入 Redis 依赖
- **THEN** MCP Server 仅使用本地广播，不影响正常运行

### Requirement: 与 Magic-API 集成

MCP Server SHALL 支持将 Magic-API 的接口、函数、任务自动注册为 MCP 工具，使 Magic-API 脚本能力可通过 MCP 协议访问。


```

Full source: docs/openspec/changes/mcp-spring-boot-starter/specs/mcp-server-starter/spec.md
