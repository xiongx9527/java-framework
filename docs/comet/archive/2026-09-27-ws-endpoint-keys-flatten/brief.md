# 目标

把 WS 地基默认实例的端点键从 `fw.plus.ws.websocket.path` / `fw.plus.ws.websocket.paths` 提升到顶层 `fw.plus.ws.path` / `fw.plus.ws.paths`，`websocket:` 下只保留握手参数；旧键（含从未生效的 `websocket.endpoint`、`websocket.basePath`）从配置类与代码里全部删除，不留兼容绑定、也不留为它们服务的告警桩。

完成后：默认实例与命名实例（`servers[]`）的端点键同构，一个实例 = 一份 `path`/`paths` + 一个 `appId`；仓库内不再有任何地方出现旧键。

# 范围

1. **配置键提升**：`WsConfig` 新增顶层 `path`（默认 `/ws/api`）与 `paths`（默认空列表）；同时删除 `Websocket` 下的 `path`、`paths`、`endpoint`、`basePath` 四个字段，`websocket:` 只留握手参数。
2. **读取侧改键**：`WsEndpointPathResolver.resolveRelativePaths(WsConfig)`、`WsInstanceValidator.validate`、`WsBaseService.getWebsocketPath()` 三处从读 `websocket.path/paths` 改为读顶层 `path/paths`；`WsConfig.flatten()` 输出 `path`、`paths` 与 `websocket.trustedDomain`、`websocket.subProtocols`、`websocket.supportsPartialMessages`。
3. **旧键与失真描述清理**：删除 `Websocket.endpoint`（`/ws/execute`）与 `Websocket.basePath` 字段；修正 `WsBasePathProvider` 类级 javadoc 中「默认实现读取 `fw.plus.ws.websocket.basePath`」的失实描述（该接口是 `@FunctionalInterface`，从来没有默认实现）。
4. **javadoc 键名同步**：`WsConfig` 类级注释、`servers` 字段注释、`WsEndpointPathResolver`、`WsInstanceValidator`（含端点冲突报错文案）、`WsAutoConfiguration`、`WsWebFluxAutoConfiguration`、`WsEndpointRegistrar`、`MagicAPIWsConfiguration` 中所有 `fw.plus.ws.websocket.path` / `websocket.paths` 字样改为顶层键。
5. **测试**：既有测试全部改用新键；新增新键绑定与缺省、新键显式空串、扁平键集合契约三类用例；不保留任何旧键用例。
6. **文档与样例**：`fw-plus-ws-spring-boot-starter` README、`fw-plus-magicapi-plugin-ws` README、样例 `application.yml` 与样例 README、`WsSampleApplication` javadoc（其中「握手带 token、`GET /demo/token` 签发」一段已随握手鉴权删除而失效，一并更正）全部同步，并写明「旧键已移除，请改用顶层 `path`/`paths`」。
7. **规格**：`docs/comet/specs/ws-spring-boot-starter/spec.md` 按改造后的完整行为重写受影响场景，新增场景续编号。

# 非目标

- 不动 `servers[]` 命名实例的形态与校验规则（key 必填唯一、端点全局唯一、实例无端点失败、`appId` 留空回落顶层）：上一个 change 刚落地并归档，本次只改默认实例的端点键。
- 不为 `instances` 或 `websocket.path/paths` 保留任何绑定、取值回落或启动告警；旧配置由 Spring 默认忽略，README 承担迁移说明。
- 不启用 `ignoreUnknownFields=false`，不让 `fw.plus.ws` 下任意未知键导致启动失败。
- 不改握手、会话管理、广播、订阅派发与双栈展开矩阵的行为；`WsBaseService`、`WsEndpointRegistrar`、`WsInstanceResolver` 的公开签名一律不变。
- 不动 magic-api ws 插件的 Java 代码：它只引用 javadoc 键名，本次只改其 README 里的两行配置表。
- 不顺手清理 `fw.plus.ws` 之外的其它历史遗留。

# 验收示例

