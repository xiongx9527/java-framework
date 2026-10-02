# easy-rag 知识库 完整目标规格（实现 change）

> **文档角色**：本文档承接设计文档（easy-rag-knowledge 归档定稿）作为实现 change 的完整目标规格，内容与设计定稿一致。实现验收以 brief 的 A1–A17 与本文各章节为准。DDL（§2.2）在 Build 第一批落地为 `META-INF/sql/fw-plus-ai-knowledge.sql`。管理端与对外 API 同属 content-retriever 插件同一服务（§3.3），API 实现 Sa-Token 认证授权。

fw-plus-ai 层的知识库能力：Dify 风格的知识库管理 + magic 控制台 UI + 对外 API 服务（业务对接与 Dify External Knowledge API 兼容）+ knowledge 类型检索器。参照 Dify 知识库全功能设计，数据模型采用 数据集 → 文档 → 分段 三层。

## 1. 模块与包结构

不新增 Maven 模块（用户决定 D1）。

- 后端：`fw-plus-ai-boot-autoconfigure` 的 content-retriever 后端实现包 `com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever` 下新增 `knowledge` 子包（用户决定 2026-10-02：knowledge 的 Java 实现与检索器实现同包体系，即 content-retriever 后端）
  - `KnowledgePluginConfiguration`（@Import 进 MagicAPIBaseAutoConfiguration）
  - `entity/` KbDataset、KbDocument、KbSegment（继承 MpBaseEntity）
  - `mapper/` KbDatasetMapper、KbDocumentMapper、KbSegmentMapper（继承 MpBaseMapper）
  - `service/` KbDatasetService、KbDocumentService、KbSegmentService、KbIndexingService、KbSearchService
  - `pipeline/` DocumentPipeline、KbIndexingExecutor、`splitter/ParentChildSplitter`、`qa/QaExtractor`
  - `store/` KbOriginalFileStore 接口 + LocalKbOriginalFileStore
  - `retriever/` KnowledgeContentRetriever、KnowledgeContentRetrieverProvider
  - `web/` KbConsoleController（管理端接口）、KbOpenApiController（对外 API，同一服务）
  - `auth/` KbSaTokenAuthChecker（对外 API 的 Sa-Token 认证授权校验）
- 前端：现有 `fw-plus-ai-magicapi-plugin-langchain4j-content-retriever` 插件 console 扩展
- SQL：根目录 `META-INF/sql/fw-plus-ai-knowledge.sql`

## 2. 数据模型与索引管道

### 2.1 装配与数据源

- 元数据表落在应用主数据源，走 Spring 默认 DataSource + MyBatis-Plus；不单独设计数据源配置。
- KnowledgePluginConfiguration 装配条件：`@ConditionalOnBean(SqlSessionFactory)`；应用未配置 JDBC 数据源时知识库功能整体不装配，log 警告，不影响其余 langchain4j 功能。
- 启动时校验三张表存在性，缺失时警告并禁用知识库功能。
- pom：fw-plus-ai-boot-autoconfigure 补 `fw-plus-mp-base`（optional=true）；确认 tika 版本由 BOM 管理。

### 2.2 表结构（完整 DDL 设计稿）

