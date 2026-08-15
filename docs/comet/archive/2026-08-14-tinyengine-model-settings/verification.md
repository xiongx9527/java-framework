---
generated_from_state_version: 17
---

# Verification

## Current result

- Result: **Passed**
- Assurance: **skill-coordinated**
- Goal cycle: 1
- Iteration: 4
- Verifier attempt: 1
- Completed: 2026-08-14T15:30:27.336Z
- Summary: 通过。上一轮未解决的A16/A18/A54已由MySQL 5.7兼容迁移、真实密文跨会话持久化及应用清理检查收敛；A67正式WebFlux聊天路由已使用TinyEngineReactiveExecutor；A74已覆盖401/403、限流、连接、超时及其他非HTTP错误的统一脱敏模型错误边界。其余验收项未见回归，Runtime相关JDK8回归均通过。

## Acceptance

| ID | Result | Source | Criterion | Reason |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | A1：同一应用的 Owner 创建模型服务和模型后，Admin 可管理，Dev 可读取并调用；无角色、跨租户用户不可读取或调用。 | Core 在服务边界按当前租户和应用角色校验 READ/MANAGE 权限；Owner/Admin 可管理，Dev 可读取和解析已启用模型，无角色或跨租户访问被拒绝。 |
| A2 | passed | brief.md | A2：Owner/Admin 可新增、修改、删除服务和模型；Dev 的管理请求返回权限不足且不写入数据。 | 所有服务和模型写操作均要求 AppPermission.MANAGE，Dev 权限测试确认请求被拒绝且不会执行持久化写入。 |
| A3 | passed | brief.md | A3：管理响应只返回 `hasApiKey` 和固定掩码，不包含明文、密文或可逆令牌；可用列表也不包含 API Key。 | AiModelServiceDto 仅暴露 hasApiKey 和固定掩码 ********，不定义 apiKey 字段；可用视图同样使用该安全 DTO。 |
| A4 | passed | brief.md | A4：模型服务实体的 `apiKey` 使用公共 `@FieldEncrypt`；数据库原始 JDBC 查询为密文，Mapper 业务读取为明文，业务代码不手动加解密。 | AiModelServiceConfig.apiKey 标注公共 @FieldEncrypt，Mapper 不含手工加解密；加密持久化测试和 MySQL 跨会话检查确认 JDBC 原值为密文、Mapper 读取为明文。 |
| A5 | passed | brief.md | A5：API Key 留空表示保留，非空表示替换，`clearApiKey=true` 表示清除；替换和清除同时出现时拒绝请求。 | updateService 明确实现空白保留、非空替换和 clearApiKey 清除三态，并拒绝替换与清除同时出现及不允许空密钥时的清除。 |
| A6 | passed | brief.md | A6：所有模型配置 SQL 同时约束 `tenant_id` 和 `app_id`，服务 ID 或模型 ID 不能绕过应用隔离。 | 两个模型 Mapper 的查询、更新和删除 SQL 均同时约束 tenant_id 与 app_id，服务和模型 ID 不能单独绕过应用隔离。 |
| A7 | passed | brief.md | A7：MVC 与 WebFlux 暴露同构的可用列表、管理列表、服务/模型增删改和本地导入 API，均委托同一 Facade/Core 服务；WebFlux 使用既有阻塞调度器。 | MVC/WebFlux 提供同构配置接口并委托 TinyEngineApiFacade/Core；WebFlux 管理接口及正式聊天接口均使用 TinyEngineReactiveExecutor。 |
| A8 | passed | brief.md | A8：聊天请求只包含 `appId`、`serviceId`、`modelName` 和生成参数，不包含 `baseUrl`、`apiKey`；旧客户端提交的凭据不得成为正常调用来源。 | 正式聊天请求要求 appId、serviceId、modelName；TinyEngineChatRequest 忽略客户端 baseUrl/apiKey/modelSettings，Robot payload 不再发送凭据。 |
| A9 | passed | brief.md | A9：服务端调用 `resolveInvocation` 后，真实 LangChain4j provider 使用解析出的 endpoint、API Key 和模型名请求上游；同步和流式路径均覆盖。 | 默认 Chat Service 的同步和流式路径均先调用 Core resolveInvocation，再把解析出的 endpoint、API Key、模型名和设置交给 LangChain4j provider。 |
| A10 | passed | brief.md | A10：上游 401/403 被转换为脱敏模型调用错误，不清除 TinyEngine `engineToken`，不跳转登录；真正的 TinyEngine 登录失效仍按既有流程处理。 | LangChain4j 将上游 401/403 转为脱敏 TinyEngineUpstreamException，正式双栈 Controller 再统一映射为 502 模型错误，不触发 TinyEngine 登录清理。 |
| A11 | passed | brief.md | A11：Owner/Admin 的设置界面显示管理功能；Dev 只显示可用模型选择，不显示密钥输入、创建、编辑或删除动作。 | Robot 根据 app_owner/app_admin 设置 canManage 并显示管理功能；app_dev 仅加载可用列表，不提供创建、编辑、删除或密钥操作。 |
| A12 | passed | brief.md | A12：前端状态、localStorage、聊天请求和普通日志中不保留服务端 API Key；编辑时只显示是否已配置及固定掩码，并支持留空保留和显式清除。 | 远端状态和表单仅保留 hasApiKey/apiKeyMask；正常聊天和持久化状态不含 API Key，编辑时空输入保留且显式操作才清除。 |
| A13 | passed | brief.md | A13：旧 localStorage 在数据库无配置时只导入一次；数据库已有配置时不覆盖；导入成功才删除模型相关字段，失败时完整保留；聊天设置及未知键不丢失。 | 共享配置初始化仅在远端无服务且无行为设置时导入；成功后删除 services/defaultModel/quickModel，保留聊天设置和未知键，失败时不改写 localStorage。 |
| A14 | passed | brief.md | A14：并发旧配置导入最多成功一次，不产生重复服务/模型，也不会用旧快照覆盖已存在的数据库配置。 | importLocal 在统一事务中先锁定租户应用，再使用 FOR UPDATE 当前读检查服务和模型表；已有配置立即拒绝，唯一约束阻止重复数据。 |
| A15 | passed | brief.md | A15：删除应用时模型、服务、角色关系和应用删除位于一致事务边界；失败时回滚，成功后两张模型配置表无该应用数据。 | 应用删除位于 rollbackFor=Exception 的统一事务中，依次清理模型、服务、角色和应用；清理异常会传播并触发整体回滚。 |
| A16 | passed | brief.md | A16：MySQL/H2 迁移可重复执行；服务键、模型名等应用内唯一性成立，主键类型与 BIGINT 一致。 | MySQL 5.7 不支持的 DROP COLUMN IF EXISTS 已改为 information_schema+PREPARE；模型迁移可重复执行，H2/MySQL 唯一约束及 BIGINT 主键、service_id 已由测试和 MySQL 5.7.44 检查确认。 |
| A17 | passed | brief.md | A17：Java 8 下 Core、Boot、AI 定向与相关回归通过；Robot 定向 Vitest、可收集全量测试和生产 build 通过。 | Runtime 记录 JDK8 Core 47项、AI 45项、LangChain4j 8项、Boot 鉴权/迁移4项均通过；本轮未改前端，上一候选的109项 Vitest及Vite生产构建结果仍适用。 |
| A18 | passed | brief.md | A18：本机 MySQL 验证配置能跨浏览器/用户按应用共享、原始 API Key 列为密文，应用删除后关联数据清理。 | Runtime 在 MySQL 5.7.44 上验证了跨会话共享读取、原始 API Key 密文、Mapper 解密、唯一约束以及应用删除后两张配置表无残留。 |
| A19 | passed | specs/tinyengine-ai-auth/spec.md | TinyEngine 应允许已登录且拥有应用权限的用户通过后端代理调用应用配置的 OpenAI 兼容模型，同时严格分离 TinyEngine 登录凭据与模型服务凭据。模型服务鉴权或调用失败不得被解释为 TinyEngine 登录失效。 | 正式 AI 路由已严格分离浏览器携带的 TinyEngine 登录令牌与数据库解析出的模型服务凭据。 |
| A20 | passed | specs/tinyengine-ai-auth/spec.md | 受保护的 TinyEngine AI 请求应继续使用 `Authorization: Bearer <engineToken>` 携带 TinyEngine 登录令牌。 | 公共 HTTP 请求处理继续从 localStorage 读取 engineToken 并设置 Authorization: Bearer <engineToken>。 |
| A21 | passed | specs/tinyengine-ai-auth/spec.md | TinyEngine 认证过滤器只应把 `Authorization` 解释为 TinyEngine 登录凭据。 | Servlet 和 Reactive 认证过滤器只把入站 Authorization 作为 TinyEngine 登录凭据处理，模型凭据不进入该过滤器。 |
| A22 | passed | specs/tinyengine-ai-auth/spec.md | 登录令牌缺失、无效或过期时，AI 路由应返回明确的 TinyEngine 未认证结果。 | 双栈认证测试确认受保护 AI 路由缺少或无效登录令牌时返回 TinyEngine 未认证结果，登录白名单不受影响。 |
| A23 | passed | specs/tinyengine-ai-auth/spec.md | 模型调用不得覆盖、移除或复用登录令牌作为上游模型凭据。 | 模型 API Key 仅在服务端 provider 客户端构造时使用；浏览器登录 Authorization 不会被覆盖、移除或复用。 |
| A24 | passed | specs/tinyengine-ai-auth/spec.md | 浏览器正常聊天请求不得携带模型 API Key、密文、可逆令牌或 Base URL。 | Robot 正常聊天 payload 只包含应用/服务/模型标识、消息和生成参数，不包含 API Key、密文、可逆令牌或 Base URL。 |
| A25 | passed | specs/tinyengine-ai-auth/spec.md | 浏览器只提交应用 ID、模型服务 ID、模型名、消息和生成参数。 | Agent、Chat 和修复调用均注入 appId、serviceId、modelName，并保留消息及生成参数。 |
| A26 | passed | specs/tinyengine-ai-auth/spec.md | 后端应从应用共享模型配置中解析并透明解密 API Key，且只在调用上游时构造上游 `Authorization`。 | Core resolveInvocation 从应用配置 Mapper 读取经 @FieldEncrypt 透明解密的 API Key，并仅通过内部请求交给上游模型客户端。 |
| A27 | passed | specs/tinyengine-ai-auth/spec.md | 内部模型凭据不得复制到普通日志、错误消息、HTTP 响应或前端状态。 | 内部调用配置和敏感请求字段均禁止 HTTP 序列化，Controller/provider 错误消息固定脱敏，未发现普通日志输出认证信息。 |
| A28 | passed | specs/tinyengine-ai-auth/spec.md | 旧浏览器凭据只允许通过受控的一次性迁移 API 导入数据库，不得作为正常聊天链路的备用来源。 | 旧浏览器 API Key 仅由 import-local 请求的 apiKeys 映射使用，正常聊天链路明确忽略客户端 apiKey/baseUrl。 |
| A29 | passed | specs/tinyengine-ai-auth/spec.md | Chat Completions 路由应注册在前端使用的 `/tiny-engine/app-center/api/chat/completions` 路径。 | 正式 Chat Completions 映射使用 TinyEngine base-path 加 /app-center/api/chat/completions，测试确认实际路径为 /tiny-engine/app-center/api/chat/completions。 |
| A30 | passed | specs/tinyengine-ai-auth/spec.md | 模型 Base URL 应规范到 OpenAI 兼容 API 根路径，最终 Chat Completions 地址应正确。 | OpenAiCompatibleBaseUrl 对主机根、/v1、尾斜杠和 /chat/completions 做统一规范化，并拒绝非 HTTP、查询串、片段和用户信息。 |
| A31 | passed | specs/tinyengine-ai-auth/spec.md | 后端应校验当前用户对请求应用具有有效使用权限，并校验服务和模型已启用。 | resolveInvocation 在当前租户和用户 READ 权限下读取目标应用，并校验应用、服务和模型均启用。 |
| A32 | passed | specs/tinyengine-ai-auth/spec.md | 仓库配置不得包含真实模型 API Key；运行时凭据来自数据库加密配置。 | 仓库和配置仅包含占位凭据；运行时模型凭据来自数据库加密字段，Runtime 高风险秘密扫描通过。 |
| A33 | passed | specs/tinyengine-ai-auth/spec.md | 未配置可用服务、模型或所需 API Key 时，前端应显示单次明确提示并停止请求。 | Robot 对缺失或无效服务、模型选择不会静默回退，生成空的调用标识并在请求前给出单次配置错误提示。 |
| A34 | passed | specs/tinyengine-ai-auth/spec.md | TinyEngine 返回明确登录错误码，或认证层确认登录令牌无效时，前端应清除 `engineToken` 并进入登录页。 | 公共 HTTP 层对真实 TinyEngine 401或明确登录错误码调用 toLogin，清除 engineToken 并进入登录状态。 |
| A35 | passed | specs/tinyengine-ai-auth/spec.md | 上游模型返回 401/403、限流、网络错误或其他模型调用错误时，前端应显示模型调用错误并保留 `engineToken`、当前页面和当前会话。 | 上游认证、限流、连接和超时故障在服务端转换为脱敏模型错误，前端按普通请求错误处理并保留令牌、页面和会话。 |
| A36 | passed | specs/tinyengine-ai-auth/spec.md | 全局 HTTP 拦截器不得仅凭代理路由的 HTTP 401/403 认定 TinyEngine 登录失效。 | 正式代理链路不会透传上游 401/403；全局拦截器不再把403直接视为登录失效，真正的入站401仍保持登录语义。 |
| A37 | passed | specs/tinyengine-ai-auth/spec.md | 模型错误不得取消无关 TinyEngine 请求或设置全局未登录状态。 | 模型错误返回502等非登录状态，不调用 toLogin、abortAllRequests 或全局未登录状态切换。 |
| A38 | passed | specs/tinyengine-ai-auth/spec.md | 非流式 Chat Completions 应返回 Robot 插件可消费的模型内容。 | 非流式正式路由返回 Robot 可消费的 chat.completion、choices、assistant message 和 finish_reason 结构。 |
| A39 | passed | specs/tinyengine-ai-auth/spec.md | 流式 Chat Completions 应返回可消费的 SSE 数据并包含正常完成标记。 | TinyEngineChatSseEncoder 输出标准 data 帧、delta 内容、stop 完成帧和 data: [DONE]；MVC/WebFlux HTTP 测试均通过。 |
| A40 | passed | specs/tinyengine-ai-auth/spec.md | 流式和非流式调用应遵守相同的应用权限、凭据解析和错误边界。 | 同步和流式都由同一 DefaultTinyEngineAiChatService 执行 resolveInvocation，并经过相同 provider 错误规范化边界。 |
| A41 | passed | specs/tinyengine-ai-auth/spec.md | API Key 不得出现在仓库文件、提交消息、测试快照、普通应用日志、错误通知或验收报告中。 | Runtime diff/secret scan 未发现真实模型 API Key；测试、文档和代码仅使用明确的占位凭据。 |
| A42 | passed | specs/tinyengine-ai-auth/spec.md | 诊断日志可以记录应用/模型标识、目标主机、状态分类和追踪标识，但必须脱敏敏感字段与认证头。 | 新增错误路径仅返回状态分类和固定脱敏消息，不拼接完整上游响应、API Key或Authorization头。 |
| A43 | passed | specs/tinyengine-ai-auth/spec.md | 测试应使用占位凭据或动态注入凭据，不使用真实 Key。 | 所有相关测试使用动态或显式 placeholder 凭据，未使用真实 Key。 |
| A44 | passed | specs/tinyengine-ai-auth/spec.md | 现有非 AI TinyEngine 请求的登录行为保持不变。 | 非 AI TinyEngine 请求继续使用既有认证过滤器和登录错误处理；本次改动未改变其认证语义。 |
| A45 | passed | specs/tinyengine-ai-auth/spec.md | Servlet MVC 与 WebFlux 的 TinyEngine 登录认证语义保持一致，AI Controller 不得绕过认证边界。 | Servlet/WebFlux 认证回归均通过，正式 AI Controller 位于受保护的 app-center 路由下且未绕过认证过滤器。 |
| A46 | passed | specs/tinyengine-ai-auth/spec.md | 实现应兼容 Java 8。 | Runtime 使用 JDK 8 完成相关 Java 测试，新增实现未使用 Java 9及以上 API。 |
| A47 | passed | specs/tinyengine-app-ai-model-settings/spec.md | TinyEngine 应把 AI 模型服务和模型配置持久化到数据库并按应用共享。应用 Owner 和 Admin 管理配置，Dev 使用已启用模型。模型 API Key 对浏览器不可见，由服务端透明解密并用于一次上游调用。 | 服务和模型持久化按 tenant_id/app_id 共享，Owner/Admin 管理、Dev 使用已启用项，API Key 仅在服务端透明解密。 |
| A48 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 系统应使用 `t_ai_model_service` 保存租户、应用、服务键、provider、显示名称、Base URL、加密 API Key、空密钥策略、启用状态、排序和审计字段。 | t_ai_model_service 包含租户、应用、服务键、provider、显示名、Base URL、加密密钥策略、启用、排序和审计字段。 |
| A49 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 系统应使用 `t_ai_model` 保存租户、应用、服务 ID、模型名、显示名称、启用状态、能力、排序、设置和审计字段。 | t_ai_model 包含租户、应用、服务ID、模型名、显示名、能力、settings、启用、排序和审计字段。 |
| A50 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 所有主键和外键类型应与数据库 BIGINT 对齐。 | 新表主键和 service_id 使用 BIGINT/Java Long，app_id 与既有 t_app 整型主键保持一致。 |
| A51 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 模型服务实体的 API Key 字段应使用公共 `@FieldEncrypt`，数据库原始值为密文，Mapper 读取为业务明文。 | AiModelServiceConfig.apiKey 使用公共 @FieldEncrypt；加密持久化及 MySQL 检查确认数据库密文和 Mapper 明文读取。 |
| A52 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 业务服务、Controller、Facade 和前端不得手动加解密 API Key。 | 业务服务、Facade、Controller 和前端未调用手工加解密；加密行为完全由公共 MyBatis 拦截器承担。 |
| A53 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 管理 DTO 只应返回 `hasApiKey` 和固定掩码；可用 DTO 不应包含 API Key、密文或可逆令牌。 | 管理和可用响应均使用不含 apiKey 的 AiModelServiceDto，只提供 hasApiKey 和固定掩码。 |
| A54 | passed | specs/tinyengine-app-ai-model-settings/spec.md | MySQL 与 H2 迁移应幂等，并强制应用内服务键、服务内模型名等唯一性。 | H2迁移重复执行测试和MySQL 5.7.44迁移两次检查均通过；应用内服务键、服务内模型名唯一约束实际生效。 |
| A55 | passed | specs/tinyengine-app-ai-model-settings/spec.md | `app_owner`、`app_admin` 应能读取管理视图并新增、修改、删除模型服务和模型。 | Core 的管理列表和全部 CRUD 要求 MANAGE 权限，app_owner/app_admin 权限矩阵测试通过。 |
| A56 | passed | specs/tinyengine-app-ai-model-settings/spec.md | `app_dev` 应能读取可用视图并使用已启用模型，但不能查看管理视图或执行写操作。 | app_dev 可调用 READ/DEVELOP 所需的可用列表和共享行为设置，但管理列表及服务/模型写操作被拒绝。 |
| A57 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 无有效应用角色、关系过期或跨租户用户不得读取、管理或使用配置。 | 权限服务结合当前用户、租户、应用角色及有效期执行校验，跨租户、无角色和过期关系不能读取、管理或使用配置。 |
| A58 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 所有 Mapper 操作必须同时约束 `tenant_id` 和 `app_id`；资源 ID 不得绕过隔离。 | 所有自定义模型 Mapper SQL 的资源查询、更新和删除均包含 tenant_id 与 app_id 条件。 |
| A59 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 权限、租户校验和事务必须位于 Core 服务边界，由 MVC、WebFlux 和 AI 调用共享。 | 权限、租户校验、事务和锁均位于 AppAiModelConfigServiceImpl；MVC、WebFlux 和 AI 调用共享该服务。 |
| A60 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 系统应提供可用列表、管理列表、服务新增/更新/删除、模型新增/更新/删除、本地配置导入和内部调用解析操作。 | Core/Fascade 提供可用列表、管理列表、设置读写、服务/模型 CRUD、本地导入、内部解析和应用清理操作。 |
| A61 | passed | specs/tinyengine-app-ai-model-settings/spec.md | API Key 更新采用三态：空白输入保留，非空输入替换，`clearApiKey=true` 清除；替换与清除同时出现时拒绝。 | API Key 三态逻辑及冲突拒绝由 Core 实现，Runtime 的 serviceApiKeyUpdateHasKeepReplaceAndClearStates 等测试通过。 |
| A62 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 删除服务应同时删除其模型；应用删除应在同一事务中删除该应用全部服务和模型。 | 删除服务先按 tenant/app/service 删除模型再删除服务；应用删除按 tenant/app 删除全部模型和服务。 |
| A63 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 模型设置中的 `defaultModel`、`quickModel`、`chatMode`、`enableThinking` 及未知设置键在读写、迁移和更新时不得无意丢失。 | AppAiModelSettings.copyOf 和 persistSettings 保留 defaultModel、quickModel、chatMode、enableThinking、应用 extendConfig 及未知键。 |
| A64 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 并发导入应通过统一应用锁和当前读保证最多一次成功，且数据库已有配置时不覆盖。 | 导入在事务内先锁 t_app，再对服务和模型表执行 FOR UPDATE 当前读；已有远端配置时拒绝且数据库唯一约束提供最终保护。 |
| A65 | passed | specs/tinyengine-app-ai-model-settings/spec.md | MVC 与 WebFlux 应提供同构的 `/app-center/api/apps/{appId}/ai-model-services` 资源 API。 | MVC/WebFlux 均暴露同构的 /app-center/api/apps/{appId}/ai-model-services 设置、列表、CRUD和导入路由。 |
| A66 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 两个 Web 栈应委托相同 `TinyEngineApiFacade` 和 Core 服务，不在 Controller 复制角色或密钥逻辑。 | 两套 Controller 都是薄适配器，统一委托 TinyEngineApiFacade，再由 DefaultTinyEngineApiFacade 调用同一 Core 服务。 |
| A67 | passed | specs/tinyengine-app-ai-model-settings/spec.md | WebFlux 调用阻塞 Core 服务时应使用现有 blocking scheduler。 | 正式 TinyEngineAiWebFluxController.chatCompletions 通过 TinyEngineReactiveExecutor.submit 执行同步和流式 Core/LangChain4j 阻塞调用；调度线程测试通过。 |
| A68 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 错误响应应脱敏，不包含 API Key、认证头或完整上游错误。 | 正式 Controller 将 TinyEngineUpstreamException 转为固定502消息，响应不包含 API Key、认证头或完整上游错误。 |
| A69 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 浏览器聊天请求应提交 `appId`、`serviceId`、`modelName`、消息和生成参数，不提交 Base URL 或 API Key。 | Robot 聊天请求提交 appId、serviceId、modelName、消息和生成参数，并删除兼容 model 字段，不发送 Base URL或API Key。 |
| A70 | passed | specs/tinyengine-app-ai-model-settings/spec.md | AI Chat Service 应调用 Core 的内部解析接口，校验当前用户可使用目标应用和模型。 | DefaultTinyEngineAiChatService 的同步和流式入口都调用 AppAiModelConfigService.resolveInvocation，由 Core 校验当前用户应用权限。 |
| A71 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 内部调用配置应携带 provider、endpoint、解密 API Key、模型名及必要设置，且不参与 HTTP 序列化。 | AiModelInvocationConfig 携带 provider、baseUrl、解密API Key、modelName和settings，并通过 JsonIgnoreProperties 阻止HTTP序列化。 |
| A72 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 真实 LangChain4j provider 应使用内部调用配置创建本次请求所需的模型客户端，不应继续使用与应用无关的预注册凭据。 | LangChain4j provider 在存在内部调用配置时即时创建 OpenAiChatModel/OpenAiStreamingChatModel，使用解析出的地址、密钥和模型，而非无关预注册凭据。 |
| A73 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 同步和流式调用应使用相同的凭据解析和错误边界。 | 同步和流式共享 resolveInvocation、内部请求构造、Base URL规范化及 normalizeUpstreamFailure。 |
| A74 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 上游 401/403、限流或网络错误应作为模型调用错误返回，不得解释为 TinyEngine 登录失效。 | 401/403及429等HTTP失败统一成为脱敏上游异常；普通连接、超时和其他非HTTP失败统一包装为脱敏502模型错误，双栈Controller不返回TinyEngine登录401。 |
| A75 | passed | specs/tinyengine-app-ai-model-settings/spec.md | Robot 插件应从当前应用的远端 API 加载服务、模型和选择设置。 | Robot 初始化和 reload 均通过 modelConfigApi 从当前 appId 加载远端服务、模型和共享选择设置。 |
| A76 | passed | specs/tinyengine-app-ai-model-settings/spec.md | Owner/Admin 应看到模型服务和模型的管理入口；Dev 只看到已启用模型的选择能力。 | app_owner/app_admin 加载管理视图并启用管理UI；app_dev 仅加载已启用的可用服务和模型。 |
| A77 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 前端状态、表单回填、localStorage 和聊天 payload 不得包含服务端 API Key。 | 远端模型类型不含 apiKey，状态仅保存掩码；聊天选择仅生成 appId/serviceId/modelName，正常 localStorage 不再写服务端密钥。 |
| A78 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 编辑服务时应显示是否已有密钥和固定掩码；空输入保留，显式操作才清除。 | 编辑对话框显示 hasApiKey 对应的固定掩码，初始化时密钥输入为空；只有非空输入替换或显式 clearApiKey 才修改密钥。 |
| A79 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 同一错误不得由组件和全局拦截器重复弹框。 | Robot 组件请求错误沿统一处理链传递，认证错误标记 skipShowError，避免组件与全局拦截器对同一错误重复通知。 |
| A80 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 旧 `tiny-engine-robot-settings` 在远端无配置且操作者可管理时应尝试一次性导入。 | Owner/Admin 初始化时若远端服务和行为设置均为空，会读取 tiny-engine-robot-settings 并调用一次 import-local。 |
| A81 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 远端已有配置时不得导入或覆盖。 | 只要远端存在服务或任一共享行为设置，前端不发起导入；Core 也会在锁后再次拒绝已有配置。 |
| A82 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 导入成功后只删除旧模型服务、模型选择及已迁移字段，保留聊天设置和未知键；迁移失败时不得改写本地配置。 | 导入成功后仅清除 services/defaultModel/quickModel，保留 chatMode、enableThinking和未知键；导入抛错时清理逻辑不会执行。 |
| A83 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 浏览器旧 API Key 仅用于受控迁移请求，不得继续用于正常聊天调用。 | 旧 API Key 只被 buildImportRequest 收集到受控 import-local 的 apiKeys 字段；正式聊天请求和provider配置不读取它。 |
| A84 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 删除应用时应先删除模型和服务，再删除应用角色与应用，并保持统一事务和锁顺序。 | AppServiceImpl 的统一事务在删除角色和应用前调用模型配置清理；配置服务锁定应用并按模型、服务顺序删除。 |
| A85 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 任一步失败时应回滚；成功后该租户和应用的两张模型配置表应无残留。 | 应用删除及配置清理均声明 rollbackFor=Exception；Runtime 的生命周期和MySQL检查确认成功后应用及两张配置表无残留。 |
| A86 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 实现应兼容 Java 8。 | 所有新增Java代码兼容Java 8，JDK8 Runtime测试全部通过。 |
| A87 | passed | specs/tinyengine-app-ai-model-settings/spec.md | TinyEngine 登录令牌继续由浏览器通过 `Authorization: Bearer <engineToken>` 发送，模型凭据不得覆盖该请求头。 | 浏览器HTTP层继续独立设置 Authorization: Bearer engineToken；模型API Key仅存在于服务端构造的上游客户端中。 |
| A88 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 真正的 TinyEngine 登录错误继续清理登录状态；上游模型错误保留当前页面、会话和登录状态。 | 真实TinyEngine 401/登录错误码仍执行toLogin和清理令牌；上游模型错误统一为非登录错误，保留页面、会话和登录状态。 |
| A89 | passed | specs/tinyengine-app-ai-model-settings/spec.md | 仓库、配置、日志、测试和验收产物不得包含真实 API Key。 | Runtime高风险秘密扫描和当前差异检查均通过，仓库、测试、日志及验收材料只包含占位凭据。 |

