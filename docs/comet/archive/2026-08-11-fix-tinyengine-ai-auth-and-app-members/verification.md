---
generated_from_state_version: 16
---

# Verification

## Current result

- Result: **Passed**
- Assurance: **skill-coordinated**
- Goal cycle: 1
- Iteration: 3
- Verifier attempt: 2
- Completed: 2026-08-11T12:47:14.314Z
- Summary: 第3轮第2次独立验收通过。A1、A10、A21、A39、A104修复均有当前实现和检查支撑；A3、A15、A34按用户明确接受的证据边界通过，未声称完成真实浏览器点击。A1-A107全部通过。

## Acceptance

| ID | Result | Source | Criterion | Reason |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | A1：已登录用户发起 Chat Completions 时，`Authorization` 只携带 TinyEngine 登录令牌，模型 API Key 通过请求体独立传递，后端代理调用上游时再使用模型凭据。 | 内联补全与普通聊天均将模型凭据放入请求体并移除模型 Authorization，登录 Authorization 仅由 TinyEngine HTTP 层注入。 |
| A2 | passed | brief.md | A2：模型 API Key 缺失时，前端显示明确设置提示且不发送 Chat Completions 请求。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A3 | passed | brief.md | A3：上游模型返回 401/403 或 Key 无效时，页面显示模型调用错误，保留 `engineToken`、当前页面和登录状态。 | 上游鉴权错误映射为非登录语义并包含 SSE 结束标记；真实浏览器点击未执行，但用户已明确接受代码、契约、构建与部署 API 证据边界。 |
| A4 | passed | brief.md | A4：TinyEngine 登录令牌缺失、失效或明确返回登录错误码时，前端仍清除登录状态并进入登录页。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A5 | passed | brief.md | A5：19080 样例启动时 AI Controller 和 Chat Completions 路由已启用；真实登录用户发送“你好”可收到有效模型内容或完整 SSE 结束标记，不跳登录页。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A6 | passed | brief.md | A6：仓库配置、提交、测试输出和正式验收产物中不保留或新增真实模型 API Key；样例通过运行时外部配置读取凭据。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A7 | passed | brief.md | A7：应用中心卡片和表格视图中，`app_owner`、`app_admin` 可看到“成员管理”，`app_dev` 不显示该入口。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A8 | passed | brief.md | A8：成员对话框成功加载并展示成员账号或用户标识、角色；加载失败时保留对话框并可重试，不把业务 403 当作登录失效。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A9 | passed | brief.md | A9：Owner/Admin 可为当前租户内其他非成员添加 `app_admin` 或 `app_dev`，成功后只刷新当前成员列表。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A10 | passed | brief.md | A10：同一应用同一用户重复添加、操作者操作自己、分配 `app_owner` 或越权添加时，前后端均阻止操作并显示明确错误，不产生写入。 | 前端请求前阻止重复成员和自身操作，Core 同时阻止重复、自身、Owner 与越权写入。 |
| A11 | passed | brief.md | A11：Owner/Admin 可把其他非 Owner 成员在 `app_admin` 与 `app_dev` 之间明确变更；不可修改自己或 Owner。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A12 | passed | brief.md | A12：Owner/Admin 可移除其他 `app_admin`、`app_dev`，不可移除自己或 Owner；成功后只刷新当前成员列表。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A13 | passed | brief.md | A13：`app_dev`、无应用角色用户和跨租户用户直接调用成员写接口时返回权限不足且不产生部分数据。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A14 | passed | brief.md | A14：Servlet MVC 与 WebFlux 成员 API 共享 Core 角色管理语义；前端使用现有 API 数据模型，不建立重复权限体系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A15 | passed | brief.md | A15：AI、成员入口和既有 TinyEngine 相关定向测试通过，前端生产构建和 Java 8 后端构建成功，部署后完成真实浏览器与 API 验收。 | Runtime 检查、Java 8构建、Starter安装、MySQL 5.7双次迁移和19080 API回归均通过；用户明确接受未执行真实浏览器点击的证据边界。 |
| A16 | passed | specs/tinyengine-ai-auth/spec.md | TinyEngine 应允许已登录用户通过后端代理调用配置的 OpenAI 兼容模型，同时严格分离 TinyEngine 登录凭据与模型服务凭据。模型服务鉴权或调用失败不得被解释为 TinyEngine 登录失效。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A17 | passed | specs/tinyengine-ai-auth/spec.md | 受保护的 TinyEngine AI 请求应继续使用 `Authorization: Bearer <engineToken>` 携带 TinyEngine 登录令牌。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A18 | passed | specs/tinyengine-ai-auth/spec.md | TinyEngine 认证过滤器只应把 `Authorization` 解释为 TinyEngine 登录凭据，不应把模型 API Key 当作登录令牌。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A19 | passed | specs/tinyengine-ai-auth/spec.md | 登录令牌缺失、无效或过期时，AI 路由应返回 TinyEngine 明确的未认证结果。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A20 | passed | specs/tinyengine-ai-auth/spec.md | 登录有效时，用户发送 Chat Completions 不应因模型凭据存在而覆盖或移除登录令牌。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A21 | passed | specs/tinyengine-ai-auth/spec.md | 浏览器应把当前模型的 API Key 作为 Chat Completions 请求体字段发送给 TinyEngine 后端代理。 | 普通聊天和内联补全均在 Chat Completions 请求体传递模型 apiKey。 |
| A22 | passed | specs/tinyengine-ai-auth/spec.md | 启用密钥加密时，请求体可以携带现有 `EKEY_` 凭据；后端应在调用上游前按现有密钥能力解析。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A23 | passed | specs/tinyengine-ai-auth/spec.md | 后端转发给模型服务前应从内部请求模型中读取模型凭据，并且不得把该敏感字段复制到普通日志、错误消息或响应体。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A24 | passed | specs/tinyengine-ai-auth/spec.md | 后端调用上游 OpenAI 兼容接口时，才应使用模型 API Key 构造上游 `Authorization`。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A25 | passed | specs/tinyengine-ai-auth/spec.md | 浏览器不得直接向第三方模型地址发送配置的模型 API Key。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A26 | passed | specs/tinyengine-ai-auth/spec.md | 服务端不应把用户输入的真实模型 API Key持久化到应用数据库。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A27 | passed | specs/tinyengine-ai-auth/spec.md | TinyEngine AI 总开关、Web 能力、Chat 能力及所需 LangChain4j 模型模块应在目标样例中启用。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A28 | passed | specs/tinyengine-ai-auth/spec.md | Chat Completions 路由应注册在前端配置使用的 `/tiny-engine/app-center/api/chat/completions` 路径。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A29 | passed | specs/tinyengine-ai-auth/spec.md | 加密 Key 路由启用时应与机器人插件的现有配置协议兼容；不支持加密时应保持可诊断错误并允许既定明文代理路径工作。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A30 | passed | specs/tinyengine-ai-auth/spec.md | 模型 Base URL 应规范到 OpenAI 兼容 API 根路径，Chat Completions 的最终上游地址应正确。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A31 | passed | specs/tinyengine-ai-auth/spec.md | 仓库配置不得包含真实模型 API Key；运行时 Key 应来自用户设置或受控外部配置。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A32 | passed | specs/tinyengine-ai-auth/spec.md | 模型 API Key 缺失且模型不允许空 Key 时，前端应在发送前提示用户配置 Key并停止请求。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A33 | passed | specs/tinyengine-ai-auth/spec.md | TinyEngine 返回明确登录错误码，或认证层确认登录令牌无效时，前端应清除 `engineToken` 并进入登录页。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A34 | passed | specs/tinyengine-ai-auth/spec.md | 上游模型返回 401/403、限流、网络错误或其他模型调用错误时，前端应显示模型调用错误并保留 `engineToken`、当前页面和当前会话。 | 模型错误不会触发登录清理；真实浏览器点击未执行，但用户已明确接受代码、契约、构建与19080 API证据边界。 |
| A35 | passed | specs/tinyengine-ai-auth/spec.md | 全局 HTTP 拦截器不得仅凭代理路由的 HTTP 401/403 就认定 TinyEngine 登录失效；应依据 TinyEngine 认证错误语义区分登录错误和上游错误。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A36 | passed | specs/tinyengine-ai-auth/spec.md | 模型错误不得触发取消所有无关 TinyEngine 请求或永久设置全局未登录状态。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A37 | passed | specs/tinyengine-ai-auth/spec.md | 非流式 Chat Completions 应返回机器人插件可消费的模型内容。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A38 | passed | specs/tinyengine-ai-auth/spec.md | 流式 Chat Completions 应返回插件可消费的 SSE 数据，并包含正常完成标记。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A39 | passed | specs/tinyengine-ai-auth/spec.md | 流式和非流式调用应遵守相同的凭据分离和错误边界。 | 流式和非流式共享凭据分离与模型错误边界，相关测试通过。 |
| A40 | passed | specs/tinyengine-ai-auth/spec.md | API Key 不得出现在仓库文件、提交消息、测试快照、普通应用日志、错误通知或验收报告中。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A41 | passed | specs/tinyengine-ai-auth/spec.md | 诊断日志可以记录模型标识、目标主机、状态分类和追踪标识，但必须脱敏敏感请求字段与认证头。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A42 | passed | specs/tinyengine-ai-auth/spec.md | 测试应使用占位凭据或动态注入凭据，不使用真实 Key。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A43 | passed | specs/tinyengine-ai-auth/spec.md | 现有非 AI TinyEngine 请求的登录行为保持不变。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A44 | passed | specs/tinyengine-ai-auth/spec.md | Servlet MVC 的 TinyEngine 登录认证语义保持不变；AI Controller 的启用不应绕过该认证边界。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A45 | passed | specs/tinyengine-ai-auth/spec.md | 实现应兼容 Java 8。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A46 | passed | specs/tinyengine-app-access/spec.md | TinyEngine 应根据当前真实用户在每个应用上的有效角色关系返回应用列表并限制应用操作。应用中心应为有权角色提供成员管理入口；框架内置的 `system` 技术上下文继续支持未启用认证时的租户级默认行为。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A47 | passed | specs/tinyengine-app-access/spec.md | 系统应以 `t_permission_role.code` 识别应用角色，稳定代码分别为 `app_owner`、`app_admin`、`app_dev`。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A48 | passed | specs/tinyengine-app-access/spec.md | 三种角色的 `role_type` 均应为 `app`。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A49 | passed | specs/tinyengine-app-access/spec.md | 每个应用最多存在一个 `app_owner` 关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A50 | passed | specs/tinyengine-app-access/spec.md | 用户与应用角色关系应存储在 `r_auth_users_units_roles`，其中 `unit_type` 为 `app`，`unit_id` 为应用 ID，`user_id` 为用户 ID，`role_id` 指向对应角色。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A51 | passed | specs/tinyengine-app-access/spec.md | 仅 `expired_time` 为空或晚于当前数据库时间的关系有效。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A52 | passed | specs/tinyengine-app-access/spec.md | 对真实用户，应用分页列表只应返回当前租户内、该用户持有有效 `app_owner`、`app_admin` 或 `app_dev` 关系的应用。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A53 | passed | specs/tinyengine-app-access/spec.md | 按设计器 ID 查询应用列表应遵循相同规则，并继续应用设计器 ID 条件。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A54 | passed | specs/tinyengine-app-access/spec.md | 用户没有应用关系、关系角色不是三种应用角色或关系已过期时，不应返回对应应用。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A55 | passed | specs/tinyengine-app-access/spec.md | 超级管理员、组织管理员、设计器管理员和其他非应用角色不应自动绕过应用列表过滤。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A56 | passed | specs/tinyengine-app-access/spec.md | 分页总数应使用与列表相同的租户、用户角色和请求查询条件。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A57 | passed | specs/tinyengine-app-access/spec.md | 当前真实用户 ID 缺失时，可见应用集合应为空。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A58 | passed | specs/tinyengine-app-access/spec.md | 当前上下文是框架内置 `system` 技术身份时，应保留当前租户内的既有列表行为，不要求数据库角色关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A59 | passed | specs/tinyengine-app-access/spec.md | `app_owner` 应能读取、更新应用管理配置、开发编辑与保存、预览、发布和删除对应应用。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A60 | passed | specs/tinyengine-app-access/spec.md | `app_admin` 应能读取、更新应用管理配置、开发编辑与保存、预览和发布对应应用，但不能删除应用。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A61 | passed | specs/tinyengine-app-access/spec.md | `app_dev` 应能读取、编辑与保存既有开发资源、预览对应应用，但不能新增模型、更新应用管理配置、发布或删除应用。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A62 | passed | specs/tinyengine-app-access/spec.md | 应用管理配置应包括应用元数据和应用扩展配置。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A63 | passed | specs/tinyengine-app-access/spec.md | 开发编辑与保存应包括页面、区块、既有模型、数据源、资源、国际化、画布保存及现有同类应用内开发能力。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A64 | passed | specs/tinyengine-app-access/spec.md | 新增模型只应允许 `app_owner`、`app_admin`；`app_dev` 新增模型时应返回权限不足且不写入数据。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A65 | passed | specs/tinyengine-app-access/spec.md | 页面与区块锁定/解锁按独立的现有互斥规则处理，三种应用角色均可使用。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A66 | passed | specs/tinyengine-app-access/spec.md | 用户执行无权操作时，系统应返回统一的权限不足结果，且不得产生部分数据变更。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A67 | passed | specs/tinyengine-app-access/spec.md | 角色授权应在 Core 服务边界执行，并由 Servlet MVC、WebFlux 和其他服务调用路径共享，不能仅依赖 Controller 检查。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A68 | passed | specs/tinyengine-app-access/spec.md | 授权应同时校验当前租户、应用 ID、用户 ID、角色代码和角色有效期。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A69 | passed | specs/tinyengine-app-access/spec.md | 系统应同时提供 Servlet MVC 与 WebFlux 后端 API，用于查询应用成员角色、分配或变更角色以及移除角色。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A70 | passed | specs/tinyengine-app-access/spec.md | 对应应用的 `app_owner` 和 `app_admin` 可以调用应用成员角色管理 API。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A71 | passed | specs/tinyengine-app-access/spec.md | 成员查询应返回应用成员的用户标识、账号信息、角色代码和角色有效期等现有关系信息。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A72 | passed | specs/tinyengine-app-access/spec.md | `app_owner` 或 `app_admin` 只能为当前租户内的其他用户分配或变更 `app_admin`、`app_dev`，不得对自己的角色关系执行新增、变更或移除。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A73 | passed | specs/tinyengine-app-access/spec.md | 同一用户在同一应用上最多存在一个 `unit_type = 'app'` 的关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A74 | passed | specs/tinyengine-app-access/spec.md | 首次分配与角色变更应为不同操作；用户已有该应用角色关系时，重复新增应被拒绝且不得覆盖原角色，角色调整必须使用明确的变更操作。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A75 | passed | specs/tinyengine-app-access/spec.md | 成员角色关系被移除后，可以重新为该用户分配当前应用的 `app_admin` 或 `app_dev`。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A76 | passed | specs/tinyengine-app-access/spec.md | 移除 `app_admin` 或 `app_dev` 后，新的列表可见性和应用操作权限应立即生效。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A77 | passed | specs/tinyengine-app-access/spec.md | 成员管理 API 不应允许任何调用者分配、变更或删除 `app_owner`，也不提供 Owner 转让或多 Owner 能力。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A78 | passed | specs/tinyengine-app-access/spec.md | Owner 唯一性应在持久化边界强制执行并覆盖并发写入，任何产生第二个 `app_owner` 的操作都应失败且保留原 Owner。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A79 | passed | specs/tinyengine-app-access/spec.md | `app_dev`、无应用角色用户和跨租户用户调用成员管理 API 时应返回权限不足，且不得产生部分数据变更。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A80 | passed | specs/tinyengine-app-access/spec.md | 成员角色管理的授权与写入应在 Core 服务边界完成，MVC 与 WebFlux 只负责协议适配。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A81 | passed | specs/tinyengine-app-access/spec.md | 应用中心卡片视图和表格视图的每个应用操作区都应提供可发现的“成员管理”入口。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A82 | passed | specs/tinyengine-app-access/spec.md | 入口只对该应用的 `app_owner`、`app_admin` 显示；`app_dev` 和无应用角色用户不应看到入口。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A83 | passed | specs/tinyengine-app-access/spec.md | 前端入口可见性所需的当前用户应用角色应来自后端可信数据，不应根据创建人名称等展示字段推断。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A84 | passed | specs/tinyengine-app-access/spec.md | 成员管理应使用独立对话框，展示成员账号或用户标识及应用角色，不跳转到新的应用中心页面。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A85 | passed | specs/tinyengine-app-access/spec.md | 对话框应支持添加成员、把其他非 Owner 成员在 `app_admin` 与 `app_dev` 之间变更、移除其他非 Owner 成员。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A86 | passed | specs/tinyengine-app-access/spec.md | Owner 行、当前操作者行及不允许的操作应隐藏或禁用；前端保护不得替代后端权限校验。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A87 | passed | specs/tinyengine-app-access/spec.md | 添加、修改或移除成功后只应刷新当前应用的成员列表，不刷新整个应用中心页面。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A88 | passed | specs/tinyengine-app-access/spec.md | 成员列表加载失败时应保留对话框和入口，并提供重试能力。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A89 | passed | specs/tinyengine-app-access/spec.md | 重复成员、操作自己、Owner 保护、越权和跨租户错误应显示明确业务消息；业务 403 不应自动清除 TinyEngine 登录状态。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A90 | passed | specs/tinyengine-app-access/spec.md | 真实用户成功创建应用时，系统应为其创建该应用的 `app_owner` 关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A91 | passed | specs/tinyengine-app-access/spec.md | 真实用户创建应用时应产生且仅产生一个 `app_owner`；已有 Owner 时不得再写入第二条 Owner 关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A92 | passed | specs/tinyengine-app-access/spec.md | 应用、默认首页和所有者关系应在同一事务中完成；任何一步失败时全部回滚。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A93 | passed | specs/tinyengine-app-access/spec.md | 技术性的 `system` 身份创建应用时，不要求写入真实用户角色关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A94 | passed | specs/tinyengine-app-access/spec.md | 删除应用成功时，系统应删除该应用在 `r_auth_users_units_roles` 中的全部应用级用户角色关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A95 | passed | specs/tinyengine-app-access/spec.md | 关系清理应同时匹配当前租户、应用 ID 和 `unit_type = 'app'`，不得删除同一用户或同一 ID 对应的其他业务单元关系。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A96 | passed | specs/tinyengine-app-access/spec.md | 应用页面、应用本身和应用角色关系的删除应位于同一事务；任何一步失败时全部回滚。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A97 | passed | specs/tinyengine-app-access/spec.md | 只有 `app_owner` 可以删除应用；本节同时约束删除后的关联数据一致性。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A98 | passed | specs/tinyengine-app-access/spec.md | `app_owner`、`app_admin`、`app_dev` 均可对其有权访问的应用发起页面或区块锁定与解锁。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A99 | passed | specs/tinyengine-app-access/spec.md | 锁定与解锁应保持现有占用者互斥规则：用户不能抢占其他用户持有的锁，且只有当前占用者可以解锁。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A100 | passed | specs/tinyengine-app-access/spec.md | 三种应用角色之间不应对锁定与解锁增加额外权限差异。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A101 | passed | specs/tinyengine-app-access/spec.md | MySQL 与 H2 初始化数据应提供三种唯一应用角色代码。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A102 | passed | specs/tinyengine-app-access/spec.md | 升级脚本应将既有应用管理员、应用开发者角色规范为 `app_admin`、`app_dev`，并增加 `app_owner`。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A103 | passed | specs/tinyengine-app-access/spec.md | 升级脚本应为历史应用创建者回填 `app_owner`，但仅限该创建者尚无同一应用的任何应用级角色关系时。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A104 | passed | specs/tinyengine-app-access/spec.md | 初始化和升级操作应可重复执行，不产生重复角色或重复用户-应用关系。 | H2与真实MySQL 5.7迁移均连续执行两次成功，未产生重复Owner。 |
| A105 | passed | specs/tinyengine-app-access/spec.md | Servlet MVC 与 WebFlux API 应共享同一 Core 查询和成员管理语义。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A106 | passed | specs/tinyengine-app-access/spec.md | 现有查询参数、响应结构、排序和分页上限保持兼容；新增可信角色字段应保持向后兼容。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |
| A107 | passed | specs/tinyengine-app-access/spec.md | 实现应兼容 Java 8。 | 独立 Verifier 已根据目标规格、当前实现、Runtime 检查及部署证据确认通过。 |

