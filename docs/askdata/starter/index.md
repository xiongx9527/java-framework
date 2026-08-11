# AskData Starter

AskData Starter 为 `fw-plus` 提供 Java 8、Spring Boot 2.7.18 基线下的自然语言问数装配。Core 仅暴露厂商无关的 API、上下文和值对象；Starter 负责中立能力注册和可替换 Bean 装配，不默认创建 LangChain4j、Ollama 或 Provider 模型。业务应用仍掌握模型框架选择、认证、项目成员授权、数据源连接与最终运行边界。

## 已实现功能

下表仅列出当前 Starter 与 Core 已有实现及其验证边界，不将接口预留表述为已交付业务功能。

| 能力 | 当前实现 | 使用边界 |
| --- | --- | --- |
| 核心问数 | `AskDataService`、请求/结果与旧流程适配；结果收敛为答案或稳定错误 | 默认适配兼容现有单项目流程，不能作为跨项目授权依据 |
| 项目与操作人上下文 | 请求强制携带项目、操作人上下文；记忆、推荐、监控按该范围隔离 | 宿主应用负责认证与成员、角色、数据范围的真实判定 |
| 模型路由 | 按请求、项目、数据源、智能体任务、默认配置的顺序选择逻辑模型 key | Core 只产生可审计路由决定，不直接调用模型 SDK |
| 模型 Adapter | LangChain4j Adapter 已实现并由业务显式引入；`qwen3:32b`、`bge-m3` 只是默认查找 key | 只引入 AskData Starter 时不会创建模型；Core 不依赖 LangChain4j 或 Ollama 类型 |
| 故障兜底 | 仅已分类的基础设施失败可继续选择后继候选 | 鉴权、策略、输入和预算等不可重试错误原样返回；SQL 与外部副作用不参与兜底 |
| 状态与监控日志 | 默认内存会话、检查点、Agent 状态与脱敏监控日志；项目管理员默认可读本项目日志 | 内存只限单机；外部导出失败不得影响本地日志和问数结果 |
| SQL 插件链 | 具名参数、插件审计、最终 SQL 校验、JDBC 只读执行器 | 插件不得获得裸连接；所有插件输出必须重新校验 |
| 多提问 | 稳定子问题 ID、依赖 DAG、有界并发、独立终态、父子检查点与恢复 | 部分失败保留子结果；已完成 SQL 不重放，恢复按项目、操作人和授权版本 fail-closed |
| 长期记忆 | 默认单机内存的回忆和保存 SPI 桥接 | 以项目和操作人隔离，不能跨项目或跨人回退 |
| 推荐 | 默认单机内存的个人推荐读写桥接 | 推荐画像不在不同操作人之间共享 |
| Starter 依赖装配 | Starter 显式装配 Core 契约与中立自动配置；LangChain4j Adapter 由业务另行引入 | 不传递引入具体 LangChain4j Provider、Spring AI、Spring AI Alibaba、AgentScope 或 Java 17 专属依赖 |

## 快速配置

```yaml
fw:
  plus:
    ai:
      askdata:
        enabled: true
        model:
          default-chat-model: qwen3:32b
          default-embedding-model: bge-m3
        sql:
          execute-enabled: false
          default-max-rows: 100
          enforce-limit: true
```

将 Starter 加入应用后，业务必须显式引入 LangChain4j Adapter 及所需 Provider SDK，或提供其他中立模型 Bean；上述默认值只是注册表查找 key。SQL 执行默认关闭；业务仅在已配置只读数据源、项目权限与审计留存策略后开启它。

## 默认装配与覆盖

自动配置使用 `@ConditionalOnMissingBean` 提供默认实现。业务可按粒度覆盖模型注册、SQL 校验器、SQL 插件、会话/检查点/Agent 状态存储、监控日志存储、长期记忆、推荐、问题规划器，或直接实现 Core `AskDataService` 接管完整流程。覆盖后仍应保留 Core 的项目/操作人上下文、SQL 安全、审计和恢复边界。

## 阅读路径

- [配置参考](configuration.md)：配置项、默认值和单机/集群限制。
- [运行时替换](runtime-replacement.md)：将业务选择的 LangChain4j Adapter 替换为其他运行时的边界。
- [Java 17 Adapter 规划](../java17-runtime-adapters.md)：Spring AI、Spring AI Alibaba 与 AgentScope 的模块、协议和验收边界。
- [Java 17 远程运行时](java17-remote-runtime.md)：不兼容运行时的远程接入协议和治理要求。
- [SQL 安全](sql-safety.md)：插件链、最终校验、限行与审计。
- [存储矩阵](storage-matrix.md)：默认内存、共享存储和容量规划。
- [插件 SDK](plugin-sdk.md)：模型、向量、SQL、Agent、存储和观测扩展，以及 Java 8 可编译示例。
- [存储运维](storage-operations.md)：迁移、故障恢复、容量、脱敏和验证步骤。
- [数据库设计](database-design.md)：逻辑数据模型与持久化扩展点。
- [流程图](flow-diagrams.md)：业务、技术、故障、恢复、监控与外部集成流程。
- [SQLBot 外部系统](sqlbot-external-integrations.md)：模型、MCP、嵌入、企业平台和联调契约。
- [接口重实现示例](transport-examples.md)：HTTP、RPC、消息和 MCP 的业务侧实现方式。
- [问数调用示例](question-examples.md)：单问题、显式多问题、复合问题和部分失败。
- [验证记录](verification.md)：已运行验证、需要运行的环境验证和当前风险。

## 业务覆盖接口

当前 Starter 为项目、成员/角色授权、全量连接器和方言、MDL 语义治理、训练资产、持久化存储、REST/MCP 管理接口、嵌入式对接、企业身份平台、仪表板和控制台提供 Core 契约、能力注册、存储、SQL、观测、发布 SPI、数据库设计和联调用例。

默认实现覆盖单机核心问数；完整产品体验由业务模块接入这些契约，并在真实接入时补充权限、审计、容量、企业 IAM/JustAuth、外部系统和回滚验证。
