# 目标

把 magic-api WebSocket 插件里**与 magic 无关的 WS 地基**下沉到既有模块 `fw-plus-boot-autoconfigure` 内的独立包 `com.xiongx9527.fw.plus.boot.autoconfigure.web.ws`（**不新增 Maven 模块**），以「包级隔离」的方式把 WebSocket 能力（路径注册、握手鉴权、会话管理、心跳、跨实例广播）与 magic 语义分离：该包内不引用任何 magic-api 类型。magic 侧（`...autoconfigure.magicapi.plugin.ws.*`）通过实现该包定义的 SPI，保留「订阅定义存 magic 资源存储 + 用 magic-script 处理消息」的既有能力。

# 范围

现状（已核实）：

- WS 实现共 17 个主类 + 2 个测试类，位于 `fw-plus-boot-autoconfigure` 的 `...autoconfigure.magicapi.plugin.ws` 包；由 `MagicApiPluginAutoConfiguration` 的 `@Import(MagicAPIWsConfiguration.class)` 注册。
- WebFlux 路径由 `MagicAPIWebFluxControllerRegistrar` 直接 `new MagicWsController(configuration)` 注册 → magic 侧与 WS 地基必须同时可见，且依赖方向单向（magic → 地基）。
- `fw-plus-ws-spring-boot-starter` 为纯 POM，聚合 `spring-boot-starter-websocket` + jjwt 三件套；`fw-plus-magicapi-plugin-ws` 内只有前端（`src/console/**`、`src/main/resources/magic-editor/ws/**`）。
- 前端 console 写死 `WsModule` 全限定名 `com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module.WsModule`（`src/console/src/service/magic-ws-datasource.js` 与 `src/console/dist/magic-ws.1.0.0.iife.js` 各一处）。
- magic 公共件 `MagicBaseService`、`MagicAPIConfigUtils` 位于 `...autoconfigure.magicapi.common`，被 debezium / rocketmq / elasticsearch / pulsar / mqtt / mcp / ai 等 30+ 处共用。
- 当前 WS 类与 magic 的实际耦合：`BaseDynamicWebSocketHandler` 用 `ScriptManager` + `MagicScriptContext` 执行 magic-script；`SimpleDynamicWebSocketHandler` / `RedisDynamicWebSocketHandler` 直接读 magic 资源存储；`WsDataSourceInfo` / `WsSubscribeInfo` 继承 `MagicEntity` / `PathMagicEntity`；4 个 storage/registry + `WsModule` + `MagicWsController` + `MagicAPIWsConfiguration` 均为 magic 绑定。

本次要做：

1. 在既有模块 `fw-plus-boot-autoconfigure` 内新增包 `com.xiongx9527.fw.plus.boot.autoconfigure.web.ws`（**不新增 Maven 模块**，也不新增聚合/BOM 条目）。该包内**不得 import 或引用任何 magic-api 类型**（包级隔离）。
2. 在该包内实现与 magic 无关的 WS 地基：
   - **配置**：`fw.plus.ws.*`（`enabled`、`websocket.path`、`token.secret`、`token.expireHours`、`trustedDomain`、`subProtocols`、`clientIdParamName` 等）。键名与默认值逐项保持不变。
   - **握手与鉴权**：握手拦截器 SPI（保留现有 `MagicAPIHandshakeInterceptor` 的语义与注册方式）+ JWT 签发/校验工具（自 `ws/common/BaseService` 提取，算法与 `token.secret` / `token.expireHours` 语义不变）。
   - **会话与消息**：抽象 WebSocket handler，负责会话注册表、心跳（ping/pong）、按订阅广播、定向发送、异常/关闭清理；Redis 跨实例广播通道（对应现有 `RedisDynamicWebSocketHandler` 的职责）。
   - **新增 SPI**：① 订阅解析（当前会话应绑定哪个订阅定义）；② 消息执行（如何用业务逻辑处理一次消息）；③ 结果回发句柄（执行期间向会话发送数据）。SPI 由该包定义，magic 侧实现。
   - **Spring Boot 自动配置**：以 `fw-plus-boot-autoconfigure` 自身 `spring.factories` 注册（`WsAutoConfiguration`、`WsRedisConfiguration`），启用后即可生效。
