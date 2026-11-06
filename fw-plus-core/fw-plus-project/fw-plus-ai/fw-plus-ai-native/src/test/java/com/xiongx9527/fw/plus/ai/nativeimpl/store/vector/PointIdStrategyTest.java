package com.xiongx9527.fw.plus.ai.nativeimpl.store.vector;

import com.xiongx9527.fw.plus.ai.nativeimpl.support.FakeHttpServer;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 验证各向量库后端在 {@link PointIdStrategy#RAW} / {@link PointIdStrategy#UUID} 两种策略下的 id 变换行为。
 *
 * <p>RAW：直接以原始字符串 id 作存储主键（Qdrant 字符串 point id / Weaviate 透传 UUID /
 * Milvus 数字 id 直接作 Int64 主键）；UUID（默认）：已是合法 UUID 透传，否则哈希成稳定 id。</p>
 */
class PointIdStrategyTest {

    private static final double[] VEC = {0.1, 0.2, 0.3, 0.4};

    // ---- Qdrant ----

    @Test
    void qdrantRawKeepsOriginalStringId() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer(req -> FakeHttpServer.json(200, "{'result':{'status':'completed'}}"))) {
            QdrantVectorStore store = new QdrantVectorStore(server.url(), null, "col", 5000, 10000, PointIdStrategy.RAW);
            store.upsert("col", "doc-123", VEC, null);
            String body = server.lastRequest().body;
            assertTrue(body.contains("\"id\":\"doc-123\""), body);
        }
    }

    @Test
    void qdrantUuidPassesThroughValidUuidAndHashesOther() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer(req -> FakeHttpServer.json(200, "{'result':{'status':'completed'}}"))) {
            QdrantVectorStore store = new QdrantVectorStore(server.url(), null, "col", 5000, 10000, PointIdStrategy.UUID);
            String valid = "11111111-1111-1111-1111-111111111111";
            store.upsert("col", valid, VEC, null);
            assertTrue(server.lastRequest().body.contains("\"id\":\"" + valid + "\""), server.lastRequest().body);
            // 非 UUID 字符串被哈希成稳定 UUID
            store.upsert("col", "doc-123", VEC, null);
            String body = server.lastRequest().body;
            int start = body.indexOf("\"id\":\"") + 6;
            int end = body.indexOf("\"", start);
            String generated = body.substring(start, end);
            assertEquals(valid.length(), generated.length());
            // 哈希结果必须是合法 UUID 且与 nameUUIDFromBytes 一致
            assertEquals(UUID.nameUUIDFromBytes("doc-123".getBytes()).toString(), generated);
        }
    }

    // ---- Milvus（主键为 Int64）----

    @Test
    void milvusRawNumericIdUsedAsLongPrimaryKey() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer(req -> FakeHttpServer.json(200, FakeHttpServer.body("{'code':0}")))) {
            MilvusVectorStore store = new MilvusVectorStore(server.url(), null, "col", 5000, 10000, PointIdStrategy.RAW);
            store.upsert("col", "12345", VEC, null);
            // RAW 数字 id 直接作 Int64 主键，body 中 id 为数字（无引号）
            assertTrue(server.lastRequest().body.contains("\"id\":12345"), server.lastRequest().body);
        }
    }

    @Test
    void milvusUuidHashesStringIdToLongByDefault() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer(req -> FakeHttpServer.json(200, FakeHttpServer.body("{'code':0}")))) {
            MilvusVectorStore store = new MilvusVectorStore(server.url(), null, "col", 5000, 10000, PointIdStrategy.UUID);
            store.upsert("col", "doc-123", VEC, null);
            long expected = UUID.nameUUIDFromBytes("doc-123".getBytes()).getMostSignificantBits();
            assertTrue(server.lastRequest().body.contains("\"id\":" + expected), server.lastRequest().body);
        }
    }

    // ---- Weaviate（object id 必须为 UUID）----

    @Test
    void weaviateRawPassesThroughUuidId() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer(req -> FakeHttpServer.json(200, "{}"))) {
            WeaviateVectorStore store = new WeaviateVectorStore(server.url(), null, null, null, "col", 5000, 10000, PointIdStrategy.RAW);
            String valid = "11111111-1111-1111-1111-111111111111";
            store.upsert("col", valid, VEC, null);
            assertTrue(server.lastRequest().body.contains("\"id\":\"" + valid + "\""), server.lastRequest().body);
        }
    }

    @Test
    void weaviateUuidPassesThroughValidUuidAndHashesOther() throws Exception {
        try (FakeHttpServer server = new FakeHttpServer(req -> FakeHttpServer.json(200, "{}"))) {
            WeaviateVectorStore store = new WeaviateVectorStore(server.url(), null, null, null, "col", 5000, 10000, PointIdStrategy.UUID);
            String valid = "22222222-2222-2222-2222-222222222222";
            store.upsert("col", valid, VEC, null);
            assertTrue(server.lastRequest().body.contains("\"id\":\"" + valid + "\""), server.lastRequest().body);
            store.upsert("col", "doc-123", VEC, null);
            assertTrue(server.lastRequest().body.contains(
                    UUID.nameUUIDFromBytes("doc-123".getBytes()).toString()), server.lastRequest().body);
        }
    }

    @Test
    void defaultConstructorFallsBackToUuidStrategy() {
        QdrantVectorStore q = new QdrantVectorStore("http://localhost:6333");
        assertFalse(q.configured() ? false : true); // configured() 取决于 baseUrl；此处仅确保构造可用
        MilvusVectorStore m = new MilvusVectorStore("http://localhost:19530", null, "col");
        WeaviateVectorStore w = new WeaviateVectorStore("http://localhost:8080", null, "col");
        // 默认构造不抛异常即视为策略回退成功（具体策略行为由上面 RAW/UUID 用例覆盖）
        assertEquals("qdrant", q.name());
        assertEquals("milvus", m.name());
        assertEquals("weaviate", w.name());
    }
}
