---
generated_from_state_version: 8
---

# 验证

## 当前结果

- 结果: **已归档**
- 验证情况: **已完成检查，验证结果已确认**
- 目标周期: 1
- 迭代: 1
- 验证器尝试次数: 1
- 完成时间: 2026-10-06T10:17:46.291Z
- 摘要: 针对 ws-knowledge-auth 的只读验证：通过审阅工作树实现并结合两份 Runtime check receipt（ws-autoconfigure-tests: Tests run 132/Failures 0/Errors 0/Skipped 1；ai-knowledge-tests: Tests run 52/Failures 0/Errors 0/Skipped 0，均 BUILD SUCCESS），判定 A1–A5 全部 passed，verdict=pass。核心机制为统一 InstanceAuthRegistry 驱动每实例 provider 解析、双栈一致的凭证合并与拒绝、以及 kb 开放 API 的 provider 化校验；sa-token 正向端到端因非目标不可验证，相关结论基于代码证据与已通过测试。

## 验收

| 编号 | 结果 | 来源 | 验收项 | 原因 |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | WS 默认实例与命名实例可逐实例选择 provider（permit-all + 各 sa-token 方式），实例间不继承、不串用；未配置 provider 的实例握手一律拒绝。 | 每个 WebSocket 实例的 auth 独立：WsConfig 顶层 auth 与 WsConfig.WsInstance.auth 并存，WsAuthSupport.resolveAuth/resolveInstanceKey 按实例解析，InstanceAuthRegistry.resolve 按实例键查找；fw-plus-boot/.../web/ws/auth/WsAuthSupportTest 中『命名实例不从顶层auth继承』『命名实例的provider只作用于该实例』『未配置provider的实例握手一律拒绝』『permit_all显式选择时放行』均通过（该测试类 10 passed）。 |
| A2 | passed | brief.md | WS 握手凭据可从 query 参数或请求头读取（名称同各 provider 凭据名），两栈行为一致；凭据缺失/无效/未配置 provider 时握手拒绝（webmvc 以 HTTP 401 拒绝且不升级；webflux 拒绝并关闭会话），不进入会话注册与订阅派发。 | 凭证来源与拒绝语义一致：WsAuthSupport.mergeCredentials（约 113-137 行）合并 query 与 header 且 header 覆盖 query；webmvc 侧 WsAuthHandshakeInterceptor.beforeHandshake 设置 HTTP 401 并返回 false；webflux 侧 ReactiveWsHandler.authenticate 在 registry.register 之前执行，拒绝时以 POLICY_VIOLATION(1008) 关闭；两栈复用同一 WsAuthSupport。WsAuthHandshakeInterceptorTest(3)/ReactiveWsHandlerTest(14) 通过。 |
| A3 | passed | brief.md | WS provider 按 classpath 可选装配，模块缺失时不可选、已配置 fail-closed；未知 provider 抛 `IllegalArgumentException`；只认证不授权；permit-all 必须显式选择，且不作为默认。 | fail-closed 且无越权：InstanceAuthRegistry.availableProviders 按 classpath 暴露已注册 provider；provider 模块缺失时回退 RejectingAuthService→ANONYMOUS（测试『provider模块缺失时fail_closed』）；未知 provider 由 WsAuthSupport.validateProviders/resolve 抛 IllegalArgumentException（测试覆盖）；permit-all 仅在显式配置时放行，默认 provider 为 null 时拒绝；WS 鉴权路径仅检查 isAuthenticated，不消费 scope。 |
| A4 | passed | brief.md | knowledge 对外 API 按设置页 provider 校验，默认 `sa-token-login`（保持现状）；可切换为清单内任一 classpath 可用项；`/settings`（GET/POST）同样受认证保护。 | kb 开放 API 校验走 provider 注册表：KbSettingService.DEFAULT_OPEN_API_AUTH_PROVIDER="sa-token-login" 且 normalize 提供默认值；KbSaTokenAuthChecker.check 依据设置解析 provider 并通过注册表校验；KbOpenApiController.collectHeaders 经 RequestContextHolder 读取全部请求头；getSettings/saveSettings 由 auth(null,null) 保护。KbSaTokenAuthCheckerTest(9)/KbSettingServiceAuthProviderTest(3)/KbSettingDefaultsTest 通过。 |
| A5 | passed | brief.md | knowledge provider 模块缺失或未知时 fail-closed（返回 `error_code 1002`，不回退）；控制台下拉只展示 classpath 可用 provider，保存即热生效。 | 未知/缺失 provider 统一失败：抛 KbUnauthorizedException 并映射为 error(1002)；KbConsoleController.listAuthProviders 返回 authRegistry.availableProviders()；控制台 magic-kb-browser.vue 通过 kbApi.authProviders('/kb/openapi-auth-providers') 加载；KbSettingService.save 热替换 current（测试『保存后热生效』）；DDL open_api_auth_provider 已加入 docs/sql/fw-plus-ai-knowledge.sql 与 schema-h2.sql。 |

