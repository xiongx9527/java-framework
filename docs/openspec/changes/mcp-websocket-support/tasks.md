# Tasks

## 1. Core 层：会话外部化接口与实现

- [x] 1.1 新增 `McpSessionStore` 接口（`save`/`load`/`remove`/`touch`），定义在 `fw-plus-ai-mcp` 的 `transport/session` 包，验证接口契约清晰 <!-- comet-task:b0a7a006-53ec-4747-aa79-8952f129859a -->
- [x] 1.2 新增 `InMemoryMcpSessionStore` 实现，将当前 `AbstractStreamableTransport` 内嵌的会话管理逻辑迁移至此，验证单机模式行为不变 <!-- comet-task:fb7ab883-f1c4-44bb-bd6c-1f075b1e72db -->
- [x] 1.3 新增 `RedisMcpSessionStore` 实现，使用 `RedisTemplate<String, String>` 存储序列化会话 JSON，Key 格式 `mcp:session:{sessionId}`，验证 Redis 可用时正确读写和过期 <!-- comet-task:e3e19667-b403-42c6-99a7-5f67012d561e -->

## 2. Core 层：WebSocket Transport 实现

- [x] 2.1 新增 `McpWebSocketTransport`，继承 `AbstractStreamableTransport`，覆写 SSE 发送方法为 `WebSocketSession.send`，验证双向 JSON-RPC 消息路由正确 <!-- comet-task:3d09e572-0a2f-43a7-8599-608a80dbda07 -->
- [x] 2.2 新增 `McpWebSocketEndpoint`（MVC 栈），继承 `AbstractWebSocketHandler`，处理握手、消息接收和连接关闭，验证端点可建立连接并处理 JSON-RPC <!-- comet-task:6edc9509-5387-4db2-b0f3-e8b9248b7f0b -->
- [~] 2.3 新增 `McpWebFluxWebSocketEndpoint`（WebFlux 栈），实现 `WebSocketHandler`，处理 `Flux<WebSocketMessage>`，验证 Reactive 端点正常工作 <!-- comet-task:0fea7940-ba73-4bd0-bc18-20126d4ba07 --> — 延后至后续版本
- [x] 2.4 编写 WebSocket transport 单测，覆盖连接建立、消息收发、异常关闭场景 <!-- comet-task:4a8e2030-098e-4b09-8475-16afe373d92a -->

## 3. Autoconfigure 层：装配与配置

- [x] 3.1 修改 `McpProperties`，新增 `sseEnabled`、`ssePath`、`websocketEnabled`、`websocketPath` 四个属性，验证配置绑定正确 <!-- comet-task:b5b2034f-d5a6-4546-b56c-6bf582c67a41 -->
- [x] 3.2 新增 `McpSessionAutoConfiguration`，条件装配 `InMemoryMcpSessionStore`（默认）和 `RedisMcpSessionStore`（Redis 在 classpath 时激活），验证自动切换逻辑 <!-- comet-task:72145e51-7839-46b0-9dc6-7f348bd84d01 -->
- [x] 3.3 新增 `McpWebSocketAutoConfiguration`（MVC）和 `McpWebFluxWebSocketAutoConfiguration`（WebFlux），条件装配 WebSocket transport 和端点，验证与现有 SSE 装配互不干扰 <!-- comet-task:348ff34d-e143-4abe-b209-a2bac66cb3d4 -->

## 4. Starter 层：依赖与注册

- [x] 4.1 在 `fw-plus-ai-mcp-spring-boot-starter/pom.xml` 新增 `spring-boot-starter-websocket` 依赖，验证 Maven 构建通过 <!-- comet-task:94a78709-5785-46c4-8006-5ea40f7d44a9 -->
- [x] 4.2 更新 `README.md`，新增 WebSocket 和 SSE 端点配置说明，验证文档准确 <!-- comet-task:a7403e65-368c-4f8f-a702-1a0c6a3a77ce -->

## 5. 样例与验证

- [x] 5.1 在 `fw-plus-ai-mcp-server-sample` 中增加 WebSocket 客户端示例代码，验证 WebSocket 连接、工具调用完整流程 <!-- comet-task:f571a5ed-faa8-43d0-ace8-bb86d84bdbe6 -->
- [x] 5.2 在 `fw-plus-ai-mcp-server-sample` 中增加 SSE 端点示例，验证 `/mcp/sse` 正常工作 <!-- comet-task:3caef652-752c-4c0a-8b60-9674c0834e07 -->
- [x] 5.3 运行全量 Maven 编译（`mvnw clean install -DskipTests`），验证无编译错误 <!-- comet-task:e707ef23-b529-4ffd-bc33-2e00f2105e8f -->
