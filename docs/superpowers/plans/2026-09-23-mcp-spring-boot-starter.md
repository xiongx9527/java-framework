---
change: mcp-spring-boot-starter
design-doc: docs/openspec/changes/mcp-spring-boot-starter/design.md
base-ref: 44c62593d17e209b24617e71d6e378b73a9e37fe
archived-with: 2026-09-24-mcp-spring-boot-starter
---

# Implementation Plan: MCP Spring Boot Starter

## 1. Starter 模块创建

<!-- comet-task-ref:f2cb6e65-44e0-4d44-835f-d62eddd546bf -->
- 范围：确认 `fw-plus-ai-mcp-spring-boot-starter` 模块目录和 pom.xml 已存在
- 依赖：`fw-plus-ai-boot-autoconfigure`、`fw-plus-ai-mcp`
- 验证：`cd fw-plus-core && ./mvnw compile -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-mcp -am -DskipTests`

<!-- comet-task-ref:46b72dca-39f4-4244-85d9-89623c358c58 -->
- 范围：确认 `fw-plus-ai-boot-starters/pom.xml` 已注册新模块
- 验证：`cd fw-plus-core && ./mvnw compile -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-mcp-spring-boot-starter -am -DskipTests`

## 2. 自动配置增强

<!-- comet-task-ref:fd8a8a56-4f01-4309-ad9d-f19de65e9194 -->
- 范围：在 `fw-plus-ai-boot-autoconfigure` 中创建 `McpProperties`（`fw-plus.ai.mcp.*` 前缀），包含 server.name/version、transport、base-package
- 验证：编写 `McpPropertiesTest`，验证配置绑定

<!-- comet-task-ref:50fca93a-f749-4f52-aa08-6888e62ed425 -->
- 范围：创建 `McpAutoConfiguration`，注册 `McpServer`、`ServerConfig`、`FeatureMgr`，基于 `fw-plus-ai-mcp` 核心模块注册 SSE/StreamableHttp 传输 Bean
- 验证：编写 `McpAutoConfigurationTest`，验证条件化 Bean 创建

<!-- comet-task-ref:7b74e07d-f6c7-497b-b8ba-675ae6cac6ee -->
- 范围：通过 `@ConditionalOnClass(StringRedisTemplate.class)` 实现可选 Redis 广播
- 验证：编写 `McpRedisBroadcasterTest`，验证 Redis 存在/不存在时的行为

<!-- comet-task-ref:da17e647-56cf-49fa-b51b-7160515a6563 -->
- 范围：确认 `spring.factories` 和 `AutoConfiguration.imports` 已注册 `McpAutoConfiguration`
- 验证：`McpAutoConfigurationRegistrationTest`

## 3. 样例应用

<!-- comet-task-ref:f5a3a290-b55f-40b6-a320-e426400be407 -->
- 范围：在 `fw-plus-ai-mcp-server-sample` 中添加 Tool、Resource、Prompt 示例
- 验证：启动样例应用，验证 MCP 请求响应

## 4. 测试与文档

<!-- comet-task-ref:6198b5bf-4965-48c6-a113-4443070e907a -->
- 范围：单元测试（配置绑定、Bean 创建、传输切换）
- 验证：`mvn test` 全部通过

<!-- comet-task-ref:d98e727d-5fd9-4bd3-9b26-4dfe7bcd0dd6 -->
- 范围：集成测试（启动 Spring Boot 应用并验证 MCP Server 行为）
- 验证：`mvn verify` 通过

<!-- comet-task-ref:ffe13b29-fa57-43ce-878d-2c67ed1a82c1 -->
- 范围：README.md 编写
- 验证：文档完整性检查
