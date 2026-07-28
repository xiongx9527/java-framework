package dev.langchain4j.store.memory.chat.cassandra;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import com.datastax.oss.driver.api.core.cql.Row;
import com.datastax.oss.driver.api.core.uuid.Uuids;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.ChatMessageDeserializer;
import dev.langchain4j.data.message.ChatMessageSerializer;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import lombok.NonNull;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Java 8 compatible Cassandra chat memory store.
 */
public class CassandraChatMemoryStore implements ChatMemoryStore {

    public static final String DEFAULT_TABLE_NAME = "message_store";

    private static final String PARTITION_ID = "partition_id";
    private static final String ROW_ID = "row_id";
    private static final String BODY_BLOB = "body_blob";

    private final CqlSession cassandraSession;
    private final String tableName;

    public CassandraChatMemoryStore(CqlSession session) {
        this(session, DEFAULT_TABLE_NAME);
    }

    public CassandraChatMemoryStore(CqlSession session, String tableName) {
        this.cassandraSession = session;
        this.tableName = tableName;
        create();
    }

    public void create() {
        cassandraSession.execute("CREATE TABLE IF NOT EXISTS " + tableName + " ("
                + PARTITION_ID + " text, "
                + ROW_ID + " timeuuid, "
                + BODY_BLOB + " text, "
                + "PRIMARY KEY (" + PARTITION_ID + ", " + ROW_ID + ")) "
                + "WITH CLUSTERING ORDER BY (" + ROW_ID + " DESC)");
    }

    public void delete() {
        cassandraSession.execute("DROP TABLE IF EXISTS " + tableName);
    }

    public void clear() {
        cassandraSession.execute("TRUNCATE TABLE " + tableName);
    }

    public CqlSession getCassandraSession() {
        return cassandraSession;
    }

    @Override
    public List<ChatMessage> getMessages(@NonNull Object memoryId) {
        List<Row> rows = cassandraSession.execute("SELECT " + BODY_BLOB + " FROM " + tableName
                + " WHERE " + PARTITION_ID + " = ?", getMemoryId(memoryId)).all();
        List<ChatMessage> latestFirstList = new ArrayList<>();
        for (Row row : rows) {
            latestFirstList.add(toChatMessage(row));
        }
        Collections.reverse(latestFirstList);
        return latestFirstList;
    }

    @Override
    public void updateMessages(@NonNull Object memoryId, @NonNull List<ChatMessage> messages) {
        deleteMessages(memoryId);
        for (ChatMessage message : messages) {
            cassandraSession.execute("INSERT INTO " + tableName + " (" + PARTITION_ID + ", " + ROW_ID + ", " + BODY_BLOB
                    + ") VALUES (?, ?, ?)", getMemoryId(memoryId), Uuids.timeBased(), ChatMessageSerializer.messageToJson(message));
        }
    }

    @Override
    public void deleteMessages(@NonNull Object memoryId) {
        cassandraSession.execute("DELETE FROM " + tableName + " WHERE " + PARTITION_ID + " = ?", getMemoryId(memoryId));
    }

    private ChatMessage toChatMessage(@NonNull Row row) {
        try {
            return ChatMessageDeserializer.messageFromJson(row.getString(BODY_BLOB));
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to parse message body", e);
        }
    }

    private String getMemoryId(Object memoryId) {
        if (!(memoryId instanceof String)) {
            throw new IllegalArgumentException("memoryId must be a String");
        }
        return (String) memoryId;
    }

    public static class Builder {
        public static Integer DEFAULT_PORT = 9042;
        private List<String> contactPoints;
        private String localDataCenter;
        private Integer port = DEFAULT_PORT;
        private String userName;
        private String password;
        protected String keyspace;
        protected String table = DEFAULT_TABLE_NAME;

        public CassandraChatMemoryStore.Builder contactPoints(List<String> contactPoints) {
            this.contactPoints = contactPoints;
            return this;
        }

        public CassandraChatMemoryStore.Builder localDataCenter(String localDataCenter) {
            this.localDataCenter = localDataCenter;
            return this;
        }

        public CassandraChatMemoryStore.Builder port(Integer port) {
            this.port = port;
            return this;
        }

        public CassandraChatMemoryStore.Builder userName(String userName) {
            this.userName = userName;
            return this;
        }

        public CassandraChatMemoryStore.Builder password(String password) {
            this.password = password;
            return this;
        }

        public CassandraChatMemoryStore.Builder keyspace(String keyspace) {
            this.keyspace = keyspace;
            return this;
        }

        public CassandraChatMemoryStore.Builder table(String table) {
            this.table = table;
            return this;
        }

        public CassandraChatMemoryStore build() {
            CqlSessionBuilder builder = CqlSession.builder()
                    .withKeyspace(keyspace)
                    .withLocalDatacenter(localDataCenter);
            if (userName != null && password != null) {
                builder.withAuthCredentials(userName, password);
            }
            contactPoints.forEach(cp -> builder.addContactPoint(new InetSocketAddress(cp, port)));
            return new CassandraChatMemoryStore(builder.build(), table);
        }
    }

    public static CassandraChatMemoryStore.Builder builder() {
        return new CassandraChatMemoryStore.Builder();
    }

    public static CassandraChatMemoryStore.BuilderAstra builderAstra() {
        return new CassandraChatMemoryStore.BuilderAstra();
    }

    public static class BuilderAstra {
        public BuilderAstra token(String token) { return this; }
        public CassandraChatMemoryStore.BuilderAstra databaseId(UUID dbId) { return this; }
        public CassandraChatMemoryStore.BuilderAstra env(Object env) { return this; }
        public CassandraChatMemoryStore.BuilderAstra databaseRegion(String dbRegion) { return this; }
        public CassandraChatMemoryStore.BuilderAstra keyspace(String keyspaceName) { return this; }
        public CassandraChatMemoryStore.BuilderAstra table(String tableName) { return this; }
        public CassandraChatMemoryStore build() {
            throw unsupportedAstraDbClient();
        }
    }

    private static UnsupportedOperationException unsupportedAstraDbClient() {
        return new UnsupportedOperationException("astra-db-client is compiled for Java 11 and is not provided by the Java 8 compatibility module");
    }
}
