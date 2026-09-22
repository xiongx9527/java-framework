# 目标

把 magic-api WebSocket 插件所需的依赖收敛为一个**纯 POM** 的 Spring Boot starter `fw-plus-ws-spring-boot-starter`：它只声明依赖（含 `spring-boot-starter-websocket` 与 JWT 三件套），不包含任何代码；`fw-plus-magicapi-plugin-ws` 改为依赖该 starter 获得 WS 能力，其它应用也可直接引入这个 starter。同时把 WS 配置前缀由 `magic-api.plugin.ws.*` 迁移为 `fw.plus.ws.*`。

# 范围

现状（已核实）：
- WS 的 Java 实现（17 个主类 + 2 个测试类）位于 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/ws` 包，由 `MagicApiPluginAutoConfiguration` 的 `@Import` 注册。
- `fw-plus-magicapi-plugin-ws` 模块内没有 Java 源码，只有 `src/console/**` 前端控制台、`src/main/resources/magic-editor/ws/**` 资源，以及自行声明的 `jjwt-api`/`jjwt-impl`/`jjwt-jackson` 依赖。
- 仓库既有 starter 约定（如 `fw-plus-mqtt-client-spring-boot-starter`）就是「只有 `pom.xml` + 父 POM `fw-plus-boot-starters` + 依赖 `fw-plus-boot-starter` 及目标第三方库」，没有 `src/`。
- 当前没有任何模块显式声明 `spring-boot-starter-websocket`（靠 magic-api starter 传递）。

本次要做：

1. 新增模块 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-ws-spring-boot-starter`，**只有 `pom.xml`**：没有 `src/`、没有 Java 代码、没有资源、没有 auto-configuration、没有 `spring.factories`。依赖集合：
   - `com.xiongx9527.fw.plus.boot:fw-plus-boot-starter`
   - `org.springframework.boot:spring-boot-starter-websocket`
   - `io.jsonwebtoken:jjwt-api`、`jjwt-impl`、`jjwt-jackson`
   父 POM 为 `fw-plus-boot-starters`，版本沿用 `${revision}`，声明 `<main.basedir>${basedir}/../../..</main.basedir>`。
2. `fw-plus-magicapi-plugin-ws/pom.xml`：新增对 `fw-plus-ws-spring-boot-starter` 的依赖；移除本模块原先自行声明的 3 个 jjwt 依赖（改由 starter 传递）。模块内容（`src/console/**`、`src/main/resources/magic-editor/ws/**`）与前端构建配置不变。
3. `fw-plus-boot-starters/pom.xml` 增加新模块条目；`fw-plus-boot-dependencies` 的 `dependencyManagement` 增加该 starter（`<version>${project.version}</version>`，与其它 starter 写法一致）。
4. WS 的 Java 实现继续留在 `fw-plus-boot-autoconfigure` 的 `magicapi/plugin/ws` 包：不迁移、不改包名、不改类名、不改注册方式，`MagicApiPluginAutoConfiguration` 的 `@Import` 保持不变。
5. 配置前缀迁移：把 `magic-api.plugin.ws.*` 统一改为 `fw.plus.ws.*`，不兼容旧键。已知需要修改的位置：
   - `MagicAPIWsConfiguration`、`MagicAPIWsHandlerConfiguration` 的 `@ConditionalOnProperty` 前缀；
   - `MagicAPIWsConfig` 的 `@ConfigurationProperties`；
   - `BaseService` 中 `magic-api.plugin.ws.token.secret must be configured` 异常文案；
   - `fw-plus-boot-autoconfigure` 测试 `MagicAPIServletIsolationAutoConfigurationTest` 中的 `magic-api.plugin.ws.enabled=false` 属性；
   - `fw-plus-magicapi-plugin-ws/README.md` 中全部旧前缀配置项与说明。
   键名层级与默认值保持不变（`enabled` 缺省启用、`websocket.path` 缺省 `/ws/api`、`token.expireHours` 缺省 2 等）。

# 非目标

- 不迁移 WS 的 Java 代码，不改包名、类名、注册方式，不引入 SPI 或抽象层。
- 不为新 starter 写 Java 代码、auto-configuration 或 `spring.factories`（它就是纯 POM）。
- 不为旧前缀 `magic-api.plugin.ws.*` 提供兼容读取或迁移告警。
- 不改变 WS 运行时语义、token 算法、控制台前端功能与控制台交互。
- 不改动 MCP WebSocket 传输（属进行中的 classic change `mcp-websocket-support`），不改仓库中未提交的 `fw-plus-ai-mcp/.../transport/session` 代码。
- 不改动 `fw-plus-boot-autoconfigure` 中其它 magic-api 插件的依赖与注册方式。

# 验收示例

- E1 构建不回归：`cd fw-plus-boot/fw-plus-boot-project && mvn -DskipTests compile` 成功（新模块参与构建）；`fw-plus-magicapi-spring-boot-starter-sample` 仍能编译（前端构建可用 `-Dexec.skip=true` 跳过）。
- E2 依赖传递不缩水：对 `fw-plus-magicapi-plugin-ws` 解析依赖树时，`spring-boot-starter-websocket` 与 `jjwt-api`/`jjwt-impl`/`jjwt-jackson` 仍然出现在编译/运行期依赖中，且来源为 `fw-plus-ws-spring-boot-starter`。

# 约束与不变量

- 构建必须显式使用 JDK 8：`JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home`；Maven 为 `/Users/xiongxiao/Workspaces/Maven/apache-maven-3.6.3/bin/mvn`。
- 新模块必须遵守既有 starter 约定：父 POM `fw-plus-boot-starters`、不覆写 `<packaging>`、`<main.basedir>${basedir}/../../..</main.basedir>`、版本用 `${revision}`。
- 验证构建只用 `compile`/`test`，或对含前端构建的模块加 `-Dexec.skip=true`，避免 `prepare-package` 阶段的 `npm install`/`npm run build` 联网与耗时。
- 依赖上移后运行期可用依赖集合不得缩水：引入 `fw-plus-magicapi-plugin-ws` 的应用仍能拿到 `spring-websocket` 与 `jjwt-*`。
- 不改动与本次无关的其它模块、POM 与未提交改动。

# 决策

- D1 工作区：`current` —— 沿用当前目录与 `dev` 分支。
- D2 抽取形态：starter 为**纯 POM 依赖聚合**，遵守仓库既有 starter 约定；WS 的 Java 实现位置与注册方式保持不变。理由：与 `fw-plus-mqtt-client-spring-boot-starter` 等既有 starter 一致；本仓约定是 starter 只做依赖聚合，代码统一放在 `fw-plus-boot-autoconfigure`。
- D3 模块坐标：`fw-plus-ws-spring-boot-starter`，路径 `fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-ws-spring-boot-starter`。
- D4 依赖集合与去重：starter 依赖 = `fw-plus-boot-starter` + `spring-boot-starter-websocket` + jjwt 三件套；插件侧 jjwt 声明上移到 starter，插件只依赖 starter。
- D5 配置前缀：迁移为 `fw.plus.ws.*`，不兼容旧键。理由：把 WS 的配置命名从 magic-api 解耦，与 starter 的独立命名一致；已核实仓库内没有样例或配置文件使用旧前缀，实际只需改 3 处注解、1 处异常文案、1 处测试属性和 README。

# 待解决问题

无。四个影响用户可见结果的决定（工作区、starter 形态、依赖上移、配置前缀）已由用户确认，其余为实现选择。

# 验证预期

- 结构检查：确认 `fw-plus-ws-spring-boot-starter` 只有 `pom.xml`、无 `src/`；已在 `fw-plus-boot-starters/pom.xml` 与 `fw-plus-boot-dependencies/pom.xml` 中登记。
- 编译检查：`cd fw-plus-boot/fw-plus-boot-project && mvn -DskipTests compile` 通过。
- 依赖树检查：`mvn -pl fw-plus-boot-starters/fw-plus-magicapi-plugin/fw-plus-magicapi-plugin-ws dependency:tree`，确认 `spring-boot-starter-websocket` 与 `jjwt-*` 经 `fw-plus-ws-spring-boot-starter` 传递进来，且插件自身不再直接声明 jjwt。
- 样例检查：`fw-plus-magicapi-spring-boot-starter-sample` 编译通过（`-Dexec.skip=true`）。
- 前缀检查：静态核对代码与 README 中不再出现 `magic-api.plugin.ws` 作为配置前缀；属性绑定验证 `fw.plus.ws.*` 生效、缺省值不变。
