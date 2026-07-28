package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding;

import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.DefaultEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.DefaultEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProperties;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.store.embeding.EmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.provider.*;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreDefinition;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreException;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreRegistry;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreRegistry;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties({EmbeddingStoreProperties.class, AiEmbeddingStoreProperties.class})
public class EmbeddingStoreAutoConfiguration {

    private static final String PREFIX = "fw-plus.ai.langchain4j.embedding-store";
    private static final Logger LOGGER = LoggerFactory.getLogger(EmbeddingStoreAutoConfiguration.class);

    @Bean
    @ConditionalOnMissingBean(EmbeddingStoreFactory.class)
    public DefaultEmbeddingStoreFactory embeddingStoreFactory(List<EmbeddingStoreProviderAdapter> adapters) {
        return new DefaultEmbeddingStoreFactory(adapters);
    }

    @Bean
    @ConditionalOnMissingBean(AiEmbeddingStoreFactory.class)
    public AiEmbeddingStoreFactory aiEmbeddingStoreFactory() {
        return new DefaultAiEmbeddingStoreFactory();
    }

    @Bean
    @ConditionalOnMissingBean(name = "legacyEmbeddingStoreAiAdapter")
    public AiEmbeddingStoreAdapter legacyEmbeddingStoreAiAdapter(EmbeddingStoreFactory factory,
                                                                  List<EmbeddingStoreProviderAdapter> adapters) {
        return new LegacyEmbeddingStoreAiAdapter(factory, adapters);
    }

