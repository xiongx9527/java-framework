# SQLBot 与 WrenAI 功能清单

- Change: `rework-ai-askdata-core-starter`
- Date: 2026-07-27
- Baseline: SQLBot v1 文档、SQLBot 开源仓库、Canner/WrenAI 公开仓库
- Rule: SQLBot 功能优先实现；WrenAI 功能必须覆盖。重复功能按 SQLBot 产品语义落地，吸收 WrenAI 的底层增强。任何需真实租户、外部凭据或业务 UI 接入验证的条目都必须在追踪矩阵中标记验证状态，不得把契约覆盖误报为真实外部连通。

## 1. SQLBot 功能清单

### 1.1 基础与部署

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| 产品介绍与工作原理 | 文档说明 LLM + RAG 智能问数工作机制 | console / core | 文档首页必须说明 |
| 安装部署 | 支持部署、初始化、默认账号/配置说明 | console / api-mcp | 后续按 Starter 和控制台交付 |
| 命令行工具 | 支持 SQLBot 文档中的命令行运维能力映射 | enterprise / api-mcp | Java Starter 需提供等价管理入口或说明 |
| 参数配置 | 支持系统参数、第三方参数、运行参数 | enterprise | X-Pack 参数配置纳入企业能力 |
| 外观设置 | 支持外观/品牌/页面展示配置 | enterprise / console | 不复用 SQLBot 品牌素材 |
| 操作日志 | 支持管理员查询操作日志 | enterprise / observability | 与对话监控日志区分 |

### 1.2 项目、组织与权限

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| 工作空间/项目 | 项目是所有数据、对话、记忆、推荐、审计的前提边界 | governance / core | Core 已定义 `projectId` 强制归属 |
| 成员管理 | 支持项目成员、角色、授权关系 | governance / console | 前端组件化实现 |
| 权限配置 | 支持数据源、表、列、行级权限 | governance | SQL 执行必须强制校验 |
| 组织用户同步 | 由业务 IAM/JustAuth 支持企业微信、钉钉、飞书等组织用户同步 | enterprise | AskData 只接收映射后的项目成员与权限 |

### 1.3 数据源与方言

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| 数据源概览 | 控制台展示数据源状态、类型、权限、同步状态 | console / governance | 组件化实现 |
| Excel/CSV | 支持文件型数据源 | governance / query-engine | 需明确存储、解析和权限策略 |
| MySQL | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| Oracle | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| PostgreSQL | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| SQL Server | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| ClickHouse | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| 达梦 | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| Apache Doris | 支持元数据、方言、只读校验、执行 | governance / query-engine | 只是必须验证项之一，不代表全量支持 |
| AWS Redshift | 支持元数据、方言、只读校验、执行 | governance / query-engine | 与 WrenAI Redshift 合并跟踪 |
| Elasticsearch | 支持数据源配置和查询语义映射 | governance / query-engine | 需定义 SQL/DSL 安全边界 |
| Kingbase | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| StarRocks | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |
| Apache Hive | 支持元数据、方言、只读校验、执行 | governance / query-engine | 进入能力矩阵 |

### 1.4 智能问数与训练

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| 智能问数 | 自然语言提问、SQL 生成、执行、解释、图表返回 | core / api-mcp / console | SQLBot 产品语义优先 |
| 多轮上下文 | 对话上下文、压缩、恢复、按人存储 | core | `projectId + operatorId` 强制归属 |
| 多提问 | 显式多问题和复合问题拆分、聚合、部分失败 | core | 子问题独立审计 |
| SQL 安全 | 单语句、只读、授权对象、限行、超时、审计 | core / governance | 所有插件产出必须重校验 |
| 图表/图片渲染 | 返回结构化结果、图表配置、图片资源地址 | api-mcp / console | MCP 也需支持 |
| 仪表板 | 创建、查看、推荐或生成仪表板 | api-mcp / console / training | WrenAI dashboard 能力作为增强 |
| 小助手应用 | 基础/高级小助手、浮窗、全屏、页面嵌入 | console / api-mcp | 外部接入能力 |
| 术语配置 | 术语、同义词、业务口径管理 | training | 与 WrenAI instructions/MDL 合并增强 |
| 训练检索 | 训练资产索引、召回、RAG | training | 支持多向量存储，默认内存 |
| SQL 示例 | 已确认 NL-SQL / SQL pair 训练样例 | training | 与 WrenAI SQL pair 对齐 |
| Prompt/自定义提示词 | 系统/项目/数据源级提示词配置 | enterprise / training | X-Pack 自定义提示词 |
| 长期记忆 | 偏好、确认问数、语义线索、结果摘要 | core / training | 按项目和操作人隔离 |
| 推荐 | 按操作人推荐问题、指标、语义对象 | core / training | 不跨人复用个人画像 |

