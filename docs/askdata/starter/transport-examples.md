# HTTP、RPC、消息和 MCP 重实现示例

业务可以重实现发布层，但只能调用一个受控的 `AskDataFacade`，不能直接调用模型、JDBC 连接或共享状态实现。企业微信、钉钉、飞书的登录授权使用 [JustAuth](https://gitee.com/yadong.zhang/JustAuth) 由业务 IAM 完成；发布层只接收其已经认证和授权映射出的 `operatorId/projectId`，不新增自研扫码或 OAuth 协议。下列伪接口展示输入、输出和边界；真实实现应将 `AskDataContext` 映射到 Core DTO。

## 统一门面

```java
interface AskDataFacade {
    AskDataResult ask(AskDataRequest request);
}

final class AskDataRequest {
    String projectId;
    String operatorId;
    String question;
    String modelKey;
    String requestId;
}
```

门面执行项目授权、模型路由、记忆隔离、SQL 插件链、最终校验、审计和观测。发布层只负责协议解析、认证映射、超时、幂等和结果编码。

## HTTP/REST 与 SSE

企业平台请求到达 HTTP 层前，业务 IAM/JustAuth 已完成回调、扫码或免登，并完成组织用户到项目成员的授权映射。HTTP 层只从可信的业务身份上下文取得 `operatorId/projectId`，不接受前端自行声明的管理员身份或未签名映射。

```text
POST /api/askdata/v1/questions
Authorization: Bearer <short-lived-token>
Idempotency-Key: <request-id>
Content-Type: application/json

{"projectId":"project-a","operatorId":"operator-a","question":"本月订单数","modelKey":"ollama.chat.default"}
```

响应包含 `requestId`、稳定 `status`、子问题结果和脱敏的 `auditId`。SSE 事件只发送 `started`、`progress`、`result`、`failed`、`completed`，断线重连使用只读事件游标，不重复执行 SQL。

## RPC/gRPC

定义 `AskDataRequest`、`AskDataResult`、`AskDataError` 和 `AskDataEvent` 的版本字段。服务端校验 mTLS/JWT、版本、项目成员和能力白名单；deadline 传递到模型、SQL 和工具节点。RPC 重试只允许明确幂等的规划节点，不能重试执行中的 SQL 或外部副作用。

## 消息队列

消息体至少包含 `requestId`、`projectId`、`operatorId` 摘要、`capabilityKey`、`schemaVersion`、`attempt` 和 `idempotencyKey`。消费者先校验签名和授权，再领取租约；重复消息只返回已保存结果。失败消息进入隔离队列，保留稳定错误码和审计 ID，不把完整 token、SQL 参数或原始响应放入消息。

## MCP

发布 `mcp_question`、`mcp_ws_list` 和 `mcp_datasource_list` 时，tool schema 固定必填项目上下文或从已认证 session 注入。每次调用都重新做权限过滤；MCP 返回结构化结果和错误，不返回连接串、凭据或未授权表。资源订阅断开后只能重新查询，不能凭旧消息恢复执行状态。

## 重新实现验收

同一组契约测试分别驱动 HTTP、RPC、消息和 MCP：成功问数、参数错误、认证失败、项目越权、模型超时、SQL 拒绝、重复请求、断线重连和部分失败。企业平台另行由业务 IAM/JustAuth 验证回调、扫码、免登、组织同步、身份撤销和授权映射，然后用映射后的 `operatorId/projectId` 调用 AskData；不得在 AskData 发布层重复实现这些协议。四种发布层的 `auditId`、稳定错误码、项目/操作人隔离和幂等语义必须一致。
