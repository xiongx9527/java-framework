package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jDataSource;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@MagicModule("langchain4j")
public class Langchain4jModule implements DynamicAttribute<Langchain4jModule, Langchain4jModule>, DynamicModule<Langchain4jModule> {

    /**
     * Langchain4j模块类
     *
     * 该类封装了Langchain4j的数据源和模型映射，用于管理和访问不同的语言模型
     */

    private final MagicDynamicLangchain4jDataSource dataSource;

    /**
     * 模型映射表，用于存储模型名称与对应LangChain4jModel实例的映射关系
     * key: 模型名称
     * value: 对应的LangChain4jModel实例
     */
    private final Map<String, LangChain4jModel> modelMap;


    /**
     * 构造函数，初始化Langchain4j模块
     *
     * @param dataSource 动态数据源实例，用于提供模型访问的数据支持
     * @param modelMap 模型映射表，存储模型名称到LangChain4jModel实例的映射关系
     */
    public Langchain4jModule(MagicDynamicLangchain4jDataSource dataSource, Map<String, LangChain4jModel> modelMap) {
        this.dataSource = dataSource;
        this.modelMap = modelMap;
    }


    /**
     * 根据图像模型实例获取对应的提供商信息
     *
     * @param model 图像模型实例，用于识别其所属的提供商
     * @return 返回与图像模型对应的提供商枚举值，如果无法识别则返回null
     */
    private LangChain4jModel.Provider getProviderByImageModel(ImageModel model) {
        if (model == null) {
            return null;
        }
        String className = model.getClass().getName();
        // 根据模型的类名匹配对应的提供商
        switch (className) {
            case "dev.langchain4j.model.openai.OpenAiImageModel":
                return LangChain4jModel.Provider.OPENAI;
            case "dev.langchain4j.model.dashscope.WanxImageModel":
                return LangChain4jModel.Provider.QWEN;
            case "dev.langchain4j.model.zhipu.ZhipuAiImageModel":
                return LangChain4jModel.Provider.ZHIPU;
            case "dev.langchain4j.model.azure.AzureOpenAiImageModel":
                return LangChain4jModel.Provider.AZURE_OPENAI;
            default:
                return null;
        }
    }



        /**
     * 根据传入的 EmbeddingModel 实例，获取其对应的模型提供商（Provider）。
     *
     * @param model 输入的 EmbeddingModel 实例，用于判断其具体实现类
     * @return 返回与该模型实例对应的 LangChain4jModel.Provider 枚举值；
     *         如果模型为 null 或者未匹配到已知实现类，则返回 null
     */
    private LangChain4jModel.Provider getProviderByEmbeddingModel(EmbeddingModel model) {
        if (model == null) {
            return null;
        }
        String className = model.getClass().getName();
        switch (className) {
            case "dev.langchain4j.model.openai.OpenAiEmbeddingModel":
                return LangChain4jModel.Provider.OPENAI;
            case "dev.langchain4j.model.dashscope.QwenEmbeddingModel":
                return LangChain4jModel.Provider.QWEN;
            case "dev.langchain4j.model.huggingface.HuggingFaceEmbeddingModel":
                return LangChain4jModel.Provider.HUGGING_FACE;
            case "dev.langchain4j.model.ollama.OllamaEmbeddingModel":
                return LangChain4jModel.Provider.OLLAMA;
            case "dev.langchain4j.model.qianfan.QianfanEmbeddingModel":
                return LangChain4jModel.Provider.QIANFAN;
            case "dev.langchain4j.model.zhipu.ZhipuAiEmbeddingModel":
                return LangChain4jModel.Provider.ZHIPU;
            case "dev.langchain4j.model.localai.LocalAiEmbeddingModel":
                return LangChain4jModel.Provider.LOCAL_AI;
            case "dev.langchain4j.model.googleai.GoogleAiEmbeddingModel":
                return LangChain4jModel.Provider.GEMINI;
            case "dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel":
                return LangChain4jModel.Provider.AZURE_OPENAI;
            case "dev.langchain4j.model.embedding.onnx.allminilml6v2q.AllMiniLmL6V2QuantizedEmbeddingModel":
                return LangChain4jModel.Provider.ALL_MINI_LM_L6_V2_QUANTIZED;
            default:
                return null;
        }
    }


