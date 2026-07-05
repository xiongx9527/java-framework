## Context

WS 插件当前通过 Spring WebSocket `WebSocketHandler` 承载连接，MagicAPI 资源提供 `ws-datasource` 和 `ws-subscribe` 两类动态资源，脚本通过 `ws` 模块进行点对点或广播推送。此前已经修复过 token 密钥、跨域默认值、并发发送、Redis handler 类型、cache replacement、资源重复校验等问题，但要达到“按 WebSocket 标准对接”的目标，还需要把协议层行为和业务脚本层行为分清。

当前约束：

- Java 1.8，Spring Boot 2.7.x，优先保持现有依赖体系。
- 兼容 MagicAPI 现有脚本资源：`ws-datasource`、`ws-subscribe`、`ws` 模块。
- 不引入 STOMP 等上层协议；本次只规范裸 WebSocket 插件。
- 不强制破坏既有文本 `ping` 和 JSON `{type: ...}` 客户端约定，但标准行为优先。

## Goals / Non-Goals

### Goals

- 握手认证和来源校验前置化，失败路径可诊断且符合 Spring WebSocket 语义。
- 支持标准 Ping/Pong 控制帧，文本 `ping` 仅作为兼容路径。
- 对 Text/Binary/Ping/Pong/Close 的行为边界做清晰实现。
- 对非法消息、未知业务类型、脚本异常、容量限制和服务端关闭使用合理 close code 或结构化错误响应。
- 保证多 session、并发发送、Redis 集群广播、资源清理不会因标准化改造退化。
- 补齐关键单元测试和 README 对接说明。

### Non-Goals

- 不实现 STOMP、SockJS、Socket.IO 或 MQTT。
- 不改 MagicAPI 引擎核心。
- 不引入新持久化会话存储。
- 不做客户端 SDK。
- 不要求所有历史非标准客户端立即迁移；合理保留兼容行为。

## Decisions

### 1. 协议层与业务层分离

决定：协议层负责握手、控制帧、close code、消息类型判定和基础错误；业务层只处理合法文本 JSON 业务消息并分发到 `ws-subscribe.businessId`。

备选方案：继续在业务消息处理里混合处理 token、ping、JSON、关闭逻辑。该方案改动小，但标准语义不清，难以测试和维护。

理由：WebSocket 标准行为应先于业务脚本执行，避免脚本承担协议职责。

### 2. 标准 Ping/Pong 优先，文本 ping 兼容

决定：实现 `handleMessage` 对 `PingMessage` 返回 `PongMessage`；`PongMessage` 可记录活跃时间但不触发业务脚本；文本 `ping` 继续返回 `pong` 以兼容旧客户端。

备选方案：删除文本 `ping`。该方案更纯粹，但会破坏已有对接。

理由：标准客户端可使用控制帧，历史客户端仍可平滑迁移。

### 3. 非文本消息默认拒绝

决定：BinaryMessage 默认不进入 MagicAPI 脚本，按不支持的数据类型处理，可关闭连接或返回错误；优先使用标准关闭码 `1003`。

备选方案：把 binary 暴露给脚本。该方案需要设计脚本变量、编码和大消息策略，超出本次裸 WS 标准化范围。

理由：当前插件定位是 MagicAPI 文本业务消息，明确拒绝比隐式忽略更安全。

### 4. 业务错误默认返回结构化错误，协议错误关闭连接

决定：非法 JSON、缺少 `type`、未知 `type` 等请求层错误返回 JSON 错误消息；明显协议/策略问题（不支持消息类型、token/appId 失效、容量过载）使用 close code 关闭。脚本异常返回错误消息，必要时使用 `1011` 关闭。

备选方案：所有错误都关闭连接。该方案严格但用户体验差，业务调试成本高。

理由：区分“连接还能继续使用的业务请求错误”和“连接不应继续存在的协议/策略错误”。

### 5. 关闭脚本只执行一次

决定：通过 session attribute 标记或原子状态保证 `close` 业务脚本在 `afterConnectionClosed`、主动清理、缓存驱逐等路径中只执行一次。

备选方案：保持现状由多路径调用 `end`。该方案容易重复触发下线逻辑。

理由：关闭事件是生命周期语义，重复触发会导致业务侧状态不一致。

### 6. 配置保持轻量，优先修正默认行为

决定：本次不大规模新增配置项；如果需要开关，仅增加少量兼容性配置，例如是否保留文本 ping、非法业务消息是否关闭连接。

备选方案：为每个行为都增加配置。该方案灵活但配置复杂，容易形成不可测试组合。

理由：标准化目标是收敛行为，而不是扩大行为矩阵。

## Risks / Trade-offs

- 旧客户端依赖非标准行为 → 保留文本 `ping`，README 标记标准控制帧优先。
- 某些客户端发送 BinaryMessage → 默认 `1003` 拒绝，并在文档说明当前插件只支持文本业务消息。
- 错误响应格式可能影响前端判断 → 采用固定 `{type:"error", code, message}` 格式并补充文档。
- 关闭脚本去重可能改变重复下线通知行为 → 这是纠正不合理行为，测试覆盖主动关闭和被动关闭路径。
- Redis 集群广播路径受 close/session 清理影响 → 保留现有广播 API，增加回归测试覆盖。

## Migration Plan

1. 先补测试描述当前标准化目标，锁定现有兼容能力。
2. 调整 handler 的消息分发、控制帧、关闭码和 session 生命周期实现。
3. 调整握手拦截/注册配置，确保 token、Origin、subprotocol 行为清晰。
4. 更新 README，给出标准客户端对接和兼容行为说明。
5. 运行 autoconfigure 模块测试；必要时运行更大范围 Maven 验证。

回滚策略：本次改动集中在 WS 插件包和测试/README；如发现兼容性风险，可回滚 handler 标准化逻辑并保留文档/测试中已确认的安全修复。

## Open Questions

- 是否新增配置项控制“业务错误是否关闭连接”，默认先按设计采用“可恢复错误返回 JSON”。
- 是否在 README 中给出浏览器无法主动发送标准 Ping frame 的说明，因为浏览器 WebSocket API 不暴露 Ping 控制帧。
- 是否暴露 session 活跃时间/最后 pong 时间给脚本，本次默认不做。
