package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config;

import org.springframework.boot.data.redis.autoconfigure.JedisClientConfigurationBuilderCustomizer;
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties;
import org.springframework.boot.context.properties.PropertyMapper;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration.JedisClientConfigurationBuilder;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.util.StringUtils;
import redis.clients.jedis.JedisPoolConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Redis connection configuration using Jedis.
 * 当使用jedis客户端时，jedis客户端的配置信息
 *
 * @author Mark Paluch
 * @author Stephane Nicoll
 */
class JedisConnectionConfigure extends RedisConnectionConfiguration {

    /**
     * Jedis客户端配置构建器自定义器列表
     *
     * 该字段存储了一系列JedisClientConfigurationBuilderCustomizer接口的实现，
     * 用于对Jedis客户端配置构建器进行自定义配置。每个自定义器都可以修改构建器
     * 的配置参数，以满足特定的应用需求。
     *
     * 通过遍历此列表中的所有自定义器，可以依次应用各种配置修改，实现灵活的
     * 客户端配置定制功能。
     */
    private final List<JedisClientConfigurationBuilderCustomizer> builderCustomizers;

    /**
     * 构造函数，用于创建Jedis连接配置对象
     *
     * @param properties Redis属性配置对象，包含基本的Redis连接信息
     * @param sentinelConfiguration Redis哨兵配置对象，用于哨兵模式下的连接配置
     * @param clusterConfiguration Redis集群配置对象，用于集群模式下的连接配置
     * @param builderCustomizers Jedis客户端配置构建器自定义器列表，用于自定义Jedis客户端配置
     * @param database 数据库索引号，指定要连接的Redis数据库
     */
    JedisConnectionConfigure(DataRedisProperties properties,
                             RedisSentinelConfiguration sentinelConfiguration,
                             RedisClusterConfiguration clusterConfiguration,
                             List<JedisClientConfigurationBuilderCustomizer> builderCustomizers,
                             int database) {
        super(properties, sentinelConfiguration, clusterConfiguration, database);
        // 初始化builderCustomizers，如果为null则使用空列表
        this.builderCustomizers = Optional.ofNullable(builderCustomizers).orElse(new ArrayList<>());
    }


    /**
     * 创建jedis连接工厂
     *
     * @return JedisConnectionFactory jedis连接工厂实例
     */
    JedisConnectionFactory redisConnectionFactory() {
        // 调用具体的创建方法来获取Jedis连接工厂实例
        return createJedisConnectionFactory();
    }



    /**
     * 创建jedis连接工厂
     *
     * @return JedisConnectionFactory jedis连接工厂实例
     */
    private JedisConnectionFactory createJedisConnectionFactory() {
        JedisClientConfiguration clientConfiguration = getJedisClientConfiguration(builderCustomizers);
        JedisConnectionFactory jedisConnectionFactory;
        // 根据不同的配置类型创建对应的Jedis连接工厂
        if (getSentinelConfig() != null) {
            jedisConnectionFactory = new JedisConnectionFactory(getSentinelConfig(), clientConfiguration);
        } else if (getClusterConfiguration() != null) {
            jedisConnectionFactory = new JedisConnectionFactory(getClusterConfiguration(), clientConfiguration);
        } else {
            jedisConnectionFactory = new JedisConnectionFactory(getStandaloneConfig(), clientConfiguration);
        }
        // 由于我们手动创建jedisConnectionFactory连接工厂，所以afterPropertiesSet并不会像spring一样自动被吊起
        // 必须手动调用afterPropertiesSet()，初始化connectionProvider，不然创建连接会报错空指针
        jedisConnectionFactory.afterPropertiesSet();
        return jedisConnectionFactory;
    }


    /**
     * 获取jedis客户端配置
     *
     * @param builderCustomizers jedis客户端配置构建器自定义器列表，用于对配置构建器进行自定义配置
     * @return 返回构建完成的Jedis客户端配置对象
     */
    private JedisClientConfiguration getJedisClientConfiguration(
            List<JedisClientConfigurationBuilderCustomizer> builderCustomizers) {
        // 应用基础属性配置到构建器
        JedisClientConfigurationBuilder builder = applyProperties(JedisClientConfiguration.builder());

        // 获取并应用连接池配置
        DataRedisProperties.Pool pool = getProperties().getJedis().getPool();
        if (pool != null) {
            applyPooling(pool, builder);
        }

        // 如果配置了URL，则从URL中自定义配置
        if (StringUtils.hasText(getProperties().getUrl())) {
            customizeConfigurationFromUrl(builder);
        }

        // 应用额外的自定义配置
        customize(builder);

        // 构建并返回最终的客户端配置
        return builder.build();
    }


    /**
     * 应用配置属性到Jedis客户端配置构建器
     *
     * @param builder Jedis客户端配置构建器实例
     * @return 配置完成的Jedis客户端配置构建器
     */
    private JedisClientConfigurationBuilder applyProperties(JedisClientConfigurationBuilder builder) {
        // 创建属性映射器，只在值非空时应用映射（Spring Boot 4 起默认跳过 null 值）
        PropertyMapper map = PropertyMapper.get();

        // 根据配置属性设置SSL、读取超时、连接超时和客户端名称
        map.from(getProperties().getSsl().isEnabled()).whenTrue().toCall(builder::useSsl);
        map.from(getProperties().getTimeout()).to(builder::readTimeout);
        map.from(getProperties().getConnectTimeout()).to(builder::connectTimeout);
        map.from(getProperties().getClientName()).whenHasText().to(builder::clientName);
        return builder;
    }


    /**
     * 应用Redis连接池配置
     *
     * @param pool Redis连接池配置属性对象，包含最大连接数、最小空闲连接数等配置信息
     * @param builder Jedis客户端配置构建器，用于设置连接池相关的配置参数
     */
    private static void applyPooling(DataRedisProperties.Pool pool,
                                     JedisClientConfigurationBuilder builder) {
        // 配置Jedis连接池
        builder.usePooling().poolConfig(jedisPoolConfig(pool));
    }


    /**
     * jedis连接池配置
     */
    private static JedisPoolConfig jedisPoolConfig(DataRedisProperties.Pool pool) {
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(pool.getMaxActive());
        config.setMaxIdle(pool.getMaxIdle());
        config.setMinIdle(pool.getMinIdle());
        if (pool.getTimeBetweenEvictionRuns() != null) {
            config.setTimeBetweenEvictionRunsMillis(pool.getTimeBetweenEvictionRuns().toMillis());
        }
        if (pool.getMaxWait() != null) {
            config.setMaxWaitMillis(pool.getMaxWait().toMillis());
        }
        return config;
    }

    /**
     * 从redis url中自定义配置
     */
    private void customizeConfigurationFromUrl(JedisClientConfigurationBuilder builder) {
        ConnectionInfo connectionInfo = parseUrl(getProperties().getUrl());
        if (connectionInfo.isUseSsl()) {
            builder.useSsl();
        }
    }

    /**
     * 自定义Jedis客户端配置构建器
     *
     * @param builder Jedis客户端配置构建器实例，用于构建Jedis客户端配置
     */
    private void customize(JedisClientConfigurationBuilder builder) {
        // 遍历所有自定义器，依次对构建器进行自定义配置
        for (JedisClientConfigurationBuilderCustomizer customizer : builderCustomizers) {
            customizer.customize(builder);
        }
    }


}
