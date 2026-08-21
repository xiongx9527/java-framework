package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.SQLException;

public interface Neo4jQueryExecutorFactory {
    Neo4jQueryExecutor create(Neo4jConnectionConfig config) throws SQLException;
}
