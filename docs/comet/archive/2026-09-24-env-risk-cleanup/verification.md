---
generated_from_state_version: 13
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 1
- 迭代: 1
- 验证器尝试次数: 1
- 完成时间: 2026-09-24T10:52:57.064Z
- 摘要: 独立验收通过。三条环境性风险均已落地并有可复现证据：W1 新增进程内端到端测试在无外部 Redis 的前提下跑通 4 个用例，覆盖 Redis 广播发布、订阅、反序列化、serverId 排除与本地投递全链路；W2 样例聚合器 12 个有源码模块全部 SUCCESS 并产出 target/classes，BUILD SUCCESS；W3 将 W3 清单 11 个文件中的 jjwt 0.12.x 废弃 API 清零并改用推荐 API，密钥派生、算法选择与 token 载荷不变，迁移前签发的冻结 token 仍能被同一校验链路解析。jjwt.version 保持 0.12.7，未新增 Maven 依赖，未使用反射。全量回归 713 个测试 0 failed / 0 errors，唯一 skipped 为 A2 的真实 Redis 类。遗留风险三条：真实 Redis 未执行、冻结 token 仅做格式载荷层校验、工作区含与本 change 无关的既有噪音。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | **A1｜Scenario**：进程内跨实例广播端到端 —— 在未设置 `FW_WS_TEST_REDIS_HOST` / `FW_WS_TEST_REDIS_PORT` 的环境下执行 `fw-plus-boot-autoconfigure` 测试，新增的进程内端到端测试类实际运行：4 个用例全部执行且通过、0 skipped，分别断言另一实例发布的广播经 Redis 容器链路投递到本地会话、定向广播恰好投递一次、排除自身 serverId 的广播不投递到本地、广播给全部客户端可到达本地会话。 | 独立验收员在未设置 FW_WS_TEST_REDIS_HOST / FW_WS_TEST_REDIS_PORT 的环境下重跑 fw-plus-boot-autoconfigure 测试，WsRedisBroadcastInProcessEndToEndTest 实际运行 4 个用例，结果 Tests run: 4, Failures: 0, Errors: 0, Skipped: 0（/tmp/a1-a2-a3.log:66）。测试代码要求 RedisMessageListenerContainer 启动并完成广播频道订阅后，再通过公开 API 与握手注册会话进行断言（WsRedisBroadcastInProcessEndToEndTest.java:74、95、121、145），覆盖发布、订阅、反序列化、serverId 排除与本地投递全链路，非 mock listener 的浅验证。 |
| A2 | passed | brief.md | **A2｜Scenario**：真实 Redis 为可选深证 —— 设置 `FW_WS_TEST_REDIS_HOST` / `FW_WS_TEST_REDIS_PORT` 时 `WsRedisBroadcastEndToEndTest` 对真实 Redis 执行同一组断言并通过；未设置时按 assumption 整类跳过，并在验证记录中标注为"未执行"，不得计入通过数。 | 同一次运行中 WsRedisBroadcastEndToEndTest 因未设环境变量按 assumption 整类跳过，Tests run: 1, Failures: 0, Errors: 0, Skipped: 1（/tmp/a1-a2-a3.log:33）；跳过原因在 WsRedisBroadcastEndToEndTest.java:57 明确记为未设置 FW_WS_TEST_REDIS_HOST / FW_WS_TEST_REDIS_PORT。按验收口径该条标注为未执行，不计入通过数；本机未设变量，无法验证真实 Redis 的同一组断言，已列入 risks。 |
| A3 | passed | brief.md | **A3｜Scenario**：token 语义逐字不变 —— 32 / 48 / 64 字节密钥下签发的 token，header `alg` 依次为 `HS256` / `HS384` / `HS512`，`subject` 取 appId，过期时间按 `fw.plus.ws.token.expireHours` 生效，`validateAndExtractAppIdFromToken` 的接受 / 拒绝行为不变。 | 同一次运行中 WsBaseServiceTest 10 例全部通过、0 skipped（/tmp/a1-a2-a3.log:67），MagicAPIWsWiringTest 6 例全部通过、0 skipped（/tmp/a1-a2-a3.log:69）。独立核对源码确认签发与校验已改用单参 signWith(Key)、verifyWith 与 parseSignedClaims，算法分支仍按密钥长度选 HS256/HS384/HS512，subject 取 appId，expireHours 仍读配置（WsBaseService.java:203、220、257/260；MagicWsController.java:169/173；WsModule.java:91/102），接受与拒绝边界不变。 |
| A4 | passed | brief.md | **A4｜Scenario**：废弃 API 清零且解析链路兼容 —— `signWith(SignatureAlgorithm,…)`、`signWith(Key,SignatureAlgorithm)`、`setSigningKey`、`setSigningKeyResolver`、`parseClaimsJwt` / `parseClaimsJws` 与 `ClaimsMutator` 的 `setSubject` / `setIssuedAt` / `setExpiration` / `setNotBefore` / `setIssuer` / `setAudience` / `setId` 在 W3 列出的全部文件中 0 处残留；同时迁移前用同一密钥与相同 subject / iat / exp 生成的固定 token 仍能被同一校验链路解析出相同 appId。 | 独立对 W3 清单 11 个文件执行 grep：双参 signWith(SignatureAlgorithm,…) / signWith(Key,SignatureAlgorithm)、setSigningKey 与 setSigningKeyResolver、parseClaimsJwt / parseClaimsJws、ClaimsMutator 及其 setter 家族 setSubject/setIssuedAt/setExpiration/setNotBefore/setIssuer/setAudience/setId 均为 0 处调用残留；唯一命中为 WsBaseServiceTest.java:94 描述冻结 token 的 Javadoc 文字，非调用。第三方冻结 token 独立解码为 HS256、sub=app-frozen，iat/exp 与注释一致（WsBaseServiceTest.java:97），证明迁移后链路仍能解析迁移前签发的 token。jjwt.version 保持 0.12.7 未动。 |
| A5 | passed | brief.md | **A5｜Scenario**：样例聚合器整体构建 —— `cd fw-plus-core && ./mvnw -o -f ../fw-plus-sample/pom.xml clean compile -DskipTests` 返回 BUILD SUCCESS，`fw-plus-sample/pom.xml` 声明的每个样例模块都实际参与构建；无 `target/classes` 产物的模块需显式说明理由（如无源码模块）。 | 独立执行 cd fw-plus-core && ./mvnw -o -f ../fw-plus-sample/pom.xml clean compile -DskipTests，得到 Reactor Summary for Fw-Plus Samples 0.1.1-SNAPSHOT，12 个样例模块全部 SUCCESS，最终 BUILD SUCCESS（/tmp/w2-agg.log:362、367、387）。聚合器自身仅为 pom，12 个有源码模块均产出 target/classes；唯一无 target/classes 的 fw-plus-cloud-samples/nacos-server-sample 不是 Maven 子模块且未被 fw-plus-sample/pom.xml:11 声明，不构成缺漏。 |
| A6 | passed | brief.md | **A6｜Scenario**：回归护栏 —— 迁移与新增测试完成后 `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test` 结果 0 failed / 0 errors，skipped 仅限 A2 声明的真实 Redis 类。 | 独立执行 cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test，结果 Tests run: 713, Failures: 0, Errors: 0, Skipped: 1（/tmp/a6-final.log:1848），BUILD SUCCESS（/tmp/a6-final.log:1851），Total time 05:52 min。唯一 skipped 即 A2 声明的 WsRedisBroadcastEndToEndTest 整类，符合 A6 对 skipped 范围的限定。 |

