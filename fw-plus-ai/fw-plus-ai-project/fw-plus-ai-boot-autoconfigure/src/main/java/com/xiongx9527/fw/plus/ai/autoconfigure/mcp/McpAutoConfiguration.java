package com.xiongx9527.fw.plus.ai.autoconfigure.mcp;

import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.common.ServerConfig;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisBroadcaster;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.McpRedisMessageConsumer;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerSseRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.cluster.RedisServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.config.McpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.McpSseController;
import com.xiongx9527.fw.plus.ai.autoconfigure.mcp.web.McpStreamableHttpController;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.spi.ServerStreamableHttpRegistry;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerSseRegistry;
import com.xiongx9527.fw.plus.ai.mcp.support.DefaultServerStreamableHttpRegistry;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.util.UUID;

/**
 * MCP Server 自动配置。
 * <p>
 * 通过 {@code fw-plus.ai.mcp.enabled} 控制整体开关；集群 Bean 依赖 Redis 自动配置，
 * 因此声明在其后加载。
 * </p>
 */
@Configuration
@EnableConfigurationProperties(McpProperties.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "enabled", havingValue = "true", matchIfMissing = true)
@AutoConfigureAfter(name = "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration")
public class McpAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public ServerConfig mcpServerConfig(McpProperties properties) {
        ServerConfig config = new ServerConfig();
        if (properties.getServer() != null) {
            config.setName(properties.getServer().getName());
            config.setVersion(properties.getServer().getVersion());
        }
        return config;
    }

    @Bean
    @ConditionalOnMissingBean
    public FeatureMgr mcpFeatureMgr() {
        return new FeatureMgr();
    }

    @Bean
    @ConditionalOnMissingBean
    public McpServer mcpServer(ServerConfig serverConfig, FeatureMgr featureMgr) {
        McpServer server = new McpServer();
        server.setServerConfig(serverConfig);
        server.setFeatureMgr(featureMgr);
        return server;
    }

    @Bean
    @ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "transport", havingValue = "http-sse", matchIfMissing = true)
    @ConditionalOnMissingBean(ServerSseRegistry.class)
    public DefaultServerSseRegistry defaultServerSseRegistry(McpServer server) {
        // 节点标识随机生成，与集群广播器保持同一语义；服务名只用于协议握手。
        return new DefaultServerSseRegistry(server, UUID.randomUUID().toString());
    }

    @Bean
    @ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "transport", havingValue = "streamable-http")
    @ConditionalOnMissingBean(ServerStreamableHttpRegistry.class)
    public DefaultServerStreamableHttpRegistry defaultServerStreamableHttpRegistry(McpServer server) {
        return new DefaultServerStreamableHttpRegistry(server, UUID.randomUUID().toString());
    }

    @Bean
    @ConditionalOnMissingBean
    public McpServerInitializer mcpServerInitializer(McpServer server, McpProperties properties,
                                                      ApplicationContext context, FeatureMgr featureMgr) {
        return new McpServerInitializer(server, properties, context, featureMgr);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "transport", havingValue = "http-sse", matchIfMissing = true)
    public McpSseController mcpSseController(ApplicationContext context) {
        return new McpSseController(context);
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "transport", havingValue = "streamable-http")
    public McpStreamableHttpController mcpStreamableHttpController(ApplicationContext context) {
        return new McpStreamableHttpController(context);
    }

    /**
     * Redis 集群装配。
     * <p>
     * 单独成类并挂类级 {@link ConditionalOnClass}：spring-data-redis 是 optional 依赖，
     * 缺失时本类不会被加载，避免 {@code @Bean} 方法签名里的 Redis 类型在内省阶段触发
     * {@link NoClassDefFoundError}。类级 {@link ConditionalOnBean} 对应原广播器方法上的条件，
     * 即 Redis 自动配置就绪后才装配集群 Bean。
     * </p>
     */
    @Configuration
    @ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
    @ConditionalOnBean(type = "org.springframework.data.redis.core.StringRedisTemplate")
    static class RedisClusterConfiguration {

        @Bean
        @ConditionalOnMissingBean
        public McpRedisBroadcaster mcpRedisBroadcaster(StringRedisTemplate redisTemplate) {
            return new McpRedisBroadcaster(redisTemplate);
        }

        @Bean
        @ConditionalOnMissingBean
        public McpRedisMessageConsumer mcpRedisMessageConsumer(ApplicationContext context,
                McpRedisBroadcaster broadcaster) {
            return new McpRedisMessageConsumer(context, broadcaster);
        }

        @Bean
        @ConditionalOnMissingBean
        public RedisMessageListenerContainer mcpRedisMessageListenerContainer(StringRedisTemplate redisTemplate,
                McpRedisMessageConsumer consumer) {
            RedisMessageListenerContainer container = new RedisMessageListenerContainer();
            container.setConnectionFactory(redisTemplate.getConnectionFactory());
            container.addMessageListener(consumer, new PatternTopic(McpRedisBroadcaster.CHANNEL));
            return container;
        }

        @Bean
        @ConditionalOnMissingBean(ServerSseRegistry.class)
        @ConditionalOnBean(McpRedisBroadcaster.class)
        @ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "transport", havingValue = "http-sse",
                matchIfMissing = true)
        public RedisServerSseRegistry redisServerSseRegistry(McpServer server, McpRedisBroadcaster broadcaster) {
            return new RedisServerSseRegistry(server, broadcaster);
        }

        @Bean
        @ConditionalOnMissingBean(ServerStreamableHttpRegistry.class)
        @ConditionalOnBean(McpRedisBroadcaster.class)
        @ConditionalOnProperty(prefix = "fw-plus.ai.mcp", name = "transport", havingValue = "streamable-http")
        public RedisServerStreamableHttpRegistry redisServerStreamableHttpRegistry(McpServer server,
                McpRedisBroadcaster broadcaster) {
            return new RedisServerStreamableHttpRegistry(server, broadcaster);
        }
    }
}
