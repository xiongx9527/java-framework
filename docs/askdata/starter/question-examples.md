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

### Core Chat/Embedding 分层路由

Core 请求直接表达真实业务来源，不把普通候选伪装成项目、数据源或 Agent 配置：

```java
AskDataModelSelection chat = AskDataModelSelection.builder()
        .requestModelKey("chat-request")
        .projectModelKey("chat-project")
        .datasourceModelKey("chat-datasource")
        .agentTaskModelKey("chat-agent")
        .policyModelKey("chat-policy")
        .fallbackModelKeys(Arrays.asList("chat-fallback-a", "chat-fallback-b"))
        .policyTags(Collections.singletonMap("purpose", "askdata"))
        .build();
AskDataModelSelection embedding = AskDataModelSelection.builder()
        .projectModelKey("embedding-project")
        .datasourceModelKey("embedding-datasource")
        .fallbackModelKeys(Collections.singletonList("embedding-fallback"))
        .build();
AskDataRequest request = AskDataRequest.builder(
        new AskDataProjectContext("project-a", "operator-a"), "查询本月已支付订单数")
        .datasourceKey("sales")
        .chatModelSelection(chat)
        .embeddingModelSelection(embedding)
        .execute(Boolean.FALSE)
        .build();
AskDataResult result = askDataService.ask(request);
```

需要相应导入 Core 的 `AskDataRequest`、`AskDataResult`、`AskDataService`、`AskDataProjectContext` 和 `AskDataModelSelection`，以及 JDK 的 `Arrays`、`Collections`。Router 按请求、项目、数据源、Agent、策略、fallback、默认顺序分别处理 Chat 与 Embedding；响应、检查点、AgentState 和监控日志记录真实路由原因、每次尝试及最终实际模型 key。

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

每个子问题拥有 `questionId`、独立审计和独立状态，聚合结果不能把失败子问题改写成零值。多问题恢复使用父检查点：恢复令牌、项目、操作人、授权版本、计划快照和模型路由快照全部一致时，只调度未完成节点；已成功节点和已完成 SQL 不重放。任一身份或快照不匹配、检查点版本不兼容、SQL 副作用状态不确定时受控拒绝恢复。

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

## 生命周期与取消

正常请求发布 `QUESTION_PLAN_STARTED`，随后进入对话、计划、子问题、模型、Agent、SQL、记忆、压缩、检查点和终态事件。步骤 Agent 必须先发布 `AGENT_NODE_STARTED`，再以 `AGENT_NODE_EXECUTED`、`AGENT_NODE_FAILED` 或 `AGENT_NODE_CANCELLED` 唯一结束。取消会保留已完成子结果，并依次形成对应的 `SUB_QUESTION_CANCELLED` 与 `CONVERSATION_CANCELLED`；规划阶段取消使用 `QUESTION_PLAN_CANCELLED`。

父请求、子问题和 Agent 事件共享父 `requestId`。每个子问题使用独立子会话与 child-root event，并携带 `subQuestionId` 和 `parentEventId` 回指父链。所有事件仅包含脱敏摘要；本地日志、LangSmith、Langfuse 或 OpenTelemetry 传输失败不得修改取消、SQL 或对话结果。
