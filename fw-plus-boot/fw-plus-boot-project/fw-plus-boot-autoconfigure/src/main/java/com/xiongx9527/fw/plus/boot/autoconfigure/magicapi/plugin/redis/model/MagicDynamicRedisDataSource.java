package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model;

import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import io.lettuce.core.ClientOptions;
import io.lettuce.core.SocketOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.pool2.impl.GenericObjectPoolConfig;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.data.redis.connection.*;
import org.springframework.data.redis.connection.jedis.JedisClientConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration;
import redis.clients.jedis.JedisPoolConfig;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class MagicDynamicRedisDataSource implements DisposableBean {

    /**
     * 存储所有已创建的Redis连接工厂实例
     * 使用ConcurrentHashMap保证线程安全，key为数据源的唯一标识，value为对应的Redis连接工厂实例
     */
    private static final Map<String, RedisConnectionFactory> redisConnectionFactoryMap = new ConcurrentHashMap<>();

    /**
     * Redis数据源信息映射表
     * 用于存储Redis数据源的配置信息，以数据源名称为键，RedisDataSourceInfo对象为值
     * 使用ConcurrentHashMap保证线程安全，支持多线程并发访问
     */
    private static final Map<String, RedisDataSourceInfo> redisDataSourceInfoMap = new ConcurrentHashMap<>();


    /**
     * 创建Redis连接工厂
     * 根据数据源信息中的客户端类型创建相应的Redis连接工厂实例
     *
     * @param dataSourceInfo Redis数据源信息，包含连接所需的各种配置参数
     * @return RedisConnectionFactory Redis连接工厂实例，如果创建失败则返回null
     */
    public RedisConnectionFactory createRedisConnectionFactory(RedisDataSourceInfo dataSourceInfo) {
        RedisConnectionFactory connectionFactory = null;
        // 检查数据源信息是否为空
        if (dataSourceInfo == null) {
            log.warn("数据源信息为空，无法构建Redis连接工厂");
            return connectionFactory;
        }
        String clientType = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "clientType", dataSourceInfo.getClientType());
        // 根据客户端类型创建相应的连接工厂
        if ("lettuce".equalsIgnoreCase(clientType)) {
            connectionFactory = createLettuceConnectionFactory(dataSourceInfo);
        } else if ("jedis".equalsIgnoreCase(clientType)) {
            connectionFactory = createJedisConnectionFactory(dataSourceInfo);
        } else {
            log.warn("不支持的Redis客户端类型: {}", clientType);
        }
        return connectionFactory;
    }


    /**
     * 构建Redis连接工厂
     * 根据数据源信息创建并初始化相应的Redis连接工厂，并将其存储在连接工厂映射中
     *
     * @param dataSourceInfo Redis数据源信息，包含连接所需的各种配置参数
     */
    public void buildRedisConnectionFactory(RedisDataSourceInfo dataSourceInfo) {
        RedisConnectionFactory connectionFactory = createRedisConnectionFactory(dataSourceInfo);
        String key = dataSourceInfo.getKey();
        // 初始化连接工厂并存储
        if (connectionFactory != null) {
            try {
                // 初始化连接工厂
                if (connectionFactory instanceof LettuceConnectionFactory) {
                    ((LettuceConnectionFactory) connectionFactory).afterPropertiesSet();
                } else if (connectionFactory instanceof JedisConnectionFactory) {
                    ((JedisConnectionFactory) connectionFactory).afterPropertiesSet();
                }
                RedisConnectionFactory oldConnectionFactory = redisConnectionFactoryMap.put(key, connectionFactory);
                if (oldConnectionFactory != null) {
                    destroyRedisConnectionFactory(oldConnectionFactory);
                }
                redisDataSourceInfoMap.put(key, dataSourceInfo);
            } catch (Exception e) {
                log.error("初始化Redis连接工厂失败: key={}", key, e);
                // 如果初始化失败，清理已创建的连接工厂
                try {
                    destroyRedisConnectionFactory(connectionFactory);
                } catch (Exception destroyException) {
                    log.warn("销毁Redis连接工厂失败: key={}", key, destroyException);
                }
            }
        }
    }

    /**
     * 创建Lettuce连接工厂
     *
     * @param dataSourceInfo 数据源信息
     * @return LettuceConnectionFactory
     */
    private LettuceConnectionFactory createLettuceConnectionFactory(RedisDataSourceInfo dataSourceInfo) {
        // 添加空值检查
        if (dataSourceInfo == null) {
            log.warn("数据源信息为空，无法创建Lettuce连接工厂");
            return null;
        }
        LettuceConnectionFactory connectionFactory = null;
        LettuceClientConfiguration clientConfiguration = createLettuceClientConfiguration(dataSourceInfo);
        String mode = dataSourceInfo.getMode();
        mode = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "mode", mode);
        if ("standalone".equalsIgnoreCase(mode)) {
            // 单机模式配置
            RedisStandaloneConfiguration standaloneConfig = createStandaloneConfig(dataSourceInfo);
            connectionFactory = new LettuceConnectionFactory(standaloneConfig, clientConfiguration);
        } else if ("cluster".equalsIgnoreCase(mode)) {
            // 集群模式配置
            RedisClusterConfiguration clusterConfig = createClusterConfig(dataSourceInfo);
            connectionFactory = new LettuceConnectionFactory(clusterConfig, clientConfiguration);
        } else if ("sentinel".equalsIgnoreCase(mode)) {
            // 哨兵模式配置
            RedisSentinelConfiguration sentinelConfig = createSentinelConfig(dataSourceInfo);
            connectionFactory = new LettuceConnectionFactory(sentinelConfig, clientConfiguration);
        } else {
            // 默认使用单机模式
            RedisStandaloneConfiguration standaloneConfig = createStandaloneConfig(dataSourceInfo);
            connectionFactory = new LettuceConnectionFactory(standaloneConfig, clientConfiguration);
        }
        
        // 从properties字段读取其他配置选项并应用到ConnectionFactory
        // {"timeout": 2000, "connectTimeout": 3000, "clientName": "myClient", "shutdownTimeout": 100}
        JSONObject jsonObject = resolveProperties(dataSourceInfo);
        if (!jsonObject.isEmpty()) {
                // 设置客户端名称
                String clientName = jsonObject.getString("clientName");
                if (clientName != null) {
                    connectionFactory.setClientName(clientName);
                }
                // 设置关闭超时时间
                Integer shutdownTimeout = jsonObject.getInteger("shutdownTimeout");
                if (shutdownTimeout != null) {
                    connectionFactory.setShutdownTimeout(shutdownTimeout);
                }
        }
        
        return connectionFactory;
    }

    /**
     * 创建Jedis连接工厂
     *
     * @param dataSourceInfo 数据源信息
     * @return JedisConnectionFactory
     */
    private JedisConnectionFactory createJedisConnectionFactory(RedisDataSourceInfo dataSourceInfo) {
        // 添加空值检查
        if (dataSourceInfo == null) {
            log.warn("数据源信息为空，无法创建Jedis连接工厂");
            return null;
        }
        
        JedisConnectionFactory connectionFactory = null;
        JedisClientConfiguration clientConfiguration = createJedisClientConfiguration(dataSourceInfo);
        String mode = dataSourceInfo.getMode();
        mode = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "mode", mode);
        if ("standalone".equalsIgnoreCase(mode)) {
            // 单机模式配置
            RedisStandaloneConfiguration standaloneConfig = createStandaloneConfig(dataSourceInfo);
            connectionFactory = new JedisConnectionFactory(standaloneConfig, clientConfiguration);
        } else if ("cluster".equalsIgnoreCase(mode)) {
            // 集群模式配置
            RedisClusterConfiguration clusterConfig = createClusterConfig(dataSourceInfo);
            connectionFactory = new JedisConnectionFactory(clusterConfig, clientConfiguration);
        } else if ("sentinel".equalsIgnoreCase(mode)) {
            // 哨兵模式配置
            RedisSentinelConfiguration sentinelConfig = createSentinelConfig(dataSourceInfo);
            connectionFactory = new JedisConnectionFactory(sentinelConfig, clientConfiguration);
        } else {
            // 默认使用单机模式
            RedisStandaloneConfiguration standaloneConfig = createStandaloneConfig(dataSourceInfo);
            connectionFactory = new JedisConnectionFactory(standaloneConfig, clientConfiguration);
        }
        // 从properties字段读取其他配置选项并应用到ConnectionFactory
        // {"timeout": 2000, "connectTimeout": 3000, "clientName": "myClient"}
        JSONObject jsonObject = resolveProperties(dataSourceInfo);
        if (!jsonObject.isEmpty()) {
                // 设置客户端名称
                String clientName = jsonObject.getString("clientName");
                if (clientName != null) {
                    connectionFactory.setClientName(clientName);
                }
        }
        
        return connectionFactory;
    }

    /**
     * 创建Lettuce客户端配置
     *
     * @param dataSourceInfo 数据源信息
     * @return LettuceClientConfiguration
     */
    private LettuceClientConfiguration createLettuceClientConfiguration(RedisDataSourceInfo dataSourceInfo) {
        // 添加空值检查
        if (dataSourceInfo == null) {
            log.warn("数据源信息为空，无法创建Lettuce客户端配置");
            // 返回默认的连接池配置
            return LettucePoolingClientConfiguration.builder()
                    .poolConfig(createDefaultLettucePoolConfig())
                    .build();
        }
        
        LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder builder = LettucePoolingClientConfiguration.builder();
        
        // 设置默认值
        Duration defaultTimeout = Duration.ofMillis(2000);
        Duration defaultConnectTimeout = Duration.ofMillis(2000);
        Duration defaultShutdownTimeout = Duration.ofMillis(100);
        
        JSONObject jsonObject = resolveProperties(dataSourceInfo);
        
        // 设置命令超时时间
        Integer timeout = jsonObject.getInteger("timeout");
        builder.commandTimeout(timeout != null ? Duration.ofMillis(timeout) : defaultTimeout);
        
        // 设置连接超时时间
        Integer connectTimeout = jsonObject.getInteger("connectTimeout");
        Duration connectTimeoutDuration = connectTimeout != null ? Duration.ofMillis(connectTimeout) : defaultConnectTimeout;
        builder.clientOptions(
            ClientOptions.builder()
                .socketOptions(SocketOptions.builder()
                    .connectTimeout(connectTimeoutDuration)
                    .build())
                .build()
        );
        
        // 设置客户端名称
        String clientName = jsonObject.getString("clientName");
        if (clientName != null && !clientName.isEmpty()) {
            builder.clientName(clientName);
        }
        
        // 设置关闭超时时间
        Integer shutdownTimeout = jsonObject.getInteger("shutdownTimeout");
        builder.shutdownTimeout(shutdownTimeout != null ? Duration.ofMillis(shutdownTimeout) : defaultShutdownTimeout);
        
        // 设置SSL
        Boolean ssl = jsonObject.getBoolean("ssl");
        if (ssl != null && ssl) {
            builder.useSsl();
        }
        
        // 处理连接池配置
        JSONObject poolConfig = jsonObject.getJSONObject("pool");
        GenericObjectPoolConfig<?> pool = createLettucePoolConfig(poolConfig);
        
        // 应用连接池配置
        builder.poolConfig(pool);
        return builder.build();
    }

    /**
     * 创建默认的Lettuce连接池配置
     *
     * @return GenericObjectPoolConfig
     */
    private GenericObjectPoolConfig<?> createDefaultLettucePoolConfig() {
        GenericObjectPoolConfig<?> pool = new GenericObjectPoolConfig<>();
        pool.setMaxTotal(8);
        pool.setMaxIdle(8);
        pool.setMinIdle(0);
        pool.setMaxWaitMillis(-1);
        pool.setTimeBetweenEvictionRunsMillis(1800000); // 30分钟
        return pool;
    }

    /**
     * 创建Lettuce连接池配置
     *
     * @param poolConfig JSON格式的连接池配置
     * @return GenericObjectPoolConfig
     */
    private GenericObjectPoolConfig<?> createLettucePoolConfig(JSONObject poolConfig) {
        GenericObjectPoolConfig<?> pool = new GenericObjectPoolConfig<>();
        
        // 设置默认值
        int defaultMaxActive = 8;
        int defaultMaxIdle = 8;
        int defaultMinIdle = 0;
        long defaultMaxWait = -1;
        long defaultTimeBetweenEvictionRuns = 1800000; // 30分钟
        
        if (poolConfig != null) {
            // 设置最大连接数
            Integer maxActive = poolConfig.getInteger("maxActive");
            pool.setMaxTotal(maxActive != null ? maxActive : defaultMaxActive);
            
            // 设置最大空闲连接数
            Integer maxIdle = poolConfig.getInteger("maxIdle");
            pool.setMaxIdle(maxIdle != null ? maxIdle : defaultMaxIdle);
            
            // 设置最小空闲连接数
            Integer minIdle = poolConfig.getInteger("minIdle");
            pool.setMinIdle(minIdle != null ? minIdle : defaultMinIdle);
            
            // 设置最大等待时间
            Integer maxWait = poolConfig.getInteger("maxWait");
            pool.setMaxWaitMillis(maxWait != null ? maxWait : defaultMaxWait);
            
            // 设置空闲对象驱逐线程运行间隔时间
            Long timeBetweenEvictionRuns = poolConfig.getLong("timeBetweenEvictionRuns");
            pool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRuns != null ? timeBetweenEvictionRuns : defaultTimeBetweenEvictionRuns);
        } else {
            // 使用默认连接池配置
            pool.setMaxTotal(defaultMaxActive);
            pool.setMaxIdle(defaultMaxIdle);
            pool.setMinIdle(defaultMinIdle);
            pool.setMaxWaitMillis(defaultMaxWait);
            pool.setTimeBetweenEvictionRunsMillis(defaultTimeBetweenEvictionRuns);
        }
        
        return pool;
    }

    /**
     * 创建Jedis客户端配置
     *
     * @param dataSourceInfo 数据源信息
     * @return JedisClientConfiguration
     */
    private JedisClientConfiguration createJedisClientConfiguration(RedisDataSourceInfo dataSourceInfo) {
        // 添加空值检查
        if (dataSourceInfo == null) {
            log.warn("数据源信息为空，无法创建Jedis客户端配置");
            JedisPoolConfig defaultPool = createDefaultJedisPoolConfig();
            return JedisClientConfiguration.builder().usePooling().poolConfig(defaultPool).build();
        }
        
        JedisClientConfiguration.JedisClientConfigurationBuilder builder = JedisClientConfiguration.builder();
        
        // 设置默认值
        Duration defaultTimeout = Duration.ofMillis(2000);
        Duration defaultConnectTimeout = Duration.ofMillis(2000);
        
        JSONObject jsonObject = resolveProperties(dataSourceInfo);
        
        // 设置读取超时时间
        Integer timeout = jsonObject.getInteger("timeout");
        builder.readTimeout(timeout != null ? Duration.ofMillis(timeout) : defaultTimeout);
        
        // 设置连接超时时间
        Integer connectTimeout = jsonObject.getInteger("connectTimeout");
        builder.connectTimeout(connectTimeout != null ? Duration.ofMillis(connectTimeout) : defaultConnectTimeout);
        
        // 设置客户端名称
        String clientName = jsonObject.getString("clientName");
        if (clientName != null && !clientName.isEmpty()) {
            builder.clientName(clientName);
        }
        
        // 设置SSL
        Boolean ssl = jsonObject.getBoolean("ssl");
        if (ssl != null && ssl) {
            builder.useSsl();
        }
        
        // 处理连接池配置
        JSONObject poolConfig = jsonObject.getJSONObject("pool");
        JedisPoolConfig pool = createJedisPoolConfig(poolConfig);
        
        // 使用连接池配置
        return builder.usePooling().poolConfig(pool).build();
    }

    /**
     * 创建默认的Jedis连接池配置
     *
     * @return JedisPoolConfig
     */
    private JedisPoolConfig createDefaultJedisPoolConfig() {
        JedisPoolConfig pool = new JedisPoolConfig();
        pool.setMaxTotal(8);
        pool.setMaxIdle(8);
        pool.setMinIdle(0);
        pool.setMaxWaitMillis(-1);
        pool.setTimeBetweenEvictionRunsMillis(1800000); // 30分钟
        return pool;
    }

    /**
     * 创建Jedis连接池配置
     *
     * @param poolConfig JSON格式的连接池配置
     * @return JedisPoolConfig
     */
    private JedisPoolConfig createJedisPoolConfig(JSONObject poolConfig) {
        JedisPoolConfig pool = new JedisPoolConfig();
        
        // 设置默认值
        int defaultMaxActive = 8;
        int defaultMaxIdle = 8;
        int defaultMinIdle = 0;
        long defaultMaxWait = -1;
        long defaultTimeBetweenEvictionRuns = 1800000; // 30分钟
        
        if (poolConfig != null) {
            // 设置最大连接数
            Integer maxActive = poolConfig.getInteger("maxActive");
            pool.setMaxTotal(maxActive != null ? maxActive : defaultMaxActive);
            
            // 设置最大空闲连接数
            Integer maxIdle = poolConfig.getInteger("maxIdle");
            pool.setMaxIdle(maxIdle != null ? maxIdle : defaultMaxIdle);
            
            // 设置最小空闲连接数
            Integer minIdle = poolConfig.getInteger("minIdle");
            pool.setMinIdle(minIdle != null ? minIdle : defaultMinIdle);
            
            // 设置最大等待时间
            Integer maxWait = poolConfig.getInteger("maxWait");
            pool.setMaxWaitMillis(maxWait != null ? maxWait : defaultMaxWait);
            
            // 设置空闲对象驱逐线程运行间隔时间
            Long timeBetweenEvictionRuns = poolConfig.getLong("timeBetweenEvictionRuns");
            pool.setTimeBetweenEvictionRunsMillis(timeBetweenEvictionRuns != null ? timeBetweenEvictionRuns : defaultTimeBetweenEvictionRuns);
        } else {
            // 使用默认连接池配置
            pool.setMaxTotal(defaultMaxActive);
            pool.setMaxIdle(defaultMaxIdle);
            pool.setMinIdle(defaultMinIdle);
            pool.setMaxWaitMillis(defaultMaxWait);
            pool.setTimeBetweenEvictionRunsMillis(defaultTimeBetweenEvictionRuns);
        }
        
        return pool;
    }

    /**
     * 创建Redis单机配置
     *
     * @param dataSourceInfo 数据源信息
     * @return RedisStandaloneConfiguration
     */
    private RedisStandaloneConfiguration createStandaloneConfig(RedisDataSourceInfo dataSourceInfo) {
        // 添加空值检查
        if (dataSourceInfo == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.datasourceInfoRequired"));
        }
        
        RedisStandaloneConfiguration standaloneConfig = new RedisStandaloneConfiguration();
        String hostname = dataSourceInfo.getHostname();
        hostname = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "hostname", hostname);
        String username = dataSourceInfo.getUsername();
        username = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "username", username);
        String password = dataSourceInfo.getPassword();
        password = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "password", password);
        standaloneConfig.setHostName(hostname);
        
        // 设置端口，默认为6379
        String portStr = dataSourceInfo.getPort();
        portStr = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "port", portStr);
        int port;
        try {
            port = Integer.parseInt(portStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.portInvalid", portStr), e);
        }
        standaloneConfig.setPort(port);
        // 设置用户名（如果存在）
        if (StringUtils.isNotBlank(username)) {
            standaloneConfig.setUsername(username);
        }
        // 设置密码（如果存在）
        if (StringUtils.isNotBlank(password)) {
            standaloneConfig.setPassword(RedisPassword.of(password));
        }
        // 设置数据库索引，默认为0
        String databaseStr = dataSourceInfo.getDatabase();
        databaseStr = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "database", databaseStr);
        if (StringUtils.isBlank(databaseStr)) {
            databaseStr = "0";
        }
        int database;
        try {
            database = Integer.parseInt(databaseStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.databaseInvalid", databaseStr), e);
        }
        standaloneConfig.setDatabase(database);
        return standaloneConfig;
    }

    /**
     * 创建Redis集群配置
     *
     * @param dataSourceInfo 数据源信息
     * @return RedisClusterConfiguration
     */
    private RedisClusterConfiguration createClusterConfig(RedisDataSourceInfo dataSourceInfo) {
        // 添加空值检查
        if (dataSourceInfo == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.datasourceInfoRequired"));
        }
        RedisClusterConfiguration clusterConfig = new RedisClusterConfiguration();
        // 注意：Redis集群模式下，通常不支持SELECT命令切换数据库，只能使用默认的0号数据库
        // 但我们可以尝试设置，具体是否生效取决于Redis服务器的配置
        Set<String> addedNodes = new HashSet<>();
        // 添加主节点（hostname:port）作为集群节点之一
        String hostname = dataSourceInfo.getHostname();
        hostname = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "hostname", hostname);
        String portStr = dataSourceInfo.getPort();
        portStr = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "port", portStr);
        if (StringUtils.isNotBlank(hostname) && StringUtils.isNotBlank(portStr)) {
            int port;
            try {
                port = Integer.parseInt(portStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.portInvalid", portStr), e);
            }
            String nodeKey = hostname + ":" + port;
            if (!addedNodes.contains(nodeKey)) {
                clusterConfig.clusterNode(hostname, port);
                addedNodes.add(nodeKey);
            }
        }
        // 从properties字段读取其他节点信息和其他配置
        // {"nodes": [{"host": "192.168.1.1", "port": 6379}, {"host": "192.168.1.2", "port": 6380}], 
        //  "maxRedirects": 3, "username": "clusterUser", "password": "clusterPass"}
        JSONObject jsonObject = resolveProperties(dataSourceInfo);
        if (!jsonObject.isEmpty()) {
            try {
                JSONArray nodes = jsonObject.getJSONArray("nodes");
                if (nodes != null) {
                    for (int i = 0; i < nodes.size(); i++) {
                        JSONObject node = nodes.getJSONObject(i);
                        String host = node.getString("host");
                        String port = node.getString("port");
                        if (host != null && !host.isEmpty() && port != null) {
                            int portInt;
                            try {
                                portInt = Integer.parseInt(port);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.clusterNodePortInvalid", port), e);
                            }
                            String nodeKey = host + ":" + portInt;
                            if (!addedNodes.contains(nodeKey)) {
                                clusterConfig.clusterNode(host, portInt);
                                addedNodes.add(nodeKey);
                            }
                        }
                    }
                }
                // 如果JSON中指定了maxRedirects，则使用指定的值
                Integer maxRedirects = jsonObject.getInteger("maxRedirects");
                if (maxRedirects != null && maxRedirects > 0) {
                    clusterConfig.setMaxRedirects(maxRedirects);
                }
            } catch (Exception e) {
                log.warn("应用 Redis 集群 properties 失败", e);
            }
        }
        String username = dataSourceInfo.getUsername();
        username = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "username", username);
        String password = dataSourceInfo.getPassword();
        password = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "password", password);
        // 设置用户名（如果存在）
        if (StringUtils.isNotBlank(username)) {
            clusterConfig.setUsername(username);
        }
        // 设置密码（如果存在）
        if (StringUtils.isNotBlank(password)) {
            clusterConfig.setPassword(RedisPassword.of(password));
        }
        return clusterConfig;
    }

    /**
     * 创建Redis哨兵配置
     *
     * @param dataSourceInfo 数据源信息
     * @return RedisSentinelConfiguration
     */
    private RedisSentinelConfiguration createSentinelConfig(RedisDataSourceInfo dataSourceInfo) {
        // 添加空值检查
        if (dataSourceInfo == null) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.datasourceInfoRequired"));
        }
        
        RedisSentinelConfiguration sentinelConfig = new RedisSentinelConfiguration();
        // 设置数据库索引，默认为0
        String databaseStr = dataSourceInfo.getDatabase();
        databaseStr = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "database", databaseStr);
        if (StringUtils.isBlank(databaseStr)) {
            databaseStr = "0";
        }
        int database;
        try {
            database = Integer.parseInt(databaseStr);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.databaseInvalid", databaseStr), e);
        }
        sentinelConfig.setDatabase(database);

        Set<String> addedSentinels = new HashSet<>();
        String defaultMasterName = "mymaster";
        
        // 从properties字段读取哨兵信息
        // {"masterName": "mymaster", "sentinels": [{"host": "192.168.1.1", "port": 26379}, {"host": "192.168.1.2", "port": 26380}],
        //  "sentinelPassword": "sentinelPass", "timeout": 2000}
        JSONObject jsonObject = resolveProperties(dataSourceInfo);
        if (!jsonObject.isEmpty()) {
            try {
                String masterName = jsonObject.getString("masterName");
                if (masterName != null && !masterName.isEmpty()) {
                    sentinelConfig.master(masterName);
                } else {
                    sentinelConfig.master(defaultMasterName);
                }
                String hostname = dataSourceInfo.getHostname();
                hostname = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "hostname", hostname);
                String portStr = dataSourceInfo.getPort();
                portStr = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "port", portStr);
                // 添加主节点（hostname:port）作为哨兵节点之一
                if (StringUtils.isNotBlank(hostname) && StringUtils.isNotBlank(portStr)) {
                    String sentinelKey = hostname + ":" + portStr;
                    if (!addedSentinels.contains(sentinelKey)) {
                        int port;
                        try {
                            port = Integer.parseInt(portStr);
                        } catch (NumberFormatException e) {
                            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.portInvalid", portStr), e);
                        }
                        sentinelConfig.sentinel(hostname, port);
                        addedSentinels.add(sentinelKey);
                    }
                }
                JSONArray sentinels = jsonObject.getJSONArray("sentinels");
                if (sentinels != null) {
                    for (int i = 0; i < sentinels.size(); i++) {
                        JSONObject sentinel = sentinels.getJSONObject(i);
                        String host = sentinel.getString("host");
                        String port = sentinel.getString("port");
                        if (host != null && !host.isEmpty() && port != null) {
                            int portInt;
                            try {
                                portInt = Integer.parseInt(port);
                            } catch (NumberFormatException e) {
                                throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.sentinelNodePortInvalid", port), e);
                            }
                            String sentinelKey = host + ":" + portInt;
                            if (!addedSentinels.contains(sentinelKey)) {
                                sentinelConfig.sentinel(host, portInt);
                                addedSentinels.add(sentinelKey);
                            }
                        }
                    }
                }

                // 如果JSON中指定了哨兵密码，则使用指定的密码
                String sentinelPassword = jsonObject.getString("sentinelPassword");
                if (sentinelPassword != null && !sentinelPassword.isEmpty()) {
                    sentinelConfig.setSentinelPassword(RedisPassword.of(sentinelPassword));
                }
            } catch (Exception e) {
                log.warn("应用 Redis 哨兵 properties 失败", e);
                // 出现异常时使用默认配置
                handleDefaultSentinelConfig(sentinelConfig, dataSourceInfo, defaultMasterName, addedSentinels);
            }
        } else {
            // 如果没有properties配置，则使用默认配置
            handleDefaultSentinelConfig(sentinelConfig, dataSourceInfo, defaultMasterName, addedSentinels);
        }

        String username = dataSourceInfo.getUsername();
        username = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "username", username);
        String password = dataSourceInfo.getPassword();
        password = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "password", password);

        // 设置用户名（如果存在）
        if (StringUtils.isNotBlank(username)) {
            sentinelConfig.setUsername(username);
        }

        // 设置密码（如果存在）
        if (StringUtils.isNotBlank(password)) {
            sentinelConfig.setPassword(RedisPassword.of(password));
        }

        return sentinelConfig;
    }

    private JSONObject resolveProperties(RedisDataSourceInfo dataSourceInfo) {
        String configuredProperties = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "properties", null);
        return new JSONObject(MagicAPIConfigUtils.mergeProperties(dataSourceInfo.getProperties(), configuredProperties));
    }
    
    /**
     * 处理默认哨兵配置
     *
     * @param sentinelConfig 哨兵配置对象
     * @param dataSourceInfo 数据源信息
     * @param defaultMasterName 默认主节点名称
     * @param addedSentinels 已添加的哨兵节点集合
     */
    private void handleDefaultSentinelConfig(RedisSentinelConfiguration sentinelConfig, 
                                           RedisDataSourceInfo dataSourceInfo, 
                                           String defaultMasterName, 
                                           Set<String> addedSentinels) {
        String hostname = dataSourceInfo.getHostname();
        hostname = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "hostname", hostname);
        String portStr = dataSourceInfo.getPort();
        portStr = MagicAPIConfigUtils.getRedisConfig(dataSourceInfo.getKey(), "port", portStr);
		// 如果没有properties配置，则使用hostname和port作为主节点
        if (StringUtils.isNotBlank(hostname) && StringUtils.isNotBlank(portStr)) {
            // 默认主节点名称为mymaster
            sentinelConfig.master(defaultMasterName);
            // 添加主节点作为哨兵节点
            String sentinelKey = hostname + ":" + portStr;
            if (!addedSentinels.contains(sentinelKey)) {
                int port;
                try {
                    port = Integer.parseInt(portStr);
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException(I18nMessageResolver.resolveKey("fw.boot.redis.portInvalid", portStr), e);
                }
                sentinelConfig.sentinel(hostname, port);
                addedSentinels.add(sentinelKey);
            }
        }
    }

    /**
     * 销毁Redis连接工厂及其相关连接
     * 通过调用DisposableBean.destroy()确保整个连接池被正确释放
     *
     * @param redisConnectionFactory 需要被销毁的Redis连接工厂实例
     */
    public void destroyRedisConnectionFactory(RedisConnectionFactory redisConnectionFactory) {
        if (redisConnectionFactory == null) {
            return;
        }
        try {
            if (redisConnectionFactory instanceof DisposableBean) {
                ((DisposableBean) redisConnectionFactory).destroy();
            }
        } catch (Exception e) {
            log.warn("销毁Redis连接工厂异常", e);
        }
    }

    /**
     * 根据指定的key销毁对应的Redis连接工厂及其相关连接
     * 该方法会安全地关闭各种类型的Redis连接（普通连接、哨兵连接、集群连接），并在最后从映射中移除该连接工厂
     *
     * @param key Redis连接工厂的唯一标识符
     */
    private void destroyByKey(String key) {
        // 添加空值检查
        if (key == null) {
            log.warn("尝试销毁Redis连接工厂时，key为空");
            return;
        }
        
        // 使用原子操作获取并移除连接工厂
        RedisConnectionFactory redisConnectionFactory = redisConnectionFactoryMap.remove(key);
        if (redisConnectionFactory != null) {
            try {
                destroyRedisConnectionFactory(redisConnectionFactory);
            } catch (Exception e) {
                log.warn("销毁Redis连接工厂时发生异常，key: {}", key, e);
            }
        }
        redisDataSourceInfoMap.remove(key);
    }

    /**
     * 根据指定的key获取Redis连接工厂
     * 该方法从内部维护的映射表中查找并返回与指定key关联的Redis连接工厂实例
     *
     * @param key Redis连接工厂的唯一标识符
     * @return 与指定key关联的RedisConnectionFactory实例，如果未找到则返回null
     */
    public RedisConnectionFactory getRedisConnectionFactory(String key) {
        return redisConnectionFactoryMap.get(key);
    }

    /**
     * 根据指定的键获取对应的Redis数据源信息。
     *
     * @param key 用于查找Redis数据源信息的键，不能为空。
     * @return 返回与指定键关联的Redis数据源信息对象；如果未找到对应键，则返回null。
     */
    public RedisDataSourceInfo getRedisDataSourceInfo(String key) {
        return redisDataSourceInfoMap.get(key);
    }

    /**
     * 获取所有数据源节点信息列表
     * 该方法会收集所有已注册的Redis数据源信息，并添加一个默认数据源到列表中
     *
     * @return Redis数据源信息列表，包含所有已注册的数据源和一个默认数据源
     */
    public List<RedisDataSourceInfo> datasourceNodes() {
        List<RedisDataSourceInfo> dataSourceInfoList = new ArrayList<>(redisDataSourceInfoMap.values());
        try {
            RedisConnectionFactory redisConnectionFactory = SpringUtils.getBean(RedisConnectionFactory.class);
            if (redisConnectionFactory != null) {
                RedisDataSourceInfo dataSourceInfo = new RedisDataSourceInfo();
                dataSourceInfo.setKey("default");
                dataSourceInfo.setName("默认数据源");
                dataSourceInfoList.add(dataSourceInfo);
            }
        } catch (Exception e) {
        }
        return dataSourceInfoList;
    }

    /**
     * 根据指定的key销毁Redis连接工厂
     * 该方法提供了对外的接口，用于关闭和清理指定key对应的Redis连接资源
     *
     * @param key Redis连接工厂的唯一标识符
     */
    public void destroyRedisConnectionFactoryByKey(String key) {
        log.info("正在关闭 Redis 引擎: key={}", key);
        destroyByKey(key);
        log.info("已关闭 Redis 引擎: key={}", key);
    }

    @Override
    public void destroy() throws Exception {
        log.info("正在关闭所有 Redis 引擎");
        new ArrayList<>(redisConnectionFactoryMap.keySet()).forEach(this::destroyByKey);
        log.info("已关闭所有 Redis 引擎");
    }
}
