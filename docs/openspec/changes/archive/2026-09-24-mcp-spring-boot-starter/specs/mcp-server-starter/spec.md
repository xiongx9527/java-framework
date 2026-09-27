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

#### Scenario: Magic-API 工具自动注册
- **WHEN** 用户启用 Magic-API MCP 集成
- **THEN** Magic-API 中的 API 接口自动注册为 MCP 工具，AI 客户端可通过 MCP 调用
