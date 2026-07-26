package dev.langchain4j.store.embedding.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.data.CqlVector;
import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.CosineSimilarity;
import dev.langchain4j.store.embedding.EmbeddingMatch;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.RelevanceScore;
import lombok.Getter;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static dev.langchain4j.internal.ValidationUtils.ensureBetween;
import static dev.langchain4j.internal.ValidationUtils.ensureGreaterThanZero;
import static java.util.stream.Collectors.toList;

/**
 * Java 8 compatible Cassandra embedding store.
 */
public class CassandraEmbeddingStore implements EmbeddingStore<TextSegment> {

    private static final String ROW_ID = "row_id";
    private static final String VECTOR = "vector";
    private static final String BODY_BLOB = "body_blob";
    private static final String METADATA_S = "metadata_s";

    @Getter
    protected CqlSession cassandraSession;

    private final String tableName;
    private final int dimension;

    public CassandraEmbeddingStore(CqlSession session, String tableName, int dimension) {
        this.cassandraSession = session;
        this.tableName = tableName;
        this.dimension = dimension;
        create();
    }

    /**
     * Kept for source compatibility with the upstream builder shape. Astra-specific metric values are ignored in this Java 8 implementation.
     */
    public CassandraEmbeddingStore(CqlSession session, String tableName, int dimension, Object metric) {
        this(session, tableName, dimension);
    }

    public void create() {
        cassandraSession.execute("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + ROW_ID + " text PRIMARY KEY, "
                + VECTOR + " vector<float," + dimension + ">, "
                + BODY_BLOB + " text, "
                + METADATA_S + " map<text,text>)");
        cassandraSession.execute("CREATE CUSTOM INDEX IF NOT EXISTS ON " + tableName + "(" + VECTOR + ") "
                + "USING 'StorageAttachedIndex'");
    }

    public void delete() {
        cassandraSession.execute("DROP TABLE IF EXISTS " + tableName);
    }

    public void clear() {
        cassandraSession.execute("TRUNCATE TABLE " + tableName);
    }

    public static class Builder {
        public static Integer DEFAULT_PORT = 9042;
        private List<String> contactPoints;
        private String localDataCenter;
        private Integer port = DEFAULT_PORT;
        private String userName;
        private String password;
        protected String keyspace;
        protected String table;
        protected Integer dimension;
        protected Object metric;

        public Builder contactPoints(List<String> contactPoints) {
            this.contactPoints = contactPoints;
            return this;
        }

        public Builder localDataCenter(String localDataCenter) {
            this.localDataCenter = localDataCenter;
            return this;
        }

        public Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public Builder password(String password) {
            this.password = password;
            return this;
        }

        public Builder keyspace(String keyspace) {
            this.keyspace = keyspace;
            return this;
        }

        public Builder table(String table) {
            this.table = table;
            return this;
        }

        public Builder dimension(Integer dimension) {
            this.dimension = dimension;
            return this;
        }

        public Builder metric(Object metric) {
            this.metric = metric;
            return this;
        }

