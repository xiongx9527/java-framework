# Tinyflow 知识库能力规格

## Requirement: 统一知识库 SPI

Core 必须提供最小只读 `TinyflowKnowledgeProvider`、可选管理 `TinyflowKnowledgeManagementProvider`、Provider Registry、统一请求/结果及 Tinyflow Knowledge 节点适配器。

#### Scenario: Provider 注册和路由

- **GIVEN** 系统注册 `fw-plus-local` 与 `dify` Provider
- **WHEN** Knowledge 节点以 `provider + knowledgeId` 发起检索
- **THEN** Registry 将请求路由到匹配 Provider
- **AND** 重复 key 在启动时失败
- **AND** Provider 或知识库不存在时明确失败而不是返回空结果

#### Scenario: Provider 能力

- **WHEN** 管理 UI 查询 Provider 能力
- **THEN** `fw-plus-local` 声明完整管理和检索能力
- **AND** `dify` 声明只读检索能力
- **AND** UI 不为只读 Provider 展示上传、切片、重建或删除操作

## Requirement: Provider 实现边界

`fw-plus-local` 必须复用现有模型、Embedding、向量库和对象存储实现完整知识库管理；`dify` 必须位于 Tinyflow dify 模块并依赖现有通用 `fw-plus-dify` 客户端。

#### Scenario: 本地资源复用

- **GIVEN** 系统已有 Embedding 模型、Vector Store 和 S3 资源
- **WHEN** 创建本地知识库
- **THEN** 定义只保存对应资源 key 或对象引用
- **AND** 不创建 Tinyflow 私有连接或保存相关密钥

#### Scenario: Tinyflow 检索 Dify

- **GIVEN** 节点配置 `provider=dify`、连接引用和 Dataset ID
- **WHEN** 执行检索
- **THEN** Provider 通过现有 `fw-plus-dify` Dataset 客户端映射 TopK、阈值和结果
- **AND** 超时、鉴权失败、Dataset 不存在和无效响应转换为稳定且不泄密的 Tinyflow 错误

#### Scenario: 通用 Dify 客户端保持独立

- **WHEN** 检查模块依赖
- **THEN** `fw-plus-dify` 不依赖 Tinyflow
- **AND** Dify 与 Tinyflow 的组合代码只存在于 `fw-plus-ai-tinyflow-dify`

## Requirement: Magic-API 知识库资源

Magic 插件必须用结构化 JSON 注册和管理六类 resource：`tinyflow-knowledge`、`tinyflow-document`、`tinyflow-chunk`、`tinyflow-ingestion-task`、`tinyflow-knowledge-key`、`tinyflow-dify-connection`。

#### Scenario: 资源 CRUD 与版本

- **WHEN** 创建、读取、更新或删除任一知识库资源
- **THEN** 系统使用 Magic-API 的资源 ID、分组、备份、版本和权限能力
- **AND** 并发修改通过资源版本乐观校验

#### Scenario: 敏感信息存储

- **WHEN** 保存资源正文
- **THEN** 模型、向量、S3 和 Dify 只保存安全引用
- **AND** Dify 外部访问密钥只保存强哈希、可识别前缀和授权范围
- **AND** 不保存任何明文 API Key 或 Secret

## Requirement: 四级来源身份

每个 chunk 必须包含非空且语义独立的 `sourceId`、`sourceType`、`documentId`、`chunkId`，并在导入、资源、向量 metadata、检索和外部响应全链路中保持。

#### Scenario: 来源字段校验

- **WHEN** 创建或更新 chunk
- **THEN** 缺少四个字段中的任一个都被拒绝
- **AND** `sourceType` 内置支持 `file`、`text`、`url`、`database`、`api`、`connector`
- **AND** 非空稳定扩展 key 也被允许

#### Scenario: 完整引用信息

- **WHEN** 保存和返回 chunk
- **THEN** 除四级 ID 外还保留 `sourceUri`、`sourceVersion`、定位信息、position、parentChunkId、content、tokenCount、metadata、enabled 和 vectorIds

#### Scenario: 来源删除

- **GIVEN** 一个 `sourceId` 存在多个文档版本
- **WHEN** 删除该来源
- **THEN** 系统找到全部 document、chunk 和 vector ID 并进入可恢复删除流程
- **AND** 删除失败时保留补偿信息，不谎报完成

