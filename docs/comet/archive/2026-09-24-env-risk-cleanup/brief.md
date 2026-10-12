# 目标

清理上一轮 `ws-spring-boot-starter-hardening` 归档时记录的三条环境性风险，把"证据缺口"和"依赖债务"变成可复现的验收事实：

1. **W1 跨实例广播无运行态证据**：`WsRedisBroadcastEndToEndTest` 依赖 `FW_WS_TEST_REDIS_HOST` / `FW_WS_TEST_REDIS_PORT`，本机与 CI 未设置时整类按 assumption 跳过，703 个测试里只计为 Skipped 1，Redis pub/sub 广播链路实际从未被执行过。
2. **W2 样例聚合器被判"无法整体构建"**：上一轮因 `fw-plus-centrifuge-spring-boot-starter-sample` 目录不在 worktree 而无法整体构建，只能给单模块回执；合并到 dev 后该目录已补齐，需要一份可复现的整体构建结论。
3. **W3 jjwt 废弃 API 债务**：ws 主代码、magic-api ws 插件、ws 样例与相关测试大面积使用 jjwt 0.12.7 已标记 `@Deprecated` 的公开 API（`signWith(SignatureAlgorithm,…)`、`setSigningKey`、`parseClaimsJws`、`ClaimsMutator` 的 setter 家族），升级 jjwt 主版本前必须迁移。

完成后：跨实例广播有一条"不需要外部 Redis 也能跑通"的进程内端到端证明；`fw-plus-sample` 聚合器有绿色整体构建回执；上述 JWT 调用全部换成 0.12.x 推荐 API，且 token 语义逐字不变。

# 范围

1. **W1 进程内端到端证明**
   - 新增进程内假 `RedisConnectionFactory`（测试代码内）：`convertAndSend` 的报文投递给已订阅该频道的 listener，从而驱动真实的 `RedisMessageListenerContainer`、其任务执行器和 `WsBroadcastMessageListener`，覆盖除网络外的全链路（发布 → 订阅 → 反序列化 → serverId 排除 → 本地投递）。
   - 现有真实 Redis 测试保留为环境可选的更深证明，未设环境变量时明确跳过并标注"未执行"。
   - 不新增 Maven 依赖，不引入 embedded-redis / testcontainers。
2. **W2 聚合器构建回执**
   - 对 `fw-plus-sample/pom.xml` 声明的 boot/cloud/ai 三组样例执行整体 `clean compile`，记录每个模块的构建结果；只补证据，除非暴露真实缺陷才改代码。
3. **W3 jjwt 迁移**
   - 主代码：`web/ws/common/WsBaseService.java`、`magicapi/plugin/ws/web/MagicWsController.java`、`magicapi/plugin/ws/module/WsModule.java`。
   - 样例：`fw-plus-ws-spring-boot-starter-sample` 的 `DemoTokenController.java`。
   - 测试：`WsBaseServiceTest`、`ReactiveWsHandlerTest`、`WsEndpointHttpUpgradeTest`、`WsRedisBroadcastEndToEndTest`、`WsHandshakeAuthenticationTest`、`AbstractWebSocketHandlerTest`。
   - `fw-plus-boot-dependencies` 中的 `jjwt.version` 保持 `0.12.7`，不做主版本升级。

# 非目标

- 不升级 jjwt 主版本，不改任何依赖版本；`org.ssssssss` 组依赖禁止升版的约束继续有效。
- 不改造 Redis 广播模型：仍是 pub/sub，不做分布式会话表、不做连接迁移或粘性会话。
- 不修改 magic-api ws 插件的业务语义（订阅存储、脚本执行、控制台、动态注册）。
- 不清理其它模块的 deprecated 调用；例如 `MyFileStorageService` 中的 `setExpiration` 属 MinIO SDK 链式调用，与 jjwt 无关，不在范围。
- 不为测试新增对被测类私有成员的反射；`WsBaseService.SecretKeyAndSignatureAlgorithm` 的字段与 getter 形状保持不变。
- 不改 `fw-plus-ws-spring-boot-starter` 的 thin starter 形态与 README。
- 不处理运行态 `resources/` 目录（当前工作区未检出）。

# 验收示例

- **A1｜Scenario**：进程内跨实例广播端到端 —— 在未设置 `FW_WS_TEST_REDIS_HOST` / `FW_WS_TEST_REDIS_PORT` 的环境下执行 `fw-plus-boot-autoconfigure` 测试，新增的进程内端到端测试类实际运行：4 个用例全部执行且通过、0 skipped，分别断言另一实例发布的广播经 Redis 容器链路投递到本地会话、定向广播恰好投递一次、排除自身 serverId 的广播不投递到本地、广播给全部客户端可到达本地会话。
- **A2｜Scenario**：真实 Redis 为可选深证 —— 设置 `FW_WS_TEST_REDIS_HOST` / `FW_WS_TEST_REDIS_PORT` 时 `WsRedisBroadcastEndToEndTest` 对真实 Redis 执行同一组断言并通过；未设置时按 assumption 整类跳过，并在验证记录中标注为"未执行"，不得计入通过数。
- **A3｜Scenario**：token 语义逐字不变 —— 32 / 48 / 64 字节密钥下签发的 token，header `alg` 依次为 `HS256` / `HS384` / `HS512`，`subject` 取 appId，过期时间按 `fw.plus.ws.token.expireHours` 生效，`validateAndExtractAppIdFromToken` 的接受 / 拒绝行为不变。
- **A4｜Scenario**：废弃 API 清零且解析链路兼容 —— `signWith(SignatureAlgorithm,…)`、`signWith(Key,SignatureAlgorithm)`、`setSigningKey`、`setSigningKeyResolver`、`parseClaimsJwt` / `parseClaimsJws` 与 `ClaimsMutator` 的 `setSubject` / `setIssuedAt` / `setExpiration` / `setNotBefore` / `setIssuer` / `setAudience` / `setId` 在 W3 列出的全部文件中 0 处残留；同时迁移前用同一密钥与相同 subject / iat / exp 生成的固定 token 仍能被同一校验链路解析出相同 appId。
- **A5｜Scenario**：样例聚合器整体构建 —— `cd fw-plus-core && ./mvnw -o -f ../fw-plus-sample/pom.xml clean compile -DskipTests` 返回 BUILD SUCCESS，`fw-plus-sample/pom.xml` 声明的每个样例模块都实际参与构建；无 `target/classes` 产物的模块需显式说明理由（如无源码模块）。
- **A6｜Scenario**：回归护栏 —— 迁移与新增测试完成后 `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test` 结果 0 failed / 0 errors，skipped 仅限 A2 声明的真实 Redis 类。

