# Brainstorm Summary

- Change: rework-ai-askdata-core-starter
- Date: 2026-07-27

## 已确认的约束

- Java 8、Spring Boot 2.7.18；Core 不泄露 Spring、LangChain4j、Ollama、LangGraph 或存储厂商类型。
- 先实现 Vue3 组件化前端控制台、前端 API SDK、Mock 服务和页面级交互原型；再实现后端 Starter；最后做前后端联调和功能矩阵闭环。
- 只发布一个 AskData Starter；默认聊天与向量厂商为 Ollama，LangChain4j 只是默认适配，不是固定运行时。业务可替换为 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 或自定义运行时，也可通过 SPI、能力插件或 Spring Bean 覆盖单项或完整服务/Agent。
- 若 Spring AI、Spring AI Alibaba 或 AgentScope Java 2.0+ 无法在 Java 8 / Spring Boot 2.7.18 中内嵌运行，文档必须写清官方基线、验证失败原因和限制；Java 8 Starter 不传递不兼容依赖，改由 Java 17 独立适配服务通过 HTTP/SSE、MCP、A2A、gRPC 或消息队列对接。
- SQLBot 开源、SQLBot v1 文档和 SQLBot 企业级功能优先；WrenAI 企业级 GenBI 能力也必须覆盖。重复功能采用 SQLBot 产品语义，吸收 WrenAI 底层增强。
- SQLBot 和 WrenAI 的功能必须全部列出并进入追踪矩阵。SQLBot 至少列出部署/命令行、工作空间/项目、成员权限、数据源、智能问数、仪表板、小助手、术语、训练检索、SQL 示例、Prompt、模型配置、模型接入、MCP Server、嵌入式对接、DataEase、外部 Agent 平台、登录认证、企业微信/钉钉/飞书、参数配置、外观设置、操作日志。WrenAI 至少列出自然语言问数、Text-to-SQL、Text-to-Chart、AI 摘要/报告、可嵌入 API、MDL 语义层、数据源连接器、模型接入、语义索引/描述、关系/问题推荐、instructions、SQL pair、feedback、evaluation、dry-plan/dry-run、metadata/validate/model-substitute/preview、dashboard 和遥测。
- SQLBot 与 WrenAI 支持的数据源/方言都必须进入能力矩阵；Doris 只是必须验证项之一，不能代表完整支持。
- 必须支持 SQLBot 文档里所有外部系统对接能力；当前至少包括模型接入/Ollama、MCP Server、嵌入式对接、DataEase、MaxKB、Dify、Coze、n8n、X-Pack 登录认证、平台对接、企业微信、钉钉、飞书、扫码登录、免登、组织用户同步、参数配置，覆盖 token/chat_id、工作空间/数据源列表、基础/高级/页面嵌入、宿主凭证映射、跨域、AES-Key、审计和联调测试。
- 项目是第一层分区，已认证操作人是第二层分区；所有对话与派生状态以 `projectId + operatorId` 归属。
- 单机所有状态和向量默认内存；集群必须改用共享存储。Agent 状态、长期记忆、会话和监控日志可按类别组合多种存储实现。
- 每次对话强制写入脱敏监控日志；项目管理员可查看本项目日志，跨项目访问需业务授权。
- SQL 插件类似 MyBatis 拦截器，但不能持有裸连接或绕过最终安全校验。
- 说明文档是详细、可验收的交付物，须覆盖架构、配置、Ollama、模型/存储/插件、安全、运维、迁移与排障，且示例可验证。
- 说明文档必须包含详细设计文档、数据库设计文档、技术流程图、业务流程图、测试用例文档和运行时替换文档；源码关键接口、SPI、安全/恢复/权限/联调逻辑必须有详细中文注释。
- 文档首页必须维护按版本的已实现功能清单，列出验证证据和限制；后续/未实现能力单列，不能混入已实现范围。
- SQLBot 开源项目和 v1 文档是整个多 change 批次的硬功能验收基线；WrenAI 企业级 GenBI 是补强基线；通过追踪矩阵逐项映射实现、文档和验证证据，未闭环不得宣称完整实现。
- `openspec/changes/rework-ai-askdata-core-starter/feature-list.md` 已作为 SQLBot 与 WrenAI 全功能清单事实源落盘，后续正式说明文档和验收矩阵必须以它为入口维护功能、归属、接口/配置、存储、文档、测试、状态和限制。

## 候选技术方案

1. 渐进式六边形架构（推荐）：新增无框架 Core 领域模块和 SPI；以兼容桥接复用现有自动配置；Starter 仅负责默认 Ollama/LangChain4j、内存/JDBC/Redis 和条件装配。风险最低，可持续二次开发。
2. 在现有自动配置模块内直接增加插件接口：改动较小，但 Core 继续暴露 Spring/LangChain4j 类型，无法满足业务完整覆盖和多存储要求。
3. 重新实现独立 Agent 运行时：边界最纯，但会丢失既有请求兼容性并扩大 Java 8、LangGraph 和迁移风险。

## 确认的技术方案

- 用户已回复“继续”，按设计确认处理。
- 采用方案 1：渐进式六边形架构，按 Core 契约、适配器、Starter 自动配置、兼容迁移四层实施。
- LangGraph Java 保持可选插件并先进行 Java 8 验证。
- Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 若无法内嵌 Java 8 / Spring Boot 2.7.18，则通过 Java 17 独立适配服务和 Remote Runtime SPI 对接。
- 正式 Design Doc 已创建：`docs/superpowers/specs/2026-07-27-rework-ai-askdata-core-starter-design.md`。

## 关键取舍与风险

- SQL 只重放可证明无副作用的模型/检索节点；执行中 SQL 状态不确定时不自动重放。
- 模型候选与兜底仅处理可分类基础设施故障，不能掩盖策略、预算或结构化输出错误。
- 内存实现仅限单机；集群启动期严格拒绝任何所需状态类别的本地存储。

## 测试策略

- Core 契约测试覆盖能力版本协商、覆盖优先级、项目/操作人隔离、多存储组合和 SQL 插件安全边界。
- 自动配置测试覆盖默认 Ollama 装配和业务覆盖；集成测试覆盖恢复、模型切换、多提问、日志访问和集群错误配置。

## Spec Patch

已补充 `askdata-query-engine` 的功能清单文档场景，要求 `feature-list.md` 成为设计期事实源并在正式说明文档中转换为可追踪矩阵。追加运行时兼容性场景：Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 若不兼容 Java 8 / Spring Boot 2.7.18，必须文档化官方基线、验证结果和 Java 17 独立适配服务对接方式。
