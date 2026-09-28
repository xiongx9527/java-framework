package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.anthropic.Langchain4jAnthropicModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.azure.Langchain4jAzureOpenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.github.Langchain4jGitHubModelsModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.googleai.Langchain4jGoogleAiGeminiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.huggingface.Langchain4jHuggingFaceModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.localai.Langchain4jLocalAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ollama.Langchain4jOllamaModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.onnx.Langchain4jOnnxEmbeddingModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai.Langchain4jOpenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.qianfan.Langchain4jQianfanModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.qwen.Langchain4jQwenModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jElasticsearchModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jInMemoryModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jMilvusModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jNeo4jModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jQdrantModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jRedisModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jWeaviateModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.vertexai.Langchain4jVertexAiGeminiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.voyageai.Langchain4jVoyageAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.zhipu.Langchain4jZhipuModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.*;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.*;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jDatasourceController;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web.Langchain4jStartupRegistrar;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.ssssssss.magicapi.core.config.MagicPluginConfiguration;
import org.ssssssss.magicapi.core.model.Plugin;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.core.web.MagicControllerRegister;

/**
 * LangChain4j 模型资源插件装配：七类 datasource 的实例持有者、storage、动态注册器、工厂短名表、
 * 连接测试 Controller 与启动重试。
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
public class Langchain4jDatasourceConfiguration implements MagicPluginConfiguration {

    /** 全部 provider 工厂：短名 → 工厂；新增供应商在此登记 */
    private static ModelFactory[] defaultFactories() {
        return new ModelFactory[] {
                new Langchain4jOpenAiModelFactory(),
                new Langchain4jQwenModelFactory(),
                new Langchain4jOllamaModelFactory(),
                new Langchain4jAnthropicModelFactory(),
                new Langchain4jQianfanModelFactory(),
                new Langchain4jZhipuModelFactory(),
                new Langchain4jGoogleAiGeminiModelFactory(),
                new Langchain4jGitHubModelsModelFactory(),
                new Langchain4jVoyageAiModelFactory(),
                new Langchain4jAzureOpenAiModelFactory(),
                new Langchain4jVertexAiGeminiModelFactory(),
                new Langchain4jHuggingFaceModelFactory(),
                new Langchain4jLocalAiModelFactory(),
                new Langchain4jOnnxEmbeddingModelFactory("bge-small"),
                new Langchain4jOnnxEmbeddingModelFactory("all-minlm"),
                new Langchain4jInMemoryModelFactory(),
                new Langchain4jRedisModelFactory(),
                new Langchain4jMilvusModelFactory(),
                new Langchain4jQdrantModelFactory(),
                new Langchain4jWeaviateModelFactory(),
                new Langchain4jElasticsearchModelFactory(),
                new Langchain4jNeo4jModelFactory(),
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jModelFactoryRegistry langchain4jModelFactoryRegistry(
            ObjectProvider<ModelFactory> factoryProvider) {
        Langchain4jModelFactoryRegistry registry = new Langchain4jModelFactoryRegistry();
        for (ModelFactory factory : defaultFactories()) {
            registry.register(factory);
        }
        for (ModelFactory factory : factoryProvider) {
            if (factory != null) {
                registry.register(factory);
            }
        }
        return registry;
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jChatModelDataSource langchain4jChatModelDataSource() {
        return new MagicDynamicLangchain4jChatModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jStreamingChatModelDataSource langchain4jStreamingChatModelDataSource() {
        return new MagicDynamicLangchain4jStreamingChatModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jLanguageModelDataSource langchain4jLanguageModelDataSource() {
        return new MagicDynamicLangchain4jLanguageModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jStreamingLanguageModelDataSource langchain4jStreamingLanguageModelDataSource() {
        return new MagicDynamicLangchain4jStreamingLanguageModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jEmbeddingModelDataSource langchain4jEmbeddingModelDataSource() {
        return new MagicDynamicLangchain4jEmbeddingModelDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jTokenizerDataSource langchain4jTokenizerDataSource() {
        return new MagicDynamicLangchain4jTokenizerDataSource();
    }

    @Bean
    @ConditionalOnMissingBean
    public MagicDynamicLangchain4jEmbeddingStoreDataSource langchain4jEmbeddingStoreDataSource() {
        return new MagicDynamicLangchain4jEmbeddingStoreDataSource();
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
    public Langchain4jStreamingChatModelMagicResourceStorage langchain4jStreamingChatModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jStreamingChatModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.STREAMING_CHAT_MODEL));
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
    public Langchain4jStreamingLanguageModelMagicResourceStorage langchain4jStreamingLanguageModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jStreamingLanguageModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingModelMagicResourceStorage langchain4jEmbeddingModelMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jEmbeddingModelMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.EMBEDDING_MODEL));
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
    public Langchain4jEmbeddingStoreMagicResourceStorage langchain4jEmbeddingStoreMagicResourceStorage(
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jEmbeddingStoreMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.EMBEDDING_STORE));
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jChatModelMagicDynamicRegistry langchain4jChatModelMagicDynamicRegistry(
            Langchain4jChatModelMagicResourceStorage storage,
            MagicDynamicLangchain4jChatModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jChatModelMagicDynamicRegistry(storage, dataSource, factories);
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingChatModelMagicDynamicRegistry langchain4jStreamingChatModelMagicDynamicRegistry(
            Langchain4jStreamingChatModelMagicResourceStorage storage,
            MagicDynamicLangchain4jStreamingChatModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jStreamingChatModelMagicDynamicRegistry(storage, dataSource, factories);
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jLanguageModelMagicDynamicRegistry langchain4jLanguageModelMagicDynamicRegistry(
            Langchain4jLanguageModelMagicResourceStorage storage,
            MagicDynamicLangchain4jLanguageModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jLanguageModelMagicDynamicRegistry(storage, dataSource, factories);
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jStreamingLanguageModelMagicDynamicRegistry langchain4jStreamingLanguageModelMagicDynamicRegistry(
            Langchain4jStreamingLanguageModelMagicResourceStorage storage,
            MagicDynamicLangchain4jStreamingLanguageModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jStreamingLanguageModelMagicDynamicRegistry(storage, dataSource, factories);
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingModelMagicDynamicRegistry langchain4jEmbeddingModelMagicDynamicRegistry(
            Langchain4jEmbeddingModelMagicResourceStorage storage,
            MagicDynamicLangchain4jEmbeddingModelDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jEmbeddingModelMagicDynamicRegistry(storage, dataSource, factories);
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jTokenizerMagicDynamicRegistry langchain4jTokenizerMagicDynamicRegistry(
            Langchain4jTokenizerMagicResourceStorage storage,
            MagicDynamicLangchain4jTokenizerDataSource dataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jTokenizerMagicDynamicRegistry(storage, dataSource, factories);
    }

    @Bean
    @ConditionalOnMissingBean
    public Langchain4jEmbeddingStoreMagicDynamicRegistry langchain4jEmbeddingStoreMagicDynamicRegistry(
            Langchain4jEmbeddingStoreMagicResourceStorage storage,
            MagicDynamicLangchain4jEmbeddingStoreDataSource dataSource,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModelDataSource,
            Langchain4jModelFactoryRegistry factories) {
        return new Langchain4jEmbeddingStoreMagicDynamicRegistry(storage, dataSource, embeddingModelDataSource,
                factories);
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
        return (mapping, configuration) -> mapping.registerController(new Langchain4jDatasourceController(configuration));
    }
}
