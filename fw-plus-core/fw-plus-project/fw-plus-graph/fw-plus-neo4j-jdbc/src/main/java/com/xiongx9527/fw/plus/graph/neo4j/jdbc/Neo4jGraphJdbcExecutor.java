package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcExecutor;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcParameterBindings;
import com.xiongx9527.fw.plus.graph.jdbc.GraphJdbcResult;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

final class Neo4jGraphJdbcExecutor implements GraphJdbcExecutor {

    private final Neo4jQueryExecutor delegate;

    Neo4jGraphJdbcExecutor(Neo4jQueryExecutor delegate) {
        this.delegate = delegate;
    }

    @Override
    public GraphJdbcResult execute(String cypher, GraphJdbcParameterBindings bindings) throws SQLException {
        PreparedCypher preparedCypher = bind(cypher, bindings);
        Neo4jQueryResult result = delegate.execute(preparedCypher.cypher, preparedCypher.parameters);
        if (hasResultSet(result)) {
            return GraphJdbcResult.rows(result.getColumns(), result.getRows());
        }
        return GraphJdbcResult.updateCount(result.getUpdateCount());
    }

    private static boolean hasResultSet(Neo4jQueryResult result) {
        return !result.getColumns().isEmpty() || !result.getRows().isEmpty() || result.getUpdateCount() < 0;
    }

    private static PreparedCypher bind(String cypher, GraphJdbcParameterBindings bindings) throws SQLException {
        StringBuilder sql = new StringBuilder();
        Map<String, Object> parameters = new LinkedHashMap<String, Object>();
        for (Map.Entry<Integer, Object> entry : bindings.asMap().entrySet()) {
			parameters.put("p" + entry.getKey(), normalize(bindings.getBinding(entry.getKey().intValue())));
        }
        boolean inString = false;
        int parameterIndex = 1;
        for (int index = 0; index < cypher.length(); index++) {
            char current = cypher.charAt(index);
            if (current == '\'') {
                sql.append(current);
                if (inString && index + 1 < cypher.length() && cypher.charAt(index + 1) == '\'') {
                    sql.append(cypher.charAt(++index));
                }
                else {
                    inString = !inString;
                }
            }
            else if (!inString && current == '?') {
                String name = "p" + parameterIndex;
				if (!parameters.containsKey(name)) {
					parameters.put(name, normalize(bindings.getBinding(parameterIndex)));
				}
                sql.append('$').append(name);
                parameterIndex++;
            }
            else {
                sql.append(current);
            }
        }
        return new PreparedCypher(sql.toString(), parameters);
    }

    static String namedParameters(String sql) {
        StringBuilder named = new StringBuilder();
        boolean inString = false;
        int parameterIndex = 1;
        for (int index = 0; index < sql.length(); index++) {
            char current = sql.charAt(index);
            if (current == '\'') {
                named.append(current);
                if (inString && index + 1 < sql.length() && sql.charAt(index + 1) == '\'') {
                    named.append(sql.charAt(++index));
                }
                else {
                    inString = !inString;
                }
            }
            else if (!inString && current == '?') {
                named.append("$p").append(parameterIndex++);
            }
            else {
                named.append(current);
            }
        }
        return named.toString();
    }

    private static Object normalize(GraphJdbcParameterBindings.Binding binding) throws SQLException {
        Object value = binding == null ? null : binding.getValue();
        String setterName = binding == null ? null : binding.getSetterName();
        long length = binding == null || binding.getLength() == null ? -1L : binding.getLength().longValue();
		if (value == null) {
			return null;
		}
        if (value instanceof RowId) {
            return value.toString();
        }
        if (value instanceof Ref) {
            return ((Ref) value).getObject();
        }
        if (value instanceof Array) {
            return ((Array) value).getArray();
        }
        if (value instanceof SQLXML) {
            return ((SQLXML) value).getString();
        }
        if (value instanceof NClob || value instanceof Clob) {
            return clobValue((Clob) value);
        }
        if (value instanceof Blob) {
            return blobValue((Blob) value);
        }
		if ("setAsciiStream".equals(setterName) && value instanceof InputStream) {
			return new String(readBytes((InputStream) value, length), StandardCharsets.US_ASCII);
		}
		if ("setUnicodeStream".equals(setterName) && value instanceof InputStream) {
			return new String(readBytes((InputStream) value, length), StandardCharsets.UTF_8);
		}
        if (value instanceof InputStream) {
			return readBytes((InputStream) value, length);
        }
        if (value instanceof Reader) {
			return readString((Reader) value, length);
        }
        return value;
    }

    private static byte[] blobValue(Blob blob) throws SQLException {
        long length = blob.length();
        if (length > Integer.MAX_VALUE) {
            throw new SQLException("Blob parameter exceeds supported in-memory size.");
        }
        return blob.getBytes(1L, (int) length);
    }

    private static String clobValue(Clob clob) throws SQLException {
        long length = clob.length();
        if (length > Integer.MAX_VALUE) {
            throw new SQLException("Clob parameter exceeds supported in-memory size.");
        }
        return clob.getSubString(1L, (int) length);
    }

    private static byte[] readBytes(InputStream inputStream, long requestedLength) throws SQLException {
        try {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
			long remaining = requestedLength;
			while (remaining != 0L) {
				int max = buffer.length;
				if (remaining > 0L && remaining < max) {
					max = (int) remaining;
				}
				int read = inputStream.read(buffer, 0, max);
				if (read < 0) {
					break;
				}
                output.write(buffer, 0, read);
				if (remaining > 0L) {
					remaining -= read;
				}
            }
            return output.toByteArray();
        }
        catch (IOException exception) {
            throw new SQLException("Failed to read stream parameter.", exception);
        }
    }

    private static String readString(Reader reader, long requestedLength) throws SQLException {
        try {
            StringWriter writer = new StringWriter();
            char[] buffer = new char[4096];
			long remaining = requestedLength;
			while (remaining != 0L) {
				int max = buffer.length;
				if (remaining > 0L && remaining < max) {
					max = (int) remaining;
				}
				int read = reader.read(buffer, 0, max);
				if (read < 0) {
					break;
				}
                writer.write(buffer, 0, read);
				if (remaining > 0L) {
					remaining -= read;
				}
            }
            return writer.toString();
        }
        catch (IOException exception) {
            throw new SQLException("Failed to read reader parameter.", exception);
        }
    }

    private static final class PreparedCypher {

        private final String cypher;
        private final Map<String, Object> parameters;

        private PreparedCypher(String cypher, Map<String, Object> parameters) {
            this.cypher = cypher;
            this.parameters = parameters;
        }
    }
}