## 检查

_没有记录 Runtime 检查。_

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- A1 进程内跨实例广播端到端（WsRedisBroadcastInProcessEndToEndTest）: passed — 在未设置 FW_WS_TEST_REDIS_HOST/PORT 下随 fw-plus-boot-autoconfigure 全量测试实际运行：Tests run: 4, Failures: 0, Errors: 0, Skipped: 0。由测试代码提供假 RedisConnectionFactory，把 convertAndSend 的报文投递给已订阅该频道的 listener，从而驱动真实的 RedisMessageListenerContainer、其任务执行器与 WsBroadcastMessageListener，覆盖发布、订阅、反序列化、serverId 排除、本地投递全链路。四条断言分别为：跨实例可达、定向广播恰好投递一次、排除自身 serverId 不投递、广播给全部客户端可达本地会话。关键根因：ConnectionUtils.isAsync() 只认 LettuceConnectionFactory，假工厂被判为阻塞型而走 BlockingSubscriber，其 eventuallyPerformSubscription() 假设 subscribe() 阻塞到连接关闭，同步返回的假工厂导致订阅被立即清空；修法为假工厂 close 分支只置 closed 标志，取消订阅只走 unsubscribe 与 Subscription.close()。证据：/tmp/a6-final.log 第 526、558 行。
- A2 真实 Redis 端到端（WsRedisBroadcastEndToEndTest）: not-run — 本机与 CI 未设置 FW_WS_TEST_REDIS_HOST / FW_WS_TEST_REDIS_PORT，该测试类按 assumption 整类跳过，A6 全量回归中 Skipped: 1 即此一条。按 A2 要求标注为未执行，不计入通过数。
- A3 token 语义逐字不变（WsBaseServiceTest 10 例 + MagicAPIWsWiringTest 6 例）: passed — WsBaseServiceTest 由 6 增至 10 例、0 skipped：新增 tokenIssuedBeforeMigrationShouldStillResolveToSameAppId（迁移前旧 API 签发的冻结 token 仍解析出相同 appId）、expiredTokenShouldBeRejected、headerAlgorithmShouldFollowKeyLength（32/48/64 字节密钥依次得 HS256/HS384/HS512，另加过短密钥走 SHA-512 派生仍出 HS512）、selectedAlgorithmShouldMatchTokenHeader（SignatureAlgorithm.getValue() 比对 token header alg）、expireHoursShouldFollowConfig（getTokenExpireHours() 读 token.expireHours）。MagicAPIWsWiringTest 由 5 增至 6 例：新增 generatedTokenShouldHonorConfiguredAppIdAndExpireHours，用 ApplicationContextRunner 注入真实 WsDataSourceInfoMagicResourceStorage bean 并由公开 setter 注入假 MagicResourceService，再经 WsModule.generateToken 断言 subject 取 appId、exp 减 iat 等于 TimeUnit.HOURS.toMillis(7)。证据：/tmp/a6-final.log 第 568、1527 行。
- A4 废弃 API 静态清零（W3 清单全部文件 rg 复核）: passed — 对 11 个文件逐项 rg 复核（3 主代码 WsBaseService/MagicWsController/WsModule，1 样例 DemoTokenController，6 个测试，外加 MagicAPIWsWiringTest）：signWith 双参形式 0 处调用；setSigningKey 与 setSigningKeyResolver 0 处；parseClaimsJwt 与 parseClaimsJws 0 处；ClaimsMutator 的 setSubject/setIssuedAt/setExpiration/setNotBefore/setIssuer/setAudience/setId 0 处调用。唯一命中位于 WsBaseServiceTest.java 第 93-94 行 Javadoc，是说明两个迁移前冻结 token 由旧 API 签发的注释文字，非调用。新 API 采用计数：signWith 单参 12 处、verifyWith 3 处（WsBaseService.java 第 261 行加 2 个测试）、parseSignedClaims 3 处、Keys.hmacShaKeyFor 6 处。jjwt.version 保持 0.12.7。
- A4 迁移等价性（同密钥同 subject/iat/exp 逐字节比对）: passed — 离线脚本验证：同一密钥、相同 subject/iat/exp 下，旧 API 与新 API 产出的 compact token 在 HS256/HS384/HS512 三种算法上逐字节相同；过期 token 在新旧两条链路上都抛 ExpiredJwtException，接受与拒绝边界一致。同时 WsBaseServiceTest 已固化两个迁移前签发的冻结 token 常量作为长期回归护栏。
- A5 样例聚合器整体构建: passed — cd fw-plus-core && ./mvnw -o -f ../fw-plus-sample/pom.xml clean compile -DskipTests，得到 Reactor Summary for Fw-Plus Samples 0.1.1-SNAPSHOT 与 BUILD SUCCESS，Total time 01:32 min。fw-plus-sample 下已产出 target/classes 的样例目录 12 个，与聚合器声明的实际有源码模块一致。证据：/tmp/w2-agg.log。
- A6 回归护栏（fw-plus-boot-autoconfigure 全量测试）: passed — cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test，得到 Tests run: 713, Failures: 0, Errors: 0, Skipped: 1 与 BUILD SUCCESS，Total time 05:52 min。全部 skipped 仅 1 条，即 A2 声明的 WsRedisBroadcastEndToEndTest 整类，符合 A6 的 skipped 口径。证据：/tmp/a6-final.log 第 1848 行。
- 已知限制: fw-plus-redis-spring-boot-starter-sample 无任何 Java 源码，Maven 编译时打印 No sources to compile，因此没有 target/classes 产物；这是该样例的既有形态，非本次改动引入，A5 按无源码模块需显式说明理由的口径记录。
- 已知限制: A2 的 WsRedisBroadcastEndToEndTest 需要真实 Redis，本机与 CI 均未设置 FW_WS_TEST_REDIS_HOST / FW_WS_TEST_REDIS_PORT，该类按 assumption 整类跳过并标注为未执行，不计入通过数；真实 Redis 环境未执行。

