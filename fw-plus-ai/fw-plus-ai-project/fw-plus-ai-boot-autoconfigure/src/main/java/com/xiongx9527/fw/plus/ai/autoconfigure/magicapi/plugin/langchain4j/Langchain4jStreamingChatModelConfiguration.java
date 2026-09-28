package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jStreamingChatModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jStreamingChatModelMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** streaming-chat-model 数据源的装配：实例持有者、资源存储与动态注册器。 */
@Configuration(proxyBeanMethods = false)
public class Langchain4jStreamingChatModelConfiguration {

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
}