```sql
-- 知识库数据集
CREATE TABLE kb_dataset (
    id                        VARCHAR(32)  NOT NULL COMMENT '主键',
    name                      VARCHAR(255) NOT NULL COMMENT '数据集名称',
    description               VARCHAR(1024)         DEFAULT NULL COMMENT '描述',
    indexing_technique        VARCHAR(16)  NOT NULL DEFAULT 'high_quality' COMMENT '索引技术：high_quality/economy（economy 预留）',
    embedding_model_key       VARCHAR(255) NOT NULL COMMENT '配置的嵌入模型资源 key（langchain4j embedding-model）',
    embedding_model_active    VARCHAR(255)          DEFAULT NULL COMMENT '上次成功索引实际使用的嵌入模型 key；检索嵌入用此值保证与向量一致；切换配置后提示重建索引，重建完成后更新',
    embedding_store_key       VARCHAR(255) NOT NULL COMMENT '向量库资源 key（langchain4j embedding-store）',
    scoring_model_key         VARCHAR(255)          DEFAULT NULL COMMENT 'rerank 模型资源 key（scoring-model），空则不重排',
    chat_model_key            VARCHAR(255)          DEFAULT NULL COMMENT 'QA 分段抽取用对话模型资源 key（langchain4j chat-model），segment_mode=qa 时必填',
    collection_name           VARCHAR(255)          DEFAULT NULL COMMENT '向量库 collection 名，按数据集隔离；空则按约定生成',
    retrieval_top_k           INT          NOT NULL DEFAULT 4 COMMENT '默认召回数量',
    retrieval_score_threshold DECIMAL(6,4) NOT NULL DEFAULT 0.0000 COMMENT '默认分数阈值',
    segment_max_length        INT          NOT NULL DEFAULT 1000 COMMENT '分段最大长度（字符）',
    segment_overlap           INT          NOT NULL DEFAULT 200 COMMENT '分段重叠长度（字符）',
    segment_delimiter         VARCHAR(32)           DEFAULT NULL COMMENT '自定义分段分隔符，空则仅按递归规则',
    segment_mode              VARCHAR(16)  NOT NULL DEFAULT 'general' COMMENT '分段模式：general 通用（分段直接嵌入）/parent-child 父子（父段落表不嵌入，子段嵌入带 parent_id）/qa 问答抽取（需配置 chat_model_key）',
    status                    VARCHAR(16)  NOT NULL DEFAULT 'active' COMMENT '状态：active/disabled',
    error_msg                 TEXT                  COMMENT '最近错误信息',
    create_time               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time               DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    del_flag                  CHAR(1)      NOT NULL DEFAULT '0' COMMENT '逻辑删除：0 否 1 是',
    PRIMARY KEY (id),
    KEY idx_kb_dataset_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='easy-rag 知识库数据集';

-- 知识库文档
CREATE TABLE kb_document (
    id              VARCHAR(32)  NOT NULL COMMENT '主键',
    dataset_id      VARCHAR(32)  NOT NULL COMMENT '所属数据集 id',
    name            VARCHAR(255) NOT NULL COMMENT '文档名称（原始文件名）',
    file_path       VARCHAR(1024)         DEFAULT NULL COMMENT '原始文件存储路径（KbOriginalFileStore 落盘后回写；create-by-text 时为空）',
    file_ext        VARCHAR(16)           DEFAULT NULL COMMENT '文件扩展名',
    file_size       BIGINT                DEFAULT NULL COMMENT '文件大小（字节）',
    data_source_type VARCHAR(16) NOT NULL DEFAULT 'upload' COMMENT '来源：upload/create-by-text',
    word_count      INT                   DEFAULT 0 COMMENT '词数（近似）',
    latency_ms      BIGINT                DEFAULT NULL COMMENT '索引耗时（毫秒）',
    indexing_status VARCHAR(16)  NOT NULL DEFAULT 'waiting' COMMENT '索引状态：waiting/parsing/splitting/indexing/completed/error',
    error_msg       TEXT                  COMMENT '索引失败原因',
    enabled         CHAR(1)      NOT NULL DEFAULT '1' COMMENT '启用：1 是 0 否（禁用不参与检索）',
    archived        CHAR(1)      NOT NULL DEFAULT '0' COMMENT '归档：1 是 0 否（归档不参与检索，与启禁用独立）',
    metadata        TEXT                  COMMENT '文档自定义元数据 JSON（键值对，检索过滤用）',
    doc_hash        VARCHAR(64)           DEFAULT NULL COMMENT '内容 SHA-256，数据集内去重',
    create_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_document_dataset (dataset_id, indexing_status),
    UNIQUE KEY uk_kb_document_hash (dataset_id, doc_hash)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='easy-rag 知识库文档';

-- 知识库分段
CREATE TABLE kb_segment (
    id               VARCHAR(32)  NOT NULL COMMENT '主键（同时作为向量库 vector id）',
    dataset_id       VARCHAR(32)  NOT NULL COMMENT '所属数据集 id',
    document_id      VARCHAR(32)  NOT NULL COMMENT '所属文档 id',
    position         INT          NOT NULL DEFAULT 0 COMMENT '分段序号',
    content          TEXT         NOT NULL COMMENT '分段文本（持久化，重嵌入无需重解析原文）',
    answer           TEXT                  COMMENT 'QA 模式的答案；通用/父子分段为空',
    keywords         VARCHAR(512)          DEFAULT NULL COMMENT '分段关键词（逗号分隔，管理端可编辑）',
    word_count       INT                   DEFAULT 0 COMMENT '词数（近似）',
    tokens           INT                   DEFAULT 0 COMMENT 'token 估计值（KbTokenCounter 字符近似，M1）',
    index_node_id    VARCHAR(128)          DEFAULT NULL COMMENT '向量库节点 id（嵌入成功后回填）',
    index_node_hash  VARCHAR(64)           DEFAULT NULL COMMENT '嵌入内容 hash（幂等重建去重）',
    status           VARCHAR(16)  NOT NULL DEFAULT 'waiting' COMMENT '分段状态：waiting/indexing/completed/error',
    enabled          CHAR(1)      NOT NULL DEFAULT '1' COMMENT '启用：1 是 0 否（禁用不参与检索）',
    parent_id        VARCHAR(32)           DEFAULT NULL COMMENT '父子分段：父分段 id；父分段自身为 NULL（父段落表不嵌入）',
    hit_count        BIGINT       NOT NULL DEFAULT 0 COMMENT '召回命中计数',
    error_msg        TEXT                  COMMENT '嵌入失败原因',
    create_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time      DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_kb_segment_document (document_id),
    KEY idx_kb_segment_dataset (dataset_id),
    KEY idx_kb_segment_parent (parent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='easy-rag 知识库分段';
```