### 1.5 模型与运行时

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| AI 模型配置 | 配置模型、API Key、默认模型、能力标签 | core / api-mcp / console | API Key 需脱敏与审计 |
| 模型接入 | 支持 SQLBot 模型接入能力 | core / api-mcp | 纳入外部系统对接矩阵 |
| Ollama 部署模型接入 | 默认模型厂商为 Ollama，默认聊天 `qwen3:32b`，向量 `bge-m3` | core | 可配置覆盖 |
| LangChain4j 默认适配 | 复用当前 fw-plus LangChain4j 封装 | core | 不是固定运行时 |
| Spring AI 替换 | 文档说明依赖、配置、Bean/SPI 覆盖、验证 | core | 可选适配 |
| Spring AI Alibaba 替换 | 文档说明模型、Workflow、DataAgent、MCP/A2A 替换 | core | 可选适配 |
| AgentScope Java 2.0+ 替换 | 文档说明 HarnessAgent、状态、权限、tracing 替换 | core | 可选适配 |
| Java 17 独立适配服务 | 当 Spring AI、Spring AI Alibaba、AgentScope Java 2.0+ 无法内嵌 Java 8 / Spring Boot 2.7.18 时，通过 HTTP/SSE、MCP、A2A、gRPC 或消息队列远程对接 | core | 文档必须说明不兼容原因、部署、鉴权、上下文、错误、审计和回滚 |

### 1.6 外部系统对接

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| MCP Server | 支持 SQLBot MCP 服务能力 | api-mcp / console | 外部 Agent 平台调用入口 |
| `mcp_start` | 初始化对话，返回 `access_token` 和 `chat_id` | api-mcp | 必须绑定项目/操作人/审计 |
| `mcp_question` | 提交问题，返回 SQL、结果、图表、图片地址 | api-mcp | 同一 `chat_id` 维持上下文 |
| `mcp_ws_list` | 返回当前用户可访问工作空间 | api-mcp | 权限过滤 |
| `mcp_datasource_list` | 返回指定工作空间可用数据源 | api-mcp | 权限过滤 |
| SSE/Streamable HTTP | MCP 传输协议支持 | api-mcp | 默认 SSE，预留 Streamable HTTP |
| 嵌入式对接 | 小助手嵌入和页面嵌入 | api-mcp / console | JS/组件化交付 |
| 基础应用 | 游客/员工权限，使用 AskData 自身数据源 | api-mcp / console | 支持 `online` 参数 |
| 高级应用 | 宿主权限和宿主 API 数据源 | api-mcp / console | 后端不得保存宿主凭据 |
| 浮窗嵌入 | 页面右下角浮窗小助手 | console | Vue3 组件 |
| 全屏/区域嵌入 | 全屏或指定区域对话页面 | console | Vue3 组件 |
| 页面嵌入 token | 后端生成 token，避免泄露 app secret | api-mcp | appId/account/appSecret 规则 |
| 宿主凭证映射 | localStorage、Cookie、Header、参数映射 | api-mcp / console | 支持 `sqlbot-embedded-token` 等模式 |
| DataEase 接入 | 以高级应用方式接入 DataEase | api-mcp / console | SQLBot 文档硬基线 |
| `/de2api/sqlbot/datasource` | DataEase 固定数据源接口 | api-mcp | 支持权限 SQL 和字段描述 |
| `x-de-token` | DataEase token Header 映射 | api-mcp | `user.token` / `de_v2_user.token` |
| AES-Key | DataEase 敏感数据 AES 加密 | api-mcp / enterprise | 配置和审计 |
| MaxKB 对接 | 通过 MCP/嵌入/API 调用 AskData | api-mcp | 文档和联调用例 |
| Dify 对接 | 通过 MCP/嵌入/API 调用 AskData | api-mcp | 文档和联调用例 |
| Coze 对接 | 通过 MCP/嵌入/API 调用 AskData | api-mcp | 文档和联调用例 |
| n8n 对接 | 通过 MCP/嵌入/API 调用 AskData | api-mcp | 文档和联调用例 |

