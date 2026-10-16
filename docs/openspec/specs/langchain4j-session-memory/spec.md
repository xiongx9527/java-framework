# langchain4j-session-memory Specification

## Purpose
定义助手记忆的会话级作用域与一级滚动会话摘要行为，使同一用户的多次对话彼此隔离，并在长会话中保持主线信息不丢失。

## Requirements

### Requirement: 记忆键包含会话维度

助手聊天记忆 SHALL 以五段键 `{tenantId}:{appId}:{agentId}:{userId}:{sessionId}` 划分作用域，其中 `sessionId` 标识单次会话；记忆的读取与写入 SHALL 使用同一会话键。

#### Scenario: 同用户不同会话互相隔离

- **WHEN** 用户 u1 在会话 s1 中写入消息
- **THEN** 以 u1 与 s2 组成的键读取记忆时，SHALL NOT 返回 s1 的消息

#### Scenario: 既有四段键解析不受影响

- **WHEN** 记忆键为四段 `{tenantId}:{appId}:{agentId}:{userId}`
- **THEN** 系统 SHALL 仍能解析出 tenant、app、agent、user 四段并保持既有回退语义

### Requirement: 会话标识兜底

当调用方未提供 `sessionId` 时，系统 SHALL 通过可配置的会话解析器补全会话标识，默认按天生成，使最终生效的记忆键恒为五段。

#### Scenario: 未提供会话时按天兜底

- **WHEN** 调用方未提供 `sessionId`
- **THEN** 系统 SHALL 生成按天标识（形如 `dYYYYMMDD`）并以此构成五段记忆键

#### Scenario: 显式会话优先于兜底

- **WHEN** 调用方显式提供了 `sessionId`
- **THEN** 系统 SHALL 使用该值，SHALL NOT 用兜底规则覆盖它

### Requirement: 助手公开的四段记忆标识保持不变

`SimpleAssistant.memoryId()` SHALL 继续返回四段标识；会话维度 SHALL 通过新增能力暴露，SHALL NOT 改变该既有方法的返回契约。

#### Scenario: 既有断言保持

- **WHEN** 以 tenant=t1、app=crm、agent=bot、user=u1 构造助手并读取 `memoryId()`
- **THEN** 返回值 SHALL 等于 `t1:crm:bot:u1`

### Requirement: 一级会话摘要滚动维护

系统 SHALL 为每个会话至多维护一级滚动的会话摘要，覆盖范围限于已滚出最近消息窗口的历史；摘要 SHALL 在写入路径上抢在窗口驱逐之前更新，并在读取路径上兜底补齐。

#### Scenario: 超阈值触发摘要

- **WHEN** 某会话的历史累计超出 `trigger-tokens`
- **THEN** 系统 SHALL 生成或更新该会话摘要，并将其压缩到 `keep-tokens` 以内

#### Scenario: 摘要不删除原文

- **WHEN** 某会话摘要已生成
- **THEN** 该会话的原始消息 SHALL 仍完整保留在消息存储中，只有显式删除才移除

#### Scenario: 摘要失败时回退

- **WHEN** 摘要生成失败
- **THEN** 本轮上下文 SHALL 退化为最近若干条原文，主对话 SHALL NOT 中断

### Requirement: 摘要策略可插拔

系统 SHALL 支持按配置选择摘要策略，至少包含增量精炼、全量重摘、分段映射归并、逐段留存与结构化状态更新；默认 SHALL 为增量精炼，并按配置的间隔周期性执行一次全量重摘。

#### Scenario: 按配置切换摘要策略

- **WHEN** 配置指定了某个受支持的摘要策略
- **THEN** 系统 SHALL 使用该策略生成该会话摘要

#### Scenario: 增量与全量重摘交替

- **WHEN** 某会话连续执行增量精炼达到 `rebuild-every` 次数
- **THEN** 系统 SHALL 对该会话执行一次全量重摘

### Requirement: 摘要作用域与会话一致

会话摘要 SHALL 以会话键为作用域；新建会话 SHALL NOT 继承其它会话的摘要内容。

#### Scenario: 跨会话摘要不串用

- **WHEN** 同一用户开启一个新会话
- **THEN** 该会话 SHALL 以空摘要开始，SHALL NOT 读取其它会话的摘要
