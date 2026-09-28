package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.core.Langchain4jModule;
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
 * Magic-API → LangChain4j 集成入口：只读的脚本模块 {@link Langchain4jModule}（读用 datasource
 * 插件持有的实例）与工具装配 {@link Langchain4jToolConfiguration}。
 *
 * <p>脚本模块依赖 datasource 插件的七类实例持有者，因此仅在插件已装配时注册；插件未启用时
 * 不注册该 bean，脚本侧不会出现半可用的模块。</p>
 */
@Configuration
@ConditionalOnClass(name = "dev.langchain4j.model.chat.ChatLanguageModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Import(Langchain4jToolConfiguration.class)
public class Langchain4jConfiguration {

    @Bean
    @ConditionalOnBean(MagicDynamicLangchain4jChatModelDataSource.class)
    public Langchain4jModule langchain4jModule(MagicDynamicLangchain4jChatModelDataSource chatModels,
            MagicDynamicLangchain4jStreamingChatModelDataSource streamingChatModels,
            MagicDynamicLangchain4jLanguageModelDataSource languageModels,
            MagicDynamicLangchain4jStreamingLanguageModelDataSource streamingLanguageModels,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels,
            MagicDynamicLangchain4jTokenizerDataSource tokenizers,
            MagicDynamicLangchain4jEmbeddingStoreDataSource embeddingStores) {
        return new Langchain4jModule(chatModels, streamingChatModels, languageModels, streamingLanguageModels,
                embeddingModels, tokenizers, embeddingStores);
    }
}
