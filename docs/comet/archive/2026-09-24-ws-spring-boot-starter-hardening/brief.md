# 目标

把 `fw-plus-ws-spring-boot-starter` 从"能跑的内部模块"提升到可上生产的水平：修复 6 个 P0 缺陷，消除代码规范问题，并补齐二进制消息与 webmvc/webflux 双栈支持。

实现不在 starter 内（starter 只有 pom.xml，thin starter 符合项目惯例），全部位于
`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/web/ws/`（13 类 2421 行）。

完成后：WebSocket 地基可作为生产级长连接通道使用，且 webmvc 与 webflux 两种栈行为一致。

# 范围

1. **P0 缺陷修复**
   - A 心跳/误杀：`isSessionValid()` 只看 `lastPingTime`，仅 Ping/Pong 帧刷新；Guava Cache 驱逐回调会主动关闭仍 open 的会话并返回 SERVICE_OVERLOAD。
   - B shutdown hook：构造期注册 `Runtime.addShutdownHook`，无法注销、重复累积。
   - C 消息大小：未设文本/二进制帧上限。
   - D 连接数：无全局上限，单个 clientId 下会话数无上限。
   - F JWT 密钥缓存永不失效，`@RefreshScope` 刷新后仍用旧密钥。
   - G 广播同步发送：Redis 监听线程同步遍历全部会话发送，慢客户端阻塞广播通道。
2. **规范性重构**：`WsConfig` 由 `extends HashMap<String,Object>` 改为类型安全嵌套 POJO；拆分 `AbstractWebSocketHandler`（1104 行）上帝类；Redis 监听容器抽离并用构造器注入；清理误用的 jjwt 内部 API 与全限定名内联；删除死配置；修正拼写与命名。
3. **二进制消息支持**：现状 `BinaryMessage` 直接 `close(NOT_ACCEPTABLE)`，改为可配置支持，走与文本相同的订阅派发链路。
4. **webmvc + webflux 双栈**：引入与传输无关的 `WsSession` 抽象，webmvc 与 webflux 各提供适配器；webflux 侧从零实现（全仓当前无响应式 WebSocket 代码）。
5. **外围**：starter README、补全 `AutoConfiguration.imports`、新增样例模块。

# 非目标

- 不改动 magic-api ws 插件（`magicapi/plugin/ws/`）的业务语义：订阅存储、脚本内容、控制台、动态注册。
- 不新增分布式会话存储：Redis 仍保持 pub/sub 广播模型，不改造为分布式会话表。
- 不做连接迁移或粘性会话。
- 不迁移其它模块的配置类，本次只改 `WsConfig`。
- 不改动 `spring.factories` 中已有的 14 项自动配置注册（只增补 Ws 到 imports）。
- 不改 `fw-plus-ws-spring-boot-starter` 的 thin starter 形态（保持只有 pom.xml）。

# 验收示例

- **A1｜Scenario**：WsConfig POJO 绑定 —— `WsConfig` 为类型安全嵌套 POJO，`fw.plus.ws.*` 下既有 21 个键（token.secret、websocket.path、session.cache.expireMinutes…）与新增键均可绑定；`flatten()` 产出的点号扁平键集合与改造前逐键一致。
- **A2｜Scenario**：插件侧零改动 —— `WsBaseService` 现有全部语义 getter 方法名与返回类型不变；magic-api ws 插件的 `BaseService`/`MagicAPIWsConfiguration`/`WsModule`/`MagicWsController`/`BaseDynamicWebSocketHandler` 能取到与改造前相同的配置值。
- **A3｜Scenario**：长连接不被误杀 —— 只发送业务文本消息（从不发 ping）的会话，超过 `session.cache.expireMinutes` 后仍为有效状态，服务端不主动关闭、不返回 SERVICE_OVERLOAD。
- **A4｜Scenario**：生命周期清理 —— 应用关闭通过 Spring `DisposableBean` 清理会话；同一 JVM 内重复创建 handler 不累积 JVM shutdown hook，代码中无 `Runtime.addShutdownHook`。
- **A5｜Scenario**：帧大小限制 —— 握手成功后会话设置了文本与二进制消息大小上限（默认 65536），超限帧被拒绝。
- **A6｜Scenario**：连接数上限 —— 会话总数达 `session.maxTotalSessions` 或单 clientId 达 `session.maxSessionsPerClient` 时，新握手被拒绝并返回 SERVICE_OVERLOAD。
- **A7｜Scenario**：密钥热更新 —— `fw.plus.ws.token.secret` 变更后 JWT 校验立即使用新密钥，无需重启应用。
- **A8｜Scenario**：广播异步开关 —— `fw.plus.ws.broadcast.async` 默认 false，行为与改造前一致（同步）；置 true 时 Redis 触发的本地广播提交到有界线程池，队列满时丢弃并告警，不阻塞监听线程。
- **A9｜Scenario**：二进制消息 —— `fw.plus.ws.binary.enabled=true` 时二进制帧按 `fw.plus.ws.binary.type`（默认 binary）进入订阅派发链路，上下文可标识 BINARY 并取到 byte[]；置 false 时维持 NOT_ACCEPTABLE。
- **A10｜Scenario**：webmvc 行为回归 —— `AbstractWebSocketHandler` 公共方法签名与报文契约（UNKNOWN_TYPE / INVALID_JSON / SCRIPT_ERROR / ping-pong）保持不变，既有 webmvc 测试全部通过。
- **A11｜Scenario**：webflux 双栈 —— `@ConditionalOnWebApplication(type=REACTIVE)` 下注册响应式 WebSocket 端点，能完成握手鉴权、文本与二进制收发、定向发送与广播。
- **A12｜Scenario**：webflux 脚本非阻塞 —— webflux 栈下订阅脚本提交到独立有界线程池执行，不阻塞 Reactor event loop。
- **A13｜Scenario**：统一会话抽象 —— `WsInvocationContext.getSession()` 返回 `WsSession`，提供 `getAttributes()`/`sendText`/`sendBinary`/`close`/`isOpen`；webmvc 与 webflux 各有适配器实现。
- **A14｜Scenario**：自动配置清单一致 —— `AutoConfiguration.imports` 包含 `WsAutoConfiguration`、`WsRedisConfiguration`，与 `spring.factories` 不冲突。
- **A15｜Scenario**：清理与命名 —— 死配置 `getErrorCode`/`getWebsocketEndpoint`/`getWebsocketBasePath` 已移除；`getInitBusinessType`/`getCloseBusinessType` 可用且旧拼写保留 `@Deprecated` 委托；无 `io.jsonwebtoken.lang.Collections` 等第三方内部 API 依赖。
- **A16｜Scenario**：外围产物 —— starter 提供 README（对齐 centrifuge 章节）；`fw-plus-sample` 下新增可编译的 ws 样例模块并在 `fw-plus-boot-samples/pom.xml` 注册。

