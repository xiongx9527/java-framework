---
generated_from_state_version: 37
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 7
- 迭代: 2
- 验证器尝试次数: 1
- 完成时间: 2026-10-06T06:15:02.424Z
- 摘要: 本轮修复仅改 fw-plus-auth-satoken 的 SaTokenConfig/StpLogic 两个同包覆写类与其 pom。独立核对确认两个覆写类已重新落回、与 sa-token 1.46.0 上游逐行对齐，本地 tokenNames 回退补丁（字段/getter/setter、getTokenValueByTokenName 与遍历回退）完整保留，与既有 HEAD 补丁语义一致；模块依赖统一由 BOM sa-token.version=1.46.0 管理，全仓无 1.44.0 残留，编译产物含补丁。A1–A6 核心实现（InstanceAuthRegistry、5 个 SaToken provider、McpAuthService 适配、MVC/WebFlux 401/403 与 scope 检查、Magic-API 存储校验与控制台 provider 选择、/mcp/server/authProviders）及其测试均在，579 项 autoconfigure 测试通过，无回归。Runtime 回执与当前候选（candidateId 9accde24、stateVersion=34、inputFingerprint 8eb3cdf0）一致且均为只读/编译命令。A1–A7 逐项判 passed，最终 verdict=pass。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | 每个 MCP runtime 的授权配置独立生效，实例间不能因共享认证组件而互相继承或串用授权规则。 | fw-plus-auth-core 新增 InstanceAuthRegistry 按 instanceKey 解析 provider，各 runtime 独立绑定；McpRuntimeFactory 与 MagicDynamicMcpServer 按实例注入 auth；McpRuntimeFactoryTest（含 shouldNotInheritTopLevelAuthWhenNamedRuntimeOmitsIt）、MagicApiMcpServerRegistryTest、MagicDynamicMcpServerTest 均仍在，命名实例不继承顶层授权。 |
| A2 | passed | brief.md | Starter 每个 runtime 可逐实例选择 PermitAll 或任一可用的 Sa-Token 认证方式（登录 token、OAuth2、JWT、API Key、SSO ticket），未配置时拒绝。 | Starter 默认与命名 runtime 逐实例选择 provider：McpProperties/MagicApiMcpProperties 增字段；InstanceAuthRegistry.availableProviders() 输出 PermitAll + 5 种 Sa-Token 方式（login/oauth2/jwt/apikey/sso）；未配置时 resolve 返回 null→401。本轮修复未触及这些文件，无回退。 |
| A3 | passed | brief.md | Magic-API 每个实例可选择与 Starter 同一份 provider 清单；控制台表单按 classpath 可用性展示可选项，资源只存 provider 与非敏感选择字段。 | Magic-API 侧复用同一份 InstanceAuthRegistry.availableProviders()；控制台 service/magic-mcp-server.js 调 /mcp/server/authProviders，magic-mcp-server-info.vue 按返回清单渲染；MagicMcpServerInfo/MagicMcpServerInfoMagicResourceStorage 仅存 provider 与非敏感字段，MagicMcpServerStorageTest 覆盖 custom 拒绝与 auth 往返。 |
| A4 | passed | brief.md | 各 Sa-Token provider 按 classpath 可选装配：对应模块缺失时该 provider 不可选，选中缺失模块的 provider 时 fail-closed（拒绝请求而非回退到其他 provider）。 | InstanceAuthRegistry 各分支用 available(CLASS_...) ? 实例 : reject(...) 明确 fail-closed；classpath 缺失 provider 不出现在清单、已配置则恒拒绝不回退；McpAuthServiceRegistryTest 覆盖缺失模块与 custom/unknown。 |
| A5 | passed | brief.md | 认证成功的主体必须携带 scopes，scopes 可为多个值或空集；OAuth2/API Key 取凭据原生 scopes，JWT 取 claim，登录 token 与 SSO ticket 无原生 scopes 时为空集。 | McpPrincipal 复用主体 scopes：OAuth2 取 accessToken.getScopes()，API Key 取 ApiKeyModel.getScopes()，JWT 取 scopes/scope claim，登录 token 与 SSO ticket 无原生 scopes 时为空集；SaTokenJwtAuthServiceTest 等覆盖多值/空集/畸形。 |
| A6 | passed | brief.md | MVC 与 WebFlux 对未认证/无效主体返回 401 且不进入 MCP 内核；认证通过但目标 scope 不足（含空 scopes）时返回 403；显式 PermitAll 绕过 scope 检查。 | 回归证据充分：MagicMcpDiagnosticControllerAuthTest（诊断缺认证 401、无 scope 403）、McpAuthEntryMvcTest（SSE 建连前拒绝、Streamable GET/DELETE 拒绝且不进内核）、McpAuthEntryFluxTest、McpScopeEntryMvcTest/McpScopeEntryFluxTest（缺 scope/空 scopes 拒绝、PermitAll 绕过）均存在；Runtime mcp-autoconfigure-tests 579 项全通过。 |
| A7 | passed | brief.md | sa-token 升级到 1.46.0 后，`fw-plus-auth-satoken` 覆写的 `StpLogic`/`SaTokenConfig` 与上游新版本对齐且保留本地 `tokenNames` 回退补丁，相关模块编译与测试通过。 | 独立逐行 diff（上游 CRLF 已 tr -d '\r'）：SaTokenConfig 相对上游 1.46.0 仅 tokenNames 字段（默认 {"satoken"}）与 getter/setter（19 行），StpLogic 仅 getTokenValueByTokenName 辅助方法与 getTokenValueNotCut 中 tokenNames 遍历回退（56 行），无其他偏离；fw-plus-auth-satoken/pom.xml 5 个 sa-token 依赖均为 ${sa-token.version}，BOM sa-token.version=1.46.0，全仓无 1.44.0 残留；javap 确认 target/classes 内 SaTokenConfig.class（含 tokenNames/getTokenNames/setTokenNames）与 StpLogic.class（含 getTokenValueByTokenName），class 时间晚于源码与 pom，即按当前源码+pom 编译。Runtime satoken-1.46-override-build 与 satoken-tokenNames-patch-present 均 passed。 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| fw-plus-auth-satoken 在 sa-token 1.46.0 下编译安装（覆写类对齐） | -o -pl fw-plus-core/fw-plus-project/fw-plus-auth/fw-plus-auth-satoken -am install -DskipTests | . | passed | 0 | 5850 ms |
| 覆写类保留本地 tokenNames 回退补丁 | -c set -e; grep -q tokenNames fw-plus-core/fw-plus-project/fw-plus-auth/fw-plus-auth-satoken/src/main/java/cn/dev33/satoken/config/SaTokenConfig.java; grep -q tokenNames fw-plus-core/fw-plus-project/fw-plus-auth/fw-plus-auth-satoken/src/main/java/cn/dev33/satoken/stp/StpLogic.java; echo tokenNames-patch-present | . | passed | 0 | 16 ms |
| MCP autoconfigure 完整测试套件（579 个测试） | -o -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test | . | passed | 0 | 45992 ms |
| 两个 MCP starter 全量编译安装（覆盖 boot 层 sa-token 1.46.0） | -o -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-mcp-server,fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-mcp-server-spring-boot-starter -am install -DskipTests | . | passed | 0 | 71353 ms |
| Magic-API MCP Server 控制台 vite 构建 | run build | fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/fw-plus-ai-magicapi-plugin-mcp-server/src/console | passed | 0 | 2061 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- fw-plus-auth-satoken 在 sa-token 1.46.0 下编译/安装（-am install -DskipTests）: passed — ./mvnw -o -pl fw-plus-core/fw-plus-project/fw-plus-auth/fw-plus-auth-satoken -am install -DskipTests，exit=0；target/classes/cn/dev33/satoken/{config/SaTokenConfig.class,stp/StpLogic.class} 已生成。
- 覆写类含本地 tokenNames 补丁: passed — 两个文件含 tokenNames（SaTokenConfig 字段+getter/setter、StpLogic getTokenValueByTokenName 回退与遍历）；与上游 1.46.0 逐行 diff 仅本地补丁差异。
- fw-plus-ai-boot-autoconfigure 完整测试套件: passed — ./mvnw -o -pl fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test，Tests run: 579, Failures: 0, Errors: 0，BUILD SUCCESS。
- fw-plus-boot-autoconfigure + fw-plus-magicapi-plugin-satoken 编译安装: passed — ./mvnw -o -pl fw-plus-boot/fw-plus-boot-project/fw-plus-boot-autoconfigure,fw-plus-boot/.../fw-plus-magicapi-plugin-satoken -am install -DskipTests，exit=0（含插件控制台 vite build 产物 magic-satoken.1.0.0.iife.js）。
- 已知限制: fw-plus-auth-satoken 本身无单元测试；覆写类行为由其编译进产物及下游 AI autoconfigure 579 项测试间接覆盖。
- 已知限制: SSO/OAuth2 未联网、未对接外部 Sa-Token 服务端（SSO 走 isHttp=false 的 redis 模式，测试不联网）。
- 已知限制: 控制台改动仅做 vite 静态构建验证，未在真实 Magic-API 控制台页面交互验证。
- 已知限制: 所有改动仍未提交 Git（isolation=current，分支 dev），按 Comet 协议在 Archive 阶段再按授权提交。

