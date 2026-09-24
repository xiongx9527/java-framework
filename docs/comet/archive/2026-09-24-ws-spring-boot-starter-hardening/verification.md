---
generated_from_state_version: 15
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 1
- 迭代: 2
- 验证器尝试次数: 1
- 完成时间: 2026-09-24T08:41:43.076Z
- 摘要: 本轮只修复 A16：ws 样例模块由空壳补齐为可编译的最小可用样例（3 个 Java 类 + application.yml + static/index.html + README.md），并修复了 DemoWsDispatcher 对二进制帧业务类型的匹配。Runtime 两项正式检查均通过（autoconfigure 全量测试 703 通过 0 失败 1 跳过；样例模块 Compiling 3 source files + BUILD SUCCESS），独立只读 Verifier 复核了源码、测试与回执，16 条验收项全部通过，未发现阻断 spec 的正确性、安全或行为回归问题。剩余风险集中在 Redis 端到端运行时证据缺失与 samples 聚合器缺 centrifuge 样例目录两项环境性限制。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | **A1｜Scenario**：WsConfig POJO 绑定 —— `WsConfig` 为类型安全嵌套 POJO，`fw.plus.ws.*` 下既有 21 个键（token.secret、websocket.path、session.cache.expireMinutes…）与新增键均可绑定；`flatten()` 产出的点号扁平键集合与改造前逐键一致。 | WsConfig 为类型安全嵌套 POJO（web/ws/config/WsConfig.java:24），flatten() 输出旧读取契约的全部扁平键（同文件 :67），WsConfigBindingTest 覆盖默认值、嵌套绑定与扁平键（src/test/.../web/ws/config/WsConfigBindingTest.java:24,:82）。 |
| A2 | passed | brief.md | **A2｜Scenario**：插件侧零改动 —— `WsBaseService` 现有全部语义 getter 方法名与返回类型不变；magic-api ws 插件的 `BaseService`/`MagicAPIWsConfiguration`/`WsModule`/`MagicWsController`/`BaseDynamicWebSocketHandler` 能取到与改造前相同的配置值。 | WsBaseService 语义 getter 名与返回类型未变（web/ws/common/WsBaseService.java:181,:203,:303,:312），magic-api ws 插件经 MagicAPIWsConfiguration/MagicWsController/WsModule 仍取到相同配置值（magicapi/plugin/ws/starter/MagicAPIWsConfiguration.java:141、magicapi/plugin/ws/web/MagicWsController.java:92、magicapi/plugin/ws/module/WsModule.java:89），相关测试在 703 条全量测试中通过。 |
| A3 | passed | brief.md | **A3｜Scenario**：长连接不被误杀 —— 只发送业务文本消息（从不发 ping）的会话，超过 `session.cache.expireMinutes` 后仍为有效状态，服务端不主动关闭、不返回 SERVICE_OVERLOAD。 | 入站消息刷新 lastActiveTime 与缓存计时（handler/AbstractWebSocketHandler.java:516），isSessionValid 以活跃时间判定（同文件 :986,:999），驱逐回调默认不关闭会话；回归测试 AbstractWebSocketHandlerTest.java:344。 |
| A4 | passed | brief.md | **A4｜Scenario**：生命周期清理 —— 应用关闭通过 Spring `DisposableBean` 清理会话；同一 JVM 内重复创建 handler 不累积 JVM shutdown hook，代码中无 `Runtime.addShutdownHook`。 | AbstractWebSocketHandler 实现 DisposableBean，destroy() 走 cleanupAllResources（handler/AbstractWebSocketHandler.java:188,:195）；全 fw-plus-boot 无 Runtime.addShutdownHook，重复创建 handler 不累积钩子。 |
| A5 | passed | brief.md | **A5｜Scenario**：帧大小限制 —— 握手成功后会话设置了文本与二进制消息大小上限（默认 65536），超限帧被拒绝。 | 握手成功后设置文本与二进制帧上限，默认 65536（handler/AbstractWebSocketHandler.java:403、config/WsConfig.java:184），AbstractWebSocketHandlerTest.java:239 断言上限设置；超限帧由 Spring 传输层拒绝。 |
| A6 | passed | brief.md | **A6｜Scenario**：连接数上限 —— 会话总数达 `session.maxTotalSessions` 或单 clientId 达 `session.maxSessionsPerClient` 时，新握手被拒绝并返回 SERVICE_OVERLOAD。 | 握手阶段校验总数与单 clientId 上限，超限返回 CloseStatus.SERVICE_OVERLOAD（handler/AbstractWebSocketHandler.java:468,:475），webflux 侧同样实现（webflux/ReactiveWsHandler.java:95）；测试 AbstractWebSocketHandlerTest.java:268,:288。 |
| A7 | passed | brief.md | **A7｜Scenario**：密钥热更新 —— `fw.plus.ws.token.secret` 变更后 JWT 校验立即使用新密钥，无需重启应用。 | 密钥缓存以密钥字符串为键，secret 变更后即时生效；WsConfig 带刷新能力（config/WsConfig.java:22、web/ws/common/WsBaseService.java:203）；secretRotationShouldTakeEffectWithoutRestart 通过（AbstractWebSocketHandlerTest.java:324）。 |
| A8 | passed | brief.md | **A8｜Scenario**：广播异步开关 —— `fw.plus.ws.broadcast.async` 默认 false，行为与改造前一致（同步）；置 true 时 Redis 触发的本地广播提交到有界线程池，队列满时丢弃并告警，不阻塞监听线程。 | broadcast.async 默认 false，本地广播保持同步（config/WsConfig.java:346）；置 true 时提交到有界 WsBroadcastExecutor，队列满丢弃并告警（broadcast/WsBroadcastExecutor.java:23、handler/AbstractWebSocketHandler.java:1028）；测试 AbstractWebSocketHandlerTest.java:373,:387。 |
| A9 | passed | brief.md | **A9｜Scenario**：二进制消息 —— `fw.plus.ws.binary.enabled=true` 时二进制帧按 `fw.plus.ws.binary.type`（默认 binary）进入订阅派发链路，上下文可标识 BINARY 并取到 byte[]；置 false 时维持 NOT_ACCEPTABLE。 | binary.enabled=true 时二进制帧按 binary.type 派发，上下文为 MessageKind.BINARY 且携带 byte[]（handler/AbstractWebSocketHandler.java:530、webflux/ReactiveWsHandler.java:126,:175、config/WsConfig.java:329）；置 false 维持 NOT_ACCEPTABLE；测试 AbstractWebSocketHandlerTest.java:165 与 ReactiveWsHandlerTest.java:88,:111。 |
| A10 | passed | brief.md | **A10｜Scenario**：webmvc 行为回归 —— `AbstractWebSocketHandler` 公共方法签名与报文契约（UNKNOWN_TYPE / INVALID_JSON / SCRIPT_ERROR / ping-pong）保持不变，既有 webmvc 测试全部通过。 | AbstractWebSocketHandler 公共方法签名与报文契约（UNKNOWN_TYPE / INVALID_JSON / SCRIPT_ERROR / ping-pong）未漂移（handler/AbstractWebSocketHandler.java:509,:548,:560,:691,:759），既有 webmvc 测试在全量 703 条中通过（AbstractWebSocketHandlerTest.java:49,:61,:88,:100,:215）。 |
| A11 | passed | brief.md | **A11｜Scenario**：webflux 双栈 —— `@ConditionalOnWebApplication(type=REACTIVE)` 下注册响应式 WebSocket 端点，能完成握手鉴权、文本与二进制收发、定向发送与广播。 | @ConditionalOnWebApplication(type=REACTIVE) 下注册响应式端点（webflux/WsWebFluxAutoConfiguration.java:34、ReactiveWsHandler.java:88,:159,:342），握手鉴权、文本/二进制收发、定向发送与广播均有测试覆盖（ReactiveWsHandlerTest.java:51,:88,:146,:161）。 |
| A12 | passed | brief.md | **A12｜Scenario**：webflux 脚本非阻塞 —— webflux 栈下订阅脚本提交到独立有界线程池执行，不阻塞 Reactor event loop。 | webflux 栈订阅脚本经 publishOn(Schedulers.fromExecutor(scriptExecutor)) 与 runOnScriptExecutor() 进入独立有界线程池（webflux/ReactiveWsHandler.java:70,:108,:210,:318），测试 ReactiveWsHandlerTest.java:131 断言不在 Reactor event loop 执行。 |
| A13 | passed | brief.md | **A13｜Scenario**：统一会话抽象 —— `WsInvocationContext.getSession()` 返回 `WsSession`，提供 `getAttributes()`/`sendText`/`sendBinary`/`close`/`isOpen`；webmvc 与 webflux 各有适配器实现。 | WsInvocationContext.getSession() 返回 WsSession，提供 getAttributes/sendText/sendBinary/close/isOpen（spi/WsSession.java:21、spi/WsInvocationContext.java:81），webmvc 与 webflux 各有适配器（webmvc/ServletWsSession.java:18、webflux/ReactiveWsSession.java:22）并在两侧派发中实际使用（AbstractWebSocketHandler.java:751、ReactiveWsHandler.java:165）。 |
| A14 | passed | brief.md | **A14｜Scenario**：自动配置清单一致 —— `AutoConfiguration.imports` 包含 `WsAutoConfiguration`、`WsRedisConfiguration`，与 `spring.factories` 不冲突。 | AutoConfiguration.imports 与 spring.factories 均登记 WsAutoConfiguration、WsRedisConfiguration 且互不冲突（.../META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports:15,:16 与 .../META-INF/spring.factories:17,:18），装配测试 WsAutoConfigurationTest.java:35、WsRedisConfigurationTest.java:41。 |
| A15 | passed | brief.md | **A15｜Scenario**：清理与命名 —— 死配置 `getErrorCode`/`getWebsocketEndpoint`/`getWebsocketBasePath` 已移除；`getInitBusinessType`/`getCloseBusinessType` 可用且旧拼写保留 `@Deprecated` 委托；无 `io.jsonwebtoken.lang.Collections` 等第三方内部 API 依赖。 | 死配置 getErrorCode/getWebsocketEndpoint/getWebsocketBasePath 已不存在；getInitBusinessType/getCloseBusinessType 可用且旧拼写保留 @Deprecated 委托（web/ws/common/WsBaseService.java:312,:321,:331,:342）；未见 io.jsonwebtoken.lang.Collections 等第三方内部 API 依赖。 |
| A16 | passed | brief.md | **A16｜Scenario**：外围产物 —— starter 提供 README（对齐 centrifuge 章节）；`fw-plus-sample` 下新增可编译的 ws 样例模块并在 `fw-plus-boot-samples/pom.xml` 注册。 | starter README 与 centrifuge README 同为 引入依赖/配置/使用/注意事项 结构（fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-ws-spring-boot-starter/README.md:8,:19；对照 fw-plus-centrifuge-spring-boot-starter/README.md）。ws 样例模块已从空壳补齐：pom.xml:26 引入 starter，src/main 下有 WsSampleApplication、DemoTokenController、DemoWsDispatcher 三个类与 application.yml、static/index.html，并已在 fw-plus-sample/fw-plus-boot-samples/pom.xml 的 modules 中注册。样例调用的 WsBaseService#getSecretKeyAndSignatureAlgorithm()、WsBusinessDispatcher、WsInvocationContext、WsReplyHandle 与地基真实签名一致（DemoTokenController.java:43、DemoWsDispatcher.java:41,:59）。Runtime 回执 7f0a4b65-e8bb-47d5-a6ac-43c8fa4d9e15-ws-sample-compile.log 显示 Compiling 3 source files 且 BUILD SUCCESS，不再是 No sources to compile。 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| fw-plus-boot-autoconfigure 全量测试（WS 地基与双栈） | -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test | fw-plus-boot | passed | 0 | 237807 ms |
| ws 样例模块 clean compile（A16 可编译证据） | -o -f fw-plus-sample/fw-plus-boot-samples/fw-plus-ws-spring-boot-starter-sample/pom.xml clean compile | . | passed | 0 | 6846 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- fw-plus-boot-autoconfigure 全量测试: passed — cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test：Tests run 703, Failures 0, Errors 0, Skipped 1。跳过的 1 条为依赖真实 Redis 的 WsRedisBroadcastEndToEndTest，不计入通过。日志 /tmp/ws-autoconf-test.log，耗时约 3 分 34 秒。
- ws 样例模块 clean compile: passed — ./fw-plus-core/mvnw -o -f fw-plus-sample/fw-plus-boot-samples/fw-plus-ws-spring-boot-starter-sample/pom.xml clean compile：Compiling 3 source files，BUILD SUCCESS，耗时约 28 秒。已不再是空模块（上一轮回执为 No sources to compile）。
- 已知限制: WsRedisBroadcastEndToEndTest 需要 FW_WS_TEST_REDIS_HOST/PORT 环境变量，本机未设置，故整类跳过；跨实例广播的端到端链路仍缺运行时证据。
- 已知限制: fw-plus-sample 聚合器无法整体构建：HEAD 的 fw-plus-boot-samples/pom.xml 声明了 fw-plus-centrifuge-spring-boot-starter-sample，该目录来自 dev 基线未提交状态，不在本 change 工作区；因此样例证据采用对 ws 样例模块单独 compile 的方式给出。
- 已知限制: 工作区保留了一批来自 dev 基线的未提交文件（fw-plus-centrifuge-spring-boot-starter、autoconfigure/centrifuge 等），因 HEAD 的 spring.factories 与 starters pom 已引用它们，删除会导致构建失败。
- 已知限制: 样例 application.yml 中 fw.plus.ws.websocket.trustedDomain 设为 '*' 仅为本地联调，README 已注明生产环境需改为具体域名。
- 已知限制: 样例 pom 显式跳过 test/javadoc/install/deploy，与仓库既有样例模块一致。

