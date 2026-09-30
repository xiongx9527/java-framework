package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.anthropic.Langchain4jAnthropicModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.azure.Langchain4jAzureOpenAiModelFactory;
<<<<<<< HEAD
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.bedrock.Langchain4jBedrockModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.chatglm.Langchain4jChatGlmModelFactory;
=======
>>>>>>> dev
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.cohere.Langchain4jCohereModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.github.Langchain4jGitHubModelsModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.googlegenai.Langchain4jGoogleGenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.googleai.Langchain4jGoogleAiGeminiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.huggingface.Langchain4jHuggingFaceModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.jina.Langchain4jJinaModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.localai.Langchain4jLocalAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.mistralai.Langchain4jMistralAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.nomic.Langchain4jNomicModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ollama.Langchain4jOllamaModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.onnx.Langchain4jOnnxEmbeddingModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ocigenai.Langchain4jOciGenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openai.Langchain4jOpenAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.openaiofficial.Langchain4jOpenAiOfficialModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ovhai.Langchain4jOvhAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.qianfan.Langchain4jQianfanModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.qwen.Langchain4jQwenModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jElasticsearchModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jInMemoryModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jMilvusModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jNebulaModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jNeo4jModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jQdrantModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jRedisModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jWeaviateModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.vertexai.Langchain4jVertexAiGeminiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.voyageai.Langchain4jVoyageAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.watsonx.Langchain4jWatsonxModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.workersai.Langchain4jWorkersAiModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.xinference.Langchain4jXinferenceModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.zhipu.Langchain4jZhipuModelFactory;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * provider 工厂注册表装配：与具体资源类型无关的共享内容，单独成配置以便被父装配构造注入。
 */
@Configuration(proxyBeanMethods = false)
public class Langchain4jModelFactoryConfiguration {

    /** 全部 provider 工厂：短名 → 工厂；新增供应商在此登记 */
    static ModelFactory[] defaultFactories() {
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
                new Langchain4jOnnxEmbeddingModelFactory("all-minilm"),
                new Langchain4jInMemoryModelFactory(),
                new Langchain4jRedisModelFactory(),
                new Langchain4jMilvusModelFactory(),
                new Langchain4jQdrantModelFactory(),
                new Langchain4jWeaviateModelFactory(),
                new Langchain4jElasticsearchModelFactory(),
                new Langchain4jNeo4jModelFactory(),
                new Langchain4jNebulaModelFactory(),
                // 新增提供商（2026-09：主线 stable / beta 与 community 线）
                new Langchain4jBedrockModelFactory(),
                new Langchain4jMistralAiModelFactory(),
                new Langchain4jOpenAiOfficialModelFactory(),
                new Langchain4jGoogleGenAiModelFactory(),
                new Langchain4jWatsonxModelFactory(),
                new Langchain4jWorkersAiModelFactory(),
                new Langchain4jOvhAiModelFactory(),
                new Langchain4jChatGlmModelFactory(),
                new Langchain4jXinferenceModelFactory(),
                new Langchain4jOciGenAiModelFactory(),
                new Langchain4jCohereModelFactory(),
                new Langchain4jJinaModelFactory(),
                new Langchain4jNomicModelFactory(),
        };
    }

    /**
     * provider 分发表：全部 provider 工厂 + Spring 容器中的 {@link ChatModelListener}。
     * listeners 会在构建 chat-model / streaming-chat-model 时统一下发给 provider。
     */
    @Bean
    @ConditionalOnMissingBean
    public Langchain4jModelFactoryRegistry langchain4jModelFactoryRegistry(
            ObjectProvider<ModelFactory> factoryProvider,
            ObjectProvider<ChatModelListener> listenerProvider) {
        List<ChatModelListener> listeners = new ArrayList<>();
        for (ChatModelListener listener : listenerProvider) {
            if (listener != null) {
                listeners.add(listener);
            }
        }
        Langchain4jModelFactoryRegistry registry = new Langchain4jModelFactoryRegistry(listeners);
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
}
