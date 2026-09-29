package dev.langchain4j.store.embedding.nebula;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.vesoft.nebula.client.graph.data.Node;
import com.vesoft.nebula.client.graph.data.ValueWrapper;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.graph.nebula.NebulaException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static dev.langchain4j.store.graph.nebula.NebulaUtils.quote;
import static dev.langchain4j.store.graph.nebula.NebulaUtils.toText;

/**
 * {@link NebulaEmbeddingStore} 的常量与转换工具。
 *
 * <p>NebulaGraph 没有 Neo4j 那种向量索引，也没有 {@code db.create.setNodeVectorProperty} 之类的原生向量写入，
 * 因此向量以 JSON 数组文本存成 tag 的 string 属性，检索时在 Java 侧计算余弦相似度。
 */
class NebulaEmbeddingUtils {

    /* default configs */
    static final String DEFAULT_TAG = "Document";
    static final String DEFAULT_ID_PROP = "id";
    static final String DEFAULT_TEXT_PROP = "text";
    static final String DEFAULT_EMBEDDING_PROP = "embedding";
    static final String DEFAULT_METADATA_PROP = "metadata";
    static final int DEFAULT_MAX_SCAN_ROWS = 10_000;
    /** NebulaGraph 的 DDL 需要心跳同步，等待 tag 可见的超时时间（毫秒） */
    static final long DEFAULT_AWAIT_SCHEMA_TIMEOUT = 60_000L;
    static final long AWAIT_SCHEMA_POLL_INTERVAL = 1_000L;

    private NebulaEmbeddingUtils() {
    }

    /**
     * 读取点的 tag 属性。
     */
    static Map<String, ValueWrapper> properties(NebulaEmbeddingStore store, Node node) {

        try {
            Map<String, ValueWrapper> properties = node.properties(store.getTag());
            return properties == null ? new HashMap<String, ValueWrapper>() : properties;
        } catch (Exception e) {
            throw new NebulaException("Error reading the properties of the NebulaGraph vertex: " + node, e);
        }
    }

    static String text(Map<String, ValueWrapper> properties, String property) {

        ValueWrapper value = properties.get(property);
        return value == null ? null : toText(value);
    }

    static EmbeddingMatch<TextSegment> toEmbeddingMatch(NebulaEmbeddingStore store,
                                                        Map<String, ValueWrapper> properties,
                                                        double score) {

        String id = text(properties, store.getIdProperty());
        String text = text(properties, store.getTextProperty());
        List<Float> vector = parseEmbedding(text(properties, store.getEmbeddingProperty()));

        Metadata metadata = parseMetadata(text(properties, store.getMetadataProperty()));
        TextSegment textSegment = text == null || text.isEmpty() ? null : TextSegment.from(text, metadata);

        return new EmbeddingMatch<>(score, id, Embedding.from(vector), textSegment);
    }

    /**
     * 生成写入单个文档的 nGQL。NebulaGraph 的 {@code UPSERT VERTEX} 一次只能处理一个 vid，
     * 因此每个文档一条语句。
     */
    static String toUpsertStatement(NebulaEmbeddingStore store, String id, Embedding embedding, TextSegment embedded) {

        String text = embedded == null ? "" : embedded.text();
        String metadata = embedded == null || embedded.metadata() == null
                ? "{}"
                : JSONUtil.toJsonStr(embedded.metadata().asMap());

        return new StringBuilder("UPSERT VERTEX ON ")
                .append(store.getTag())
                .append(" ")
                .append(quote(id))
                .append(" SET ")
                .append(store.getIdProperty()).append(" = ").append(quote(id)).append(", ")
                .append(store.getTextProperty()).append(" = ").append(quote(text)).append(", ")
                .append(store.getEmbeddingProperty()).append(" = ").append(quote(toEmbeddingString(embedding.vectorAsList()))).append(", ")
                .append(store.getMetadataProperty()).append(" = ").append(quote(metadata))
                .toString();
    }

    static String toEmbeddingString(List<Float> vector) {

        return JSONUtil.toJsonStr(vector);
    }

    static List<Float> parseEmbedding(String value) {

        List<Float> vector = new ArrayList<>();
        if (value == null || value.trim().isEmpty()) {
            return vector;
        }
        JSONArray array = JSONUtil.parseArray(value);
        for (int i = 0; i < array.size(); i++) {
            Object item = array.get(i);
            vector.add(item == null ? 0F : Float.parseFloat(item.toString()));
        }
        return vector;
    }

    static Metadata parseMetadata(String value) {

        Map<String, String> metadata = new HashMap<>();
        if (value != null && !value.trim().isEmpty()) {
            JSONObject json = JSONUtil.parseObj(value);
            for (Map.Entry<String, Object> entry : json.entrySet()) {
                Object item = entry.getValue();
                metadata.put(entry.getKey(), item == null ? null : item.toString());
            }
        }
        return new Metadata(metadata);
    }

    /**
     * 余弦相似度。NebulaGraph 没有向量索引，相似度只能在 Java 侧计算。
     */
    static double cosineSimilarity(List<Float> reference, List<Float> candidate) {

        if (reference == null || candidate == null
                || reference.isEmpty() || reference.size() != candidate.size()) {
            return 0D;
        }
        double dotProduct = 0D;
        double referenceNorm = 0D;
        double candidateNorm = 0D;
        for (int i = 0; i < reference.size(); i++) {
            float referenceValue = reference.get(i);
            float candidateValue = candidate.get(i);
            dotProduct += referenceValue * candidateValue;
            referenceNorm += referenceValue * referenceValue;
            candidateNorm += candidateValue * candidateValue;
        }
        if (referenceNorm == 0D || candidateNorm == 0D) {
            return 0D;
        }
        return dotProduct / (Math.sqrt(referenceNorm) * Math.sqrt(candidateNorm));
    }
}
