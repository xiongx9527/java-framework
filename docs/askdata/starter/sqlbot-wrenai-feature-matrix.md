# SQLBot / WrenAI 功能矩阵

本矩阵逐项追踪 `feature-list.md` 的 SQLBot 与 WrenAI 功能。产品语义以 **SQLBot 优先**：重复项保留 SQLBot 的项目、权限、MCP、嵌入和企业集成边界，WrenAI 只作为语义层、评测、生成或调度增强。本矩阵的“设计覆盖”仅表示已有设计文档说明了契约和边界，绝不表示业务功能已交付。

状态含义：**已实现**为当前 Core/Starter 已有且在文档中列为实现；**部分实现**为已有基础 SPI 或单机默认实现；**设计覆盖**为设计和接口边界已写明；**后续实现**为尚无实现；**外部集成预留**为需业务模块/控制台接入的外部能力。

约定：`FL` 是 [功能清单](../../../openspec/changes/rework-ai-askdata-core-starter/feature-list.md)，`IDX`、`CFG`、`SQL`、`STORE`、`DB`、`RT`、`J17`、`FLOW` 分别是本目录的 index、configuration、sql-safety、storage-matrix、database-design、runtime-replacement、java17-remote-runtime、flow-diagrams。模块列的“业务模块”不是已存在模块；它表示应由后续 change 实现。

## SQLBot

