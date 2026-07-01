package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.anthropic.AnthropicChatModel;
import dev.langchain4j.model.anthropic.AnthropicStreamingChatModel;
import dev.langchain4j.model.azure.*;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.dashscope.*;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiEmbeddingModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceChatModel;
import dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel;
import dev.langchain4j.model.huggingface.HuggingFaceLanguageModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.localai.LocalAiChatModel;
import dev.langchain4j.model.localai.LocalAiEmbeddingModel;
import dev.langchain4j.model.localai.LocalAiLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.model.ollama.OllamaLanguageModel;
import dev.langchain4j.model.openai.*;
import dev.langchain4j.model.qianfan.QianfanChatModel;
import dev.langchain4j.model.qianfan.QianfanEmbeddingModel;
import dev.langchain4j.model.qianfan.QianfanLanguageModel;
import dev.langchain4j.model.qianfan.QianfanStreamingChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiChatModel;
import dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel;
import dev.langchain4j.model.zhipu.ZhipuAiImageModel;
import dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

public interface LangChain4jModelFactory {

    @Data
    class ModelInfo {
        private Object model;
        private LangChain4jModel.Provider provider;
    }

    static LangChain4jModel createModel(LangChain4jModel.Provider provider, LangChain4jModel.ModelType modelType, Map<String, Object> config) {
        ModelInfo nativeModel = createNativeModel(provider, modelType, config);
        return wrapModel(modelType, nativeModel);
    }

    static LangChain4jModel wrapModel(LangChain4jModel.ModelType modelType, ModelInfo nativeModel) {
        switch (modelType) {
            case CHAT:
                if (nativeModel.getModel() instanceof ChatLanguageModel) {
                    return new LangChain4jModel.LangChain4jGeneralChatModelImpl((ChatLanguageModel) nativeModel.getModel(), nativeModel.getProvider());
                }
                break;
            case STREAMING_CHAT:
                if (nativeModel.getModel() instanceof StreamingChatLanguageModel) {
                    return new LangChain4jModel.LangChain4jStreamingChatModel((StreamingChatLanguageModel) nativeModel.getModel(), nativeModel.getProvider());
                }
                break;
            case EMBEDDING:
                if (nativeModel.getModel() instanceof EmbeddingModel) {
                    return new LangChain4jModel.LangChain4jEmbeddingModel((EmbeddingModel) nativeModel.getModel(), nativeModel.getProvider());
                }
                break;
            case IMAGE:
                if (nativeModel.getModel() instanceof ImageModel) {
                    return new LangChain4jModel.LangChain4jImageModelModel((ImageModel) nativeModel.getModel(), nativeModel.getProvider());
                }
                break;
            case LANGUAGE:
                if (nativeModel.getModel() instanceof LanguageModel) {
                    return new LangChain4jModel.LangChain4jGeneralLanguageModel((LanguageModel) nativeModel.getModel(), nativeModel.getProvider());
                }
                break;
            case STREAMING_LANGUAGE:
                if (nativeModel.getModel() instanceof StreamingLanguageModel) {
                    return new LangChain4jModel.LangChain4jStreamingLanguageModel((StreamingLanguageModel) nativeModel.getModel(), nativeModel.getProvider());
                }
                break;
        }
        throw new IllegalArgumentException("Unsupported model type or native model");
    }

    // ============ Provider Builder Registry ============

    Map<LangChain4jModel.Provider, Map<LangChain4jModel.ModelType, ModelBuilder>> PROVIDER_BUILDERS = buildRegistry();

