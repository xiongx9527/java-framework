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
| `tokens` | 空 | 按端点路径绑定的 token 规则，每项含 `path`、`secret` 与可选 `expireHours` |
| `websocket.path` | `/ws/api` | 主端点路径 |
| `websocket.paths` | 空 | 追加端点路径，与 `websocket.path` 合并去重后全部注册 |
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
| `redis.channel.broadcast` | `fw-plus:websocket:broadcast` | 广播频道 |
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

## 多端点与多注册来源

端点注册矩阵 = 全部 `WsBasePathProvider` 的 basePath ×（`websocket.path` + `websocket.paths`）。两条栈按同一份配置展开出同一组完整路径。

相对路径：主路径在前，`websocket.paths` 按配置顺序追加；逐项去空白并补前导斜杠，`LinkedHashSet` 保序去重。主路径显式置空串时不补 `/ws/api`，保留改造前 webmvc 语义：只按基础路径注册。基础路径：容器内 `WsBasePathProvider` 候选全部纳入，`@Primary` 优先，其余按 `@Order` 或 `Ordered` 排序；无候选时按无 basePath 处理。连接处斜杠的拼接由 `WsEndpointPathResolver.joinBasePath` 统一处理。

webmvc 逐条 `registry.addHandler`，跨域、子协议与握手拦截器对每条路径同等生效；单条路径注册失败只记录日志，不影响其余路径。webflux 把全部路径写入同一个 `SimpleUrlHandlerMapping` 的 urlMap，Bean 名与 order（`Ordered.LOWEST_PRECEDENCE - 100`）保持稳定。

多注册来源：webmvc 侧地基注册器不独占 `WebSocketConfigurer`，由 Spring `DelegatingWebSocketConfiguration` 收集容器内全部实例；宿主可自行实例化 `WsEndpointRegistrar`（公开类）或写自己的 `WebSocketConfigurer`，与地基注册器并存生效，也可用同名 Bean `wsEndpointRegistrar` 整体覆盖地基注册。webflux 侧宿主自带 `SimpleUrlHandlerMapping` 时与地基 mapping 由 order 决定先后，互不覆盖。一个应用只会是 servlet 或 reactive 一种栈，不构造双栈共存的上下文。

## 一个实例对应一条路径 + 一个 token

`fw.plus.ws.tokens` 是规则列表，每条规则 = 一条端点路径 + 一套 token 密钥（`path` + `secret` + 可选 `expireHours`）。多个应用共用一套地基时，各实例只认自己路径上的密钥，互不串用。

解析由 `WsTokenResolver` 完成，纯静态、无 Spring 依赖，webmvc 握手、webflux 握手与 magic 侧的 `ws['xxx'].generateToken()`、`/ws/datasource/test` 共用同一套规则。按握手请求的完整路径（含基础路径）依次匹配：

1. 精确命中：规则 `path` 规范化后与请求完整路径完全相等；
2. 后缀命中：请求路径以规则 `path` 结尾且长度严格更长（规则路径本身带斜杠，`/my-api` 不会命中 `/api`）；
3. 未命中，或命中规则的 `secret` 留空：回落全局 `fw.plus.ws.token.secret`，为空时抛 `IllegalStateException("fw.plus.ws.token.secret must be configured")`。

规则的 `expireHours` 缺省时继承全局 `token.expireHours`。规则可写相对路径（靠后缀边界命中带基础路径的请求），也可直接写完整路径；精确匹配整轮优先于后缀匹配整轮，因此写在后面的完整路径规则不会被同尾缀的短规则抢先。命中同一规则的请求由 `AbstractWebSocketHandler` / `ReactiveWsHandler` 使用该规则的密钥验签，`secretKeyCache` 仍以密钥字符串为键，多套密钥并存时自动区分。

magic 侧 `WsDataSourceInfo` 新增 `path` 字段：填写后签发 token 与数据源测试都按该路径解析密钥；留空表示使用全局密钥。

## SPI

- `WsBusinessDispatcher`：`existsApp`、`findEnabledSubscribes`、`execute`。未注册时地基退化为纯通道。
- `WsInvocationContext`：阶段（INIT/MESSAGE/CLOSE）、appId、clientId、sessionId、businessType、`MessageKind`、`params`（文本为 JSONObject，二进制为 null）、`binaryBody`、`WsSession`、`WsReplyHandle`。
- `WsReplyHandle`：`sendText`、`sendJson`、`sendBinary`。
- `WsSubscribeScript`、`WsBasePathProvider`：订阅描述与基础路径扩展点。

## 自动装配

WS 能力在 `spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中只登记 `WsAutoConfiguration` 一项。Redis 跨实例广播（`WsRedisConfiguration`）与 webflux 端点（`WsWebFluxAutoConfiguration`）由 `WsAutoConfiguration` 的 `@Import` 收编，各自条件与 `fw.plus.ws.enabled` 总开关不变：Redis 配置在存在 `StringRedisTemplate` 时生效，否则退化为单机广播；webflux 配置仅在响应式 Web 应用中生效。

## Scenario

Scenario 编号延续历史变更：A1 至 A16 为此前 change 已验收的既有场景，A17 起为本 change 新增。

- **Scenario A1**：WsConfig 为类型安全嵌套 POJO，`fw.plus.ws.*` 下 `websocket.path`、`websocket.paths` 与其余既有键均可绑定；`flatten()` 扁平键集合在改造前的基础上仅新增 `websocket.paths`。
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
- **Scenario A14**：WS 自动配置只登记 `WsAutoConfiguration` 一项，`spring.factories` 与 `AutoConfiguration.imports` 一致；Redis 与 webflux 装配经 `@Import` 生效，与分别登记时行为一致。
- **Scenario A15**：死配置已移除，新拼写可用且旧拼写保留 `@Deprecated` 委托，无第三方内部 API 依赖。
- **Scenario A16**：starter 提供 README，`fw-plus-sample` 新增可编译样例并在 samples pom 注册。
- **Scenario A17**：webmvc 栈按 `websocket.paths` 与多个 `WsBasePathProvider` 展开出全部端点，每条路径的跨域、子协议与握手拦截器一致；容器内存在多个 base path provider 不再抛 `NoUniqueBeanDefinitionException`。
- **Scenario A18**：同一应用内除地基注册器外还有宿主自注册的 configurer（webmvc）或 mapping（webflux）时，双方路径都能完成注册与握手，互不覆盖；两端展开出的完整路径集合一致。
- **Scenario A19**：一个实例对应一条路径 + 一个 token。同一份配置下 `/ws/order` 与 `/ws/pay` 绑定不同密钥，用对方密钥签发的 token 在两端都被拒，用本端密钥签发的 token 通过；未命中任何规则的路径回落全局 `token.secret`；规则未配 `expireHours` 时继承全局有效期。webmvc 与 webflux 两栈判定一致，magic 侧按 `ws-datasource.path` 签发与测试同一套密钥。