## 阻塞项

_无。_

## 风险与跳过的工作

- 跨实例广播仍缺真实 Redis 运行态证据：WsRedisBroadcastEndToEndTest 因未设置 FW_WS_TEST_REDIS_HOST/PORT 被整类跳过，本次全量测试中计为 Skipped 1，不能视为已通过。
- fw-plus-sample 聚合器在当前 worktree 下无法整体构建：fw-plus-boot-samples/pom.xml 声明了 fw-plus-centrifuge-spring-boot-starter-sample，该目录来自 dev 基线未提交状态、不在本工作区；A16 的样例证据以 ws 样例模块单独 compile 的回执给出。
- DemoTokenController、WsBaseService、MagicWsController 与 WsModule 使用了 jjwt 中已标记 @Deprecated 的公开 API，当前编译通过，但后续升级 jjwt 主版本需迁移。
- 本工作区保留了一批来自 dev 基线的未提交文件（fw-plus-centrifuge-spring-boot-starter、autoconfigure/centrifuge 等），因 HEAD 的 spring.factories 与 starters pom 已引用它们，不能删除。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | fail | A16 | 核对了 Runtime 两份正式检查回执与当前实现版本，时间戳与内容自洽：全量测试 703 通过 / 0 失败 / 0 错误 / 1 跳过（跳过者为依赖真实 Redis 的集成测试，未计入通过）。除 A16 外，15 条验收均可由源码、测试断言与回执证实：6 个 P0 缺陷（缓存驱逐误杀、shutdown hook、帧上限、连接数上限、密钥热更新、广播异步）、二进制支持、webmvc+webflux 双栈、统一 WsSession 抽象、自动配置清单、清理与命名均已落实，且 webmvc 报文契约逐字保持。唯一硬伤是 A16：README 与模块注册到位，但样例模块目录内没有任何源码或资源，编译回执为 No sources to compile，实质为空壳。总体判断：15 项通过、1 项失败（A16）、0 项阻塞。 | 2026-09-24T05:03:28.699Z |
| 1 | 2 | 1 | pass | — | 本轮只修复 A16：ws 样例模块由空壳补齐为可编译的最小可用样例（3 个 Java 类 + application.yml + static/index.html + README.md），并修复了 DemoWsDispatcher 对二进制帧业务类型的匹配。Runtime 两项正式检查均通过（autoconfigure 全量测试 703 通过 0 失败 1 跳过；样例模块 Compiling 3 source files + BUILD SUCCESS），独立只读 Verifier 复核了源码、测试与回执，16 条验收项全部通过，未发现阻断 spec 的正确性、安全或行为回归问题。剩余风险集中在 Redis 端到端运行时证据缺失与 samples 聚合器缺 centrifuge 样例目录两项环境性限制。 | 2026-09-24T08:41:43.076Z |



## 结论

本轮只修复 A16：ws 样例模块由空壳补齐为可编译的最小可用样例（3 个 Java 类 + application.yml + static/index.html + README.md），并修复了 DemoWsDispatcher 对二进制帧业务类型的匹配。Runtime 两项正式检查均通过（autoconfigure 全量测试 703 通过 0 失败 1 跳过；样例模块 Compiling 3 source files + BUILD SUCCESS），独立只读 Verifier 复核了源码、测试与回执，16 条验收项全部通过，未发现阻断 spec 的正确性、安全或行为回归问题。剩余风险集中在 Redis 端到端运行时证据缺失与 samples 聚合器缺 centrifuge 样例目录两项环境性限制。
