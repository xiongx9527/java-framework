# 目标

为 MCP Server 的每个实例提供独立、可选择的认证验证方式，避免同一应用中的实例共享授权规则；Starter 与 Magic-API 插件均支持逐实例配置。MCP server 只负责验证请求携带的凭据，不实现认证服务，也不实现 Sa-Token 服务端。

# 范围

- MCP Server Starter 的默认 runtime 与 `fw-plus.ai.mcp.servers[]` 命名 runtime。
- Magic-API MCP Server 插件，包括 `magic-api.plugin.mcp.servers[]` 配置、动态 `mcp-server` 资源及 runtime 创建/销毁路径。
- Servlet MVC 与 WebFlux 两种 MCP 请求入口。
- Starter、Magic-API 静态/动态实例的逐实例认证选择/配置，以及 Magic-API 控制台表单编辑。
- sa-token 依赖升级 1.44.0 → 1.46.0（含 `fw-plus-auth-satoken` 覆写类重新对齐）。
- 新增 Sa-Token 各认证方式所需的可选依赖模块与逐模块 classpath 条件装配。

# 非目标

- 不实现认证服务：不做登录、令牌签发、凭据管理，MCP 不保存任何 secret。
- 不实现 Sa-Token 服务端：不做 OAuth2 授权服务器、SSO 服务端、quick-login 登录端点；MCP 仅以 Sa-Token 客户端视角调用验证类 API。
- 不实现任何带页面操作的认证授权：MCP server 不提供登录页、SSO 登录/回调页、授权确认页等需要浏览器页面交互的认证或授权入口；所有认证方式只能通过请求携带的凭据头/参数完成无界面验证。（本条限定于认证授权，不限制 MCP 其他能力的展示形式。）
- 不提供 Custom provider：不支持按实例注入 Java `McpAuthService` Bean，也不支持 Magic-API `.ms` function 认证适配。
- quick-login 方式不纳入 provider（该模块只有服务端控制器与 servlet filter，没有可供客户端调用的凭据验证 API）。
- 改动与 MCP 请求授权无关的 Magic-API 工具筛选行为。

# 验收示例

- 每个 MCP runtime 的授权配置独立生效，实例间不能因共享认证组件而互相继承或串用授权规则。
- Starter 每个 runtime 可逐实例选择 PermitAll 或任一可用的 Sa-Token 认证方式（登录 token、OAuth2、JWT、API Key、SSO ticket），未配置时拒绝。
- Magic-API 每个实例可选择与 Starter 同一份 provider 清单；控制台表单按 classpath 可用性展示可选项，资源只存 provider 与非敏感选择字段。
- 各 Sa-Token provider 按 classpath 可选装配：对应模块缺失时该 provider 不可选，选中缺失模块的 provider 时 fail-closed（拒绝请求而非回退到其他 provider）。
- 认证成功的主体必须携带 scopes，scopes 可为多个值或空集；OAuth2/API Key 取凭据原生 scopes，JWT 取 claim，登录 token 与 SSO ticket 无原生 scopes 时为空集。
- MVC 与 WebFlux 对未认证/无效主体返回 401 且不进入 MCP 内核；认证通过但目标 scope 不足（含空 scopes）时返回 403；显式 PermitAll 绕过 scope 检查。
- sa-token 升级到 1.46.0 后，`fw-plus-auth-satoken` 覆写的 `StpLogic`/`SaTokenConfig` 与上游新版本对齐且保留本地 `tokenNames` 回退补丁，相关模块编译与测试通过。

# 约束与不变量

- 当前 `McpRuntimeFactory` 把同一个 `McpAuthService` 传给所有 runtime；`McpRequest` 含 endpoint、transport、session 和请求头，但没有 runtime key。
- Magic-API MCP 插件通过 `MagicDynamicMcpServer` 创建独立 runtime，但同样注入共享 `McpAuthService`；当前 `MagicMcpServerInfo` 尚无授权字段。
- 插件实例既可由 YAML 配置创建，也可由 Magic-API `mcp-server` 资源动态维护；插件 runtime 创建路径当前注入共享 `McpAuthService`。server key 已是控制台可维护字段。
- 现有 capability scope 裁剪是可选项。命名 runtime 配置合并及插件 runtime 创建都没有传播 `scopeCheckEnabled`，相关授权配置需确保在实例级确实生效。
- 会话和能力注册表按 runtime 隔离；新增授权不得削弱已有会话归属校验。
- 用户选择在当前 `dev` 工作区开发；保留其中已有未提交改动，不清理或覆盖无关文件。
- sa-token 版本由 `fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml` 的 `sa-token.version` 统一管理；`fw-plus-auth-satoken` 以同包名覆写 `StpLogic`/`SaTokenConfig`，升级必须逐行合并上游差异并保留本地 `tokenNames` 补丁（1.44→1.46 上游差异约 133 / 58 行）。
- sa-token 1.46.0 编译目标为 Java 1.8，与本仓 JDK 1.8 约束兼容。

# 决策

