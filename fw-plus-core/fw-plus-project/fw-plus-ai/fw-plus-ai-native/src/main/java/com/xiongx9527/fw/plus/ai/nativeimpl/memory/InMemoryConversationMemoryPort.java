package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.AbstractConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryListener;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import com.xiongx9527.fw.plus.ai.base.memory.MemoryRecord;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 进程内对话记忆后端：不依赖任何外部服务，适用于单机 / 测试 / 离线兜底。
 *
 * <p>不做 LLM 事实抽取（无外部 LLM 依赖），而是把每条消息文本直接作为一条记忆存储；
 * 检索用朴素词重叠打分，返回按相关度降序的命中。语义检索能力弱于 mem0 / 向量后端，
 * 但零依赖、可预测，是默认兜底与契约测试的首选。</p>
 */
public class InMemoryConversationMemoryPort extends AbstractConversationMemoryPort {

    private final Map<String, StoredMemory> store = new ConcurrentHashMap<String, StoredMemory>();
    /** 作用域 → 记忆 id 列表，支撑 list / deleteAll。 */
    private final Map<String, List<String>> byScope = new ConcurrentHashMap<String, List<String>>();

    public InMemoryConversationMemoryPort() {
        this(null);
    }

    public InMemoryConversationMemoryPort(List<ConversationMemoryListener> listeners) {
        super(listeners);
    }

    @Override
    public List<MemoryRecord> add(ConversationMemoryAddRequest request) {
        if (request == null || request.getMessages().isEmpty()) {
            throw new ConversationMemoryException("in-memory add requires at least one message");
        }
        List<MemoryRecord> records = new ArrayList<MemoryRecord>();
        for (ChatMessage m : request.getMessages()) {
            String text = m.getContent();
            if (text == null || text.trim().isEmpty()) {
                continue;
            }
            String id = UUID.randomUUID().toString();
            StoredMemory sm = new StoredMemory(id, text,
                    scopeKey(request.getUserId(), request.getAgentId(), request.getRunId()),
                    request.getUserId(), request.getAgentId(), request.getRunId());
            store.put(id, sm);
            indexScope(sm.scopeKey, id);
            records.add(toRecord(sm, "ADD"));
        }
        notifyAdd(scopeOf(request.getUserId(), request.getAgentId(), request.getRunId()), records);
        return records;
    }

