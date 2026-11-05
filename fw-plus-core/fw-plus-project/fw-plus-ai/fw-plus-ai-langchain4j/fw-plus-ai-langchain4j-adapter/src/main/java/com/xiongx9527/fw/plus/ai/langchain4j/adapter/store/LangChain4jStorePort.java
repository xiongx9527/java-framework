package com.xiongx9527.fw.plus.ai.langchain4j.adapter.store;

import com.xiongx9527.fw.plus.ai.base.store.Store;
import com.xiongx9527.fw.plus.ai.base.store.StoreRecord;
import com.xiongx9527.fw.plus.ai.base.store.StoreType;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.Filter;
import com.xiongx9527.fw.plus.ai.base.store.vector.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcFilters;
import com.xiongx9527.fw.plus.ai.langchain4j.adapter.support.LcVectors;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.EmbeddingSearchResult;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.filter.comparison.IsEqualTo;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 把 LangChain4j 0.35 的 {@link EmbeddingStore}（{@code TextSegment} 载荷）适配成 base 的
 * {@link Store} / {@link VectorStorePort}。
 *
 * <p>0.35 只有 {@code EmbeddingStore} 一种存储抽象，不区分向量库 / 图库，因此本模块只提供
 * 这一个存储适配器；base 的 {@code GraphStorePort} 由 {@code fw-plus-ai-native} 实现。</p>
 *
 * <p><b>后端能力假设（重要）</b>：{@code EmbeddingStore} 接口的
 * {@code search(EmbeddingSearchRequest)} 默认实现只转发到 {@code findRelevant}，会忽略
 * {@code request.filter()}；{@code remove(String)} / {@code removeAll(Filter)} 默认实现直接抛
 * {@link UnsupportedOperationException}。像 {@code Neo4jEmbeddingStore} 就没有覆写它们。
 * 因此本适配器：</p>
 * <ul>
 *   <li><b>默认一律客户端过滤</b>：集合、业务 {@link Filter}、候选 id 都在客户端校验，
 *       并放大召回量后截断 topK；只有业务侧确认后端支持 filter 时，才用
 *       {@code pushdownFilters=true} 开启下推；</li>
 *   <li><b>删除不静默</b>：后端不支持 remove 时抛明确异常；可注入 {@link EmbeddingRemover}
 *       由业务侧补齐删除能力；</li>
 *   <li>把 {@code collection} 与业务 {@code id} 写进 {@code TextSegment.metadata}
 *       （{@link #KEY_COLLECTION} / {@link #KEY_ID}），检索时据此还原 {@link Document}；</li>
 *   <li>用 {@link #ensureCollection} 在进程内登记集合维度，写入时做一致性校验。</li>
 * </ul>
 *
 * <p><b>已知限制</b>：0.35 没有「指定 id + 携带载荷」的 add，重复 {@code upsert} 同一业务 id
 * 会生成新记录（旧记录成为孤儿）；{@code Neo4jEmbeddingStore} 把 metadata 值统一转成 String，
 * 数值类型的过滤与读取会退化。需要幂等写入或保留数值类型时，由业务侧包装
 * {@code EmbeddingStore} 提供。</p>
 */
public class LangChain4jStorePort implements VectorStorePort {

    /** 业务文档 id 在 {@code TextSegment.metadata} 中的保留键。 */
    public static final String KEY_ID = "__fw_id";

    /** 集合名在 {@code TextSegment.metadata} 中的保留键。 */
    public static final String KEY_COLLECTION = "__fw_collection";

    private static final String KEY_CONTENT = "content";

    private final EmbeddingStore<TextSegment> store;

    private final String name;

    private final String defaultCollection;

    private final boolean pushdownFilters;

    private final EmbeddingRemover remover;

    private final Map<String, Integer> dimensions = new ConcurrentHashMap<String, Integer>();

    private final Map<String, String> idIndex = new ConcurrentHashMap<String, String>();

    public LangChain4jStorePort(EmbeddingStore<TextSegment> store) {
        this(store, "langchain4j", "default", false, null);
    }

    public LangChain4jStorePort(EmbeddingStore<TextSegment> store, String name,
                                String defaultCollection) {
        this(store, name, defaultCollection, false, null);
    }

    public LangChain4jStorePort(EmbeddingStore<TextSegment> store, String name,
                                String defaultCollection, boolean pushdownFilters,
                                EmbeddingRemover remover) {
        if (store == null) {
            throw new IllegalArgumentException("embedding store is required");
        }
        this.store = store;
        this.name = name == null || name.trim().isEmpty() ? "langchain4j" : name;
        this.defaultCollection = defaultCollection == null || defaultCollection.trim().isEmpty()
                ? "default" : defaultCollection;
        this.pushdownFilters = pushdownFilters;
        this.remover = remover;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public StoreType type() {
        return StoreType.VECTOR;
    }

    @Override
    public boolean configured() {
        return true;
    }

    @Override
    public boolean health() {
        return true;
    }

    @Override
    public String defaultCollection() {
        return defaultCollection;
    }

    @Override
    public void ensureCollection(String collection, int dimension) {
        Integer existing = dimensions.get(collection);
        if (existing != null) {
            if (existing.intValue() != dimension) {
                throw new DimensionMismatchException("collection " + collection
                        + " expects dim " + existing + " but got " + dimension);
            }
            return;
        }
        dimensions.putIfAbsent(collection, Integer.valueOf(dimension));
    }

    @Override
    public Integer collectionDimension(String collection) {
        return dimensions.get(collection);
    }

    @Override
    public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
        if (collection == null || id == null || vector == null) {
            return;
        }
        Integer expected = dimensions.get(collection);
        if (expected != null && expected.intValue() != vector.length) {
            throw new DimensionMismatchException("collection " + collection
                    + " expects dim " + expected + " but got " + vector.length);
        }
        Map<String, Object> stored = new LinkedHashMap<String, Object>();
        if (metadata != null) {
            stored.putAll(metadata);
        }
        stored.put(KEY_ID, id);
        stored.put(KEY_COLLECTION, collection);
        Object content = metadata == null ? null : metadata.get(KEY_CONTENT);
        String text = content == null ? "" : String.valueOf(content);
        String embeddingId = store.add(LcVectors.toEmbedding(vector),
                TextSegment.from(text, toMetadata(stored)));
        if (embeddingId != null) {
            idIndex.put(indexKey(collection, id), embeddingId);
        }
        dimensions.putIfAbsent(collection, Integer.valueOf(vector.length));
    }

    @Override
    public void delete(String collection, String id) {
        if (collection == null || id == null) {
            return;
        }
        String embeddingId = idIndex.remove(indexKey(collection, id));
        if (embeddingId == null) {
            removeWhere(new IsEqualTo(KEY_ID, id).and(new IsEqualTo(KEY_COLLECTION, collection)));
            return;
        }
        removeById(embeddingId);
    }

    @Override
    public void dropCollection(String collection) {
        if (collection == null) {
            return;
        }
        dimensions.remove(collection);
        String prefix = collection + "\u0000";
        List<String> keys = new ArrayList<String>();
        for (String key : idIndex.keySet()) {
            if (key.startsWith(prefix)) {
                keys.add(key);
            }
        }
        for (String key : keys) {
            String embeddingId = idIndex.remove(key);
            if (embeddingId != null) {
                removeById(embeddingId);
            }
        }
        removeWhere(new IsEqualTo(KEY_COLLECTION, collection));
    }

    @Override
    public void deleteByMetadata(String collection, String key, Object value) {
        if (collection == null || key == null) {
            return;
        }
        removeWhere(new IsEqualTo(key, value).and(new IsEqualTo(KEY_COLLECTION, collection)));
    }

    @Override
    public VectorSearchResult search(VectorSearchRequest request) {
        if (request == null || request.getVector() == null) {
            return VectorSearchResult.empty();
        }
        String collection = request.getCollection() == null
                ? defaultCollection : request.getCollection();
        int topK = request.getTopK() > 0 ? request.getTopK() : 10;
        Filter filter = request.getFilter();
        Set<String> candidates = request.getCandidateIds();
        boolean hasCandidates = candidates != null && !candidates.isEmpty();

        dev.langchain4j.store.embedding.filter.Filter pushed = null;
        boolean pushedAll = false;
        if (pushdownFilters && !hasCandidates) {
            dev.langchain4j.store.embedding.filter.Filter business = LcFilters.toUpstream(filter);
            boolean canPushBusiness = filter == null || filter.isNoOp() || business != null;
            if (canPushBusiness) {
                pushed = new IsEqualTo(KEY_COLLECTION, collection);
                if (business != null) {
                    pushed = pushed.and(business);
                }
                pushedAll = true;
            }
        }
        int fetch = pushedAll ? topK : Math.max(topK * 10, 200);

        EmbeddingSearchRequest upstream = EmbeddingSearchRequest.builder()
                .queryEmbedding(LcVectors.toEmbedding(request.getVector()))
                .maxResults(Integer.valueOf(fetch))
                .minScore(Double.valueOf(0.0))
                .filter(pushed)
                .build();
        EmbeddingSearchResult<TextSegment> result;
        try {
            result = store.search(upstream);
        } catch (RuntimeException e) {
            return VectorSearchResult.empty();
        }
        if (result == null || result.matches() == null) {
            return VectorSearchResult.empty();
        }
        List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
        for (EmbeddingMatch<TextSegment> match : result.matches()) {
            if (match == null || match.embedded() == null) {
                continue;
            }
            TextSegment segment = match.embedded();
            Metadata raw = segment.metadata();
            // 集合隔离始终在客户端校验，不依赖后端是否尊重 filter
            if (raw == null || !collection.equals(raw.get(KEY_COLLECTION))) {
                continue;
            }
            Document doc = toDocument(segment);
            if (hasCandidates && !candidates.contains(doc.getId())) {
                continue;
            }
            if (filter != null && !filter.isNoOp() && !filter.matches(doc.getMetadata())) {
                continue;
            }
            double score = match.score() == null ? 0.0 : match.score().doubleValue();
            hits.add(new ScoredDocument(doc, score));
        }
        hits.sort((left, right) -> Double.compare(right.getScore(), left.getScore()));
        if (hits.size() > topK) {
            hits = new ArrayList<ScoredDocument>(hits.subList(0, topK));
        }
        return VectorSearchResult.of(hits);
    }

    /** 向量库不支持按 id 随机读；返回 {@code null}，请改用 {@code search}。 */
    @Override
    public StoreRecord get(String id) {
        return null;
    }

    @Override
    public List<StoreRecord> getAll(Collection<String> ids) {
        return new ArrayList<StoreRecord>();
    }

    private void removeById(String embeddingId) {
        if (remover != null) {
            remover.remove(embeddingId);
            return;
        }
        try {
            store.remove(embeddingId);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "embedding store does not support remove; inject an EmbeddingRemover", e);
        }
    }

    private void removeWhere(dev.langchain4j.store.embedding.filter.Filter filter) {
        if (remover != null) {
            remover.removeAll(filter);
            return;
        }
        try {
            store.removeAll(filter);
        } catch (UnsupportedOperationException e) {
            throw new UnsupportedOperationException(
                    "embedding store does not support removeAll(filter); inject an EmbeddingRemover", e);
        }
    }

    private static Document toDocument(TextSegment segment) {
        Metadata metadata = segment.metadata();
        Map<String, Object> values = new LinkedHashMap<String, Object>();
        String id = null;
        if (metadata != null) {
            values.putAll(metadata.toMap());
            id = metadata.get(KEY_ID);
            values.remove(KEY_ID);
            values.remove(KEY_COLLECTION);
        }
        return Document.builder()
                .id(id)
                .content(segment.text())
                .metadata(values)
                .build();
    }

    private static Metadata toMetadata(Map<String, Object> values) {
        Metadata metadata = new Metadata();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof Integer) {
                metadata.put(entry.getKey(), ((Integer) value).intValue());
            } else if (value instanceof Long) {
                metadata.put(entry.getKey(), ((Long) value).longValue());
            } else if (value instanceof Double) {
                metadata.put(entry.getKey(), ((Double) value).doubleValue());
            } else if (value instanceof Float) {
                metadata.put(entry.getKey(), ((Float) value).floatValue());
            } else if (value instanceof UUID) {
                metadata.put(entry.getKey(), (UUID) value);
            } else {
                metadata.put(entry.getKey(), String.valueOf(value));
            }
        }
        return metadata;
    }

    private static String indexKey(String collection, String id) {
        return collection + "\u0000" + id;
    }
}
