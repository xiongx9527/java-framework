# TinyEngine AI 鉴权

## 目标

TinyEngine 应允许已登录用户通过后端代理调用配置的 OpenAI 兼容模型，同时严格分离 TinyEngine 登录凭据与模型服务凭据。模型服务鉴权或调用失败不得被解释为 TinyEngine 登录失效。

## 浏览器到 TinyEngine 的鉴权

- 受保护的 TinyEngine AI 请求应继续使用 `Authorization: Bearer <engineToken>` 携带 TinyEngine 登录令牌。
- TinyEngine 认证过滤器只应把 `Authorization` 解释为 TinyEngine 登录凭据，不应把模型 API Key 当作登录令牌。
- 登录令牌缺失、无效或过期时，AI 路由应返回 TinyEngine 明确的未认证结果。
- 登录有效时，用户发送 Chat Completions 不应因模型凭据存在而覆盖或移除登录令牌。

## 模型凭据传递

- 浏览器应把当前模型的 API Key 作为 Chat Completions 请求体字段发送给 TinyEngine 后端代理。
- 启用密钥加密时，请求体可以携带现有 `EKEY_` 凭据；后端应在调用上游前按现有密钥能力解析。
- 后端转发给模型服务前应从内部请求模型中读取模型凭据，并且不得把该敏感字段复制到普通日志、错误消息或响应体。
- 后端调用上游 OpenAI 兼容接口时，才应使用模型 API Key 构造上游 `Authorization`。
- 浏览器不得直接向第三方模型地址发送配置的模型 API Key。
- 服务端不应把用户输入的真实模型 API Key持久化到应用数据库。

## 模型配置与路由

- TinyEngine AI 总开关、Web 能力、Chat 能力及所需 LangChain4j 模型模块应在目标样例中启用。
- Chat Completions 路由应注册在前端配置使用的 `/tiny-engine/app-center/api/chat/completions` 路径。
- 加密 Key 路由启用时应与机器人插件的现有配置协议兼容；不支持加密时应保持可诊断错误并允许既定明文代理路径工作。
- 模型 Base URL 应规范到 OpenAI 兼容 API 根路径，Chat Completions 的最终上游地址应正确。
- 仓库配置不得包含真实模型 API Key；运行时 Key 应来自用户设置或受控外部配置。

## 前端错误边界

- 模型 API Key 缺失且模型不允许空 Key 时，前端应在发送前提示用户配置 Key并停止请求。
- TinyEngine 返回明确登录错误码，或认证层确认登录令牌无效时，前端应清除 `engineToken` 并进入登录页。
- 上游模型返回 401/403、限流、网络错误或其他模型调用错误时，前端应显示模型调用错误并保留 `engineToken`、当前页面和当前会话。
- 全局 HTTP 拦截器不得仅凭代理路由的 HTTP 401/403 就认定 TinyEngine 登录失效；应依据 TinyEngine 认证错误语义区分登录错误和上游错误。
- 模型错误不得触发取消所有无关 TinyEngine 请求或永久设置全局未登录状态。

## 流式与非流式响应

- 非流式 Chat Completions 应返回机器人插件可消费的模型内容。
- 流式 Chat Completions 应返回插件可消费的 SSE 数据，并包含正常完成标记。
- 流式和非流式调用应遵守相同的凭据分离和错误边界。

## 安全与可观测性

- API Key 不得出现在仓库文件、提交消息、测试快照、普通应用日志、错误通知或验收报告中。
- 诊断日志可以记录模型标识、目标主机、状态分类和追踪标识，但必须脱敏敏感请求字段与认证头。
- 测试应使用占位凭据或动态注入凭据，不使用真实 Key。

## 兼容性

- 现有非 AI TinyEngine 请求的登录行为保持不变。
- Servlet MVC 的 TinyEngine 登录认证语义保持不变；AI Controller 的启用不应绕过该认证边界。
- 实现应兼容 Java 8。
