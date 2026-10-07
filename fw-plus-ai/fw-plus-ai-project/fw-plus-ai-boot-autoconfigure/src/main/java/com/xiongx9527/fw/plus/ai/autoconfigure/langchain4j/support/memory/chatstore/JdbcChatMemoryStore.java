package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * 基于 JDBC 的 {@link dev.langchain4j.store.memory.chat.ChatMemoryStore}（逐条消息存储）。
 *
 * <p>表结构见仓库 {@code docs/sql/fw-plus-ai-chat-memory.sql}（MySQL 方言），
 * 默认表 {@code ai_chat_memory_message}：主键 {@code (tenant_id, memory_id, message_seq)}，
 * 每条消息一行，含 {@code biz_type}、{@code message_type} 列。</p>
 *
 * <p>更新语义为全量覆盖：按 {@code message_seq} 原位 UPDATE 已有序号、INSERT 新增序号、
 * DELETE 裁剪超长部分（不整表先删后插，避免并发读空窗，跨库通用）；
 * 主键 {@code id} 由应用侧雪花算法生成（Hutool {@code IdUtil}），非数据库自增；
 * {@code create_time} 仅在首次插入时写入，{@code update_time} 随每次覆盖刷新。</p>
 *
 * <p>{@link JdbcTemplate} 通过 {@link Supplier} 每次调用时动态解析（类似
 * {@code MagicDynamicEmbeddingStore}），magic-api 数据源运行期变更即时生效。</p>
 */
public class JdbcChatMemoryStore extends AbstractJsonChatMemoryStore {

    private static final String IDENTIFIER_PATTERN = "^[A-Za-z0-9_]+$";

    private final Supplier<JdbcTemplate> jdbcTemplateSupplier;

    private final LongSupplier idSupplier;

    private final String tableName;

    public JdbcChatMemoryStore(Supplier<JdbcTemplate> jdbcTemplateSupplier, String tableName,
            ChatMemoryTenantResolver tenantResolver, String defaultBizType, LongSupplier idSupplier) {
        super(tenantResolver, defaultBizType);
        if (jdbcTemplateSupplier == null) {
            throw new IllegalArgumentException("JdbcChatMemoryStore: jdbcTemplateSupplier 不能为空");
        }
        if (idSupplier == null) {
            throw new IllegalArgumentException("JdbcChatMemoryStore: idSupplier 不能为空");
        }
        if (tableName == null || !tableName.matches(IDENTIFIER_PATTERN)) {
            throw new IllegalArgumentException(
                    "JdbcChatMemoryStore: 非法表名 '" + tableName + "'（仅允许字母、数字、下划线）");
        }
        this.jdbcTemplateSupplier = jdbcTemplateSupplier;
        this.idSupplier = idSupplier;
        this.tableName = tableName;
    }

    @Override
    protected List<StoredMessage> readMessages(String tenantId, String memoryId, String bizType) {
        return jdbcTemplate().query("SELECT message_type, message_json FROM " + tableName
                        + " WHERE tenant_id = ? AND memory_id = ? ORDER BY message_seq ASC",
                new StoredMessageRowMapper(), tenantId, memoryId);
    }

    @Override
    protected void writeMessages(String tenantId, String memoryId, String bizType, List<StoredMessage> messages) {
        JdbcTemplate jdbcTemplate = jdbcTemplate();
        Integer existing = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName
                + " WHERE tenant_id = ? AND memory_id = ?", Integer.class, tenantId, memoryId);
        int existingCount = existing == null ? 0 : existing;
        Timestamp now = new Timestamp(System.currentTimeMillis());
        for (int i = 0; i < messages.size(); i++) {
            StoredMessage message = messages.get(i);
            if (i < existingCount) {
                jdbcTemplate.update("UPDATE " + tableName
                                + " SET message_type = ?, message_json = ?, update_time = ?"
                                + " WHERE tenant_id = ? AND memory_id = ? AND message_seq = ?",
                        message.getMessageType(), message.getJson(), now, tenantId, memoryId, i);
            } else {
                jdbcTemplate.update("INSERT INTO " + tableName
                                + " (id, tenant_id, memory_id, biz_type, message_seq, message_type, message_json,"
                                + " create_time, update_time) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        idSupplier.getAsLong(),
                        tenantId, memoryId, bizType, i, message.getMessageType(), message.getJson(), now, now);
            }
        }
        if (existingCount > messages.size()) {
            jdbcTemplate.update("DELETE FROM " + tableName
                            + " WHERE tenant_id = ? AND memory_id = ? AND message_seq >= ?",
                    tenantId, memoryId, messages.size());
        }
    }

    @Override
    protected void removeMessages(String tenantId, String memoryId) {
        jdbcTemplate().update("DELETE FROM " + tableName + " WHERE tenant_id = ? AND memory_id = ?",
                tenantId, memoryId);
    }

    private JdbcTemplate jdbcTemplate() {
        JdbcTemplate jdbcTemplate = jdbcTemplateSupplier.get();
        if (jdbcTemplate == null) {
            throw new IllegalStateException("JdbcChatMemoryStore: 无法解析 JdbcTemplate 客户端");
        }
        return jdbcTemplate;
    }

    private static final class StoredMessageRowMapper implements RowMapper<StoredMessage> {

        @Override
        public StoredMessage mapRow(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
            return new StoredMessage(rs.getString("message_type"), rs.getString("message_json"));
        }
    }
}
