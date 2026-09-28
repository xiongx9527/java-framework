package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jChatModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jChatModelMagicResourceStorage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** chat-model 数据源的装配：实例持有者、资源存储与动态注册器。 */
@Configuration(proxyBeanMethods = false)
public class Langchain4jChatModelConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jChatModelDataSource langchain4jChatModelDataSource() {
        return new MagicDynamicLangchain4jChatModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jChatModelMagicResourceStorage langchain4jChatModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jChatModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.CHAT_MODEL));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jChatModelMagicDynamicRegistry langchain4jChatModelMagicDynamicRegistry(
            Langchain4jChatModelMagicResourceStorage storage,
            MagicDynamicLangchain4jChatModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jChatModelMagicDynamicRegistry(storage, dataSource, factories);
    }
}
