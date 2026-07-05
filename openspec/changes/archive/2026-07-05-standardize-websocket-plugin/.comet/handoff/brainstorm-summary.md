# Brainstorm Summary

- Change: standardize-websocket-plugin
- Date: 2026-07-05

## 确认的技术方案

已确认：采用“协议标准化内核 + 兼容层”的方案；用户明确允许不标准行为直接整改。

核心设计：

1. 将 `BaseDynamicWebSocketHandler` 内部处理拆分为清晰阶段：握手属性读取、会话注册、控制帧处理、文本业务消息处理、错误响应、关闭清理。
2. 标准 WebSocket 行为优先：`PingMessage` 返回 `PongMessage`，`PongMessage` 只更新连接状态，`BinaryMessage` 默认 `1003` 关闭。
3. 保留必要兼容：文本 `ping` 可继续返回文本 `pong`；现有 `{type: ...}` 文本 JSON 业务消息格式继续作为脚本入口。
4. 不标准行为可以改掉：如果现有行为与标准 close code、控制帧、握手失败或消息类型处理冲突，优先按标准修正。
5. 业务错误可恢复：非法 JSON、缺少 `type`、未知 `type` 默认返回结构化错误消息 `{type:"error", code, message}`，不直接断开；协议/策略错误使用 close code 关闭。
6. 生命周期幂等：成功注册后只执行一次 `init`，无论客户端关闭、服务端清理、缓存驱逐还是 shutdown，`close` 脚本对同一 session 最多执行一次。
7. 服务端推送能力保持：`broadcastMessageToClient`、`broadcastDistributedMessageToAllClient` 和 Redis 广播语义保持不变，但内部发送和清理路径继续保证并发安全。

候选方案对比：

- 方案 A：严格标准模式。所有非法业务消息直接关闭连接。优点是协议强约束；缺点是对前端调试和旧客户端不友好。
- 方案 B：标准内核 + 兼容层。标准控制帧/关闭码明确，业务错误结构化返回，旧文本 ping 保留。优点是兼顾标准和必要兼容；缺点是需要定义错误响应格式。
- 方案 C：最小修补。只加 Ping/Pong 和 Binary 关闭。优点是改动小；缺点是无法解决生命周期、错误语义和完整性问题。

采用方案 B，但实现时遇到明显不标准的历史行为，优先改为标准语义。

## 关键取舍与风险

- 业务错误不默认关闭连接，避免前端因普通请求错误频繁重连；真正协议错误仍关闭。
- Binary 默认拒绝，不暴露给 MagicAPI 脚本，避免编码、大小限制、脚本变量设计扩大范围。
- 浏览器 WebSocket API 无法主动发送标准 Ping frame，README 必须说明浏览器侧通常依赖服务端/容器或文本 ping 兼容。
- close 脚本去重可能改变旧行为中重复下线通知的问题，但这是合理修正。
- 当前工作区已有上一轮 WS 修复和测试未提交，build 阶段必须先归因并保留这些相关改动，不能回滚。

## 测试策略

1. 单元测试优先覆盖 `BaseDynamicWebSocketHandler`：标准 Ping/Pong、文本 ping、Binary 1003、非法 JSON、缺少 type、未知 type、脚本异常、关闭脚本幂等。
2. 配置/注册测试覆盖 `MagicAPIWsConfiguration` 和 `MagicAPIWsHandlerConfiguration`：Origin 默认收紧、显式 `*`、subprotocol trim/过滤、Redis handler 类型。
3. 存储校验测试保留并扩展：`ws-datasource.key` 唯一、`ws-subscribe.businessId` 同 key 唯一、空 id 新增场景。
4. 集群路径尽量通过 Redis handler 单元测试覆盖消息来源 serverId 跳过、自身消息不重复投递；无法稳定集成 Redis 时使用 mock。
5. 验证命令优先运行 `mvn -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test`。

## Spec Patch

回写 delta spec：新增 `Structured recoverable error response` requirement，明确业务可恢复错误返回 `{type:"error", code, message}`，协议/策略错误仍使用 WebSocket close code。
