package com.xiongx9527.fw.plus.nosql.redis.util;

import org.junit.jupiter.api.Test;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnvironmentUtilTest {

    @Test
    void loadRedisDataSourceNameReturnsEmptySetForNullEnvironment() {
        Set<String> result = EnvironmentUtil.loadRedisDataSourceName(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void loadRedisDataSourceNameParsesValidDatasourceNames() {
        StandardEnvironment environment = environmentWith("redis", properties(
                "spring.redis.datasource.first.host", "127.0.0.1",
                "spring.redis.datasource.second.port", "6379"
        ));

        Set<String> result = EnvironmentUtil.loadRedisDataSourceName(environment);

        assertEquals(2, result.size());
        assertTrue(result.contains("first"));
        assertTrue(result.contains("second"));
    }

    @Test
    void loadRedisDataSourceNameIgnoresInvalidAndIncompletePropertyNames() {
        StandardEnvironment environment = environmentWith("redis", properties(
                "spring.redis.host", "127.0.0.1",
                "spring.redis.datasource.", "invalid",
                "spring.redis.datasource..host", "invalid",
                "spring.redis.datasource.valid.host", "127.0.0.1"
        ));

        Set<String> result = EnvironmentUtil.loadRedisDataSourceName(environment);

        assertEquals(1, result.size());
        assertTrue(result.contains("valid"));
    }

    @Test
    void loadRedisDataSourceNameDeduplicatesNamesAcrossPropertySources() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("redis-one", properties(
                "spring.redis.datasource.shared.host", "127.0.0.1"
        )));
        environment.getPropertySources().addFirst(new MapPropertySource("redis-two", properties(
                "spring.redis.datasource.shared.port", "6379"
        )));

        Set<String> result = EnvironmentUtil.loadRedisDataSourceName(environment);

        assertEquals(1, result.size());
        assertTrue(result.contains("shared"));
    }

    private static StandardEnvironment environmentWith(String name, Map<String, Object> properties) {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource(name, properties));
        return environment;
    }

    private static Map<String, Object> properties(String... keyValues) {
        Map<String, Object> properties = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            properties.put(keyValues[i], keyValues[i + 1]);
        }
        return properties;
    }
}