    static Map<LangChain4jModel.Provider, Map<LangChain4jModel.ModelType, ModelBuilder>> buildRegistry() {
        Map<LangChain4jModel.Provider, Map<LangChain4jModel.ModelType, ModelBuilder>> registry = new HashMap<>();

        // OpenAI
        Map<LangChain4jModel.ModelType, ModelBuilder> openAi = new HashMap<>();
        openAi.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> OpenAiChatModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        openAi.put(LangChain4jModel.ModelType.STREAMING_CHAT, (url, key, name, cfg) -> OpenAiStreamingChatModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        openAi.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> OpenAiEmbeddingModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        openAi.put(LangChain4jModel.ModelType.IMAGE, (url, key, name, cfg) -> OpenAiImageModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        openAi.put(LangChain4jModel.ModelType.LANGUAGE, (url, key, name, cfg) -> OpenAiLanguageModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        openAi.put(LangChain4jModel.ModelType.STREAMING_LANGUAGE, (url, key, name, cfg) -> OpenAiStreamingLanguageModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        registry.put(LangChain4jModel.Provider.OPENAI, openAi);

        // Azure OpenAI (使用 endpoint 代替 baseUrl, deploymentName 代替 modelName)
        Map<LangChain4jModel.ModelType, ModelBuilder> azureAi = new HashMap<>();
        azureAi.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> AzureOpenAiChatModel.builder().endpoint(url).apiKey(key).deploymentName(name).build());
        azureAi.put(LangChain4jModel.ModelType.STREAMING_CHAT, (url, key, name, cfg) -> AzureOpenAiStreamingChatModel.builder().endpoint(url).apiKey(key).deploymentName(name).build());
        azureAi.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> AzureOpenAiEmbeddingModel.builder().endpoint(url).apiKey(key).deploymentName(name).build());
        azureAi.put(LangChain4jModel.ModelType.IMAGE, (url, key, name, cfg) -> AzureOpenAiImageModel.builder().endpoint(url).apiKey(key).deploymentName(name).build());
        azureAi.put(LangChain4jModel.ModelType.LANGUAGE, (url, key, name, cfg) -> AzureOpenAiLanguageModel.builder().endpoint(url).apiKey(key).deploymentName(name).build());
        azureAi.put(LangChain4jModel.ModelType.STREAMING_LANGUAGE, (url, key, name, cfg) -> AzureOpenAiStreamingLanguageModel.builder().endpoint(url).apiKey(key).deploymentName(name).build());
        registry.put(LangChain4jModel.Provider.AZURE_OPENAI, azureAi);

        // Qwen
        Map<LangChain4jModel.ModelType, ModelBuilder> qwen = new HashMap<>();
        qwen.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> QwenChatModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        qwen.put(LangChain4jModel.ModelType.STREAMING_CHAT, (url, key, name, cfg) -> QwenStreamingChatModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        qwen.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> QwenEmbeddingModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        qwen.put(LangChain4jModel.ModelType.IMAGE, (url, key, name, cfg) -> WanxImageModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        qwen.put(LangChain4jModel.ModelType.LANGUAGE, (url, key, name, cfg) -> QwenLanguageModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        qwen.put(LangChain4jModel.ModelType.STREAMING_LANGUAGE, (url, key, name, cfg) -> QwenStreamingLanguageModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        registry.put(LangChain4jModel.Provider.QWEN, qwen);

        // Ollama (无需apiKey)
        Map<LangChain4jModel.ModelType, ModelBuilder> ollama = new HashMap<>();
        ollama.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> OllamaChatModel.builder().baseUrl(url).modelName(name).build());
        ollama.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> OllamaEmbeddingModel.builder().baseUrl(url).modelName(name).build());
        ollama.put(LangChain4jModel.ModelType.LANGUAGE, (url, key, name, cfg) -> OllamaLanguageModel.builder().baseUrl(url).modelName(name).build());
        registry.put(LangChain4jModel.Provider.OLLAMA, ollama);

        // HuggingFace (使用 accessToken 代替 apiKey, modelId 代替 modelName)
        Map<LangChain4jModel.ModelType, ModelBuilder> hf = new HashMap<>();
        hf.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> HuggingFaceChatModel.builder().accessToken(key).modelId(name).build());
        hf.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> HuggingFaceEmbeddingModel.builder().accessToken(key).modelId(name).build());
        hf.put(LangChain4jModel.ModelType.LANGUAGE, (url, key, name, cfg) -> HuggingFaceLanguageModel.builder().accessToken(key).modelId(name).build());
        registry.put(LangChain4jModel.Provider.HUGGING_FACE, hf);

        // Zhipu (使用 model 代替 modelName)
        Map<LangChain4jModel.ModelType, ModelBuilder> zhipu = new HashMap<>();
        zhipu.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> ZhipuAiChatModel.builder().baseUrl(url).apiKey(key).model(name).build());
        zhipu.put(LangChain4jModel.ModelType.STREAMING_CHAT, (url, key, name, cfg) -> ZhipuAiStreamingChatModel.builder().baseUrl(url).apiKey(key).model(name).build());
        zhipu.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> ZhipuAiEmbeddingModel.builder().baseUrl(url).apiKey(key).model(name).build());
        zhipu.put(LangChain4jModel.ModelType.IMAGE, (url, key, name, cfg) -> ZhipuAiImageModel.builder().baseUrl(url).apiKey(key).model(name).build());
        registry.put(LangChain4jModel.Provider.ZHIPU, zhipu);

        // Qianfan (需要 secretKey，与 apiKey 相同)
        Map<LangChain4jModel.ModelType, ModelBuilder> qianfan = new HashMap<>();
        qianfan.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> QianfanChatModel.builder().baseUrl(url).apiKey(key).secretKey(key).modelName(name).build());
        qianfan.put(LangChain4jModel.ModelType.STREAMING_CHAT, (url, key, name, cfg) -> QianfanStreamingChatModel.builder().baseUrl(url).apiKey(key).secretKey(key).modelName(name).build());
        qianfan.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> QianfanEmbeddingModel.builder().baseUrl(url).apiKey(key).secretKey(key).modelName(name).build());
        qianfan.put(LangChain4jModel.ModelType.LANGUAGE, (url, key, name, cfg) -> QianfanLanguageModel.builder().baseUrl(url).apiKey(key).secretKey(key).modelName(name).build());
        registry.put(LangChain4jModel.Provider.QIANFAN, qianfan);

        // LocalAI (无需apiKey)
        Map<LangChain4jModel.ModelType, ModelBuilder> localAi = new HashMap<>();
        localAi.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> LocalAiChatModel.builder().baseUrl(url).modelName(name).build());
        localAi.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> LocalAiEmbeddingModel.builder().baseUrl(url).modelName(name).build());
        localAi.put(LangChain4jModel.ModelType.LANGUAGE, (url, key, name, cfg) -> LocalAiLanguageModel.builder().baseUrl(url).modelName(name).build());
        registry.put(LangChain4jModel.Provider.LOCAL_AI, localAi);

        // Gemini (无需baseUrl)
        Map<LangChain4jModel.ModelType, ModelBuilder> gemini = new HashMap<>();
        gemini.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> GoogleAiGeminiChatModel.builder().apiKey(key).modelName(name).build());
        gemini.put(LangChain4jModel.ModelType.EMBEDDING, (url, key, name, cfg) -> GoogleAiEmbeddingModel.builder().apiKey(key).modelName(name).build());
        registry.put(LangChain4jModel.Provider.GEMINI, gemini);

        // Anthropic
        Map<LangChain4jModel.ModelType, ModelBuilder> anthropic = new HashMap<>();
        anthropic.put(LangChain4jModel.ModelType.CHAT, (url, key, name, cfg) -> AnthropicChatModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        anthropic.put(LangChain4jModel.ModelType.STREAMING_CHAT, (url, key, name, cfg) -> AnthropicStreamingChatModel.builder().baseUrl(url).apiKey(key).modelName(name).build());
        registry.put(LangChain4jModel.Provider.ANTHROPIC, anthropic);

        return registry;
    }

    // ============ Core Method ============

    static ModelInfo createNativeModel(LangChain4jModel.Provider provider, LangChain4jModel.ModelType modelType, Map<String, Object> config) {
        String baseUrl = (String) config.get("baseUrl");
        String apiKey = (String) config.get("apiKey");
        String modelName = (String) config.get("modelName");

        Map<LangChain4jModel.ModelType, ModelBuilder> builders = PROVIDER_BUILDERS.get(provider);
        if (builders == null) {
            throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
        ModelBuilder builder = builders.get(modelType);
        if (builder == null) {
            throw new IllegalArgumentException("Unsupported model type " + modelType + " for provider: " + provider);
        }

        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(provider);
        modelInfo.setModel(builder.build(baseUrl, apiKey, modelName, config));
        return modelInfo;
    }

    // ============ ModelBuilder Interface ============

    interface ModelBuilder {
        Object build(String baseUrl, String apiKey, String modelName, Map<String, Object> config);
    }

}