    @Bean
    @ConditionalOnMissingBean(name = "redisStackEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter redisStackEmbeddingStoreProviderAdapter() {
        return new RedisStackEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "milvusEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter milvusEmbeddingStoreProviderAdapter() {
        return new MilvusEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "elasticsearchEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter elasticsearchEmbeddingStoreProviderAdapter() {
        return new ElasticsearchEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "qdrantEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter qdrantEmbeddingStoreProviderAdapter() {
        return new QdrantEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "weaviateEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter weaviateEmbeddingStoreProviderAdapter() {
        return new WeaviateEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "astraDbEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter astraDbEmbeddingStoreProviderAdapter() {
        return new AstraDbEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "azureAiSearchEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter azureAiSearchEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        return new AzureAiSearchEmbeddingStoreProviderAdapter(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(name = "azureCosmosMongoVCoreEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter azureCosmosMongoVCoreEmbeddingStoreProviderAdapter() {
        return new AzureCosmosMongoVCoreEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "azureCosmosNoSqlEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter azureCosmosNoSqlEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        return new AzureCosmosNoSqlEmbeddingStoreProviderAdapter(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(name = "cassandraEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter cassandraEmbeddingStoreProviderAdapter() {
        return new CassandraEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "chromaEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter chromaEmbeddingStoreProviderAdapter() {
        return new ChromaEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "couchbaseEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter couchbaseEmbeddingStoreProviderAdapter() {
        return new CouchbaseEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "inMemoryEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter inMemoryEmbeddingStoreProviderAdapter() {
        return new InMemoryEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "infinispanEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter infinispanEmbeddingStoreProviderAdapter() {
        return new InfinispanEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "mongoDbEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter mongoDbEmbeddingStoreProviderAdapter() {
        return new MongoDbEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "neo4jEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter neo4jEmbeddingStoreProviderAdapter() {
        return new Neo4jEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "openSearchEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter openSearchEmbeddingStoreProviderAdapter() {
        return new OpenSearchEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "oracleEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter oracleEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        return new OracleEmbeddingStoreProviderAdapter(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(name = "pgVectorEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter pgVectorEmbeddingStoreProviderAdapter() {
        return new PgVectorEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "pineconeEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter pineconeEmbeddingStoreProviderAdapter() {
        return new PineconeEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "tablestoreEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter tablestoreEmbeddingStoreProviderAdapter(ApplicationContext applicationContext) {
        return new TablestoreEmbeddingStoreProviderAdapter(applicationContext);
    }

    @Bean
    @ConditionalOnMissingBean(name = "vearchEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter vearchEmbeddingStoreProviderAdapter() {
        return new VearchEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "vespaEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter vespaEmbeddingStoreProviderAdapter() {
        return new VespaEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(name = "langChain4jEmbeddingStoreProviderAdapter")
    public EmbeddingStoreProviderAdapter langChain4jEmbeddingStoreProviderAdapter() {
        return new LangChain4jEmbeddingStoreProviderAdapter();
    }

    @Bean
    @ConditionalOnMissingBean(EmbeddingStoreRegistry.class)
    @ConditionalOnProperty(prefix = "fw-plus.ai.langchain4j.embedding-store", name = "enabled", havingValue = "true", matchIfMissing = true)
    public EmbeddingStoreRegistry embeddingStoreRegistry(EmbeddingStoreProperties properties,
                                                         EmbeddingStoreFactory factory,
                                                         List<EmbeddingStoreProviderAdapter> adapters) {
        Map<String, EmbeddingStoreDefinition> definitions = definitions(properties);
        Map<String, EmbeddingStore<TextSegment>> stores = new LinkedHashMap<String, EmbeddingStore<TextSegment>>();
        for (EmbeddingStoreDefinition definition : definitions.values()) {
            stores.put(definition.getKey(), factory.create(definition, adapters));
        }
        validateDefaultStore(properties.getDefaultStore(), stores);
        return new DefaultEmbeddingStoreRegistry(stores, properties.getDefaultStore());
    }

    @Bean
    @ConditionalOnMissingBean(AiEmbeddingStoreRegistry.class)
    public AiEmbeddingStoreRegistry aiEmbeddingStoreRegistry(AiEmbeddingStoreProperties neutralProperties,
                                                              EmbeddingStoreProperties legacyProperties,
                                                              AiEmbeddingStoreFactory factory,
                                                              List<AiEmbeddingStoreAdapter> adapters,
                                                              EmbeddingStoreFactory legacyFactory,
                                                              List<EmbeddingStoreProviderAdapter> providerAdapters) {
        Map<String, AiEmbeddingStoreDefinition> definitions = new LinkedHashMap<String, AiEmbeddingStoreDefinition>();
        Map<String, AiEmbeddingStoreDefinition> configuredDefinitions =
                new LinkedHashMap<String, AiEmbeddingStoreDefinition>();
        if (neutralProperties.isEnabled()) {
            for (Map.Entry<String, AiEmbeddingStoreProperties.Store> entry : neutralProperties.getStores().entrySet()) {
                AiEmbeddingStoreDefinition definition = entry.getValue();
                if (definition == null) {
                    continue;
                }
                if (isBlank(definition.getKey())) {
                    definition.setKey(entry.getKey());
                }
                AiEmbeddingStoreDefinition existing = configuredDefinitions.get(definition.getKey());
                if (existing != null && existing.isEnabled() && definition.isEnabled()) {
                    throw new AiEmbeddingStoreException("Duplicate enabled store key: key=" + definition.getKey());
                }
                if (existing == null || (!existing.isEnabled() && definition.isEnabled())) {
                    configuredDefinitions.put(definition.getKey(), definition);
                }
                if (definition.isEnabled()) {
                    definitions.put(definition.getKey(), definition);
                }
            }
        }
        if (legacyProperties.isEnabled()) {
            for (EmbeddingStoreDefinition legacy : definitions(legacyProperties).values()) {
                AiEmbeddingStoreDefinition mapped = neutral(legacy);
                AiEmbeddingStoreDefinition configured = configuredDefinitions.get(mapped.getKey());
                if (configured != null) {
                    LOGGER.warn("忽略同 key 的旧 LangChain4j 向量存储配置，使用中立配置: key={}, framework={}, provider={}",
                            configured.getKey(), configured.getFramework(), configured.getProvider());
                    continue;
                }
                definitions.put(mapped.getKey(), mapped);
            }
        }
        List<AiEmbeddingStoreAdapter> effectiveAdapters = new ArrayList<AiEmbeddingStoreAdapter>(adapters);
        boolean hasLegacyFactoryAdapter = false;
        for (AiEmbeddingStoreAdapter adapter : effectiveAdapters) {
            if (adapter == legacyFactory || adapter instanceof LegacyEmbeddingStoreAiAdapter) {
                hasLegacyFactoryAdapter = true;
                break;
            }
        }
        if (!hasLegacyFactoryAdapter) {
            effectiveAdapters.add(new LegacyEmbeddingStoreAiAdapter(legacyFactory, providerAdapters));
        }
        String defaultKey = neutralProperties.isEnabled() && !isBlank(neutralProperties.getDefaultStore())
                ? neutralProperties.getDefaultStore()
                : legacyProperties.isEnabled() ? legacyProperties.getDefaultStore() : null;
        Map<String, com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore> stores =
                new LinkedHashMap<String, com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore>();
        for (AiEmbeddingStoreDefinition definition : definitions.values()) {
            stores.put(definition.getKey(), factory.create(definition, effectiveAdapters));
        }
        return new DefaultAiEmbeddingStoreRegistry(stores, defaultKey);
    }

    private AiEmbeddingStoreDefinition neutral(EmbeddingStoreDefinition legacy) {
        AiEmbeddingStoreDefinition neutral = new AiEmbeddingStoreDefinition();
        neutral.setKey(legacy.getKey());
        neutral.setFramework("langchain4j");
        neutral.setProvider(legacy.getProvider());
        neutral.setType(AiEmbeddingStoreType.VECTOR);
        neutral.setEnabled(legacy.isEnabled());
        Map<String, Object> properties = new LinkedHashMap<String, Object>(legacy.getProperties());
        if (legacy.getDimension() != null) {
            properties.put("dimension", legacy.getDimension());
        }
        if (legacy.getMetadataKeys() != null && !legacy.getMetadataKeys().isEmpty()) {
            properties.put("metadataKeys", new ArrayList<String>(legacy.getMetadataKeys()));
        }
        neutral.setProperties(properties);
        return neutral;
    }

    private Map<String, EmbeddingStoreDefinition> definitions(EmbeddingStoreProperties properties) {
        Map<String, EmbeddingStoreDefinition> definitions = new LinkedHashMap<String, EmbeddingStoreDefinition>();
        for (Map.Entry<String, EmbeddingStoreDefinition> entry : properties.getStores().entrySet()) {
            EmbeddingStoreDefinition definition = entry.getValue();
            if (definition == null || !definition.isEnabled()) {
                continue;
            }
            if (isBlank(definition.getKey())) {
                definition.setKey(entry.getKey());
            }
            if (isBlank(definition.getKey())) {
                throw new EmbeddingStoreException("Embedding store key must not be blank");
            }
            if (isBlank(definition.getProvider())) {
                throw new EmbeddingStoreException("Embedding store provider must not be blank: " + definition.getKey());
            }
            if (definitions.put(definition.getKey(), definition) != null) {
                throw new EmbeddingStoreException("Duplicate embedding store key: " + definition.getKey());
            }
        }
        return definitions;
    }

    private void validateDefaultStore(String defaultStore, Map<String, EmbeddingStore<TextSegment>> stores) {
        if (!isBlank(defaultStore) && !stores.containsKey(defaultStore)) {
            throw new EmbeddingStoreException("Default embedding store key is invalid: " + defaultStore);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }
}
