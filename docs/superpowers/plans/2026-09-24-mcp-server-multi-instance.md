---
change: mcp-server-multi-instance
design-doc: docs/openspec/changes/mcp-server-multi-instance/design.md
base-ref: 696651b4105c7912aad44de16b39b28cb9493c67
---

# 实施计划：mcp-server-multi-instance

<!-- comet-task-authority: docs/openspec/changes/mcp-server-multi-instance/tasks.md -->

> 任务清单唯一权威：`tasks.md`（revision `05810d01...`）。本计划只说明实施顺序、依赖、约束与验收方式。

## 执行方式（已确认）

- build_mode: `autonomous`（串行实施 + 关键里程碑独立审查）
- tdd_mode: `tdd`（RED→GREEN；恢复场景已存在的实现不回退，如实记录 RED 缺失）
- review_mode: `standard`（风险任务段独立审查；Verify 完成唯一一次最终集成审查）

## 1. 编译基线（阻塞项，最先做）

<!-- comet-task-ref:1.1 -->

范围：core（`fw-plus-ai-mcp`）+ boot（`fw-plus-ai-boot-autoconfigure`）全量编译通过，消除既有改动的编译错误。
当前缺口：`McpAutoConfiguration` 仍是旧单例装配，而控制器/初始化器/runtime 工厂均已改造——必须重写后才能编译。

- `cd fw-plus-core && ./mvnw -q -pl fw-plus-project/fw-plus-ai/fw-plus-ai-mcp -am compile -DskipTests`
- `cd fw-plus-ai && ./mvnw -q -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am compile -DskipTests`
- 约束：Java 1.8；UTF-8；不引入 Java 9+ API；React 5.3 / Reactor 3.4.x 可用。

## 2. `McpAutoConfiguration` 重构（升级装配为 runtime 工厂）

<!-- comet-task-ref:2.2 2.1 -->

- @Bean `McpAuthService`（`PermitAllAuthService`，`@ConditionalOnMissingBean`）
- @Bean `List<McpServerRuntime> mcpServerRuntimes`：调 `McpRuntimeFactory.createRuntimes(properties, authService, ObjectProvider<McpRedisBroadcaster>)`
- @Bean `McpServerInitializer(List<McpServerRuntime>, ApplicationContext)`
- @Import `McpRoutingRegistrar`（MVC，`@ConditionalOnClass`）与 `McpWebfluxWebBinding`（Flux，`@ConditionalOnClass`）
- 嵌套 `RedisClusterConfiguration`：`McpRedisBroadcaster`、`McpRegistry`、`McpRedisMessageConsumer(List<McpServerRuntime>, broadcaster)`、`RedisMessageListenerContainer`；删除 `DefaultServerSseRegistry`/`RedisServerSseRegistry` 等全局单例 @Bean（移入 factory）
- 保持 `@ConditionalOnProperty(enabled)`、`@AutoConfigureAfter(RedisAutoConfiguration)` 语义

## 3. 配置模型与校验（1.1 / 1.2 / 1.5 / 2.1 / 2.3 已随实现落盘，补单测）

<!-- comet-task-ref:babcfea3-3ebc-499c-b500-70cb36d8cf0d 90067cef-b5a7-4cf1-9566-b7ce71450791 507807a2-e24f-4254-94b9-fe48b0a08b99 4a4419f8-295d-44e2-9412-5231cfd1d50b 89cde243-32e7-489e-b9b7-700c3383bb80 -->

- `McpPropertiesTest` 补：`servers` 绑定、threadPoolSize 默认
- `McpRuntimeFactoryTest`：default+命名 runtime 生成；同 transport endpoint 重复抛 `IllegalArgumentException`；缺 key 报错；Redis 可用/不可用两种 registry 分支
- `McpFeatureScannerTest`：类级/方法级/未标注归属；孤儿 key 抛 `IllegalStateException`；与 `FeatureMgr#init` 等价的注册结果（tool/resource/prompt/completion）
- `McpServerRuntimeTest`：两个 runtime `hasSession` 互不可见
- `McpServerInitializerTest`：合并多 runtime 待扫包；按 key 分发到各 FeatureMgr；`server.start()` 逐 runtime 调用

