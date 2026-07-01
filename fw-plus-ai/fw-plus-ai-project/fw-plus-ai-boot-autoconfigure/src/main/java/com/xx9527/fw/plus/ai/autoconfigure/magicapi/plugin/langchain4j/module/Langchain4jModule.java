package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModel;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jDataSource;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;
import reactor.core.publisher.Flux;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@MagicModule("langchain4j")
public class Langchain4jModule implements DynamicAttribute<Langchain4jModule, Langchain4jModule>, DynamicModule<Langchain4jModule> {

    private final MagicDynamicLangchain4jDataSource dataSource;
    private final Map<String, LangChain4jModel> modelMap;

    // ============ 类名到 Provider 的统一映射表 ============

    private static final Map<String, LangChain4jModel.Provider> CLASS_NAME_PROVIDER_MAP = buildProviderMap();

    private static Map<String, LangChain4jModel.Provider> buildProviderMap() {
        Map<String, LangChain4jModel.Provider> map = new HashMap<>();
        // Chat
        map.put("dev.langchain4j.model.openai.OpenAiChatModel", LangChain4jModel.Provider.OPENAI);
        map.put("dev.langchain4j.model.azure.AzureOpenAiChatModel", LangChain4jModel.Provider.AZURE_OPENAI);
        map.put("dev.langchain4j.model.dashscope.QwenChatModel", LangChain4jModel.Provider.QWEN);
        map.put("dev.langchain4j.model.huggingface.HuggingFaceChatModel", LangChain4jModel.Provider.HUGGING_FACE);
        map.put("dev.langchain4j.model.ollama.OllamaChatModel", LangChain4jModel.Provider.OLLAMA);
        map.put("dev.langchain4j.model.zhipu.ZhipuAiChatModel", LangChain4jModel.Provider.ZHIPU);
        map.put("dev.langchain4j.model.qianfan.QianfanChatModel", LangChain4jModel.Provider.QIANFAN);
        map.put("dev.langchain4j.model.localai.LocalAiChatModel", LangChain4jModel.Provider.LOCAL_AI);
        map.put("dev.langchain4j.model.googleai.GoogleAiGeminiChatModel", LangChain4jModel.Provider.GEMINI);
        // Streaming Chat
        map.put("dev.langchain4j.model.openai.OpenAiStreamingChatModel", LangChain4jModel.Provider.OPENAI);
        map.put("dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel", LangChain4jModel.Provider.AZURE_OPENAI);
        map.put("dev.langchain4j.model.dashscope.QwenStreamingChatModel", LangChain4jModel.Provider.QWEN);
        map.put("dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel", LangChain4jModel.Provider.ZHIPU);
        map.put("dev.langchain4j.model.qianfan.QianfanStreamingChatModel", LangChain4jModel.Provider.QIANFAN);
        map.put("dev.langchain4j.model.localai.LocalAiStreamingChatModel", LangChain4jModel.Provider.LOCAL_AI);
        // Embedding
        map.put("dev.langchain4j.model.openai.OpenAiEmbeddingModel", LangChain4jModel.Provider.OPENAI);
        map.put("dev.langchain4j.model.dashscope.QwenEmbeddingModel", LangChain4jModel.Provider.QWEN);
        map.put("dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel", LangChain4jModel.Provider.HUGGING_FACE);
        map.put("dev.langchain4j.model.ollama.OllamaEmbeddingModel", LangChain4jModel.Provider.OLLAMA);
        map.put("dev.langchain4j.model.qianfan.QianfanEmbeddingModel", LangChain4jModel.Provider.QIANFAN);
        map.put("dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel", LangChain4jModel.Provider.ZHIPU);
        map.put("dev.langchain4j.model.localai.LocalAiEmbeddingModel", LangChain4jModel.Provider.LOCAL_AI);
        map.put("dev.langchain4j.model.googleai.GoogleAiEmbeddingModel", LangChain4jModel.Provider.GEMINI);
        map.put("dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel", LangChain4jModel.Provider.AZURE_OPENAI);
        map.put("dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel", LangChain4jModel.Provider.ALL_MINI_LM_L6_V2_QUANTIZED);
        // Language
        map.put("dev.langchain4j.model.openai.OpenAiLanguageModel", LangChain4jModel.Provider.OPENAI);
        map.put("dev.langchain4j.model.azure.AzureOpenAiLanguageModel", LangChain4jModel.Provider.AZURE_OPENAI);
        map.put("dev.langchain4j.model.dashscope.QwenLanguageModel", LangChain4jModel.Provider.QWEN);
        map.put("dev.langchain4j.model.huggingface.HuggingFaceLanguageModel", LangChain4jModel.Provider.HUGGING_FACE);
        map.put("dev.langchain4j.model.ollama.OllamaLanguageModel", LangChain4jModel.Provider.OLLAMA);
        map.put("dev.langchain4j.model.qianfan.QianfanLanguageModel", LangChain4jModel.Provider.QIANFAN);
        map.put("dev.langchain4j.model.localai.LocalAiLanguageModel", LangChain4jModel.Provider.LOCAL_AI);
        // Streaming Language
        map.put("dev.langchain4j.model.openai.OpenAiStreamingLanguageModel", LangChain4jModel.Provider.OPENAI);
        map.put("dev.langchain4j.model.dashscope.QwenStreamingLanguageModel", LangChain4jModel.Provider.QWEN);
        map.put("dev.langchain4j.model.azure.AzureOpenAiStreamingLanguageModel", LangChain4jModel.Provider.AZURE_OPENAI);
        // Image
        map.put("dev.langchain4j.model.openai.OpenAiImageModel", LangChain4jModel.Provider.OPENAI);
        map.put("dev.langchain4j.model.dashscope.WanxImageModel", LangChain4jModel.Provider.QWEN);
        map.put("dev.langchain4j.model.zhipu.ZhipuAiImageModel", LangChain4jModel.Provider.ZHIPU);
        map.put("dev.langchain4j.model.azure.AzureOpenAiImageModel", LangChain4jModel.Provider.AZURE_OPENAI);
        return map;
    }

