# fw-plus WebSocket 能力规格

`fw-plus-ws-spring-boot-starter` 提供与业务无关的 WebSocket 地基：端点注册、会话管理、心跳、定向发送与跨实例广播。业务语义（哪些订阅生效、脚本如何执行）由宿主通过 SPI 注入；未注入时退化为纯 WebSocket 通道。webmvc 与 webflux 两种栈行为一致。

## 模块结构

Starter 为 thin starter，仅含 `pom.xml`。实现位于 `fw-plus-boot-autoconfigure` 的 `com.xiongx9527.fw.plus.boot.autoconfigure.web.ws` 包，分包：`config/`、`common/`、`session/`、`broadcast/`、`handler/`、`spi/`、`webmvc/`、`webflux/`、`redis/`、`starter/`。

## 配置项（`fw.plus.ws`）

| 键 | 默认 | 说明 |
| --- | --- | --- |
| `enabled` | `true` | 总开关 |
| `appId` | `default` | 默认实例 appId |
| `servers` | 空 | 命名实例列表，每项含 `key`、`path`、`paths` 与 `appId`；不进入 `flatten()`，由 `WsInstanceResolver` 按请求路径解析 |
| `path` | `/ws/api` | 默认实例主端点路径，相对路径或含基础路径的完整路径均可 |
| `paths` | 空 | 默认实例追加端点路径，与 `path` 合并去重后全部注册 |
| `websocket.trustedDomain` | 空 | 受信任来源，逗号分隔；空表示不允许跨域 |
| `websocket.subProtocols` | 空 | 子协议，逗号分隔 |
| `websocket.supportsPartialMessages` | `false` | 是否支持部分消息 |
| `param.clientId` | `clientId` | 握手 query 参数名 |
| `param.maxClientIdLength` | `50` | clientId 最大长度 |
| `init.type` / `close.type` | `init` / `close` | 建连/断连订阅业务类型 |
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

端点键只有顶层 `path` / `paths` 一个来源，与命名实例 `servers[].path` / `servers[].paths` 同形；`websocket:` 下只保留 `trustedDomain`、`subProtocols`、`supportsPartialMessages` 三个握手键。旧键 `websocket.path`、`websocket.paths`、`websocket.endpoint`、`websocket.basePath` 已从配置类删除，写入时既不生效也不告警，端点按顶层键取值（缺省 `/ws/api`）。

## 连接

握手阶段从 query 解析 clientId：缺失 clientId、clientId 超长时以 `CloseStatus.POLICY_VIOLATION` 拒绝。会话数超限时以 `SERVICE_OVERLOAD` 拒绝。

会话归属的 appId 由握手请求路径经 `WsInstanceResolver` 解析实例得到：命中实例且实例填写了 `appId` 时取实例值，否则回落 `fw.plus.ws.appId`。

握手成功后设置文本与二进制帧大小上限。

## 会话管理

会话注册表为唯一持有者，按 clientId 聚合多个会话。`SessionInfo` 记录 appId、clientId、创建时间与 `lastActiveTime`。

**活跃判定**：收到任何文本帧、Ping、Pong 都刷新 `lastActiveTime`；空闲阈值取 `session.cache.expireMinutes`。缓存驱逐默认**不**主动关闭仍 open 的会话（可由 `closeOnCacheEviction` 恢复旧行为）。`idleScanEnabled` 开启后由定时扫描兜底回收不发 FIN 就掉线的连接。

资源清理通过 Spring `DisposableBean` 完成，不注册 JVM shutdown hook。

## 消息

文本帧：按 `type` 字段路由。非 JSON 返回 `INVALID_JSON`；缺 `type` 返回 `MISSING_TYPE`；无匹配订阅返回 `UNKNOWN_TYPE`；脚本异常返回 `SCRIPT_ERROR`。`type=ping` 回 `pong`；原生 Ping 回 Pong。