# 约束与不变量

- JDK 1.8 语法、UTF-8、Tab 缩进；不改 Maven 依赖版本（`org.ssssssss` 组依赖禁止升版）。
- Spring Boot 2.7.18 / Spring Framework 5.3.6：`WebSocketSession` **没有** `setMaxTextMessageBufferSize()`（6.0 才有），只能用 `setTextMessageSizeLimit()` / `setBinaryMessageSizeLimit()`。
- `AbstractWebSocketHandler` 必须保留并委托（不删除、不搬家）：`broadcastMessageToClient(String,String,String)`、`broadcastMessageToClientLocalOnly(...)`、`broadcastMessageToAllClientLocalOnly(...)`（保持包私有可覆写）、`getWebSocketServerId()`、`getConfig()`、`businessDispatcher` 字段 + `@Autowired(required=false)` setter + `protected dispatcher()`、单参构造器 `(WsConfig)`。
- `WsBaseService` 全部 getter **方法名与返回类型**保留，尤其 `getDefaultKey()`（`MagicAPIWsConfiguration:141` 在用）与 `getTokenExpireHours()`。
- 默认值回归安全：`broadcast.async=false`、`session.idleScanEnabled=false`、`session.closeOnCacheEviction=false`（置 true 可恢复旧行为）、`binary.enabled=true`。
- webmvc 栈除 P0 修复外，对外报文与行为保持不变。

# 决策

- **D1 流程**：使用 Native 工作流；因 `current-change.json` 指向 classic / `mcp-spring-boot-starter`，不得转换，新建独立 Native change `ws-spring-boot-starter-hardening`。
- **D2 工作区**：新 worktree（`.worktrees/ws-spring-boot-starter-hardening`，分支 `comet/ws-spring-boot-starter-hardening`，目标分支 dev）。原因：主目录有 118 个未提交文件（含 mcp change 在途改动），必须隔离。
- **D3 改造深度**：P0 修复 + 规范性重构一并做（`WsConfig` POJO 化、拆分上帝类、清理死配置与命名）。
- **D4 二进制**：新增支持，默认 `binary.enabled=true`；`WsInvocationContext` 增加 `MessageKind` 与 `byte[] binaryBody`，保留原构造器。
- **D5 双栈**：本次一并实现 webmvc + webflux，`WsInvocationContext.getSession()` 返回类型改为统一抽象 `WsSession`（用户确认接受此 SPI 变更，外溢面仅 `BaseDynamicWebSocketHandler:111-112` 两行 + 1 个测试）。
- **D6 webflux 脚本调度**：提交到独立有界线程池，不阻塞 Reactor event loop。
- **D7 广播异步**：默认同步，留 `broadcast.async` 开关。理由：避免端到端测试 flaky 与跨会话时序变化。
- **D8 外围**：补 starter README + `AutoConfiguration.imports` 注册 + 新增样例模块。
- **D9 不拆分 Supervisor**：webflux 与核心抽象强耦合、需反复修改同一核心区域，用单个 Native Change 顺序推进。

# 待解决问题

无。

# 验证预期

- 每阶段以 `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test` 全绿为准；JDK 8、UTF-8、Tab。
- `WsRedisBroadcastEndToEndTest` 依赖环境变量 `FW_WS_TEST_REDIS_HOST/PORT`，未设则整类跳过 —— 不得把跳过误判为通过。
- 需新增的测试：`WsConfigBindingTest`（绑定与扁平键等价，覆盖最高风险点）、二进制帧派发与超限拒绝、webflux 握手与收发、长连接不误杀、连接数上限拒绝、密钥热更新。
- 不再新增对被测类私有成员的反射；改造后协作对象通过 protected 构造器或访问器注入桩件。
- 阶段顺序：清理 → WsConfig POJO → WsBaseService 演进 → Redis 装配抽离 → 会话/鉴权层抽取（含 P0 A/B/D/F）→ 二进制 + P0 C → 广播异步 + 消息路由 → 双栈 webflux → README 与样例。
