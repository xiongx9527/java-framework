# TinyEngine AI 鉴权

## 目标

TinyEngine 应允许已登录且拥有应用权限的用户通过后端代理调用应用配置的 OpenAI 兼容模型，同时严格分离 TinyEngine 登录凭据与模型服务凭据。模型服务鉴权或调用失败不得被解释为 TinyEngine 登录失效。

## 浏览器到 TinyEngine 的鉴权

- 受保护的 TinyEngine AI 请求应继续使用 `Authorization: Bearer <engineToken>` 携带 TinyEngine 登录令牌。
- TinyEngine 认证过滤器只应把 `Authorization` 解释为 TinyEngine 登录凭据。
- 登录令牌缺失、无效或过期时，AI 路由应返回明确的 TinyEngine 未认证结果。
- 模型调用不得覆盖、移除或复用登录令牌作为上游模型凭据。

## 模型凭据来源与使用

- 浏览器正常聊天请求不得携带模型 API Key、密文、可逆令牌或 Base URL。
- 浏览器只提交应用 ID、模型服务 ID、模型名、消息和生成参数。
- 后端应从应用共享模型配置中解析并透明解密 API Key，且只在调用上游时构造上游 `Authorization`。
- 内部模型凭据不得复制到普通日志、错误消息、HTTP 响应或前端状态。
- 旧浏览器凭据只允许通过受控的一次性迁移 API 导入数据库，不得作为正常聊天链路的备用来源。

## 模型配置与路由

- Chat Completions 路由应注册在前端使用的 `/tiny-engine/app-center/api/chat/completions` 路径。
- 模型 Base URL 应规范到 OpenAI 兼容 API 根路径，最终 Chat Completions 地址应正确。
- 后端应校验当前用户对请求应用具有有效使用权限，并校验服务和模型已启用。
- 仓库配置不得包含真实模型 API Key；运行时凭据来自数据库加密配置。

## 前端错误边界

- 未配置可用服务、模型或所需 API Key 时，前端应显示单次明确提示并停止请求。
- TinyEngine 返回明确登录错误码，或认证层确认登录令牌无效时，前端应清除 `engineToken` 并进入登录页。
- 上游模型返回 401/403、限流、网络错误或其他模型调用错误时，前端应显示模型调用错误并保留 `engineToken`、当前页面和当前会话。
- 全局 HTTP 拦截器不得仅凭代理路由的 HTTP 401/403 认定 TinyEngine 登录失效。
- 模型错误不得取消无关 TinyEngine 请求或设置全局未登录状态。

## 流式与非流式响应

- 非流式 Chat Completions 应返回 Robot 插件可消费的模型内容。
- 流式 Chat Completions 应返回可消费的 SSE 数据并包含正常完成标记。
- 流式和非流式调用应遵守相同的应用权限、凭据解析和错误边界。

## 安全与可观测性

- API Key 不得出现在仓库文件、提交消息、测试快照、普通应用日志、错误通知或验收报告中。
- 诊断日志可以记录应用/模型标识、目标主机、状态分类和追踪标识，但必须脱敏敏感字段与认证头。
- 测试应使用占位凭据或动态注入凭据，不使用真实 Key。

## 兼容性

- 现有非 AI TinyEngine 请求的登录行为保持不变。
- Servlet MVC 与 WebFlux 的 TinyEngine 登录认证语义保持一致，AI Controller 不得绕过认证边界。
- 实现应兼容 Java 8。