3. `fw-plus-ws-spring-boot-starter` 继续作为 WS 能力的对外入口。它已通过 `fw-plus-boot-starter` → `fw-plus-boot-autoconfigure` 链路拿到 WS 地基，故**无需新增依赖**；同时继续显式提供 `spring-websocket` 与 `jjwt-*`（这些在 `fw-plus-boot-autoconfigure` 中声明为 optional、不传递）。`fw-plus-magicapi-plugin-ws` 对它的依赖保持不变。
4. magic 侧（**包名保持不变**，前端零改动）：保留 `WsDataSourceInfo`、`WsSubscribeInfo`、4 个 storage/registry、`WsModule`、`MagicWsController`、`MagicAPIWsConfiguration`、`BaseDynamicWebSocketHandler`、`ws/common/BaseService` 中 magic 相关部分，位置仍在 `...autoconfigure.magicapi.plugin.ws.*`：
   - `BaseDynamicWebSocketHandler` 改为实现该包定义的 SPI（订阅来自 magic 资源存储，执行走 magic-script，行为与改动前一致）；
   - `MagicAPIWsConfiguration` 改为装配该包组件 + SPI 实现；
   - `MagicApiPluginAutoConfiguration` 的 `@Import` 与 `MagicAPIWebFluxControllerRegistrar` 中对 `MagicWsController` 的引用保持不变（类仍在原包）。
5. 不改动聚合 POM 的模块列表与 BOM 条目（不引入新模块）。

# 非目标

- 不改前端 console：不改 `magic-ws-datasource.js`，不重新构建 `dist/magic-ws.1.0.0.iife.js`，`WsModule` 全限定名不变。
- 不迁移 `MagicBaseService`、`MagicAPIConfigUtils`：仍留在 `...autoconfigure.magicapi.common`，30+ 处 import 保持不动。
- 不改 `fw.plus.ws.*` 的键名、层级与默认值；不恢复旧前缀 `magic-api.plugin.ws.*` 的兼容读取。
- 不改变 WS 运行时可见语义：endpoint 路径拼接规则、token 算法与有效期语义、跨域/子协议行为、控制台功能与交互。
- WS 地基包 `...autoconfigure.web.ws` 内不引用 magic-api 类型；不把 magic 相关类放进该包；不因为落点在 `fw-plus-boot-autoconfigure` 内而让该包反向引用 `...autoconfigure.magicapi.*`。
- 不改动其它 magic-api 插件的依赖与注册方式；不改动仓库中未提交的 MCP / AI 相关改动。

# 验收示例

- **E1 构建不回归**：`cd fw-plus-boot/fw-plus-boot-project && mvn -DskipTests compile` 成功；含前端构建的模块用 `-Dexec.skip=true`。
- **E2 地基与 magic 解耦（包级隔离）**：`...autoconfigure.web.ws` 包内**不含任何 magic-api 类型的 import 或引用**（静态检查 `import` 行与 magic 类型标识符均 0 命中）；magic 侧单向依赖该包，反向引用为 0。
- **E3 WS 能力可用（启用自动配置）**：启用 WS 自动配置时，真实 Servlet 容器按 `fw.plus.ws.websocket.path` 注册 WebSocket endpoint 并能完成真实 HTTP Upgrade；握手鉴权按 `token.secret` 生效；Redis 可用时跨实例广播经真实 Redis 生效。
- **E4 magic 侧端到端不回退**：引入 magic ws 插件后，订阅/数据源定义仍从 magic 资源存储读写，消息仍由 magic-script 处理，配置 `fw.plus.ws.*` 生效、缺省值不变。

# 约束与不变量

