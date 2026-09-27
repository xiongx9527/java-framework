# 目标

把 ws 地基命名实例的配置形态对齐 MCP 多实例：`fw.plus.ws.instances` 改名为 `fw.plus.ws.servers`，实例内 `name` 改名为 `key`（必填非空、全局唯一），并补齐与 MCP `McpRuntimeFactory` 一致的启动期校验（key 缺失或重复、端点跨实例重复均启动失败）；命名实例未配 `appId` 时继承顶层 `fw.plus.ws.appId`。

完成后：`fw.plus.ws.servers[]` 每项一个 `key` + 一组端点（`path`/`paths`）+ 一个 `appId`，配置写法与 `fw-plus.ai.mcp.servers[]` 同构；默认实例始终存在且行为不变；解析、注册、派发链路只跟随改名，不改变匹配语义。

# 范围

1. **配置改名**：`WsConfig.instances` 改名为 `servers`，`WsConfig.WsInstance.name` 改名为 `key`；javadoc 与注释同步。`servers` 仍不进入 `flatten()` 扁平键。
2. **启动期校验**（对齐 MCP）：实例 `key` 缺失、空串或纯空白时启动失败；`key` 在 `servers` 内重复（trim 后）时启动失败；实例端点（`path` 或 `paths`，规范化后）与默认实例端点或另一实例端点重复时启动失败；实例只配 `key` 未配任何端点时启动失败。webmvc 与 webflux 两条栈共用同一校验逻辑，各自在装配路径上调用。
3. **链路跟随改名**：`WsInstanceResolver`、`WsEndpointPathResolver`、`AbstractWebSocketHandler`、`ReactiveWsHandler` 等只做字段名与文案更新，匹配语义（精确匹配整轮优先、后缀边界匹配）与端点展开矩阵不变。
4. **测试**：ws 包既有测试（绑定、解析、注册、握手、双栈）跟随改名；新增 key 必填、key 唯一、端点唯一、实例无端点四类校验用例与旧键绑定用例。
5. **文档与样例**：`fw-plus-ws-spring-boot-starter` README、样例 `fw-plus-ws-spring-boot-starter-sample` 的 `application.yml` 与样例 README 同步改为 `servers`/`key` 形态。
6. **规格**：更新 `docs/comet/specs/ws-spring-boot-starter/spec.md` 为改造后的完整行为（既有场景按新形态重述，新增场景续编号）。

参照物为仓库内已归档的 MCP 多实例实现（`McpProperties.servers[]`、`McpRuntimeFactory#validateKeys`/`validateEndpoints`）与用户在对话中给出的 MCP 配置示例，属实现参考，无外部来源文件，不触发源文档完整覆盖模式。

# 非目标

- 不改 magic-api ws 插件代码与数据源语义：`ws['xxx']` 仍按 appId 取用，`WsInstanceResolver` 对外 API 签名不变（用户已明确「先不管 magic」）。
- 不移植 MCP 的 `explicitFields` 显式记账机制：ws 实例字段默认值即空串，「留空即继承顶层」不需要记账。
- 不新增顶层 `fw.plus.ws.key`：ws 没有 MCP 那种顶层单实例回落键，默认实例由 `appId` 标识。
- 不做旧键兼容别名：`instances`/`name` 移除后按 Spring 绑定默认行为处理（见验收示例 A2），不为旧键保留读取路径。
- 不启用 `ignoreUnknownFields=false`，不让 `fw.plus.ws` 下任意未知键导致启动失败。
- 不改握手、会话管理、广播、订阅派发等其余 ws 行为。

# 验收示例

- **A1｜Scenario**：配置形态 —— `fw.plus.ws.servers` 可按列表绑定，每项字段为 `key`、`path`、`paths`、`appId`；默认实例仍取顶层 `websocket.path`/`websocket.paths` 与 `appId`；未配 `servers` 时端点集合、会话 appId 与订阅派发和改造前完全一致。
- **A2｜Scenario**：旧键移除 —— YAML 写旧键 `fw.plus.ws.instances` 或实例内 `name` 时不再被绑定（Spring 默认忽略未知字段），不产生任何实例端点，默认实例行为不变，应用正常启动。
- **A3｜Scenario**：key 必填 —— 任一实例 `key` 缺失、空串或纯空白时启动失败，错误信息含 `fw.plus.ws.servers[].key`。
- **A4｜Scenario**：key 唯一 —— 两个实例 `key` 相同（trim 后）时启动失败，错误信息给出冲突的 key。
- **A5｜Scenario**：端点唯一 —— 实例端点与默认实例端点、或与另一实例端点（规范化后）重复时启动失败，错误信息含冲突路径与相关 key；同一实例内部 `path` 与 `paths` 重复仍按配置顺序去重、不失败；只配 `key` 不配任何端点同样启动失败。
- **A6｜Scenario**：appId 继承与两栈一致 —— 实例未配 `appId`（留空）时握手会话 appId 取顶层 `fw.plus.ws.appId`，显式配置时取实例值；webmvc 与 webflux 两栈判定一致。
- **A7｜Scenario**：文档与样例同步 —— starter README 与样例 `application.yml` 只出现 `servers`/`key` 形态，无 `instances`/`name` 残留；ws 自动配置模块 ws 包测试改名后全绿（跳过项仅限依赖真实 Redis 的用例）。

