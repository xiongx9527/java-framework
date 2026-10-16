# 目标

为 WebSocket 地基（`com.xiongx9527.fw.plus.boot.autoconfigure.web.ws`）与知识库对外 API（`...langchain4j.retriever.knowledge`）补上逐实例/可配置的认证校验，复用 MCP 已落地的 `fw-plus-auth-core` `InstanceAuthRegistry` provider 机制与同一份 provider 清单。

两个模块都只验证请求携带的凭据，不实现登录、令牌签发、凭据管理，也不实现 Sa-Token 服务端；所有认证均为无界面凭据校验。

# 范围

- WS：默认实例与 `fw.plus.ws.servers[]` 命名实例的握手认证；webmvc（`AbstractWebSocketHandler` / `WsEndpointRegistrar`）与 webflux（`ReactiveWsHandler`）两条栈一致。
- WS：`fw.plus.ws.auth.provider`（默认实例）与 `fw.plus.ws.servers[].auth.provider`（命名实例）配置；凭据来源为握手 query 参数 + 请求头。
- knowledge：对外 Open API（`KbOpenApiController` 的 `/v1/*` 与 `/settings`）认证 provider 化；provider 由 `kb_setting` 设置页配置、保存即热生效。
- knowledge：控制台设置页新增认证 provider 下拉（按 classpath 可用性展示），并新增后端可用 provider 清单接口。
- `fw-plus-boot-autoconfigure`（WS 所在层）引入 `fw-plus-auth-core` 及可选 `fw-plus-auth-satoken` 依赖，复用 `InstanceAuthRegistry`，不重复实现 provider 解析。

# 非目标

- 不实现认证服务：不做登录、令牌签发、凭据管理，不保存任何 secret。
- 不实现 Sa-Token 服务端：不做 OAuth2 授权服务器、SSO 服务端、quick-login 端点、令牌签发。
- 不实现任何带页面操作的认证授权（无登录页、SSO 登录/回调页、授权确认页）；只做无界面凭据校验。
- 不提供 Custom provider：不支持按实例注入 Java 认证 Bean，也不支持 `.ms` function 认证适配。
- quick-login 不纳入 provider（无客户端凭据验证 API）。
- WS 不做授权（无 scope/capability 裁剪）：仅认证；业务级权限判断仍由宿主 `WsBusinessDispatcher` 负责。
- knowledge 不做 scope 授权；不改变数据集/文档/分段业务语义与响应/错误码契约。
- 不改动 WS 既有端点注册、会话管理、消息派发、广播行为（除握手新增认证拒绝路径）。
- 不改动 Magic-API 依赖版本，不改动与认证无关的 Magic-API 工具筛选行为。

# 验收示例

- WS 默认实例与命名实例可逐实例选择 provider（permit-all + 各 sa-token 方式），实例间不继承、不串用；未配置 provider 的实例握手一律拒绝。
- WS 握手凭据可从 query 参数或请求头读取（名称同各 provider 凭据名），两栈行为一致；凭据缺失/无效/未配置 provider 时握手拒绝（webmvc 以 HTTP 401 拒绝且不升级；webflux 拒绝并关闭会话），不进入会话注册与订阅派发。
- WS provider 按 classpath 可选装配，模块缺失时不可选、已配置 fail-closed；未知 provider 抛 `IllegalArgumentException`；只认证不授权；permit-all 必须显式选择，且不作为默认。
- knowledge 对外 API 按设置页 provider 校验，默认 `sa-token-login`（保持现状）；可切换为清单内任一 classpath 可用项；`/settings`（GET/POST）同样受认证保护。
- knowledge provider 模块缺失或未知时 fail-closed（返回 `error_code 1002`，不回退）；控制台下拉只展示 classpath 可用 provider，保存即热生效。

# 约束与不变量

- 复用 `fw-plus-auth-core` 的 `InstanceAuthRegistry` 与 `InstanceAuthProperties`；provider 清单固定为 `permit-all` + `sa-token-login`/`sa-token-oauth2`/`sa-token-jwt`/`sa-token-apikey`/`sa-token-sso`，classpath 条件装配，缺失 fail-closed，未知 provider（含历史 `custom`）抛 `IllegalArgumentException`，与全局及 MCP 语义一致。
- sa-token 版本由 `sa-token.version`（1.46.0）统一管理。
- WS 复用现有握手校验点：webmvc 在 `beforeHandshake` 拒绝并置 HTTP 401；webflux 在 `handle` 拒绝并关闭会话；未配置 provider 即拒绝是本 change 的有意行为变更。
- WS 命名实例的 `auth` 不从顶层 `auth` 继承（与 MCP「各 runtime 不得继承其他实例配置」一致）；`appId` 的继承规则保持不变。
- WS 凭据来源为握手 query 参数与请求头，二者按同一凭据名解析（如 `Authorization: Bearer`、tokenName 默认 `satoken`、`apikey`、`ticket`）；`Authorization` 无法放入浏览器原生 WS 头时可用同名 query 参数承载。
- knowledge 认证配置以 `kb_setting` 单行为准，保存即热生效；`KbSaTokenAuthChecker` 既有凭据兼容（`Authorization: Bearer`、明文 `Authorization`、`kb-token`）必须保留。
- 沿用当前 `dev` 工作区，保留其中已有未提交改动，不清理或覆盖无关文件。
- JDK 1.8；源码 UTF-8。

# 决策

- 用户明确 WS 与 knowledge 两个模块都要加认证；`ws` 指 `com.xiongx9527.fw.plus.boot.autoconfigure.web.ws`。
- 用户选择沿用当前 `dev` 工作区开发（MCP 认证框架代码仍在工作区未提交，必须同工作区复用）。
- 用户选择 WS 未配置 provider 时 fail-closed（未配置即拒绝握手）。
- 用户选择 WS 凭据来源为握手 query 参数 + 请求头。
- 用户选择 WS 仅认证、不做授权（无 scope）。
- 用户选择 knowledge 认证在 `kb_setting` 页面配置并 provider 化；未配置默认 `sa-token-login`（保持现状）；为 `/settings` 补齐鉴权。
- 复用 MCP 同一份 provider 清单与 fail-closed 语义。

# 待解决问题

（无）

# 验证预期

- 覆盖 WS 默认/命名实例的逐实例认证隔离、fail-closed、query+header 凭据、webmvc/webflux 两栈拒绝语义、classpath 缺失与未知 provider、permit-all 显式放行。
- 覆盖 knowledge 对外 API 与 `/settings` 的 provider 选择、默认 sa-token-login、热生效、classpath 缺失/未知 provider 的 fail-closed、控制台可用清单。
- 运行相关模块 Maven 编译与测试。