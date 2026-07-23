package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

class FakeNeo4jQueryExecutorFactory implements Neo4jQueryExecutorFactory {
    private String lastCypher;
    private Map<String, Object> lastParameters = Collections.emptyMap();
    private boolean autoCommit = true;
    private int commitCount;
    private int rollbackCount;

    @Override
    public Neo4jQueryExecutor create(Neo4jConnectionConfig config) {
        return new Neo4jQueryExecutor() {
            @Override
            public Neo4jQueryResult execute(String cypher, Map<String, Object> parameters) throws SQLException {
                lastCypher = cypher;
                lastParameters = new LinkedHashMap<String, Object>(parameters);
                if (isUpdate(cypher)) {
                    return new Neo4jQueryResult(Collections.<String>emptyList(),
                            Collections.<Map<String, Object>>emptyList(), 1);
                }
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                row.put("name", "Alice");
                row.put("marker", parameters.get("p2"));
                return new Neo4jQueryResult(Arrays.asList("name", "marker"), Collections.singletonList(row), -1);
            }

            @Override
            public void close() {
            }

            public void setAutoCommit(boolean autoCommit) throws SQLException {
                FakeNeo4jQueryExecutorFactory.this.autoCommit = autoCommit;
            }

            public boolean getAutoCommit() {
                return autoCommit;
            }

            public void commit() throws SQLException {
                commitCount++;
            }

            public void rollback() throws SQLException {
                rollbackCount++;
            }
        };
    }

    String getLastCypher() {
        return lastCypher;
    }

    Map<String, Object> getLastParameters() {
        return lastParameters;
    }

    boolean isAutoCommit() {
        return autoCommit;
    }

    int getCommitCount() {
        return commitCount;
    }

    int getRollbackCount() {
        return rollbackCount;
    }

    private static boolean isUpdate(String cypher) {
        String normalized = cypher == null ? "" : cypher.trim().toUpperCase(java.util.Locale.ROOT);
        return normalized.startsWith("CREATE ")
                || normalized.startsWith("MERGE ")
                || normalized.startsWith("DELETE ")
                || normalized.contains(" DELETE ")
                || normalized.contains(" SET ");
    }
}