## 阻塞项

_无。_

## 风险与跳过的工作

- A2 的真实 Redis 链路未执行：本机与 CI 未设置 FW_WS_TEST_REDIS_HOST / FW_WS_TEST_REDIS_PORT，只能验证未设置时整类跳过，无法验证真实 Redis 下的同一组断言。
- A4 的冻结 token 只做了格式与载荷层面的独立校验：验收员人工解码 token 头部与负载，确认 algorithm、subject、iat/exp 与注释一致，但未在本机另跑一次旧 API 与新 API 的端到端对比（当前 classpath 已迁移到 0.12.7 新 API）。
- worktree 内存在与本 change 无关的既有噪音：fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-agentic/** 大量文件处于删除或修改状态、fw-plus-sample/fw-plus-cloud-samples/eureka-service-sample/.flattened-pom.xml 出现在工作区、docs/comet/changes/ 未跟踪。它们不在本次 W3 的 11 个文件范围内，但 A5/A6 是在含这些噪音的工作区状态下执行的，结论未完全隔离该影响。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | pass | — | 独立验收通过。三条环境性风险均已落地并有可复现证据：W1 新增进程内端到端测试在无外部 Redis 的前提下跑通 4 个用例，覆盖 Redis 广播发布、订阅、反序列化、serverId 排除与本地投递全链路；W2 样例聚合器 12 个有源码模块全部 SUCCESS 并产出 target/classes，BUILD SUCCESS；W3 将 W3 清单 11 个文件中的 jjwt 0.12.x 废弃 API 清零并改用推荐 API，密钥派生、算法选择与 token 载荷不变，迁移前签发的冻结 token 仍能被同一校验链路解析。jjwt.version 保持 0.12.7，未新增 Maven 依赖，未使用反射。全量回归 713 个测试 0 failed / 0 errors，唯一 skipped 为 A2 的真实 Redis 类。遗留风险三条：真实 Redis 未执行、冻结 token 仅做格式载荷层校验、工作区含与本 change 无关的既有噪音。 | 2026-09-24T10:52:57.064Z |



## 结论

独立验收通过。三条环境性风险均已落地并有可复现证据：W1 新增进程内端到端测试在无外部 Redis 的前提下跑通 4 个用例，覆盖 Redis 广播发布、订阅、反序列化、serverId 排除与本地投递全链路；W2 样例聚合器 12 个有源码模块全部 SUCCESS 并产出 target/classes，BUILD SUCCESS；W3 将 W3 清单 11 个文件中的 jjwt 0.12.x 废弃 API 清零并改用推荐 API，密钥派生、算法选择与 token 载荷不变，迁移前签发的冻结 token 仍能被同一校验链路解析。jjwt.version 保持 0.12.7，未新增 Maven 依赖，未使用反射。全量回归 713 个测试 0 failed / 0 errors，唯一 skipped 为 A2 的真实 Redis 类。遗留风险三条：真实 Redis 未执行、冻结 token 仅做格式载荷层校验、工作区含与本 change 无关的既有噪音。