- 用户确认“单独授权”作为本次目标，并明确 Magic-API MCP Server 插件也纳入范围。
- 用户选择沿用当前工作区；已有未提交修改必须保留。
- 用户选择每个 MCP Server 实例使用独立认证提供方/凭据，而非仅为共享认证配置实例 scopes。
- 用户选择未配置实例授权时默认拒绝访问。
- 用户将 Magic-API MCP Server 控制台表单加入认证配置纳入范围，并要求 MCP Server Starter（默认实例及每个命名实例）也支持逐实例配置，两者配置语义保持同步。
- 用户要求 Starter 与 Magic-API 也显式支持 `PermitAllAuthService`；它必须是逐实例显式选择，未配置时仍 fail-closed。
- 用户确认显式选择 `PermitAllAuthService` 时绕过 capability scope 检查；PermitAll 不作为任何实例的默认认证方式。
- 用户要求各 provider 都由真实 `McpAuthService` 实现承载，不只增加 UI 或配置字段。
- 用户要求将 sa-token 升级到最新版本（Maven Central 最新 release 1.46.0，当前 1.44.0），与本次 MCP 认证改动同时进行；升级需同步重新对齐 `fw-plus-auth-satoken` 覆写的 `StpLogic`/`SaTokenConfig` 与本地 `tokenNames` 补丁，不丢弃本地修改。
- 用户明确 MCP server 只负责验证（校验请求携带的凭据），不负责实现认证服务：登录、令牌签发、凭据管理等均不进入 MCP。
- 用户明确不实现 Sa-Token 服务端：不做 OAuth2 授权服务器、SSO 服务端、quick-login 登录端点、令牌签发。
- 用户明确 MCP server 不实现任何带页面操作的认证授权（无登录页、无 SSO 登录/回调页、无授权确认页）；认证只能走无界面的凭据验证（请求头/参数携带 token、ticket、api key 等）。该限制只针对认证授权，不限制 MCP 其他能力的展示形式。
- Q1（已答）：用户选择严格全部 Sa-Token 认证方式作为内置 provider 覆盖范围，不只限于 OAuth2。
- Q2（已答）：移除 Custom provider，不再支持按实例注入 Java `McpAuthService` Bean 或 Magic-API `.ms` function 认证。
- Q3（已答）：MCP server 相当于 Sa-Token 的客户端——provider 覆盖所有可从客户端验证的方式：登录 token（`StpUtil`）、OAuth2 access token、JWT、API Key、SSO ticket（客户端 `buildCheckTicketMessage`/`pushMessage` 向外部 SSO 服务端校验）；quick-login 无客户端验证 API，按事实排除。
- Q4（已答）：Starter 与 Magic-API 使用同一份 provider 清单与相同配置语义；每个 Sa-Token 模块按 classpath 可选装配，模块缺失时该 provider 不可选并 fail-closed，不回退到其他 provider。
- Q5（已答）：无原生 scopes 的凭据（登录 token、SSO ticket）认证成功后主体 scopes 取空集；开启 scope 检查时按既有“空 scopes → 403”规则处理，不新增 scope 映射配置。
- 用户要求认证结果主体必须携带 scopes，scopes 可包含多个值，也可以为空；capability scope 检查按既有规则消费该字段。

## 被推翻的决策（superseded）

- 原“三类 provider：`PermitAllAuthService`、`SaTokenOAuth2McpAuthService`、自定义”由 Q1/Q2 取代：provider 改为 PermitAll + Sa-Token 客户端可验证的全部认证方式，移除 Custom。
- 原“Magic-API 实例支持 Spring Bean 或 `.ms` function 两类自定义认证”由 Q2 取代：Custom provider 整体移除。
- 原“API key 校验由 Custom Java Bean 或 `.ms` function 实现”由 Q2/Q3 取代：API key 改由内置 `sa-token-apikey` provider 承载。
- 原“不提供第四种内置 API key provider”由 Q1/Q3 取代：API key 成为 Sa-Token 内置 provider 之一。
- 原“不新增 OAuth2/OIDC 配置；已有 Sa-Token OAuth2 自定义认证实现可作为按实例绑定的 Java `McpAuthService` 使用”中“自定义实现绑定”部分由 Q2 取代：OAuth2 验证保留为内置 provider，但不再由应用注入自定义 Bean。

# Open questions

# 待解决问题

# 验证预期

- 覆盖默认 runtime、命名 runtime、Magic-API YAML 实例和动态资源实例的实例级授权隔离及缺省行为。
- 对 MVC 与 WebFlux 分别验证合法授权放行、缺少/无效授权拒绝、其他实例授权不能用于当前实例。
- 验证 Starter 默认/命名 runtime 与 Magic-API 静态/动态实例均按各自选择的认证方式独立授权，创建、更新、启停、删除路径绑定正确。
- 验证 Magic-API 表单可选 provider 与 Starter 清单一致（PermitAll + 可用的 Sa-Token 认证方式），无 Custom 选项、无凭据写入资源。
- 验证各 Sa-Token provider 的 401/403 语义、主体 scopes 多值与空集两种形态、classpath 缺失时的 fail-closed 行为。
- 验证 MCP 侧不暴露任何 Sa-Token 服务端能力（无授权端点、无登录端点、无令牌签发）。
- 验证 MCP 侧不存在带页面操作的认证授权入口（无登录页、无 SSO 登录/回调页、无授权确认页），全部认证均为无界面凭据验证。
- 验证 sa-token 1.46.0 升级后覆写类与本地补丁完整保留，相关模块 Maven 编译与测试通过。
- 运行相关 MCP autoconfigure、Magic-API MCP 插件测试，以及受影响模块的 Maven 验证。