二进制帧：`binary.enabled=true` 时按 `binary.type` 进入与文本相同的订阅派发链路，上下文 `MessageKind=BINARY` 并携带 `byte[]`；`false` 时维持 `NOT_ACCEPTABLE`。

## 广播

`broadcastMessageToClient` 先投递本地会话，再经抽象通道做跨实例分发。Redis 存在时经 `redis.channel.broadcast` 发布，接收方以 serverId 排除自身、只做本地投递，避免放大循环。

本地投递在 `broadcast.async=true` 时提交到有界线程池（队列满丢弃并告警），默认同步。单个 sessionId 内发送始终串行，保证单会话消息顺序。

## 双栈

`WsSession` 是与传输无关的会话抽象，提供 `getId()`、`isOpen()`、`getAttributes()`、`sendText(String)`、`sendBinary(byte[])`、`close(CloseStatus)`。webmvc 适配器包装 `org.springframework.web.socket.WebSocketSession`，webflux 适配器包装 `org.springframework.web.reactive.socket.WebSocketSession`。

webmvc 栈：`WsEndpointRegistrar implements WebSocketConfigurer`，在 `@ConditionalOnWebApplication(type=SERVLET)` 下生效。

webflux 栈：注册响应式 `WebSocketHandler` 与 `SimpleUrlHandlerMapping`，在 `@ConditionalOnWebApplication(type=REACTIVE)` 下生效。订阅脚本提交到独立有界线程池，不阻塞 Reactor event loop。

## 多端点与多注册来源

端点注册矩阵 = 全部 `WsBasePathProvider` 的 basePath ×（`path` + `paths`）。默认实例取顶层端点键，命名实例取各自 `path` / `paths`，两者叠加去重后进入同一矩阵。两条栈按同一份配置展开出同一组完整路径。

相对路径：主路径在前，`paths` 按配置顺序追加；逐项去空白并补前导斜杠，`LinkedHashSet` 保序去重。顶层 `path` 显式置空串时不补 `/ws/api`，保留改造前语义：只按基础路径注册（含只在基础路径后追加一个连接斜杠的口径）。基础路径：容器内 `WsBasePathProvider` 候选全部纳入，`@Primary` 优先，其余按 `@Order` 或 `Ordered` 排序；无候选时按无 basePath 处理。连接处斜杠的拼接由 `WsEndpointPathResolver.joinBasePath` 统一处理。

webmvc 逐条 `registry.addHandler`，跨域、子协议与握手拦截器对每条路径同等生效；单条路径注册失败只记录日志，不影响其余路径。webflux 把全部路径写入同一个 `SimpleUrlHandlerMapping` 的 urlMap，Bean 名与 order（`Ordered.LOWEST_PRECEDENCE - 100`）保持稳定。

多注册来源：webmvc 侧地基注册器不独占 `WebSocketConfigurer`，由 Spring `DelegatingWebSocketConfiguration` 收集容器内全部实例；宿主可自行实例化 `WsEndpointRegistrar`（公开类）或写自己的 `WebSocketConfigurer`，与地基注册器并存生效，也可用同名 Bean `wsEndpointRegistrar` 整体覆盖地基注册。webflux 侧宿主自带 `SimpleUrlHandlerMapping` 时与地基 mapping 由 order 决定先后，互不覆盖。一个应用只会是 servlet 或 reactive 一种栈，不构造双栈共存的上下文。

## 命名实例：一个 key + 一组端点 + 一个 appId

`fw.plus.ws.servers` 是命名实例列表，每个实例 = 一个 key + 一组端点 + 一个 appId，配置形态与 `fw-plus.ai.mcp.servers[]` 同构。多个应用共用一套地基时，各实例只认自己路径上的 appId，互不串用；列表在默认实例之外做纯增量，不配时行为与改造前完全一致。

命名实例未显式配置 `appId`（留空）时继承顶层 `fw.plus.ws.appId`；显式配置时取实例值。

