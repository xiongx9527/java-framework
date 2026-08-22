---
generated_from_state_version: 6
---

# Verification

## Current result

- Result: **Passed**
- Assurance: **skill-coordinated**
- Goal cycle: 1
- Iteration: 1
- Verifier attempt: 1
- Completed: 2026-08-14T08:08:27.777Z
- Summary: 独立 Verifier 逐项核对 A1-A85：85 passed、0 failed、0 blocked。重点 A37/A38 的原始字节限额、真实 MVC converter、direct Service 防绕过、Provider 前拒绝、HTTP 顺序、单次委托、稳定错误及安全审计均有实现和 Runtime 95 项测试证据。

## Acceptance

| ID | Result | Source | Criterion | Reason |
| --- | --- | --- | --- | --- |
| A1 | passed | brief.md | 在 Magic-API 控制台通过 Tinyflow toolbar 新建工作流，选择现有 LangChain4j 模型，保存后输入变量运行，能得到真实结果；含挂起节点的流程能以实例 ID 恢复。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A2 | passed | brief.md | 工作流资源正文是 Tinyflow 原生 JSON。非法 JSON、未知节点、无起止路径或未保存修改均得到明确提示，且不会覆盖已保存版本。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A3 | passed | brief.md | Starter JAR 中存在版本化 Tinyflow UI JS/CSS；Magic 插件只依赖 Starter 并能加载这些资源，卸载页面时正确销毁画布。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A4 | passed | brief.md | 业务注册自定义 `TinyflowNodeProvider` 及对应前端节点定义后，新节点可保存并执行；缺少后端解析器时明确报错。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A5 | passed | brief.md | 本地知识库管理 UI 可创建知识库、导入文档、查看和编辑 chunk、重建索引、删除文档并执行检索测试；模型、向量库和对象存储均从现有资源选择。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A6 | passed | brief.md | 同一 `sourceId` 内容未变化时导入幂等；内容变化时创建新 `documentId` 版本并原子切换，旧向量可补偿删除。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A7 | passed | brief.md | 每个 chunk 都包含非空 `sourceId`、`sourceType`、`documentId`、`chunkId`，检索结果保持四级来源身份。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A8 | passed | brief.md | Tinyflow Knowledge 节点使用 `provider=fw-plus-local` 或 `provider=dify` 检索；未知 Provider、知识库不存在、能力不支持和远端失败均返回稳定错误。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A9 | passed | brief.md | Dify 携带有效访问密钥调用 `POST /retrieval` 可检索本地知识库；响应 metadata 必含 `source_id`、`source_type`、`document_id`、`chunk_id`，无效密钥或租户越权被拒绝。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A10 | passed | specs/tinyflow-knowledge-base/spec.md | Provider 注册和路由 - **GIVEN** 系统注册 `fw-plus-local` 与 `dify` Provider - **WHEN** Knowledge 节点以 `provider + knowledgeId` 发起检索 - **THEN** Registry 将请求路由到匹配 Provider - **AND** 重复 key 在启动时失败 - **AND** Provider 或知识库不存在时明确失败而不是返回空结果 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A11 | passed | specs/tinyflow-knowledge-base/spec.md | Provider 能力 - **WHEN** 管理 UI 查询 Provider 能力 - **THEN** `fw-plus-local` 声明完整管理和检索能力 - **AND** `dify` 声明只读检索能力 - **AND** UI 不为只读 Provider 展示上传、切片、重建或删除操作 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A12 | passed | specs/tinyflow-knowledge-base/spec.md | 本地资源复用 - **GIVEN** 系统已有 Embedding 模型、Vector Store 和 S3 资源 - **WHEN** 创建本地知识库 - **THEN** 定义只保存对应资源 key 或对象引用 - **AND** 不创建 Tinyflow 私有连接或保存相关密钥 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A13 | passed | specs/tinyflow-knowledge-base/spec.md | Tinyflow 检索 Dify - **GIVEN** 节点配置 `provider=dify`、连接引用和 Dataset ID - **WHEN** 执行检索 - **THEN** Provider 通过现有 `fw-plus-dify` Dataset 客户端映射 TopK、阈值和结果 - **AND** 超时、鉴权失败、Dataset 不存在和无效响应转换为稳定且不泄密的 Tinyflow 错误 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A14 | passed | specs/tinyflow-knowledge-base/spec.md | 通用 Dify 客户端保持独立 - **WHEN** 检查模块依赖 - **THEN** `fw-plus-dify` 不依赖 Tinyflow - **AND** Dify 与 Tinyflow 的组合代码只存在于 `fw-plus-ai-tinyflow-dify` | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A15 | passed | specs/tinyflow-knowledge-base/spec.md | 资源 CRUD 与版本 - **WHEN** 创建、读取、更新或删除任一知识库资源 - **THEN** 系统使用 Magic-API 的资源 ID、分组、备份、版本和权限能力 - **AND** 并发修改通过资源版本乐观校验 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A16 | passed | specs/tinyflow-knowledge-base/spec.md | 敏感信息存储 - **WHEN** 保存资源正文 - **THEN** 模型、向量、S3 和 Dify 只保存安全引用 - **AND** Dify 外部访问密钥只保存强哈希、可识别前缀和授权范围 - **AND** 不保存任何明文 API Key 或 Secret | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A17 | passed | specs/tinyflow-knowledge-base/spec.md | 来源字段校验 - **WHEN** 创建或更新 chunk - **THEN** 缺少四个字段中的任一个都被拒绝 - **AND** `sourceType` 内置支持 `file`、`text`、`url`、`database`、`api`、`connector` - **AND** 非空稳定扩展 key 也被允许 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A18 | passed | specs/tinyflow-knowledge-base/spec.md | 完整引用信息 - **WHEN** 保存和返回 chunk - **THEN** 除四级 ID 外还保留 `sourceUri`、`sourceVersion`、定位信息、position、parentChunkId、content、tokenCount、metadata、enabled 和 vectorIds | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A19 | passed | specs/tinyflow-knowledge-base/spec.md | 来源删除 - **GIVEN** 一个 `sourceId` 存在多个文档版本 - **WHEN** 删除该来源 - **THEN** 系统找到全部 document、chunk 和 vector ID 并进入可恢复删除流程 - **AND** 删除失败时保留补偿信息，不谎报完成 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A20 | passed | specs/tinyflow-knowledge-base/spec.md | 幂等导入 - **GIVEN** 相同知识库、`sourceId` 与 checksum 已成功导入 - **WHEN** 再次导入相同内容 - **THEN** 系统复用当前文档版本 - **AND** 不重复解析、Embedding 或向量写入 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A21 | passed | specs/tinyflow-knowledge-base/spec.md | 内容更新 - **GIVEN** 已存在可检索的旧文档版本 - **WHEN** 同一 `sourceId` 以新 checksum 导入 - **THEN** 系统生成新 `documentId` 和 chunk - **AND** 新索引全部成功后原子切换可检索版本 - **AND** 任一阶段失败时旧版本仍可检索 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A22 | passed | specs/tinyflow-knowledge-base/spec.md | 异步任务状态 - **WHEN** 执行导入、重建或删除 - **THEN** 任务在 `PENDING`、`PARSING`、`CHUNKING`、`EMBEDDING`、`INDEXING`、`COMPLETED`、`FAILED`、`CANCELLING`、`CANCELLED` 等状态间合法转换 - **AND** 保存阶段、进度、处理数量、重试次数、时间和安全错误 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A23 | passed | specs/tinyflow-knowledge-base/spec.md | 向量补偿 - **GIVEN** 向量写入成功但资源保存失败 - **WHEN** 任务进入失败恢复 - **THEN** 系统保存待补偿 vector ID 并重试删除 - **AND** 不产生不可追踪向量 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A24 | passed | specs/tinyflow-knowledge-base/spec.md | 完整检索结果 - **WHEN** 检索命中有效 chunk - **THEN** 每项结果包含 content、score、title、四级来源 ID、来源定位和原始 metadata - **AND** 停用知识库、文档或 chunk 不参与召回 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A25 | passed | specs/tinyflow-knowledge-base/spec.md | 向量库不支持 Metadata 过滤 - **GIVEN** 当前 Vector Store 不支持服务端 Metadata 过滤 - **WHEN** 请求带 metadata 条件 - **THEN** 系统按可配置倍数扩大候选并从资源仓库二次过滤 - **AND** 候选数量受硬上限约束，不执行无界扫描 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A26 | passed | specs/tinyflow-knowledge-base/spec.md | Vector Store 隔离 - **WHEN** 创建本地知识库 - **THEN** 默认一个知识库独占一个 Vector Store - **AND** 只有具体实现明确支持知识库过滤时才允许共享 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A27 | passed | specs/tinyflow-knowledge-base/spec.md | 合法检索请求 - **GIVEN** 请求携带有效 Bearer Key、授权范围内的 `knowledge_id`、query、整数 `top_k` 和 0..1 `score_threshold` - **WHEN** 调用 `/retrieval` - **THEN** 服务限制 TopK、超时和频率后调用本地 Provider - **AND** 返回 HTTP 200 和 `records` 数组 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A28 | passed | specs/tinyflow-knowledge-base/spec.md | Dify 响应 metadata - **WHEN** 外部检索返回一条或多条记录 - **THEN** 每条记录包含 `content`、`score`、`title` 和非 null 对象 `metadata` - **AND** metadata 强制包含 `source_id`、`source_type`、`document_id`、`chunk_id` - **AND** 无结果返回 HTTP 200 和空数组 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A29 | passed | specs/tinyflow-knowledge-base/spec.md | Metadata 条件 - **WHEN** 请求包含 `metadata_condition` - **THEN** 系统支持 `and`、`or` 逻辑 - **AND** 支持 contains、not contains、start with、end with、is、is not、in、not in、empty、not empty、等于、不等于、大小比较、before、after 运算 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A30 | passed | specs/tinyflow-knowledge-base/spec.md | 认证和错误契约 - **GIVEN** 鉴权头无效、密钥无效、越权、知识库不存在、超时或内部错误 - **WHEN** 调用外部接口 - **THEN** 系统返回稳定非 200 状态及 `error_code`、`error_msg` - **AND** 不泄露凭证或敏感正文 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A31 | passed | specs/tinyflow-knowledge-base/spec.md | 防止循环调用 - **GIVEN** 外部请求的目标试图解析为 `dify` Provider - **WHEN** 服务路由请求 - **THEN** 系统拒绝请求 - **AND** 不向上游 Dify 发起二次检索 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A32 | passed | specs/tinyflow-knowledge-base/spec.md | 本地知识库工作台 - **WHEN** 用户打开 `fw-plus-local` 知识库 - **THEN** 页面提供概览、文档、切片、召回测试、API 接入、任务和设置 - **AND** 支持文件/文本/URL/批量导入、切片预览编辑、启停、Metadata、父子关系和来源定位 - **AND** 支持重建、删除、重试、取消和进度查看 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A33 | passed | specs/tinyflow-knowledge-base/spec.md | API 和连接管理 - **WHEN** 用户管理外部接入 - **THEN** 可创建、轮换、吊销 API Key，明文只显示一次 - **AND** 展示 Dify Endpoint 和 External Knowledge ID - **AND** Dify Provider 页面仅管理服务地址、凭证引用、Dataset ID 和连通性测试 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A34 | passed | specs/tinyflow-knowledge-base/spec.md | 资源选项 - **WHEN** 用户选择 Embedding 模型、Vector Store 或对象存储 - **THEN** UI 从现有 Magic/Spring Provider 获取非敏感 key、名称、类型和 provider - **AND** 不在知识库页面重复提供这些数据源的 CRUD | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A35 | passed | specs/tinyflow-knowledge-base/spec.md | 跨租户访问 - **GIVEN** 两个租户使用相同 knowledgeId、sourceId 或 chunkId - **WHEN** 任一租户读取、修改、检索或删除 - **THEN** 只能影响当前租户授权范围内资源 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A36 | passed | specs/tinyflow-knowledge-base/spec.md | URL 导入防护 - **WHEN** 用户提交 URL 来源 - **THEN** 系统限制协议、目标地址、重定向、文件大小、响应类型和超时 - **AND** 阻止访问本机、内网及云元数据地址 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A37 | passed | specs/tinyflow-knowledge-base/spec.md | 资源和请求限额 - **WHEN** 上传、解析或外部检索超过格式、大小、数量、解压规模、并发、内存、TopK、候选数、超时或频率限制 - **THEN** 系统在执行危险或无界工作前拒绝或终止请求 | Filter 对 known Content-Length 和 unknown/chunked body 均在危险工作前按原始字节限制，InputStream/Reader 和真实 MockMvc String converter 已覆盖；Service 对 query、metadata 条件数及 Map/Collection/array 深度在 direct 调用和 Provider 前统一拒绝。 |
| A38 | passed | specs/tinyflow-knowledge-base/spec.md | 审计和脱敏 - **WHEN** 创建、更新、删除、导入、重建、密钥变更、连接测试或外部检索发生 - **THEN** 系统记录不含凭证和完整敏感正文的审计事件 - **AND** 普通日志与错误不输出 API Key、Authorization、S3/模型/向量凭证 | AuditCoordinator 只记录安全字段；Filter、MVC malformed、Controller 和 Service 按委托边界恰好一次审计；成功审计失败 fail-closed，业务失败保留原异常并附加 suppressed；无 sink 不发布 endpoint graph，所有实例显式注入 Coordinator。 |
| A39 | passed | specs/tinyflow-knowledge-base/spec.md | 原子重建 - **GIVEN** 当前索引可用 - **WHEN** 重建新索引失败 - **THEN** 系统保留旧索引和失败任务用于诊断及重试 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A40 | passed | specs/tinyflow-knowledge-base/spec.md | 延迟删除 - **GIVEN** 删除向量失败 - **WHEN** 删除文档或来源 - **THEN** 资源进入 `DELETE_PENDING` 并创建补偿任务 - **AND** API 不报告删除已完全完成 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A41 | passed | specs/tinyflow-knowledge-base/spec.md | 无 GraphRAG 实现 - **GIVEN** 应用未注册 `TinyflowKnowledgeGraphProvider` - **WHEN** 使用默认本地知识库 - **THEN** 向量、关键词和混合检索正常工作 - **AND** classpath 不需要 Neo4j | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A42 | passed | specs/tinyflow-knowledge-base/spec.md | 自定义扩展 - **GIVEN** 业务注册兼容 SPI 的解析器、Reranker 或 Graph Provider - **WHEN** 对应知识库配置启用该能力 - **THEN** 系统使用扩展实现并维持租户、来源 ID、安全和错误契约 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A43 | passed | specs/tinyflow-knowledge-base/spec.md | Core 必须提供最小只读 `TinyflowKnowledgeProvider`、可选管理 `TinyflowKnowledgeManagementProvider`、Provider Registry、统一请求/结果及 Tinyflow Knowledge 节点适配器。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A44 | passed | specs/tinyflow-knowledge-base/spec.md | `fw-plus-local` 必须复用现有模型、Embedding、向量库和对象存储实现完整知识库管理；`dify` 必须位于 Tinyflow dify 模块并依赖现有通用 `fw-plus-dify` 客户端。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A45 | passed | specs/tinyflow-knowledge-base/spec.md | Magic 插件必须用结构化 JSON 注册和管理六类 resource：`tinyflow-knowledge`、`tinyflow-document`、`tinyflow-chunk`、`tinyflow-ingestion-task`、`tinyflow-knowledge-key`、`tinyflow-dify-connection`。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A46 | passed | specs/tinyflow-knowledge-base/spec.md | 每个 chunk 必须包含非空且语义独立的 `sourceId`、`sourceType`、`documentId`、`chunkId`，并在导入、资源、向量 metadata、检索和外部响应全链路中保持。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A47 | passed | specs/tinyflow-knowledge-base/spec.md | 本地 Provider 必须支持文件、文本、URL 和扩展连接器导入，内置解析 TXT、Markdown、PDF、DOCX、HTML、CSV、XLSX，并支持可替换解析器和普通、Q&A、父子切片策略。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A48 | passed | specs/tinyflow-knowledge-base/spec.md | `fw-plus-local` 必须支持向量、关键词和混合检索，并依次处理可选查询改写、候选召回、Metadata 过滤、融合去重、分数归一化、可选 Rerank、父子展开、阈值和 TopK。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A49 | passed | specs/tinyflow-knowledge-base/spec.md | 系统必须在显式启用且存在 Access Key Repository 时发布可配置基础路径下的 `POST /retrieval`，实现现有 `fw-plus-dify` 的 `IKnowledgeService` 契约并且只路由到 `fw-plus-local`。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A50 | passed | specs/tinyflow-knowledge-base/spec.md | Starter 内置 UI 必须提供参考 Dify/RAGFlow 的完整本地知识库管理工作台，Magic 插件通过独立 `toolbars` 入口加载，并根据 Provider 能力控制操作。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A51 | passed | specs/tinyflow-knowledge-base/spec.md | 知识库、文档、chunk、任务、连接和密钥必须携带 tenantId；管理接口、外部接口和资源仓库必须执行租户与资源权限隔离。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A52 | passed | specs/tinyflow-knowledge-base/spec.md | 系统必须区分 Provider/知识库/模型/向量库不存在、无权限、解析失败、索引失败、检索失败、上游 Dify 失败和超时，并提供可恢复导入、重建和删除语义。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A53 | passed | specs/tinyflow-knowledge-base/spec.md | Core 必须定义可替换的文档解析、切片、Rerank、对象存储、凭证、审计和 GraphRAG SPI；首版不得注册默认 Neo4j 或 GraphRAG 实现。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A54 | passed | specs/tinyflow-workflow/spec.md | Java 8 兼容依赖 - **GIVEN** 当前项目使用 Java 8、Spring Boot 2.7.18 和 LangChain4j 0.35.0 - **WHEN** 构建 Tinyflow 模块依赖树 - **THEN** 系统使用 `dev.tinyflow:tinyflow-core:2.0.7` - **AND** 依赖树不包含 `tinyflow-support-langchain4j`、LangChain4j 1.x 或 Java 17 字节码 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A55 | passed | specs/tinyflow-workflow/spec.md | 框架无关 Core - **WHEN** 检查 Core Tinyflow 模块依赖 - **THEN** Core 不依赖 Spring、Servlet、Magic-API 或前端构建工具 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A56 | passed | specs/tinyflow-workflow/spec.md | 消息和响应转换 - **GIVEN** Tinyflow LLM 节点包含系统消息、文本内容或图片内容 - **WHEN** 适配器调用 LangChain4j 模型 - **THEN** 适配器使用 0.35.0 的 `SystemMessage`、`UserMessage`、`TextContent`、`ImageContent` 和 `generate` API - **AND** 将 `Response<AiMessage>.content()` 转换为 Tinyflow 文本响应 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A57 | passed | specs/tinyflow-workflow/spec.md | 无效模型响应 - **GIVEN** 模型返回空 Response、空 `AiMessage` 或空文本 - **WHEN** Tinyflow 执行 LLM 节点 - **THEN** 系统抛出稳定的 Tinyflow 执行异常 - **AND** 错误不包含密钥、请求头或敏感变量 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A58 | passed | specs/tinyflow-workflow/spec.md | 模型解析 - **GIVEN** 节点声明 `llmId` - **WHEN** LLM Provider 创建适配器 - **THEN** 系统从现有 `LangChain4jModelRegistry` 按 key 获取 `ChatLanguageModel` - **AND** 空 `llmId` 使用注册表默认模型 - **AND** 模型缺失或类型错误时返回包含安全 `llmId` 的明确错误 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A59 | passed | specs/tinyflow-workflow/spec.md | 逐请求采样参数限制 - **GIVEN** 节点保存 temperature、topP、topK 或其他采样选项 - **WHEN** 底层仅暴露 LangChain4j 0.35.0 通用 `ChatLanguageModel` - **THEN** 系统保留节点配置但使用已注册模型 Bean 的构建参数 - **AND** 可通过不同模型 key 选择具有不同采样配置的模型实例 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A60 | passed | specs/tinyflow-workflow/spec.md | 默认启动 - **GIVEN** classpath 包含 Tinyflow 且未定义自有 Bean - **WHEN** Spring Boot 应用启动 - **THEN** 自动配置创建完整执行所需 Bean - **AND** 默认同步超时为 5 分钟 - **AND** 默认内存状态仓库仅承诺单进程生命周期内恢复 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A61 | passed | specs/tinyflow-workflow/spec.md | 条件和覆盖 - **GIVEN** 应用关闭 Tinyflow 或提供自定义仓库/Provider Bean - **WHEN** 自动配置生效 - **THEN** 总开关关闭时不创建 Tinyflow Bean - **AND** 自定义 Bean 替换对应默认 Bean | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A62 | passed | specs/tinyflow-workflow/spec.md | 静态 Provider 生命周期 - **WHEN** 应用上下文启动和关闭 - **THEN** 系统注册当前 LangChain4j LLM Provider - **AND** 关闭时从 Tinyflow `LlmManager` 注销，避免重复注册 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A63 | passed | specs/tinyflow-workflow/spec.md | 保存有效定义 - **GIVEN** 工作流包含有效 `nodes`、`edges` 和可选 `viewport` - **WHEN** 用户保存资源 - **THEN** 插件先完成 JSON、节点、连线和起止路径校验 - **AND** 保存成功后立即刷新内存定义 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A64 | passed | specs/tinyflow-workflow/spec.md | 拒绝无效定义 - **GIVEN** 工作流 JSON 非法、存在未知节点、缺少节点/连线或无完整起止路径 - **WHEN** 用户保存资源 - **THEN** 系统返回包含资源路径、节点 ID 和节点 type 的可诊断错误 - **AND** 不覆盖当前已保存版本 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A65 | passed | specs/tinyflow-workflow/spec.md | 路径解析 - **GIVEN** MagicScript 使用资源分组路径调用工作流 - **WHEN** 路径唯一匹配资源 - **THEN** 系统按资源 ID 注册并执行定义 - **AND** 路径不存在、歧义或资源已删除时明确失败 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A66 | passed | specs/tinyflow-workflow/spec.md | 同步测试运行 - **GIVEN** 用户已保存工作流并提供输入变量 - **WHEN** 调用同步执行 - **THEN** 系统在有限超时内返回清理内部状态后的结果 - **AND** 超时或线程中断产生受控错误且恢复中断标记 - **AND** 超时只终止调用方等待，不承诺强制终止底层工作流或已开始的外部副作用 - **AND** 可能长时间运行的流程使用异步执行与恢复接口 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A67 | passed | specs/tinyflow-workflow/spec.md | 异步和恢复 - **GIVEN** 工作流包含挂起节点 - **WHEN** 调用异步执行后按实例 ID 恢复 - **THEN** 系统返回实例 ID并从默认状态仓库恢复执行 - **AND** API 明确默认内存仓库不支持跨重启恢复 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A68 | passed | specs/tinyflow-workflow/spec.md | 未保存修改 - **GIVEN** 设计器存在未保存修改 - **WHEN** 用户发起测试运行、切换资源或关闭设计器 - **THEN** 系统要求先保存或明确放弃修改 - **AND** 测试始终使用后端已保存定义 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A69 | passed | specs/tinyflow-workflow/spec.md | 注册自定义节点 - **GIVEN** 业务提供唯一节点 type 的后端 Provider 和匹配的前端节点描述 - **WHEN** 应用启动并打开设计器 - **THEN** 节点可在设计器中创建、保存并由后端执行 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A70 | passed | specs/tinyflow-workflow/spec.md | 节点冲突或缺失 - **GIVEN** 多个 Bean 注册同一 type 且无法确定优先级，或工作流引用未安装 type - **WHEN** 构建解析器或执行工作流 - **THEN** 冲突在启动时失败并列出相关 Bean - **AND** 未安装节点在执行前明确失败，不得静默忽略 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A71 | passed | specs/tinyflow-workflow/spec.md | 模型和向量选项 - **WHEN** 设计器请求资源选项 - **THEN** LLM 节点只看到现有 CHAT 模型 - **AND** 向量化只看到现有 EMBEDDING 模型 - **AND** 知识库只看到现有向量库与 Provider - **AND** 响应不包含密钥或完整连接属性 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A72 | passed | specs/tinyflow-workflow/spec.md | Magic 动态资源 - **GIVEN** Magic-API 已注册动态模型或向量数据源 - **WHEN** Tinyflow 插件处于应用中 - **THEN** 插件 Provider 同时暴露 Spring 注册资源和 Magic 动态资源 - **AND** Starter 不反向依赖 Magic 动态数据源类 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A73 | passed | specs/tinyflow-workflow/spec.md | 构建和打包 - **WHEN** 执行 Starter 前端和 Maven 构建 - **THEN** 使用精确版本和 lock 文件完成 `npm ci` - **AND** Starter JAR 包含 `/META-INF/resources/fw-plus/tinyflow-ui/1.3.6/` 下的 JS、CSS 和许可证信息 - **AND** 仓库不提交 `node_modules` | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A74 | passed | specs/tinyflow-workflow/spec.md | 页面加载与卸载 - **WHEN** 用户点击 `toolbars` Tinyflow 入口 - **THEN** 插件在全屏宿主中按当前 context path 加载 Starter UI 并挂载画布 - **AND** 页面卸载时调用 `destroy()` 清理实例和监听器 - **AND** 静态资源缺失或版本不匹配时显示诊断信息而非空白页面 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A75 | passed | specs/tinyflow-workflow/spec.md | MagicScript 调用 - **WHEN** 脚本调用 `tinyflow['group/name'].execute(variables)`、`executeAsync` 或 `resume` - **THEN** 模块路由到同一 `TinyflowService` 并返回稳定结果或错误 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A76 | passed | specs/tinyflow-workflow/spec.md | 管理接口安全 - **WHEN** 未授权用户请求资源、模型列表、保存或测试接口 - **THEN** 系统沿用现有编辑器认证和权限拒绝请求 - **AND** 日志与响应不泄露模型密钥、Authorization、请求头或敏感变量 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A77 | passed | specs/tinyflow-workflow/spec.md | 系统必须在 Core AI 层提供 Tinyflow core、local、dify 模块及聚合模块，在 AI 层提供 Tinyflow Starter 和 Magic-API Tinyflow 插件，并维持 Magic 插件 -> Starter -> Core/Provider 的单向依赖。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A78 | passed | specs/tinyflow-workflow/spec.md | 系统必须提供完全限定名为 `dev.tinyflow.langchain4j.provider.Langchain4jLlm` 的 Java 8 兼容实现，将 Tinyflow 消息转换到 LangChain4j 0.35.0 `ChatLanguageModel`，且运行时只能存在一个该类。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A79 | passed | specs/tinyflow-workflow/spec.md | Starter 必须在 `fw.plus.ai.tinyflow.enabled` 开启时提供解析器、执行器、定义仓库、状态仓库、`TinyflowService`、模型/向量 Provider 和 LLM Provider，并允许业务 Bean 覆盖默认实现。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A80 | passed | specs/tinyflow-workflow/spec.md | Magic-API 插件必须注册 `tinyflow` resource，正文直接保存 `@tinyflow-ai/ui` 导出的原生 JSON，不增加项目自定义外层对象，并以 Magic 资源 ID 作为执行定义 ID。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A81 | passed | specs/tinyflow-workflow/spec.md | 系统必须支持同步执行、异步执行和按实例 ID 恢复，并在执行前解析最新已保存资源。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A82 | passed | specs/tinyflow-workflow/spec.md | Core 必须提供框架无关的 `TinyflowNodeProvider` SPI，Starter 收集节点解析器，前端通过 Tinyflow `customNodes` 注册匹配的节点定义。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A83 | passed | specs/tinyflow-workflow/spec.md | 设计器和执行端必须通过相同的模型、向量和知识库 Provider 暴露及解析现有资源 key，不建设 Tinyflow 私有连接配置。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A84 | passed | specs/tinyflow-workflow/spec.md | Starter 必须通过独立 npm 工程固定使用 `@tinyflow-ai/ui:1.3.6`，将构建产物和许可证物理打入版本化 classpath 目录；Magic 插件只提供宿主和加载器。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |
| A85 | passed | specs/tinyflow-workflow/spec.md | Magic 插件必须注册 `resource` 单数配置、工作流及知识库 `toolbars`、MagicScript `tinyflow` 模块和所需管理/测试接口，并沿用 Magic-API 编辑器权限边界。 | 独立复核正式 brief/spec、当前实现与 Runtime 最新检查，本项实现证据成立且本轮 A37/A38 重构未造成回归。 |