# 约束与不变量

- JDK 1.8 语法（禁止 `List.of` 等 Java 9+ API）、UTF-8、Tab 缩进；不改任何 Maven 依赖版本。
- `WsBaseService.SecretKeyAndSignatureAlgorithm` 保留 `secretKey` 与 `algorithm` 两个字段及既有 getter，magic-api ws 插件、样例与测试的取用方式不变。
- 广播对外契约不变：`AbstractWebSocketHandler.broadcastMessageToClient(String,String,String)` 三兄弟签名不变，`WsBroadcastMessage` 的 JSON 结构不变，频道名解析（`fw.plus.ws.redis.channel.broadcast`，默认 `websocket:broadcast`）不变，`WsRedisMessageListenerContainer` 构造期绑定 listener 的方式不变。
- 密钥派生不变：继续使用 `Keys.hmacShaKeyFor(bytes)`，按长度 ≥64 / ≥48 / ≥32 选 HS512 / HS384 / HS256；迁移不得改变 token 的 `alg` 头与 payload。
- 假 Redis 只存在于 `src/test`，不进 `src/main`；不得为它新增 Maven 依赖，也不得改用真实 Redis 的替身框架替代被测链路。
- jjwt 0.12.x 语义约束：`verifyWith(SecretKey)` 取代 `setSigningKey(Key)`，`parseSignedClaims()` 取代 `parseClaimsJws()`，`subject()` / `issuedAt()` / `expiration()` 取代同名 setter。
- 默认配置值不得变化（`broadcast.async=false`、`binary.enabled=true` 等）。

# 决策

- **D1 流程与工作区**：沿用 Native 工作流；worktree `.worktrees/env-risk-cleanup`，分支 `comet/env-risk-cleanup`，目标分支 `dev`；上一个 change 已合并推送到 dev，本 change 从其后开始。
- **D2 三条风险合并为一个 change**：三者都强绑定 ws 与同一套 `fw-plus-boot-autoconfigure` 测试入口，拆开会让每轮多付一次约 3 分半的全量测试代价。
- **D3 W1 用进程内假 `RedisConnectionFactory`**：让真实的 `RedisMessageListenerContainer` 与 `WsBroadcastMessageListener` 保持被测对象，只替换最底层的连接工厂，从而覆盖解析、serverId 排除、本地投递全链路；真实 Redis 版保留为环境可选的更深证明。理由：mock listener 只能验证"收到消息"，无法验证排除与投递行为。
- **D4 W2 只补证据**：合并后 centrifuge 样例目录已在 dev 上，此前聚合器已构建成功；本 change 只落一份可复现回执，若暴露真实缺陷再修复。
- **D5 W3 迁移范围按文件列举**：只动 W3 列出的 3 个主代码 / 样例文件与 6 个测试文件；其它模块的 deprecated 调用不在范围，避免把风险清理扩大成全仓重构。
- **D6 不拆 Supervisor**：三条任务落在同一模块、同一测试命令，需要反复修改同一批文件，单会话顺序推进的协调成本更低。
- **D7 不改写 `ws-spring-boot-starter` 既有 capability 规格**：三项清理都不改变 ws 的用户可见行为，新增 capability `build-and-dependency-hygiene` 记录清理后的完整行为，避免与既有规格重复或产生漂移。
- **D8 无需向用户提问**：三条风险的范围由用户指定，其余为实现选择；按澄清参考不作为阻塞项提问。

# 待解决问题

无。

# 验证预期

- `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test`（约 3 分 35 秒，703 个测试）作为主回归入口。
- `cd fw-plus-core && ./mvnw -o -f ../fw-plus-sample/pom.xml clean compile -DskipTests`（约 3 分钟）作为聚合器证据。
- 单样例：`./mvnw -o -f ../fw-plus-sample/fw-plus-boot-samples/fw-plus-ws-spring-boot-starter-sample/pom.xml clean compile`。
- 长时 Maven 必须前台执行，以 `target/` 产物与 surefire 报告作为回执依据，不依赖终端回显。
- 跳过即未通过：`WsRedisBroadcastEndToEndTest` 在未设环境变量时的 Skipped 不得计入通过，A1 的进程内测试类不得出现 skipped。
