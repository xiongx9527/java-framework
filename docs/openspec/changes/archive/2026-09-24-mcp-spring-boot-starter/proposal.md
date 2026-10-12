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
