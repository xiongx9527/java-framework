package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.xiongx9527.fw.plus.common.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/**
 * 基于 Elasticsearch（{@code elasticsearch-java} 客户端）的
 * {@link dev.langchain4j.store.memory.chat.ChatMemoryStore}（逐条消息存储）：
 * 每条消息一篇文档，文档 ID 为 {@code tenantId|memoryId|seq}，
 * 字段 {@code tenantId / memoryId / bizType / messageSeq / messageType / messageJson / createTime / updateTime}。
 *
 * <p>更新语义为全量覆盖：按 {@code messageSeq} 原位覆盖（按文档 ID index，即 upsert）并裁剪
 * 超长部分（按确定性文档 ID delete），不做先删后插；写入带 {@code refresh=true}
 * 保证读己之写（ChatMemory 同一请求内先写后读，如 MessageWindowChatMemory 裁剪窗口）。</p>
 *
 * <p>{@link ElasticsearchClient} 通过 {@link Supplier} 每次调用时动态解析（类似
 * {@code MagicDynamicEmbeddingStore}），magic-api elasticsearch 数据源运行期变更即时生效。</p>
 */
public class ElasticsearchChatMemoryStore extends AbstractJsonChatMemoryStore {

    /** 默认索引名 */
    public static final String DEFAULT_INDEX_NAME = "ai-chat-memory-message";

    /** 单次读取的消息条数上限（与 ES 默认 max_result_window 对齐） */
    private static final int MAX_RESULT_WINDOW = 10000;

    private static final String FIELD_TENANT_ID = "tenantId";

    private static final String FIELD_MEMORY_ID = "memoryId";

    private static final String FIELD_BIZ_TYPE = "bizType";

    private static final String FIELD_MESSAGE_SEQ = "messageSeq";

    private static final String FIELD_MESSAGE_TYPE = "messageType";

    private static final String FIELD_MESSAGE_JSON = "messageJson";

    private static final String FIELD_UPDATE_TIME = "updateTime";

    private static final String FIELD_CREATE_TIME = "createTime";

    private static final String DOC_ID_SEPARATOR = "|";

    private final Supplier<ElasticsearchClient> clientSupplier;

    private final String indexName;

    public ElasticsearchChatMemoryStore(Supplier<ElasticsearchClient> clientSupplier, String indexName,
            ChatMemoryTenantResolver tenantResolver, String defaultBizType) {
        super(tenantResolver, defaultBizType);
        if (clientSupplier == null) {
            throw new IllegalArgumentException("ElasticsearchChatMemoryStore: clientSupplier 不能为空");
        }
        this.clientSupplier = clientSupplier;
        this.indexName = StringUtils.isEmpty(indexName) ? DEFAULT_INDEX_NAME : indexName;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected List<StoredMessage> readMessages(String tenantId, String memoryId, String bizType) {
        try {
            SearchResponse<Map> response = client().search(s -> s.index(indexName)
                            .ignoreUnavailable(true)
                            .query(idQuery(tenantId, memoryId))
                            .sort(so -> so.field(f -> f.field(FIELD_MESSAGE_SEQ).order(SortOrder.Asc)))
                            .size(MAX_RESULT_WINDOW),
                    Map.class);
            List<StoredMessage> result = new ArrayList<>(response.hits().hits().size());
            for (Hit<Map> hit : response.hits().hits()) {
                Map<String, Object> source = hit.source();
                if (source == null) {
                    continue;
                }
                Object messageType = source.get(FIELD_MESSAGE_TYPE);
                Object messageJson = source.get(FIELD_MESSAGE_JSON);
                result.add(new StoredMessage(
                        messageType == null ? null : String.valueOf(messageType),
                        messageJson == null ? null : String.valueOf(messageJson)));
            }
            return result;
        } catch (Exception e) {
            throw new IllegalStateException("ElasticsearchChatMemoryStore 读取失败: memoryId=" + memoryId, e);
        }
    }

    @Override
    protected void writeMessages(String tenantId, String memoryId, String bizType, List<StoredMessage> messages) {
        try {
            // 先取当前消息数，原位覆盖（按确定性文档 ID index 即 upsert）并按 ID 裁剪超长部分
            Long existing = client().count(c -> c.index(indexName)
                    .ignoreUnavailable(true)
                    .query(idQuery(tenantId, memoryId))).count();
            int existingCount = existing == null ? 0 : existing.intValue();
            Instant now = Instant.now();
            List<BulkOperation> operations =
                    new ArrayList<>(messages.size() + Math.max(0, existingCount - messages.size()));
            for (int i = 0; i < messages.size(); i++) {
                final int seq = i;
                Map<String, Object> document = new HashMap<>();
                document.put(FIELD_TENANT_ID, tenantId);
                document.put(FIELD_MEMORY_ID, memoryId);
                document.put(FIELD_BIZ_TYPE, bizType);
                document.put(FIELD_MESSAGE_SEQ, seq);
                document.put(FIELD_MESSAGE_TYPE, messages.get(i).getMessageType());
                document.put(FIELD_MESSAGE_JSON, messages.get(i).getJson());
                document.put(FIELD_CREATE_TIME, now.toString());
                document.put(FIELD_UPDATE_TIME, now.toString());
                operations.add(BulkOperation.of(o -> o.index(idx -> idx
                        .index(indexName).id(docId(tenantId, memoryId, seq)).document(document))));
            }
            for (int i = messages.size(); i < existingCount; i++) {
                final int seq = i;
                operations.add(BulkOperation.of(o -> o.delete(d -> d
                        .index(indexName).id(docId(tenantId, memoryId, seq)))));
            }
            if (!operations.isEmpty()) {
                client().bulk(b -> b.operations(operations).refresh(Refresh.True));
            }
        } catch (Exception e) {
            throw new IllegalStateException("ElasticsearchChatMemoryStore 写入失败: memoryId=" + memoryId, e);
        }
    }

    @Override
    protected void removeMessages(String tenantId, String memoryId) {
        try {
            client().deleteByQuery(d -> d.index(indexName)
                    .ignoreUnavailable(true)
                    .query(idQuery(tenantId, memoryId))
                    .refresh(true));
        } catch (Exception e) {
            throw new IllegalStateException("ElasticsearchChatMemoryStore 删除失败: memoryId=" + memoryId, e);
        }
    }

    private ElasticsearchClient client() {
        ElasticsearchClient client = clientSupplier.get();
        if (client == null) {
            throw new IllegalStateException("ElasticsearchChatMemoryStore: 无法解析 ElasticsearchClient 客户端");
        }
        return client;
    }

    private static co.elastic.clients.elasticsearch._types.query_dsl.Query idQuery(String tenantId, String memoryId) {
        return co.elastic.clients.elasticsearch._types.query_dsl.Query.of(q -> q.bool(b -> b
                .must(m -> m.term(t -> t.field(FIELD_TENANT_ID + ".keyword").value(tenantId)))
                .must(m -> m.term(t -> t.field(FIELD_MEMORY_ID + ".keyword").value(memoryId)))));
    }

    private static String docId(String tenantId, String memoryId, int seq) {
        return tenantId + DOC_ID_SEPARATOR + memoryId + DOC_ID_SEPARATOR + seq;
    }
}