向量数据不落表：segment id 为 vector id；向量 metadata 携带 `dataset_id/document_id/segment_id/enabled/archived`（父子分段另带 parent_id，文档 metadata 同步进向量 metadata 供 filter 下推）。

**对外 API 字段契约（语义冻结）**：`kb_document.indexing_status/enabled/archived/metadata/doc_hash/file_path`、`kb_segment.index_node_id/enabled/parent_id`、`kb_dataset.embedding_model_active`——这些字段的语义通过对外 API 响应暴露给业务方，实现 change 不得变更其含义。

### 2.3 解析、切分与嵌入

- 解析：easy-rag 传递依赖的 Tika DocumentParser（PDF/Word/Excel/HTML/MD/TXT 等全格式）。
- Token 计数隔离（升级兼容约束）：token 统计收敛到单一封装点 `KbTokenCounter`；M1 用字符近似估计，不引入 Tokenizer 词表依赖；升级 langchain4j 1.x 时替换为 TokenCountEstimator 只改该封装点。
- 入库与索引执行解耦：管理端上传与对外 API 入库（同一服务）均只将文档置 waiting 并保存原始文件；索引执行方扫描 waiting 文档执行管道（入库即时触发 + 定时轮询兜底）。数据库即队列。
- 并发认领：状态流转用 CAS 更新（`UPDATE kb_document SET indexing_status='parsing' WHERE id=? AND indexing_status='waiting'`），多实例部署下同一文档不会被重复索引。
- 切分：
  - 通用分段：`DocumentSplitters.recursive(maxSegmentSize, maxOverlap)` 字符版（不绑 Tokenizer 词表），参数数据集级可配（segment_max_length/segment_overlap），支持自定义分隔符 segment_delimiter。
  - 父子分段：ParentChildSplitter 自研——大粒度 recursive 切父段（仅落表，不入向量库），小粒度切子段（嵌入，metadata 带 parent_id）。
  - QA 模式：QaExtractor 调用数据集配置的 chat model key，逐父段 prompt 抽取 {question, answer} 生成 QA 分段。
- 嵌入：按数据集 embedding_model_active（生效模型，与向量一致）从现有 Langchain4j 工厂 registry 取单例；`EmbeddingStore.addAll` 批量写入；回填 index_node_id 与 index_node_hash。切换配置模型后提示重建索引，重建完成后更新 active。
- 状态机：`waiting → parsing → splitting → indexing → completed | error`，状态实时写回 kb_document，失败写 error_msg。
- 重建索引：文档级删除全部分段 + 向量库按 document_id 删除（store 能力不足时按 collection 重建降级），回读原始文件重新入队；分段按 index_node_hash 幂等。
- 异步执行：KbIndexingExecutor，显式 JDK8 ThreadPoolExecutor（核心 2、有界队列、CallerRunsPolicy）。

### 2.4 原始文件存储

- 上传的原始文件先经 KbOriginalFileStore 持久化（文件名用 document id 防冲突），路径写回 kb_document.file_path；解析从保存的原文读取。
- 默认实现 LocalKbOriginalFileStore：本地目录 `fw.plus.ai.knowledge.storage-path`（默认 `${user.home}/.fw-plus/kb`）。
- 删除文档时同步删除原始文件。
- 切分文本已持久化在 kb_segment.content：重嵌入（换嵌入模型）不需重解析原文件；重建索引（换切分参数）回读原文。

## 3. API

### 3.1 管理端 KbConsoleController（前缀 `/magic/web/kb/`，随 MagicController 鉴权）