        public CassandraEmbeddingStore build() {
            CqlSessionBuilder builder = CqlSession.builder()
                    .withKeyspace(keyspace)
                    .withLocalDatacenter(localDataCenter);
            if (userName != null && password != null) {
                builder.withAuthCredentials(userName, password);
            }
            contactPoints.forEach(cp -> builder.addContactPoint(new InetSocketAddress(cp, port)));
            return new CassandraEmbeddingStore(builder.build(), table, dimension, metric);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static BuilderAstra builderAstra() {
        return new BuilderAstra();
    }

    public static class BuilderAstra {
        public BuilderAstra token(String token) { return this; }
        public BuilderAstra env(Object env) { return this; }
        public BuilderAstra databaseId(UUID dbId) { return this; }
        public BuilderAstra databaseRegion(String dbRegion) { return this; }
        public BuilderAstra keyspace(String keyspaceName) { return this; }
        public BuilderAstra table(String tableName) { return this; }
        public BuilderAstra dimension(int dimension) { return this; }
        public BuilderAstra metric(Object metric) { return this; }
        public CassandraEmbeddingStore build() {
            throw unsupportedAstraDbClient();
        }
    }

    @Override
    public String add(@NonNull Embedding embedding) {
        return add(embedding, null);
    }

    @Override
    public String add(@NonNull Embedding embedding, TextSegment textSegment) {
        String rowId = UUID.randomUUID().toString();
        addInternal(rowId, embedding, textSegment);
        return rowId;
    }

    @Override
    public void add(@NonNull String rowId, @NonNull Embedding embedding) {
        addInternal(rowId, embedding, null);
    }

    @Override
    public List<String> addAll(List<Embedding> embeddingList) {
        return embeddingList.stream()
                .map(embedding -> add(embedding, null))
                .collect(toList());
    }

    @Override
    public List<String> addAll(List<Embedding> embeddingList, List<TextSegment> textSegmentList) {
        if (embeddingList == null || textSegmentList == null || embeddingList.size() != textSegmentList.size()) {
            throw new IllegalArgumentException("embeddingList and textSegmentList must not be null and have the same size");
        }
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < embeddingList.size(); i++) {
            ids.add(add(embeddingList.get(i), textSegmentList.get(i)));
        }
        return ids;
    }

    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding embedding, int maxResults, double minScore) {
        ensureGreaterThanZero(maxResults, "maxResults");
        ensureBetween(minScore, 0, 1, "minScore");
        List<Row> rows = cassandraSession.execute("SELECT " + ROW_ID + ", " + VECTOR + ", " + BODY_BLOB + ", " + METADATA_S
                + " FROM " + tableName + " ORDER BY " + VECTOR + " ANN OF ? LIMIT ?",
                CqlVector.newInstance(embedding.vectorAsList()), maxResults).all();
        List<EmbeddingMatch<TextSegment>> matches = new ArrayList<>();
        for (Row row : rows) {
            EmbeddingMatch<TextSegment> match = mapSearchResult(embedding, row);
            if (match.score() >= minScore) {
                matches.add(match);
            }
        }
        return matches;
    }

    public List<EmbeddingMatch<TextSegment>> findRelevant(Embedding embedding, int maxResults, double minScore, Metadata metadata) {
        return findRelevant(embedding, maxResults, minScore);
    }

    private void addInternal(String rowId, Embedding embedding, TextSegment textSegment) {
        String body = null;
        Map<String, String> metadata = null;
        if (textSegment != null) {
            body = textSegment.text();
            metadata = textSegment.metadata().asMap();
        }
        cassandraSession.execute("INSERT INTO " + tableName + " (" + ROW_ID + ", " + VECTOR + ", " + BODY_BLOB + ", " + METADATA_S
                + ") VALUES (?, ?, ?, ?)", rowId, CqlVector.newInstance(embedding.vectorAsList()), body, metadata);
    }

    private static EmbeddingMatch<TextSegment> mapSearchResult(Embedding referenceEmbedding, Row row) {
        CqlVector<Float> vector = row.getVector(VECTOR, Float.class);
        List<Float> vectorList = new ArrayList<>();
        for (Float value : vector) {
            vectorList.add(value);
        }
        Embedding embedding = Embedding.from(vectorList);
        double score = RelevanceScore.fromCosineSimilarity(CosineSimilarity.between(referenceEmbedding, embedding));
        TextSegment embedded = null;
        String body = row.getString(BODY_BLOB);
        Map<String, String> metadata = row.getMap(METADATA_S, String.class, String.class);
        if (body != null && metadata != null) {
            embedded = TextSegment.from(body, new Metadata(metadata));
        }
        return new EmbeddingMatch<>(score, row.getString(ROW_ID), embedding, embedded);
    }

    private static UnsupportedOperationException unsupportedAstraDbClient() {
        return new UnsupportedOperationException("astra-db-client is compiled for Java 11 and is not provided by the Java 8 compatibility module");
    }
}
