package com.xiongx9527.fw.plus.boot.autoconfigure.nosql.redis.config;

import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import io.lettuce.core.TimeoutOptions;
import io.lettuce.core.cluster.ClusterClientOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions;
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions.Builder;
import io.lettuce.core.resource.ClientResources;
import io.lettuce.core.resource.DefaultClientResources;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.boot.autoconfigure.data.redis.LettuceClientConfigurationBuilderCustomizer;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Lettuce.Cluster.Refresh;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties.Pool;
import org.springframework.data.redis.connection.RedisClusterConfiguration;
import org.springframework.data.redis.connection.RedisSentinelConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration.LettuceClientConfigurationBuilder;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Redis connection configuration using Lettuce.
 * 自定义改造lettuce连接配置
 *
 * @author Mark Paluch
 * @author Andy Wilkinson
 */
class LettuceConnectionConfigure extends RedisConnectionConfiguration {

    /**
     * redis配置properties
     */
    private final RedisProperties properties;

    /**
     * Lettuce客户端定制
     */
    private final List<LettuceClientConfigurationBuilderCustomizer> builderCustomizers;

    /**
     * 客户端资源对象，用于管理客户端连接池、线程池等共享资源
     * 该字段被声明为final，确保在对象初始化后不可更改
     * 通常用于Redis客户端或其他需要共享资源管理的场景
     */
    private final ClientResources clientResources;


    /**
     * 构造函数，用于创建Lettuce连接配置实例
     *
     * @param properties Redis属性配置对象，包含基本的Redis连接信息
     * @param sentinelConfigurationProvider Redis哨兵配置提供者，用于哨兵模式下的配置
     * @param clusterConfigurationProvider Redis集群配置提供者，用于集群模式下的配置
     * @param builderCustomizers Lettuce客户端配置构建器自定义器列表，用于自定义客户端配置
     * @param database 数据库索引号，指定要连接的Redis数据库
     */
    LettuceConnectionConfigure(RedisProperties properties,
                               RedisSentinelConfiguration sentinelConfigurationProvider,
                               RedisClusterConfiguration clusterConfigurationProvider,
                               List<LettuceClientConfigurationBuilderCustomizer> builderCustomizers,
                               int database) {
        super(properties, sentinelConfigurationProvider, clusterConfigurationProvider, database);
        this.properties = properties;
        this.builderCustomizers = Optional.ofNullable(builderCustomizers).orElse(new ArrayList<>());
        // 每次new LettuceConnectionConfiguration 都新建一个clientResources，也可以使用容器中默认注入的
        // 参考 LettuceConnectionConfiguration
        clientResources = DefaultClientResources.create();
    }


    /**
     * 创建lettuce连接工厂
     */
    LettuceConnectionFactory redisConnectionFactory() {
        // 获取lettuce连接工厂配置信息
        LettuceClientConfiguration clientConfig = getLettuceClientConfiguration(clientResources, properties.getLettuce().getPool());
        // 创建lettuce连接工厂
        return createLettuceConnectionFactory(clientConfig);
    }

    /**
     * 创建lettuce连接工厂
     */
    private LettuceConnectionFactory createLettuceConnectionFactory(LettuceClientConfiguration clientConfiguration) {
        LettuceConnectionFactory lettuceConnectionFactory;
        if (getSentinelConfig() != null) {
            lettuceConnectionFactory = new LettuceConnectionFactory(getSentinelConfig(), clientConfiguration);
        } else if (getClusterConfiguration() != null) {
            lettuceConnectionFactory = new LettuceConnectionFactory(getClusterConfiguration(), clientConfiguration);
        } else {
            lettuceConnectionFactory = new LettuceConnectionFactory(getStandaloneConfig(), clientConfiguration);
        }
        // 由于我们手动创建lettuceConnectionFactory连接工厂，所以afterPropertiesSet并不会像spring一样自动被吊起
        // 必须手动调用afterPropertiesSet()，初始化connectionProvider，不然创建连接会报错空指针
        lettuceConnectionFactory.afterPropertiesSet();
        return lettuceConnectionFactory;
    }

    /**
     * 获取lettuce客户端配置
     */
    private LettuceClientConfiguration getLettuceClientConfiguration(ClientResources clientResources, Pool pool) {
        LettuceClientConfigurationBuilder builder = createBuilder(pool);
        applyProperties(builder);
        if (StringUtils.hasText(getProperties().getUrl())) {
            customizeConfigurationFromUrl(builder);
        }
        builder.clientOptions(createClientOptions());
        builder.clientResources(clientResources);
        customize(builder);
        return builder.build();
    }

    /**
     * 变更源码排序
     */
    private void customize(LettuceClientConfigurationBuilder builder) {
        for (LettuceClientConfigurationBuilderCustomizer customizer : builderCustomizers) {
            customizer.customize(builder);
        }
    }

    /**
     * 创建客户端配置构建器，用于构建客户端配置
     */
    private static LettuceClientConfigurationBuilder createBuilder(Pool pool) {
        if (pool == null) {
            return LettuceClientConfiguration.builder();
        }
        return PoolBuilderFactory.createBuilder(pool);
    }

