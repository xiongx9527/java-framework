# MQTT 自动配置标准化优化设计

## 背景

目标路径：

`fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt`

当前 MQTT 自动配置存在几个结构性问题：

- `MqttClientAutoConfiguration` 只声明条件，不创建任何客户端能力。
- 服务端默认认证、订阅校验、发布权限全部放行，容易让使用者误以为已经具备安全控制。
- 多个核心 bean 缺少 `@ConditionalOnMissingBean`，用户自定义覆盖不完整。
- Redis 集群 broker 组件对 Redis/Mica 依赖和 bean 初始化假设较强，缺少失败前置校验。
- `MqttServerConfiguration` 同时承担默认组件、server creator、server lifecycle、内存实现选择等职责，边界偏散。
- 当前目标包没有覆盖测试，后续重构风险不可控。

本次允许结构性优化，不限制为最小修改；但范围限定在 MQTT 自动配置包及其直接协作类，不顺带重构 MagicAPI 其它插件或通用基础设施。

## 目标

1. 按 Spring Boot 2.7 自动配置习惯重整 MQTT server/client/broker 配置。
2. 补齐客户端自动配置，使启用客户端时确实产出可用 MQTT client bean。
3. 修正默认安全语义：默认实现应明确、安全、可配置、可替换。
4. 让内存单机模式和 Redis 集群 broker 模式条件清晰，互不误启。
5. 所有默认核心 bean 支持用户自定义覆盖。
6. 为自动配置条件、bean 覆盖和关键默认行为补回归测试。

## 非目标

- 不改 MagicAPI MQTT 插件的资源格式和脚本 API。
- 不引入新的外部依赖版本。
- 不升级 Spring Boot、Mica MQTT、Redis 或 Magic-API 版本。
- 不新增管理端页面、监控指标、健康检查端点。
- 不设计完整 ACL 数据库模型；本次只提供默认安全实现和扩展点。

## 推荐方案

采用“标准化重构 + 严格默认行为 + 回归测试兜底”。

核心思路：

- `MqttAutoConfiguration` 保持总入口，只导入 server/client 自动配置。
- `MqttServerAutoConfiguration` 只负责启用条件和导入 server/broker 分层配置。
- `MqttServerConfiguration` 保留 server creator、server、template、launcher 创建，但所有可替换组件加缺省条件。
- 默认认证/授权类从“示例全放行”调整为“根据配置决定是否放行”。当配置显式关闭认证或授权时才放行；启用时没有合法配置则拒绝。
- `MqttServerBrokerConfiguration` 只在 Redis/Mica 类与必要 Redis bean 可用时启用。
- `MqttBrokerConfiguration` 中 serializer、store、dispatcher、receiver、manager、broker service、listener 全部支持用户覆盖。
- `MqttClientAutoConfiguration` 补齐客户端 creator/template/launcher 或复用 Mica MQTT 客户端标准构建链，确保启用后有可注入能力。
- 使用 `ApplicationContextRunner` 增加测试，先覆盖当前缺口，再重构实现。

## 配置模型

优先复用 Mica MQTT 已有属性：

- `mqtt.server.enabled`
- `mqtt.client.enabled`
- `mqtt.server.auth.*`
- `mqtt.server.http-basic-auth.*`
- Mica MQTT server/client 已有网络、SSL、buffer、debug 等属性

如现有属性不足，再在 fw-plus 包内新增轻量属性，不修改 Mica 属性语义：

- `fw-plus.mqtt.server.security.publish-auth-enabled`
- `fw-plus.mqtt.server.security.subscribe-auth-enabled`
- `fw-plus.mqtt.server.security.allow-anonymous-when-auth-disabled`

默认值：

- `publish-auth-enabled=true`
- `subscribe-auth-enabled=true`
- `allow-anonymous-when-auth-disabled=true`

含义：

- 认证开启时，必须匹配配置用户名/密码或用户自定义 `IMqttServerAuthHandler`。
- 发布/订阅授权开启时，默认实现拒绝空 `clientId`、空 topic/topicFilter、非法通配符发布等明显不合法请求。
- 业务 ACL 仍由用户自定义 `IMqttServerPublishPermission` 和 `IMqttServerSubscribeValidator` 扩展。

## 自动配置分层

### 总入口

`MqttAutoConfiguration`

- `@Configuration(proxyBeanMethods = false)`
- `@Import({MqttServerAutoConfiguration.class, MqttClientAutoConfiguration.class})`
- 不直接创建 bean。

### 服务端入口

`MqttServerAutoConfiguration`

- 条件：
  - `@ConditionalOnClass(MqttServerTemplate.class)`
  - `@ConditionalOnProperty(prefix = MqttServerProperties.PREFIX, name = "enabled", havingValue = "true")`
- 导入：
  - `MqttServerConfiguration`
  - `MqttServerBrokerConfiguration`

### 服务端核心配置

`MqttServerConfiguration`

创建默认 bean：

- `IMqttServerUniqueIdService`
- `IMqttServerAuthHandler`
- `IMqttServerSubscribeValidator`
- `IMqttServerPublishPermission`
- `HttpFilter`
- `IMqttConnectStatusListener` 单机默认实现
- `IMqttMessageListener` 单机默认实现
- `MqttServerCreator`
- `MqttServer`
- `MqttServerLauncher`
- `MqttServerTemplate`
- `IMqttSessionManager` 单机默认实现
- `IMqttMessageStore` 单机默认实现
- `IMqttService` 单机默认实现

