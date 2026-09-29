package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jStreamingLanguageModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingLanguageModelMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import dev.langchain4j.model.language.StreamingLanguageModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** streaming-language-model 数据源的装配：实例持有者、资源存储与动态注册器。 */
@ConditionalOnClass(name = "dev.langchain4j.model.language.StreamingLanguageModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.streaming-language-model", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jStreamingLanguageModelConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-streaming-language-model.1.0.0.iife.js";

    public Langchain4jStreamingLanguageModelConfiguration() {
        super("langchain4j-streaming-language-model", "MagicLangchain4jStreamingLanguageModel", JAVASCRIPT_FILENAME);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jStreamingLanguageModelDataSource langchain4jStreamingLanguageModelDataSource() {
        return new MagicDynamicLangchain4jStreamingLanguageModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingLanguageModelMagicResourceStorage
            langchain4jStreamingLanguageModelMagicResourceStorage(Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jStreamingLanguageModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingLanguageModelMagicDynamicRegistry
            langchain4jStreamingLanguageModelMagicDynamicRegistry(
                    Langchain4jStreamingLanguageModelMagicResourceStorage storage,
                    MagicDynamicLangchain4jStreamingLanguageModelDataSource dataSource,
                    Langchain4jModelFactoryRegistry factories,
            ObjectProvider<StreamingLanguageModel> defaultInstance) {
        return new Langchain4jStreamingLanguageModelMagicDynamicRegistry(storage, dataSource, factories, defaultInstance);
    }

    /** 脚本模块：chatModel['key'] 按 key 取实例；不指定 key 时回落到 Spring 默认实例 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingLanguageModelModule langchain4jStreamingLanguageModelModule(MagicDynamicLangchain4jStreamingLanguageModelDataSource streamingLanguageModels, ObjectProvider<StreamingLanguageModel> defaultInstance) {
        return new Langchain4jStreamingLanguageModelModule(streamingLanguageModels, defaultInstance);
    }
}