## Checks

| Check | Command | Working directory | Status | Exit | Duration |
| --- | --- | --- | --- | ---: | ---: |
| Tinyflow AI 自动配置定向测试 | -o -pl fw-plus-ai-project/fw-plus-ai-boot-autoconfigure -am -Dtest=MagicTinyflowControllerTest,TinyflowMagicResourceStorageTest,TinyflowResourceDynamicRegistryTest,TinyflowAutoConfigurationTest,TinyflowDifyExternalAuditCoordinatorTest,TinyflowDifyExternalRequestBodyFilterTest,TinyflowDifyExternalControllerTest,TinyflowDifyExternalKnowledgeServiceTest,TinyflowDifyExternalAutoConfigurationTest -DfailIfNoTests=false -Djava.awt.headless=true -DargLine=-Djava.awt.headless=true test | fw-plus-ai | passed | 0 | 23319 ms |
| Git diff 空白检查 | diff --check | . | passed | 0 | 61 ms |

## Blockers

_None._

## Risks and skipped work

- 真实 Dify、真实浏览器交互和最终部署 JAR E2E 属于当前 brief 已确认的后续独立范围。
- Maven settings repository 标签、local POM、重复 headless 和多 SLF4J binding 为既有环境警告，未影响 95 项定向测试。
- 工作区包含与本轮无关的既有修改、删除和未跟踪文件，未据此改变验收结论。

