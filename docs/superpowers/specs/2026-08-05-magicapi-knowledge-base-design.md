# Tinyflow 本地知识库与 Dify 接口设计

## 背景

fw-plus 需要在 Tinyflow 集成中提供统一知识库能力。Tinyflow 只定义知识库 SPI，通过 Provider 支持不同后端；当前实现固定为 `fw-plus-local` 和 `dify`。`fw-plus-local` 使用项目已有模型、Embedding、向量库、Magic-API 资源存储和 S3，`dify` 复用现有 `fw-plus-dify` 客户端。

Magic-API Tinyflow 插件提供以本地知识库为主的管理 UI。除了供 Tinyflow Knowledge 节点检索，本地知识库还要实现 Dify External Knowledge API，使 Dify 能把 fw-plus 注册为外部知识库并调用 `POST /retrieval`。

本设计面向当前 `dev` 分支，保持 Java 8、Spring Boot 2.7.18、Magic-API 2.2.2 和 LangChain4j 0.35.0 兼容，不修改 `springboot2` 或 `springboot3` 分支。

## 目标

- 在 Tinyflow 模块族中定义稳定、框架无关的知识库 SPI。
- 提供 `fw-plus-local` 和 `dify` 两个 Provider 实现。
- 本地实现复用现有模型、Embedding、向量库、Magic `resource` 和 S3，不重复建设数据源管理。
- 提供知识库、文档、切片、索引、检索、任务、权限和审计的完整管理能力。
- 通过 Magic-API `toolbars` 提供参考 Dify/RAGFlow 工作流的全屏知识库管理 UI。
- 实现 Dify External Knowledge API，让 Dify 检索 `fw-plus-local` 知识库。
- 每个 chunk 强制携带稳定的来源 ID 和来源类型，支持幂等更新、删除和引用追踪。
- 支持 Tinyflow 工作流设计完成后真实执行 Knowledge 节点并验证召回结果。

## 非目标

- 不复制 Dify 或 RAGFlow 前端源码，只参考其信息架构和操作流程。
- 不在知识库 UI 中重复提供模型、Embedding 或向量数据源 CRUD。
- 不迁移或复制现有 `fw-plus-dify` 客户端代码。
- 不要求 Dify 能通过 External Knowledge API 修改本地知识库；Dify 官方协议只提供检索。
- 不让通用 `fw-plus-dify` 模块反向依赖 Tinyflow。
- 不强制依赖 Neo4j。GraphRAG 仅保留可选 SPI，不进入默认本地检索链路。
- 不新增关系数据库表。运行数据使用 Magic `resource`，原始文件使用现有 S3。

## 模块与依赖

Core AI 中的 Tinyflow 目录调整为 Maven 聚合模块：

```text
fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-tinyflow/
├── fw-plus-ai-tinyflow-core/
├── fw-plus-ai-tinyflow-local/
└── fw-plus-ai-tinyflow-dify/
```

依赖方向固定为：

```text
fw-plus-ai-tinyflow-local
    -> fw-plus-ai-tinyflow-core
    -> 现有 fw-plus-ai-model / fw-plus-ai-store-embedding 契约

fw-plus-ai-tinyflow-dify
    -> fw-plus-ai-tinyflow-core
    -> 现有 fw-plus-dify

fw-plus-ai-tinyflow-spring-boot-starter
    -> fw-plus-ai-tinyflow-local
    -> fw-plus-ai-tinyflow-dify

fw-plus-ai-magicapi-plugin-tinyflow
    -> fw-plus-ai-tinyflow-spring-boot-starter
```

`fw-plus-ai-tinyflow-core` 不依赖 `fw-plus-dify`、Spring、Magic-API、Servlet 或具体向量数据库。`fw-plus-dify` 保持通用 Dify 客户端定位，不依赖 Tinyflow。Dify 与 Tinyflow 的组合代码全部位于 `fw-plus-ai-tinyflow-dify`。

## 知识库 SPI

核心检索接口保持最小化：

```java
public interface TinyflowKnowledgeProvider {
	String getKey();
	TinyflowKnowledgeCapabilities capabilities();
	TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request);
}
```

管理能力通过扩展接口提供，不要求只读 Provider 实现无意义的 CRUD：

