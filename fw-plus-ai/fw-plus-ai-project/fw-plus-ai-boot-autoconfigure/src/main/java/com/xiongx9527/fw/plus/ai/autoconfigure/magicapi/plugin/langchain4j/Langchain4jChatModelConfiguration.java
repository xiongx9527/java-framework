package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jChatModelMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jChatModelMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jDatasourceController;
import dev.langchain4j.model.chat.ChatModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jChatModelModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/** chat-model 数据源的装配：实例持有者、资源存储与动态注册器；并承载连接测试 Controller。 */
@ConditionalOnClass(name = "dev.langchain4j.model.chat.ChatModel")
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j.chat-model", name = "enabled",
        havingValue = "true", matchIfMissing = true)
@Configuration(proxyBeanMethods = false)
public class Langchain4jChatModelConfiguration extends AbstractLangchain4jPluginConfiguration {

    private static final String JAVASCRIPT_FILENAME = "magic-langchain4j-chat-model.1.0.0.iife.js";

    private final Langchain4jModelFactoryRegistry factories;
    /** 惰性依赖：embedding-model 按类型独立装配，可能未启用；仅连接测试 embedding-store 时取用 */
    private final ObjectProvider<MagicDynamicLangchain4jEmbeddingModelDataSource> embeddingModels;

    public Langchain4jChatModelConfiguration(Langchain4jModelFactoryRegistry factories,
            ObjectProvider<MagicDynamicLangchain4jEmbeddingModelDataSource> embeddingModels) {
        super("langchain4j-chat-model", "MagicLangchain4jChatModel", JAVASCRIPT_FILENAME);
        this.factories = factories;
        this.embeddingModels = embeddingModels;
    }

    /**
     * 连接测试 Controller 由本类承载：路径为 {@code /langchain4j/{type}/datasource/test}，
     * 一个 Controller 即服务七类，其余类型配置沿用公共类的空注册器，避免同一路径重复注册。
     */
    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) ->
                mapping.registerController(new Langchain4jDatasourceController(configuration, factories,
                        embeddingModels.getIfAvailable()));
    }


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
            Langchain4jModelFactoryRegistry factories,
            ObjectProvider<ChatModel> defaultInstance) {
        return new Langchain4jChatModelMagicDynamicRegistry(storage, dataSource, factories, defaultInstance);
    }

    /** 脚本模块：chatModel['key'] 按 key 取实例；不指定 key 时回落到 Spring 默认实例 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jChatModelModule langchain4jChatModelModule(MagicDynamicLangchain4jChatModelDataSource chatModels, ObjectProvider<ChatModel> defaultInstance) {
        return new Langchain4jChatModelModule(chatModels, defaultInstance);
    }
}