## Checks

| Check | Command | Working directory | Status | Exit | Duration |
| --- | --- | --- | --- | ---: | ---: |
| TinyEngine AI and member frontend contracts | -lc node --test fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-ai-frontend-contract.test.mjs fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-app-members-frontend-contract.test.mjs fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/test/js/tiny-engine-sql-resources.test.mjs | . | passed | 0 | 318 ms |
| Starter owner guard migration and manifest tests | -lc JAVA_HOME=/Library/Java/JavaVirtualMachines/zulu-8.jdk/Contents/Home mvn -f fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/pom.xml -Dtest=TinyEngineSqlScriptManifestTest,TinyEngineAppRoleMigrationTest,TinyEngineH2ManifestTest test | . | passed | 0 | 8469 ms |
| TinyEngine full frontend production build | -lc pnpm run build | fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console | passed | 0 | 721227 ms |
| Diff whitespace and model credential scan | -lc git diff --check && ! git grep -n -E 'sk-[A-Za-z0-9]{20,}' -- . ':!docs/comet/changes/fix-tinyengine-ai-auth-and-app-members/comet-state.yaml' ':!docs/comet/changes/fix-tinyengine-ai-auth-and-app-members/verification.md' | . | passed | 0 | 1425 ms |

## Blockers

_None._

