# fw-plus WebSocket 能力规格

`fw-plus-ws-spring-boot-starter` 提供与业务无关的 WebSocket 地基：端点注册、握手鉴权、会话管理、心跳、定向发送与跨实例广播。业务语义（哪些订阅生效、脚本如何执行）由宿主通过 SPI 注入；未注入时退化为纯 WebSocket 通道。webmvc 与 webflux 两种栈行为一致。

## 模块结构

Starter 为 thin starter，仅含 `pom.xml`。实现位于 `fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.web.ws` 包，分包：`config/`、`common/`、`session/`、`auth/`、`message/`、`broadcast/`、`handler/`、`spi/`、`webmvc/`、`webflux/`、`redis/`、`starter/`。

## 配置项（`fw.plus.ws`）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `token.secret` | 必填 | JWT 密钥，缺失抛 `IllegalStateException` |
| `token.expireHours` | `2` | Token 有效期 |
| `websocket.path` | `/ws/api` | 端点路径 |
| `websocket.trustedDomain` | 空 | 受信任来源，逗号分隔；空表示不允许跨域 |
| `websocket.subProtocols` | 空 | 子协议，逗号分隔 |
| `websocket.supportsPartialMessages` | `false` | 是否支持部分消息 |
| `websocket.endpoint` | `/ws/execute` | 兼容占位 |
| `websocket.basePath` | 空 | 兼容占位 |
| `param.token` / `param.clientId` | `token` / `clientId` | 握手 query 参数名 |
| `param.maxClientIdLength` | `50` | clientId 最大长度 |
| `init.type` / `close.type` | `init` / `close` | 建连/断连订阅业务类型 |
| `appId` | `default` | 默认应用 ID |
| `session.cache.maxClients` | `10000` | 会话缓存最大 clientId 数 |
| `session.cache.expireMinutes` | `30` | 会话缓存与空闲判定阈值 |
| `session.cache.concurrencyLevel` | `16` | 缓存并发级别 |
| `session.delayMillis` | `1000` | 延迟关闭毫秒数 |
| `session.maxTextMessageSize` | `65536` | 单条文本帧上限 |
| `session.maxBinaryMessageSize` | `65536` | 单条二进制帧上限 |
| `session.maxTotalSessions` | `10000` | 全局会话上限 |
| `session.maxSessionsPerClient` | `100` | 单 clientId 会话上限 |
| `session.idleScanEnabled` | `false` | 空闲会话定时扫描 |
| `session.idleScanIntervalSeconds` | `60` | 扫描周期 |
| `session.closeOnCacheEviction` | `false` | 缓存驱逐时是否主动关闭仍 open 的会话 |
| `scheduler.delayedClosePoolSize` | `2` | 延迟关闭线程池大小 |
| `redis.channel.broadcast` | `websocket:broadcast` | 广播频道 |
| `binary.enabled` | `true` | 是否接受二进制帧 |
| `binary.type` | `binary` | 二进制帧业务类型 |
| `broadcast.async` | `false` | 广播是否异步 |
| `webflux.scriptThreadPool.coreSize` | `8` | webflux 脚本线程池核心大小 |
| `webflux.scriptThreadPool.maxSize` | `32` | 最大大小 |
| `webflux.scriptThreadPool.queueCapacity` | `1000` | 队列容量 |

配置类 `WsConfig` 为类型安全嵌套 POJO。对外仍提供 `flatten()` 产出点号扁平键，供内部按既有方式读取。

## 连接与鉴权

握手阶段从 query 解析 token 与 clientId：缺失参数、clientId 超长、token 校验失败、appId 未登记时以 `CloseStatus.POLICY_VIOLATION` 拒绝。会话数超限时以 `SERVICE_OVERLOAD` 拒绝。

Token 为 JWT，取 `subject` 作为 appId；未配置 `fw.plus.ws.token.secret` 直接启动失败。密钥缓存以密钥字符串为键，配置刷新后自动失效。

握手成功后设置文本与二进制帧大小上限。

## 会话管理

会话注册表为唯一持有者，按 clientId 聚合多个会话。`SessionInfo` 记录 appId、clientId、创建时间与 `lastActiveTime`。

**活跃判定**：收到任何文本帧、Ping、Pong 都刷新 `lastActiveTime`；空闲阈值取 `session.cache.expireMinutes`。缓存驱逐默认**不**主动关闭仍 open 的会话（可由 `closeOnCacheEviction` 恢复旧行为）。`idleScanEnabled` 开启后由定时扫描兜底回收不发 FIN 就掉线的连接。

资源清理通过 Spring `DisposableBean` 完成，不注册 JVM shutdown hook。

## 消息

