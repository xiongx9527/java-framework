# Task 15 实现报告：AskData Starter 文档、流程图与数据库设计

## 范围

- Base commit：`2744bd23e7fa076ac1f05796cb54f2708b892929`
- 分支：`feature/20260727/rework-ai-askdata-core-starter`
- 仅创建 `docs/askdata/starter/` 下的 Task 15 文档及本报告；未修改源码、POM、OpenSpec 状态/计划、功能清单或既有无关脏文件。

## 创建文件与覆盖内容

- `docs/askdata/starter/index.md`：Starter 已实现功能清单、最小 Ollama 配置、默认装配与业务覆盖边界。清单涵盖核心问数、项目/操作人上下文、模型路由、默认 Ollama、故障兜底、状态/监控日志、SQL 插件链、多提问、记忆、推荐和依赖装配。
- `docs/askdata/starter/configuration.md`：完整 `fw.plus.ai.askdata` 配置参考、默认值、单机/集群限制、Ollama 聊天/向量默认模型、SQL 安全、状态容量、监控日志和向量存储说明。
- `docs/askdata/starter/runtime-replacement.md`：LangChain4j、Spring AI、Spring AI Alibaba、AgentScope Java 与业务运行时的 SPI 边界、替换步骤、Java 8 主进程限制和回退策略。
- `docs/askdata/starter/java17-remote-runtime.md`：官方兼容性核验入口、Java 17 远程部署、能力注册、HTTP/SSE、MCP、A2A、gRPC、消息队列、认证、上下文 payload、错误映射、审计和回滚。
- `docs/askdata/starter/sql-safety.md`：SQL 插件链、具名参数、租户/数据范围、最终校验、只读、物理限行、超时、审计和脱敏要求。
- `docs/askdata/starter/storage-matrix.md`：内存默认、集群拒绝内存、JDBC/Redis/业务自定义共享存储、向量内存模式、Agent 状态、监控日志、容量与迁移建议。
- `docs/askdata/starter/database-design.md`：项目、成员、数据源、元数据、MDL、训练资产、会话、记忆、推荐、SQL 审计、监控日志、Agent 状态、模型配置、MCP/API、企业集成和插件注册表的逻辑表设计。
- `docs/askdata/starter/flow-diagrams.md`：业务、技术、模型 fallback、Java 17 remote runtime、SQL safety、recovery、monitoring 和 external integrations 的 Mermaid 图。

## 验证

执行：

```bash
rg -n "T[B]D|T[O]DO|待[补]|占[位]" docs/askdata/starter
```

结果：无输出，所有 Starter 文档均未出现禁止的未完成标记。

附加执行：

```bash
git diff --check -- docs/askdata/starter
```

结果：无输出，新增 Markdown 不含空白错误。

## 风险与边界

- 文档已明确区分当前默认实现与业务覆盖/后续能力；项目成员治理、持久化适配、全量连接器、外部 API/MCP 管理、企业集成和控制台不被描述为已实现。
- Spring AI、Spring AI Alibaba、AgentScope Java 的具体兼容性会随官方版本变化。文档仅引用官方入口并要求接入时完成 JDK、Spring Boot、BOM、最小编译和启动验证；不满足 Java 8/Spring Boot 2.7.18 基线时使用 Java 17 Remote Runtime。
- Mermaid 图为 Markdown 源码，渲染效果依赖文档浏览器支持 Mermaid；图中流程与当前 Core/Starter 的安全、路由和恢复边界一致。
- 数据库设计是持久化 SPI 的逻辑蓝图；当前默认实现仍为单机内存，集群落地前必须补齐共享存储、租约、迁移、备份与压测验证。
