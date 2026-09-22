# Spec — ws-starter

## Purpose

新增纯 POM 的依赖聚合 starter `fw-plus-ws-spring-boot-starter`，把 magic-api WS 插件所需的 WebSocket 与 JWT 依赖收敛到一处，并让 `fw-plus-magicapi-plugin-ws` 通过依赖它获得 WS 能力。WS 的 Java 实现、包名、类名、自动配置与注册方式保持现状不变；starter 不包含任何代码与资源。

## Requirements

### Requirement: 提供纯 POM 的 WS starter 模块

系统 SHALL 提供模块 `fw-plus-ws-spring-boot-starter`，其内容仅为 `pom.xml`，不含 `src/` 目录、Java 源码、资源、auto-configuration 或 `META-INF/spring.factories`；该模块 SHALL 被 `fw-plus-boot-starters` 聚合 POM 与 `fw-plus-boot-dependencies` 的 `dependencyManagement` 收录，并继承父 POM `fw-plus-boot-starters`、使用 `${revision}` 版本。

#### Scenario: starter 模块只含 pom 且已登记

- **WHEN** 检查 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-ws-spring-boot-starter` 目录内容，并检索 `fw-plus-boot-starters/pom.xml` 与 `fw-plus-boot-dependencies/pom.xml`
- **THEN** 该模块下除 `pom.xml`（以及 Maven 生成的 `.flattened-pom.xml`、`target/`）外不存在 `src/` 等源码或资源目录；`fw-plus-boot-starters` 的 `<modules>` 含该模块，`fw-plus-boot-dependencies` 的 `dependencyManagement` 含该 artifactId 与 `${project.version}` 版本

### Requirement: starter 提供 WS 依赖集合

系统 SHALL 在 `fw-plus-ws-spring-boot-starter` 中声明 WS 能力所需依赖：`fw-plus-boot-starter`、`spring-boot-starter-websocket`，以及 `jjwt-api`、`jjwt-impl`、`jjwt-jackson`。

#### Scenario: starter 依赖集合完整

- **WHEN** 读取 `fw-plus-ws-spring-boot-starter/pom.xml` 的依赖声明
- **THEN** 其中包含 `fw-plus-boot-starter`、`spring-boot-starter-websocket` 与 jjwt 三件套（`jjwt-api`、`jjwt-impl`、`jjwt-jackson`），且不声明 `fw-plus-magicapi-plugin-ws` 等上层插件

### Requirement: 插件模块经 starter 获得 WS 依赖

系统 SHALL 让 `fw-plus-magicapi-plugin-ws` 依赖 `fw-plus-ws-spring-boot-starter`，且该模块不再自行声明 jjwt 依赖；依赖上移后，引入该插件的应用在编译/运行期仍能获得 `spring-websocket` 与 `jjwt-*`。

#### Scenario: 插件依赖 starter 且不重复声明

- **WHEN** 读取 `fw-plus-magicapi-plugin-ws/pom.xml` 的依赖声明
- **THEN** 该模块声明了对 `fw-plus-ws-spring-boot-starter` 的依赖，且不再包含 `jjwt-api`、`jjwt-impl`、`jjwt-jackson` 的直接声明；其原有 `src/console`、`src/main/resources/magic-editor/ws` 内容与前端构建配置保持不变

#### Scenario: 依赖树仍包含 WS 与 JWT 构件

- **WHEN** 对 `fw-plus-magicapi-plugin-ws` 解析依赖树
- **THEN** 依赖树中通过 `fw-plus-ws-spring-boot-starter` 出现 `spring-boot-starter-websocket` 与 `jjwt-api`、`jjwt-impl`、`jjwt-jackson`

### Requirement: 配置前缀迁移为 fw.plus.ws

系统 SHALL 只从 `fw.plus.ws.*` 读取 WS 配置，且不再读取 `magic-api.plugin.ws.*`，键名层级与默认值保持不变：`enabled`、`websocket.path`、`websocket.trustedDomain`、`websocket.subProtocols`、`websocket.supportsPartialMessages`、`websocket.endpoint`、`token.secret`、`token.expireHours`、`param.token`、`param.clientId`、`param.maxClientIdLength`、`init.type`、`close.type`、`appId`、`error.code.*`、`redis.channel.broadcast`、`session.cache.*`、`session.delayMillis`、`scheduler.delayedClosePoolSize`。

#### Scenario: 新前缀配置生效

- **WHEN** 应用以 `fw.plus.ws.token.secret`、`fw.plus.ws.websocket.path`、`fw.plus.ws.token.expireHours`、`fw.plus.ws.param.clientId` 等键提供配置
- **THEN** 这些值被 WS 配置对象绑定，并在 token 签发、端点注册与连接参数解析中生效

#### Scenario: 旧前缀不再被读取

- **WHEN** 应用只提供 `magic-api.plugin.ws.*` 而未提供 `fw.plus.ws.*`
- **THEN** 旧键不参与 WS 配置绑定，WS 行为回落到 `fw.plus.ws.*` 的默认值（`enabled` 缺省启用、`websocket.path` 缺省 `/ws/api`、`token.expireHours` 缺省 2）