启动期由 webmvc `WsAutoConfiguration` 与 webflux `WsWebFluxAutoConfiguration` 在装配路径上各调用一次同一校验，因此不依赖宿主 `@EnableWebSocket` 也能启动即失败。出现以下情形抛 `IllegalArgumentException`：实例 `key` 缺失、空串或纯空白；`key` 在 `servers` 内 trim 后重复；实例端点（`path` 或 `paths`，规范化后）与默认实例端点（`path` + `paths`）或另一实例端点重复；实例只配 `key` 未配任何端点。错误信息含配置键路径（如 `fw.plus.ws.servers[].key`）与冲突双方标识。同一实例内部 `path` 与 `paths` 的重复仍按配置顺序去重，不算冲突。旧键 `instances` 与实例内 `name` 已移除，YAML 继续书写时按 Spring 绑定默认（`ignoreUnknownFields=true`）静默忽略，不产生实例端点；旧端点键 `websocket.path` / `websocket.paths` 同样被忽略，端点只认顶层键。

解析由 `WsInstanceResolver` 完成，纯静态、无 Spring 依赖，webmvc 握手与 webflux 握手共用同一套规则。按握手请求的完整路径（含基础路径）依次匹配：

1. 精确命中：规则 `path` 规范化后与请求完整路径完全相等；
2. 后缀命中：请求路径以规则 `path` 结尾且长度严格更长（规则路径本身带斜杠，`/my-api` 不会命中 `/api`）；
3. 未命中，或命中实例的 `appId` 留空：回落全局 `fw.plus.ws.appId`。

实例端点可写相对路径（靠后缀边界命中带基础路径的请求），也可直接写完整路径；精确匹配整轮优先于后缀匹配整轮，因此写在后面的完整路径实例不会被同尾缀的相对路径抢先。命中实例的 appId 由 `AbstractWebSocketHandler` / `ReactiveWsHandler` 写进 `WsSessionInfo` 与 `WsInvocationContext`，并用于 `findEnabledSubscribes(appId, businessType)` 与 Redis 广播的 appId 过滤。

实例端点同样参与注册：相对路径集合 = 默认实例（`path` + `paths`）叠加各命名实例的 `path` 与 `paths`，按「默认实例、命名实例」顺序去重，与 `WsBasePathProvider` 叉乘后逐条注册。`servers` 不进入 `WsConfig.flatten()`，扁平键保持纯标量契约。

## SPI

- `WsBusinessDispatcher`：`existsApp`、`findEnabledSubscribes`、`execute`。未注册时地基退化为纯通道。
- `WsInvocationContext`：阶段（INIT/MESSAGE/CLOSE）、appId、clientId、sessionId、businessType、`MessageKind`、`params`（文本为 JSONObject，二进制为 null）、`binaryBody`、`WsSession`、`WsReplyHandle`。
- `WsReplyHandle`：`sendText`、`sendJson`、`sendBinary`。
- `WsSubscribeScript`、`WsBasePathProvider`：订阅描述与基础路径扩展点。

## 自动装配

WS 能力在 `spring.factories` 与 `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` 中只登记 `WsAutoConfiguration` 一项。Redis 跨实例广播（`WsRedisConfiguration`）与 webflux 端点（`WsWebFluxAutoConfiguration`）由 `WsAutoConfiguration` 的 `@Import` 收编，各自条件与 `fw.plus.ws.enabled` 总开关不变：Redis 配置在存在 `StringRedisTemplate` 时生效，否则退化为单机广播；webflux 配置仅在响应式 Web 应用中生效。

## Scenario

Scenario 编号延续历史变更：A1 至 A25 为此前 change 已验收的既有场景（其中受端点键提升影响的场景已按新形态重述，行为口径不变），A26 起为端点键提升 change 新增，A33 起为本次 code-review 修复 change 新增。

