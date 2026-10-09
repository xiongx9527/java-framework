# langchain4j-streaming-thinking-events Specification

## Purpose
使 LangChain4j 流式对话能够把模型明确提供的独立思考片段传递给 SSE 客户端，同时保留回答、工具和结束事件的既有协议；未提供独立思考能力的模型不得被伪装成会发出思考内容。

## Requirements

### Requirement: 流式思考片段通过既有 SSE 事件发送

流式对话 SHALL 将模型适配器明确提供的思考增量作为 `thinking` SSE 事件发送，data 为该增量文本；思考阶段结束后 SHALL 发送一次 `thinking-done` 事件，data 为本轮完整思考文本。思考转入回答、工具调用或流完成时，思考完成事件 SHALL 先于对应的 `token`、`tool-call` 或 `done` 事件发送。没有独立思考增量时 SHALL 不发送思考事件，也 SHALL 不从普通回答文本推测思考内容。

#### Scenario: 推送增量思考与完整思考
- **WHEN** 流式模型产生一个或多个独立思考增量，随后开始输出回答
- **THEN** 客户端先收到对应的 `thinking` 增量事件，再收到包含完整思考文本的 `thinking-done`，然后收到回答 `token` 事件

#### Scenario: 思考后直接调用工具
- **WHEN** 流式模型产生独立思考增量后请求执行工具
- **THEN** 客户端先收到 `thinking` 与 `thinking-done`，再收到 `tool-call`；工具执行完成后仍收到 `tool-result`

#### Scenario: 思考阶段结束时没有回答片段
- **WHEN** 流式模型已产生独立思考增量并在 `done` 前结束该思考阶段，且没有回答或工具调用事件可作为阶段边界
- **THEN** 客户端在 `done` 前收到 `thinking-done`，其 data 为本轮完整思考文本

#### Scenario: 模型没有独立思考输出
- **WHEN** 模型只产生回答文本，或 provider 适配器没有提供独立思考增量
- **THEN** 客户端只收到既有回答与完成事件，不会收到伪造或从回答文本推测的 `thinking` 事件

#### Scenario: 直接流式模型回调思考增量
- **WHEN** SimpleAssistant 通过直接 StreamingChatModel 路径流式调用模型，且 LangChain4j 的 `StreamingChatResponseHandler` 提供独立思考增量
- **THEN** 客户端收到既有格式的 `thinking` 事件；思考转入回答或流完成时先收到一次 `thinking-done`，再收到对应的 `token` 或 `done`

#### Scenario: 思考配置导致 provider 请求失败
- **WHEN** 配置了 provider 专属思考选项，但当前模型或服务端拒绝该选项
- **THEN** 流式请求通过既有错误事件与错误结束路径向客户端报告失败，不静默改写为普通思考/回答文本

### Requirement: 思考能力依赖 provider 的独立输出支持

每个流式聊天 provider SHALL 仅在其 LangChain4j 适配器可以输出独立思考增量时，才将该能力映射至思考 SSE 事件。provider 专属思考选项 SHALL 按各自真实名称和类型配置；没有该选项的 provider SHALL 保持其模型默认行为。系统 SHALL 明确记录适配器不映射独立思考输出的 provider 限制。

#### Scenario: provider 原生选项启用思考输出
- **WHEN** provider 有公开思考选项，调用方按该 provider 配置启用思考，且模型实际返回独立思考内容
- **THEN** LangChain4j 适配器提供的思考增量按本能力的 SSE 契约发送给客户端

#### Scenario: provider 没有独立思考映射
- **WHEN** provider 只在原始协议中存在思考相关字段，但 LangChain4j 适配器没有将其映射为独立思考增量
- **THEN** 系统不从原始回答流进行猜测式解析，并在能力说明中声明该 provider 当前不能发出思考 SSE 事件