| product | category | feature | source | priority | duplicatePolicy | change | module | apiOrConfig | storage | docs | tests | status | limitation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| SQLBot | 基础与部署 | 产品介绍与工作原理 | FL §1.1 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | 文档 | AskData Starter 概述 | 无 | IDX | DOC-01 | 设计覆盖 | 仅 Starter 说明，不是完整产品控制台。 |
| SQLBot | 基础与部署 | 安装部署 | FL §1.1 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | 业务模块 | Starter 装配/部署清单 | 无 | IDX, CFG | DOC-02 | 设计覆盖 | 未交付 SQLBot 安装器、默认账号或控制台。 |
| SQLBot | 基础与部署 | 命令行工具 | FL §1.1 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | 业务模块 | 管理入口待定义 | 无 | IDX | DOC-03 | 后续实现 | 当前没有等价 CLI。 |
| SQLBot | 基础与部署 | 参数配置 | FL §1.1 | SQLBot 优先 | 企业参数不与 Starter 配置混同 | rework-ai-askdata-core-starter | enterprise | 企业配置命名空间待定义 | askdata_plugin_registry | CFG, DB | ENT-01 | 后续实现 | 仅已有 Starter 默认配置，无 X-Pack 参数管理。 |
| SQLBot | 基础与部署 | 外观设置 | FL §1.1 | SQLBot 优先 | SQLBot 品牌语义优先 | rework-ai-askdata-core-starter | console | 主题/品牌配置待定义 | askdata_project | DB | FE-01 | 后续实现 | 未实现控制台或品牌素材。 |
| SQLBot | 基础与部署 | 操作日志 | FL §1.1 | SQLBot 优先 | 管理日志独立于监控日志 | rework-ai-askdata-core-starter | enterprise | 管理审计 API 待定义 | askdata_sql_audit | SQL, DB | AUD-01 | 设计覆盖 | 当前仅有范围隔离的内存监控日志，不是管理员操作日志。 |
| SQLBot | 项目、组织与权限 | 工作空间/项目 | FL §1.2 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | Core | AskDataProjectContext | 内存/SPI；askdata_project | IDX, DB | CTX-01 | 已实现 | Core 强制 projectId/operatorId；项目实体和治理持久化未实现。 |
| SQLBot | 项目、组织与权限 | 成员管理 | FL §1.2 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | governance/console | 成员角色 API 待定义 | askdata_project_member | DB | AUTH-01 | 后续实现 | 宿主应用负责真实成员和角色判定。 |
| SQLBot | 项目、组织与权限 | 权限配置 | FL §1.2 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | governance | 对象授权解析 SPI | askdata_project_member, askdata_metadata | SQL, DB | AUTH-02 | 设计覆盖 | SQL 最终校验已实现边界；行列权限解析尚未提供。 |
| SQLBot | 项目、组织与权限 | 组织用户同步 | FL §1.2 | SQLBot 优先 | 与企业平台同步合并 | rework-ai-askdata-core-starter | enterprise | 同步任务/API 待定义 | askdata_enterprise_integration | DB | ENT-02 | 外部集成预留 | 需企业微信、钉钉、飞书业务凭据与映射。 |
| SQLBot | 数据源与方言 | 数据源概览 | FL §1.3 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | console/governance | 数据源状态 API 待定义 | askdata_datasource | DB | CONN-01 | 后续实现 | Starter 不管理业务数据源。 |
| SQLBot | 数据源与方言 | Excel/CSV | FL §1.3 | SQLBot 优先 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 文件连接器 SPI 待定义 | askdata_datasource, askdata_metadata | DB | CONN-02 | 后续实现 | 未定义文件上传、解析或权限策略实现。 |
| SQLBot | 数据源与方言 | MySQL | FL §1.3 | SQLBot 优先 | 与 WrenAI 连接器合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource, askdata_metadata | SQL, DB | CONN-03 | 设计覆盖 | JDBC 只读执行器边界已实现；连接器、元数据与授权未实现。 |
| SQLBot | 数据源与方言 | Oracle | FL §1.3 | SQLBot 优先 | 与 WrenAI 连接器合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource, askdata_metadata | SQL, DB | CONN-04 | 设计覆盖 | 方言验证和连接器未实现。 |
| SQLBot | 数据源与方言 | PostgreSQL | FL §1.3 | SQLBot 优先 | 与 WrenAI 连接器合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource, askdata_metadata | SQL, DB | CONN-05 | 设计覆盖 | 方言验证和连接器未实现。 |
| SQLBot | 数据源与方言 | SQL Server | FL §1.3 | SQLBot 优先 | 与 WrenAI 连接器合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource, askdata_metadata | SQL, DB | CONN-06 | 设计覆盖 | 方言验证和连接器未实现。 |
| SQLBot | 数据源与方言 | ClickHouse | FL §1.3 | SQLBot 优先 | 与 WrenAI 连接器合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource, askdata_metadata | SQL, DB | CONN-07 | 设计覆盖 | 方言验证和连接器未实现。 |
| SQLBot | 数据源与方言 | 达梦 | FL §1.3 | SQLBot 优先 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource, askdata_metadata | SQL, DB | CONN-08 | 后续实现 | 未实现驱动、方言和验证。 |
| SQLBot | 数据源与方言 | Apache Doris | FL §1.3 | SQLBot 优先 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource, askdata_metadata | SQL, DB | CONN-09 | 设计覆盖 | 只是必须验证项，不表示已全量支持。 |
| SQLBot | 数据源与方言 | AWS Redshift | FL §1.3 | SQLBot 优先 | 与 WrenAI Redshift 合并 | rework-ai-askdata-core-starter | governance/query-engine | 连接器 SPI 待定义 | askdata_datasource | DB | CONN-10 | 后续实现 | 未实现。 |
| SQLBot | 数据源与方言 | Elasticsearch | FL §1.3 | SQLBot 优先 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | SQL/DSL 安全边界待定义 | askdata_datasource, askdata_metadata | SQL, DB | CONN-11 | 后续实现 | 尚未定义 DSL 执行和安全实现。 |
| SQLBot | 数据源与方言 | Kingbase | FL §1.3 | SQLBot 优先 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-12 | 后续实现 | 未实现。 |
| SQLBot | 数据源与方言 | StarRocks | FL §1.3 | SQLBot 优先 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-13 | 后续实现 | 未实现。 |
| SQLBot | 数据源与方言 | Apache Hive | FL §1.3 | SQLBot 优先 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-14 | 后续实现 | 未实现。 |
| SQLBot | 智能问数与训练 | 智能问数 | FL §1.4 | SQLBot 优先 | WrenAI Text-to-SQL 增强 | rework-ai-askdata-core-starter | Core/Starter | AskDataService | 内存/SPI | IDX, FLOW | CORE-01 | 已实现 | 默认单项目流程；宿主承担真实数据权限。 |
| SQLBot | 智能问数与训练 | 多轮上下文 | FL §1.4 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | Core | 会话/压缩/检查点 SPI | 内存会话/检查点 | CFG, STORE | STATE-01 | 部分实现 | 默认内存仅单机，压缩与共享持久化需业务实现。 |
| SQLBot | 智能问数与训练 | 多提问 | FL §1.4 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | Core | 问题规划/聚合 | 子问题审计 | IDX, FLOW | CORE-02 | 已实现 | 恢复令牌的多提问请求受控拒绝。 |
| SQLBot | 智能问数与训练 | SQL 安全 | FL §1.4 | SQLBot 优先 | WrenAI dry-plan/dry-run 增强 | rework-ai-askdata-core-starter | Core/Starter | SQL 校验器、插件、只读执行器 | 内存审计/SPI | SQL, FLOW | SQL-01 | 已实现 | 对象授权和生产审计落库由业务提供。 |
| SQLBot | 智能问数与训练 | 图表/图片渲染 | FL §1.4 | SQLBot 优先 | 与 WrenAI Text-to-Chart 合并 | rework-ai-askdata-core-starter | api-mcp/console | 图表 DTO/API 待定义 | 资源存储待定义 | FLOW | FE-02 | 后续实现 | 当前只返回受控问数结果，不渲染图表/图片。 |
| SQLBot | 智能问数与训练 | 仪表板 | FL §1.4 | SQLBot 优先 | WrenAI 生成/调度增强 | rework-ai-askdata-core-starter | api-mcp/console | 仪表板 API 待定义 | 仪表板存储待定义 | DB | FE-03 | 后续实现 | 未实现。 |
| SQLBot | 智能问数与训练 | 小助手应用 | FL §1.4 | SQLBot 优先 | 嵌入能力归 SQLBot | rework-ai-askdata-core-starter | console/api-mcp | 应用/嵌入 SPI 待定义 | askdata_mcp_api | FLOW | FE-04 | 外部集成预留 | 未交付 Vue3 组件或发布 API。 |
| SQLBot | 智能问数与训练 | 术语配置 | FL §1.4 | SQLBot 优先 | WrenAI instructions/MDL 合并 | rework-ai-askdata-core-starter | training | 术语资产 SPI 待定义 | askdata_training_asset | DB | TRAIN-01 | 设计覆盖 | 仅有逻辑存储蓝图。 |
| SQLBot | 智能问数与训练 | 训练检索 | FL §1.4 | SQLBot 优先 | WrenAI 语义索引增强 | rework-ai-askdata-core-starter | training | 向量存储 SPI | 内存向量/SPI | CFG, STORE | TRAIN-02 | 部分实现 | 默认内存仅开发使用，持久化向量库未实现。 |
| SQLBot | 智能问数与训练 | SQL 示例 | FL §1.4 | SQLBot 优先 | 与 WrenAI SQL pair 合并 | rework-ai-askdata-core-starter | training | 训练资产 SPI 待定义 | askdata_training_asset | DB | TRAIN-03 | 后续实现 | 未实现样例管理与部署。 |
| SQLBot | 智能问数与训练 | Prompt/自定义提示词 | FL §1.4 | SQLBot 优先 | 与 WrenAI instructions 合并 | rework-ai-askdata-core-starter | enterprise/training | 项目/数据源 Prompt 配置待定义 | askdata_training_asset | DB | TRAIN-04 | 后续实现 | 不保存或暴露原始敏感提示词。 |
| SQLBot | 智能问数与训练 | 长期记忆 | FL §1.4 | SQLBot 优先 | WrenAI 历史问题增强 | rework-ai-askdata-core-starter | Core | LongTermMemory SPI | 内存/SPI；askdata_memory | IDX, STORE, DB | MEM-01 | 部分实现 | 单机内存；必须按项目和操作人隔离。 |
| SQLBot | 智能问数与训练 | 推荐 | FL §1.4 | SQLBot 优先 | WrenAI recommendation 算法增强 | rework-ai-askdata-core-starter | Core | Recommendation SPI | 内存/SPI；askdata_recommendation | IDX, STORE, DB | REC-01 | 部分实现 | 单机内存且不能跨人共享画像。 |
| SQLBot | 模型与运行时 | AI 模型配置 | FL §1.5 | SQLBot 优先 | WrenAI 模型路由增强 | rework-ai-askdata-core-starter | Core/Starter | model.* 配置/能力 key | 路由诊断/askdata_model_config | CFG, DB | MODEL-01 | 已实现 | API Key 托管和企业模型管理未实现。 |
| SQLBot | 模型与运行时 | 模型接入 | FL §1.5 | SQLBot 优先 | WrenAI 厂商列表补充 | rework-ai-askdata-core-starter | Core/api-mcp | CapabilityProvider | 逻辑模型 key | RT | MODEL-02 | 设计覆盖 | 当前默认仅 LangChain4j/Ollama；其余需适配。 |
| SQLBot | 模型与运行时 | Ollama 部署模型接入 | FL §1.5 | SQLBot 优先 | 与 WrenAI Ollama 共用 | rework-ai-askdata-core-starter | Starter | model.default-chat-model/embedding | 路由诊断 | IDX, CFG | MODEL-03 | 已实现 | 默认 `qwen3:32b`/`bge-m3` 可被业务覆盖。 |
| SQLBot | 模型与运行时 | LangChain4j 默认适配 | FL §1.5 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | Starter | 自动配置/Bean 覆盖 | 路由诊断 | IDX, RT | AUTO-01 | 已实现 | 不是固定或唯一运行时。 |
| SQLBot | 模型与运行时 | Spring AI 替换 | FL §1.5 | SQLBot 优先 | 运行时替换共用 | rework-ai-askdata-core-starter | Core | Runtime SPI | 远程运行时状态/SPI | RT, J17 | RT-01 | 设计覆盖 | 需先验证 Java 8/Boot 2.7 兼容性。 |
| SQLBot | 模型与运行时 | Spring AI Alibaba 替换 | FL §1.5 | SQLBot 优先 | 运行时替换共用 | rework-ai-askdata-core-starter | Core | Runtime SPI/MCP/A2A | 远程运行时状态/SPI | RT, J17 | RT-02 | 设计覆盖 | 不兼容时只能远程部署，未实现适配器。 |
| SQLBot | 模型与运行时 | AgentScope Java 2.0+ 替换 | FL §1.5 | SQLBot 优先 | 运行时替换共用 | rework-ai-askdata-core-starter | Core | Remote Runtime SPI | agent_state/SPI | RT, J17 | RT-03 | 设计覆盖 | 未验证目标版本，远程适配优先。 |
| SQLBot | 模型与运行时 | Java 17 独立适配服务 | FL §1.5 | SQLBot 优先 | 不重复 | rework-ai-askdata-core-starter | remote-runtime | HTTP/SSE/MCP/A2A/gRPC/MQ | agent_state/SPI | J17, FLOW | RT-04 | 外部集成预留 | 仅定义协议、安全、审计和回滚要求。 |
| SQLBot | 外部系统对接 | MCP Server | FL §1.6 | SQLBot 优先 | WrenAI API 仅补 SDK | rework-ai-askdata-core-starter | api-mcp | MCP 发布 SPI | askdata_mcp_api | FLOW | MCP-01 | 外部集成预留 | 未实现 MCP 服务端。 |
| SQLBot | 外部系统对接 | `mcp_start` | FL §1.6 | SQLBot 优先 | SQLBot MCP 语义优先 | rework-ai-askdata-core-starter | api-mcp | mcp_start 待定义 | askdata_mcp_api, conversation | FLOW | MCP-02 | 外部集成预留 | 未提供 access_token/chat_id 接口。 |
| SQLBot | 外部系统对接 | `mcp_question` | FL §1.6 | SQLBot 优先 | SQLBot MCP 语义优先 | rework-ai-askdata-core-starter | api-mcp | mcp_question 待定义 | conversation/sql_audit | FLOW | MCP-03 | 外部集成预留 | 结果、图表和图片 API 未实现。 |
| SQLBot | 外部系统对接 | `mcp_ws_list` | FL §1.6 | SQLBot 优先 | SQLBot MCP 语义优先 | rework-ai-askdata-core-starter | api-mcp | mcp_ws_list 待定义 | askdata_project | DB | MCP-04 | 外部集成预留 | 未实现权限过滤接口。 |
| SQLBot | 外部系统对接 | `mcp_datasource_list` | FL §1.6 | SQLBot 优先 | SQLBot MCP 语义优先 | rework-ai-askdata-core-starter | api-mcp | mcp_datasource_list 待定义 | askdata_datasource | DB | MCP-05 | 外部集成预留 | 未实现权限过滤接口。 |
| SQLBot | 外部系统对接 | SSE/Streamable HTTP | FL §1.6 | SQLBot 优先 | 与 Remote Runtime 协议共用 | rework-ai-askdata-core-starter | api-mcp | MCP transport 待定义 | askdata_mcp_api | J17 | MCP-06 | 外部集成预留 | 仅远程运行时文档定义传输约束。 |
| SQLBot | 外部系统对接 | 嵌入式对接 | FL §1.6 | SQLBot 优先 | WrenAI API 补 SDK | rework-ai-askdata-core-starter | api-mcp/console | 嵌入 SDK 待定义 | askdata_mcp_api | FLOW | FE-05 | 外部集成预留 | 未交付 SDK 或组件。 |
| SQLBot | 外部系统对接 | 基础应用 | FL §1.6 | SQLBot 优先 | SQLBot 应用语义优先 | rework-ai-askdata-core-starter | api-mcp/console | online 参数待定义 | askdata_mcp_api | FLOW | FE-06 | 后续实现 | 游客/员工授权和应用管理未实现。 |
| SQLBot | 外部系统对接 | 高级应用 | FL §1.6 | SQLBot 优先 | SQLBot 应用语义优先 | rework-ai-askdata-core-starter | api-mcp/console | 宿主 API 映射待定义 | askdata_mcp_api | FLOW | FE-07 | 外部集成预留 | 后端不得保存宿主凭据；未实现。 |
| SQLBot | 外部系统对接 | 浮窗嵌入 | FL §1.6 | SQLBot 优先 | SQLBot 嵌入优先 | rework-ai-askdata-core-starter | console | Vue3 浮窗组件待定义 | 无 | FLOW | FE-08 | 后续实现 | 未实现前端。 |
| SQLBot | 外部系统对接 | 全屏/区域嵌入 | FL §1.6 | SQLBot 优先 | SQLBot 嵌入优先 | rework-ai-askdata-core-starter | console | Vue3 嵌入组件待定义 | 无 | FLOW | FE-09 | 后续实现 | 未实现前端。 |
| SQLBot | 外部系统对接 | 页面嵌入 token | FL §1.6 | SQLBot 优先 | SQLBot 嵌入优先 | rework-ai-askdata-core-starter | api-mcp | 短期 token API 待定义 | askdata_mcp_api | DB | FE-10 | 外部集成预留 | 未实现 appId/account/appSecret 规则。 |
| SQLBot | 外部系统对接 | 宿主凭证映射 | FL §1.6 | SQLBot 优先 | SQLBot 嵌入优先 | rework-ai-askdata-core-starter | api-mcp/console | Header/Cookie 映射待定义 | askdata_mcp_api | FLOW | FE-11 | 外部集成预留 | 需脱敏、最小权限且不得持久化宿主密钥。 |
| SQLBot | 外部系统对接 | DataEase 接入 | FL §1.6 | SQLBot 优先 | SQLBot 硬基线 | rework-ai-askdata-core-starter | api-mcp/console | 高级应用集成待定义 | askdata_mcp_api | FLOW | DE-01 | 外部集成预留 | 未实现 DataEase 联调。 |
| SQLBot | 外部系统对接 | `/de2api/sqlbot/datasource` | FL §1.6 | SQLBot 优先 | DataEase 固定接口 | rework-ai-askdata-core-starter | api-mcp | DataEase datasource API 待定义 | askdata_datasource | FLOW | DE-02 | 外部集成预留 | 未实现权限 SQL/字段描述响应。 |
| SQLBot | 外部系统对接 | `x-de-token` | FL §1.6 | SQLBot 优先 | DataEase 固定 Header | rework-ai-askdata-core-starter | api-mcp | Token 映射待定义 | askdata_mcp_api | FLOW | DE-03 | 外部集成预留 | 未实现 `user.token`/`de_v2_user.token` 映射。 |
| SQLBot | 外部系统对接 | AES-Key | FL §1.6 | SQLBot 优先 | DataEase 敏感数据增强 | rework-ai-askdata-core-starter | api-mcp/enterprise | 密钥引用/轮换待定义 | 密钥服务引用 | DB | DE-04 | 外部集成预留 | 禁止明文保存 AES-Key。 |
| SQLBot | 外部系统对接 | MaxKB 对接 | FL §1.6 | SQLBot 优先 | MCP/嵌入/API 通用 | rework-ai-askdata-core-starter | api-mcp | 调用方注册待定义 | askdata_mcp_api | FLOW | EXT-01 | 外部集成预留 | 依赖 MCP 或嵌入发布能力。 |
| SQLBot | 外部系统对接 | Dify 对接 | FL §1.6 | SQLBot 优先 | MCP/嵌入/API 通用 | rework-ai-askdata-core-starter | api-mcp | 调用方注册待定义 | askdata_mcp_api | FLOW | EXT-02 | 外部集成预留 | Dify 联调未实现。 |
| SQLBot | 外部系统对接 | Coze 对接 | FL §1.6 | SQLBot 优先 | MCP/嵌入/API 通用 | rework-ai-askdata-core-starter | api-mcp | 调用方注册待定义 | askdata_mcp_api | FLOW | EXT-03 | 外部集成预留 | Coze 联调未实现。 |
| SQLBot | 外部系统对接 | n8n 对接 | FL §1.6 | SQLBot 优先 | MCP/嵌入/API 通用 | rework-ai-askdata-core-starter | api-mcp | 调用方注册待定义 | askdata_mcp_api | FLOW | EXT-04 | 外部集成预留 | n8n 工作流节点未实现。 |
| SQLBot | X-Pack/企业能力 | 登录认证 | FL §1.7 | SQLBot 优先 | 不与宿主认证争夺所有权 | rework-ai-askdata-core-starter | enterprise | SSO/登录 API 待定义 | askdata_enterprise_integration | DB | ENT-03 | 后续实现 | 当前由宿主应用认证。 |
| SQLBot | X-Pack/企业能力 | 平台对接 | FL §1.7 | SQLBot 优先 | 企业微信/钉钉/飞书统一接入 | rework-ai-askdata-core-starter | enterprise/console | 业务 IAM/JustAuth 映射待定义 | askdata_enterprise_integration | DB, FLOW | ENT-04 | 外部集成预留 | AskData 不自研扫码、免登或 OAuth，只接收已认证 `projectId/operatorId`。 |
| SQLBot | X-Pack/企业能力 | 企业微信 | FL §1.7 | SQLBot 优先 | 平台对接子项 | rework-ai-askdata-core-starter | enterprise | JustAuth 企业微信登录授权/组织同步 | askdata_enterprise_integration | DB, FLOW | ENT-05 | 外部集成预留 | 企业 ID、应用密钥与可信域名由业务 IAM/JustAuth 管理并审计。 |
| SQLBot | X-Pack/企业能力 | 钉钉 | FL §1.7 | SQLBot 优先 | 平台对接子项 | rework-ai-askdata-core-starter | enterprise | JustAuth 钉钉登录授权/免登回调 | askdata_enterprise_integration | DB, FLOW | ENT-06 | 外部集成预留 | AgentId/AppKey/AppSecret 由业务 IAM/JustAuth 托管，不得明文保存。 |
| SQLBot | X-Pack/企业能力 | 飞书 | FL §1.7 | SQLBot 优先 | 平台对接子项 | rework-ai-askdata-core-starter | enterprise | JustAuth 飞书网页应用/重定向 | askdata_enterprise_integration | DB, FLOW | ENT-07 | 外部集成预留 | App ID/App Secret 由业务 IAM/JustAuth 托管并审计。 |
| SQLBot | X-Pack/企业能力 | 扫码登录 | FL §1.7 | SQLBot 优先 | 企业平台统一体验 | rework-ai-askdata-core-starter | enterprise/console | JustAuth 扫码回调后映射项目成员 | askdata_enterprise_integration | DB | ENT-08 | 后续实现 | AskData 不处理扫码协议，只校验业务传入的权限映射。 |
| SQLBot | X-Pack/企业能力 | 免登 | FL §1.7 | SQLBot 优先 | 企业平台统一体验 | rework-ai-askdata-core-starter | enterprise/console | JustAuth 免登后映射 `projectId/operatorId` | askdata_enterprise_integration | DB | ENT-09 | 后续实现 | 不能缺失 projectId/operatorId 归属。 |
| SQLBot | X-Pack/企业能力 | 组织用户同步 | FL §1.7 | SQLBot 优先 | 与 §1.2 同一能力 | rework-ai-askdata-core-starter | enterprise/governance | 业务 IAM/JustAuth 同步后写入项目成员 | askdata_enterprise_integration, askdata_project_member | DB | ENT-10 | 外部集成预留 | AskData 只使用同步后的成员和权限结果。 |
| SQLBot | X-Pack/企业能力 | 参数配置 | FL §1.7 | SQLBot 优先 | 与 §1.1 企业配置合并 | rework-ai-askdata-core-starter | enterprise | 企业配置待定义 | askdata_plugin_registry | CFG, DB | ENT-11 | 后续实现 | Starter 配置不等于企业参数中心。 |
| SQLBot | X-Pack/企业能力 | 外观设置 | FL §1.7 | SQLBot 优先 | 与 §1.1 外观合并 | rework-ai-askdata-core-starter | enterprise/console | 主题配置待定义 | askdata_project | DB | ENT-12 | 后续实现 | 未实现。 |
| SQLBot | X-Pack/企业能力 | 操作日志 | FL §1.7 | SQLBot 优先 | 与 §1.1 管理日志合并 | rework-ai-askdata-core-starter | enterprise/observability | 管理审计 API 待定义 | askdata_sql_audit | SQL, DB | ENT-13 | 设计覆盖 | 监控日志不能替代企业管理员操作审计。 |

