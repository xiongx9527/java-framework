package com.xx9527.fw.plus.boot.autoconfigure.nosql.redis;

import com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.config.DynamicRedisTemplateFactory;
import com.xx9527.fw.plus.boot.autoconfigure.nosql.redis.config.properties.RedisDataSourceProperties;
import lombok.Data;
import org.springframework.beans.BeansException;
import org.springframework.boot.autoconfigure.data.redis.JedisClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * redis 多数据源上下文
 * 该上下文的作用：
 * 1、通过数据源名称获取对应的配置信息
 * 2、通过上一步获取到的配置信息获取DynamicRedisTemplateFactory
 *
 * @author Mr_wenpan@163.com 2021/8/31 11:23 上午
 */
@Data
public abstract class RedisDataSourceContext implements ApplicationContextAware {

    /**
     * 数据源名称字段的常量定义
     */
    public static final String FIELD_DATASOURCE_NAME = "dataSourceName";

    /**
     * 应用上下文对象，用于获取Spring容器中的bean实例
     */
    protected ApplicationContext applicationContext;

    /**
     * 数据源名称，用于标识和区分不同的数据源
     */
    protected String dataSourceName;

    /**
     * 通过数据源名称获取该数据源对应的Redis配置
     */
    protected RedisProperties getRedisProperties() {
        RedisDataSourceProperties dataSourceProperties = applicationContext.getBean(RedisDataSourceProperties.class);
        // 通过数据源名称获取application配置文件中配置的该数据源的redis-properties
        return dataSourceProperties.getDatasource().get(dataSourceName);
    }

    /**
     * 通过数据源对应的Redis配置获取DynamicRedisTemplateFactory
     */
    protected DynamicRedisTemplateFactory<String, String> getDynamicRedisTemplateFactory() {
        // 获取数据源对应的Redis配置信息
        RedisProperties redisProperties = getRedisProperties();
        List<JedisClientConfigurationBuilderCustomizer> jedisBuilderCustomizers = getJedisBuilderCustomizers();
        List<LettuceClientConfigurationBuilderCustomizer> lettuceBuilderCustomizers = getLettuceBuilderCustomizers();
        RedisSentinelConfiguration sentinelConfiguration = getSentinelConfiguration();
        RedisClusterConfiguration redisClusterConfiguration = getRedisClusterConfiguration();

        // 根据配置信息构建一个RedisTemplateFactory
        return new DynamicRedisTemplateFactory<>(redisProperties, sentinelConfiguration,
                redisClusterConfiguration, jedisBuilderCustomizers, lettuceBuilderCustomizers);
    }

    /**
     * 获取Jedis客户端配置构建器自定义器列表
     *
     * @return JedisClientConfigurationBuilderCustomizer类型的自定义器列表
     */
    protected List<JedisClientConfigurationBuilderCustomizer> getJedisBuilderCustomizers() {
        return new ArrayList<>(applicationContext.getBeansOfType(JedisClientConfigurationBuilderCustomizer.class).values());
    }

    /**
     * 获取Lettuce客户端配置构建器自定义器列表
     *
     * @return LettuceClientConfigurationBuilderCustomizer类型的自定义器列表
     */
    protected List<LettuceClientConfigurationBuilderCustomizer> getLettuceBuilderCustomizers() {
        return new ArrayList<>(applicationContext.getBeansOfType(LettuceClientConfigurationBuilderCustomizer.class).values());
    }

    /**
     * 获取Redis哨兵配置
     *
     * @return RedisSentinelConfiguration类型的配置对象，如果不存在则返回null
     */
    protected RedisSentinelConfiguration getSentinelConfiguration() {
        Collection<RedisSentinelConfiguration> values = applicationContext.getBeansOfType(RedisSentinelConfiguration.class).values();
        // values == null 永远为false
        return values.size() <= 0 ? null : new ArrayList<>(values).get(0);
    }

    /**
     * 获取Redis集群配置
     *
     * @return RedisClusterConfiguration类型的配置对象，如果不存在则返回null
     */
    protected RedisClusterConfiguration getRedisClusterConfiguration() {
        Collection<RedisClusterConfiguration> values = applicationContext.getBeansOfType(RedisClusterConfiguration.class).values();
        return values.size() <= 0 ? null : new ArrayList<>(values).get(0);
    }

    /**
     * 设置应用上下文
     *
     * @param applicationContext 应用上下文对象
     * @throws BeansException Bean异常
     */
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

}
