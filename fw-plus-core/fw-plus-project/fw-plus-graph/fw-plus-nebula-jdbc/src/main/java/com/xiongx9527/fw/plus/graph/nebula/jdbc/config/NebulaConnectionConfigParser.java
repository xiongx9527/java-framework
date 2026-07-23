package com.xiongx9527.fw.plus.graph.nebula.jdbc.config;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.client.NebulaHostAddress;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.utils.NebulaJdbcUrlParser;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.utils.NebulaPropertyKey;

import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;
import java.util.List;
import java.util.Properties;

public final class NebulaConnectionConfigParser {
    private NebulaConnectionConfigParser() {
    }

    public static NebulaConnectionConfig parse(String url, Properties properties) throws SQLException {
        Properties input = properties == null ? new Properties() : properties;
        Properties parsed;
        List<NebulaHostAddress> addresses;
        try {
            parsed = NebulaJdbcUrlParser.parse(url, input);
            addresses = NebulaJdbcUrlParser.getAddresses(url);
        } catch (Exception exception) {
            throw new SQLException(exception);
        }

        Properties mergedProperties = flatten(input);
        overlay(mergedProperties, parsed);
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
        copyStringProperties(flattened, properties);
        copyDirectEntries(flattened, properties);
        return flattened;
    }

    private static void copyStringProperties(Properties target, Properties source) {
        if (source == null) {
            return;
        }
        Enumeration<?> propertyNames = source.propertyNames();
        while (propertyNames.hasMoreElements()) {
            Object key = propertyNames.nextElement();
            if (key instanceof String) {
                String value = source.getProperty((String) key);
                if (value != null) {
                    target.setProperty((String) key, value);
                }
            }
        }
    }

    private static void copyDirectEntries(Properties target, Properties source) {
        for (Map.Entry<Object, Object> entry : source.entrySet()) {
            target.put(entry.getKey(), entry.getValue());
        }
    }

    private static void overlay(Properties properties, Properties overrides) {
        for (Map.Entry<Object, Object> entry : overrides.entrySet()) {
            properties.put(entry.getKey(), entry.getValue());
        }
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
            throw new SQLException("Invalid numeric property " + keyName + ": '" + value + "'");
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
