package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jDatasourceController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jStartupRegistrar;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * LangChain4j 模型资源插件的父装配：跨类型共享内容（工厂短名表、启动重试、插件标识与
 * 连接测试 Controller）留在此处，七类资源各自的持有者、存储与注册器由对应的子
 * Configuration 声明并通过 {@link Import} 引入。
 *
 * <p>生效条件：magic-api starter 在 classpath、{@link MagicResourceService} bean 已注册、
 * {@code fw.plus.magicapi.langchain4j} 未关闭（默认开启）。storage 的 provider 短名表
 * 由已注册工厂的能力探测生成，与 FactoryRegistry 单一事实源一致。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@ConditionalOnBean(MagicResourceService.class)
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Import({
        Langchain4jModelFactoryConfiguration.class,
        Langchain4jChatModelConfiguration.class,
        Langchain4jStreamingChatModelConfiguration.class,
        Langchain4jLanguageModelConfiguration.class,
        Langchain4jStreamingLanguageModelConfiguration.class,
        Langchain4jEmbeddingModelConfiguration.class,
        Langchain4jTokenizerConfiguration.class,
        Langchain4jEmbeddingStoreConfiguration.class
})
public class Langchain4jDatasourceConfiguration implements MagicPluginConfiguration {

    private final Langchain4jModelFactoryRegistry factories;
    private final MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;

    public Langchain4jDatasourceConfiguration(Langchain4jModelFactoryRegistry factories,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels) {
        this.factories = factories;
        this.embeddingModels = embeddingModels;
    }

    /** 启动完成钩子：embedding-store 先于其 embedding-model 加载时，统一重试一次注册 */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStartupRegistrar langchain4jStartupRegistrar(MagicResourceService magicResourceService,
            MagicDynamicLangchain4jEmbeddingStoreDataSource storeDataSource,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModelDataSource,
            Langchain4jModelFactoryRegistry factories,
            Langchain4jEmbeddingStoreMagicResourceStorage storeStorage) {
        return new Langchain4jStartupRegistrar(magicResourceService, storeDataSource, embeddingModelDataSource,
                factories, storeStorage);
    }

    @Override
    public Plugin plugin() {
        return new Plugin("langchain4j", "MagicLangchain4j", "magic-langchain4j.1.0.0.iife.js");
    }

    @Override
    public MagicControllerRegister controllerRegister() {
        return (mapping, configuration) ->
                mapping.registerController(new Langchain4jDatasourceController(configuration, factories,
                        embeddingModels));
    }
}
