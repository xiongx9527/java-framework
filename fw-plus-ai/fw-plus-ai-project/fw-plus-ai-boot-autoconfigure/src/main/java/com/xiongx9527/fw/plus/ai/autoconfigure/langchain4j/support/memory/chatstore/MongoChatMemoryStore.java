package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import com.xiongx9527.fw.plus.common.util.StringUtils;
import org.bson.Document;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Supplier;

/**
 * 基于 MongoDB 的 {@link dev.langchain4j.store.memory.chat.ChatMemoryStore}（逐条消息存储）：
 * 每条消息一篇文档，{@code _id} 为 {@code tenantId|memoryId|seq}，字段
 * {@code tenantId / memoryId / bizType / messageSeq / messageType / messageJson / createTime / updateTime}，
 * 按 {@code messageSeq} 升序回放。
 *
 * <p>更新语义为全量覆盖：按 {@code messageSeq} 原位 upsert（Upsert，{@code createTime}
 * 用 {@code setOnInsert} 仅在首次写入生效）、裁剪超长部分，不做先删后插。</p>
 *
 * <p>{@link MongoTemplate} 通过 {@link Supplier} 每次调用时动态解析（类似
 * {@code MagicDynamicEmbeddingStore}），magic-api mongo 数据源运行期变更即时生效。</p>
 */
public class MongoChatMemoryStore extends AbstractJsonChatMemoryStore {

    /** 默认集合名 */
    public static final String DEFAULT_COLLECTION_NAME = "ai_chat_memory_message";

    private static final String FIELD_TENANT_ID = "tenantId";

    private static final String FIELD_MEMORY_ID = "memoryId";

    private static final String FIELD_BIZ_TYPE = "bizType";

    private static final String FIELD_MESSAGE_SEQ = "messageSeq";

    private static final String FIELD_MESSAGE_TYPE = "messageType";

    private static final String FIELD_MESSAGE_JSON = "messageJson";

    private static final String FIELD_UPDATE_TIME = "updateTime";

    private static final String FIELD_CREATE_TIME = "createTime";

    private static final String DOC_ID_SEPARATOR = "|";

    private final Supplier<MongoTemplate> mongoTemplateSupplier;

    private final String collectionName;

    public MongoChatMemoryStore(Supplier<MongoTemplate> mongoTemplateSupplier, String collectionName,
            ChatMemoryTenantResolver tenantResolver, String defaultBizType) {
        super(tenantResolver, defaultBizType);
        if (mongoTemplateSupplier == null) {
            throw new IllegalArgumentException("MongoChatMemoryStore: mongoTemplateSupplier 不能为空");
        }
        this.mongoTemplateSupplier = mongoTemplateSupplier;
        this.collectionName = StringUtils.isEmpty(collectionName) ? DEFAULT_COLLECTION_NAME : collectionName;
    }

    @Override
    protected List<StoredMessage> readMessages(String tenantId, String memoryId, String bizType) {
        Query query = idQuery(tenantId, memoryId).with(Sort.by(Sort.Direction.ASC, FIELD_MESSAGE_SEQ));
        List<Document> documents = mongoTemplate().find(query, Document.class, collectionName);
        List<StoredMessage> result = new ArrayList<>(documents.size());
        for (Document document : documents) {
            result.add(new StoredMessage(document.getString(FIELD_MESSAGE_TYPE),
                    document.getString(FIELD_MESSAGE_JSON)));
        }
        return result;
    }

    @Override
    protected void writeMessages(String tenantId, String memoryId, String bizType, List<StoredMessage> messages) {
        MongoTemplate mongoTemplate = mongoTemplate();
        Long existing = mongoTemplate.count(idQuery(tenantId, memoryId), Document.class, collectionName);
        int existingCount = existing == null ? 0 : existing.intValue();
        Date now = new Date();
        for (int i = 0; i < messages.size(); i++) {
            StoredMessage message = messages.get(i);
            Update update = new Update()
                    .set(FIELD_BIZ_TYPE, bizType)
                    .set(FIELD_MESSAGE_TYPE, message.getMessageType())
                    .set(FIELD_MESSAGE_JSON, message.getJson())
                    .set(FIELD_UPDATE_TIME, now)
                    .setOnInsert(FIELD_TENANT_ID, tenantId)
                    .setOnInsert(FIELD_MEMORY_ID, memoryId)
                    .setOnInsert(FIELD_MESSAGE_SEQ, i)
                    .setOnInsert(FIELD_CREATE_TIME, now);
            mongoTemplate.upsert(Query.query(Criteria.where("_id").is(docId(tenantId, memoryId, i))),
                    update, Document.class, collectionName);
        }
        if (existingCount > messages.size()) {
            mongoTemplate.remove(Query.query(Criteria.where(FIELD_TENANT_ID).is(tenantId)
                            .and(FIELD_MEMORY_ID).is(memoryId)
                            .and(FIELD_MESSAGE_SEQ).gte(messages.size())),
                    Document.class, collectionName);
        }
    }

    @Override
    protected void removeMessages(String tenantId, String memoryId) {
        mongoTemplate().remove(idQuery(tenantId, memoryId), Document.class, collectionName);
    }

    private MongoTemplate mongoTemplate() {
        MongoTemplate mongoTemplate = mongoTemplateSupplier.get();
        if (mongoTemplate == null) {
            throw new IllegalStateException("MongoChatMemoryStore: 无法解析 MongoTemplate 客户端");
        }
        return mongoTemplate;
    }

    private Query idQuery(String tenantId, String memoryId) {
        return Query.query(Criteria.where(FIELD_TENANT_ID).is(tenantId).and(FIELD_MEMORY_ID).is(memoryId));
    }

    /** 文档主键：{tenantId}|{memoryId}|{seq}，保证同一条消息在覆盖更新时身份稳定 */
    private static String docId(String tenantId, String memoryId, int seq) {
        return tenantId + DOC_ID_SEPARATOR + memoryId + DOC_ID_SEPARATOR + seq;
    }
}
