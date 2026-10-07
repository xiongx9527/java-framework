package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jTokenizerMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jTokenizerMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jTokenizerModule;
import dev.langchain4j.model.Tokenizer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** tokenizer 数据源的装配：实例持有者、资源存储与动态注册器。 */
@ConditionalOnClass(name = "dev.langchain4j.model.Tokenizer")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.tokenizer", name = "enabled",
        havingValue = "true")
@Configuration(proxyBeanMethods = false)
public class Langchain4jTokenizerConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-tokenizer.1.0.0.iife.js";

    public Langchain4jTokenizerConfiguration() {
        super("langchain4j-tokenizer", "MagicLangchain4jTokenizer", JAVASCRIPT_FILENAME, false);
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
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<Tokenizer> defaultInstance) {
        return new Langchain4jTokenizerMagicDynamicRegistry(storage, dataSource, factories, defaultInstance);
    }

    /** 脚本模块：chatModel['key'] 按 key 取实例；不指定 key 时回落到 Spring 默认实例 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jTokenizerModule langchain4jTokenizerModule(MagicDynamicLangchain4jTokenizerDataSource tokenizers, ObjectProvider<Tokenizer> defaultInstance) {
        return new Langchain4jTokenizerModule(tokenizers, defaultInstance);
    }
}