```java
public interface TinyflowKnowledgeManagementProvider extends TinyflowKnowledgeProvider {
	TinyflowKnowledgeDefinition create(TinyflowKnowledgeDefinition definition);
	TinyflowKnowledgeDefinition update(TinyflowKnowledgeDefinition definition);
	void delete(String tenantId, String knowledgeId);
	TinyflowIngestionTask importDocument(TinyflowDocumentImport request);
}
```

Core 同时定义：

- `TinyflowKnowledgeProviderRegistry`：按 `providerKey` 注册和解析 Provider，重复 key 启动失败。
- `TinyflowKnowledgeRepository`：知识库、文档、chunk、任务和向量 ID 的持久化契约。
- `TinyflowKnowledgeAccessKeyRepository`：Dify 外部访问密钥、租户和知识库授权范围。
- `TinyflowDifyConnectionRepository`：Dify 连接、凭证引用和 Dataset ID。
- `TinyflowDocumentParser`：按格式解析文件、文本或 URL。
- `TinyflowChunkStrategy`：普通、Q&A、父子等切片策略。
- `TinyflowReranker`：可选 Rerank 能力。
- `TinyflowKnowledgeGraphProvider`：可选 GraphRAG 能力，不提供默认实现。

统一检索请求至少包含 `providerKey`、`tenantId`、`knowledgeId`、`query`、`topK`、`scoreThreshold`、检索模式、Metadata 条件和可选 Rerank 参数。统一结果包含 chunk、分数、标题、来源和 Metadata，不暴露底层向量库对象。

## Provider 实现

### fw-plus-local

`FwPlusLocalKnowledgeProvider` 实现完整管理接口，Provider key 固定为 `fw-plus-local`。它通过 `TinyflowModelProvider` 和 `TinyflowVectorStoreProvider` 解析现有模型与向量库，通过 `TinyflowKnowledgeRepository` 保存业务数据。

本地实现不创建私有模型配置或私有向量连接。知识库定义只保存现有 Embedding 模型 key 和 Vector Store key，UI 与执行端必须使用同一 Provider 解析这些 key。

### dify

`DifyTinyflowKnowledgeProvider` 位于 `fw-plus-ai-tinyflow-dify`，Provider key 固定为 `dify`。它依赖现有 `fw-plus-dify`，使用已有 Dataset 客户端检索 Dify 知识库，并将 Dify 结果映射为 Tinyflow 统一结果。

Dify Provider 默认声明只读能力。知识库管理 UI 只提供服务地址、凭证引用、Dataset ID 和连通性测试，不复制 Dify 的知识库、文档和切片管理界面。

## Magic-API 资源模型

Magic-API Tinyflow 插件注册以下资源类型：

- `tinyflow-knowledge`：知识库定义、Provider、模型 key、向量库 key、切片和检索策略。
- `tinyflow-document`：文档版本、来源、S3 原文地址、checksum、解析状态和错误。
- `tinyflow-chunk`：切片正文、父子关系、来源、Metadata、启用状态和向量 ID。
- `tinyflow-ingestion-task`：导入、解析、向量化、重建和删除任务。
- `tinyflow-knowledge-key`：Dify 外部访问密钥哈希、前缀、租户和知识库授权范围。
- `tinyflow-dify-connection`：Dify 服务地址、凭证引用、Dataset ID 和连接状态。

资源保存结构化 JSON，使用 Magic-API 的资源 ID、分组、备份和权限能力。资源正文不得保存模型密钥、向量库密钥、S3 Secret 或 Dify 明文 API Key。

原始文件写入现有 S3，文档资源只保存对象标识、原文件名、媒体类型、大小和 checksum。文本粘贴可保存为受控的文本对象，仍生成稳定来源标识。

## 来源与版本标识

每个 chunk 必须同时保存三个不同语义的 ID，并强制保存来源类型：

- `sourceId`：原始来源的稳定 ID。上传文件未提供业务 ID 时由系统首次导入生成；URL、连接器或业务数据可传入稳定外部 ID。
- `sourceType`：来源类别，内置值至少包含 `file`、`text`、`url`、`database`、`api` 和 `connector`；扩展类型必须使用稳定 key。
- `documentId`：一次导入或一次文档版本的内部 ID。相同 `sourceId` 更新内容时生成新版本。
- `chunkId`：当前文档版本中一个切片的 ID。