## 阻塞项

_无。_

## 风险与跳过的工作

- 低：Runtime satoken-1.46-override-build 日志为 'Nothing to compile - all classes are up to date'（重复 install 而非新鲜重编译）；编译完整性仍成立（class 文件晚于源码/pom 且含补丁），独立证据来自逐行 diff + javap。
- 低：satoken-tokenNames-patch-present 为 grep -q 弱断言；独立验收以逐行 diff + javap 提供更强证据。
- 低：fw-plus-auth-satoken 自身无单元测试，补丁行为仅由编译产物及下游 AI autoconfigure 579 项测试间接覆盖。
- 低：OAuth2/SSO 未联网、未对接真实 Sa-Token 服务端；控制台仅 vite 静态构建验证。
- 低：所有改动仍未提交 Git（isolation=current，分支 dev），按 Comet 协议待 Archive 阶段提交。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 0 | recovery | — | Native confirmed acceptance criteria changed | 2026-10-05T09:55:06.256Z |
| 2 | 1 | 1 | fail | A2, A3 | 独立验收发现 A2 与 A3 存在可复现的授权绕过路径；A1 已通过。Runtime 检查回执对应当前候选且完整 autoconfigure 测试通过，但缺少 diagnostics 与 MVC SSE 建连、Streamable GET/DELETE 的拒绝及不调用内核覆盖。需要回 Build 修复并补测试。 | 2026-10-05T10:02:44.732Z |
| 2 | 2 | 0 | recovery | — | Native Shape artifacts changed | 2026-10-05T10:07:01.410Z |
| 3 | 1 | 1 | fail | A2, A3 | A1 通过；A2 的诊断授权和 A3 的 MVC/WebFlux 拒绝一致性仍有 scope 授权缺口。Runtime 完整测试回执与当前候选绑定且通过；已有 scope 测试揭示缺失 scope 放行及状态码差异，无需重复全量测试。回 Build 严格执行已启用 scope 检查，并在 WebFlux 以 403 表示 scope 拒绝。 | 2026-10-05T10:13:37.725Z |
| 3 | 2 | 0 | recovery | — | Native Shape artifacts changed | 2026-10-05T10:18:42.459Z |
| 4 | 1 | 1 | pass | — | 独立审查 brief、完整规格、当前实现、测试和本轮 Runtime 检查回执；三个验收项均通过。Runtime 全量测试日志显示 551 项通过，工作区/候选与检查回执匹配。未修改文件。 | 2026-10-05T10:28:30.337Z |
| 4 | 1 | 1 | recovery | — | 用户要求 Magic-API MCP server 表单增加认证凭据配置（如 API key），与已确认的按 key 绑定应用 McpAuthService 且凭据不入资源/控制台的设计相冲突；需要返回 Shape 修订需求并评估凭据持久化安全约束。 | 2026-10-05T10:45:38.055Z |
| 5 | 0 | 0 | recovery | — | Native confirmed acceptance criteria changed | 2026-10-05T11:03:48.816Z |
| 6 | 1 | 0 | recovery | — | Native confirmed acceptance criteria changed | 2026-10-05T16:04:13.773Z |
| 7 | 1 | 1 | fail | A7 | 独立只读验收 brief、两份 spec、comet-state.yaml(A1–A7)、核心实现、关键测试与 3 份 Runtime 回执。A1–A6 逐项有明确实现与测试证据，判 passed；历史缺口（diagnostics 401、MVC SSE 建连、Streamable GET/DELETE 拒绝且不进内核、WebFlux scope 不足 403）均已有对应测试覆盖。A7 判 failed：fw-plus-auth-satoken 的 StpLogic/SaTokenConfig 覆写类被整体删除（-4216 行、无替代、tokenNames 补丁无残留），直接违反验收文字与'不丢弃本地修改'决策，且该模块仍硬编码 sa-token 1.44.0 与 BOM 1.46.0 不一致。Runtime 检查回执与当前候选（basedOnStateVersion=31、candidateId=f950cc70）一致且无副作用，但不足以支撑 A7。最终 verdict=fail。 | 2026-10-06T06:01:55.822Z |
| 7 | 2 | 1 | pass | — | 本轮修复仅改 fw-plus-auth-satoken 的 SaTokenConfig/StpLogic 两个同包覆写类与其 pom。独立核对确认两个覆写类已重新落回、与 sa-token 1.46.0 上游逐行对齐，本地 tokenNames 回退补丁（字段/getter/setter、getTokenValueByTokenName 与遍历回退）完整保留，与既有 HEAD 补丁语义一致；模块依赖统一由 BOM sa-token.version=1.46.0 管理，全仓无 1.44.0 残留，编译产物含补丁。A1–A6 核心实现（InstanceAuthRegistry、5 个 SaToken provider、McpAuthService 适配、MVC/WebFlux 401/403 与 scope 检查、Magic-API 存储校验与控制台 provider 选择、/mcp/server/authProviders）及其测试均在，579 项 autoconfigure 测试通过，无回归。Runtime 回执与当前候选（candidateId 9accde24、stateVersion=34、inputFingerprint 8eb3cdf0）一致且均为只读/编译命令。A1–A7 逐项判 passed，最终 verdict=pass。 | 2026-10-06T06:15:02.424Z |



## 结论

本轮修复仅改 fw-plus-auth-satoken 的 SaTokenConfig/StpLogic 两个同包覆写类与其 pom。独立核对确认两个覆写类已重新落回、与 sa-token 1.46.0 上游逐行对齐，本地 tokenNames 回退补丁（字段/getter/setter、getTokenValueByTokenName 与遍历回退）完整保留，与既有 HEAD 补丁语义一致；模块依赖统一由 BOM sa-token.version=1.46.0 管理，全仓无 1.44.0 残留，编译产物含补丁。A1–A6 核心实现（InstanceAuthRegistry、5 个 SaToken provider、McpAuthService 适配、MVC/WebFlux 401/403 与 scope 检查、Magic-API 存储校验与控制台 provider 选择、/mcp/server/authProviders）及其测试均在，579 项 autoconfigure 测试通过，无回归。Runtime 回执与当前候选（candidateId 9accde24、stateVersion=34、inputFingerprint 8eb3cdf0）一致且均为只读/编译命令。A1–A7 逐项判 passed，最终 verdict=pass。
