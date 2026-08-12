# AI 运行时替换

AskData Core 对模型和运行时保持厂商无关：路由只输出逻辑能力 key 与优先级，运行时适配层负责执行并把错误映射为稳定结果。当前可选的 LangChain4j Adapter 可以被替换，但 AskData Starter 不默认创建 LangChain4j、Ollama 或 Provider；项目权限、SQL 安全、审计、检查点和监控日志必须继续由 Java 8 Starter 侧执行。

## 接口边界

| 边界 | Core/Starter 责任 | 替换运行时责任 |
| --- | --- | --- |
| 模型路由 | 组合请求、项目、数据源、任务与默认 key，记录路由层级 | 根据已决策 key 查找自身模型，不重新解释优先级 |
| 模型与向量 | 定义聊天、Embedding、结果与错误契约 | 调用 SDK 或远程服务，返回脱敏的结构化结果 |
| Agent/工作流 | 保留顺序流程、问题规划、状态/恢复接口 | 将工具调用、图编排、流式事件转换为 AskData DTO |
| SQL | 接收受控 SQL 命令，运行插件和最终校验 | 只能返回计划或 SQL 命令，不能持有业务裸连接或绕过校验 |
| 观测 | 强制写入脱敏监控日志和关联标识 | 传回阶段事件、耗时、稳定错误码，不返回敏感原始载荷 |

## 替换步骤

1. 为聊天、Embedding、Agent 或远程运行时实现对应 AskData SPI，声明稳定能力 key、契约版本、优先级与配置命名空间。
2. 注册 `AskDataCapabilityProvider` 或提供同类型 Spring Bean，让自动配置的默认 Bean 让位。
3. 将 SDK 异常分类为可重试基础设施错误或不可重试策略/输入错误；仅前者允许 Core 选择下一个候选。
4. 保持 `projectId`、`operatorId`、`conversationId`、`requestId` 和审计关联 ID 在全链路传递，日志仅保存脱敏摘要。
5. 执行模型路由、故障降级、SQL 最终校验、恢复不重放和跨项目隔离的契约测试后再切换生产流量。

## 适配选项

| 运行时 | 建议接入方式 | 适用条件 |
| --- | --- | --- |
| LangChain4j | 显式引入本地 Adapter 或替换模型注册 Bean | 当前 Java 8 已实现 Adapter，适合 Java 8/Spring Boot 2.7 主进程，但不是 AskData Starter 的默认传递依赖。 |
| Spring AI | 可选内嵌适配或远程适配 | 先验证目标版本的 Java、Spring Boot、Jakarta 依赖与 BOM；不通过时使用远程模式。 |
| Spring AI Alibaba | 可选内嵌适配或远程适配 | 先验证目标版本的 Java、Spring Boot、Spring AI 依赖线和 Alibaba 组件组合；不通过时使用远程模式。 |
| AgentScope Java | Agent/工作流远程适配优先 | 对目标 AgentScope Java 版本进行 Java 与 Spring 依赖验证，再决定是否内嵌。 |
| 业务自定义运行时 | 本地 SPI 或 Remote Runtime SPI | 必须遵守同一上下文、错误、SQL 安全与审计协议。 |

## Java 8 主进程边界

当前主进程固定 Java 8 与 Spring Boot 2.7.18，**不直接引入**仅支持 Java 17+、Spring Boot 3.x+、Jakarta 命名空间或其传递依赖的运行时组件。目标版本能否内嵌不是由名称决定，而须在接入时完成 BOM 解析、最小 Maven 编译、Spring 上下文启动和契约测试。

若任一步骤出现 class file version、`javax`/`jakarta`、自动配置或依赖收敛冲突，应停止内嵌方式，改用 [Java 17 远程运行时](java17-remote-runtime.md)。远程服务不能替代 Starter 的权限、SQL 最终校验、审计和恢复授权。

## 切换与回退

采用能力 key 灰度：先将新运行时注册为候选，记录成功率、超时、稳定错误码和降级次数；验证后逐项目切换默认 key。发生模型或协议故障时移除候选或回指业务上一已验证的 LangChain4j key。已开始的 SQL、工具或外部副作用节点不得通过模型兜底重放。
