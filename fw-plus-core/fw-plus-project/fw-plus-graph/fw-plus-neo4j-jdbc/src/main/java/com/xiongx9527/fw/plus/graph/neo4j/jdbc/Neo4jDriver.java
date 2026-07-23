package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.DriverPropertyInfo;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.Properties;
import java.util.logging.Logger;

public class Neo4jDriver implements Driver {
    private static final String[] SUPPORTED_PREFIXES = new String[] {
            "jdbc:neo4j://", "jdbc:neo4j+s://", "jdbc:neo4j+ssc://", "jdbc:bolt://", "jdbc:bolt+s://", "jdbc:bolt+ssc://"
    };
    private static volatile Neo4jQueryExecutorFactory executorFactory = new Neo4jJavaDriverQueryExecutorFactory();

    static {
        try {
            DriverManager.registerDriver(new Neo4jDriver());
        }
        catch (SQLException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    public static void setExecutorFactory(Neo4jQueryExecutorFactory factory) {
        executorFactory = factory == null ? new Neo4jJavaDriverQueryExecutorFactory() : factory;
    }

    static boolean accepts(String url) {
        if (url == null) {
            return false;
        }
        for (String prefix : SUPPORTED_PREFIXES) {
            if (url.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Connection connect(String url, Properties info) throws SQLException {
        if (!acceptsURL(url)) {
            return null;
        }
        Neo4jConnectionConfig config = Neo4jConnectionConfig.parse(url, info);
        return new Neo4jConnection(config, executorFactory.create(config));
    }

    @Override
    public boolean acceptsURL(String url) {
        return accepts(url);
    }

    @Override
    public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) {
        return new DriverPropertyInfo[0];
    }

    @Override
    public int getMajorVersion() {
        return 0;
    }

    @Override
    public int getMinorVersion() {
        return 1;
    }

    @Override
    public boolean jdbcCompliant() {
        return false;
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        throw new SQLFeatureNotSupportedException("java.util.logging is not used by fw-plus-neo4j-jdbc.");
    }
}
