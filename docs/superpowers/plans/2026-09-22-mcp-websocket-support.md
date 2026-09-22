---
change: mcp-websocket-support
design-doc: docs/openspec/changes/mcp-websocket-support/design.md
base-ref: c7b3b0651df147815c409a3432ebdb7cd64987c0
archived-with: 2026-09-22-mcp-websocket-support
---

<!-- comet-task-authority: docs/openspec/changes/mcp-websocket-support/tasks.md -->

# Implementation Plan: mcp-websocket-support

## 1. Core 层：会话外部化接口与实现

### 1.1 McpSessionStore 接口
<!-- comet-task-ref:b0a7a006-53ec-4747-aa79-8952f129859a -->
- **Task ID**: `b0a7a006-53ec-4747-aa79-8952f129859a`
- **Scope**: 新增 `fw-plus-ai-mcp/src/main/java/.../transport/session/McpSessionStore.java`
- **Dependencies**: 无
- **Constraints**: 接口需包含 `save`/`load`/`remove`/`touch` 四个方法，与现有 `McpSession` 数据结构兼容
- **Verification**: 接口编译通过，方法签名清晰

### 1.2 InMemoryMcpSessionStore 实现
<!-- comet-task-ref:fb7ab883-f1c4-44bb-bd6c-1f075b1e72db -->
- **Task ID**: `fb7ab883-f1c4-44bb-bd6c-1f075b1e72db`
- **Scope**: 新增 `InMemoryMcpSessionStore.java`；修改 `AbstractStreamableTransport` 将内嵌会话管理委托给 `McpSessionStore`
- **Dependencies**: 1.1
- **Constraints**: 行为与当前完全一致，不改变现有 SSE transport 的外部行为
- **Verification**: 单测覆盖会话 CRUD；`AbstractStreamableTransport` 单测全绿

### 1.3 RedisMcpSessionStore 实现
<!-- comet-task-ref:e3e19667-b403-42c6-99a7-5f67012d561e -->
- **Task ID**: `e3e19667-b403-42c6-99a7-5f67012d561e`
- **Scope**: 新增 `RedisMcpSessionStore.java`，使用 `RedisTemplate<String, String>`，Key `mcp:session:{sessionId}`
- **Dependencies**: 1.1
- **Constraints**: 依赖 `spring-boot-starter-data-redis` 通过条件装配激活
- **Verification**: 集成测试验证 Redis 读写和 TTL 过期

---

## 2. Core 层：WebSocket Transport 实现

### 2.1 McpWebSocketTransport
<!-- comet-task-ref:3d09e572-0a2f-43a7-8599-608a80dbda07 -->
- **Task ID**: `3d09e572-0a2f-43a7-8599-608a80dbda07`
- **Scope**: 新增 `McpWebSocketTransport.java`（继承 `AbstractStreamableTransport`）；新增 `McpWebSocketServerTransportProvider.java`
- **Dependencies**: 1.1
- **Constraints**: 复用会话管理、协议版本协商、事件恢复逻辑；仅覆写 SSE 发送方法为 `WebSocketSession.send`
- **Verification**: 单测验证双向 JSON-RPC 消息路由

### 2.2 McpWebSocketEndpoint（MVC 栈）
<!-- comet-task-ref:6edc9509-5387-4db2-b0f3-e8b9248b7f0b -->
- **Task ID**: `6edc9509-5387-4db2-b0f3-e8b9248b7f0b`
- **Scope**: 新增 `McpWebSocketEndpoint.java`（继承 `AbstractWebSocketHandler`）
- **Dependencies**: 2.1
- **Constraints**: 处理握手、消息接收、连接关闭；委托 `McpWebSocketTransport`
- **Verification**: 单测验证握手、JSON-RPC 消息收发、异常关闭清理

### 2.3 McpWebFluxWebSocketEndpoint（WebFlux 栈）
- **Task ID**: `0fea7940-ba73-4bd0-bc18-20126d4ba07`
- **Scope**: 新增 `McpWebFluxWebSocketEndpoint.java`（实现 `WebSocketHandler`）
- **Dependencies**: 2.1
- **Constraints**: 处理 `Flux<WebSocketMessage>`；委托 `McpWebSocketTransport`
- **Verification**: 单测验证 Reactive WebSocket 消息流

