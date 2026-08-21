package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.SQLException;
import java.sql.Struct;
import java.util.Arrays;
import java.util.Map;

public class Neo4jStruct implements Struct {
    private final String typeName;
    private final Object[] attributes;

    public Neo4jStruct(String typeName, Object[] attributes) throws SQLException {
        if (typeName == null || typeName.trim().isEmpty()) {
            throw new SQLException("Struct type name must not be empty.");
        }
        this.typeName = typeName;
        this.attributes = attributes == null ? new Object[0] : Arrays.copyOf(attributes, attributes.length);
    }

    @Override
    public String getSQLTypeName() throws SQLException {
        return typeName;
    }

    @Override
    public Object[] getAttributes() throws SQLException {
        return Arrays.copyOf(attributes, attributes.length);
    }

    @Override
    public Object[] getAttributes(Map<String, Class<?>> map) throws SQLException {
        return getAttributes();
    }
}
