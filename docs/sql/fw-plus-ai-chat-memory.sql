-- ------------------------------------------------------------------------------
-- fw-plus-ai ChatMemoryStore JDBC 存储建表脚本（MySQL 方言）
--
-- 对应 fw-plus-ai-magicapi-plugin-langchain4j-chat-memory-store 的 jdbc 提供商：
--   - 资源定义 type=jdbc 时使用本表，表名可通过资源 params.tableName 调整（同步修改下方 DDL），
--     或全局配置 fw.plus.ai.langchain4j.chat-memory.jdbc.table-name
--   - 数据源可指定 magic-api 命名数据源 Key（datasourceKey），留空使用 Spring 默认 JdbcTemplate
--
-- 存储模型：逐条消息一行，主键 id 由应用侧雪花算法生成（非数据库自增）；
-- memory_id 约定格式为 {bizType}:{conversationId}（业务类型为冒号前缀）；
-- tenant_id 由 ChatMemoryTenantResolver 解析，默认 'default'
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `ai_chat_memory_message` (
    `id`           BIGINT        NOT NULL COMMENT '主键（雪花 ID，由应用写入）',
    `tenant_id`    VARCHAR(64)   NOT NULL DEFAULT 'default' COMMENT '租户 ID',
    `memory_id`    VARCHAR(512)  NOT NULL COMMENT '会话记忆 ID（{bizType}:{conversationId}）',
    `biz_type`     VARCHAR(64)   NOT NULL DEFAULT 'default' COMMENT '业务类型（memoryId 冒号前缀，冗余存储便于按业务查询）',
    `message_seq`  INT           NOT NULL COMMENT '消息序号（0 起，按会话内顺序）',
    `message_type` VARCHAR(32)   NOT NULL COMMENT '消息类型（ChatMessageType）：SYSTEM/USER/AI/TOOL_EXECUTION_RESULT',
    `message_json` LONGTEXT               COMMENT '单条消息 JSON（单元素 ChatMessage 数组）',
    `create_time`  DATETIME      NULL     COMMENT '创建时间',
    `update_time`  DATETIME      NULL     COMMENT '最近更新时间',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_tenant_memory_seq` (`tenant_id`, `memory_id`, `message_seq`),
    KEY `idx_tenant_biz_type` (`tenant_id`, `biz_type`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 对话记忆存储（逐条消息）';

-- ------------------------------------------------------------------------------
-- WORKER_NODE：UID 生成器 workerId 分配表（百度 uidgenerator 标准结构）
-- jdbc 提供商的主键 id 由 DisposableWorkerIdAssigner + DefaultUidGenerator 生成，
-- workerId 即本表自增记录 ID 对 1024 取模；随所选数据源分配（表须存在于对应库中）
-- ------------------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS `WORKER_NODE` (
    `ID`          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键（取模 1024 后作为 workerId）',
    `HOST_NAME`   VARCHAR(64)  NOT NULL COMMENT '主机名',
    `PORT`        VARCHAR(64)  NOT NULL COMMENT '端口',
    `TYPE`        INT          NOT NULL COMMENT '节点类型：ACTUAL=2、CONTAINER=1',
    `LAUNCH_DATE` DATE         NOT NULL COMMENT '启动日期',
    `MODIFIED`    DATETIME     NOT NULL COMMENT '修改时间（dispose 时更新，可用于清理长期失效节点）',
    `CREATED`     DATETIME     NOT NULL COMMENT '创建时间',
    PRIMARY KEY (`ID`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'UID 生成器 WorkerId 分配表';
