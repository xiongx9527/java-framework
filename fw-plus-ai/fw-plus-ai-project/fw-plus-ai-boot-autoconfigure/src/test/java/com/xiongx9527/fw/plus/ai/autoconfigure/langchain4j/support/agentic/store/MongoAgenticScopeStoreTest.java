package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic.store;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import dev.langchain4j.agentic.scope.AgenticScopeKey;
import dev.langchain4j.agentic.scope.DefaultAgenticScope;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link MongoAgenticScopeStore} 单元测试：JSON 往返、复合 {@code _id} 归一、构建器校验与资源关闭。
 *
 * <p>不连接真实 MongoDB —— 构造 MongoClient 不触网（未配置 ttl 时也不建索引）。</p>
 */
class MongoAgenticScopeStoreTest {

    private static final String LOCAL_URI = "mongodb://localhost:27017";

    @Test
    void scopeDocumentShouldRoundTripState() {
        DefaultAgenticScope scope = DefaultAgenticScope.ephemeralAgenticScope();
        scope.writeState("answer", "42");

        Document document = MongoAgenticScopeStore.scopeToDocument(new AgenticScopeKey("a", "m"), scope);
        DefaultAgenticScope restored = MongoAgenticScopeStore.documentToScope(document);

        assertThat(restored.readState("answer")).isEqualTo("42");
    }

    @Test
    void scopeDocumentShouldCarryJsonBodyAndUpdatedAt() {
        Document document = MongoAgenticScopeStore.scopeToDocument(
                new AgenticScopeKey("a", "m"), DefaultAgenticScope.ephemeralAgenticScope());

        assertThat(document.getString("scope_json")).isNotBlank();
        assertThat(document.getDate("_updated_at")).isNotNull();
    }

    @Test
    void keyDocumentShouldStringifyMemoryIdOnBothDirections() {
        Document id = MongoAgenticScopeStore.keyDocument(new AgenticScopeKey("agent-1", 42));

        assertThat(id).containsEntry("agent", "agent-1").containsEntry("memory", "42");
        assertThat(MongoAgenticScopeStore.keyFromDocument(id)).isEqualTo(new AgenticScopeKey("agent-1", "42"));
    }

    @Test
    void builderShouldRejectMissingClientSource() {
        assertThatThrownBy(() -> MongoAgenticScopeStore.builder().build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("mongoClient");
    }

    @Test
    void builderShouldRejectClientAndConnectionStringTogether() {
        MongoClient client = MongoClients.create(LOCAL_URI);
        try {
            assertThatThrownBy(() -> MongoAgenticScopeStore.builder()
                    .mongoClient(client)
                    .connectionString(LOCAL_URI)
                    .build())
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("二选一");
        } finally {
            client.close();
        }
    }

    @Test
    void builderShouldRejectBlankNamesAndNonPositiveTtl() {
        assertThatThrownBy(() -> MongoAgenticScopeStore.builder()
                .connectionString(LOCAL_URI)
                .database(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("database");

        assertThatThrownBy(() -> MongoAgenticScopeStore.builder()
                .connectionString(LOCAL_URI)
                .collection(" ")
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("collection");

        assertThatThrownBy(() -> MongoAgenticScopeStore.builder()
                .connectionString(LOCAL_URI)
                .ttl(Duration.ZERO)
                .build())
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ttl");
    }

    @Test
    void builderShouldUseDefaultNamespaceAndHonorOverrides() {
        try (MongoAgenticScopeStore store = MongoAgenticScopeStore.builder()
                .connectionString(LOCAL_URI)
                .build()) {
            assertThat(store.collection().getNamespace().getDatabaseName())
                    .isEqualTo(MongoAgenticScopeStore.DEFAULT_DATABASE);
            assertThat(store.collection().getNamespace().getCollectionName())
                    .isEqualTo(MongoAgenticScopeStore.DEFAULT_COLLECTION);
        }

        try (MongoAgenticScopeStore store = MongoAgenticScopeStore.builder()
                .connectionString(LOCAL_URI)
                .database("db1")
                .collection("col1")
                .build()) {
            assertThat(store.collection().getNamespace().getDatabaseName()).isEqualTo("db1");
            assertThat(store.collection().getNamespace().getCollectionName()).isEqualTo("col1");
        }
    }

    @Test
    void closeShouldBeIdempotent() {
        MongoAgenticScopeStore store = MongoAgenticScopeStore.builder()
                .connectionString(LOCAL_URI)
                .build();

        store.close();
        store.close();
    }
}
