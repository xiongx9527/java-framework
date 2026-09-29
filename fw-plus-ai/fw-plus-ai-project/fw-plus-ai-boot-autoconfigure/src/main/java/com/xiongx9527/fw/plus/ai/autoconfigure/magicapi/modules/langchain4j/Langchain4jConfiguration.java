package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jEmbeddingStoreModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingLanguageModelModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jTokenizerModule;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.tool.Langchain4jToolConfiguration;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Magic-API → LangChain4j 集成入口：七类脚本模块（脚本用 {@code chatModel['key']}、
 * {@code embeddingStore['key']} 等直接取模型实例后调用 langchain4j 方法）与工具装配
 * {@link Langchain4jToolConfiguration}。
 *
 * <p>各脚本模块依赖 datasource 插件对应的实例持有者，因此各自以
 * {@code @ConditionalOnBean(对应持有者)} 独立装配；某类型停用时仅该脚本模块缺席。</p>
 */
@Configuration
@ConditionalOnClass(name = "dev.langchain4j.model.chat.ChatLanguageModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Import(Langchain4jToolConfiguration.class)
public class Langchain4jConfiguration {

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jChatModelDataSource.class)
    public Langchain4jChatModelModule langchain4jChatModelModule(
            MagicDynamicLangchain4jChatModelDataSource chatModels) {
        return new Langchain4jChatModelModule(chatModels);
    }

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jStreamingChatModelDataSource.class)
    public Langchain4jStreamingChatModelModule langchain4jStreamingChatModelModule(
            MagicDynamicLangchain4jStreamingChatModelDataSource streamingChatModels) {
        return new Langchain4jStreamingChatModelModule(streamingChatModels);
    }

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jLanguageModelDataSource.class)
    public Langchain4jLanguageModelModule langchain4jLanguageModelModule(
            MagicDynamicLangchain4jLanguageModelDataSource languageModels) {
        return new Langchain4jLanguageModelModule(languageModels);
    }

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jStreamingLanguageModelDataSource.class)
    public Langchain4jStreamingLanguageModelModule langchain4jStreamingLanguageModelModule(
            MagicDynamicLangchain4jStreamingLanguageModelDataSource streamingLanguageModels) {
        return new Langchain4jStreamingLanguageModelModule(streamingLanguageModels);
    }

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jEmbeddingModelDataSource.class)
    public Langchain4jEmbeddingModelModule langchain4jEmbeddingModelModule(
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels) {
        return new Langchain4jEmbeddingModelModule(embeddingModels);
    }

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jTokenizerDataSource.class)
    public Langchain4jTokenizerModule langchain4jTokenizerModule(
            MagicDynamicLangchain4jTokenizerDataSource tokenizers) {
        return new Langchain4jTokenizerModule(tokenizers);
    }

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jEmbeddingStoreDataSource.class)
    public Langchain4jEmbeddingStoreModule langchain4jEmbeddingStoreModule(
            MagicDynamicLangchain4jEmbeddingStoreDataSource embeddingStores) {
        return new Langchain4jEmbeddingStoreModule(embeddingStores);
    }
}