## Risks and skipped work

- 未执行真实浏览器点击复验；本次通过依据用户明确接受的前端契约、完整生产构建、代码审查和19080部署API证据边界，不代表浏览器点击已经完成。
- 此前公开过的模型凭据不在仓库或验收产物中，但仍需由用户在外部服务侧轮换。

## Previous iterations

| Goal cycle | Iteration | Attempt | Outcome | Unresolved | Summary | Completed |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | fail | A3, A5, A10, A13, A14, A15, A18, A19, A34, A39, A46, A49, A76, A78, A79, A80, A89, A105 | 第 1 轮未通过。AI 凭据分离、非流式错误映射、可信 currentRole、成员 UI、Core 角色矩阵、生命周期清理和迁移主体已实现；但 WebFlux 认证用户作用域、流式上游错误边界、Owner 持久化唯一性、成员并发授权仍需修复，真实浏览器/有效模型验收仍受阻。 | 2026-08-11T09:37:28.522Z |
| 1 | 2 | 1 | fail | A1, A3, A10, A15, A21, A34, A39, A104 | 第2轮后端修复有效：WebFlux 用户上下文、成员锁后重授权、Owner 并发唯一性、流式鉴权错误脱敏及 Servlet SSE 500 均有实现和通过的检查，19080 API 结果也符合预期。但仍发现三个实质问题：内联 Chat Completions 未在请求体携带模型 Key，前端未预阻止重复成员新增，owner_guard 升级脚本不可重复执行；另有真实浏览器验收因当前无浏览器实例而无法完成。因此本轮结论为 fail。 | 2026-08-11T11:12:31.531Z |
| 1 | 3 | 1 | blocked | A3, A15, A34 | 第3轮三个实质修复均通过代码、契约、构建、MySQL 5.7迁移和19080 API证据复核，A1、A10、A21、A39、A104 已解决。除真实浏览器可见行为外其余104项通过；A3、A15、A34因无可用浏览器实例暂时无法完成最终UI验收，因此结论为blocked。 | 2026-08-11T12:05:42.341Z |
| 1 | 3 | 2 | pass | — | 第3轮第2次独立验收通过。A1、A10、A21、A39、A104修复均有当前实现和检查支撑；A3、A15、A34按用户明确接受的证据边界通过，未声称完成真实浏览器点击。A1-A107全部通过。 | 2026-08-11T12:47:14.314Z |

## Conclusion

第3轮第2次独立验收通过。A1、A10、A21、A39、A104修复均有当前实现和检查支撑；A3、A15、A34按用户明确接受的证据边界通过，未声称完成真实浏览器点击。A1-A107全部通过。
