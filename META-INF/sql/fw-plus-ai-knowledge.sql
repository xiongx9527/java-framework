-- =====================================================================
-- easy-rag 知识库元数据表（设计稿原样落地，手工执行；不自动建表）
-- 来源：docs/comet/archive/2026-10-02-easy-rag-knowledge/specs/ai-knowledge-rag/spec.md §2.2
-- =====================================================================

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
    collection_name           VARCHAR(255)          DEFAULT NULL COMMENT '向量库 collection 名，按数据集隔离；空则按约定生成',
    retrieval_top_k           INT          NOT NULL DEFAULT 4 COMMENT '默认召回数量',
    retrieval_score_threshold DECIMAL(6,4) NOT NULL DEFAULT 0.0000 COMMENT '默认分数阈值',
    segment_max_length        INT          NOT NULL DEFAULT 1000 COMMENT '分段最大长度（字符）',
    segment_overlap           INT          NOT NULL DEFAULT 200 COMMENT '分段重叠长度（字符）',
    segment_delimiter         VARCHAR(32)           DEFAULT NULL COMMENT '自定义分段分隔符，空则仅按递归规则',
    segment_mode              VARCHAR(16)  NOT NULL DEFAULT 'general' COMMENT '分段模式：general 通用（分段直接嵌入）/parent-child 父子（父段落表不嵌入，子段嵌入带 parent_id）；qa 为预留，当前版本不支持',
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