## WrenAI

| product | category | feature | source | priority | duplicatePolicy | change | module | apiOrConfig | storage | docs | tests | status | limitation |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| WrenAI | GenBI 问数体验 | 自然语言问数 | FL §2.1 | WrenAI 增强 | SQLBot 问数语义优先 | rework-ai-askdata-core-starter | Core/Starter | AskDataService | 内存/SPI | IDX, FLOW | CORE-01 | 部分实现 | 无 WrenAI 专项体验或多语言策略实现。 |
| WrenAI | GenBI 问数体验 | Text-to-SQL | FL §2.1 | WrenAI 增强 | SQLBot 问数优先；MDL/评测增强 | rework-ai-askdata-core-starter | Core/training | SQL 命令 SPI | SQL 审计/SPI | SQL, FLOW | SQL-01 | 部分实现 | 已有受控 SQL 边界，不含 WrenAI MDL 生成。 |
| WrenAI | GenBI 问数体验 | Text-to-Chart | FL §2.1 | WrenAI 增强 | SQLBot 图表入口优先 | rework-ai-askdata-core-starter | api-mcp/console | 图表 DTO 待定义 | 资源存储待定义 | FLOW | FE-02 | 后续实现 | 未实现。 |
| WrenAI | GenBI 问数体验 | AI 摘要 | FL §2.1 | WrenAI 增强 | SQLBot 结果解释优先 | rework-ai-askdata-core-starter | Core/console | 摘要 DTO 待定义 | conversation | FLOW | GENBI-01 | 后续实现 | 未实现。 |
| WrenAI | GenBI 问数体验 | AI 报告 | FL §2.1 | WrenAI 增强 | SQLBot 仪表板入口优先 | rework-ai-askdata-core-starter | console/api-mcp | 报告 API 待定义 | 报告存储待定义 | DB | GENBI-02 | 后续实现 | 未实现。 |
| WrenAI | GenBI 问数体验 | GenBI Insights | FL §2.1 | WrenAI 增强 | SQLBot 仪表板入口优先 | rework-ai-askdata-core-starter | console/training | 洞察 API 待定义 | 训练资产/仪表板存储 | DB | GENBI-03 | 后续实现 | 未实现。 |
| WrenAI | GenBI 问数体验 | 多语言问数 | FL §2.1 | WrenAI 增强 | SQLBot Prompt 优先 | rework-ai-askdata-core-starter | Core/training | 模型/Prompt 策略待定义 | askdata_training_asset | RT | GENBI-04 | 后续实现 | 未实现语言识别和策略。 |
| WrenAI | GenBI 问数体验 | 可嵌入 API | FL §2.1 | WrenAI 增强 | SQLBot MCP/嵌入优先 | rework-ai-askdata-core-starter | api-mcp | SDK/API 待定义 | askdata_mcp_api | FLOW | MCP-01 | 外部集成预留 | 发布 API 未实现。 |
| WrenAI | MDL 语义层 | MDL manifest | FL §2.2 | WrenAI 增强 | SQLBot 元数据权限优先 | rework-ai-askdata-core-starter | training/governance | MDL SPI 待定义 | askdata_mdl | DB | MDL-01 | 设计覆盖 | 仅逻辑表设计，无 manifest 管理实现。 |
| WrenAI | MDL 语义层 | 模型 | FL §2.2 | WrenAI 增强 | SQLBot 术语/元数据优先 | rework-ai-askdata-core-starter | training | 业务模型 SPI 待定义 | askdata_mdl | DB | MDL-02 | 后续实现 | 未实现。 |
| WrenAI | MDL 语义层 | 列 | FL §2.2 | WrenAI 增强 | 权限过滤先于语义 | rework-ai-askdata-core-starter | training/governance | 列元数据 SPI 待定义 | askdata_metadata, askdata_mdl | DB | MDL-03 | 后续实现 | 未实现元数据治理。 |
| WrenAI | MDL 语义层 | 嵌套列 | FL §2.2 | WrenAI 增强 | MDL 专项增强 | rework-ai-askdata-core-starter | training | 嵌套列定义待定义 | askdata_mdl | DB | MDL-04 | 后续实现 | 未实现。 |
| WrenAI | MDL 语义层 | 关系 | FL §2.2 | WrenAI 增强 | SQLBot 权限先于 join 建议 | rework-ai-askdata-core-starter | training | 关系/join SPI 待定义 | askdata_mdl | DB | MDL-05 | 后续实现 | 未实现。 |
| WrenAI | MDL 语义层 | 指标 | FL §2.2 | WrenAI 增强 | SQLBot 术语/指标优先 | rework-ai-askdata-core-starter | training | 指标定义待定义 | askdata_mdl | DB | MDL-06 | 后续实现 | 未实现。 |
| WrenAI | MDL 语义层 | 视图 | FL §2.2 | WrenAI 增强 | 需经过 SQL 安全 | rework-ai-askdata-core-starter | training/governance | 视图/preview/dry-plan 待定义 | askdata_mdl | SQL, DB | MDL-07 | 后续实现 | 未实现，不能绕过最终校验。 |
| WrenAI | MDL 语义层 | deploy log | FL §2.2 | WrenAI 增强 | 独立于管理员操作日志 | rework-ai-askdata-core-starter | training/observability | 语义部署日志待定义 | askdata_mdl | DB | MDL-08 | 后续实现 | 未实现。 |
| WrenAI | MDL 语义层 | 语义版本 | FL §2.2 | WrenAI 增强 | SQLBot 恢复/审计兼容 | rework-ai-askdata-core-starter | training | semanticVersion 契约 | askdata_mdl | DB, FLOW | MDL-09 | 设计覆盖 | 恢复契约要求比对版本；MDL 版本实现未提供。 |
| WrenAI | 数据源与连接器 | Athena/Trino | FL §2.3 | WrenAI 增强 | 与 Trino 合并 | rework-ai-askdata-core-starter | governance/query-engine | 连接器 SPI 待定义 | askdata_datasource | DB | CONN-15 | 后续实现 | 未实现。 |
| WrenAI | 数据源与连接器 | Redshift | FL §2.3 | WrenAI 增强 | 与 SQLBot AWS Redshift 合并 | rework-ai-askdata-core-starter | governance/query-engine | 连接器 SPI 待定义 | askdata_datasource | DB | CONN-10 | 后续实现 | 未实现。 |
| WrenAI | 数据源与连接器 | BigQuery | FL §2.3 | WrenAI 增强 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 连接器 SPI 待定义 | askdata_datasource | DB | CONN-16 | 后续实现 | 未实现。 |
| WrenAI | 数据源与连接器 | DuckDB | FL §2.3 | WrenAI 增强 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 连接器 SPI 待定义 | askdata_datasource | DB | CONN-17 | 后续实现 | 未实现。 |
| WrenAI | 数据源与连接器 | PostgreSQL | FL §2.3 | WrenAI 增强 | 与 SQLBot PostgreSQL 合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-05 | 设计覆盖 | 连接器未实现。 |
| WrenAI | 数据源与连接器 | MySQL | FL §2.3 | WrenAI 增强 | 与 SQLBot MySQL 合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-03 | 设计覆盖 | 连接器未实现。 |
| WrenAI | 数据源与连接器 | SQL Server | FL §2.3 | WrenAI 增强 | 与 SQLBot SQL Server 合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-06 | 设计覆盖 | 连接器未实现。 |
| WrenAI | 数据源与连接器 | ClickHouse | FL §2.3 | WrenAI 增强 | 与 SQLBot ClickHouse 合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-07 | 设计覆盖 | 连接器未实现。 |
| WrenAI | 数据源与连接器 | Oracle | FL §2.3 | WrenAI 增强 | 与 SQLBot Oracle 合并 | rework-ai-askdata-core-starter | governance/query-engine | 方言/只读执行 SPI | askdata_datasource | SQL, DB | CONN-04 | 设计覆盖 | 连接器未实现。 |
| WrenAI | 数据源与连接器 | Trino | FL §2.3 | WrenAI 增强 | 与 Athena/Trino 合并 | rework-ai-askdata-core-starter | governance/query-engine | 连接器 SPI 待定义 | askdata_datasource | DB | CONN-15 | 后续实现 | 未实现。 |
| WrenAI | 数据源与连接器 | Snowflake | FL §2.3 | WrenAI 增强 | 并集必选 | rework-ai-askdata-core-starter | governance/query-engine | 连接器 SPI 待定义 | askdata_datasource | DB | CONN-18 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | OpenAI | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-04 | 后续实现 | 可由业务适配，当前默认不是 OpenAI。 |
| WrenAI | 模型接入 | Azure OpenAI | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-05 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | DeepSeek | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-06 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | Gemini / Google AI Studio | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-07 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | Vertex AI | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-08 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | Bedrock | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-09 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | Anthropic | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-10 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | Groq | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-11 | 后续实现 | 未实现。 |
| WrenAI | 模型接入 | Ollama | FL §2.4 | WrenAI 增强 | 与 SQLBot Ollama 共用 | rework-ai-askdata-core-starter | Starter | model.default-* | 路由诊断 | IDX, CFG | MODEL-03 | 已实现 | 仅默认 LangChain4j/Ollama 适配，不含 WrenAI Provider。 |
| WrenAI | 模型接入 | Databricks | FL §2.4 | WrenAI 增强 | SQLBot 模型路由优先 | rework-ai-askdata-core-starter | Core | CapabilityProvider/模型 key | askdata_model_config | RT | MODEL-12 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | 语义准备/索引 | FL §2.5 | WrenAI 增强 | SQLBot 训练检索优先 | rework-ai-askdata-core-starter | training | 向量/训练 SPI | 内存向量/SPI | STORE, DB | TRAIN-02 | 部分实现 | 仅内存向量默认实现，MDL/schema 写入未实现。 |
| WrenAI | 语义索引、推荐与评测 | 语义描述 | FL §2.5 | WrenAI 增强 | SQLBot 术语优先 | rework-ai-askdata-core-starter | training | 描述资产 SPI 待定义 | askdata_training_asset | DB | TRAIN-05 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | 关系推荐 | FL §2.5 | WrenAI 增强 | 权限过滤先于推荐 | rework-ai-askdata-core-starter | training | 关系推荐 SPI 待定义 | askdata_mdl | DB | TRAIN-06 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | 问题推荐 | FL §2.5 | WrenAI 增强 | SQLBot 个人推荐优先 | rework-ai-askdata-core-starter | Core/training | Recommendation SPI | 内存/SPI | IDX, STORE | REC-01 | 部分实现 | 只有单机个人推荐桥接，无 WrenAI 算法。 |
| WrenAI | 语义索引、推荐与评测 | 历史问题 | FL §2.5 | WrenAI 增强 | SQLBot 记忆隔离优先 | rework-ai-askdata-core-starter | Core/training | 会话/记忆 SPI | 内存/SPI | STORE, DB | MEM-02 | 部分实现 | 不可跨项目或跨人召回；持久索引未实现。 |
| WrenAI | 语义索引、推荐与评测 | instructions | FL §2.5 | WrenAI 增强 | 与 SQLBot Prompt/术语合并 | rework-ai-askdata-core-starter | training | 指令资产 SPI 待定义 | askdata_training_asset | DB | TRAIN-04 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | SQL pair | FL §2.5 | WrenAI 增强 | 与 SQLBot SQL 示例合并 | rework-ai-askdata-core-starter | training | SQL pair 部署待定义 | askdata_training_asset | DB | TRAIN-03 | 后续实现 | 功能清单是目标，不是当前实现证据。 |
| WrenAI | 语义索引、推荐与评测 | ask feedback | FL §2.5 | WrenAI 增强 | SQLBot 个人隔离优先 | rework-ai-askdata-core-starter | training/observability | 反馈 API 待定义 | askdata_recommendation | DB | REC-02 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | evaluation | FL §2.5 | WrenAI 增强 | SQLBot 安全与质量验证优先 | rework-ai-askdata-core-starter | training/verify | 评测集/指标待定义 | askdata_training_asset | DB | EVAL-01 | 后续实现 | 尚无自动评测框架。 |
| WrenAI | 语义索引、推荐与评测 | dry-plan | FL §2.5 | WrenAI 增强 | SQLBot 最终校验优先 | rework-ai-askdata-core-starter | Core/query-engine | 计划分析 SPI 待定义 | askdata_sql_audit | SQL | SQL-02 | 设计覆盖 | SQL 校验已存在，不等于 dry-plan 实现。 |
| WrenAI | 语义索引、推荐与评测 | dry-run | FL §2.5 | WrenAI 增强 | SQLBot 最终校验优先 | rework-ai-askdata-core-starter | Core/query-engine | 空跑 SPI 待定义 | askdata_sql_audit | SQL | SQL-03 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | metadata constraints | FL §2.5 | WrenAI 增强 | SQLBot 权限元数据优先 | rework-ai-askdata-core-starter | governance/training | 元数据约束 SPI 待定义 | askdata_metadata | DB | MDL-10 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | validate | FL §2.5 | WrenAI 增强 | SQLBot 启动/运行校验优先 | rework-ai-askdata-core-starter | governance/query-engine | 连接器校验 SPI 待定义 | askdata_datasource | DB | CONN-19 | 后续实现 | 未实现。 |
| WrenAI | 语义索引、推荐与评测 | model substitute | FL §2.5 | WrenAI 增强 | SQLBot 候选 fallback 优先 | rework-ai-askdata-core-starter | Core/query-engine | model.fallback-enabled | 路由诊断 | CFG, FLOW | MODEL-13 | 部分实现 | 已有候选降级，不含 WrenAI 映射策略。 |
| WrenAI | 语义索引、推荐与评测 | preview data | FL §2.5 | WrenAI 增强 | 权限与 SQL 安全优先 | rework-ai-askdata-core-starter | governance/console | 预览 API 待定义 | askdata_metadata | SQL, DB | CONN-20 | 后续实现 | 未实现，必须受对象/行列权限约束。 |
| WrenAI | API、任务与仪表板 | API 文档 | FL §2.6 | WrenAI 增强 | SQLBot MCP/API 优先 | rework-ai-askdata-core-starter | api-mcp/console | REST/SDK 文档待定义 | askdata_mcp_api | FLOW | API-01 | 后续实现 | 当前仅 Starter 文档，无发布 API。 |
| WrenAI | API、任务与仪表板 | 后台任务 | FL §2.6 | WrenAI 增强 | SQLBot 状态/恢复边界优先 | rework-ai-askdata-core-starter | api-mcp/Core | 任务 SPI 待定义 | askdata_agent_state | STORE, FLOW | AGENT-01 | 设计覆盖 | 有 Agent 状态/检查点边界，无后台任务服务。 |
| WrenAI | API、任务与仪表板 | 轮询结果 | FL §2.6 | WrenAI 增强 | 与流式 API 协调 | rework-ai-askdata-core-starter | api-mcp | 状态查询 API 待定义 | askdata_agent_state | FLOW | AGENT-02 | 后续实现 | 未实现。 |
| WrenAI | API、任务与仪表板 | dashboard 生成 | FL §2.6 | WrenAI 增强 | SQLBot 仪表板优先 | rework-ai-askdata-core-starter | console/training | dashboard API 待定义 | 仪表板存储待定义 | DB | FE-03 | 后续实现 | 未实现。 |
| WrenAI | API、任务与仪表板 | dashboard 调度 | FL §2.6 | WrenAI 增强 | SQLBot 仪表板优先 | rework-ai-askdata-core-starter | console/observability | 调度 API 待定义 | 仪表板存储/监控 | DB | FE-12 | 后续实现 | 未实现。 |
| WrenAI | API、任务与仪表板 | 遥测 | FL §2.6 | WrenAI 增强 | SQLBot 监控日志优先 | rework-ai-askdata-core-starter | observability | 监控日志 SPI/异步导出 | 内存/SPI；askdata_monitoring_log | IDX, STORE, FLOW | AUD-02 | 部分实现 | 本地脱敏日志已实现；LangSmith/Langfuse/OTel 导出未实现。 |

## 覆盖核对

- `feature-list.md` 的 SQLBot 77 项和 WrenAI 59 项均在上表各有一行；重复条目没有被删除，而是在 `duplicatePolicy` 中写明以 SQLBot 为主的吸收方式。
- DataEase、MCP、Dify、Coze、n8n、企业微信、钉钉、飞书均标记为外部集成预留或后续实现；企业平台登录、扫码、免登和组织同步统一由业务 IAM/JustAuth 完成，不能据此认定 AskData 已内置这些协议。
- Java 17 远程运行时为协议和治理设计：Java 8 / Spring Boot 2.7.18 主进程不直接引入 Java 17 专属依赖，任何具体适配器均需接入时验证。