    @Override
    public List<MemoryRecord> search(ConversationMemorySearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new ConversationMemoryException("in-memory search requires a query");
        }
        String scopeKey = scopeKey(request.getUserId(), request.getAgentId(), request.getRunId());
        List<ScoredString> scored = new ArrayList<ScoredString>();
        for (StoredMemory sm : store.values()) {
            if (!matchesScope(sm.scopeKey, scopeKey)) {
                continue;
            }
            double score = overlap(request.getQuery(), sm.memory);
            if (score > 0) {
                scored.add(new ScoredString(sm, score));
            } else if (scopeKey.isEmpty()) {
                // 未限定作用域时，即便无重叠也纳入候选（避免空结果）
                scored.add(new ScoredString(sm, 0d));
            }
        }
        Collections.sort(scored, (a, b) -> Double.compare(b.score, a.score));
        int topK = request.getTopK() != null ? request.getTopK() : scored.size();
        List<MemoryRecord> records = new ArrayList<MemoryRecord>();
        for (int i = 0; i < Math.min(topK, scored.size()); i++) {
            records.add(toRecord(scored.get(i).sm, "NONE"));
            records.get(records.size() - 1).hashCode(); // no-op to keep style; score preset below
            // 覆盖分数
            records.set(records.size() - 1, withScore(toRecord(scored.get(i).sm, "NONE"), scored.get(i).score));
        }
        notifySearch(scopeOf(request.getUserId(), request.getAgentId(), request.getRunId()), records);
        return records;
    }

    @Override
    public List<MemoryRecord> list(ConversationMemoryScope scope) {
        String scopeKey = scopeKey(scope.getUserId(), scope.getAgentId(), scope.getRunId());
        List<MemoryRecord> records = new ArrayList<MemoryRecord>();
        for (StoredMemory sm : store.values()) {
            if (matchesScope(sm.scopeKey, scopeKey)) {
                records.add(toRecord(sm, "NONE"));
            }
        }
        notifyList(scope);
        return records;
    }

    @Override
    public void delete(String memoryId) {
        StoredMemory sm = store.remove(memoryId);
        if (sm != null) {
            removeScope(sm.scopeKey, memoryId);
        }
        notifyDelete(scopeOf(null, null, null), memoryId);
    }

    @Override
    public void deleteAll(ConversationMemoryScope scope) {
        String scopeKey = scopeKey(scope.getUserId(), scope.getAgentId(), scope.getRunId());
        if (scope.isEmpty()) {
            throw new ConversationMemoryException("in-memory deleteAll requires at least one scope identifier");
        }
        List<String> ids = byScope.remove(scopeKey);
        if (ids != null) {
            for (String id : ids) {
                store.remove(id);
            }
        }
        notifyDelete(scope, null);
    }

    private void notifyList(ConversationMemoryScope scope) {
        // list 不触发 add/search 语义的监听，这里仅保持与 delete 一致的可见性
        for (ConversationMemoryListener l : listeners) {
            try {
                l.onSearch(scope, Collections.<MemoryRecord>emptyList());
            } catch (RuntimeException ignore) {
                // 监听器不应阻断主流程
            }
        }
    }

    private static MemoryRecord toRecord(StoredMemory sm, String event) {
        return MemoryRecord.builder()
                .id(sm.id).memory(sm.memory).event(event)
                .userId(sm.userId).agentId(sm.agentId).runId(sm.runId).build();
    }

    private static MemoryRecord withScore(MemoryRecord base, double score) {
        return MemoryRecord.builder()
                .id(base.getId()).memory(base.getMemory()).score(score).event(base.getEvent())
                .userId(base.getUserId()).agentId(base.getAgentId()).runId(base.getRunId()).build();
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

    private static boolean matchesScope(String storedKey, String queryKey) {
        if (queryKey == null || queryKey.isEmpty()) {
            return true;
        }
        return queryKey.equals(storedKey);
    }

    private static String scopeKey(String userId, String agentId, String runId) {
        StringBuilder sb = new StringBuilder();
        sb.append(userId == null ? "" : userId).append('|')
                .append(agentId == null ? "" : agentId).append('|')
                .append(runId == null ? "" : runId);
        return sb.toString();
    }

    private static ConversationMemoryScope scopeOf(String userId, String agentId, String runId) {
        return ConversationMemoryScope.builder()
                .userId(userId).agentId(agentId).runId(runId).build();
    }

    /** 朴素词重叠打分：查询词与记忆词的交集比例（Jaccard 简化）。 */
    private static double overlap(String query, String memory) {
        if (query == null || memory == null) {
            return 0d;
        }
        java.util.Set<String> q = tokenize(query);
        java.util.Set<String> m = tokenize(memory);
        if (q.isEmpty() || m.isEmpty()) {
            return 0d;
        }
        int inter = 0;
        for (String t : q) {
            if (m.contains(t)) {
                inter++;
            }
        }
        return (double) inter / (double) Math.max(q.size(), m.size());
    }

    private static java.util.Set<String> tokenize(String text) {
        java.util.Set<String> set = new java.util.HashSet<String>();
        for (String t : text.toLowerCase().split("\\W+")) {
            if (t.length() > 1) {
                set.add(t);
            }
        }
        return set;
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

    private static final class ScoredString {
        final StoredMemory sm;
        final double score;

        ScoredString(StoredMemory sm, double score) {
            this.sm = sm;
            this.score = score;
        }
    }
}
