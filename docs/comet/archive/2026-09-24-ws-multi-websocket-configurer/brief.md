# 目标

让 WS 地基在「宿主自己也在注册 WebSocket 端点」的场景下成立：同一应用内允许多个 WebSocket 端点注册来源并存，webmvc 与 webflux 两种栈都支持（两者不会同时出现在一个应用里），并且一份配置就能把同一套 WS 处理器挂到多条端点路径上。

完成后：宿主可以自行 `@EnableWebSocket` 注册自己的 `WebSocketConfigurer`，或自带 `SimpleUrlHandlerMapping`，地基的 `/ws/api` 端点与宿主的端点并存生效；`fw.plus.ws.websocket.paths` 与多个 `WsBasePathProvider` 让同一套握手鉴权、会话管理与广播机制服务多个入口。

# 范围

1. **webmvc 多路径与多基础路径**
   - `WsEndpointRegistrar` 的注册矩阵从「单 basePath × 单 relativePath」扩展为「全部 `WsBasePathProvider` ×（`websocket.path` + `websocket.paths`）」，去重后逐条注册，规则收在 `WsEndpointPathResolver`。
   - 基础路径候选由 `ObjectProvider#getIfAvailable()` 改为有序集合（`@Primary` 优先，其余按 `@Order`/`Ordered` 排序）：容器内存在多个候选时现状直接抛 `NoUniqueBeanDefinitionException`。
   - 跨域、子协议与握手拦截器对每条路径同等生效；单条路径注册失败只记录日志，不影响其余路径。
2. **webmvc 多 `WebSocketConfigurer` 实例**
   - 地基注册器不加 `@ConditionalOnMissingBean(WebSocketConfigurer.class)` 独占，保持由 Spring `DelegatingWebSocketConfiguration` 统一收集容器内全部实例。
   - 宿主可自行实例化 `WsEndpointRegistrar`（公开类）或写自己的 `WebSocketConfigurer`，与地基注册器并存生效；新增测试证明两条路径都能注册并完成握手升级。
3. **webflux 等价能力**
   - `WsWebFluxAutoConfiguration` 按同一套矩阵展开 `websocket.paths` 与多个 `WsBasePathProvider`，复用 `WsEndpointPathResolver`，删除本地 `/ws/api` 回落，与 webmvc 口径统一。
   - 保持自有 `SimpleUrlHandlerMapping`（webflux 侧 Spring 不提供收集器，宿主自带 mapping 与本品由 order 决定先后），Bean 名与 order 不变。
4. **在途收尾**：`spring.factories` 与 `AutoConfiguration.imports` 只登记 `WsAutoConfiguration`；`WsRedisConfiguration`、`WsWebFluxAutoConfiguration` 上冗余的 `@ConditionalOnProperty` 删除；广播频道改名 `fw-plus:websocket:broadcast`。
5. **文档与规格**：starter README、magic-api ws 插件 README、总 Spec 与本 change 目标 Spec 同步。

# 非目标

- 不构造 webmvc 与 webflux 同时出现的场景（用户已明确一个应用只会是其中一种栈）。
- 不代宿主开启 `@EnableWebSocket` / `@EnableWebSocketMessageBroker`：webmvc 栈由宿主启用，地基只保证自己的注册器与宿主的任意多个实例并存。
- 不新增 SPI，`WsBusinessDispatcher`、`WsBasePathProvider` 的签名不变；不改 magic-api ws 插件代码。
- 不改单会话顺序、广播语义、握手鉴权与连接上限等既有行为；仍然不做分布式会话表。
- 不让 `websocket.basePath`、`websocket.endpoint` 这两个占位键参与注册。

# 验收示例