约束：

- 默认 bean 均加 `@ConditionalOnMissingBean`。
- 单机默认实现只在 Redis broker 未启用时创建。
- `MqttServerCreator` 只装配存在的扩展 bean，不在存在默认 bean 时重复创建匿名实例。
- `MqttServer`、`MqttServerTemplate` 和 `MqttServerLauncher` 支持用户自定义覆盖。

### Redis broker 配置

`MqttServerBrokerConfiguration`

- 条件：
  - `MicaRedisCache` 在 classpath
  - `RedisConnectionFactory` 或可用 RedisTemplate/RStreamTemplate bean 存在
- 导入 `MqttBrokerConfiguration`

`MqttBrokerConfiguration`

默认 bean：

- `IMessageSerializer`
- `IMqttConnectStatusListener`
- `IMqttMessageStore`
- `RedisMqttMessageExchangeReceiver`
- `RedisMqttMessageDownReceiver`
- `IMqttMessageDispatcher`
- `RedisMqttServerManage`
- `IMqttBrokerService`
- `IMqttMessageListener`

约束：

- 所有 bean 支持 `@ConditionalOnMissingBean`。
- Redis receiver/dispatcher 初始化时找不到必要 Redis 组件要失败清晰，而不是后续 NPE。
- Redis channel 使用 `RedisKeys`，不散落字符串。

### 客户端配置

`MqttClientAutoConfiguration`

- 条件：
  - `@ConditionalOnClass(MqttClientTemplate.class)`
  - `@ConditionalOnProperty(prefix = MqttClientProperties.PREFIX, name = "enabled", havingValue = "true")`
- 创建默认客户端能力：
  - client creator 或等价构建对象
  - client 实例
  - `MqttClientTemplate`
  - launcher/lifecycle bean（如果 Mica 客户端标准需要）
- 所有 bean 支持用户自定义覆盖。

## 默认安全行为

### 认证

默认 `MqttAuthHandler` 不再无条件返回 true。

行为：

- 如果 `mqtt.server.auth.enable=false`，允许匿名，保持可显式关闭。
- 如果 `mqtt.server.auth.enable=true`，用户名和密码必须与配置一致。
- 用户自定义 `IMqttServerAuthHandler` 时完全接管默认行为。
- `clientId` 为空时拒绝，避免后续 unique id、在线客户端管理出现不稳定状态。

### 发布权限

默认 `MqttPublishPermission`：

- `clientId` 为空拒绝。
- topic 为空拒绝。
- topic 包含订阅通配符 `+` 或 `#` 时拒绝。
- 其它请求默认允许，业务级 ACL 交给用户自定义 bean。

### 订阅校验

默认 `MqttSubscribeValidator`：

- `clientId` 为空拒绝。
- topicFilter 为空拒绝。
- 明显非法通配符格式拒绝。
- 其它请求默认允许，业务级 ACL 交给用户自定义 bean。

## 测试设计

新增 MQTT 自动配置测试包：

`fw-plus-boot-project/fw-plus-boot-autoconfigure/src/test/java/com/xiongx9527/fw/plus/boot/autoconfigure/mqtt`

使用 `ApplicationContextRunner` 覆盖：

1. `mqtt.server.enabled=false` 时不创建 server 相关 bean。
2. `mqtt.server.enabled=true` 且类存在时创建 server creator、server、template、launcher。
3. 用户自定义认证/发布/订阅 bean 时默认 bean 不创建。
4. 默认认证开启且凭据不匹配时拒绝。
5. 默认发布权限拒绝空 clientId、空 topic、含通配符 topic。
6. 默认订阅校验拒绝空 clientId、空 topicFilter、非法通配符。
7. 无 Redis/Mica 条件时创建单机内存 session/store/service。
8. Redis broker 条件满足时创建 Redis broker 组件，不创建单机内存替代组件。
9. `mqtt.client.enabled=false` 时不创建客户端 bean。
10. `mqtt.client.enabled=true` 时创建客户端 template 或可用客户端构建链。

若某些 Mica MQTT 类难以在单元测试中直接构造，则测试优先验证自动配置条件和默认安全类行为；生命周期启动不在单元测试中真实连接端口。

## 实施顺序

1. 先写自动配置与默认安全行为测试，确认当前失败。
2. 重构默认安全类，让认证、发布、订阅默认行为可配置且可测试。
3. 调整服务端配置 bean 条件和单机/集群分支。
4. 调整 Redis broker 组件的缺省条件和初始化失败信息。
5. 补齐客户端自动配置。
6. 跑目标测试，再跑 autoconfigure 模块相关测试。

## 风险与兼容

- 默认认证从全放行变为配置驱动，可能影响依赖当前裸奔默认值的应用。该变化符合“严格优化”的方向，但需要在 README 或配置示例中说明如何显式关闭认证。
- Mica MQTT 的部分构建 API 可能与预期不一致，实施时以当前依赖源码和编译结果为准。
- Redis broker 测试可能需要模拟必要 bean，避免真实 Redis 连接。

## 验收标准

- MQTT 自动配置测试全部通过。
- `fw-plus-boot-autoconfigure` 指定测试可通过 Maven Wrapper 运行。
- 新增/修改代码不引入 Java 9+ API。
- 用户自定义核心 MQTT bean 时自动配置不覆盖。
- server/client disabled 时不会创建对应 bean。
- 默认安全类不再无条件放行明显非法请求。