### 2.4 WebSocket Transport 单测
<!-- comet-task-ref:4a8e2030-098e-4b09-8475-16afe373d92a -->
- **Task ID**: `4a8e2030-098e-4b09-8475-16afe373d92a`
- **Scope**: 覆盖 2.1-2.3 的单测文件
- **Dependencies**: 2.1, 2.2, 2.3
- **Constraints**: 覆盖连接建立、消息收发、异常关闭
- **Verification**: 单测全绿

---

## 3. Autoconfigure 层：装配与配置

### 3.1 McpProperties 扩展
<!-- comet-task-ref:b5b2034f-d5a6-4546-b56c-6bf582c67a41 -->
- **Task ID**: `b5b2034f-d5a6-4546-b56c-6bf582c67a41`
- **Scope**: 修改 `McpProperties.java` 和 `McpInstanceProperties.java`，新增 4 个属性
- **Dependencies**: 无
- **Constraints**: 默认值：`sseEnabled=true`，`ssePath=/mcp/sse`，`websocketEnabled=false`，`websocketPath=/mcp/ws`
- **Verification**: `@ConfigurationProperties` 绑定测试

### 3.2 McpSessionAutoConfiguration
<!-- comet-task-ref:72145e51-7839-46b0-9dc6-7f348bd84d01 -->
- **Task ID**: `72145e51-7839-46b0-9dc6-7f348bd84d01`
- **Scope**: 新增 `McpSessionAutoConfiguration.java`，条件装配内存/Redis 会话存储
- **Dependencies**: 1.2, 1.3
- **Constraints**: `@ConditionalOnClass(RedisTemplate.class)` + `@ConditionalOnMissingBean(McpSessionStore.class)`
- **Verification**: 条件装配测试

### 3.3 McpWebSocketAutoConfiguration（MVC + WebFlux）
<!-- comet-task-ref:348ff34d-e143-4abe-b209-a2bac66cb3d4 -->
- **Task ID**: `348ff34d-e143-4abe-b209-a2bac66cb3d4`
- **Scope**: 新增 MVC 和 WebFlux 两个 WebSocket 自动配置类
- **Dependencies**: 2.2, 2.3, 3.1
- **Constraints**: 与现有 SSE 装配互不干扰
- **Verification**: 装配测试验证端点注册

---

## 4. Starter 层：依赖与注册

### 4.1 Starter 依赖更新
<!-- comet-task-ref:94a78709-5785-46c4-8006-5ea40f7d44a9 -->
- **Task ID**: `94a78709-5785-46c4-8006-5ea40f7d44a9`
- **Scope**: 修改 `fw-plus-ai-mcp-spring-boot-starter/pom.xml`
- **Dependencies**: 无
- **Constraints**: 新增 `spring-boot-starter-websocket`（非 optional）
- **Verification**: Maven 编译通过

### 4.2 README 更新
<!-- comet-task-ref:a7403e65-368c-4f8f-a702-1a0c6a3a77ce -->
- **Task ID**: `a7403e65-368c-4f8f-a702-1a0c6a3a77ce`
- **Scope**: 更新 README.md
- **Dependencies**: 4.1
- **Verification**: 文档准确，示例可运行

---

## 5. 样例与验证

### 5.1 WebSocket 客户端示例
<!-- comet-task-ref:f571a5ed-faa8-43d0-ace8-bb86d84bdbe6 -->
- **Task ID**: `f571a5ed-faa8-43d0-ace8-bb86d84bdbe6`
- **Scope**: `fw-plus-ai-mcp-server-sample` 中新增 WebSocket 客户端示例
- **Dependencies**: 2.2, 3.3
- **Verification**: 示例代码可运行，完整流程验证

### 5.2 SSE 端点示例
<!-- comet-task-ref:3caef652-752c-4c0a-8b60-9674c0834e07 -->
- **Task ID**: `3caef652-752c-4c0a-8b60-9674c0834e07`
- **Scope**: 新增 SSE 端点示例
- **Dependencies**: 3.1
- **Verification**: `/mcp/sse` 正常工作

### 5.3 全量编译验证
<!-- comet-task-ref:e707ef23-b529-4ffd-bc33-2e00f2105e8f -->
- **Task ID**: `e707ef23-b529-4ffd-bc33-2e00f2105e8f`
- **Scope**: 全量 Maven 编译
- **Dependencies**: 全部任务
- **Verification**: `mvnw clean install -DskipTests` 无错误
