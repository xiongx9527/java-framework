---
comet_change: standardize-websocket-plugin
role: technical-design
canonical_spec: openspec
archived-with: 2026-07-05-standardize-websocket-plugin
status: final
---

# 标准化 MagicAPI WebSocket 插件技术设计

## 背景

当前 WS 插件已经提供 `ws-datasource`、`ws-subscribe`、连接生命周期脚本、服务端点对点推送、全量广播和 Redis 集群广播能力。但部分行为仍是业务约定而非标准 WebSocket 语义：控制帧没有清晰分层，非文本消息边界不明确，关闭码使用不系统，业务错误与协议错误混在一起，主动清理和被动关闭可能重复触发关闭脚本。

本设计以 OpenSpec change `standardize-websocket-plugin` 为事实源，目标是在不引入 STOMP/SockJS/Socket.IO 的前提下，将裸 WebSocket 插件整理为标准、可测、可诊断、生产可用的实现。

## 目标

- 标准化握手认证、Origin、subprotocol、token/appId/clientId 校验路径。
- 标准处理 Ping/Pong/Binary/Text/Close 等 WebSocket 消息类型。
- 明确业务消息契约：文本 JSON，必须包含非空 `type`，分发给同 appId 下启用的 `ws-subscribe.businessId`。
- 区分可恢复业务错误与不可恢复协议/策略错误。
- 保证 `init` 和 `close` 生命周期脚本幂等、顺序清晰。
- 保持点对点推送、全量广播、多 session 和 Redis 集群广播能力不退化。
- 补齐测试与 README 对接说明。

## 非目标

- 不实现 STOMP、SockJS、Socket.IO、MQTT 或客户端 SDK。
- 不把 BinaryMessage 暴露给 MagicAPI 脚本。
- 不引入新的持久化会话存储。
- 不改 MagicAPI 核心资源模型。

## 采用方案

采用“协议标准化内核 + 兼容层”：

1. 协议标准化内核负责握手、消息类型判定、控制帧、close code、错误响应、会话注册和清理。
2. 业务兼容层保留 `{type: ...}` 文本 JSON 业务消息入口，保留文本 `ping` → `pong` 的轻量兼容。
3. 当历史行为与 WebSocket 标准冲突时，优先改为标准语义。

## 处理流程

### 握手与注册

- 在握手拦截或连接建立初期读取配置参数名对应的 `token` 与 `clientId`。
- 校验 token，解析 subject 作为 appId，并确认 appId 对应 `ws-datasource` 存在。
- 未通过校验的连接不得注册 session，不得执行 `init` 脚本。
- 注册成功后写入 session attributes：appId、clientId、sessionId、生命周期状态。
- 注册成功后执行同 appId 下启用的 `init` 脚本一次。

### 消息分发

消息处理按类型分流：

- `PingMessage`：立即发送 `PongMessage`，不进入业务脚本。
- `PongMessage`：作为连接级信号处理，不进入业务脚本。
- `BinaryMessage`：默认不支持，关闭连接，优先使用 `1003`。
- `TextMessage`：先处理文本 `ping` 兼容，再解析 JSON 业务消息。
- 其他不支持类型：按协议/不支持数据处理。

文本业务消息规则：

- 非 JSON：发送结构化错误，连接保持可用。
- 缺少非空 `type`：发送结构化错误，连接保持可用。
- 未匹配到启用脚本：发送结构化错误，连接保持可用。
- 匹配到脚本：注入 appId、clientId、sessionId、type、params 后执行。

### 错误响应

可恢复业务错误返回文本 JSON：

```json
{
  "type": "error",
  "code": "INVALID_MESSAGE",
  "message": "消息格式不正确"
}
```

建议错误码：

- `INVALID_JSON`：文本不是合法 JSON。
- `MISSING_TYPE`：缺少非空 `type`。
- `UNKNOWN_TYPE`：没有匹配的业务脚本。
- `SCRIPT_ERROR`：脚本执行失败且连接仍可继续。

不可恢复错误使用 WebSocket close status：

- `1002`：协议错误。
- `1003`：不支持的数据类型，例如 BinaryMessage。
- `1008`：策略违规，例如认证、权限、Origin、appId 校验失败。
- `1011`：服务端内部错误。
- `1012`：服务重启或应用关闭。
- `1013`：服务过载或容量限制。

### 生命周期幂等

- `init` 只在注册成功后执行一次。
- `close` 只对已注册 session 执行，且同一个 session 最多执行一次。
- 主动清理、`afterConnectionClosed`、缓存驱逐、应用 shutdown 都走统一清理方法。
- 清理方法负责移除 send lock、session attributes、client session list，并触发 close 脚本去重。

### 推送与集群

- `broadcastMessageToClient(clientId, message)` 向目标 clientId 下所有有效 session 发送。
- `broadcastDistributedMessageToAllClient(message)` 向目标 appId 下所有有效 session 广播。
- Redis 集群广播继续携带 serverId，接收端跳过自身来源消息。
- 发送仍按 session 串行化，避免并发 `sendMessage` 破坏底层 WebSocketSession 状态。

## 关键取舍

- 保留文本 `ping` 是兼容行为，不作为标准心跳主路径。
- Binary 默认拒绝，避免扩展脚本上下文和大消息处理复杂度。
- 业务错误默认不关闭连接，提高前端调试体验；协议/策略错误严格关闭。
- 不新增大批配置项，优先收敛为标准行为，减少配置组合风险。

## 测试策略

- 使用现有 JUnit/Mockito 风格扩展 `BaseDynamicWebSocketHandlerTest`。
- 覆盖标准控制帧、文本 ping、Binary 拒绝、非法 JSON、缺少 type、未知 type、脚本异常、关闭脚本幂等。
- 覆盖 Origin、subprotocol、Redis handler 类型和 token 密钥配置相关测试。
- 覆盖多 session 推送和 Redis 自身消息跳过的可测路径。
- 最终运行 `mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test`。

## 实施顺序

1. 先补充协议契约测试，确保行为目标可验证。
2. 重构 handler 消息分发和错误/关闭语义。
3. 统一 session 生命周期清理路径。
4. 审查握手/注册配置并修正不标准行为。
5. 更新 README 对接说明和排查信息。
6. 跑 Maven 验证并根据失败做最小修复。

## 需注意的兼容点

- 浏览器 API 不能主动发送标准 Ping 控制帧，浏览器客户端仍可使用文本 `ping` 兼容或依赖服务端/容器心跳。
- 如果旧客户端发送 BinaryMessage，本次会按不支持数据关闭，这是标准化整改的一部分。
- 如果旧客户端依赖重复 close 脚本，本次会改为一次，这是生命周期语义修正。
