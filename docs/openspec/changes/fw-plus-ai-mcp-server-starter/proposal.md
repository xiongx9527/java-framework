# Proposal: fw-plus-ai-mcp-server-starter

## 问题

当前项目缺少一个面向 Java 8 / Spring Boot 2.7 的标准 MCP Server 启动器。Magic-API MCP 插件面向 Magic-API 场景，不能直接作为普通 Spring Boot 应用的标准 MCP 服务端使用；同时官方 Java SDK 基线要求过高，与本项目当前 Java 8 约束不兼容。业务系统若需要接入 MCP，仍缺少开箱即用的 Starter、传输层和扩展机制。

## 目标

在 fw-plus 中新增 `fw-plus-ai-mcp-spring-boot-starter`，提供：

- core 层自研、Java 8 兼容的 MCP 2025-11-25 协议内核；
- Spring Boot 自动配置，默认暴露 `/mcp` 端点；
- Streamable HTTP 传输，支持 MVC 与 WebFlux 两套实现，并按应用类型互斥装配；
- 通过 Spring Bean 扩展 Tools / Resources / Prompts / Completion 能力；
- 可运行样例与使用文档，构建链 core -> ai -> sample 可编译通过。

## 范围

- `fw-plus-core/.../fw-plus-ai-mcp`：协议模型、JSON-RPC 处理、Server 核心、注册表、传输抽象、MVC / WebFlux 两套实现、`@McpTool` 注解扫描支持。
- `fw-plus-ai/.../autoconfigure/mcp`：自动配置、配置属性、Provider Bean 收集与注解 Bean 扫描注册。
- `fw-plus-ai/.../fw-plus-ai-mcp-spring-boot-starter`：Starter 空壳构件、依赖传递与 Boot 2.7 兼容注册。
- `fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-mcp-server-sample`：可运行样例。
- Starter 使用文档：坐标、依赖、配置项、SPI / 注解两种扩展方式、MVC / WebFlux 选择说明。
- 删除 `fw-plus-boot` 层重复空壳目录。

## 非目标

- 不实现 MCP Client。
- 不实现 Sampling、Roots、Tasks、Elicitation、OAuth。
- 不实现 2026-07-28 无状态协议改造。
- 不实现集群跨节点会话 / 通知。
- 不修改 Magic-API MCP 插件。

## 验收场景

- initialize 版本协商与 `Mcp-Session-Id` 握手可用，Tools / Resources / Prompts / Completion / Logging 可通过 JSON-RPC 正确响应，listChanged 等通知经 SSE 下发。
- 引入 Starter 后无自定义配置即可启动默认 `/mcp` 端点完成握手；`fw-plus.mcp.*` 可配置多实例与能力开关；MVC 与 WebFlux 按应用类型互斥装配。
- 声明为 Spring Bean 的 `McpToolProvider` / `McpResourceProvider` / `McpPromptProvider` 实现可被自动发现并暴露；`@McpTool` 注解 Bean 方法可被扫描注册并调用。
- `fw-plus-sample/fw-plus-ai-samples` 下可运行 MCP Server 样例可启动并完成握手 + 调用；构建链 core -> ai -> sample 编译通过。
