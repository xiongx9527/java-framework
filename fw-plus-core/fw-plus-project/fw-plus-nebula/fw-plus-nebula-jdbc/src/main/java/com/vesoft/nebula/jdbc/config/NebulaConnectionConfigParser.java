package com.vesoft.nebula.jdbc.config;

import com.vesoft.nebula.client.graph.data.HostAddress;
import com.vesoft.nebula.jdbc.utils.NebulaJdbcUrlParser;
import com.vesoft.nebula.jdbc.utils.NebulaPropertyKey;

import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public final class NebulaConnectionConfigParser {
    private static final Field PROPERTIES_DEFAULTS_FIELD;

    static {
        Field defaultsField = null;
        try {
            defaultsField = Properties.class.getDeclaredField("defaults");
            defaultsField.setAccessible(true);
        } catch (NoSuchFieldException | SecurityException exception) {
            defaultsField = null;
        }
        PROPERTIES_DEFAULTS_FIELD = defaultsField;
    }

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
        flattenInto(flattened, properties, Collections.newSetFromMap(new IdentityHashMap<Properties, Boolean>()));
        return flattened;
    }

    private static void flattenInto(Properties flattened, Properties properties, Set<Properties> visited) {
        if (properties == null || visited.contains(properties)) {
            return;
        }
        visited.add(properties);
        flattenInto(flattened, defaultsOf(properties), visited);
        for (Map.Entry<Object, Object> entry : properties.entrySet()) {
            flattened.put(entry.getKey(), entry.getValue());
        }
    }

    private static Properties defaultsOf(Properties properties) {
        if (PROPERTIES_DEFAULTS_FIELD == null) {
            return null;
        }
        try {
            Object defaults = PROPERTIES_DEFAULTS_FIELD.get(properties);
            return defaults instanceof Properties ? (Properties) defaults : null;
        } catch (IllegalAccessException exception) {
            return null;
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