chunk 还保存 `sourceUri`、`sourceVersion`、页码/工作表等定位信息、position、parentChunkId、content、tokenCount、Metadata、enabled 和 vectorIds。

`sourceId + checksum` 用于幂等判断：内容未变化时不重复解析和向量化；内容变化时创建新文档版本并构建新索引。切换成功后再清理旧版本，失败时保留旧版本可检索。

删除来源时按 `sourceId` 找到所有文档版本、chunk 和 vector ID。引用和 Dify 响应 Metadata 至少包含：

```json
{
  "source_id": "source-001",
  "document_id": "document-v2",
  "chunk_id": "chunk-019",
  "source_type": "file",
  "page": 12
}
```

## 文档导入与索引

支持文件上传、文本粘贴、URL 和扩展连接器导入。内置解析范围为 TXT、Markdown、PDF、DOCX、HTML、CSV 和 XLSX；OCR、版面、表格和图片提取通过 `TinyflowDocumentParser` 扩展并记录实际解析器版本。

处理流程：

```text
创建来源和文档版本
    -> 原文写入 S3
    -> 异步解析
    -> 普通/Q&A/父子切片
    -> 切片预览或自动确认
    -> 现有 Embedding 模型生成向量
    -> 写入现有 Vector Store
    -> 保存 chunk 与 vector ID 映射
    -> 原子切换可检索版本
```

任务状态至少包含 `PENDING`、`PARSING`、`CHUNKING`、`EMBEDDING`、`INDEXING`、`COMPLETED`、`FAILED`、`CANCELLING`、`CANCELLED`。任务记录阶段、进度、已处理数量、重试次数、开始/结束时间和最后错误。

删除和重建采用可恢复流程：先标记处理中，再操作向量，成功后更新资源；失败保留任务和旧索引。向量写入成功但资源保存失败时记录待补偿 vector ID，重试删除，避免不可追踪向量。

现有通用向量接口不能列出全部向量，也不保证 Metadata 过滤，因此资源仓库必须保存完整 chunk 和 vector ID。默认一个本地知识库独占一个 Vector Store；只有具体向量实现明确支持知识库过滤时，才允许多个知识库共享。

## 检索链路

`fw-plus-local` 支持向量、关键词和混合检索。完整链路为：

```text
查询改写（可选）
    -> 向量/关键词/混合候选召回
    -> Metadata 条件过滤
    -> 融合、去重和分数归一化
    -> Rerank（可选）
    -> 父子切片展开
    -> scoreThreshold 和 TopK 截断
```

通用向量库不支持 Metadata 过滤时，向量召回扩大候选数，再从资源仓库加载 chunk Metadata 完成过滤、排序和截断。候选扩大倍数和最大候选数必须可配置并有硬上限，避免无界扫描。

检索结果必须保留 `sourceId`、`sourceType`、`documentId`、`chunkId`、标题、来源定位和原始 Metadata。停用的知识库、文档或 chunk 不参与召回。找不到 Provider、知识库、模型或向量库时立即返回明确错误，不能沿用 Tinyflow Knowledge 节点找不到知识库时静默返回空结果的行为。

## Dify 双向集成

### Tinyflow 调用 Dify

Tinyflow Knowledge 节点保存：

```json
{
  "provider": "dify",
  "knowledgeId": "dify-dataset-id"
}
```

`DifyTinyflowKnowledgeProvider` 使用对应连接配置调用现有 `fw-plus-dify` Dataset 检索接口，映射 TopK、阈值和检索结果。网络超时、鉴权失败、Dataset 不存在和无效响应转换为稳定 Tinyflow 错误，日志不得输出 Dify API Key。

### Dify 调用 fw-plus-local

AI Boot 自动配置在 Dify/Tinyflow 适配类存在时发布：

```text
POST {base-path}/retrieval
Authorization: Bearer {API_KEY}
Content-Type: application/json
```