## Checks

| Check | Command | Working directory | Status | Exit | Duration |
| --- | --- | --- | --- | ---: | ---: |
| Core JDK8 clean model settings regression | -lc export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; mvn -f fw-plus-core/fw-plus-project/pom.xml -pl fw-plus-tiny-engine/fw-plus-tiny-engine-core -Dtest=OpenAiCompatibleBaseUrlTest,AppAiModelConfigServiceTest,AiModelEncryptedPersistenceTest,AppLifecycleServiceTest,TinyEngineMapperResourceTest clean install | . | passed | 0 | 23464 ms |
| Boot JDK8 clean authentication and MySQL manifest regression | -lc export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; mvn -f fw-plus-boot/fw-plus-boot-project/pom.xml -pl fw-plus-boot-autoconfigure,fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter -Dtest=TinyEngineServletAuthenticationFilterTest,TinyEngineReactiveAuthenticationFilterTest,TinyEngineMysqlManifestCompatibilityTest -Dexec.skip=true clean install | . | passed | 0 | 55261 ms |
| AI JDK8 clean formal MVC WebFlux and legacy isolation regression | -lc export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; mvn -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-autoconfigure -Dtest=TinyEngineAiControllerTest,TinyEngineAiWebFluxAutoConfigurationTest,TinyEngineAiAutoConfigurationTest,TinyEngineAiWebAutoConfigurationTest,TinyEngineOfficialAiControllerTest,TinyEngineOfficialAiWebFluxControllerTest,TinyEngineOfficialAiStreamingTest clean install | . | passed | 0 | 48589 ms |
| LangChain4j clean upstream and network failure regression | -lc export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; mvn -f fw-plus-ai/fw-plus-ai-project/pom.xml -pl fw-plus-ai-boot-starters/fw-plus-ai-tiny-engine-plugin-langchain4j -Dtest=LangChain4jTinyEngineAiProviderTest clean install | . | passed | 0 | 19035 ms |
| Isolated MySQL 5.7.44 fresh manifest persistence encryption and cleanup | -lc set -euo pipefail DATADIR=$(mktemp -d /tmp/tinyengine-runtime-mysql57.XXXXXX) SOCKET="$DATADIR/mysql.sock" PIDFILE="$DATADIR/mysql.pid" LOGFILE="$DATADIR/mysql.log" PORT=33307 cleanup() { if [ -S "$SOCKET" ]; then mysqladmin -uroot --socket="$SOCKET" shutdown >/dev/null 2>&1 \|\| true; fi python3 -c 'import shutil,sys; shutil.rmtree(sys.argv[1], ignore_errors=True)' "$DATADIR" git restore -- fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/target } trap cleanup EXIT mysqld --initialize-insecure --datadir="$DATADIR" --log-error="$LOGFILE" mysqld --datadir="$DATADIR" --socket="$SOCKET" --port="$PORT" --bind-address=127.0.0.1 --pid-file="$PIDFILE" --log-error="$LOGFILE" --daemonize for i in $(seq 1 60); do if mysqladmin -uroot --socket="$SOCKET" ping --silent >/dev/null 2>&1; then break; fi if [ "$i" -eq 60 ]; then tail -100 "$LOGFILE"; exit 1; fi sleep 1 done mysql -uroot --socket="$SOCKET" -e 'CREATE DATABASE tiny_engine_verify CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci' export TINY_ENGINE_MYSQL_TEST_URL="jdbc:mysql://127.0.0.1:${PORT}/tiny_engine_verify?useUnicode=true&characterEncoding=UTF-8&useSSL=false&serverTimezone=Asia/Shanghai" export TINY_ENGINE_MYSQL_TEST_USERNAME=root export TINY_ENGINE_MYSQL_TEST_PASSWORD= [REDACTED] JAVA_HOME=$(/usr/libexec/java_home -v 1.8) export PATH="$JAVA_HOME/bin:$PATH" mvn -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml \ -Dtest=TinyEngineAiMysqlIntegrationTest,TinyEngineAiSampleApplicationTest#startsWithTinyEnginePersistenceAndAiWebEndpoint \ -Dspring.datasource.url="$TINY_ENGINE_MYSQL_TEST_URL" \ -Dspring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver \ -Dspring.datasource.username=root \ -Dspring.datasource.password= [REDACTED] -Dfw.plus.tiny-engine.sql.database=mysql \ -Dfw.plus.tiny-engine.sql.auto-initialize=true \ clean test | . | passed | 0 | 42359 ms |
| Sample clean formal app-center route end-to-end | -lc set -e; trap 'git restore -- fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/target' EXIT; export JAVA_HOME=$(/usr/libexec/java_home -v 1.8); export PATH="$JAVA_HOME/bin:$PATH"; mvn -f fw-plus-sample/fw-plus-ai-samples/fw-plus-ai-tiny-engine-sample/pom.xml -Dtest=TinyEngineAiSampleApplicationTest#appCenterChatUsesServerSideModelConfiguration clean test | . | passed | 0 | 31722 ms |
| Robot full Vitest regression | -lc ./node_modules/.bin/vitest run | fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot | passed | 0 | 3618 ms |
| Robot production plugin build | -lc ./node_modules/.bin/vite build | fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-tiny-engine-spring-boot-starter/src/console/packages/plugins/robot | passed | 0 | 11001 ms |
| Diff and high-risk secret scan | -lc set -o pipefail; git diff --check; changed_file=$(mktemp /tmp/tinyengine-runtime-scan.XXXXXX); { git diff --name-only; git ls-files --others --exclude-standard; } \| sort -u > "$changed_file"; if xargs -I{} sh -c 'test -f "$1" && printf "%s\0" "$1"' sh {} < "$changed_file" \| xargs -0 rg -n --no-heading 'sk-[A-Za-z0-9_-]{16,}\|AIza[0-9A-Za-z_-]{20,}\|-----BEGIN (RSA \|EC \|OPENSSH )?PRIVATE KEY-----\|Bearer[[:space:]]+[A-Za-z0-9._-]{24,}'; then exit 1; else scan_code=$?; test "$scan_code" -eq 1; fi | . | passed | 0 | 638 ms |

