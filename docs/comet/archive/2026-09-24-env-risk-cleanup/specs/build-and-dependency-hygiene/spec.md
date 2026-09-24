# fw-plus 构建与依赖卫生能力规格

`build-and-dependency-hygiene` 保证"不依赖外部服务就能成立的证明"与"依赖 API 的卫生"：跨实例广播链路有进程内端到端证据，jjwt 调用只使用 0.12.x 推荐 API，样例聚合器可整体构建。本能力不引入新的用户可见功能，只为既有行为提供可复现证据并消除依赖债务。

## 跨实例广播的端到端证据

Redis 广播链路：`RedisWebSocketHandler.broadcastMessageToClient(excludedServerId, appId, clientId, message)` → `StringRedisTemplate.convertAndSend(channel, JSON)` → `WsRedisMessageListenerContainer`（`WsBroadcastMessageListener` 绑定 `ChannelTopic(channel)`）→ 以 serverId 排除自身 → `WsLocalBroadcastTarget` 本地投递。

证据分两层，互不替代：

- **进程内层（始终运行）**：由测试代码提供假 `RedisConnectionFactory`，把 `convertAndSend` 的报文投递给已订阅该频道的 listener，再由真实的 `RedisMessageListenerContainer`、其任务执行器与 `WsBroadcastMessageListener` 完成反序列化、serverId 排除与本地投递。不需要网络、外部进程或环境变量。
- **真实 Redis 层（环境可选）**：设置 `FW_WS_TEST_REDIS_HOST` 与 `FW_WS_TEST_REDIS_PORT` 时执行同一组断言；未设置时整类按 assumption 跳过，并在验证记录中标注为"未执行"，不得计入通过数。

假实现只存在于 `src/test`，不进入 `src/main`，不新增 Maven 依赖，也不替换被测的监听容器与 listener。

断言覆盖四类行为：跨实例可达、定向广播恰好一次、排除自身 serverId 不投递、广播给全部客户端可达本地会话。

## jjwt 使用卫生

ws 主代码（`WsBaseService`）、magic-api ws 插件（`MagicWsController`、`WsModule`）、ws 样例（`DemoTokenController`）与相关测试中的 JWT 收发只使用 jjwt 0.12.x 推荐 API：

- 签发：`Jwts.builder().subject(appId).issuedAt(now).expiration(expireDate).signWith(secretKey).compact()`。
- 校验：`Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token)`。

已废弃的 `JwtBuilder.signWith(SignatureAlgorithm, …)`、`signWith(Key, SignatureAlgorithm)`、`JwtParserBuilder.setSigningKey` / `setSigningKeyResolver`、`parseClaimsJwt` / `parseClaimsJws`、`ClaimsMutator` 的 `setSubject` / `setIssuedAt` / `setExpiration` / `setNotBefore` / `setIssuer` / `setAudience` / `setId` 在这些文件中不再出现。

密钥派生与 token 语义保持不变：`Keys.hmacShaKeyFor(bytes)`，按密钥长度 ≥64 / ≥48 / ≥32 依次选择 HS512 / HS384 / HS256；`subject` 取 appId，过期时间按 `fw.plus.ws.token.expireHours` 生效。迁移前用同一密钥与相同 subject / iat / exp 生成的合法 token，迁移后仍能被同一校验链路解析出相同 appId。`WsBaseService.SecretKeyAndSignatureAlgorithm` 的字段与 getter 形状不变。

## 样例聚合器构建

`fw-plus-sample` 聚合器从仓库根以 `./fw-plus-core/mvnw -o -f ../fw-plus-sample/pom.xml clean compile -DskipTests` 可整体构建：`fw-plus-sample/pom.xml` 声明的 boot / cloud / ai 三组样例模块全部实际参与构建并产出 `target/classes`；无源码的模块需显式说明理由。样例代码与库代码之间的编译期契约（自动配置类名、依赖坐标、`fw.plus.*` 配置键）因此获得回归护栏。

## 与其它能力的关系

- `ws-spring-boot-starter`：其规格描述的用户可见行为不变；本能力只为其补齐证据与 API 卫生，不修改该 capability 的 requirement。
- 构建顺序 Core → Boot → Cloud / AI 与 `./fw-plus-core/mvnw -f fw-plus-sample/pom.xml validate` 的统一入口约定不变。

## Scenario

- **Scenario A1**：在未设置 `FW_WS_TEST_REDIS_HOST` / `FW_WS_TEST_REDIS_PORT` 的环境下执行 `fw-plus-boot-autoconfigure` 测试，进程内端到端测试类实际运行，4 个用例全部执行且通过、0 skipped，分别断言另一实例发布的广播经 Redis 容器链路投递到本地会话、定向广播恰好投递一次、排除自身 serverId 的广播不投递到本地、广播给全部客户端可到达本地会话。
- **Scenario A2**：设置 `FW_WS_TEST_REDIS_HOST` / `FW_WS_TEST_REDIS_PORT` 时 `WsRedisBroadcastEndToEndTest` 对真实 Redis 执行同一组断言并通过；未设置时整类按 assumption 跳过并标注为"未执行"，不计入通过数。
- **Scenario A3**：32 / 48 / 64 字节密钥下签发的 token，header `alg` 依次为 `HS256` / `HS384` / `HS512`，`subject` 取 appId，过期时间按 `fw.plus.ws.token.expireHours` 生效，`validateAndExtractAppIdFromToken` 的接受 / 拒绝行为不变。
- **Scenario A4**：W3 列出的全部文件中，`signWith(SignatureAlgorithm,…)`、`signWith(Key,SignatureAlgorithm)`、`setSigningKey`、`setSigningKeyResolver`、`parseClaimsJwt` / `parseClaimsJws` 与 `ClaimsMutator` 的 setter 家族 0 处残留；迁移前用同一密钥与相同 subject / iat / exp 生成的固定 token 仍能被同一校验链路解析出相同 appId。
- **Scenario A5**：`cd fw-plus-core && ./mvnw -o -f ../fw-plus-sample/pom.xml clean compile -DskipTests` 返回 BUILD SUCCESS，`fw-plus-sample/pom.xml` 声明的每个样例模块都实际参与构建；无 `target/classes` 产物的模块需显式说明理由。
- **Scenario A6**：迁移与新增测试完成后 `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test` 结果 0 failed / 0 errors，skipped 仅限 A2 声明的真实 Redis 类。