## Requirement: 文档导入、解析与版本

本地 Provider 必须支持文件、文本、URL 和扩展连接器导入，内置解析 TXT、Markdown、PDF、DOCX、HTML、CSV、XLSX，并支持可替换解析器和普通、Q&A、父子切片策略。

#### Scenario: 幂等导入

- **GIVEN** 相同知识库、`sourceId` 与 checksum 已成功导入
- **WHEN** 再次导入相同内容
- **THEN** 系统复用当前文档版本
- **AND** 不重复解析、Embedding 或向量写入

#### Scenario: 内容更新

- **GIVEN** 已存在可检索的旧文档版本
- **WHEN** 同一 `sourceId` 以新 checksum 导入
- **THEN** 系统生成新 `documentId` 和 chunk
- **AND** 新索引全部成功后原子切换可检索版本
- **AND** 任一阶段失败时旧版本仍可检索

#### Scenario: 异步任务状态

- **WHEN** 执行导入、重建或删除
- **THEN** 任务在 `PENDING`、`PARSING`、`CHUNKING`、`EMBEDDING`、`INDEXING`、`COMPLETED`、`FAILED`、`CANCELLING`、`CANCELLED` 等状态间合法转换
- **AND** 保存阶段、进度、处理数量、重试次数、时间和安全错误

#### Scenario: 向量补偿

- **GIVEN** 向量写入成功但资源保存失败
- **WHEN** 任务进入失败恢复
- **THEN** 系统保存待补偿 vector ID 并重试删除
- **AND** 不产生不可追踪向量

## Requirement: 本地检索链路

`fw-plus-local` 必须支持向量、关键词和混合检索，并依次处理可选查询改写、候选召回、Metadata 过滤、融合去重、分数归一化、可选 Rerank、父子展开、阈值和 TopK。

#### Scenario: 完整检索结果

- **WHEN** 检索命中有效 chunk
- **THEN** 每项结果包含 content、score、title、四级来源 ID、来源定位和原始 metadata
- **AND** 停用知识库、文档或 chunk 不参与召回

#### Scenario: 向量库不支持 Metadata 过滤

- **GIVEN** 当前 Vector Store 不支持服务端 Metadata 过滤
- **WHEN** 请求带 metadata 条件
- **THEN** 系统按可配置倍数扩大候选并从资源仓库二次过滤
- **AND** 候选数量受硬上限约束，不执行无界扫描

#### Scenario: Vector Store 隔离

- **WHEN** 创建本地知识库
- **THEN** 默认一个知识库独占一个 Vector Store
- **AND** 只有具体实现明确支持知识库过滤时才允许共享

## Requirement: Dify External Knowledge API

系统必须在显式启用且存在 Access Key Repository 时发布可配置基础路径下的 `POST /retrieval`，实现现有 `fw-plus-dify` 的 `IKnowledgeService` 契约并且只路由到 `fw-plus-local`。

#### Scenario: 合法检索请求

- **GIVEN** 请求携带有效 Bearer Key、授权范围内的 `knowledge_id`、query、整数 `top_k` 和 0..1 `score_threshold`
- **WHEN** 调用 `/retrieval`
- **THEN** 服务限制 TopK、超时和频率后调用本地 Provider
- **AND** 返回 HTTP 200 和 `records` 数组

#### Scenario: Dify 响应 metadata

- **WHEN** 外部检索返回一条或多条记录
- **THEN** 每条记录包含 `content`、`score`、`title` 和非 null 对象 `metadata`
- **AND** metadata 强制包含 `source_id`、`source_type`、`document_id`、`chunk_id`
- **AND** 无结果返回 HTTP 200 和空数组

#### Scenario: Metadata 条件

- **WHEN** 请求包含 `metadata_condition`
- **THEN** 系统支持 `and`、`or` 逻辑
- **AND** 支持 contains、not contains、start with、end with、is、is not、in、not in、empty、not empty、等于、不等于、大小比较、before、after 运算

#### Scenario: 认证和错误契约

- **GIVEN** 鉴权头无效、密钥无效、越权、知识库不存在、超时或内部错误
- **WHEN** 调用外部接口
- **THEN** 系统返回稳定非 200 状态及 `error_code`、`error_msg`
- **AND** 不泄露凭证或敏感正文