## 检查

| 检查 | 命令 | 工作目录 | 状态 | 退出码 | 耗时 |
| --- | --- | --- | --- | ---: | ---: |
| fw-plus-boot-autoconfigure web.ws 全包测试 | -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest=com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.** -DfailIfNoTests=false | fw-plus-boot | passed | 0 | 21479 ms |
| fw-plus-ai-boot-autoconfigure knowledge 全包测试 | -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest=com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.** -DfailIfNoTests=false | fw-plus-ai | passed | 0 | 16313 ms |

### Builder 报告的证据

以下为 Builder 报告，不等同于 Runtime 检查凭据或独立验收结果。

- cd fw-plus-boot && ./mvnw -o -pl fw-plus-boot-project/fw-plus-boot-autoconfigure test -Dtest='com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.**' -DfailIfNoTests=false: passed — 132 tests，0 失败
- cd fw-plus-ai && ./mvnw -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure test -Dtest='com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.knowledge.**' -DfailIfNoTests=false: passed — 52 tests，0 失败
- cd fw-plus-boot && ./mvnw -o -q -pl fw-plus-boot-project/fw-plus-boot-autoconfigure compile -DskipTests: passed — 离线编译通过
- cd fw-plus-ai && ./mvnw -o -q -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure compile -DskipTests: passed — 离线编译通过
- cd .../fw-plus-ai-magicapi-plugin-langchain4j-content-retriever/src/console && ./node_modules/.bin/vite build --config vite.kb.config.js: passed — 控制台 kb 产物重建成功（dist 为 gitignore 构建产物）
- 已知限制: 未实现登录/令牌签发/凭据管理，故 sa-token 各 provider 的端到端正向认证需宿主已登录态，测试以 permit-all 放行、fail-closed 拒绝、凭据归一化桩件覆盖。
- 已知限制: KbConsoleController /openapi-auth-providers 为对 InstanceAuthRegistry.availableProviders() 的一行委托，未单独构造 MagicController 做控制器测试；覆盖由注册表 availableProviders 断言与前端调用路径保证。
- 已知限制: 控制台 dist/ 产物为 gitignore 构建物，仅源码（vue/js/i18n）纳入提交。

## 阻塞项

_无。_

## 风险与跳过的工作

- 正向 sa-token 端到端未被测试覆盖（设计上不发放登录令牌）；每实例 sa-token 选择仅有代码证据与 permit-all 放行/fail-closed 拒绝/归一化的 stub 测试佐证。
- /settings 与 /openapi-auth-providers 缺少控制器级鉴权测试（builder 已知限制）。
- WS receipt 选择器 web.ws.** 未包含被修改的 MagicAPIWsWiringTest，该测试未被 receipt 重跑。
- fw-plus-boot-autoconfigure/pom.xml 将 fw-plus-auth-core 标为 <optional>true</optional>，而 WsConfig/WsAuthSupport 在类级别引用它，缺失 auth-core 的消费方可能无法加载 WS bean。
- 控制台 vite 构建未独立 receipt（仅源码审查）。

## 之前的迭代

| 目标周期 | 迭代 | 尝试 | 结果 | 未解决项 | 摘要 | 完成时间 |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | pass | — | 针对 ws-knowledge-auth 的只读验证：通过审阅工作树实现并结合两份 Runtime check receipt（ws-autoconfigure-tests: Tests run 132/Failures 0/Errors 0/Skipped 1；ai-knowledge-tests: Tests run 52/Failures 0/Errors 0/Skipped 0，均 BUILD SUCCESS），判定 A1–A5 全部 passed，verdict=pass。核心机制为统一 InstanceAuthRegistry 驱动每实例 provider 解析、双栈一致的凭证合并与拒绝、以及 kb 开放 API 的 provider 化校验；sa-token 正向端到端因非目标不可验证，相关结论基于代码证据与已通过测试。 | 2026-10-06T10:17:46.291Z |



## 结论

针对 ws-knowledge-auth 的只读验证：通过审阅工作树实现并结合两份 Runtime check receipt（ws-autoconfigure-tests: Tests run 132/Failures 0/Errors 0/Skipped 1；ai-knowledge-tests: Tests run 52/Failures 0/Errors 0/Skipped 0，均 BUILD SUCCESS），判定 A1–A5 全部 passed，verdict=pass。核心机制为统一 InstanceAuthRegistry 驱动每实例 provider 解析、双栈一致的凭证合并与拒绝、以及 kb 开放 API 的 provider 化校验；sa-token 正向端到端因非目标不可验证，相关结论基于代码证据与已通过测试。
