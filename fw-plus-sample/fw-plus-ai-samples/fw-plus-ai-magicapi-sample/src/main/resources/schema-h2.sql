-- =====================================================================
-- 知识库元数据表的 H2 方言版本，供 fw-plus-ai-magicapi-sample 启动时自动建表。
--
-- 与 META-INF/sql/fw-plus-ai-knowledge.sql（MySQL 方言，需手工执行）逐表等价，差异仅在
-- H2 不接受的 MySQL 专有语法：
--   1. 列级 COMMENT '...'          → 改为本文件里的 -- 行注释
--   2. 表级 ENGINE/CHARSET/COMMENT → 去掉
--   3. 表内 KEY / UNIQUE KEY 内联索引 → 改为独立的 CREATE [UNIQUE] INDEX 语句
--   4. ON UPDATE CURRENT_TIMESTAMP  → 去掉（update_time 由应用层写入；MySQL 版保留自动刷新）
--
-- 生产环境请用 MySQL 方言那份，本文件只服务样例的 H2 内存库。
-- =====================================================================

-- 知识库全局设置（单行，id 固定 'singleton'）
CREATE TABLE kb_setting (
    id               VARCHAR(32)   NOT NULL,
    enabled          TINYINT      NOT NULL DEFAULT 1,
    storage_type     VARCHAR(16)   NOT NULL DEFAULT 'local',
    storage_path     VARCHAR(1024)          DEFAULT NULL,
    s3_key           VARCHAR(255)           DEFAULT NULL,
    hybrid_search    TINYINT      NOT NULL DEFAULT 1,
    es_enabled       TINYINT      NOT NULL DEFAULT 0,
    es_key           VARCHAR(255)           DEFAULT NULL,
    es_index_prefix  VARCHAR(255)  NOT NULL DEFAULT 'fw-kb',
    embedding_model_key   VARCHAR(255)  DEFAULT NULL,
    embedding_store_key   VARCHAR(255)  DEFAULT NULL,
    scoring_model_key     VARCHAR(255)  DEFAULT NULL,
    chat_model_key        VARCHAR(255)  DEFAULT NULL,
    default_retrieval_top_k           INT          NOT NULL DEFAULT 4,
    default_retrieval_score_threshold DECIMAL(6,4) NOT NULL DEFAULT 0.0000,
    default_segment_max_length        INT          NOT NULL DEFAULT 1000,
    default_segment_overlap           INT          NOT NULL DEFAULT 200,
    default_segment_delimiter         VARCHAR(32)           DEFAULT NULL,
    default_segment_mode              VARCHAR(16)  NOT NULL DEFAULT 'general',
    default_indexing_technique     VARCHAR(16)  NOT NULL DEFAULT 'high_quality',
    create_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
-- id          固定值 singleton
-- enabled     运行期总开关：1 启用 0 停用
-- storage_type 原始文件存储介质：local/s3
-- storage_path 原始文件目录（storage_type=local 时生效）
-- s3_key      s3 数据源资源 key（storage_type=s3 时生效；空则取 Spring 默认 s3 数据源）
-- hybrid_search 混合检索开关：1 向量路+关键词路融合 0 仅向量路
-- es_enabled  ES 全文检索开关：1 关键词路优先走 ES（需启用 magicapi elasticsearch 插件并配置 ES 数据源）
-- es_key      ES 数据源资源 key（空则取 Spring 默认 ES 数据源）
-- es_index_prefix ES 索引名前缀（分段全文索引名 = 前缀 + "-segments"）
-- embedding_model_key 新建数据集默认使用的嵌入模型资源 key（langchain4j embedding-model）
-- embedding_store_key 新建数据集默认使用的向量库资源 key（langchain4j embedding-store）
-- scoring_model_key  新建数据集默认使用的 rerank 模型资源 key（scoring-model），空则不重排
-- chat_model_key    新建数据集默认使用的对话模型资源 key（chat-model），segment_mode=qa 时需要
-- default_retrieval_top_k 新建数据集默认召回数量
-- default_retrieval_score_threshold 新建数据集默认分数阈值
-- default_segment_max_length 新建数据集默认分段最大长度（字符）
-- default_segment_overlap 新建数据集默认分段重叠长度（字符）
-- default_segment_delimiter 新建数据集默认自定义分段分隔符，空则仅按递归规则
-- default_segment_mode 新建数据集默认分段模式：general / parent-child / qa
-- default_indexing_technique 新建数据集默认索引技术：high_quality 入向量库 / economy 仅关键词单路

-- 知识库数据集
CREATE TABLE kb_dataset (
    id                        VARCHAR(32)   NOT NULL,
    name                      VARCHAR(255)  NOT NULL,
    description               VARCHAR(1024)          DEFAULT NULL,
    indexing_technique        VARCHAR(16)   NOT NULL DEFAULT 'high_quality',
    embedding_model_key       VARCHAR(255)  NOT NULL,
    embedding_model_active    VARCHAR(255)           DEFAULT NULL,
    embedding_store_key       VARCHAR(255)  NOT NULL,
    scoring_model_key         VARCHAR(255)           DEFAULT NULL,
    chat_model_key            VARCHAR(255)           DEFAULT NULL,
    collection_name           VARCHAR(255)           DEFAULT NULL,
    retrieval_top_k           INT           NOT NULL DEFAULT 4,
    retrieval_score_threshold DECIMAL(6,4)  NOT NULL DEFAULT 0.0000,
    segment_max_length        INT           NOT NULL DEFAULT 1000,
    segment_overlap           INT           NOT NULL DEFAULT 200,
    segment_delimiter         VARCHAR(32)            DEFAULT NULL,
    segment_mode              VARCHAR(16)   NOT NULL DEFAULT 'general',
    status                    VARCHAR(16)   NOT NULL DEFAULT 'active',
    error_msg                 CLOB                   DEFAULT NULL,
    create_time               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time               TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    del_flag                  CHAR(1)       NOT NULL DEFAULT '0',
    PRIMARY KEY (id)
);
CREATE INDEX idx_kb_dataset_name ON kb_dataset (name);
-- indexing_technique        索引技术：high_quality/economy（economy 预留）
-- embedding_model_active    上次成功索引实际使用的嵌入模型 key；检索嵌入用此值保证与向量一致；切换配置后提示重建索引，重建完成后更新
-- scoring_model_key         rerank 模型资源 key（scoring-model），空则不重排
-- chat_model_key            QA 分段抽取用对话模型资源 key（langchain4j chat-model），segment_mode=qa 时必填
-- collection_name           向量库 collection 名，按数据集隔离；空则按约定生成
-- retrieval_score_threshold 默认分数阈值
-- segment_delimiter         自定义分段分隔符，空则仅按递归规则
-- segment_mode              分段模式：general 通用 / parent-child 父子；qa 为预留，当前版本不支持
-- del_flag                  逻辑删除：0 否 1 是

-- 知识库文档
CREATE TABLE kb_document (
    id               VARCHAR(32)   NOT NULL,
    dataset_id       VARCHAR(32)   NOT NULL,
    name             VARCHAR(255)  NOT NULL,
    file_path        VARCHAR(1024)          DEFAULT NULL,
    file_ext         VARCHAR(16)            DEFAULT NULL,
    file_size        BIGINT                 DEFAULT NULL,
    data_source_type VARCHAR(16)   NOT NULL DEFAULT 'upload',
    word_count       INT                    DEFAULT 0,
    latency_ms       BIGINT                 DEFAULT NULL,
    indexing_status  VARCHAR(16)   NOT NULL DEFAULT 'waiting',
    error_msg        CLOB                   DEFAULT NULL,
    enabled          CHAR(1)       NOT NULL DEFAULT '1',
    archived         CHAR(1)       NOT NULL DEFAULT '0',
    metadata         CLOB                   DEFAULT NULL,
    doc_hash         VARCHAR(64)            DEFAULT NULL,
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_kb_document_dataset ON kb_document (dataset_id, indexing_status);
CREATE UNIQUE INDEX uk_kb_document_hash ON kb_document (dataset_id, doc_hash);
-- file_path        原始文件存储路径（KbOriginalFileStore 落盘后回写；create-by-text 时为空）
-- data_source_type 来源：upload/create-by-text
-- latency_ms       索引耗时（毫秒）
-- indexing_status  索引状态：waiting/parsing/splitting/indexing/completed/error
-- enabled          启用：1 是 0 否（禁用不参与检索）
-- archived         归档：1 是 0 否（归档不参与检索，与启禁用独立）
-- metadata         文档自定义元数据 JSON（键值对，检索过滤用）
-- doc_hash         内容 SHA-256，数据集内去重

-- 知识库分段
CREATE TABLE kb_segment (
    id               VARCHAR(32)   NOT NULL,
    dataset_id       VARCHAR(32)   NOT NULL,
    document_id      VARCHAR(32)   NOT NULL,
    position         INT           NOT NULL DEFAULT 0,
    content          CLOB          NOT NULL,
    answer           CLOB                   DEFAULT NULL,
    keywords         VARCHAR(512)           DEFAULT NULL,
    word_count       INT                    DEFAULT 0,
    tokens           INT                    DEFAULT 0,
    index_node_id    VARCHAR(128)           DEFAULT NULL,
    index_node_hash  VARCHAR(64)            DEFAULT NULL,
    status           VARCHAR(16)   NOT NULL DEFAULT 'waiting',
    enabled          CHAR(1)       NOT NULL DEFAULT '1',
    parent_id        VARCHAR(32)            DEFAULT NULL,
    hit_count        BIGINT        NOT NULL DEFAULT 0,
    error_msg        CLOB                   DEFAULT NULL,
    create_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE INDEX idx_kb_segment_document ON kb_segment (document_id);
CREATE INDEX idx_kb_segment_dataset ON kb_segment (dataset_id);
CREATE INDEX idx_kb_segment_parent ON kb_segment (parent_id);
-- id            主键（同时作为向量库 vector id）
-- answer        QA 模式的答案；通用/父子分段为空
-- keywords      分段关键词（逗号分隔，管理端可编辑）
-- tokens        token 估计值（KbTokenCounter 字符近似，M1）
-- index_node_id 向量库节点 id（嵌入成功后回填）
-- index_node_hash 嵌入内容 hash（幂等重建去重）
-- parent_id     父子分段：父分段 id；父段落表不嵌入
-- hit_count     召回命中计数

-- 知识库元数据字段定义
CREATE TABLE kb_metadata_field (
    id          VARCHAR(32)   NOT NULL,
    dataset_id  VARCHAR(32)   NOT NULL,
    name        VARCHAR(255)  NOT NULL,
    type        VARCHAR(16)   NOT NULL DEFAULT 'string',
    description VARCHAR(1024)          DEFAULT NULL,
    create_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);
CREATE UNIQUE INDEX uk_kb_metadata_field ON kb_metadata_field (dataset_id, name);
-- name 字段名（数据集内唯一）
-- type 字段类型：string/number/time
