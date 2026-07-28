# 问数调用与部分失败示例

所有调用都必须带 `projectId` 和 `operatorId`。示例使用短期 token 和测试标识，响应中的 SQL、参数、模型原文和凭据均不返回给普通调用方。

## 单问题

```json
{
  "projectId": "project-a",
  "operatorId": "operator-a",
  "question": "查询本月已支付订单数",
  "requestId": "req-1001"
}
```

```json
{
  "requestId": "req-1001",
  "status": "SUCCEEDED",
  "answer": "本月已支付订单数为 1280",
  "auditId": "audit-1001"
}
```

执行顺序是授权、规划、模型路由、SQL 插件、最终校验、只读执行、结果解释和审计。任何一步失败都返回稳定错误码，不用另一个模型掩盖权限或 SQL 错误。

## 显式多问题

```json
{
  "projectId": "project-a",
  "operatorId": "operator-a",
  "questions": [
    "本月订单数",
    "本月支付金额",
    "本月退款金额"
  ],
  "requestId": "req-1002"
}
```

每个子问题拥有 `questionId`、独立审计和独立状态，聚合结果不能把失败子问题改写成零值。带恢复令牌的多问题请求应拒绝或要求业务显式选择子问题，防止重复执行已开始的副作用。

## 自然语言复合问题

输入“比较本月和上月的订单数，并列出支付金额最高的三个渠道；如果退款率超过 5%，给出原因”时，规划器应生成有依赖关系的子问题：订单数比较、渠道 Top 3、退款率判断、原因解释。每个节点记录输入上下文、依赖版本和可重入性；SQL 已发送但结果不确定时不自动重放。

## 部分失败

```json
{
  "requestId": "req-1002",
  "status": "PARTIAL_FAILED",
  "items": [
    {"questionId":"q1","status":"SUCCEEDED","answer":"本月订单数为 1280"},
    {"questionId":"q2","status":"FAILED","errorCode":"DATASOURCE_TIMEOUT"},
    {"questionId":"q3","status":"SUCCEEDED","answer":"本月退款金额为 42000"}
  ],
  "auditId": "audit-1002"
}
```

调用方应展示成功项和失败原因，允许仅重试 `q2`；重试请求带原 `requestId`、新的幂等键和授权上下文，不能重新执行已经成功的子问题。认证、权限、输入、预算和 SQL 安全错误不触发模型或子问题兜底。
