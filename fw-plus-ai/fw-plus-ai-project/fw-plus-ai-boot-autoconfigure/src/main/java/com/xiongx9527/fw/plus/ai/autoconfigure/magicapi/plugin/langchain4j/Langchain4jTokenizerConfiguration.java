package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jTokenizerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jTokenizerMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** tokenizer 数据源的装配：实例持有者、资源存储与动态注册器。 */
@Configuration(proxyBeanMethods = false)
public class Langchain4jTokenizerConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-tokenizer.1.0.0.iife.js";

    public Langchain4jTokenizerConfiguration() {
        super("langchain4j-tokenizer", "MagicLangchain4jTokenizer", JAVASCRIPT_FILENAME);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jTokenizerDataSource langchain4jTokenizerDataSource() {
        return new MagicDynamicLangchain4jTokenizerDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jTokenizerMagicResourceStorage langchain4jTokenizerMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jTokenizerMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.TOKENIZER));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jTokenizerMagicDynamicRegistry langchain4jTokenizerMagicDynamicRegistry(
            Langchain4jTokenizerMagicResourceStorage storage,
            MagicDynamicLangchain4jTokenizerDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jTokenizerMagicDynamicRegistry(storage, dataSource, factories);
    }
}