- **Scenario A1**：WsConfig 为类型安全嵌套 POJO，`fw.plus.ws.*` 下 `path`、`paths` 与其余既有键均可绑定；`flatten()` 扁平键集合为 `path`、`paths` 两个端点键 + `websocket.trustedDomain`、`websocket.subProtocols`、`websocket.supportsPartialMessages` 三个握手键（端点键提升后 `websocket.` 下的四个旧键不再产出扁平键，握手 token 相关键已随鉴权移除）。
- **Scenario A2**：`WsBaseService` getter 名与返回类型不变，magic-api ws 插件取到相同配置值（历史结论：token 相关 getter 已随握手鉴权移除，magic 插件侧只做了保持编译的最小删除）。
- **Scenario A3**：只发业务文本消息不发 ping 的会话超过 `expireMinutes` 仍有效，不被关闭、不返回 SERVICE_OVERLOAD。
- **Scenario A4**：关闭走 `DisposableBean`，无 `Runtime.addShutdownHook`，重复创建 handler 不累积钩子。
- **Scenario A5**：握手后设置文本与二进制帧上限，超限帧被拒绝。
- **Scenario A6**：会话总数或单 clientId 数超限时新握手被拒并返回 SERVICE_OVERLOAD。
- **Scenario A7**：未配置 `servers` 时，端点集合、会话 appId 与订阅派发与改造前完全一致，命名实例是纯增量。
- **Scenario A8**：`broadcast.async=false` 默认同步；置 true 后走有界线程池、队列满丢弃并告警、不阻塞监听线程。
- **Scenario A9**：`binary.enabled=true` 时二进制帧按 `binary.type` 派发且上下文为 BINARY 并带 byte[]；false 时维持 NOT_ACCEPTABLE。
- **Scenario A10**：webmvc 栈公共方法签名与四类错误报文契约不变，既有 webmvc 测试全通过。
- **Scenario A11**：webflux 栈注册响应式端点，完成握手鉴权、文本与二进制收发、定向与广播。
- **Scenario A12**：webflux 栈订阅脚本在独立有界线程池执行，不阻塞 event loop。
- **Scenario A13**：`getSession()` 返回 `WsSession`，两栈各有适配器，提供 getAttributes/sendText/sendBinary/close/isOpen。
- **Scenario A14**：WS 自动配置只登记 `WsAutoConfiguration` 一项，`spring.factories` 与 `AutoConfiguration.imports` 一致；Redis 与 webflux 装配经 `@Import` 生效，与分别登记时行为一致。
- **Scenario A15**：死配置已移除，旧拼写（`websocket.path`、`websocket.paths`、`websocket.endpoint`、`websocket.basePath` 及 `instances`/`name`）直接删除字段与扁平键，不留 `@Deprecated` 兼容字段，无第三方内部 API 依赖。
- **Scenario A16**：starter 提供 README，`fw-plus-sample` 新增可编译样例并在 samples pom 注册。
- **Scenario A17**：webmvc 栈按 `paths` 与多个 `WsBasePathProvider` 展开出全部端点，每条路径的跨域、子协议与握手拦截器一致；容器内存在多个 base path provider 不再抛 `NoUniqueBeanDefinitionException`。
- **Scenario A18**：同一应用内除地基注册器外还有宿主自注册的 configurer（webmvc）或 mapping（webflux）时，双方路径都能完成注册与握手，互不覆盖；两端展开出的完整路径集合一致。
- **Scenario A19**：一个实例 = 一个 key + 一组端点 + 一个 appId。同一份配置下 `/ws/order` 与 `/ws/pay` 命中不同实例，`/ws/api` 未命中时回落 `fw.plus.ws.appId`；实例 `appId` 留空同样回落顶层 `fw.plus.ws.appId`。webmvc 与 webflux 两栈判定一致，两栈解析出的 appId 相同。
- **Scenario A20**：`servers` 不进入 `WsConfig.flatten()` 的扁平键；实例端点与默认实例端点叠加去重后一并注册，命中实例只影响会话 appId 与该 appId 的派发过滤。
- **Scenario A21**：`fw.plus.ws.servers` 可按列表绑定，每项字段为 `key`、`path`、`paths`、`appId`；旧键 `instances` 与实例内 `name` 移除后不再被绑定（Spring 默认忽略未知字段），不产生实例端点，默认实例行为不变。
- **Scenario A22**：任一实例 `key` 缺失、空串或纯空白时启动失败，错误信息含 `fw.plus.ws.servers[].key`。
- **Scenario A23**：两个实例 `key` 相同（trim 后）时启动失败，错误信息给出冲突的 key。
- **Scenario A24**：实例端点与默认实例端点或另一实例端点（规范化后）重复、或实例只配 `key` 未配任何端点时启动失败，错误信息含冲突路径与相关 key；同一实例内部 `path` 与 `paths` 重复仍按配置顺序去重、不失败。
- **Scenario A25**：starter README 与 `fw-plus-ws-spring-boot-starter-sample` 的 `application.yml` 使用 `servers`/`key` 形态，端点键在顶层，无旧键残留。
- **Scenario A26**：`fw.plus.ws.path` 可按字符串绑定、`fw.plus.ws.paths` 可按列表绑定，未配置时取内置默认 `/ws/api` 与空列表；未配 `servers` 时端点集合、会话 appId 与订阅派发和端点键提升前完全一致。
- **Scenario A27**：写入旧键 `fw.plus.ws.websocket.path`、`websocket.paths`、`websocket.endpoint`、`websocket.basePath` 时，端点取顶层 `path` 的值（未配则是内置默认 `/ws/api`），旧键既不生效、不告警也不参与取值；`Websocket` 内四个字段已删除，反射不可见。
- **Scenario A28**：`fw.plus.ws.path` 显式置空串时，默认实例端点集合退化为「只按基础路径注册」（含只在基础路径后追加一个连接斜杠的口径），与旧键置空时的表现逐条一致，webmvc 与 webflux 两栈相同。
- **Scenario A29**：`websocket:` 下只剩 `trustedDomain`、`subProtocols`、`supportsPartialMessages` 三个握手键；`flatten()` 的端点与握手键集合恰为 `path`、`paths` 与这三个键，不含任何旧键。
- **Scenario A30**：读取侧 `WsEndpointPathResolver`、`WsInstanceValidator`、`WsBaseService` 统一读顶层键，端点冲突报文为 `fw.plus.ws.path` / `fw.plus.ws.paths`；magic-api ws 插件取到的端点与握手配置值不变。
- **Scenario A31**：starter README、magicapi-plugin-ws README、样例 `application.yml` 与样例 README 一律写成顶层 `path`/`paths` + `websocket:` 只留握手参数，并标注旧键已移除及迁移写法；全仓（历史归档与测试中的负向用例除外）无旧键字样。
- **Scenario A32**：magic 侧文档不再描述连接 token 的签发与校验（握手只带 `clientId`），`ws-datasource` 的 `secret` 与 `path` 字段说明改为「仅作数据源元信息保留，不参与握手鉴权」，实例口径由「一条路径 + 一套 token」改为「一个 `servers` 实例 + 一个 appId」。
- **Scenario A33**：`fw-plus-boot-dependencies` 的 dependencyManagement 不包含已删除的 `com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mcp`；`fw-plus-boot` 层 validate/编译不再尝试解析该构件，全仓 grep `fw-plus-magicapi-plugin-mcp` 仅剩 AI 层 `fw-plus-ai-magicapi-plugin-mcp` 与 docs 归档。
- **Scenario A34**：AGENTS.md 模块树列出 `fw-plus-ai-mcp-server-sample` 与 `fw-plus-ai-chat-sample`；AGENTS.md 与 CLAUDE.md 对构建入口（根 pom + mvnw 聚合，或分层 mvnw）的描述一致，且与仓库实际结构、可执行命令相符。
- **Scenario A35**：`fw-plus-boot-autoconfigure` 的 mcp 新包主源与 `web.ws.common.WsBaseService` 的缩进风格与同包/同文件主导风格一致（mcp 包主源 Tab，`WsBaseService` 为空格）。
