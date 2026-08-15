# TinyEngine 应用级 AI 模型设置

## 目标

TinyEngine 应把 AI 模型服务和模型配置持久化到数据库并按应用共享。应用 Owner 和 Admin 管理配置，Dev 使用已启用模型。模型 API Key 对浏览器不可见，由服务端透明解密并用于一次上游调用。

## 数据模型与加密

- 系统应使用 `t_ai_model_service` 保存租户、应用、服务键、provider、显示名称、Base URL、加密 API Key、空密钥策略、启用状态、排序和审计字段。
- 系统应使用 `t_ai_model` 保存租户、应用、服务 ID、模型名、显示名称、启用状态、能力、排序、设置和审计字段。
- 所有主键和外键类型应与数据库 BIGINT 对齐。
- 模型服务实体的 API Key 字段应使用公共 `@FieldEncrypt`，数据库原始值为密文，Mapper 读取为业务明文。
- 业务服务、Controller、Facade 和前端不得手动加解密 API Key。
- 管理 DTO 只应返回 `hasApiKey` 和固定掩码；可用 DTO 不应包含 API Key、密文或可逆令牌。
- MySQL 与 H2 迁移应幂等，并强制应用内服务键、服务内模型名等唯一性。

## 权限与隔离

- `app_owner`、`app_admin` 应能读取管理视图并新增、修改、删除模型服务和模型。
- `app_dev` 应能读取可用视图并使用已启用模型，但不能查看管理视图或执行写操作。
- 无有效应用角色、关系过期或跨租户用户不得读取、管理或使用配置。
- 所有 Mapper 操作必须同时约束 `tenant_id` 和 `app_id`；资源 ID 不得绕过隔离。
- 权限、租户校验和事务必须位于 Core 服务边界，由 MVC、WebFlux 和 AI 调用共享。

## 配置操作

- 系统应提供可用列表、管理列表、服务新增/更新/删除、模型新增/更新/删除、本地配置导入和内部调用解析操作。
- API Key 更新采用三态：空白输入保留，非空输入替换，`clearApiKey=true` 清除；替换与清除同时出现时拒绝。
- 删除服务应同时删除其模型；应用删除应在同一事务中删除该应用全部服务和模型。
- 模型设置中的 `defaultModel`、`quickModel`、`chatMode`、`enableThinking` 及未知设置键在读写、迁移和更新时不得无意丢失。
- 并发导入应通过统一应用锁和当前读保证最多一次成功，且数据库已有配置时不覆盖。

## HTTP API

- MVC 与 WebFlux 应提供同构的 `/app-center/api/apps/{appId}/ai-model-services` 资源 API。
- 两个 Web 栈应委托相同 `TinyEngineApiFacade` 和 Core 服务，不在 Controller 复制角色或密钥逻辑。
- WebFlux 调用阻塞 Core 服务时应使用现有 blocking scheduler。
- 错误响应应脱敏，不包含 API Key、认证头或完整上游错误。

## AI 调用

- 浏览器聊天请求应提交 `appId`、`serviceId`、`modelName`、消息和生成参数，不提交 Base URL 或 API Key。
- AI Chat Service 应调用 Core 的内部解析接口，校验当前用户可使用目标应用和模型。
- 内部调用配置应携带 provider、endpoint、解密 API Key、模型名及必要设置，且不参与 HTTP 序列化。
- 真实 LangChain4j provider 应使用内部调用配置创建本次请求所需的模型客户端，不应继续使用与应用无关的预注册凭据。
- 同步和流式调用应使用相同的凭据解析和错误边界。
- 上游 401/403、限流或网络错误应作为模型调用错误返回，不得解释为 TinyEngine 登录失效。

## 前端模型设置

- Robot 插件应从当前应用的远端 API 加载服务、模型和选择设置。
- Owner/Admin 应看到模型服务和模型的管理入口；Dev 只看到已启用模型的选择能力。
- 前端状态、表单回填、localStorage 和聊天 payload 不得包含服务端 API Key。
- 编辑服务时应显示是否已有密钥和固定掩码；空输入保留，显式操作才清除。
- 同一错误不得由组件和全局拦截器重复弹框。

## 旧配置迁移

- 旧 `tiny-engine-robot-settings` 在远端无配置且操作者可管理时应尝试一次性导入。
- 远端已有配置时不得导入或覆盖。
- 导入成功后只删除旧模型服务、模型选择及已迁移字段，保留聊天设置和未知键；迁移失败时不得改写本地配置。
- 浏览器旧 API Key 仅用于受控迁移请求，不得继续用于正常聊天调用。

## 删除与生命周期

- 删除应用时应先删除模型和服务，再删除应用角色与应用，并保持统一事务和锁顺序。
- 任一步失败时应回滚；成功后该租户和应用的两张模型配置表应无残留。

## 兼容性与安全

- 实现应兼容 Java 8。
- TinyEngine 登录令牌继续由浏览器通过 `Authorization: Bearer <engineToken>` 发送，模型凭据不得覆盖该请求头。
- 真正的 TinyEngine 登录错误继续清理登录状态；上游模型错误保留当前页面、会话和登录状态。
- 仓库、配置、日志、测试和验收产物不得包含真实 API Key。
