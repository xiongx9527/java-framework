package com.xiongx9527.fw.plus.nosql.redis.util;

import org.springframework.core.env.AbstractEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * 环境工具
 *
 * @author Mr_wenpan@163.com 2021/09/06 17:15
 */
public abstract class EnvironmentUtil {

    private static final String REDIS_DATASOURCE_PREFIX = "spring.redis.datasource.";

    /**
     * 从环境信息Environment中解析出数据源的名称
     *
     * @return 数据源名称
     */
    public static Set<String> loadRedisDataSourceName(AbstractEnvironment environment) {
        if (environment == null) {
            return Collections.emptySet();
        }

        MutablePropertySources propertySources = environment.getPropertySources();
        Set<String> configs = StreamSupport.stream(propertySources.spliterator(), false)
                .filter(ps -> ps instanceof EnumerablePropertySource)
                .map(ps -> ((EnumerablePropertySource) ps).getPropertyNames())
                .flatMap(Arrays::stream)
                .map(EnvironmentUtil::parseRedisDataSourceName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        if (configs.size() > 0) {
            return configs;
        }

        return Collections.emptySet();
    }

    private static String parseRedisDataSourceName(String propertyName) {
        if (!propertyName.startsWith(REDIS_DATASOURCE_PREFIX)) {
            return null;
        }

        String suffix = propertyName.substring(REDIS_DATASOURCE_PREFIX.length());
        int dotIndex = suffix.indexOf('.');
        if (dotIndex <= 0) {
            return null;
        }

        return suffix.substring(0, dotIndex);
    }

}
