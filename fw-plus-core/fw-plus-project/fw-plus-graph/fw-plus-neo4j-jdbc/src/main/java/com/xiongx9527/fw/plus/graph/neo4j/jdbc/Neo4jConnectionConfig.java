package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.SQLException;
import java.util.Properties;

public class Neo4jConnectionConfig {
    static final String URL_PREFIX = "jdbc:";

    private final String jdbcUrl;
    private final String driverUri;
    private final String database;
    private final String username;
    private final String password;

    private Neo4jConnectionConfig(String jdbcUrl, String driverUri, String database, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.driverUri = driverUri;
        this.database = database;
        this.username = username;
        this.password = password;
    }

    public static Neo4jConnectionConfig parse(String url, Properties properties) throws SQLException {
        if (!Neo4jDriver.accepts(url)) {
            throw new SQLException("Unsupported Neo4j JDBC URL: " + url);
        }
        Properties safeProperties = properties == null ? new Properties() : properties;
        String withoutPrefix = url.substring(URL_PREFIX.length());
        int queryIndex = withoutPrefix.indexOf('?');
        String uriAndDatabase = queryIndex >= 0 ? withoutPrefix.substring(0, queryIndex) : withoutPrefix;
        int pathIndex = uriAndDatabase.indexOf('/', uriAndDatabase.indexOf("://") + 3);
        String driverUri = pathIndex >= 0 ? uriAndDatabase.substring(0, pathIndex) : uriAndDatabase;
        String database = pathIndex >= 0 ? uriAndDatabase.substring(pathIndex + 1) : "";
        if (database.length() == 0) {
            database = safeProperties.getProperty("database", "neo4j");
        }
        String username = firstNonBlank(safeProperties.getProperty("user"), safeProperties.getProperty("username"));
        String password = safeProperties.getProperty("password", "");
        return new Neo4jConnectionConfig(url, driverUri, database, username, password);
    }

    private static String firstNonBlank(String first, String second) {
        if (first != null && first.trim().length() > 0) {
            return first;
        }
        return second == null ? "" : second;
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getDriverUri() {
        return driverUri;
    }

    public String getDatabase() {
        return database;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }
}
