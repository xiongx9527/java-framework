package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jEmbeddingStoreMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * embedding-store 数据源的装配：实例持有者、资源存储与动态注册器。
 * 注册器额外依赖 embedding-model 持有者，用于按 {@code embeddingModelKey} 取被引用的模型。
 */
@Configuration(proxyBeanMethods = false)
public class Langchain4jEmbeddingStoreConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jEmbeddingStoreDataSource langchain4jEmbeddingStoreDataSource() {
        return new MagicDynamicLangchain4jEmbeddingStoreDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingStoreMagicResourceStorage langchain4jEmbeddingStoreMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jEmbeddingStoreMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.EMBEDDING_STORE));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingStoreMagicDynamicRegistry langchain4jEmbeddingStoreMagicDynamicRegistry(
            Langchain4jEmbeddingStoreMagicResourceStorage storage,
            MagicDynamicLangchain4jEmbeddingStoreDataSource dataSource,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModelDataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jEmbeddingStoreMagicDynamicRegistry(storage, dataSource, embeddingModelDataSource,
                factories);
    }
}