### 1.7 X-Pack / 企业能力

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| 登录认证 | 支持 X-Pack 登录认证能力 | enterprise | 不做 License 限制 |
| 平台对接 | 企业微信、钉钉、飞书由业务 IAM/JustAuth 接入 | enterprise / console | AskData 不自研扫码、免登或 OAuth |
| 企业微信 | JustAuth 企业微信登录授权、可信域名/IP 与组织同步 | enterprise | 业务 IAM 管理密钥并审计配置变更 |
| 钉钉 | JustAuth 钉钉登录授权、回调、免登与组织同步 | enterprise | 业务 IAM 管理 AgentId/AppKey/AppSecret |
| 飞书 | JustAuth 飞书网页应用、重定向 URL、免登与组织同步 | enterprise | 业务 IAM 管理 App ID/App Secret |
| 扫码登录 | JustAuth 完成平台扫码登录 | enterprise / console | AskData 只接收权限映射 |
| 免登 | JustAuth 完成 PC/移动端免登 | enterprise / console | 项目/操作人归属不可缺失 |
| 组织用户同步 | 业务 IAM/JustAuth 同步组织、用户、应用权限 | enterprise / governance | 与项目成员权限衔接 |
| 参数配置 | 企业参数、第三方参数、运行参数 | enterprise | 文档列默认值和风险 |
| 外观设置 | 主题、外观和嵌入展示配置 | enterprise / console | 可覆盖主题 |
| 操作日志 | 管理员操作日志和配置变更日志 | enterprise / observability | 与监控日志分开 |

## 2. WrenAI 功能清单

### 2.1 GenBI 问数体验

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| 自然语言问数 | 任意语言提问并生成 SQL/答案 | core / console | 与 SQLBot 智能问数重复，SQLBot 优先 |
| Text-to-SQL | 基于语义模型和上下文生成 SQL | core / training | WrenAI 语义层增强 |
| Text-to-Chart | 根据结果生成图表 | api-mcp / console | 与 SQLBot 图表渲染合并 |
| AI 摘要 | 对结果生成摘要和解释 | core / console | 结果解释增强 |
| AI 报告 | 生成报告和业务洞察 | console / api-mcp | 进入 GenBI 增强 |
| GenBI Insights | 一键业务洞察、图表、报告 | console / training | 与仪表板能力衔接 |
| 多语言问数 | 支持中文、英文等提问 | core | Prompt 和模型策略配置 |
| 可嵌入 API | 通过 API 嵌入查询和图表 | api-mcp | 与 SQLBot 嵌入/API 合并 |

### 2.2 MDL 语义层

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| MDL manifest | 语义模型 manifest 管理、版本、部署 | training / governance | 作为统一语义抽象 |
| 模型 | 业务模型定义 | training | SQLBot 元数据/术语优先，MDL 增强 |
| 列 | 字段引用名、原始列名、类型、描述 | training / governance | 权限过滤 |
| 嵌套列 | 嵌套字段描述和类型 | training | WrenAI 增强 |
| 关系 | 模型关系和 join 规则 | training | 影响 SQL 生成 |
| 指标 | 指标口径、计算字段、度量 | training | 与 SQLBot 术语/指标合并 |
| 视图 | SQL 视图、缓存视图、语义视图 | training / governance | 支持 preview/dry-plan |
| deploy log | 语义部署日志、状态、错误 | training / observability | 与操作日志区分 |
| 语义版本 | manifest hash、版本兼容、回滚 | training | 恢复与审计必需 |

### 2.3 数据源与连接器

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| Athena/Trino | 支持连接器和方言矩阵 | governance / query-engine | 与 Trino 合并 |
| Redshift | 支持连接器和方言矩阵 | governance / query-engine | 与 SQLBot AWS Redshift 合并 |
| BigQuery | 支持连接器和方言矩阵 | governance / query-engine | WrenAI 增强 |
| DuckDB | 支持连接器和方言矩阵 | governance / query-engine | WrenAI 增强 |
| PostgreSQL | 支持连接器和方言矩阵 | governance / query-engine | 与 SQLBot 合并 |
| MySQL | 支持连接器和方言矩阵 | governance / query-engine | 与 SQLBot 合并 |
| SQL Server | 支持连接器和方言矩阵 | governance / query-engine | 与 SQLBot 合并 |
| ClickHouse | 支持连接器和方言矩阵 | governance / query-engine | 与 SQLBot 合并 |
| Oracle | 支持连接器和方言矩阵 | governance / query-engine | 与 SQLBot 合并 |
| Trino | 支持连接器和方言矩阵 | governance / query-engine | WrenAI 增强 |
| Snowflake | 支持连接器和方言矩阵 | governance / query-engine | WrenAI 增强 |