## 4. 双栈与归属校验（3.1-3.5 已随实现落盘，补契约测试）

<!-- comet-task-ref:fa72d8fb-f383-4c97-9ba1-c1e4655339de 796de6bd-c74c-4bdd-b2e2-c666f6ace60a eafd1f77-490e-4606-946e-502c87bef40a 73398233-ebe4-447e-87be-d252c9fff5ac 6f1eca7b-ce2a-43f3-9022-877cc7ef9a47 -->

- 改写 `McpAutoConfigurationBeanSelectionTest`：不再断言单例 registry bean；断言 `List<McpServerRuntime>`（默认 1 个、命名后 N 个）、`McpAuthService` 默认 PermitAll、Redis 可用时 runtime.registry 为 Redis 实现类
- 新增 MVC 集成测试（`@SpringBootTest(webEnvironment=MOCK)` + `TestRestTemplate` 或 MockMvc）：
  - SSE：`GET {ep}/sse` 建立流、下发端点路径为 `{ep}/messages/{uuid}`；`POST {ep}/messages/{uuid}` 归属合法 → 处理；伪造/跨 runtime uuid → 404
  - Streamable：POST/GET/DELETE + `Mcp-Session-Id` 归属校验
- 新增 Flux 集成测试（`WebTestClient` + `spring-boot-starter-webflux` test scope）：同一路径布局与失效语义
- 路由三场景：同传输多 endpoint（并存）、异运输同 endpoint（并存）、同运输同 endpoint（启动抛异常）

## 5. 集群广播 routeKey（5.1 已随实现落盘，补 5.2 测试）

<!-- comet-task-ref:8a31d86f-a89d-4e6d-b4ec-68d72f1091e0 cc7dae11-1662-4b51-a5b1-b0ded99a8d87 -->

- 改写 `McpRedisBroadcasterTest`：payload 含 `routeKey`
- 改写 `McpRedisMessageConsumerTest`：按 routeKey 定位目标 runtime registry；routeKey 缺失兜底全部 runtime；来源节点自投递忽略
- 改写 `RedisServerSseRegistryTest` / `RedisServerStreamableHttpRegistryTest`：构造带 routeKey；本机无会话→经 Redis 转发；集群入口仅本地分发

## 6. 认证占位（4.1 已随实现落盘，4.2 不实现）

<!-- comet-task-ref:d3f0a9c8-1130-428b-b2f9-149d2736d9bf 184da21a-7dd5-4c9f-ad29-1198dfd1e856 -->

- `PermitAllAuthServiceTest`：默认返回 `McpPrincipal.ANONYMOUS`
- 覆盖测试：用户提供 `McpAuthService` bean 时 runtime 使用覆盖实现
- 4.2 按 tasks.md 标注为后续 change，不实现（勾选时以"接口面完成、实现按计划不做"为准）

## 7. 文档与样例（6.x）

<!-- comet-task-ref:1c593808-38dc-46bb-b969-ced1b00f09c8 d85b29d1-eedc-4e84-86ba-f8fd4c3364a4 475b0ef0-81d4-4d9e-a020-85a236cc1774 -->

- README（starter 模块）补多实例配置、`@McpServer` 归属、归属校验失效语义、双栈说明、认证为后续 change
- 样例：两个不同 endpoint runtime（weather/order）演示 + MVC 与 Flux 启动验证
- 全量测试 + `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate`

## 验收命令

```bash
# core
cd fw-plus-core && ./mvnw -pl fw-plus-project/fw-plus-ai/fw-plus-ai-mcp -am test
# boot autoconfigure
cd fw-plus-ai && ./mvnw -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am test
# 样例入口
./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate
```

## 审查安排（standard）

- 里程碑 A（编译修复 + AutoConfiguration 重构完成后）：独立审查装配语义与向后兼容（default 路径）
- 里程碑 B（双栈路由 + 归属校验）：独立审查会话隔离与失效响应
- 里程碑 C（集群 routeKey + 全部测试通过）：独立审查集群语义
- Verify：唯一一次最终集成审查