`base-path` 可配置，Dify 注册时填写基础地址，Dify 自动追加 `/retrieval`。Web 层提取 Bearer token 后调用 `TinyflowDifyExternalKnowledgeService`；该服务实现现有 `fw-plus-dify` 的 `IKnowledgeService` 契约并路由到 `fw-plus-local`。

外部接口使用 `fw.plus.ai.tinyflow.dify.external` 配置前缀，至少提供 `enabled`、`base-path`、`request-timeout`、`max-top-k` 和 `rate-limit`。默认不发布匿名接口；只有显式启用且存在 Access Key 仓库时才注册 Web 入口。

请求严格支持：

- `knowledge_id`：本地知识库 ID。
- `query`：查询文本。
- `retrieval_setting.top_k`：整数。
- `retrieval_setting.score_threshold`：0 到 1。
- `metadata_condition.logical_operator`：`and` 或 `or`。
- Metadata 比较操作：`contains`、`not contains`、`start with`、`end with`、`is`、`is not`、`in`、`not in`、`empty`、`not empty`、`=`、`≠`、`>`、`<`、`≥`、`≤`、`before`、`after`。

响应固定为 `records` 数组，每项包含 `content`、`score`、`title` 和对象类型的 `metadata`。Metadata 不得为 `null`，并强制包含 `source_id`、`source_type`、`document_id` 和 `chunk_id`。

现有 external DTO 需要与官方契约对齐：`top_k` 从 `String` 修正为 `Integer`，增加 `metadata_condition`，错误字段使用 `error_code` 和 `error_msg`。无结果返回 HTTP 200 和空数组。鉴权头格式错误、鉴权失败和知识库不存在分别使用稳定错误码，非成功响应使用非 200 HTTP 状态。

外部接口只允许路由 `fw-plus-local`，不得转发到 `dify` Provider，防止 Dify 调用 fw-plus 后再次调用 Dify 形成循环。

## Magic-API 管理 UI

Starter 物理打包 npm 前端工程和构建后的 JS/CSS，Magic-API Tinyflow 插件通过 `toolbars` 注册“知识库”入口、加载 Starter UI 资源并提供后端管理接口。完整管理 UI 主要服务 `fw-plus-local`。

全屏工作台包含：

- 知识库列表：名称、Provider、文档数、chunk 数、索引状态、更新时间和操作。
- 本地知识库详情：概览、文档、切片、召回测试、API 接入、任务和设置。
- 文档导入：文件、文本、URL、批量上传、解析器和切片策略。
- 切片管理：原文对照、预览、编辑、启用/禁用、Metadata、父子关系和来源定位。
- 召回测试：查询、TopK、阈值、检索模式、Rerank、分数和引用来源。
- API 接入：创建/吊销 API Key，显示 Dify Endpoint 和 External Knowledge ID。
- 任务中心：解析、向量化、重建和删除任务的进度、错误、重试与取消。
- 设置：现有 Embedding 模型、Vector Store、切片策略和检索策略。

模型和向量选项分别来自现有 `MagicDynamicModelDataSource` 和 `MagicDynamicVectorDataSource`，只返回 key、名称、类型和 provider 等非敏感信息。知识库 UI 不提供其 CRUD，用户继续在现有 Model/Vector 插件中管理。

Dify Provider 页面只显示服务地址、凭证引用、Dataset ID 和连通性测试。UI 根据 `capabilities()` 决定操作，不为只读 Provider 展示上传、切片或重建按钮。

## 权限与安全

- 知识库、文档、chunk、任务、Dify 连接和密钥均带 `tenantId`，管理接口执行租户和资源权限校验。
- Dify 外部 API Key 只保存强哈希、可识别前缀和授权范围，创建时明文仅显示一次，支持轮换和吊销。
- Dify 远程凭证通过凭证 SPI 保存，资源正文只保存凭证引用。
- URL 导入限制协议、目标地址、重定向、文件大小、响应类型和超时，阻止访问本机、内网和云元数据地址。
- 上传限制格式、大小、数量和解压规模；解析任务限制并发、执行时间和内存使用。
- 外部检索限制请求体、TopK、候选数、超时和调用频率。
- 审计创建、更新、删除、导入、重建、密钥变更、Provider 连接测试和外部检索。
- 日志和错误不得输出 API Key、Authorization、S3/模型/向量凭证或完整敏感文档内容。