- **A1｜Scenario**：webmvc 多路径 —— 配 `websocket.path=/ws/api` 加 `websocket.paths=[/ws/api2, /ws/api]` 时，去重后两条路径都注册到同一处理器，跨域与子协议设置与改造前逐条一致。
- **A2｜Scenario**：webmvc 多基础路径 —— 容器内存在两个 `WsBasePathProvider`（如 `/magic-api`、`/bff`）时，每个 basePath 与每条相对路径都注册，且不再抛 `NoUniqueBeanDefinitionException`。
- **A3｜Scenario**：webmvc 多 configurer —— 宿主自带 `@EnableWebSocket` 并注册自己的 `WebSocketConfigurer`（路径 `/host/echo`）时，宿主路径与地基 `/ws/api` 同时注册、互不覆盖，真实 Tomcat 下两条路径都能完成 HTTP upgrade 握手。
- **A4｜Scenario**：webflux 多路径与多基础路径 —— `websocket.paths` 与多个 `WsBasePathProvider` 在响应式栈展开为同一组路径，`SimpleUrlHandlerMapping` 的 urlMap 与 webmvc 注册结果一致；宿主自带 mapping 时两者由 order 决定先后、互不覆盖。
- **A5｜Scenario**：两端路径口径统一 —— 主路径显式置空串时两端一致地只按 basePath 注册（webflux 的 `/ws/api` 本地回落已删除），拼接规则共用 `WsEndpointPathResolver`。
- **A6｜Scenario**：登记与开关收敛 —— `spring.factories` 与 `AutoConfiguration.imports` 只有 `WsAutoConfiguration` 一项；`WsRedisConfiguration` 与 `WsWebFluxAutoConfiguration` 经 `@Import` 生效且身上无冗余 `@ConditionalOnProperty`；`fw.plus.ws.enabled=false` 时三类 WS Bean 全部不装配。
- **A7｜Scenario**：频道与文档 —— `WsBaseService#getBroadcastChannelName()` 为 `fw-plus:websocket:broadcast`，README（starter 与 magic-api ws 插件）记录多路径、多基础路径与多 configurer 用法，自动配置模块 ws 包测试全绿（跳过项仅限依赖真实 Redis 的用例）。

# 约束与不变量

- JDK 1.8、UTF-8；`WsConfig.Websocket` 保留 `path` 默认 `/ws/api`，新增 `paths` 默认空列表，`flatten()` 键集合仅新增 `websocket.paths`。
- `WsBaseService` 全部 getter 名与返回类型不变，`getWebsocketPath()` 仍是主路径事实来源。
- `WsEndpointRegistrar` 保持公开可实例化，构造器签名不变，宿主可自行构造为 `WebSocketConfigurer` Bean。
- 未配 `paths`、容器内只有一个 `WsBasePathProvider`（或没有）时，注册结果与改造前逐字节等价。
- 一个应用只会是 servlet 或 reactive 一种栈，测试按栈分别编写，不构造双栈共存的上下文。

# 决策

- **D1 语义定位**：把「多个 `WebSocketConfigurer` 实例」实现为「多个端点注册来源并存 + 一份配置挂多条路径」，而不是由地基去开 `@EnableWebSocket`：前者与 Spring 的收集语义一致，也不会与 `@EnableWebSocketMessageBroker` 冲突。
- **D2 多基础路径**：多个 `WsBasePathProvider` 视为多个端点 basePath 展开，而不是只取 primary；`@Primary` 语义保留，其余按 `@Order`/`Ordered` 排序后注册，`LinkedHashSet` 去重保证同一完整路径只注册一次。
- **D3 webflux 映射器**：继续使用自有的 `SimpleUrlHandlerMapping`，不自造收集器；宿主自带 mapping 与本品通过 order 共存。
- **D4 口径统一**：删除 webflux 本地 `/ws/api` 回落，统一走 `WsEndpointPathResolver`；显式置空主路径属于配置错误，两端行为一致（只在有 basePath 时挂到 basePath 上）。
- **D5 流程**：沿用 Native change `ws-multi-websocket-configurer`（worktree 隔离，基线 dev），在途的登记收敛、开关收敛、频道改名一并纳入本 change 提交，主工作区其它在途改动不进入本 change。
- **D6 收尾**：归档后按用户此前授权把 change 分支本地合并回 dev 并提交推送。

# 待解决问题

- webmvc 栈目前仍要求宿主自行 `@EnableWebSocket`（或 `@EnableWebSocketMessageBroker`），否则 `WebSocketConfigurer` Bean 不会被 Spring 收集，端点不生效；本次不代开，只在 README 注意事项写明，是否改由地基兜底另立 change。

# 验证预期

- `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.**.*Test'` 全绿；阶段结束跑模块全量测试回归。
- 需新增或扩充的测试：路径矩阵（多路径 + 多 basePathProvider）两端各一组；webmvc 双 configurer 同时注册；`WsEndpointHttpUpgradeTest` 扩充多路径 upgrade；webflux 映射 urlMap 与宿主 mapping 共存。
- 依赖真实 Redis 的用例（`WsRedisBroadcastEndToEndTest`）未设环境变量时整类跳过，不得把跳过算作通过。