    /**
     * 应用Redis属性配置到Lettuce客户端构建器
     *
     * @param builder Lettuce客户端配置构建器
     * @return 配置后的Lettuce客户端配置构建器
     */
    private LettuceClientConfigurationBuilder applyProperties(
            LettuceClientConfigurationBuilder builder) {
        // 配置SSL连接
        if (getProperties().isSsl()) {
            builder.useSsl();
        }
        // 配置命令超时时间
        if (getProperties().getTimeout() != null) {
            builder.commandTimeout(getProperties().getTimeout());
        }
        // 配置Lettuce相关属性
        if (getProperties().getLettuce() != null) {
            RedisProperties.Lettuce lettuce = getProperties().getLettuce();
            if (lettuce.getShutdownTimeout() != null && !lettuce.getShutdownTimeout().isZero()) {
                builder.shutdownTimeout(getProperties().getLettuce().getShutdownTimeout());
            }
        }
        // 配置客户端名称
        if (StringUtils.hasText(getProperties().getClientName())) {
            builder.clientName(getProperties().getClientName());
        }
        return builder;
    }


    /**
     * 创建客户端配置选项
     *
     * @return ClientOptions 客户端配置选项对象
     */
    private ClientOptions createClientOptions() {
        // 初始化客户端配置构建器
        ClientOptions.Builder builder = initializeClientOptionsBuilder();

        // 配置连接超时时间
        Duration connectTimeout = getProperties().getConnectTimeout();
        if (connectTimeout != null) {
            builder.socketOptions(SocketOptions.builder().connectTimeout(connectTimeout).build());
        }

        // 启用超时选项并构建客户端配置
        return builder.timeoutOptions(TimeoutOptions.enabled()).build();
    }

    /**
     * 初始化客户端选项构建器
     *
     * @return ClientOptions.Builder 客户端选项构建器实例
     */
    private ClientOptions.Builder initializeClientOptionsBuilder() {
        // 如果集群配置不为空，则初始化集群客户端选项
        if (getProperties().getCluster() != null) {
            ClusterClientOptions.Builder builder = ClusterClientOptions.builder();
            Refresh refreshProperties = getProperties().getLettuce().getCluster().getRefresh();

            // 构建集群拓扑刷新选项
            Builder refreshBuilder = ClusterTopologyRefreshOptions.builder()
                    .dynamicRefreshSources(refreshProperties.isDynamicRefreshSources());

            // 如果设置了刷新周期，则启用定期刷新
            if (refreshProperties.getPeriod() != null) {
                refreshBuilder.enablePeriodicRefresh(refreshProperties.getPeriod());
            }

            // 如果启用了自适应刷新，则启用所有自适应刷新触发器
            if (refreshProperties.isAdaptive()) {
                refreshBuilder.enableAllAdaptiveRefreshTriggers();
            }

            return builder.topologyRefreshOptions(refreshBuilder.build());
        }

        // 返回默认的客户端选项构建器
        return ClientOptions.builder();
    }


    /**
     * 根据URL配置自定义Lettuce客户端配置
     *
     * @param builder Lettuce客户端配置构建器，用于设置连接参数
     */
    private void customizeConfigurationFromUrl(LettuceClientConfigurationBuilder builder) {
        // 解析URL获取连接信息，并根据是否使用SSL来配置构建器
        ConnectionInfo connectionInfo = parseUrl(getProperties().getUrl());
        if (connectionInfo.isUseSsl()) {
            builder.useSsl();
        }
    }


    /**
     * Inner class to allow optional commons-pool2 dependency.
     */
    private static class PoolBuilderFactory {

        /**
         * 创建Lettuce客户端配置构建器
         *
         * @param properties 连接池配置属性对象，用于配置连接池参数
         * @return 返回配置了连接池的Lettuce客户端配置构建器实例
         */
        static LettuceClientConfigurationBuilder createBuilder(Pool properties) {
            // 使用Lettuce的连接池配置构建器，结合传入的连接池属性创建配置
            return LettucePoolingClientConfiguration.builder().poolConfig(getPoolConfig(properties));
        }

        /**
         * 根据给定的连接池属性配置创建GenericObjectPoolConfig对象
         *
         * @param properties 连接池配置属性，包含最大活跃连接数、最大空闲连接数、最小空闲连接数等参数
         * @return 配置好的GenericObjectPoolConfig对象，用于连接池的初始化
         */
        private static GenericObjectPoolConfig<?> getPoolConfig(Pool properties) {
            // 创建连接池配置对象并设置基本参数
            GenericObjectPoolConfig<?> config = new GenericObjectPoolConfig<>();
            config.setMaxTotal(properties.getMaxActive());
            config.setMaxIdle(properties.getMaxIdle());
            config.setMinIdle(properties.getMinIdle());

            // 设置连接池清理线程的执行间隔时间
            if (properties.getTimeBetweenEvictionRuns() != null) {
                config.setTimeBetweenEvictionRunsMillis(properties.getTimeBetweenEvictionRuns().toMillis());
            }

            // 设置获取连接的最大等待时间
            if (properties.getMaxWait() != null) {
                config.setMaxWaitMillis(properties.getMaxWait().toMillis());
            }

            return config;
        }

    }

}
