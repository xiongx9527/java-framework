# Java 17 远程运行时

当目标 AI 运行时无法与 Java 8 / Spring Boot 2.7.18 主进程兼容时，将其部署为 Java 17 独立服务。Starter 通过 Remote Runtime SPI 发送 AskData 自有 DTO，主进程继续负责项目权限、SQL 最终校验、审计、检查点和恢复授权。

## 兼容性验证

每次接入都应以接入时官方版本为准，记录版本、日期、BOM、JDK、Spring Boot、传递依赖与验证结论。不得依据旧版本经验把运行时加入 Starter 传递依赖。

| 运行时 | 官方核验入口 | 必做验证 | 结论处理 |
| --- | --- | --- | --- |
| Spring AI | [Spring AI 官方参考](https://docs.spring.io/spring-ai/reference/getting-started.html) | 核验目标版本系统要求、Spring Boot 兼容矩阵、BOM 解析、最小编译和启动 | 与 Java 8/Boot 2.7 不兼容时远程部署 |
| Spring AI Alibaba | [Spring AI Alibaba 官方文档](https://sca.aliyun.com/en/docs/2025/user-guide/) | 核验目标版本的 Spring AI、Spring Boot、JDK 与组件组合 | 出现版本或命名空间冲突时远程部署 |
| AgentScope Java | [AgentScope Java 官方文档](https://java.agentscope.io/en/docs/) | 核验 JDK、构建坐标、工作流/协议依赖与最小调用 | 不能通过主进程基线验证时远程部署 |

上述链接和兼容性会随官方发布演进，需以接入时官方版本为准。验证记录必须包含失败现象，例如 class file version、`javax`/`jakarta` 冲突或自动配置条件不满足，不能将未经验证的版本标为可内嵌。

## 部署与能力注册

1. 独立服务使用 Java 17 运行，部署在受控网络与独立凭据域；不得配置业务数据库裸连接。
2. 在 Starter 侧注册 Remote Runtime 能力 key、协议版本、支持的模型/Agent 能力、超时和熔断配置。
3. Remote Runtime 只接受被 Core 已决策的模型 key；不得在远程端改写项目、数据源或任务路由优先级。
4. 发布健康检查、能力版本和兼容范围；版本不匹配时拒绝调用并记录稳定错误码。
5. 灰度时按项目或能力 key 放量，保留默认 LangChain4j/Ollama 可回退路径。

## 传输方式

| 方式 | 用途 | 约束 |
| --- | --- | --- |
| HTTP | 同步模型、计划和结构化结果 | mTLS 或签名认证；显式连接/读取超时、幂等键和响应大小限制。 |
| SSE | 流式 token、Agent 阶段和进度事件 | 事件带 `requestId`、序号和完成标志；断线只允许从无副作用阶段恢复。 |
| MCP | 工具与上下文能力协商 | 使用受控工具 schema；工具调用回到 Starter 接受权限与 SQL 校验。 |
| A2A | 多 Agent 委派与任务状态 | 每个任务传入项目/操作人范围和审计 ID；远程 Agent 不可扩权。 |
| gRPC | 高吞吐内部 RPC | 维护 proto 版本、截止时间、双向流背压和错误码映射。 |
| 消息队列 | 长任务异步执行与隔离 | 消息携带幂等键、过期时间和关联 ID；消费端不得重放不确定 SQL。 |

## 上下文 payload 与认证

请求最少包含 `projectId`、`operatorId`、`conversationId`、`requestId`、`auditCorrelationId`、能力 key、协议版本、数据源逻辑 key、语义版本、已脱敏的提问/上下文、超时预算和幂等键。禁止发送 API Key、数据库密码、原始连接串、恢复令牌全文、未经授权的元数据或未脱敏结果集。

认证可使用 mTLS、服务 JWT、HMAC 签名或网关颁发的短期令牌。令牌须声明调用方、受众、过期时间、能力范围和请求关联 ID；密钥轮换与认证失败均写入审计。远程服务的身份只表示运行时调用方，不能替代终端操作人的项目权限。

## 错误映射、审计与回滚

远程端把网络、超时、限流、熔断和供应商暂不可用映射为可重试基础设施错误；把认证、输入、内容策略、预算、协议版本和结构化输出拒绝映射为不可重试稳定错误。原始异常、提示词和厂商响应不出现在 Core API 或监控日志中。

每次调用需记录能力 key、运行时版本、协议版本、路由层级、耗时、稳定错误码、重试标识与审计关联 ID。远程观测导出失败不能阻断 Starter 的本地脱敏监控日志。

回滚顺序为：停止新能力 key 流量、等待或取消无副作用任务、回指已验证的默认 key、保留审计与状态以便诊断。SQL 已开始、工具已产生外部副作用或状态不确定的任务不自动重放，必须返回受控不确定/恢复失败结果并由业务授权后处理。