- `GET /dataset`、`POST /dataset`、`GET /dataset/{id}`、`PUT /dataset/{id}`、`DELETE /dataset/{id}`
- `POST /dataset/{id}/hitTest`（召回测试，支持 metadata 过滤条件）
- `GET /dataset/{id}/retrievers`（引用该数据集的 knowledge 类型 retriever 资源列表）
- `GET /dataset/{id}/document`、`POST /document/upload`（multipart）、`POST /document/{id}/reindex`、`PATCH /document/{id}/enabled`、`PATCH /document/{id}/archived`（归档/取消归档）
- `POST /dataset/{id}/document/batch`（批量启禁/删除/归档，body: documentIds + action）
- `GET /document/{id}/download`（下载原始文档）
- `PUT /document/{id}/metadata`（设置文档 metadata 键值对）
- `GET /document/{id}/segment`、`PATCH /segment/{id}/enabled`、`PUT /segment/{id}`（含 keywords）、`DELETE /segment/{id}`

行为：
- 删除数据集前校验 knowledge 类型 retriever 引用并提示（级联删除文档/分段/向量/原始文件）。
- 分段编辑后自动重嵌入；启禁用同步更新向量 metadata（store 不支持时检索侧内存过滤兜底）。
- 归档文档不参与检索（与启禁用独立）；批量操作对 documentIds 逐个执行并返回逐项结果。
- metadata 过滤：向量库支持 metadata filter 时下推过滤，不支持时检索后内存兜底；过滤条件格式对齐 Dify metadata_condition（logical_operator + conditions），为对外 API 的 Dify 对接（§3.4）提供一致的过滤语义。
- 召回测试：query → 嵌入 → 向量检索 → metadata 过滤 → （rerank）→ 返回分段与 score（父子分段附带父段预览），hit_count 异步累加。

### 3.2 knowledge 检索器（ContentRetrieverProvider）

- `KnowledgeContentRetrieverProvider implements ContentRetrieverProvider`，`provider()` 返回 `"knowledge"`，注册进现有 ContentRetrieverProviderRegistry。
- options：`dataset-id`（必填）、`max-results`、`min-score`（可覆盖数据集默认）。
- build 返回懒解析实例（同 MagicDynamicContentRetriever 模式）：检索时解析 dataset 配置 → 取 store/model → 向量检索 → 过滤 enabled='0' 与 archived='1'（禁用与归档分段均不参与检索，向量库 filter 下推优先、内存兜底）→ score 阈值 → hit_count 异步累加。
- 多实例：每个 content-retriever 资源 key 指向不同 dataset，互不影响。
- 类型边界：知识库管理端只管理 knowledge 类型；embedding-store/neo4j/azure-ai-search/web-search/filtering/aggregating 等其它类型不进入知识库管辖，不受数据集启禁用/删除级联影响。
- magic-script 侧：spring bean 方式 `import knowledge` 调 `search(datasetKey, query)`（M1）；不做四件套资源。

### 3.3 对外 API KbOpenApiController（同一服务，业务对接，Dify /v1/datasets 风格）

对外 API 与管理端同属 content-retriever 插件同一服务（用户 S20/S21：API 不单独成服务，由本插件直接提供接口）。

- 认证授权：统一使用 Sa-Token——`KbSaTokenAuthChecker` 基于 Sa-Token 校验请求凭证（Bearer/Authorization 中的 Sa-Token token）；凭证/客户端管理复用现有 magicapi satoken 插件资源（管理端已有），本插件不重复实现凭证管理；业务方如何获取凭证不在本需求范围。
- `POST /v1/datasets/{datasetId}/document/upload-by-file`：multipart 上传，落 waiting + 存原始文件，异步索引。
- `POST /v1/datasets/{datasetId}/document/create-by-text`：body{name, text}，同上。
- `GET /v1/datasets/{datasetId}/document/{documentId}`：文档索引状态查询。
- `POST /v1/datasets/{datasetId}/retrieve`：body{query, topK?, scoreThreshold?, metadataCondition?} → 命中 segments 列表（content/score/documentName/position/metadata）。
- 该 API 服务面向所有业务系统；Dify 对接（下）是其能力之一，不是唯一定位。

### 3.4 Dify External Knowledge API 兼容（对外 API 的能力之一）

