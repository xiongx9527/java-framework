package dev.langchain4j.store.embedding.redis.spring;

import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.UnifiedJedis;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.langchain4j.store.embedding.redis.spring.RedisEmbeddingStoreProperties.PREFIX;

@AutoConfiguration
@EnableConfigurationProperties(RedisEmbeddingStoreProperties.class)
@ConditionalOnProperty(prefix = PREFIX, name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisEmbeddingStoreAutoConfiguration {

    private static final String DEFAULT_HOST = "localhost";
    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_INDEX_NAME = "langchain4j-index";

    @Bean
    @ConditionalOnMissingBean
    public RedisEmbeddingStore redisEmbeddingStore(RedisEmbeddingStoreProperties properties,
                                                   @Nullable EmbeddingModel embeddingModel) {
        String host = Optional.ofNullable(properties.getHost()).orElse(DEFAULT_HOST);
        int port = Optional.ofNullable(properties.getPort()).orElse(DEFAULT_PORT);
        String indexName = Optional.ofNullable(properties.getIndexName()).orElse(DEFAULT_INDEX_NAME);
        Integer dimension = Optional.ofNullable(properties.getDimension()).orElseGet(() -> embeddingModel == null ? null : embeddingModel.dimension());
        List<String> metadataKeys = Optional.ofNullable(properties.getMetadataKeys()).orElse(new ArrayList<>());

        DefaultJedisClientConfig.Builder clientConfig = DefaultJedisClientConfig.builder();
        if (properties.getUser() != null) {
            clientConfig.user(properties.getUser());
        }
        if (properties.getPassword() != null) {
            clientConfig.password(properties.getPassword());
        }
        UnifiedJedis unifiedJedis = new UnifiedJedis(new HostAndPort(host, port), clientConfig.build());

        return RedisEmbeddingStore.builder()
                .unifiedJedis(unifiedJedis)
                .indexName(indexName)
                .dimension(dimension)
                .metadataKeys(metadataKeys)
                .build();
    }
}
