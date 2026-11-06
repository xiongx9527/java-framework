package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.AbstractConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryListener;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import com.xiongx9527.fw.plus.ai.base.memory.MemoryRecord;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort.DimensionMismatchException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 向量库对话记忆后端：用 {@link EmbeddingPort} 把对话文本向量化，写入 {@link VectorStorePort} 做语义检索。
 *
 * <p>与 mem0 服务端不同，本端口不依赖外部 LLM 做事实抽取——每条消息（或合并后的对话片段）
 * 直接作为一个记忆向量写入；检索用查询向量做最近邻召回，返回按相关度降序的命中。</p>
 *
 * <p>向量库只负责「向量存 / 取」，不擅长「按 id 取内容」与「按作用域枚举全部」，因此本端口在
 * 进程内维护一份轻量索引（内容与作用域 → id），仅把语义检索交给向量库。文本内容与作用域同时也
 * 写入向量元数据，使检索命中可独立还原出 {@link MemoryRecord}。</p>
 */
public class VectorConversationMemoryPort extends AbstractConversationMemoryPort {

    private static final String DEFAULT_COLLECTION = "conversation_memory";

    private final EmbeddingPort embeddingPort;
    private final VectorStorePort vectorStorePort;
    private final String collection;

    /** id → 记忆本地副本（支撑 list / deleteAll 与检索命中的兜底还原）。 */
    private final Map<String, StoredMemory> store = new ConcurrentHashMap<String, StoredMemory>();
    /** 作用域 → 记忆 id 列表。 */
    private final Map<String, List<String>> byScope = new ConcurrentHashMap<String, List<String>>();
    /** 集合维度，首次写入时按真实向量确定。 */
    private volatile int dimension = 0;

    public VectorConversationMemoryPort(EmbeddingPort embeddingPort, VectorStorePort vectorStorePort) {
        this(embeddingPort, vectorStorePort, null);
    }

    public VectorConversationMemoryPort(EmbeddingPort embeddingPort, VectorStorePort vectorStorePort,
            List<ConversationMemoryListener> listeners) {
        this(embeddingPort, vectorStorePort, DEFAULT_COLLECTION, listeners);
    }

    public VectorConversationMemoryPort(EmbeddingPort embeddingPort, VectorStorePort vectorStorePort,
            String collection, List<ConversationMemoryListener> listeners) {
        super(listeners);
        if (embeddingPort == null) {
            throw new IllegalArgumentException("embeddingPort is required for the VECTOR memory provider");
        }
        if (vectorStorePort == null || !vectorStorePort.configured()) {
            throw new ConversationMemoryException(
                    "vector store is not configured for the VECTOR memory provider");
        }
        this.embeddingPort = embeddingPort;
        this.vectorStorePort = vectorStorePort;
        this.collection = collection == null || collection.trim().isEmpty() ? DEFAULT_COLLECTION : collection;
    }