## Previous iterations

| Goal cycle | Iteration | Attempt | Outcome | Unresolved | Summary | Completed |
| ---: | ---: | ---: | --- | --- | --- | --- |
| 1 | 1 | 0 | recovery | A1, A2, A3, A4, A5, A6, A7, A8, A9, A10, A11, A12, A13, A14, A15, A16, A17, A18, A19, A20, A21, A22, A23, A24, A25, A26, A27, A28, A29, A30, A31, A32, A33, A34, A35, A36, A37, A38, A39, A40, A41, A42, A43, A44, A45, A46, A47, A48, A49, A50, A51, A52, A53, A54, A55, A56, A57, A58, A59, A60, A61, A62, A63, A64, A65, A66, A67, A68, A69, A70, A71, A72, A73, A74, A75, A76, A77, A78, A79, A80, A81, A82, A83, A84, A85 | 从旧版 comet.native.v3 build 状态恢复；旧版 Loop、验证结论和运行记录未继承。 | 2026-08-05T00:00:00.000Z |
| 1 | 1 | 1 | pass | — | 独立 Verifier 逐项核对 A1-A85：85 passed、0 failed、0 blocked。重点 A37/A38 的原始字节限额、真实 MVC converter、direct Service 防绕过、Provider 前拒绝、HTTP 顺序、单次委托、稳定错误及安全审计均有实现和 Runtime 95 项测试证据。 | 2026-08-14T08:08:27.777Z |

## Conclusion

独立 Verifier 逐项核对 A1-A85：85 passed、0 failed、0 blocked。重点 A37/A38 的原始字节限额、真实 MVC converter、direct Service 防绕过、Provider 前拒绝、HTTP 顺序、单次委托、稳定错误及安全审计均有实现和 Runtime 95 项测试证据。