- 构建必须显式使用 JDK 8：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home`；Maven 为 `/Users/xiongxiao/Workspaces/Maven/apache-maven-3.6.3/bin/mvn`。
- 新增包遵守本仓约定：源码 UTF-8、`src/main/java` + `src/main/resources` 布局；不引入新模块与新的聚合/BOM 条目。
- 包级依赖单向：`...autoconfigure.magicapi.*` → `...autoconfigure.web.ws`；反向引用禁止（`...autoconfigure.web.ws` 不得 import `...autoconfigure.magicapi.*`）。
- 运行期可用依赖集合不得缩水：引入 `fw-plus-magicapi-plugin-ws` 的应用仍能拿到 `spring-websocket` 与 `jjwt-*`。
- 现有 `fw.plus.ws.*` 配置的解析结果在拆分前后必须逐项一致（含缺省值）。
- 不改动与本次无关的模块、POM 与未提交改动。已知 `fw-plus-boot-autoconfigure` 的 mqtt 测试编译存在与本次无关的既有问题，验证只用 `compile` / 定向测试，不以此为回归依据。

# 决策

- **D1 工作区**：`current` —— 沿用当前目录与 `dev` 分支。
- **D2 拆分深度**：**真拆分**。抽取 magic 无关的 WS 地基并新增 SPI，magic 侧实现 SPI 后对外行为不变。
- **D2' 拆分形态（用户后续修订）**：拆分以**包级隔离**落地，不新增 Maven 模块 —— WS 地基置于 `fw-plus-boot-autoconfigure` 的 `...autoconfigure.web.ws` 包，该包不引用 magic-api 类型。代价：`fw-plus-boot-autoconfigure` 本身仍带 magic-api 依赖，故「非 magic 应用不必拖 magic」不再是目标。
- **D3 落点**：包 `com.xiongx9527.fw.plus.boot.autoconfigure.web.ws`，位于既有模块 `fw-plus-boot-autoconfigure` 内（**不新增模块**，artifactId 侧无变化）。该包与既有 `...autoconfigure.web` 下的 WebAutoConfiguration/webflux/webmvc 等类同模块但不同子包，无同名冲突。
- **D4 公共件不迁移**：`MagicBaseService`、`MagicAPIConfigUtils` 留在 `...autoconfigure.magicapi.common`，避免 30+ 处 import 变更。magic 绑定类与 WS 地基同为 `fw-plus-boot-autoconfigure` 的成员、分属不同包，故不构成依赖环。
- **D5 前端包名不变**：magic 侧 WS 类（含 `WsModule`）保留 `...autoconfigure.magicapi.plugin.ws.*` 包名，由用户选择「前端不改优先」。
- **D6 starter 复用**：不新增 starter；`fw-plus-ws-spring-boot-starter` 通过既有的 `fw-plus-boot-starter` → `fw-plus-boot-autoconfigure` 链路拿到 WS 地基，并继续显式提供 `spring-websocket` 与 `jjwt-*`。
- **D7 SPI 归属**：SPI 接口由 `...autoconfigure.web.ws` 包定义（不引用 magic 类型），magic 侧提供实现。SPI 的精确签名在 Build 阶段确定。
- **D8 配置类形态**：暂保留现有 `HashMap<String, Object>` + 读取侧展开（`flatten()`）的实现方式，仅做搬迁与改名，避免引入属性绑定语义变化。

# 迁移副作用修复（Build 阶段发现）

WS 地基与其测试从被废弃的独立模块并入 `fw-plus-boot-autoconfigure` 后，暴露出两处**由「同模块」这一落点带来的**问题，已就地修复；两处都只影响该模块自身的测试类路径，不影响对外可用依赖集合。

- **S1 多重 SLF4J 绑定**：该模块自身（optional）依赖链上有三个 SLF4J 绑定并存 —— Spring Boot 自带的 `logback-classic`、`pulsar-client`/`pulsar-client-admin` 传递的 `log4j-slf4j-impl`、`seata-server` 传递的 `slf4j-reload4j`。任何起真实 Spring 上下文的测试都会在启动期抛 `LoggerFactory is not a Logback LoggerContext but Logback is on the classpath`（Spring Boot 按 logback 初始化，而 SLF4J 绑到了其它实现）。修复：在 `pulsar-client`、`pulsar-client-admin`、`seata-server` 三处排除外来绑定，使该模块类路径只保留 logback 一个绑定（与 POM 中既有的 `log4j-to-slf4j` 排除同一手法）。三者均为 `optional`，排除不会传递给使用方，故不违反「运行期可用依赖集合不得缩水」。
- **S2 测试最小应用改为显式导入自动配置**：`WsEndpointHttpUpgradeTest` 的 `WsTestApplication` 由 `@EnableAutoConfiguration` 改为 `@ImportAutoConfiguration({WsAutoConfiguration, WebSocketServletAutoConfiguration, ServletWebServerFactoryAutoConfiguration, DispatcherServletAutoConfiguration})`：后三条是内嵌 Servlet 容器与 JSR-356 支持所必需的 Spring Boot 标准自动配置（少 `WebSocketServletAutoConfiguration` 会因容器内缺 `ServerContainer` 而让 Upgrade 返回 500；少 `DispatcherServletAutoConfiguration` 则请求不经过 HandlerMapping）。原因：迁入后 `@EnableAutoConfiguration` 会把本模块 `spring.factories` 中 16 条自动配置**全部**加载（原独立模块只有 2 条），其中 magic-api 的 vue3 插件在 `magic-api.web` 缺省时把过滤器 url-pattern 拼成 `null/vue3/component/*`，导致 Tomcat 启动失败 —— 与被测能力无关。改后该用例证明的正是「**仅**启用 WS 自动配置即可提供 WS 能力」，比原先更强、且不受本模块自动配置集合日后增减影响。刻意不导入 `WsRedisConfiguration`，用于覆盖「无 Redis 时降级为单机广播」。

# 待解决问题

- SPI 的精确签名（方法名、参数、结果回发句柄形态、会话与订阅的关联时机）在 Build 阶段确定；已确认的是「必须有 SPI，且由 magic 侧实现」。
- `RedisDynamicWebSocketHandler` 的广播通道 topic 命名在拆分为「地基通道 + magic 订阅」后是否需要调整；要求是对外行为不变，命名属实现选择。
- 抽象 handler 与 magic 实现类的继承 vs 组合关系在 Build 阶段确定，验收只约束行为等价。
- E3 的运行时验证由 `fw-plus-boot-autoconfigure` 内的测试承担（真实 Servlet 容器 HTTP Upgrade + 真实 Redis 跨实例广播），不新增样例模块。

# 验证预期

- **结构检查**：`...autoconfigure.web.ws` 包存在且物理位于 `fw-plus-boot-autoconfigure` 内；不存在 `fw-plus-boot-autoconfigure-web` 模块目录及其 POM/聚合/BOM 残留引用；`fw-plus-magicapi-plugin-ws` 仍依赖 `fw-plus-ws-spring-boot-starter`；前端 JS 与 dist 产物中 `WsModule` 全限定名未变。
- **编译检查**：`mvn -DskipTests compile` 通过。
- **隔离检查**：`...autoconfigure.web.ws` 包内 magic-api import 与 magic 类型引用均 0 命中，且该包不 import `...autoconfigure.magicapi.*`；运行期 `spring-websocket` 与 `jjwt-*` 仍可达。
- **运行时检查（WS 路径）**：真实 Spring 上下文启动，启用 WS 自动配置时 endpoint 按 `fw.plus.ws.websocket.path` 注册、缺省 `token.secret` 未配置时行为符合预期、`token.expireHours` 缺省值不变。
- **运行时检查（magic 路径）**：magic ws 插件在场时，订阅定义经 magic 资源存储读写、消息经 magic-script 处理、`fw.plus.ws.*` 配置逐项生效。
- **静态核对**：`fw.plus.ws.*` 的键名与默认值在改动前后一致；无旧前缀 `magic-api.plugin.ws` 新增残留。
