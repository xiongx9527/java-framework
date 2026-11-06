package com.xiongx9527.fw.plus.ai.nativeimpl.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryAddRequest;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryException;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryScope;
import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemorySearchRequest;
import com.xiongx9527.fw.plus.ai.base.memory.MemoryRecord;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import com.xiongx9527.fw.plus.ai.base.store.vector.Document;
import com.xiongx9527.fw.plus.ai.base.store.vector.ScoredDocument;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchRequest;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorSearchResult;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VectorConversationMemoryPortTest {

    /** 确定性嵌入：把文本 token 落到固定维度向量（含 CJK 支持）。 */
    static final class FakeEmbeddingPort implements EmbeddingPort {
        @Override
        public double[] embed(String text) {
            if (text == null) {
                return null;
            }
            double[] v = new double[4];
            String lower = text.toLowerCase();
            StringBuilder latin = new StringBuilder();
            for (int i = 0; i < lower.length(); i++) {
                char c = lower.charAt(i);
                if (isCjk(c)) {
                    if (latin.length() > 0) {
                        addHash(v, latin.toString());
                        latin.setLength(0);
                    }
                    addHash(v, String.valueOf(c));
                } else if (Character.isLetterOrDigit(c)) {
                    latin.append(c);
                } else {
                    if (latin.length() > 0) {
                        addHash(v, latin.toString());
                        latin.setLength(0);
                    }
                }
            }
            if (latin.length() > 0) {
                addHash(v, latin.toString());
            }
            return v;
        }

        private static void addHash(double[] v, String t) {
            for (int i = 0; i < v.length; i++) {
                v[i] += Math.abs((double) (t.hashCode() + i)) / 1e6;
            }
        }

        private static boolean isCjk(char c) {
            return (c >= 0x3400 && c <= 0x4DBF)
                    || (c >= 0x4E00 && c <= 0x9FFF)
                    || (c >= 0xF900 && c <= 0xFAFF);
        }

        @Override
        public int dimension() {
            return 4;
        }
    }

    /** 内存向量库：按余弦相似度检索。 */
    static class FakeVectorStore implements VectorStorePort {
        final Map<String, Map<String, Entry>> collections = new LinkedHashMap<String, Map<String, Entry>>();

        static final class Entry {
            final String id;
            final double[] vector;
            final Map<String, Object> metadata;

            Entry(String id, double[] vector, Map<String, Object> metadata) {
                this.id = id;
                this.vector = vector;
                this.metadata = metadata;
            }
        }

        private Map<String, Entry> col(String c) {
            return collections.computeIfAbsent(c, k -> new LinkedHashMap<String, Entry>());
        }

        @Override
        public boolean health() {
            return true;
        }

        @Override
        public String name() {
            return "fake";
        }

        @Override
        public com.xiongx9527.fw.plus.ai.base.store.StoreType type() {
            return com.xiongx9527.fw.plus.ai.base.store.StoreType.VECTOR;
        }

        @Override
        public String defaultCollection() {
            return "conversation_memory";
        }

        @Override
        public void ensureCollection(String collection, int dimension) {
            col(collection);
        }

        @Override
        public void dropCollection(String collection) {
            collections.remove(collection);
        }

        @Override
        public void upsert(String collection, String id, double[] vector, Map<String, Object> metadata) {
            col(collection).put(id, new Entry(id, vector, metadata));
        }

        @Override
        public void delete(String collection, String id) {
            Map<String, Entry> m = collections.get(collection);
            if (m != null) {
                m.remove(id);
            }
        }

        @Override
        public VectorSearchResult search(VectorSearchRequest request) {
            Map<String, Entry> m = collections.get(request.getCollection());
            List<ScoredDocument> hits = new ArrayList<ScoredDocument>();
            if (m != null) {
                for (Entry e : m.values()) {
                    double score = cosine(request.getVector(), e.vector);
                    if (score > 0) {
                        hits.add(new ScoredDocument(Document.builder()
                                .id(e.id).content((String) e.metadata.get("memory"))
                                .metadata(e.metadata).build(), score));
                    }
                }
            }
            hits.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));
            int topK = request.getTopK() > 0 ? request.getTopK() : hits.size();
            List<ScoredDocument> limited = new ArrayList<ScoredDocument>();
            for (int i = 0; i < Math.min(topK, hits.size()); i++) {
                limited.add(hits.get(i));
            }
            return VectorSearchResult.of(limited);
        }

        private static double cosine(double[] a, double[] b) {
            double dot = 0, na = 0, nb = 0;
            for (int i = 0; i < a.length; i++) {
                dot += a[i] * b[i];
                na += a[i] * a[i];
                nb += b[i] * b[i];
            }
            return (na == 0 || nb == 0) ? 0 : dot / (Math.sqrt(na) * Math.sqrt(nb));
        }
    }

    private final FakeEmbeddingPort embedding = new FakeEmbeddingPort();
    private final FakeVectorStore vectorStore = new FakeVectorStore();

    @Test
    void addUpsertsEmbeddingsAndStoresLocally() {
        VectorConversationMemoryPort port = new VectorConversationMemoryPort(embedding, vectorStore);
        List<MemoryRecord> records = port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("我喜欢吃苹果")).userId("u1").build());
        assertThat(records).hasSize(1);
        assertThat(vectorStore.collections.get("conversation_memory")).hasSize(1);
    }

    @Test
    void searchReturnsSemanticHitsByScope() {
        VectorConversationMemoryPort port = new VectorConversationMemoryPort(embedding, vectorStore);
        port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("我对芒果过敏")).userId("u1").build());
        port.add(ConversationMemoryAddRequest.builder()
                .message(ChatMessage.user("今天天气不错")).userId("u1").build());

        List<MemoryRecord> hits = port.search(ConversationMemorySearchRequest.builder()
                .query("芒果 过敏").userId("u1").build());
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).getMemory()).contains("芒果");
    }

    @Test
    void listAndDeleteAllOperateOnScope() {
        VectorConversationMemoryPort port = new VectorConversationMemoryPort(embedding, vectorStore);
        port.add(ConversationMemoryAddRequest.builder().message(ChatMessage.user("a")).userId("u1").build());
        port.add(ConversationMemoryAddRequest.builder().message(ChatMessage.user("b")).userId("u1").build());
        assertThat(port.list(ConversationMemoryScope.builder().userId("u1").build())).hasSize(2);
        port.deleteAll(ConversationMemoryScope.builder().userId("u1").build());
        assertThat(port.list(ConversationMemoryScope.builder().userId("u1").build())).isEmpty();
        assertThat(vectorStore.collections.get("conversation_memory")).isEmpty();
    }

    @Test
    void requiresConfiguredVectorStore() {
        FakeVectorStore unconfigured = new FakeVectorStore() {
            @Override
            public boolean configured() {
                return false;
            }
        };
        assertThatThrownBy(() -> new VectorConversationMemoryPort(embedding, unconfigured))
                .isInstanceOf(ConversationMemoryException.class);
    }

    @Test
    void requiresEmbeddingPort() {
        assertThatThrownBy(() -> new VectorConversationMemoryPort(null, vectorStore))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