#### Scenario: 防止循环调用

- **GIVEN** 外部请求的目标试图解析为 `dify` Provider
- **WHEN** 服务路由请求
- **THEN** 系统拒绝请求
- **AND** 不向上游 Dify 发起二次检索

## Requirement: 本地知识库管理 UI

Starter 内置 UI 必须提供参考 Dify/RAGFlow 的完整本地知识库管理工作台，Magic 插件通过独立 `toolbars` 入口加载，并根据 Provider 能力控制操作。

#### Scenario: 本地知识库工作台

- **WHEN** 用户打开 `fw-plus-local` 知识库
- **THEN** 页面提供概览、文档、切片、召回测试、API 接入、任务和设置
- **AND** 支持文件/文本/URL/批量导入、切片预览编辑、启停、Metadata、父子关系和来源定位
- **AND** 支持重建、删除、重试、取消和进度查看

#### Scenario: API 和连接管理

- **WHEN** 用户管理外部接入
- **THEN** 可创建、轮换、吊销 API Key，明文只显示一次
- **AND** 展示 Dify Endpoint 和 External Knowledge ID
- **AND** Dify Provider 页面仅管理服务地址、凭证引用、Dataset ID 和连通性测试

#### Scenario: 资源选项

- **WHEN** 用户选择 Embedding 模型、Vector Store 或对象存储
- **THEN** UI 从现有 Magic/Spring Provider 获取非敏感 key、名称、类型和 provider
- **AND** 不在知识库页面重复提供这些数据源的 CRUD

## Requirement: 租户、权限和安全

知识库、文档、chunk、任务、连接和密钥必须携带 tenantId；管理接口、外部接口和资源仓库必须执行租户与资源权限隔离。

#### Scenario: 跨租户访问

- **GIVEN** 两个租户使用相同 knowledgeId、sourceId 或 chunkId
- **WHEN** 任一租户读取、修改、检索或删除
- **THEN** 只能影响当前租户授权范围内资源

#### Scenario: URL 导入防护

- **WHEN** 用户提交 URL 来源
- **THEN** 系统限制协议、目标地址、重定向、文件大小、响应类型和超时
- **AND** 阻止访问本机、内网及云元数据地址

#### Scenario: 资源和请求限额

- **WHEN** 上传、解析或外部检索超过格式、大小、数量、解压规模、并发、内存、TopK、候选数、超时或频率限制
- **THEN** 系统在执行危险或无界工作前拒绝或终止请求

#### Scenario: 审计和脱敏

- **WHEN** 创建、更新、删除、导入、重建、密钥变更、连接测试或外部检索发生
- **THEN** 系统记录不含凭证和完整敏感正文的审计事件
- **AND** 普通日志与错误不输出 API Key、Authorization、S3/模型/向量凭证

## Requirement: 错误和恢复

系统必须区分 Provider/知识库/模型/向量库不存在、无权限、解析失败、索引失败、检索失败、上游 Dify 失败和超时，并提供可恢复导入、重建和删除语义。

#### Scenario: 原子重建

- **GIVEN** 当前索引可用
- **WHEN** 重建新索引失败
- **THEN** 系统保留旧索引和失败任务用于诊断及重试

#### Scenario: 延迟删除

- **GIVEN** 删除向量失败
- **WHEN** 删除文档或来源
- **THEN** 资源进入 `DELETE_PENDING` 并创建补偿任务
- **AND** API 不报告删除已完全完成

## Requirement: 可选扩展能力

Core 必须定义可替换的文档解析、切片、Rerank、对象存储、凭证、审计和 GraphRAG SPI；首版不得注册默认 Neo4j 或 GraphRAG 实现。

#### Scenario: 无 GraphRAG 实现

- **GIVEN** 应用未注册 `TinyflowKnowledgeGraphProvider`
- **WHEN** 使用默认本地知识库
- **THEN** 向量、关键词和混合检索正常工作
- **AND** classpath 不需要 Neo4j

#### Scenario: 自定义扩展

- **GIVEN** 业务注册兼容 SPI 的解析器、Reranker 或 Graph Provider
- **WHEN** 对应知识库配置启用该能力
- **THEN** 系统使用扩展实现并维持租户、来源 ID、安全和错误契约
