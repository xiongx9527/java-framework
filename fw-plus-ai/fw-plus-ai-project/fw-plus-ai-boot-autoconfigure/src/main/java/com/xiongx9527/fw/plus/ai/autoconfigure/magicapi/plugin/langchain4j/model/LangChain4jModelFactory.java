package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

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

import java.util.Map;

public interface LangChain4jModelFactory {

    /**
     * 模型信息类，用于封装模型对象及其提供者信息
     */
    @Data
    class ModelInfo {
        /**
         * 模型对象，可以是任何类型的模型实例
         */
        private Object model;

        /**
         * 模型提供者，指定模型的来源提供方
         */
        private LangChain4jModel.Provider provider;
    }


    /**
     * 根据厂家和模型类型创建LangChain4j模型
     */
    public static LangChain4jModel createModel(LangChain4jModel.Provider provider, LangChain4jModel.ModelType modelType, Map<String, Object> config) {
        ModelInfo nativeModel = createNativeModel(provider, modelType, config);
        return wrapModel(modelType, nativeModel);
    }


    /**
     * 将原生模型对象包装为LangChain4jModel接口实现
     *
     * @param modelType 模型类型枚举，指示需要创建的模型包装器类型
     * @param nativeModel 原生模型对象，需要与指定的模型类型匹配
     * * @return 返回对应类型的LangChain4jModel实现
     * @throws IllegalArgumentException 当模型类型不支持或原生模型类型不匹配时抛出异常
     */
    static LangChain4jModel wrapModel(LangChain4jModel.ModelType modelType, ModelInfo nativeModel) {
        // 根据模型类型创建对应的包装器实例
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


   /**
     * 根据指定的提供商和模型类型创建对应的原生模型实例
     *
     * @param provider 模型提供商枚举值，决定使用哪种AI服务
     * @param modelType 模型类型，指定创建的模型用途（如聊天、嵌入等）
     * @param config 包含模型配置信息的键值对映射，通常包括baseUrl、apiKey、modelName等参数
     * @return 返回对应提供商的模型实例对象
     * @throws IllegalArgumentException 当提供不支持的提供商时抛出异常
     */
    static ModelInfo createNativeModel(LangChain4jModel.Provider provider, LangChain4jModel.ModelType modelType, Map<String, Object> config) {
        // 从配置中提取基础连接参数
        String baseUrl = (String) config.get("baseUrl");
        String apiKey = (String) config.get("apiKey");
        String modelName = (String) config.get("modelName");
        // 根据不同的提供商创建对应的模型实例
        switch (provider) {
            case OPENAI:
                return createOpenAiModel(modelType, baseUrl ,apiKey, modelName,config);
            case AZURE_OPENAI:
                return createAzureOpenAiModel(modelType, baseUrl, apiKey, modelName, config);
            case ANTHROPIC:
                return createAnthropicModel(modelType, baseUrl, apiKey, modelName, config);
            case QWEN:
                return createQwenModel(modelType, baseUrl,apiKey, modelName,config);
            case HUGGING_FACE:
                return createHuggingFaceModel(modelType,apiKey, modelName, config);
            case OLLAMA:
                return createOllamaModel(modelType, baseUrl,modelName, config);
            case ZHIPU:
                return createZhipuModel(modelType, baseUrl,apiKey, modelName,config);
            case QIANFAN:
                return createQianfanModel(modelType, baseUrl,apiKey, modelName,config);
            case GEMINI:
                return createGeminiModel(modelType, apiKey, modelName, config);
            case LOCAL_AI:
                return createLocalAiModel(modelType, baseUrl,modelName, config);
            default:
                throw new IllegalArgumentException("Unsupported provider: " + provider);
        }
    }



    /**
     * 创建OpenAI模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的OpenAI模型
     * @param baseUrl OpenAI API的基础URL地址
     * @param apiKey OpenAI API的访问密钥
     * @param modelName 具体的模型名称
     * @param config 额外的配置参数映射表（当前未使用）
     * @return 返回对应类型的OpenAI模型实例
     * @throws IllegalArgumentException 当modelType不被支持时抛出异常
     */
    static ModelInfo createOpenAiModel(LangChain4jModel.ModelType modelType, String baseUrl, String apiKey, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.OPENAI);
        // 根据模型类型创建对应的OpenAI模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(OpenAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case STREAMING_CHAT:
                modelInfo.setModel(OpenAiStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(OpenAiEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case IMAGE:
                modelInfo.setModel(OpenAiImageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case LANGUAGE:
                modelInfo.setModel(OpenAiLanguageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case STREAMING_LANGUAGE:
                modelInfo.setModel(OpenAiStreamingLanguageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for OpenAI: " + modelType);
        }
        return modelInfo;
    }


    /**
     * 创建Azure OpenAI模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的Azure OpenAI模型
     * @param baseUrl Azure OpenAI API的基础URL地址
     * @param apiKey Azure OpenAI API的访问密钥
     * @param modelName 具体的模型名称
     * @param config 额外的配置参数映射表
     * @return 返回对应类型的Azure OpenAI模型实例
     * @throws IllegalArgumentException 当modelType不被支持时抛出异常
     */
    static ModelInfo createAzureOpenAiModel(LangChain4jModel.ModelType modelType, String baseUrl, String apiKey, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.AZURE_OPENAI);
        // 根据模型类型创建对应的Azure OpenAI模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(AzureOpenAiChatModel.builder()
                        .endpoint(baseUrl)
                        .apiKey(apiKey)
                        .deploymentName(modelName)
                        .build());
                break;
            case STREAMING_CHAT:
                modelInfo.setModel(AzureOpenAiStreamingChatModel.builder()
                        .endpoint(baseUrl)
                        .apiKey(apiKey)
                        .deploymentName(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(AzureOpenAiEmbeddingModel.builder()
                        .endpoint(baseUrl)
                        .apiKey(apiKey)
                        .deploymentName(modelName)
                        .build());
                break;
            case IMAGE:
                modelInfo.setModel(AzureOpenAiImageModel.builder()
                        .endpoint(baseUrl)
                        .apiKey(apiKey)
                        .deploymentName(modelName)
                        .build());
                break;
            case LANGUAGE:
                modelInfo.setModel(AzureOpenAiLanguageModel.builder()
                        .endpoint(baseUrl)
                        .apiKey(apiKey)
                        .deploymentName(modelName)
                        .build());
                break;
            case STREAMING_LANGUAGE:
                modelInfo.setModel(AzureOpenAiStreamingLanguageModel.builder()
                        .endpoint(baseUrl)
                        .apiKey(apiKey)
                        .deploymentName(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for Azure OpenAI: " + modelType);
        }
        return modelInfo;
    }



    /**
     * 创建Qwen模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的模型实例
     * @param baseUrl 模型服务的基础URL地址
     * @param apiKey 访问模型服务的API密钥
     * @param modelName 具体的模型名称
     * @param config 模型配置参数映射表（当前实现中未使用）
     * @return 返回对应类型的模型实例对象
     * @throws IllegalArgumentException 当modelType不支持时抛出异常
     */
    static ModelInfo createQwenModel(LangChain4jModel.ModelType modelType, String baseUrl, String apiKey, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.QWEN);
        // 根据模型类型创建对应的模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(QwenChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case STREAMING_CHAT:
                modelInfo.setModel(QwenStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(QwenEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case IMAGE:
                modelInfo.setModel(WanxImageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case LANGUAGE:
                modelInfo.setModel(QwenLanguageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case STREAMING_LANGUAGE:
                modelInfo.setModel(QwenStreamingLanguageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for Qwen: " + modelType);
        }
        return modelInfo;
    }


    /**
     * 创建HuggingFace模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的模型实例
     * @param apiKey 访问HuggingFace API的认证密钥
     * @param modelName 要加载的具体模型名称
     * @param config 模型配置参数映射表
     * @return 返回对应的HuggingFace模型实例对象
     */
    static ModelInfo createHuggingFaceModel(LangChain4jModel.ModelType modelType, String apiKey, String modelName,
                                            Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        // 根据模型类型创建对应的HuggingFace模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(HuggingFaceChatModel.builder()
                        .accessToken(apiKey)
                        .modelId(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(HuggingFaceEmbeddingModel.builder()
                        .accessToken(apiKey)
                        .modelId(modelName)
                        .build());
                break;
            case LANGUAGE:
                modelInfo.setModel(HuggingFaceLanguageModel.builder()
                        .accessToken(apiKey)
                        .modelId(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for HuggingFace: " + modelType);
        }
        return modelInfo;
    }



    /**
     * 根据模型类型创建对应的Ollama模型实例
     *
     * @param modelType 模型类型，决定创建聊天模型还是嵌入模型
     * @param baseUrl Ollama服务的基础URL地址
     * @param modelName 模型名称
     * @param config 模型配置参数映射表（当前实现中未使用）
     * @return 返回创建的Ollama模型实例，可能是OllamaChatModel或OllamaEmbeddingModel类型
     * @throws IllegalArgumentException 当modelType不支持时抛出异常
     */
    static ModelInfo createOllamaModel(LangChain4jModel.ModelType modelType, String baseUrl, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.OLLAMA);
        // 根据模型类型创建对应的Ollama模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(OllamaChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(OllamaEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build());
                break;
            case LANGUAGE:
                modelInfo.setModel(OllamaLanguageModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for Ollama: " + modelType);
        }
        return modelInfo;
    }


    /**
     * 根据模型类型创建对应的智谱AI模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的模型实例
     * @param baseUrl API基础URL地址
     * @param apiKey API密钥，用于身份验证
     * @param modelName 模型名称，指定具体使用的模型
     * @param config 额外的配置参数映射表
     * @return 返回对应类型的智谱AI模型实例
     * @throws IllegalArgumentException 当传入不支持的模型类型时抛出异常
     */
    static ModelInfo createZhipuModel(LangChain4jModel.ModelType modelType, String baseUrl, String apiKey, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.ZHIPU);
        // 根据不同的模型类型创建对应的模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(ZhipuAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(modelName)
                        .build());
                break;
            case STREAMING_CHAT:
                modelInfo.setModel(ZhipuAiStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(ZhipuAiEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(modelName)
                        .build());
                break;
            case IMAGE:
                modelInfo.setModel(ZhipuAiImageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .model(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for Zhipu: " + modelType);
        }
        return modelInfo;
    }

    /**
     * 创建千帆模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的千帆模型
     * @param baseUrl 模型服务的基础URL地址
     * @param apiKey API密钥，用于身份验证
     * @param modelName 模型名称，指定具体使用的模型
     * @param config 额外的配置参数映射表
     * @return 返回对应类型的千帆模型实例对象
     * @throws IllegalArgumentException 当不支持的模型类型时抛出异常
     */
    static ModelInfo createQianfanModel(LangChain4jModel.ModelType modelType, String baseUrl, String apiKey, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.QIANFAN);
        // 根据模型类型创建对应的千帆模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(QianfanChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .secretKey(apiKey) // 百度千帆使用apiKey和secretKey相同
                        .modelName(modelName)
                        .build());
                break;
            case STREAMING_CHAT:
                modelInfo.setModel(QianfanStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .secretKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(QianfanEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .secretKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case LANGUAGE:
                modelInfo.setModel(QianfanLanguageModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .secretKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for Qianfan: " + modelType);
        }
        return modelInfo;
    }



    /**
     * 创建本地AI模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的AI模型
     * @param baseUrl 模型服务的基础URL地址
     * @param modelName 模型名称
     * @param config 模型配置参数映射表
     * @return 返回对应的本地AI模型实例对象
     */
    static ModelInfo createLocalAiModel(LangChain4jModel.ModelType modelType, String baseUrl, String modelName,
                                        Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.LOCAL_AI);
        // 根据模型类型创建对应的本地AI模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(LocalAiChatModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(LocalAiEmbeddingModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build());
                break;
            case LANGUAGE:
                modelInfo.setModel(LocalAiLanguageModel.builder()
                        .baseUrl(baseUrl)
                        .modelName(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for LocalAI: " + modelType);
        }
        return modelInfo;
    }



    /**
     * 创建Gemini模型实例
     *
     * @param modelType 模型类型，决定创建聊天模型还是嵌入模型
     * @param apiKey API密钥，用于认证Gemini服务
     * @param modelName 模型名称，指定要使用的具体模型
     * @param config 额外的配置参数（当前未使用）
     * @return 返回对应的Gemini模型实例
     * @throws IllegalArgumentException 当modelType不支持时抛出异常
     */
    static ModelInfo createGeminiModel(LangChain4jModel.ModelType modelType, String apiKey, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.GEMINI);
        // 根据模型类型创建对应的Gemini模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(GoogleAiGeminiChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case EMBEDDING:
                modelInfo.setModel(GoogleAiEmbeddingModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case LANGUAGE:
                // Gemini 暂时不支持 Language Model，抛出异常
                throw new IllegalArgumentException("Language model not supported for Gemini");
            default:
                throw new IllegalArgumentException("Unsupported model type for Gemini: " + modelType);
        }
        return modelInfo;
    }

    /**
     * 创建Anthropic模型实例
     *
     * @param modelType 模型类型，决定创建哪种类型的Anthropic模型
     * @param baseUrl Anthropic API的基础URL地址
     * @param apiKey Anthropic API的访问密钥
     * @param modelName 具体的模型名称
     * @param config 额外的配置参数映射表
     * @return 返回对应类型的Anthropic模型实例
     * @throws IllegalArgumentException 当modelType不被支持时抛出异常
     */
    static ModelInfo createAnthropicModel(LangChain4jModel.ModelType modelType, String baseUrl, String apiKey, String modelName, Map<String, Object> config) {
        ModelInfo modelInfo = new ModelInfo();
        modelInfo.setProvider(LangChain4jModel.Provider.ANTHROPIC);
        // 根据模型类型创建对应的Anthropic模型实例
        switch (modelType) {
            case CHAT:
                modelInfo.setModel(AnthropicChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            case STREAMING_CHAT:
                modelInfo.setModel(AnthropicStreamingChatModel.builder()
                        .baseUrl(baseUrl)
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .build());
                break;
            default:
                throw new IllegalArgumentException("Unsupported model type for Anthropic: " + modelType);
        }
        return modelInfo;
    }

    

}
