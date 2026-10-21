# 验证报告：mcp-websocket-custom-auth

## 1. 变更范围核对

| 类型 | 文件 |
| --- | --- |
| 新增 | `fw-plus-core/.../auth/core/CustomAuthService.java` |
| 修改 | `fw-plus-core/.../auth/core/InstanceAuthRegistry.java` |
| 修改 | `fw-plus-ai/.../mcp/server/McpAutoConfiguration.java` |
| 修改 | `fw-plus-boot/.../web/ws/starter/WsAutoConfiguration.java` |
| 新增 | `fw-plus-core/.../auth/core/InstanceAuthRegistryCustomProviderTest.java` |
| 新增 | `fw-plus-ai/.../mcp/server/McpCustomAuthProviderTest.java` |
| 新增 | `fw-plus-boot/.../web/ws/auth/WsCustomAuthProviderTest.java` |

无越界改动：`McpAuthService`/`WsAuthSupport` 公共签名、内置 SaToken provider 实现均未改。

## 2. 测试执行结果（comet check verify 证据）

| 模块（cwd） | 命令 | 结果 |
| --- | --- | --- |
| fw-plus-core | `./mvnw -o -pl fw-plus-project/fw-plus-auth/fw-plus-auth-core test` | Tests run: 8, Failures: 0, Errors: 0, Skipped: 0 |
| fw-plus-ai | `./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test` | Tests run: 1018, Failures: 0, Errors: 0, Skipped: 5 |
| fw-plus-boot | `./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test` | Tests run: 662, Failures: 0, Errors: 0, Skipped: 1 |

新增测试：`InstanceAuthRegistryCustomProviderTest` 8/8、`McpCustomAuthProviderTest` 4/4、`WsCustomAuthProviderTest` 4/4。
既有回归全绿：`McpAuthServiceRegistryTest` 5/5、`McpAutoConfigurationBeanSelectionTest` 13/13、`McpRuntimeFactoryTest` 17/17、`WsAuthSupportTest` 10/10、`WsAutoConfigurationTest` 10/10。

## 3. 规格需求映射（specs/auth-customization/spec.md）

| Requirement / Scenario | 覆盖测试 | 结果 |
| --- | --- | --- |
| 业务注册自定义 provider 并被解析（mcp） | `McpCustomAuthProviderTest.shouldUseCustomAuthServiceForConfiguredProvider` / `...ForNamedRuntime` | 通过 |
| 业务注册自定义 provider 并被解析（ws） | `WsCustomAuthProviderTest.shouldAuthenticateHandshakeByCustomProvider` / `shouldRegisterCustomAuthServiceFromContainer` | 通过 |
| McpAuthService SPI 并存不冲突 | `McpAuthServiceRegistryTest`（既有，未改） | 通过 |
| 内置 provider 行为不变 | `InstanceAuthRegistryCustomProviderTest.shouldBuiltInPermitAllUnchanged` / `builtInKeyWinsOverCustom`、`McpCustomAuthProviderTest.shouldKeepBuiltInProviderWhenCustomBeanPresent`、`WsCustomAuthProviderTest.shouldKeepBuiltInProviderWhenCustomProviderPresent`、`WsAuthSupportTest`、`McpAutoConfigurationBeanSelectionTest.shouldResolveAuthServicesByProviderConfig` | 通过 |
| 未配置/未解析自定义 provider 时拒绝 | `InstanceAuthRegistryCustomProviderTest.throwsWhenCustomProviderNotRegistered` / `throwsWhenProviderUnknown`、`McpCustomAuthProviderTest.shouldFailToStartWhenCustomProviderNotRegistered`（启动失败）、`WsCustomAuthProviderTest.shouldFailValidationWhenCustomProviderNotRegistered`（启动期校验抛错） | 通过 |

## 4. fail-closed 结论

- 自定义 key 未注册：`resolve` 抛 `IllegalArgumentException`；mcp 侧 runtime 构建期即失败（启动失败），ws 侧 `validateProviders` 启动期抛错；运行期解析同样拒绝，无误放行路径。
- 内置 key 与自定义 key 冲突：内置优先（register 跳过并告警）。
- 重复自定义 key：首次注册为准。

## 5. 结论

全部规格场景通过；新增扩展点与既有行为兼容；fail-closed 语义与现状一致。本 change 可以归档。
