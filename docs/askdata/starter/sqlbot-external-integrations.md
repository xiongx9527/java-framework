# SQLBot 外部系统对接全集

外部系统只获得受控 API、MCP tool 或嵌入 token，不能绕过 AskData 的项目成员授权、SQL 最终校验、审计和监控日志。所有联调使用独立测试租户、假数据和可撤销凭证。

## 模型、嵌入与发布入口

| 系统/入口 | 对接内容 | 必须验证 |
| --- | --- | --- |
| Ollama | 聊天模型 `qwen3:32b`、嵌入模型 `bge-m3`、逻辑能力 key | 连接超时可换候选；模型密钥和原始响应不进日志 |
| MCP Server | `mcp_question`、工作空间列表、数据源列表、结果/图表资源 | tool 入参带项目和操作人上下文；返回只含授权资源 |
| 嵌入式对接 | 基础、页面、全屏和区域小助手 | 只签发短期 token；前端不保存宿主 secret |
| DataEase | `x-de-token`、数据源/问数接口、AES-Key 配置 | token/AES-Key 只存密钥引用；跨域、过期和轮换可验证 |
| MaxKB | 问数知识库或应用调用 | 绑定项目和数据源范围，拒绝未授权知识/SQL |
| Dify | HTTP 工具、工作流节点或 MCP | token、chat_id、会话上下文和流式错误映射可追踪 |
| Coze | Bot/插件工具调用 | workspace、bot、用户和项目映射完整，结果脱敏 |
| n8n | HTTP Request、Webhook 或自定义节点 | 重试幂等、回调签名、超时和部分失败不能重复执行 |

## X-Pack 与企业平台

X-Pack 登录认证、平台对接和参数配置由宿主企业 IAM 负责。企业微信、钉钉、飞书统一推荐使用 [JustAuth](https://gitee.com/yadong.zhang/JustAuth) 完成登录授权、回调、扫码、免登和组织用户同步；AskData Starter 不自研扫码页面、OAuth 授权协议、回调签名协议或组织同步协议。业务 IAM/JustAuth 完成认证和授权后，只把已确认的 `projectId + operatorId` 及其授权摘要传入 AskData。

| 能力 | 企业微信 | 钉钉 | 飞书 |
| --- | --- | --- | --- |
| JustAuth 来源 | 企业微信登录授权 | 钉钉登录授权 | 飞书登录授权 |
| 业务 IAM/JustAuth 负责 | 扫码、回调、免登、企业/部门/成员同步 | 扫码、回调、免登、部门/成员/角色同步 | 扫码、回调、免登、租户/部门/成员同步 |
| 传入 AskData | 已认证 `operatorId`、已授权 `projectId` | 已认证 `operatorId`、已授权 `projectId` | 已认证 `operatorId`、已授权 `projectId` |
| 必测 | JustAuth 回调重放、可信域名、撤销 | JustAuth 回调签名、免登过期 | JustAuth 重定向白名单、租户隔离 |

JustAuth 登录或免登成功后，业务 IAM 仍需完成成员映射和项目授权，不能直接创建 AskData 管理员权限。组织同步删除或停用成员时，业务 IAM 负责撤销外部身份和调用凭证；AskData 只依据收到的项目/操作人授权结果处理请求，已有 token、chat_id 和嵌入会话按业务策略失效。

## 参数与跨域

- `token` 是短期调用凭证，绑定调用方、项目、操作人、能力和过期时间。
- `chat_id` 只用于同一外部会话的关联，不能作为权限主体或跨项目恢复凭据。
- 工作空间列表和数据源列表必须按宿主授权过滤；不要把全局列表返回给外部系统。
- CORS 只允许已登记的来源、方法和请求头；预检、凭证、Origin 变更和失败都写审计。
- AES-Key、应用 secret、Webhook 签名密钥和数据库凭据使用密钥管理系统引用；配置页面只展示掩码。

## 联调顺序与验收

先用 HTTP 桩验证业务 IAM/JustAuth 已认证身份到 `operatorId/projectId` 的映射、token、chat_id 和错误映射，再验证 MCP tool，随后验证 DataEase/嵌入，最后接入 MaxKB、Dify、Coze、n8n 和企业平台。企业平台联调顺序是 JustAuth 回调/扫码/免登、组织同步、业务授权映射、AskData 调用；AskData 不增加第二套 OAuth 流程。每个系统都要覆盖成功、过期、无权限、跨域拒绝、密钥轮换、超时、重复回调和审计查询；未完成这些断言时只能标记为业务集成契约覆盖。
