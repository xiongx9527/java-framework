-- 一级滚动会话摘要表：会话键（5 段 run_id）唯一，只维护一行（「覆盖」只发生在这一行）。
-- 原文仍保留在消息存储（mem0-A，append-only），本表只存压缩摘要。
CREATE TABLE IF NOT EXISTS `ai_chat_summary` (
	`id`            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
	`session_key`   VARCHAR(255) NOT NULL                COMMENT '会话键（{tenant}:{app}:{agent}:{user}:{session}）',
	`summary`       TEXT                                 COMMENT '会话摘要正文',
	`refine_count`  INT          NOT NULL DEFAULT 0      COMMENT '自上次全量重摘以来的增量次数',
	`last_covered`  VARCHAR(255)                         COMMENT '已并入摘要的最后一条窗口外历史的身份锚点（写入水位）',
	`created_at`    DATETIME     NOT NULL                COMMENT '创建时间',
	`updated_at`    DATETIME     NOT NULL                COMMENT '更新时间',
	PRIMARY KEY (`id`),
	UNIQUE KEY `uk_ai_chat_summary_session_key` (`session_key`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT = 'AI 会话摘要（一级滚动）';