    /**
     * 根据传入的语言模型对象获取对应的提供商枚举值
     *
     * @param model 语言模型对象，用于识别其所属的AI服务提供商
     * @return 返回与语言模型对应的提供商枚举值，如果无法识别则返回null
     */
    private LangChain4jModel.Provider getProviderByLanguageModel(LanguageModel model) {
        if (model == null) {
            return null;
        }
        String className = model.getClass().getName();
        // 根据类名判断模型提供商并返回对应的枚举值
        switch (className) {
            case "dev.langchain4j.model.openai.OpenAiLanguageModel":
                return LangChain4jModel.Provider.OPENAI;
            case "dev.langchain4j.model.azure.AzureOpenAiLanguageModel":
                return LangChain4jModel.Provider.AZURE_OPENAI;
            case "dev.langchain4j.model.dashscope.QwenLanguageModel":
                return LangChain4jModel.Provider.QWEN;
            case "dev.langchain4j.model.huggingface.HuggingFaceLanguageModel":
                return LangChain4jModel.Provider.HUGGING_FACE;
            case "dev.langchain4j.model.ollama.OllamaLanguageModel":
                return LangChain4jModel.Provider.OLLAMA;
            case "dev.langchain4j.model.qianfan.QianfanLanguageModel":
                return LangChain4jModel.Provider.QIANFAN;
            case "dev.langchain4j.model.localai.LocalAiLanguageModel":
                return LangChain4jModel.Provider.LOCAL_AI;
            case "dev.langchain4j.model.zhipu.ZhipuAiChatModel":
                return LangChain4jModel.Provider.ZHIPU;
            default:
                return null;
        }
    }


    /**
     * 根据流式语言模型实例获取对应的提供商类型
     *
     * @param model 流式语言模型实例，用于识别其所属的AI服务提供商
     * @return 返回匹配的提供商枚举值，如果无法识别则返回null
     */
    private LangChain4jModel.Provider getProviderByStreamingLanguageModel(StreamingLanguageModel model) {
        if (model == null) {
            return null;
        }
        // 获取模型实例的完整类名用于识别提供商类型
        String className = model.getClass().getName();
        switch (className) {
            case "dev.langchain4j.model.openai.OpenAiStreamingLanguageModel":
                return LangChain4jModel.Provider.OPENAI;
            case "dev.langchain4j.model.dashscope.QwenStreamingLanguageModel":
                return LangChain4jModel.Provider.QWEN;
            case "dev.langchain4j.model.azure.AzureOpenAiStreamingLanguageModel":
                return LangChain4jModel.Provider.AZURE_OPENAI;
            default:
                return null;
        }
    }


    /**
     * 根据ChatLanguageModel实例获取对应的提供商枚举值
     *
     * @param model ChatLanguageModel实例，用于识别其具体的实现类
     * @return LangChain4jModel.Provider 对应的提供商枚举值，如果无法识别则返回null
     */
    private LangChain4jModel.Provider getProviderByChatLanguageModel(ChatLanguageModel model) {
        if (model == null) {
            return null;
        }
        String className = model.getClass().getName();
        // 根据类名匹配对应的提供商枚举值
        switch (className) {
            case "dev.langchain4j.model.openai.OpenAiChatModel":
                return LangChain4jModel.Provider.OPENAI;
            case "dev.langchain4j.model.azure.AzureOpenAiChatModel":
                return LangChain4jModel.Provider.AZURE_OPENAI;
            case "dev.langchain4j.model.dashscope.QwenChatModel":
                return LangChain4jModel.Provider.QWEN;
            case "dev.langchain4j.model.huggingface.HuggingFaceChatModel":
                return LangChain4jModel.Provider.HUGGING_FACE;
            case "dev.langchain4j.model.ollama.OllamaChatModel":
                return LangChain4jModel.Provider.OLLAMA;
            case "dev.langchain4j.model.zhipu.ZhipuAiChatModel":
                return LangChain4jModel.Provider.ZHIPU;
            case "dev.langchain4j.model.qianfan.QianfanChatModel":
                return LangChain4jModel.Provider.QIANFAN;
            case "dev.langchain4j.model.localai.LocalAiChatModel":
                return LangChain4jModel.Provider.LOCAL_AI;
            case "dev.langchain4j.model.googleai.GoogleAiGeminiChatModel":
                return LangChain4jModel.Provider.GEMINI;
            default:
                return null;
        }
    }