    private static LangChain4jModel.Provider resolveProvider(Object model) {
        if (model == null) return null;
        return CLASS_NAME_PROVIDER_MAP.get(model.getClass().getName());
    }

    // ============ 构造函数 ============

    public Langchain4jModule(MagicDynamicLangchain4jDataSource dataSource, Map<String, LangChain4jModel> modelMap) {
        this.dataSource = dataSource;
        this.modelMap = modelMap;
    }

    public Langchain4jModule(MagicDynamicLangchain4jDataSource dataSource, LangChain4jModelInfo langChain4jModelInfo) {
        this.dataSource = dataSource;
        this.modelMap = new ConcurrentHashMap<>();
        if (null == langChain4jModelInfo) {
            return;
        }
        registerModels(langChain4jModelInfo.getImageModelMap(), LangChain4jModel.LangChain4jImageModelModel::new);
        registerModels(langChain4jModelInfo.getEmbeddingModelMap(), LangChain4jModel.LangChain4jEmbeddingModel::new);
        registerModels(langChain4jModelInfo.getLanguageModelMap(), LangChain4jModel.LangChain4jGeneralLanguageModel::new);
        registerModels(langChain4jModelInfo.getStreamingLanguageModelMap(), LangChain4jModel.LangChain4jStreamingLanguageModel::new);
        registerModels(langChain4jModelInfo.getChatLanguageModelMap(), LangChain4jModel.LangChain4jGeneralChatModelImpl::new);
        registerModels(langChain4jModelInfo.getStreamingChatLanguageModelMap(), LangChain4jModel.LangChain4jStreamingChatModel::new);
    }

    private <M> void registerModels(Map<String, M> modelSource, ModelWrapper<M> wrapper) {
        if (MapUtils.isNotEmpty(modelSource)) {
            modelSource.forEach((key, model) -> modelMap.put(key, wrapper.wrap(model, resolveProvider(model))));
        }
    }

    private interface ModelWrapper<M> {
        LangChain4jModel wrap(M model, LangChain4jModel.Provider provider);
    }

    // ============ 公共方法 ============

    public LangChain4jModel getModelObject(String key) {
        return this.modelMap.get(key);
    }

    public Object getModel(String key) {
        return this.getModelObject(key).getModel();
    }

    public AiServices<ChatAssistant> builderAiServices() {
        return AiServices.builder(ChatAssistant.class);
    }

    @Override
    public Langchain4jModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        return new Langchain4jModule(this.dataSource, this.dataSource.getModel(dataSourceKey));
    }

    @Override
    public Langchain4jModule getDynamicAttribute(String key) {
        return new Langchain4jModule(this.dataSource, this.dataSource.getModel(key));
    }

    // ============ ChatAssistant 接口 ============

    public static interface ChatAssistant {

        @SystemMessage("{{systemMessage}}")
        @UserMessage("{{userMessage}}")
        String chat(String systemMessage, String userMessage);

        @SystemMessage("{{systemMessage}}")
        @UserMessage("{{userMessage}}")
        String chatByMemory(@MemoryId String memoryId, String systemMessage, String userMessage);

        @SystemMessage("{{systemMessage}}")
        @UserMessage("{{userMessage}}")
        Flux<String> streamingChat(String systemMessage, String userMessage);

        @SystemMessage("{{systemMessage}}")
        @UserMessage("{{userMessage}}")
        Flux<String> streamingChatByMemory(@MemoryId String memoryId, String systemMessage, String userMessage);
    }

}