## Blockers

_None._

## Risks and skipped work

- 当前 verify-ready 状态尚未生成 verification.md；本结论依据 status/show 中的89项正式验收、brief、两个Spec、当前实现与差异、Runtime检查摘要及现有测试报告作出。
- MySQL空库检查使用本机MySQL 5.7.44隔离实例而非Docker；版本一致且实例、数据目录已在检查后清理。
- 第4轮未重复运行未受本轮Java/SQL修改影响的Robot测试与构建；采用上一候选已通过的109项Vitest和Vite生产构建结果。

## Previous iterations

| Goal cycle | Iteration | Attempt | Outcome | Unresolved | Summary | Completed |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 1 | fail | A30, A39 | 候选的大部分应用级模型配置、安全、权限、迁移和前端行为成立，但 A30 Base URL 规范化与 A39 HTTP SSE 协议未满足，本轮应返回 Build 修复后重新验证。 | 2026-08-14T06:49:24.623Z |
| 1 | 2 | 1 | fail | A1, A8, A9, A19, A26, A28, A30, A31, A38, A39, A40, A45, A47, A59, A68, A70, A83 | 验收失败。候选已正确修复独立的 Base URL 规范化工具和 MVC/WebFlux SSE 编码格式，且 Runtime 的 47+28+6 项 JDK8 检查及 diff/secret scan 均通过；但这些修复落在 /tiny-engine/ai 兼容 Controller。Robot 仍调用 /app-center/api/ai/chat 与 /app-center/api/chat/completions，这两条路由由旧官方 Controller 处理，继续要求/接受浏览器 baseUrl、apiKey、model，未调用 Core resolveInvocation，也未使用新 SSE 编码。故 A30/A39 在正式链路上仍不成立，并连带破坏应用权限、服务端凭据来源和真实聊天调用等验收项。 | 2026-08-14T10:20:26.127Z |
| 1 | 3 | 1 | fail | A16, A18, A54, A67, A74 | 第 3 轮已真正修复上一轮 17 个正式链路失败点：旧 official Controller 不再自动注册/扫描，Robot 的 /tiny-engine/app-center/api/ai/chat 与 /app-center/api/chat/completions 均进入 TinyEngineAiChatService -> Core resolveInvocation -> LangChain4j，攻击者 baseUrl/apiKey/model 不成为凭据来源，同步 choices 与 SSE delta/stop/[DONE]、双栈认证、Base URL 和 401/403 脱敏均成立。但验收仍失败：A67 的正式 WebFlux AI 调用未使用 blocking scheduler；A74 的普通网络/超时异常未统一映射为受控模型错误。另 A16/A18/A54 因本机 MySQL 5.7.44 在既有 mysql.007 迁移语法处启动失败而 blocked。 | 2026-08-14T13:35:29.125Z |
| 1 | 4 | 1 | pass | — | 通过。上一轮未解决的A16/A18/A54已由MySQL 5.7兼容迁移、真实密文跨会话持久化及应用清理检查收敛；A67正式WebFlux聊天路由已使用TinyEngineReactiveExecutor；A74已覆盖401/403、限流、连接、超时及其他非HTTP错误的统一脱敏模型错误边界。其余验收项未见回归，Runtime相关JDK8回归均通过。 | 2026-08-14T15:30:27.336Z |

## Conclusion

通过。上一轮未解决的A16/A18/A54已由MySQL 5.7兼容迁移、真实密文跨会话持久化及应用清理检查收敛；A67正式WebFlux聊天路由已使用TinyEngineReactiveExecutor；A74已覆盖401/403、限流、连接、超时及其他非HTTP错误的统一脱敏模型错误边界。其余验收项未见回归，Runtime相关JDK8回归均通过。