# 约束与不变量

- JDK 1.8、UTF-8；简体中文注释与日志；缩进跟随周边文件（`config` 包为 Tab）。
- `flatten()` 扁平键集合不变，不新增实例相关键。
- `WsInstanceResolver` 匹配语义不变：精确匹配整轮优先于后缀匹配，后缀按边界匹配（`/my-api` 不命中 `/api`）；未命中或实例 `appId` 留空回落顶层 `fw.plus.ws.appId`。
- 默认实例始终存在，`servers` 是默认实例之外的纯增量。
- 一个应用只会是 servlet 或 reactive 一种栈，校验与展开规则两端口径一致，不构造双栈共存上下文。
- `WsBaseService` 全部 getter 名与返回类型不变；`WsEndpointRegistrar` 构造器签名不变。
- 提交只包含本 change 相关路径，不夹带主工作区其它在途改动。

# 决策

- **D1 改名范围**：只改配置键与字段名（`instances`→`servers`、`name`→`key`），类名 `WsInstance`、解析器 `WsInstanceResolver` 等保持稳定，降低联动面。旧键不做兼容：`instances` 已随上一个 change 落地一天且未发版，长期保留两套形态反而增加认知负担；移除后旧键按 Spring 默认被静默忽略（见 A2）。
- **D2 key 语义**：`key` 是实例唯一标识（trim 后），必填非空、`servers` 内全局唯一，否则启动失败。理由：key 用于配置识别、日志与端点冲突报错，重复时归属不可分辨（对齐 MCP `validateKeys`）。
- **D3 端点唯一**：端点唯一性按「默认实例 + 全部命名实例」校验，规范化（补前导斜杠、去尾斜杠）后精确相等即冲突，启动失败（对齐 MCP `validateEndpoints`）；同一实例内部 `path` 与 `paths` 重复仍去重。理由：端点重复时后缀匹配会让后声明的一方永远无法被命中，属配置错误而非可运行状态。
- **D4 空端点实例**：只配 `key` 不配 `path`/`paths` 的实例永远无法被握手命中，启动失败，避免「配了却不生效」的静默误导。
- **D5 继承语义**：`appId` 沿用「留空回落顶层 `fw.plus.ws.appId`」，不引入 explicitFields：实例字段默认值即空串，留空与未配效果相同，无需也无法区分。
- **D6 校验位置**：校验逻辑收为一个静态方法，由 `WsAutoConfiguration`（webmvc）与 `WsWebFluxAutoConfiguration`（webflux）在装配路径上各调用一次；不放进 `registerWebSocketHandlers`——宿主未开 `@EnableWebSocket` 时该方法不会执行，做不到启动即失败。
- **D7 收尾方式**：归档后按用户此前授权，把 change 分支本地合并回 dev 并提交推送。

# 待解决问题

（无）

# 验证预期

- `cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.**.*Test'` 全绿；阶段结束跑模块全量测试回归（`./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test`）。
- 新增/扩充测试：`servers`/`key` 绑定与旧键绑定（`WsConfigBindingTest`）；key 必填、key 唯一、端点冲突（与默认实例、实例间）、实例无端点（新增校验测试）；既有 `WsInstanceResolverTest`、`WsEndpointPathResolverTest`、`WsAutoConfigurationTest`、`WsWebFluxAutoConfigurationTest`、`WsEndpointHttpUpgradeTest`、`WsHandshakeValidationTest`、`MagicAPIWsWiringTest` 改名后行为断言不变。
- 依赖真实 Redis 的用例（`WsRedisBroadcastEndToEndTest`）未设环境变量时整类跳过，跳过不计通过。