## 错误与恢复

内部错误至少区分 Provider 不存在、知识库不存在、无权限、模型不存在、向量库不存在、解析失败、索引失败、检索失败、上游 Dify 失败和超时。错误包含可定位的资源 ID 和任务阶段，不包含凭证。

并发修改使用资源版本进行乐观校验。导入和重建使用新文档/索引版本，全部完成后原子切换；失败保留旧版本。删除向量失败时资源进入 `DELETE_PENDING` 并创建补偿任务，不谎报删除完成。

## 测试策略

### Core 与 Provider

- 验证 Provider 注册、重复 key、缺失 Provider、能力判断和循环路由保护。
- 验证 `sourceId`、`sourceType`、`documentId`、`chunkId` 的生成、版本更新、幂等和删除范围。
- 验证普通、Q&A、父子切片以及 chunk 编辑、启停和 Metadata。
- 验证向量、关键词、混合检索、候选扩大、融合、阈值、Rerank 和父子展开。
- 验证向量写入、资源写入、重建和删除失败的补偿路径。
- 验证 Dify Provider 通过现有 `fw-plus-dify` 客户端完成请求映射和错误转换。

### Dify 契约

- 使用 HTTP 契约测试验证 Bearer 鉴权、`knowledge_id`、TopK、阈值和全部 Metadata 运算符。
- 验证响应 `metadata` 永不为 `null`，且包含 `source_id`、`source_type`、`document_id` 和 `chunk_id`。
- 验证无结果、无效鉴权、无权限、知识库不存在、超时和内部错误。
- 验证外部 `/retrieval` 不能路由到 `dify` Provider。
- 使用与 Dify 官方示例相同的 JSON 请求完成端到端检索。

### Magic-API 与 UI

- 验证六类知识库资源的创建、读取、更新、删除、版本和权限隔离。
- 验证模型与向量库选项来自现有插件且不泄露敏感配置。
- 执行 npm 安装、单元测试和生产构建，检查 Starter JAR 包含 UI 资源和 lock 文件对应版本。
- 使用 Playwright 验证知识库列表、导入、切片预览、任务、召回测试、API Key 和 Dify 连接测试。
- 在桌面和移动视口检查工具栏、表格、对话框和长文本不重叠；管理工作流以桌面体验为主要验收目标。

### 集成

- 启动 AI Magic-API sample，上传真实 TXT、PDF、DOCX、HTML 和表格文档。
- 使用现有 Embedding 模型和 Vector Store 完成索引与检索，不创建 Tinyflow 私有模型或向量连接。
- 在 Tinyflow 工作流中分别执行 `fw-plus-local` 和 `dify` Knowledge 节点。
- 使用真实 HTTP 请求调用 `/retrieval`，验证 Dify JSON、来源 ID、权限和引用。
- 验证 Java 8 字节码、Spring Boot 2.7 上下文、Magic-API 2.2.2 插件元数据和最终依赖树。

## 构建顺序

1. 构建现有 `fw-plus-dify` 的契约修正和回归测试。
2. 构建 Core AI 的 Tinyflow core、local、dify 子模块及 BOM。
3. 构建 AI Boot 自动配置和 Tinyflow Starter。
4. 构建 Magic-API Tinyflow 插件及 npm UI。
5. 构建并启动 AI Magic-API sample，执行 HTTP 和浏览器端到端测试。

## 完成标准

只有同时满足以下条件才算完成：Tinyflow core、local、dify 模块依赖方向正确；`fw-plus-dify` 不依赖 Tinyflow；本地知识库完整管理 UI 可用；模型、Embedding、向量库和 S3 均复用现有能力；每个 chunk 强制保存并返回 `sourceId`、`sourceType`、`documentId` 和 `chunkId`；本地导入、切片、索引、检索、重建和恢复通过测试；Tinyflow 可分别调用 local 与 Dify Provider；Dify 可通过兼容的 `/retrieval` 检索 local；Dify 外部协议、权限、安全和错误契约通过 HTTP 测试；Java 8、Spring Boot 2.7、Magic-API 2.2.2、npm 构建和浏览器验证全部通过；其他分支未被修改。