- 端点：`POST {base}/v1/external/knowledge/retrieval`（Dify 注册端点填此地址；Dify 自动追加 /retrieval 时按其规则截取 base）。
- 请求（Dify 规范 S-C）：`{knowledge_id, query, retrieval_setting:{top_k, score_threshold}, metadata_condition?}`；metadata_condition 结构与 §3.1 过滤格式一致。
- knowledge_id = kb_dataset.id，按其路由数据集。
- 响应：`{"records":[{"content","score","title","metadata"}]}`；metadata 恒为 JSON 对象（不返回 null）；无命中返回 `{"records":[]}`。
- 错误：Sa-Token 校验失败 → `{"error_code":1002,"error_msg":...}`；knowledge_id 不存在 → `{"error_code":2001,"error_msg":...}`。
- 认证经 Sa-Token（Dify 以 Bearer 传递其配置的 API Key，本服务按 Sa-Token 校验）。

## 4. magic 控制台前端（content-retriever 插件扩展）

### 4.1 toolbars 知识库管理页面

- `src/console` 同一 iife.js 同时导出 datasources 与 toolbars：`toolbars: [{id:'kb-browser', title, icon, order, adminOnly:true, allowClose:true, component}]`。
- 视图：
  - 数据集：列表/新建/编辑/删除（表单引用现有 embedding-model、embedding-store、scoring-model 资源 key，配置 topK、score 阈值、切分参数 segment_max_length/overlap/delimiter）、引用的 knowledge retriever 列表展示；认证授权凭证管理由现有 magicapi satoken 插件承担，不在本页面重复实现
  - 文档：列表（状态轮询）、上传、重建索引、启禁用、归档/取消归档、批量操作、下载、metadata 编辑、删除
  - 分段：列表、编辑（含 keywords/answer）、启禁、删除
  - 召回测试：query 输入 → 命中分段与 score 展示（父子分段附父段预览），可选 metadata 过滤
- 仅管理 knowledge 类型检索器；其它 provider 类型不出现在本页面。
- i18n 补充 knowledge 相关文案（中英）。
- 构建沿用该模块现有 exec-maven-plugin + maven-resources-plugin 流程，产物仍为 content-retriever 插件 iife.js，不新增 MagicPluginConfiguration。

### 4.2 knowledge 类型表单

- content-retriever 表单 provider 下拉增加 `knowledge`：选中后加载数据集列表接口供选择，展示 dataset-id、max-results、min-score 专属 options；其它类型表单保持不变。

## 5. 配置项汇总

```
fw.plus.ai.knowledge.enabled          # 总开关，默认 true
fw.plus.ai.knowledge.storage-path     # 原始文件目录，默认 ${user.home}/.fw-plus/kb
```

## 6. 非功能约束

- JDK 1.8 语法（禁 Java 9+ API）；Spring Boot 2.7.18。
- easy-rag/tika 依赖 optional=true，使用方显式声明；BOM 统一 tika 版本。
- 8 家向量库删除/metadata 能力差异按 store 分支降级。
- 不自动建表；DDL 在 META-INF/sql 供手工执行。

## Scenario 索引（实现验收，与 brief A1–A17 对应）

- Scenario A1 模块结构落地与 DDL（§1、§2.2）
- Scenario A2 装配条件与配置项（§2.1、§5）
- Scenario A3 索引管道至 completed + CAS 认领（§2.3）
- Scenario A4 失败状态与重建索引（§2.3）
- Scenario A5 分段管理与启禁过滤（§3.1）
- Scenario A6 召回测试与 hit_count（§3.1）
- Scenario A7 原始文件持久化与生命周期（§2.4）
- Scenario A8 knowledge 检索器多实例与过滤链（§3.2）
- Scenario A9 归档/批量/下载/metadata 接口（§3.1）
- Scenario A10 分段关键词与自定义分隔符（§2.2、§2.3、§3.1）
- Scenario A11 父子分段（§2.3）
- Scenario A12 控制台 toolbars UI（§4.1）
- Scenario A13 knowledge 类型表单与 i18n（§4.2）
- Scenario A14 对外 REST API（§3.3）
- Scenario A15 Sa-Token 认证授权（§3.3）
- Scenario A16 Dify /retrieval 协议符合性（§3.4）
- Scenario A17 构建与非功能约束（§6）

> 范围注记：本 change 只交付设计文档（用户方案 A）；功能实现由后续**一个**实现 change 承接（管理端 + 对外 API 同服务），以本文档为输入。
> 正文中 §2.3 管道行为、§3.1/§3.2 接口行为、§4 前端行为为实现 change 的验收设计基础（届时其 brief/spec 引用本文档章节）。
