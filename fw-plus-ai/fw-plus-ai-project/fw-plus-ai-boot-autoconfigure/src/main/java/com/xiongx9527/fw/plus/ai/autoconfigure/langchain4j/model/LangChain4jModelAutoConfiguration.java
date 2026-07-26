package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model;

import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.DefaultLangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelDefinition;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelException;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelFactory;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelProviderAdapter;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelRegistry;
import com.xiongx9527.fw.plus.ai.langchain4j.model.LangChain4jModelType;
import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.provider.ReflectiveLangChain4jModelProviderAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(LangChain4jModelProperties.class)
@ConditionalOnProperty(prefix = "fw-plus.ai.langchain4j.model", name = "enabled", havingValue = "true", matchIfMissing = true)
public class LangChain4jModelAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "openAiChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter openAiChatModelProviderAdapter() { return adapter("openai", LangChain4jModelType.CHAT, "dev.langchain4j.model.openai.OpenAiChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "openAiStreamingChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter openAiStreamingChatModelProviderAdapter() { return adapter("openai", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.openai.OpenAiStreamingChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "openAiEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter openAiEmbeddingModelProviderAdapter() { return adapter("openai", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.openai.OpenAiEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "openAiImageModelProviderAdapter")
    public LangChain4jModelProviderAdapter openAiImageModelProviderAdapter() { return adapter("openai", LangChain4jModelType.IMAGE, "dev.langchain4j.model.openai.OpenAiImageModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "openAiLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter openAiLanguageModelProviderAdapter() { return adapter("openai", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.openai.OpenAiLanguageModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "openAiStreamingLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter openAiStreamingLanguageModelProviderAdapter() { return adapter("openai", LangChain4jModelType.STREAMING_LANGUAGE, "dev.langchain4j.model.openai.OpenAiStreamingLanguageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "azureOpenAiChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter azureOpenAiChatModelProviderAdapter() { return adapter("azure-openai", LangChain4jModelType.CHAT, "dev.langchain4j.model.azure.AzureOpenAiChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "azureOpenAiStreamingChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter azureOpenAiStreamingChatModelProviderAdapter() { return adapter("azure-openai", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "azureOpenAiEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter azureOpenAiEmbeddingModelProviderAdapter() { return adapter("azure-openai", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "azureOpenAiImageModelProviderAdapter")
    public LangChain4jModelProviderAdapter azureOpenAiImageModelProviderAdapter() { return adapter("azure-openai", LangChain4jModelType.IMAGE, "dev.langchain4j.model.azure.AzureOpenAiImageModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "azureOpenAiLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter azureOpenAiLanguageModelProviderAdapter() { return adapter("azure-openai", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.azure.AzureOpenAiLanguageModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "azureOpenAiStreamingLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter azureOpenAiStreamingLanguageModelProviderAdapter() { return adapter("azure-openai", LangChain4jModelType.STREAMING_LANGUAGE, "dev.langchain4j.model.azure.AzureOpenAiStreamingLanguageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "qwenChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter qwenChatModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.CHAT, "dev.langchain4j.model.dashscope.QwenChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qwenStreamingChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter qwenStreamingChatModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.dashscope.QwenStreamingChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qwenEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter qwenEmbeddingModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.dashscope.QwenEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qwenImageModelProviderAdapter")
    public LangChain4jModelProviderAdapter qwenImageModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.IMAGE, "dev.langchain4j.model.dashscope.WanxImageModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qwenLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter qwenLanguageModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.dashscope.QwenLanguageModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qwenStreamingLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter qwenStreamingLanguageModelProviderAdapter() { return adapter("qwen", LangChain4jModelType.STREAMING_LANGUAGE, "dev.langchain4j.model.dashscope.QwenStreamingLanguageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "ollamaChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter ollamaChatModelProviderAdapter() { return adapter("ollama", LangChain4jModelType.CHAT, "dev.langchain4j.model.ollama.OllamaChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "ollamaEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter ollamaEmbeddingModelProviderAdapter() { return adapter("ollama", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.ollama.OllamaEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "ollamaLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter ollamaLanguageModelProviderAdapter() { return adapter("ollama", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.ollama.OllamaLanguageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "anthropicChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter anthropicChatModelProviderAdapter() { return adapter("anthropic", LangChain4jModelType.CHAT, "dev.langchain4j.model.anthropic.AnthropicChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "anthropicStreamingChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter anthropicStreamingChatModelProviderAdapter() { return adapter("anthropic", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.anthropic.AnthropicStreamingChatModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "qianfanChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter qianfanChatModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.CHAT, "dev.langchain4j.model.qianfan.QianfanChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qianfanStreamingChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter qianfanStreamingChatModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.qianfan.QianfanStreamingChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qianfanEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter qianfanEmbeddingModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.qianfan.QianfanEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "qianfanLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter qianfanLanguageModelProviderAdapter() { return adapter("qianfan", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.qianfan.QianfanLanguageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "zhipuChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter zhipuChatModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.CHAT, "dev.langchain4j.model.zhipu.ZhipuAiChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "zhipuStreamingChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter zhipuStreamingChatModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.STREAMING_CHAT, "dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "zhipuEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter zhipuEmbeddingModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "zhipuImageModelProviderAdapter")
    public LangChain4jModelProviderAdapter zhipuImageModelProviderAdapter() { return adapter("zhipu", LangChain4jModelType.IMAGE, "dev.langchain4j.model.zhipu.ZhipuAiImageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "huggingFaceChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter huggingFaceChatModelProviderAdapter() { return adapter("hugging-face", LangChain4jModelType.CHAT, "dev.langchain4j.model.huggingface.HuggingFaceChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "huggingFaceEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter huggingFaceEmbeddingModelProviderAdapter() { return adapter("hugging-face", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "huggingFaceLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter huggingFaceLanguageModelProviderAdapter() { return adapter("hugging-face", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.huggingface.HuggingFaceLanguageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "localAiChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter localAiChatModelProviderAdapter() { return adapter("local-ai", LangChain4jModelType.CHAT, "dev.langchain4j.model.localai.LocalAiChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "localAiEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter localAiEmbeddingModelProviderAdapter() { return adapter("local-ai", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.localai.LocalAiEmbeddingModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "localAiLanguageModelProviderAdapter")
    public LangChain4jModelProviderAdapter localAiLanguageModelProviderAdapter() { return adapter("local-ai", LangChain4jModelType.LANGUAGE, "dev.langchain4j.model.localai.LocalAiLanguageModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "geminiChatModelProviderAdapter")
    public LangChain4jModelProviderAdapter geminiChatModelProviderAdapter() { return adapter("gemini", LangChain4jModelType.CHAT, "dev.langchain4j.model.googleai.GoogleAiGeminiChatModel"); }
    @Bean
    @ConditionalOnMissingBean(name = "geminiEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter geminiEmbeddingModelProviderAdapter() { return adapter("gemini", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.googleai.GoogleAiEmbeddingModel"); }

    @Bean
    @ConditionalOnMissingBean(name = "allMiniLmL6V2QuantizedEmbeddingModelProviderAdapter")
    public LangChain4jModelProviderAdapter allMiniLmL6V2QuantizedEmbeddingModelProviderAdapter() { return new ReflectiveLangChain4jModelProviderAdapter("all-mini-lm-l6-v2-quantized", LangChain4jModelType.EMBEDDING, "dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel", null); }

    @Bean
    @ConditionalOnMissingBean(LangChain4jModelFactory.class)
    public LangChain4jModelFactory langChain4jModelFactory() {
        return new DefaultLangChain4jModelFactory();
    }

    @Bean
    @ConditionalOnMissingBean(LangChain4jModelRegistry.class)
    public LangChain4jModelRegistry langChain4jModelRegistry(LangChain4jModelProperties properties,
                                                              LangChain4jModelFactory factory,
                                                              List<LangChain4jModelProviderAdapter> adapters) {
        Map<String, LangChain4jModel> models = new LinkedHashMap<String, LangChain4jModel>();
        for (Map.Entry<String, LangChain4jModelProperties.Model> entry : properties.getModels().entrySet()) {
            LangChain4jModelProperties.Model model = entry.getValue();
            if (model == null || !model.isEnabled()) {
                continue;
            }
            LangChain4jModelDefinition definition = definition(entry.getKey(), model);
            models.put(definition.getKey(), factory.create(definition, adapters));
        }
        validateDefaultModel(properties.getDefaultModel(), models);
        return new DefaultLangChain4jModelRegistry(models, properties.getDefaultModel());
    }

    private LangChain4jModelDefinition definition(String key, LangChain4jModelProperties.Model model) {
        LangChain4jModelDefinition definition = new LangChain4jModelDefinition();
        definition.setKey(key);
        definition.setProvider(model.getProvider());
        definition.setModelType(model.getModelType());
        definition.setEnabled(model.isEnabled());
        definition.setBaseUrl(model.getBaseUrl());
        definition.setApiKey(model.getApiKey());
        definition.setModelName(model.getModelName());
        definition.setProperties(model.getProperties());
        return definition;
    }

    private void validateDefaultModel(String defaultModel, Map<String, LangChain4jModel> models) {
        if (!isBlank(defaultModel) && !models.containsKey(defaultModel)) {
            throw new LangChain4jModelException("Default model key is invalid: " + defaultModel);
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().length() == 0;
    }

    private LangChain4jModelProviderAdapter adapter(String provider, LangChain4jModelType modelType, String builderClassName) {
        return new ReflectiveLangChain4jModelProviderAdapter(provider, modelType, builderClassName, "builder");
    }
}
