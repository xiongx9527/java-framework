package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.SQLException;
import java.util.Map;

public interface Neo4jQueryExecutor {
    Neo4jQueryResult execute(String cypher, Map<String, Object> parameters) throws SQLException;

    void setAutoCommit(boolean autoCommit) throws SQLException;

    boolean getAutoCommit() throws SQLException;

    void commit() throws SQLException;

    void rollback() throws SQLException;

    void close() throws SQLException;
}