    /**
     * 根据流式聊天语言模型实例获取对应的提供商枚举值
     *
     * @param model 流式聊天语言模型实例，用于识别其具体的实现类
     * @return 返回与模型实例对应的提供商枚举值，如果无法识别则返回null
     */
    private LangChain4jModel.Provider getProviderByStreamingChatLanguageModel(StreamingChatLanguageModel model) {
        if (model == null) {
            return null;
        }
        // 获取模型实例的完整类名
        String className = model.getClass().getName();
        // 根据不同的类名返回对应的提供商枚举值
        switch (className) {
            case "dev.langchain4j.model.openai.OpenAiStreamingChatModel":
                return LangChain4jModel.Provider.OPENAI;
            case "dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel":
                return LangChain4jModel.Provider.AZURE_OPENAI;
            case "dev.langchain4j.model.dashscope.QwenStreamingChatModel":
                return LangChain4jModel.Provider.QWEN;
            case "dev.langchain4j.model.zhipu.ZhipuAiStreamingChatModel":
                return LangChain4jModel.Provider.ZHIPU;
            case "dev.langchain4j.model.qianfan.QianfanStreamingChatModel":
                return LangChain4jModel.Provider.QIANFAN;
            case "dev.langchain4j.model.localai.LocalAiStreamingChatModel":
                return LangChain4jModel.Provider.LOCAL_AI;
            default:
                return null;
        }
    }

