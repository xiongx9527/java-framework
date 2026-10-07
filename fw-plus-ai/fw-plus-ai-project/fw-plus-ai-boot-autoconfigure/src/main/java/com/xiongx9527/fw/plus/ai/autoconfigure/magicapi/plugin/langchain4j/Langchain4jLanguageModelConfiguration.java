package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jLanguageModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jLanguageModelMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import dev.langchain4j.model.language.LanguageModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** language-model 数据源的装配：实例持有者、资源存储与动态注册器。 */
@ConditionalOnClass(name = "dev.langchain4j.model.language.LanguageModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.language-model", name = "enabled",
        havingValue = "true")
@Configuration(proxyBeanMethods = false)
public class Langchain4jLanguageModelConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-language-model.1.0.0.iife.js";

    public Langchain4jLanguageModelConfiguration() {
        super("langchain4j-language-model", "MagicLangchain4jLanguageModel", JAVASCRIPT_FILENAME, false);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jLanguageModelDataSource langchain4jLanguageModelDataSource() {
        return new MagicDynamicLangchain4jLanguageModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jLanguageModelMagicResourceStorage langchain4jLanguageModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jLanguageModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.LANGUAGE_MODEL));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jLanguageModelMagicDynamicRegistry langchain4jLanguageModelMagicDynamicRegistry(
            Langchain4jLanguageModelMagicResourceStorage storage,
            MagicDynamicLangchain4jLanguageModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<LanguageModel> defaultInstance) {
        return new Langchain4jLanguageModelMagicDynamicRegistry(storage, dataSource, factories, defaultInstance);
    }

    /** 脚本模块：chatModel['key'] 按 key 取实例；不指定 key 时回落到 Spring 默认实例 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jLanguageModelModule langchain4jLanguageModelModule(MagicDynamicLangchain4jLanguageModelDataSource languageModels, ObjectProvider<LanguageModel> defaultInstance) {
        return new Langchain4jLanguageModelModule(languageModels, defaultInstance);
    }
}