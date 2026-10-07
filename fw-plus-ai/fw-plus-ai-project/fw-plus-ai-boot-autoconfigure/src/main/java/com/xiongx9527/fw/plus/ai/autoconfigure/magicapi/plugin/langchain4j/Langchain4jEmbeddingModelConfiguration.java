package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jEmbeddingModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingModelMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingModelModule;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** embedding-model 数据源的装配：实例持有者、资源存储与动态注册器。 */
@ConditionalOnClass(name = "dev.langchain4j.model.embedding.EmbeddingModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.embedding-model", name = "enabled",
        havingValue = "true")
@Configuration(proxyBeanMethods = false)
public class Langchain4jEmbeddingModelConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-embedding-model.1.0.0.iife.js";

    public Langchain4jEmbeddingModelConfiguration() {
        super("langchain4j-embedding-model", "MagicLangchain4jEmbeddingModel", JAVASCRIPT_FILENAME, false);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jEmbeddingModelDataSource langchain4jEmbeddingModelDataSource() {
        return new MagicDynamicLangchain4jEmbeddingModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingModelMagicResourceStorage langchain4jEmbeddingModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jEmbeddingModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.EMBEDDING_MODEL));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingModelMagicDynamicRegistry langchain4jEmbeddingModelMagicDynamicRegistry(
            Langchain4jEmbeddingModelMagicResourceStorage storage,
            MagicDynamicLangchain4jEmbeddingModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<EmbeddingModel> defaultInstance) {
        return new Langchain4jEmbeddingModelMagicDynamicRegistry(storage, dataSource, factories, defaultInstance);
    }

    /** 脚本模块：chatModel['key'] 按 key 取实例；不指定 key 时回落到 Spring 默认实例 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingModelModule langchain4jEmbeddingModelModule(MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels, ObjectProvider<EmbeddingModel> defaultInstance) {
        return new Langchain4jEmbeddingModelModule(embeddingModels, defaultInstance);
    }
}