    /**
     * 构造函数，用于初始化 Langchain4jModule 实例。
     * <p>
     * 该构造函数接收多个模型映射和数据源，并根据模型类型将其封装为统一的 LangChain4jModel 结构，
     * 存入内部的 modelMap 中，便于后续根据名称获取对应的模型实例。
     * </p>
     *
     * @param dataSource                     数据源，用于动态获取模型配置
     */
    public Langchain4jModule(MagicDynamicLangchain4jDataSource dataSource,
                             LangChain4jModelInfo langChain4jModelInfo) {
        this.dataSource = dataSource;
        this.modelMap = new ConcurrentHashMap<>();
        if (null == langChain4jModelInfo) {
            return  ;
        }
        // 处理图像模型映射，识别模型类型并封装为 LangChain4jImageModelModel
        if (MapUtils.isNotEmpty(langChain4jModelInfo.getImageModelMap())) {
            for (Map.Entry<String, ImageModel> entry : langChain4jModelInfo.getImageModelMap().entrySet()) {
                ImageModel imageModel = entry.getValue();
                LangChain4jModel.Provider provider = getProviderByImageModel(imageModel);
                modelMap.put(entry.getKey(), new LangChain4jModel.LangChain4jImageModelModel(entry.getValue(), provider));
            }
        }

        // 处理嵌入模型映射，识别模型类型并封装为 LangChain4jEmbeddingModel
        if (MapUtils.isNotEmpty(langChain4jModelInfo.getEmbeddingModelMap())) {
            for (Map.Entry<String, EmbeddingModel> entry : langChain4jModelInfo.getEmbeddingModelMap().entrySet()) {
                EmbeddingModel embeddingModel = entry.getValue();
                LangChain4jModel.Provider provider = getProviderByEmbeddingModel(embeddingModel);
                modelMap.put(entry.getKey(), new LangChain4jModel.LangChain4jEmbeddingModel(entry.getValue(), provider));
            }
        }

        // 处理语言模型映射，识别模型类型并封装为 LangChain4jGeneralLanguageModel
        if (MapUtils.isNotEmpty(langChain4jModelInfo.getLanguageModelMap())) {
            for (Map.Entry<String, LanguageModel> entry : langChain4jModelInfo.getLanguageModelMap().entrySet()) {
                LanguageModel languageModel = entry.getValue();
                LangChain4jModel.Provider provider = getProviderByLanguageModel(languageModel);
                modelMap.put(entry.getKey(), new LangChain4jModel.LangChain4jGeneralLanguageModel(entry.getValue(), provider));
            }
        }

        // 处理流式语言模型映射，识别模型类型并封装为 LangChain4jStreamingLanguageModel
        if (MapUtils.isNotEmpty(langChain4jModelInfo.getStreamingLanguageModelMap())) {
            for (Map.Entry<String, StreamingLanguageModel> entry : langChain4jModelInfo.getStreamingLanguageModelMap().entrySet()) {
                StreamingLanguageModel streamingLanguageModel = entry.getValue();
                LangChain4jModel.Provider provider = getProviderByStreamingLanguageModel(streamingLanguageModel);
                modelMap.put(entry.getKey(), new LangChain4jModel.LangChain4jStreamingLanguageModel(entry.getValue(), provider));
            }
        }

        // 处理聊天语言模型映射，识别模型类型并封装为 LangChain4jGeneralChatModelImpl
        if (MapUtils.isNotEmpty(langChain4jModelInfo.getChatLanguageModelMap())) {
            for (Map.Entry<String, ChatLanguageModel> entry : langChain4jModelInfo.getChatLanguageModelMap().entrySet()) {
                ChatLanguageModel chatLanguageModel = entry.getValue();
                LangChain4jModel.Provider provider = getProviderByChatLanguageModel(chatLanguageModel);
                modelMap.put(entry.getKey(), new LangChain4jModel.LangChain4jGeneralChatModelImpl(entry.getValue(), provider));
            }
        }

        // 处理流式聊天语言模型映射，识别模型类型并封装为 LangChain4jStreamingChatModel
        if (MapUtils.isNotEmpty(langChain4jModelInfo.getStreamingChatLanguageModelMap())) {
            for (Map.Entry<String, StreamingChatLanguageModel> entry : langChain4jModelInfo.getStreamingChatLanguageModelMap().entrySet()) {
                StreamingChatLanguageModel streamingChatLanguageModel = entry.getValue();
                LangChain4jModel.Provider provider = getProviderByStreamingChatLanguageModel(streamingChatLanguageModel);
                modelMap.put(entry.getKey(), new LangChain4jModel.LangChain4jStreamingChatModel(entry.getValue(), provider));
            }
        }

    }

    /**
     * 根据指定的键获取对应的LangChain4j模型实例
     *
     * @param key 用于标识模型的键值，作为在modelMap中查找模型的索引
     * @return 返回与给定键关联的LangChain4jModel实例，如果键不存在则返回null
     */
    public LangChain4jModel getModelObject(String key) {
        return this.modelMap.get(key);
    }

    /**
     * 根据指定的键获取模型对象
     *
     * @param key 用于查找模型对象的键值
     * @return 返回与指定键关联的模型对象，如果不存在则返回null
     */
    public Object getModel(String key) {
        return this.getModelObject(key).getModel();
    }


    /**
     * 获取动态模块实例
     *
     * @param context 魔法脚本上下文，用于获取数据源键值
     * @return 返回Langchain4jModule实例，如果数据源键值为空则返回当前实例，否则返回新的模块实例
     */
    @Override
    public Langchain4jModule getDynamicModule(MagicScriptContext context) {
        // 获取数据源键值
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        // 如果数据源键值为空，返回当前模块实例
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        // 根据数据源键值创建新的模块实例
        return new Langchain4jModule(this.dataSource, this.dataSource.getModel(dataSourceKey));
    }


    /**
     * 根据指定的键获取动态属性对应的Langchain4j模块实例
     *
     * @param key 用于获取模型的键值
     * @return 返回一个新的Langchain4jModule实例，该实例使用当前数据源和根据键获取的模型进行初始化
     */
    @Override
    public Langchain4jModule getDynamicAttribute(String key) {
        // 创建并返回新的Langchain4jModule实例，使用当前数据源和根据键获取的模型
        return new Langchain4jModule(this.dataSource, this.dataSource.getModel(key));
    }



}
