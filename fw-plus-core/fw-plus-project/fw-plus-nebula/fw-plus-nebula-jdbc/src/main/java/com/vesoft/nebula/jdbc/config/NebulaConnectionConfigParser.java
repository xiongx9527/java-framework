package com.vesoft.nebula.jdbc.config;

import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.jdbc.utils.NebulaJdbcUrlParser;
import com.vesoft.nebula.jdbc.utils.NebulaPropertyKey;

import java.sql.SQLException;
import java.util.Map;
import java.util.List;
import java.util.Properties;

public final class NebulaConnectionConfigParser {
    private NebulaConnectionConfigParser() {
    }

    public static NebulaConnectionConfig parse(String url, Properties properties) throws SQLException {
        Properties input = properties == null ? new Properties() : properties;
        Properties parsed;
        List<HostAddress> addresses;
        try {
            parsed = NebulaJdbcUrlParser.parse(url, input);
            addresses = NebulaJdbcUrlParser.getAddresses(url);
        } catch (Exception exception) {
            throw new SQLException(exception);
        }

        Properties mergedProperties = flatten(parsed);
        return new NebulaConnectionConfig(
                stringValue(mergedProperties, NebulaPropertyKey.DBNAME.getKeyName()),
                stringValue(mergedProperties, NebulaPropertyKey.USER.getKeyName()),
                stringValue(mergedProperties, NebulaPropertyKey.PASSWORD.getKeyName()),
                addresses,
                parseInt(mergedProperties, NebulaPropertyKey.MINCONNSSIZE, 0),
                parseInt(mergedProperties, NebulaPropertyKey.MAXCONNSSIZE, 10),
                parseInt(mergedProperties, NebulaPropertyKey.TIMEOUT, 0),
                parseInt(mergedProperties, NebulaPropertyKey.IDLETIME, 0),
                parseInt(mergedProperties, NebulaPropertyKey.INTERVALIDLE, -1),
                parseInt(mergedProperties, NebulaPropertyKey.WAITTIME, 0),
                mergedProperties
        );
    }

    private static String stringValue(Properties properties, String key) {
        Object value = value(properties, key);
        return value == null ? null : String.valueOf(value);
    }

    private static Properties flatten(Properties properties) {
        Properties flattened = new Properties();
        for (String propertyName : properties.stringPropertyNames()) {
            flattened.setProperty(propertyName, properties.getProperty(propertyName));
        }
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            flattened.put(entry.getKey(), entry.getValue());
        }
        return flattened;
    }

    private static int parseInt(Properties properties, NebulaPropertyKey key, int defaultValue) throws SQLException {
        String keyName = key.getKeyName();
        Object value = value(properties, keyName);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new SQLException("Invalid numeric property " + keyName + ": blank value");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new SQLException("Invalid numeric property " + keyName + ": " + value, exception);
        }
    }

    private static Object value(Properties properties, String key) {
        Object value = properties.get(key);
        if (value != null) {
            return value;
        }
        return properties.getProperty(key);
    }
}