### 2.4 模型接入

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| OpenAI | 支持模型能力 key 和路由 | core | 可通过 Spring AI/LangChain4j/业务适配 |
| Azure OpenAI | 支持模型能力 key 和路由 | core | 可选适配 |
| DeepSeek | 支持模型能力 key 和路由 | core | 可选适配 |
| Gemini / Google AI Studio | 支持模型能力 key 和路由 | core | 可选适配 |
| Vertex AI | 支持 Gemini + Anthropic 等模型 | core | 可选适配 |
| Bedrock | 支持模型能力 key 和路由 | core | 可选适配 |
| Anthropic | 支持模型能力 key 和路由 | core | 可选适配 |
| Groq | 支持模型能力 key 和路由 | core | 可选适配 |
| Ollama | 默认本地模型厂商 | core | `qwen3:32b` / `bge-m3` 默认 |
| Databricks | 支持模型能力 key 和路由 | core | WrenAI 增强 |

### 2.5 语义索引、推荐与评测

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| 语义准备/索引 | 将 MDL、schema、文档写入检索索引 | training | 支持多向量存储 |
| 语义描述 | 生成或管理模型/字段描述 | training | WrenAI 增强 |
| 关系推荐 | 推荐模型关系和 join | training | 权限过滤 |
| 问题推荐 | 按项目和操作人推荐问题 | core / training | 不跨人画像 |
| 历史问题 | 历史问题索引和召回 | training | 按项目和人隔离 |
| instructions | 指令/业务规则索引和检索 | training | 与 SQLBot Prompt/术语合并 |
| SQL pair | 已确认 SQL pair 部署 | training | 与 SQLBot SQL 示例合并 |
| ask feedback | 提问反馈采集和学习 | training / observability | 按人记录 |
| evaluation | 评测框架和指标 | training / verify | 验证 SQL/回答质量 |
| dry-plan | 执行前计划分析 | core / query-engine | SQL 安全增强 |
| dry-run | 执行前空跑/校验 | core / query-engine | SQL 安全增强 |
| metadata constraints | 元数据约束获取和使用 | governance / training | WrenAI 增强 |
| validate | 连接器字段/模型校验 | governance / query-engine | 启动和运行期校验 |
| model substitute | 模型替代/映射 | training / query-engine | WrenAI 增强 |
| preview data | 数据预览 | governance / console | 权限受控 |

### 2.6 API、任务与仪表板

| 功能 | 要求 | 归属 change | 备注 |
| --- | --- | --- | --- |
| API 文档 | 提供 REST/API SDK 文档 | api-mcp / console | 前端 SDK 对齐 |
| 后台任务 | 长任务异步执行 | api-mcp / core | 与 Agent 状态/检查点衔接 |
| 轮询结果 | 任务状态和结果查询 | api-mcp | 兼容流式响应 |
| dashboard 生成 | Agent 生成仪表板 | console / training | 与 SQLBot 仪表板合并 |
| dashboard 调度 | 仪表板定时/刷新/调度 | console / observability | WrenAI 增强 |
| 遥测 | 操作、生成、性能事件 | observability | 可导出 LangSmith/Langfuse/OTel |

## 3. 重复能力处理规则

| 重复能力 | 处理规则 |
| --- | --- |
| 问数/Text-to-SQL | SQLBot 产品语义优先，WrenAI 语义层和评测增强 |
| 语义模型/元数据 | SQLBot 项目和权限边界优先，WrenAI MDL 作为统一语义抽象增强 |
| 训练/Prompt/instructions | SQLBot 术语、SQL 示例、Prompt 优先，WrenAI instructions 和 SQL pair 合并为版本化知识资产 |
| 推荐 | SQLBot 按项目/操作人推荐优先，WrenAI question recommendation 作为算法增强 |
| 仪表板/图表 | SQLBot 仪表板和小助手入口优先，WrenAI dashboard 生成/调度增强 |
| API/嵌入 | SQLBot MCP/嵌入/DataEase 对接优先，WrenAI 可嵌入 API 补充 SDK 能力 |
| 数据源 | 两者并集进入能力矩阵，重复数据源合并，差异行为单列 |

## 4. 追踪矩阵字段

后续正式文档中的功能追踪矩阵 SHALL 至少包含以下字段：

| 字段 | 说明 |
| --- | --- |
| product | `SQLBot` 或 `WrenAI` |
| category | 功能分类 |
| feature | 功能名称 |
| source | 来源链接或仓库文件 |
| priority | SQLBot 优先、WrenAI 增强、并集必选 |
| duplicatePolicy | 重复能力处理规则 |
| change | 负责的 OpenSpec change |
| module | 实现模块或前端组件 |
| apiOrConfig | 公开 API、配置、SPI 或组件属性 |
| storage | 涉及的数据库表或状态存储 |
| docs | 对应用户文档/设计文档 |
| tests | 自动化或人工测试用例 |
| status | 未开始、设计中、实现中、已实现、已验证、后续、阻塞 |
| limitation | 限制、未验证项或风险 |
