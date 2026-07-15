package com.vesoft.nebula.jdbc.config;

import com.vesoft.nebula.jdbc.utils.NebulaJdbcUrlParser;
import com.vesoft.nebula.jdbc.utils.NebulaPropertyKey;

import java.sql.SQLException;
import java.util.Properties;

public final class NebulaConnectionConfigParser {
    private NebulaConnectionConfigParser() {
    }

    public static NebulaConnectionConfig parse(String url, Properties properties) throws SQLException {
        Properties input = properties == null ? new Properties() : properties;
        Properties parsed;
        try {
            parsed = NebulaJdbcUrlParser.parse(url, input);
        } catch (Exception exception) {
            throw new SQLException(exception);
        }

        return new NebulaConnectionConfig(
                stringValue(parsed, NebulaPropertyKey.DBADDRESS.getKeyName()),
                stringValue(parsed, NebulaPropertyKey.DBNAME.getKeyName()),
                stringValue(parsed, NebulaPropertyKey.USER.getKeyName()),
                stringValue(parsed, NebulaPropertyKey.PASSWORD.getKeyName()),
                intValue(parsed, NebulaPropertyKey.MINCONNSSIZE.getKeyName(), 0),
                intValue(parsed, NebulaPropertyKey.MAXCONNSSIZE.getKeyName(), 10),
                intValue(parsed, NebulaPropertyKey.TIMEOUT.getKeyName(), 0),
                intValue(parsed, NebulaPropertyKey.IDLETIME.getKeyName(), 0),
                intValue(parsed, NebulaPropertyKey.INTERVALIDLE.getKeyName(), -1),
                intValue(parsed, NebulaPropertyKey.WAITTIME.getKeyName(), 0)
        );
    }

    private static String stringValue(Properties properties, String key) {
        Object value = value(properties, key);
        return value == null ? null : String.valueOf(value);
    }

    private static int intValue(Properties properties, String key, int defaultValue) throws SQLException {
        Object value = value(properties, key);
        if (value == null) {
            return defaultValue;
        }
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            throw new SQLException("Invalid numeric property " + key + ": blank value");
        }
        try {
            return Integer.parseInt(text);
        } catch (NumberFormatException exception) {
            throw new SQLException("Invalid numeric property " + key + ": " + value, exception);
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
