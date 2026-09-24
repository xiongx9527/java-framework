# Tasks

## 1. Starter 模块创建

- [x] 1.1 创建 `fw-plus-ai-mcp-spring-boot-starter` 模块目录结构和 pom.xml（纯依赖 POM，无 Java 代码），依赖 `fw-plus-ai-boot-autoconfigure`，验证 `mvn compile` 通过 <!-- comet-task:f2cb6e65-44e0-4d44-835f-d62eddd546bf -->
- [x] 1.2 在 `fw-plus-ai-boot-starters/pom.xml` 中注册新模块，验证聚合构建通过 <!-- comet-task:46b72dca-39f4-4244-85d9-89623c358c58 -->

## 2. 自动配置增强（fw-plus-ai-boot-autoconfigure）

- [x] 2.1 扩展 `McpProperties` 配置类，新增 transport、base-package 等属性，验证配置绑定测试通过 <!-- comet-task:fd8a8a56-4f01-4309-ad9d-f19de65e9194 -->
- [x] 2.2 扩展 `McpAutoConfiguration`，基于 `fw-plus-ai-mcp` 核心模块注册 HTTP/SSE、Streamable HTTP 传输 Bean，验证条件化装配正确 <!-- comet-task:50fca93a-f749-4f52-aa08-6888e62ed425 -->
- [x] 2.3 实现可选的集群 Redis 广播支持，通过 `@ConditionalOnClass(RedisTemplate.class)` 动态启用，验证 Redis 存在时集群广播生效、不存在时本地广播仍可用 <!-- comet-task:7b74e07d-f6c7-497b-b8ba-675ae6cac6ee -->
- [x] 2.4 创建 `spring.factories` 注册自动配置（如尚未存在），验证引入 starter 后自动生效 <!-- comet-task:da17e647-56cf-49fa-b51b-7160515a6563 -->

## 3. 样例应用

- [x] 3.1 完善 `fw-plus-ai-mcp-server-sample`，添加 Tool、Resource、Prompt 示例，验证样例应用可启动并响应 MCP 请求 <!-- comet-task:f5a3a290-b55f-40b6-a320-e426400be407 -->

- [x] 4.1 编写单元测试（配置绑定、Bean 创建、传输切换），验证测试全部通过 <!-- comet-task:6198b5bf-4965-48c6-a113-4443070e907a -->
- [x] 4.2 编写集成测试（启动 Spring Boot 应用并验证 MCP Server 行为），验证集成测试通过 <!-- comet-task:d98e727d-5fd9-4bd3-9b26-4dfe7bcd0dd6 -->
- [x] 4.3 编写 README.md，包含依赖配置、属性说明、传输切换、集群广播说明，验证文档完整 <!-- comet-task:ffe13b29-fa57-43ce-878d-2c67ed1a82c1 -->

## 5. 集群消费端投递语义修正（用户反馈）

- [x] 5.1 修正 `McpRedisMessageConsumer.deliver`：`sessionId` 为空时调用 `broadcastOther`，非空调用 `sendOther`，验证消费端单测通过 <!-- comet-task:4e756cac-f4d0-4d92-aa0d-d47b4112e1f6 -->
- [x] 5.2 SPI `sendOther`/`broadcastOther` 放开 final，`RedisServerSseRegistry`、`RedisServerStreamableHttpRegistry` 重写为仅本地投递并按本机会话持有情况跳过，防止消息在节点间循环，验证注册表单测通过 <!-- comet-task:36e5b408-8b89-4175-a9a5-632a035ead8e -->
- [x] 5.3 修复 `spring.factories` 尾随逗号导致空类名崩溃；redis 相关 Bean 拆入嵌套配置类，消除无 redis classpath 时的 `NoClassDefFoundError`，验证有/无 redis 两种装配场景 <!-- comet-task:e744026c-40b9-45d3-826f-f7392a29b811 -->
- [x] 5.4 样例 `@McpService` 类改 public；集成测试按 SDK 协议要求补 `version`、`2025-06-18` 协议头与 `arguments`，验证样例 IT 通过 <!-- comet-task:09c250f8-06e4-4dca-a3c1-6800102dd599 -->
- [x] 5.5 README 同步最终配置（`base-packages` 逗号分隔、`endpoint`、随机 `serverId`、定向/广播语义与防循环说明） <!-- comet-task:00172b95-7287-4f72-b1c4-302ed0fd1702 -->