文本帧：按 `type` 字段路由。非 JSON 返回 `INVALID_JSON`；缺 `type` 返回 `MISSING_TYPE`；无匹配订阅返回 `UNKNOWN_TYPE`；脚本异常返回 `SCRIPT_ERROR`。`type=ping` 回 `pong`；原生 Ping 回 Pong。

二进制帧：`binary.enabled=true` 时按 `binary.type` 进入与文本相同的订阅派发链路，上下文 `MessageKind=BINARY` 并携带 `byte[]`；`false` 时维持 `NOT_ACCEPTABLE`。

日志中 token 与 secret 一律脱敏。

## 广播

`broadcastMessageToClient` 先投递本地会话，再经抽象通道做跨实例分发。Redis 存在时经 `redis.channel.broadcast` 发布，接收方以 serverId 排除自身、只做本地投递，避免放大循环。

本地投递在 `broadcast.async=true` 时提交到有界线程池（队列满丢弃并告警），默认同步。单个 sessionId 内发送始终串行，保证单会话消息顺序。

## 双栈

`WsSession` 是与传输无关的会话抽象，提供 `getId()`、`isOpen()`、`getAttributes()`、`sendText(String)`、`sendBinary(byte[])`、`close(CloseStatus)`。webmvc 适配器包装 `org.springframework.web.socket.WebSocketSession`，webflux 适配器包装 `org.springframework.web.reactive.socket.WebSocketSession`。

webmvc 栈：`WsEndpointRegistrar implements WebSocketConfigurer`，在 `@ConditionalOnWebApplication(type=SERVLET)` 下生效。
webflux 栈：注册响应式 `WebSocketHandler` 与 `SimpleUrlHandlerMapping`，在 `@ConditionalOnWebApplication(type=REACTIVE)` 下生效。订阅脚本提交到独立有界线程池，不阻塞 Reactor event loop。

## SPI

- `WsBusinessDispatcher`：`existsApp`、`findEnabledSubscribes`、`execute`。未注册时地基退化为纯通道。
- `WsInvocationContext`：阶段（INIT/MESSAGE/CLOSE）、appId、clientId、sessionId、businessType、`MessageKind`、`params`（文本为 JSONObject，二进制为 null）、`binaryBody`、`WsSession`、`WsReplyHandle`。
- `WsReplyHandle`：`sendText`、`sendJson`、`sendBinary`。
- `WsSubscribeScript`、`WsBasePathProvider`：订阅描述与基础路径扩展点。

## 自动装配

`WsAutoConfiguration` 与 `WsRedisConfiguration` 同时登记在 `spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`。Redis 配置在存在 `StringRedisTemplate` 时生效，否则退化为单机广播。

## Scenario

- **Scenario A1**：WsConfig 为 POJO，`fw.plus.ws.*` 全部键可绑定，`flatten()` 扁平键集合与改造前逐键一致。
- **Scenario A2**：`WsBaseService` getter 名与返回类型不变，magic-api ws 插件零改动取到相同配置值。
- **Scenario A3**：只发业务文本消息不发 ping 的会话超过 `expireMinutes` 仍有效，不被关闭、不返回 SERVICE_OVERLOAD。
- **Scenario A4**：关闭走 `DisposableBean`，无 `Runtime.addShutdownHook`，重复创建 handler 不累积钩子。
- **Scenario A5**：握手后设置文本与二进制帧上限，超限帧被拒绝。
- **Scenario A6**：会话总数或单 clientId 数超限时新握手被拒并返回 SERVICE_OVERLOAD。
- **Scenario A7**：`token.secret` 变更后 JWT 校验立即使用新密钥，无需重启。
- **Scenario A8**：`broadcast.async=false` 默认同步；置 true 后走有界线程池、队列满丢弃并告警、不阻塞监听线程。
- **Scenario A9**：`binary.enabled=true` 时二进制帧按 `binary.type` 派发且上下文为 BINARY 并带 byte[]；false 时维持 NOT_ACCEPTABLE。
- **Scenario A10**：webmvc 栈公共方法签名与四类错误报文契约不变，既有 webmvc 测试全通过。
- **Scenario A11**：webflux 栈注册响应式端点，完成握手鉴权、文本与二进制收发、定向与广播。
- **Scenario A12**：webflux 栈订阅脚本在独立有界线程池执行，不阻塞 event loop。
- **Scenario A13**：`getSession()` 返回 `WsSession`，两栈各有适配器，提供 getAttributes/sendText/sendBinary/close/isOpen。
- **Scenario A14**：`AutoConfiguration.imports` 含 Ws 两项且与 `spring.factories` 不冲突。
- **Scenario A15**：死配置已移除，新拼写可用且旧拼写保留 `@Deprecated` 委托，无第三方内部 API 依赖。
- **Scenario A16**：starter 提供 README，`fw-plus-sample` 新增可编译样例并在 samples pom 注册。