    @Override
    public List<MemoryRecord> add(ConversationMemoryAddRequest request) {
        if (request == null || request.getMessages().isEmpty()) {
            throw new ConversationMemoryException("vector memory add requires at least one message");
        }
        List<MemoryRecord> records = new ArrayList<MemoryRecord>();
        for (com.xiongx9527.fw.plus.ai.base.model.ChatMessage m : request.getMessages()) {
            String text = m.getContent();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }
            double[] vector = embeddingPort.embed(text);
            if (vector == null || vector.length == 0) {
                notifyError("embedding returned no vector for message", scopeOf(request));
                continue;
            }
            ensureCollection(vector.length);
            String id = UUID.randomUUID().toString();
            String scopeKey = scopeKey(request.getUserId(), request.getAgentId(), request.getRunId());
            StoredMemory sm = new StoredMemory(id, text, scopeKey,
                    request.getUserId(), request.getAgentId(), request.getRunId());
            Map<String, Object> metadata = new LinkedHashMap<String, Object>();
            metadata.put("memory", text);
            putScope(metadata, request.getUserId(), request.getAgentId(), request.getRunId());
            vectorStorePort.upsert(collection, id, vector, metadata);
            store.put(id, sm);
            indexScope(scopeKey, id);
            records.add(toRecord(sm, "ADD"));
        }
        notifyAdd(scopeOf(request), records);
        return records;
    }

    @Override
    public List<MemoryRecord> search(ConversationMemorySearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new ConversationMemoryException("vector memory search requires a query");
        }
        double[] vector = embeddingPort.embed(request.getQuery());
        if (vector == null || vector.length == 0) {
            throw new ConversationMemoryException("embedding returned no vector for query");
        }
        int topK = request.getTopK() != null ? request.getTopK() : 10;
        VectorSearchResult result = vectorStorePort.search(VectorSearchRequest.builder()
                .collection(collection)
                .vector(vector)
                .topK(topK * 4)
                .build());
        List<MemoryRecord> records = new ArrayList<MemoryRecord>();
        if (result != null) {
            String scopeKey = scopeKey(request.getUserId(), request.getAgentId(), request.getRunId());
            for (ScoredDocument hit : result.getHits()) {
                MemoryRecord rec = fromHit(hit);
                if (rec == null) {
                    continue;
                }
                if (!matchesScope(scopeKey, rec.getUserId(), rec.getAgentId(), rec.getRunId())) {
                    continue;
                }
                records.add(rec);
                if (records.size() >= topK) {
                    break;
                }
            }
        }
        notifySearch(scopeOf(request), records);
        return records;
    }

    @Override
    public List<MemoryRecord> list(ConversationMemoryScope scope) {
        String scopeKey = scopeKey(scope.getUserId(), scope.getAgentId(), scope.getRunId());
        List<MemoryRecord> records = new ArrayList<MemoryRecord>();
        for (StoredMemory sm : store.values()) {
            if (matchesScope(scopeKey, sm.userId, sm.agentId, sm.runId)) {
                records.add(toRecord(sm, "NONE"));
            }
        }
        notifyList(scope);
        return records;
    }

    @Override
    public void delete(String memoryId) {
        if (memoryId == null || memoryId.trim().isEmpty()) {
            throw new ConversationMemoryException("vector memory delete requires a memory id");
        }
        StoredMemory sm = store.remove(memoryId);
        if (sm != null) {
            removeScope(sm.scopeKey, memoryId);
        }
        vectorStorePort.delete(collection, memoryId);
        notifyDelete(scopeOf(null, null, null), memoryId);
    }

    @Override
    public void deleteAll(ConversationMemoryScope scope) {
        String scopeKey = scopeKey(scope.getUserId(), scope.getAgentId(), scope.getRunId());
        if (scope.isEmpty()) {
            throw new ConversationMemoryException("vector memory deleteAll requires at least one scope identifier");
        }
        List<String> ids = byScope.remove(scopeKey);
        if (ids != null) {
            for (String id : ids) {
                store.remove(id);
                vectorStorePort.delete(collection, id);
            }
        }
        notifyDelete(scope, null);
    }

    private void ensureCollection(int dim) {
        if (dimension == dim) {
            return;
        }
        if (dimension != 0 && dimension != dim) {
            throw new DimensionMismatchException(
                    "vector memory collection dimension mismatch: existing " + dimension + " vs " + dim);
        }
        vectorStorePort.ensureCollection(collection, dim);
        dimension = dim;
    }

    private void notifyList(ConversationMemoryScope scope) {
        for (ConversationMemoryListener l : listeners) {
            try {
                l.onSearch(scope, Collections.<MemoryRecord>emptyList());
            } catch (RuntimeException ignore) {
                // 监听器不应阻断主流程
            }
        }
    }

    private MemoryRecord fromHit(ScoredDocument hit) {
        Document doc = hit.getDocument();
        if (doc == null) {
            return null;
        }
        Map<String, Object> meta = doc.getMetadata();
        String memory = meta == null ? null : asText(meta.get("memory"));
        StoredMemory local = store.get(doc.getId());
        if (memory == null && local != null) {
            memory = local.memory;
        }
        if (memory == null) {
            memory = doc.getContent();
        }
        return MemoryRecord.builder()
                .id(doc.getId())
                .memory(memory)
                .score(hit.getScore())
                .event("NONE")
                .userId(meta == null ? null : asText(meta.get("user_id")))
                .agentId(meta == null ? null : asText(meta.get("agent_id")))
                .runId(meta == null ? null : asText(meta.get("run_id")))
                .build();
    }

    private static String asText(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static MemoryRecord toRecord(StoredMemory sm, String event) {
        return MemoryRecord.builder()
                .id(sm.id).memory(sm.memory).event(event)
                .userId(sm.userId).agentId(sm.agentId).runId(sm.runId).build();
    }

    private void indexScope(String scopeKey, String id) {
        List<String> ids = byScope.get(scopeKey);
        if (ids == null) {
            ids = new CopyOnWriteArrayList<String>();
            List<String> prev = byScope.putIfAbsent(scopeKey, ids);
            if (prev != null) {
                ids = prev;
            }
        }
        ids.add(id);
    }

    private void removeScope(String scopeKey, String id) {
        List<String> ids = byScope.get(scopeKey);
        if (ids != null) {
            ids.remove(id);
        }
    }

    private static boolean matchesScope(String queryKey, String userId, String agentId, String runId) {
        if (queryKey == null || queryKey.isEmpty()) {
            return true;
        }
        return queryKey.equals(scopeKey(userId, agentId, runId));
    }

    private static String scopeKey(String userId, String agentId, String runId) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId == null ? "" : userId).append('|')
                .append(agentId == null ? "" : agentId).append('|')
                .append(runId == null ? "" : runId);
        return sb.toString();
    }

    private static void putScope(Map<String, Object> metadata, String userId, String agentId, String runId) {
        if (userId != null && !userId.trim().isEmpty()) {
            metadata.put("user_id", userId);
        }
        if (agentId != null && !agentId.trim().isEmpty()) {
            metadata.put("agent_id", agentId);
        }
        if (runId != null && !runId.trim().isEmpty()) {
            metadata.put("run_id", runId);
        }
    }

    private static ConversationMemoryScope scopeOf(ConversationMemoryAddRequest request) {
        return ConversationMemoryScope.builder()
                .userId(request.getUserId()).agentId(request.getAgentId()).runId(request.getRunId()).build();
    }

    private static ConversationMemoryScope scopeOf(ConversationMemorySearchRequest request) {
        return ConversationMemoryScope.builder()
                .userId(request.getUserId()).agentId(request.getAgentId()).runId(request.getRunId()).build();
    }

    private static ConversationMemoryScope scopeOf(String userId, String agentId, String runId) {
        return ConversationMemoryScope.builder()
                .userId(userId).agentId(agentId).runId(runId).build();
    }

    private static final class StoredMemory {
        final String id;
        final String memory;
        final String scopeKey;
        final String userId;
        final String agentId;
        final String runId;

        StoredMemory(String id, String memory, String scopeKey, String userId, String agentId, String runId) {
            this.id = id;
            this.memory = memory;
            this.scopeKey = scopeKey;
            this.userId = userId;
            this.agentId = agentId;
            this.runId = runId;
        }
    }
}
