package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.Ref;
import java.sql.SQLException;
import java.util.Map;

public class Neo4jRef implements Ref {
    private final String baseTypeName;
    private Object value;

    public Neo4jRef(String baseTypeName, Object value) throws SQLException {
        if (baseTypeName == null || baseTypeName.trim().isEmpty()) {
            throw new SQLException("Ref base type name must not be empty.");
        }
        this.baseTypeName = baseTypeName;
        this.value = value;
    }

    @Override
    public String getBaseTypeName() throws SQLException {
        return baseTypeName;
    }

    @Override
    public Object getObject(Map<String, Class<?>> map) throws SQLException {
        return value;
    }

    @Override
    public Object getObject() throws SQLException {
        return value;
    }

    @Override
    public void setObject(Object value) throws SQLException {
        this.value = value;
    }
}
