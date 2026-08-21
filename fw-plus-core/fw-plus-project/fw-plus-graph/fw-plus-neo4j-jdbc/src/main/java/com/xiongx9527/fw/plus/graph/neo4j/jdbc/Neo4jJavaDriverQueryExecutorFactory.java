package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Record;
import org.neo4j.driver.Result;
import org.neo4j.driver.Session;
import org.neo4j.driver.SessionConfig;
import org.neo4j.driver.Transaction;
import org.neo4j.driver.summary.ResultSummary;
import org.neo4j.driver.summary.SummaryCounters;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Neo4jJavaDriverQueryExecutorFactory implements Neo4jQueryExecutorFactory {
    @Override
    public Neo4jQueryExecutor create(Neo4jConnectionConfig config) {
        Driver driver = GraphDatabase.driver(config.getDriverUri(),
                AuthTokens.basic(config.getUsername(), config.getPassword()));
        return new DriverExecutor(driver, config.getDatabase());
    }

    private static class DriverExecutor implements Neo4jQueryExecutor {
        private final Driver driver;
        private final String database;
        private boolean autoCommit = true;
        private Session transactionSession;
        private Transaction transaction;

        private DriverExecutor(Driver driver, String database) {
            this.driver = driver;
            this.database = database;
        }

        @Override
        public synchronized Neo4jQueryResult execute(String cypher, Map<String, Object> parameters) throws SQLException {
            if (!autoCommit) {
                try {
                    return queryResult(transaction().run(cypher, parameters));
                }
                catch (RuntimeException exception) {
                    throw new SQLException("Neo4j query failed: " + exception.getMessage(), exception);
                }
            }
            SessionConfig sessionConfig = SessionConfig.builder().withDatabase(database).build();
            Session session = driver.session(sessionConfig);
            try {
                return queryResult(session.run(cypher, parameters));
            }
            catch (RuntimeException exception) {
                throw new SQLException("Neo4j query failed: " + exception.getMessage(), exception);
            }
            finally {
                session.close();
            }
        }

        @Override
        public synchronized void setAutoCommit(boolean autoCommit) throws SQLException {
            if (this.autoCommit == autoCommit) {
                return;
            }
            if (autoCommit) {
                commit();
            }
            this.autoCommit = autoCommit;
        }

        @Override
        public synchronized boolean getAutoCommit() {
            return autoCommit;
        }

        @Override
        public synchronized void commit() throws SQLException {
            if (transaction == null) {
                return;
            }
            try {
                transaction.commit();
            }
            catch (RuntimeException exception) {
                throw new SQLException("Neo4j commit failed: " + exception.getMessage(), exception);
            }
            finally {
                closeTransaction();
            }
        }

        @Override
        public synchronized void rollback() throws SQLException {
            if (transaction == null) {
                return;
            }
            try {
                transaction.rollback();
            }
            catch (RuntimeException exception) {
                throw new SQLException("Neo4j rollback failed: " + exception.getMessage(), exception);
            }
            finally {
                closeTransaction();
            }
        }

        @Override
        public synchronized void close() throws SQLException {
            if (transaction != null) {
                rollback();
            }
            driver.close();
        }

        private Transaction transaction() {
            if (transaction == null) {
                SessionConfig sessionConfig = SessionConfig.builder().withDatabase(database).build();
                transactionSession = driver.session(sessionConfig);
                transaction = transactionSession.beginTransaction();
            }
            return transaction;
        }

        private void closeTransaction() {
            if (transaction != null) {
                transaction.close();
                transaction = null;
            }
            if (transactionSession != null) {
                transactionSession.close();
                transactionSession = null;
            }
        }

        private Neo4jQueryResult queryResult(Result result) {
            List<String> columns = result.keys();
            List<Record> records = result.list();
            List<Map<String, Object>> rows = new ArrayList<Map<String, Object>>();
            for (Record record : records) {
                Map<String, Object> row = new LinkedHashMap<String, Object>();
                for (String column : record.keys()) {
                    row.put(column, record.get(column).asObject());
                }
                rows.add(row);
            }
            ResultSummary summary = result.consume();
            return new Neo4jQueryResult(columns, rows, updateCount(summary.counters(), columns));
        }

        private int updateCount(SummaryCounters counters, List<String> columns) {
            if (!columns.isEmpty()) {
                return -1;
            }
            int count = counters.nodesCreated()
                    + counters.nodesDeleted()
                    + counters.relationshipsCreated()
                    + counters.relationshipsDeleted()
                    + counters.propertiesSet()
                    + counters.labelsAdded()
                    + counters.labelsRemoved()
                    + counters.indexesAdded()
                    + counters.indexesRemoved()
                    + counters.constraintsAdded()
                    + counters.constraintsRemoved();
            return count;
        }
    }
}
