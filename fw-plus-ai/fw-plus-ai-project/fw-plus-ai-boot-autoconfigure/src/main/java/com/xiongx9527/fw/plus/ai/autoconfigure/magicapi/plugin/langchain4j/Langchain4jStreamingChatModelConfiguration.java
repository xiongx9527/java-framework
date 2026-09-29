package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jStreamingChatModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingChatModelMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jStreamingChatModelModule;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** streaming-chat-model 数据源的装配：实例持有者、资源存储与动态注册器。 */
@ConditionalOnClass(name = "dev.langchain4j.model.chat.StreamingChatLanguageModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.streaming-chat-model", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jStreamingChatModelConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-streaming-chat-model.1.0.0.iife.js";

    public Langchain4jStreamingChatModelConfiguration() {
        super("langchain4j-streaming-chat-model", "MagicLangchain4jStreamingChatModel", JAVASCRIPT_FILENAME);
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jStreamingChatModelDataSource langchain4jStreamingChatModelDataSource() {
        return new MagicDynamicLangchain4jStreamingChatModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingChatModelMagicResourceStorage langchain4jStreamingChatModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jStreamingChatModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.STREAMING_CHAT_MODEL));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingChatModelMagicDynamicRegistry langchain4jStreamingChatModelMagicDynamicRegistry(
            Langchain4jStreamingChatModelMagicResourceStorage storage,
            MagicDynamicLangchain4jStreamingChatModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jStreamingChatModelMagicDynamicRegistry(storage, dataSource, factories);
    }

    /** 脚本模块：chatModel['key'] 按 key 取实例；不指定 key 时回落到 Spring 默认实例 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingChatModelModule langchain4jStreamingChatModelModule(MagicDynamicLangchain4jStreamingChatModelDataSource streamingChatModels, ObjectProvider<StreamingChatLanguageModel> defaultInstance) {
        return new Langchain4jStreamingChatModelModule(streamingChatModels, defaultInstance);
    }
}