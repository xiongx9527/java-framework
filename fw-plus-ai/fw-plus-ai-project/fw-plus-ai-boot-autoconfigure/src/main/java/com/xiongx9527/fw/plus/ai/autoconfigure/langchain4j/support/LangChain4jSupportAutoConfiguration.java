package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.logging.ChatContextLogger;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryStoreProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ChatMemoryTenantResolver;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.DisposableWorkerIdAssigner;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.ElasticsearchChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.JdbcChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.MongoChatMemoryStore;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.chatstore.RedisChatMemoryStore;
import com.xiongx9527.fw.plus.common.uid.impl.DefaultUidGenerator;

import org.elasticsearch.client.RestClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import dev.langchain4j.store.memory.chat.InMemoryChatMemoryStore;

/**
 * LangChain4j 支撑组件装配。
 *
 * <p>按 {@code fw.plus.ai.langchain4j.chat-memory.type} 条件注入一个
 * {@link ChatMemoryStore} Bean（含进程内实现）：{@code in-memory}（默认）/
 * {@code jdbc} / {@code redis} / {@code mongodb} / {@code elasticsearch}。
 * 各后端的客户端 Bean（JdbcTemplate / StringRedisTemplate / MongoTemplate /
 * ElasticsearchClient，缺省可用低层 {@link RestClient} 兜底构建 ES 客户端）由使用方环境提供；
 * 显式配置了对应 type 但客户端缺失时快速失败，避免静默降级。
 * 使用方自定义的 {@code ChatMemoryStore} Bean 优先（{@code @ConditionalOnMissingBean}）。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.Experimental")
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(ChatMemoryStoreProperties.class)
public class LangChain4jSupportAutoConfiguration {

    @Bean
    public ChatContextLogger chatContextLogger() {
        return new ChatContextLogger();
    }

    /** 默认租户解析器：返回配置的 default-tenant-id；使用方自定义 Bean 即自动覆盖 */
    @Bean
    @ConditionalOnMissingBean(ChatMemoryTenantResolver.class)
    public ChatMemoryTenantResolver chatMemoryTenantResolver(ChatMemoryStoreProperties properties) {
        return () -> properties.getDefaultTenantId();
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JdbcTemplate.class)
    static class JdbcChatMemoryStoreConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
                havingValue = ChatMemoryStoreProperties.TYPE_JDBC)
        @ConditionalOnMissingBean(ChatMemoryStore.class)
        public ChatMemoryStore jdbcChatMemoryStore(ObjectProvider<JdbcTemplate> jdbcTemplate,
                ChatMemoryTenantResolver tenantResolver, ChatMemoryStoreProperties properties) {
            JdbcTemplate template = jdbcTemplate.getIfAvailable();
            if (template == null) {
                throw new IllegalStateException(String.format(
                        "%s=%s，但 Spring 容器中不存在 JdbcTemplate Bean，请先配置数据源",
                        ChatMemoryStoreProperties.PREFIX + ".type", ChatMemoryStoreProperties.TYPE_JDBC));
            }
            // 主键 id 由随数据源的 WORKER_NODE 分配 workerId 的 DefaultUidGenerator 生成
            DisposableWorkerIdAssigner assigner = new DisposableWorkerIdAssigner(() -> template);
            DefaultUidGenerator idGenerator = new DefaultUidGenerator();
            idGenerator.setWorkerIdAssigner(assigner);
            try {
                idGenerator.afterPropertiesSet();
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                throw new IllegalStateException("初始化 DefaultUidGenerator 失败", e);
            }
            return new JdbcChatMemoryStore(() -> template, properties.getJdbc().getTableName(),
                    tenantResolver, properties.getDefaultBizType(), idGenerator::getUID);
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(StringRedisTemplate.class)
    static class RedisChatMemoryStoreConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
                havingValue = ChatMemoryStoreProperties.TYPE_REDIS)
        @ConditionalOnMissingBean(ChatMemoryStore.class)
        public ChatMemoryStore redisChatMemoryStore(ObjectProvider<StringRedisTemplate> redisTemplate,
                ChatMemoryTenantResolver tenantResolver, ChatMemoryStoreProperties properties) {
            StringRedisTemplate template = redisTemplate.getIfAvailable();
            if (template == null) {
                throw new IllegalStateException(String.format(
                        "%s=%s，但 Spring 容器中不存在 StringRedisTemplate Bean，请先配置 Redis 连接",
                        ChatMemoryStoreProperties.PREFIX + ".type", ChatMemoryStoreProperties.TYPE_REDIS));
            }
            return new RedisChatMemoryStore(() -> template, tenantResolver, properties.getDefaultBizType(),
                    properties.getRedis().getKeyPrefix(), properties.getRedis().getTtl());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(MongoTemplate.class)
    static class MongoChatMemoryStoreConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
                havingValue = ChatMemoryStoreProperties.TYPE_MONGODB)
        @ConditionalOnMissingBean(ChatMemoryStore.class)
        public ChatMemoryStore mongoChatMemoryStore(ObjectProvider<MongoTemplate> mongoTemplate,
                ChatMemoryTenantResolver tenantResolver, ChatMemoryStoreProperties properties) {
            MongoTemplate template = mongoTemplate.getIfAvailable();
            if (template == null) {
                throw new IllegalStateException(String.format(
                        "%s=%s，但 Spring 容器中不存在 MongoTemplate Bean，请先配置 MongoDB 连接",
                        ChatMemoryStoreProperties.PREFIX + ".type", ChatMemoryStoreProperties.TYPE_MONGODB));
            }
            return new MongoChatMemoryStore(() -> template, properties.getMongo().getCollectionName(),
                    tenantResolver, properties.getDefaultBizType());
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(ElasticsearchClient.class)
    static class ElasticsearchChatMemoryStoreConfiguration {

        @Bean
        @ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
                havingValue = ChatMemoryStoreProperties.TYPE_ELASTICSEARCH)
        @ConditionalOnMissingBean(ChatMemoryStore.class)
        public ChatMemoryStore elasticsearchChatMemoryStore(ObjectProvider<ElasticsearchClient> elasticsearchClient,
                ObjectProvider<RestClient> restClient, ChatMemoryTenantResolver tenantResolver,
                ChatMemoryStoreProperties properties) {
            ElasticsearchClient resolved = elasticsearchClient.getIfAvailable();
            if (resolved == null) {
                RestClient lowLevelClient = restClient.getIfAvailable();
                if (lowLevelClient == null) {
                    throw new IllegalStateException(String.format(
                            "%s=%s，但 Spring 容器中不存在 ElasticsearchClient 或 RestClient Bean，请先配置 Elasticsearch 客户端",
                            ChatMemoryStoreProperties.PREFIX + ".type",
                            ChatMemoryStoreProperties.TYPE_ELASTICSEARCH));
                }
                resolved = new ElasticsearchClient(new RestClientTransport(lowLevelClient, new JacksonJsonpMapper()));
            }
            final ElasticsearchClient client = resolved;
            return new ElasticsearchChatMemoryStore(() -> client, properties.getElasticsearch().getIndexName(),
                    tenantResolver, properties.getDefaultBizType());
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class InMemoryChatMemoryStoreConfiguration {

        /** 声明在最后：仅当 type=in-memory（或缺省）且容器中没有其他 ChatMemoryStore 时兜底 */
        @Bean
        @ConditionalOnProperty(prefix = ChatMemoryStoreProperties.PREFIX, name = "type",
                havingValue = ChatMemoryStoreProperties.TYPE_IN_MEMORY, matchIfMissing = true)
        @ConditionalOnMissingBean(ChatMemoryStore.class)
        public ChatMemoryStore inMemoryChatMemoryStore() {
            return new InMemoryChatMemoryStore();
        }
    }
}