- **A1｜新键绑定与缺省**：`fw.plus.ws.path` / `fw.plus.ws.paths` 可按列表绑定，未配置时取内置默认 `/ws/api` 与空列表；未配 `servers` 时端点集合、会话 appId 与订阅派发和改造前完全一致。
- **A2｜旧键完全失效**：写 `fw.plus.ws.websocket.path=/ws/legacy` 时端点取顶层 `path` 的值（未配则是内置默认 `/ws/api`），旧键既不生效、不告警也不参与取值；`websocket.paths`、`websocket.endpoint`、`websocket.basePath` 同理。
- **A3｜空串语义保留**：`fw.plus.ws.path` 显式置空串时默认实例端点集合退化为「只按基础路径注册」（含只在基础路径后追加一个连接斜杠的口径），与改造前对旧键置空的表现逐条一致，webmvc 与 webflux 两栈相同。
- **A4｜`websocket:` 只剩握手参数**：`Websocket` 中 `path`、`paths`、`endpoint`、`basePath` 四个字段全部删除，全仓无任何代码或文档再出现 `fw.plus.ws.websocket.path` / `websocket.paths` / `websocket.endpoint` / `websocket.basePath`；`WsBasePathProvider` javadoc 不再声称存在读取 `websocket.basePath` 的默认实现。
- **A5｜扁平键与读取侧**：`flatten()` 只产出 `path`、`paths`、`websocket.trustedDomain`、`websocket.subProtocols`、`websocket.supportsPartialMessages` 这几个端点与握手键；`WsBaseService.getWebsocketPath()` 读 `path` 且返回类型不变，magic-api ws 插件取到的配置值不变。
- **A6｜文档与样例同步**：starter README、magicapi-plugin-ws README、样例 `application.yml` 与样例 README 一律写成顶层 `path`/`paths` + `websocket:` 只留握手参数，并标注旧键已移除及迁移写法；样例 javadoc 的失效 token 说明被更正。
- **A7｜测试全绿**：ws 包测试改名后全绿；新增用例覆盖新键绑定与缺省、新键显式空串、扁平键键集合契约；跳过项仅限依赖真实 Redis 的用例。

# 约束与不变量

- JDK 1.8、UTF-8；简体中文注释；缩进跟随周边文件（`config` 包为 Tab，测试类为空格）。
- `servers` 仍不进入 `flatten()`，实例端点展开矩阵不变（默认实例在前、命名实例按序去重）。
- 默认实例始终存在，端点语义（相对路径/完整路径、精确匹配优先于后缀匹配、`WsBasePathProvider` 叉乘）不变。
- 空串是合法且唯一有意义的值形态：`path: ''` 表示「只按基础路径注册」，沿用旧键置空时的同一套判定口径。
- 一个应用只会是 servlet 或 reactive 一种栈，两条栈的端点展开结果保持逐条一致。
- 提交只包含本 change 相关路径，不夹带主工作区 `ai-magicapi-mcp-plugin` 的在途改动。

# 决策

- **D1 旧键直接删除，不做兼容绑定**：`Websocket` 下 `path`、`paths` 字段连同 `endpoint`、`basePath` 一并删除，`WsConfig` 顶层 `path`/`paths` 是端点键的唯一来源，也没有 `@Deprecated` 兼容字段与 `explicitFields` 记账。理由：端点键只有一个来源时，读取侧、`flatten()`、校验规则三者不会再有分歧；留两个来源就要为「谁优先、空串算不算显式配置」长期买单，而兼容字段在宿主侧只会变成编译告警噪音。存量配置改用顶层键即可，README 写明迁移方式。
- **D2 生效值直接读新键**：`WsConfig.getPath()` / `getPaths()` 就是新键的字面值，没有多级回落。理由：D1 删掉旧键后已无第二来源，配置类回到朴素的 data holder。
- **D3 空串判定与旧行为对齐**：`path: ''` 仍表示「只按基础路径注册」，判定只看值本身，不引入额外标记。理由：这一语义是存量文档写明过的行为，改造前后必须逐条一致。
- **D4 死键直接删除**：`websocket.endpoint`、`websocket.basePath` 删除字段与扁平键。理由：它们从未被任何代码读取，保留只会让用户继续写无效配置。
- **D5 不引入旧键告警**：本轮不加 `WsLegacyKeyGuard` 之类的启动扫描。理由：用户的指令是旧键全部删除，留着一段只为已删键服务的探测代码本身就是残留；且旧键里唯一有实际端点作用的是 `websocket.path`，缺省值恰好与内置默认 `/ws/api` 一致，未自定义路径的存量配置行为不变，自定义路径的则需要按 README 主动迁一次。若后续要提示迁移，应作为独立的配置校验能力来做，不塞进本次端点键提升。

# 待解决问题

（无）

# 验证预期

- `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.**.*Test'` 全绿；阶段结束跑模块全量测试回归（`./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test`）。
- 新增 `WsConfigBindingTest` 用例：新键绑定与缺省、新键显式空串、扁平键键集合契约、`Websocket` 中不再暴露 `path/paths/endpoint/basePath`。
- 既有测试跟随改名：`WsAutoConfigurationTest`、`WsWebFluxAutoConfigurationTest`、`WsEndpointHttpUpgradeTest`、`MagicAPIWsWiringTest`、`ReactiveWsHandlerTest`、`WsInstanceValidatorTest`、`WsBaseServiceTest`，行为断言不变。
- 依赖真实 Redis 的用例（`WsRedisBroadcastEndToEndTest`）未设环境变量时整类跳过，跳过不计通过。
