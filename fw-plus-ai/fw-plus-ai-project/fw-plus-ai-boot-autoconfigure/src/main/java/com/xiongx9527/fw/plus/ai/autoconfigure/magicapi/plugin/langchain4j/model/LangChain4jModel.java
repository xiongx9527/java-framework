package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;

public interface LangChain4jModel {

    /**
     * 获取模型类型
     *
     * @return ModelType 模型类型枚举值
     */
    ModelType getModelType();

    /**
     * 获取提供者
     *
     * @return Provider 提供者对象
     */
    Provider getProvider();

    /**
     * 获取模型对象
     *
     * @return Object 模型对象实例
     */
    Object getModel();

    /**
     * 聊天模型
     */
    public static interface LangChain4jChatModel extends LangChain4jModel {

    }


    /**
     * LangChain4jStreamingChatModel类实现了LangChain4jChatModel接口，
     * 用于包装流式聊天语言模型并提供统一的访问接口。
     */
    public static class LangChain4jStreamingChatModel implements LangChain4jChatModel {
        /**
         * StreamingChatLanguageModel类型的模型实例，用于处理流式聊天语言模型的调用
         */
        private final StreamingChatLanguageModel model;

        /**
         * Provider类型的提供者实例，用于提供相关的服务或资源配置
         */
        private final Provider provider;

        /**
         * 构造函数，初始化流式聊天模型和提供者
         * @param model 流式聊天语言模型实例
         * @param provider 模型提供者信息
         */
        public LangChain4jStreamingChatModel(StreamingChatLanguageModel model, Provider provider) {
            this.model = model;
            this.provider = provider;
        }

        /**
         * 获取模型类型
         * @return 返回STREAMING_CHAT类型的枚举值
         */
        @Override
        public ModelType getModelType() {
            return ModelType.STREAMING_CHAT;
        }

        /**
         * 获取模型提供者
         * @return 返回模型提供者信息
         */
        @Override
        public Provider getProvider() {
            return provider;
        }

        /**
         * 获取底层模型对象
         * @return 返回流式聊天语言模型实例
         */
        @Override
        public Object getModel() {
            return model;
        }
    }


    /**
     * LangChain4jGeneralChatModelImpl类实现了LangChain4jChatModel接口，
     * 用于封装通用的聊天语言模型实现。
     * 该类持有具体的ChatLanguageModel实例和对应的Provider信息。
     */
    public static class LangChain4jGeneralChatModelImpl implements LangChain4jChatModel {
        /**
         * 聊天语言模型实例
         * 用于处理聊天相关的语言模型操作
         */
        private final ChatLanguageModel model;

        /**
         * 提供者实例
         * 用于提供相关的服务或数据源
         */
        private final Provider provider;

        /**
         * 构造函数，初始化聊天语言模型和提供者
         * @param model ChatLanguageModel实例，具体的聊天语言模型实现
         * @param provider Provider实例，模型提供者信息
         */
        public LangChain4jGeneralChatModelImpl(ChatLanguageModel model, Provider provider) {
            this.model = model;
            this.provider = provider;
        }

        /**
         * 获取模型类型
         * @return ModelType 返回CHAT类型，表示这是一个聊天模型
         */
        @Override
        public ModelType getModelType() {
            return ModelType.CHAT;
        }

        /**
         * 获取模型提供者
         * @return Provider 返回模型的提供者信息
         */
        @Override
        public Provider getProvider() {
            return provider;
        }

        /**
         * 获取具体的模型实例
         * @return Object 返回封装的ChatLanguageModel实例
         */
        @Override
        public Object getModel() {
            return model;
        }
    }


    /**
     * 文本模型
     */
    public static interface LangChain4jLanguageModel extends LangChain4jModel {

    }

    /**
     * LangChain4jStreamingLanguageModel类实现了LangChain4jLanguageModel接口，
     * 用于包装流式语言模型并提供统一的访问接口。
     */
    public static class LangChain4jStreamingLanguageModel implements LangChain4jLanguageModel {
        /**
         * StreamingLanguageModel类型的模型实例，用于处理流式语言模型相关的操作
         */
        private final StreamingLanguageModel model;

        /**
         * Provider类型的提供者实例，用于提供相关服务或数据的获取
         */
        private final Provider provider;

        /**
         * 构造函数，初始化流式语言模型和提供商信息
         * @param model 流式语言模型实例
         * @param provider 模型提供商信息
         */
        public LangChain4jStreamingLanguageModel(StreamingLanguageModel model, Provider provider) {
            this.model = model;
            this.provider = provider;
        }

        /**
         * 获取模型类型
         * @return 返回STREAMING_LANGUAGE类型的枚举值
         */
        @Override
        public ModelType getModelType() {
            return ModelType.STREAMING_LANGUAGE;
        }

        /**
         * 获取模型提供商信息
         * @return 返回模型提供商对象
         */
        @Override
        public Provider getProvider() {
            return provider;
        }

        /**
         * 获取底层模型对象
         * @return 返回流式语言模型实例
         */
        @Override
        public Object getModel() {
            return model;
        }
    }


    /**
     * LangChain4jGeneralLanguageModel类实现了LangChain4jLanguageModel接口，
     * 用于封装通用的语言模型实现，提供模型类型、提供商和具体模型实例的访问接口。
     */
    public static class LangChain4jGeneralLanguageModel implements LangChain4jLanguageModel {
        /**
         * 语言模型实例，用于处理自然语言相关的功能
         */
        private final LanguageModel model;

        /**
         * 提供者实例，用于提供特定的服务或数据源
         */
        private final Provider provider;

        /**
         * 构造函数，初始化语言模型和提供商
         * @param model 语言模型实例
         * @param provider 模型提供商
         */
        public LangChain4jGeneralLanguageModel(LanguageModel model, Provider provider) {
            this.model = model;
            this.provider = provider;
        }

        /**
         * 获取模型类型
         * @return 返回LANGUAGE模型类型
         */
        @Override
        public ModelType getModelType() {
            return ModelType.LANGUAGE;
        }

        /**
         * 获取模型提供商
         * @return 返回当前模型的提供商
         */
        @Override
        public Provider getProvider() {
            return provider;
        }

        /**
         * 获取具体的语言模型实例
         * @return 返回封装的语言模型对象
         */
        @Override
        public Object getModel() {
            return model;
        }
    }


    /**
     * LangChain4jEmbeddingModel类实现了LangChain4jModel接口，用于封装嵌入模型的相关信息
     * 该类提供了获取模型类型、提供商和实际模型对象的功能
     */
    public static class LangChain4jEmbeddingModel implements LangChain4jModel {
        /**
         * 嵌入模型实例，用于生成文本的向量表示
         */
        private final EmbeddingModel model;

        /**
         * 提供者实例，用于管理模型的提供方信息
         */
        private final Provider provider;

        /**
         * 构造函数，初始化嵌入模型和提供商
         * @param model 嵌入模型对象，用于实际的嵌入计算
         * @param provider 模型提供商，标识模型的来源
         */
        public LangChain4jEmbeddingModel(EmbeddingModel model, Provider provider) {
            this.model = model;
            this.provider = provider;
        }

        /**
         * 获取模型类型
         * @return 返回模型类型枚举值，对于嵌入模型固定返回ModelType.EMBEDDING
         */
        @Override
        public ModelType getModelType() {
            return ModelType.EMBEDDING;
        }

        /**
         * 获取模型提供商
         * @return 返回模型的提供商信息
         */
        @Override
        public Provider getProvider() {
            return provider;
        }

        /**
         * 获取实际的模型对象
         * @return 返回封装的嵌入模型对象
         */
        @Override
        public Object getModel() {
            return model;
        }
    }


    /**
     * LangChain4jImageModelModel类用于封装图像模型的实现
     * 该类实现了LangChain4jModel接口，专门处理图像模型相关的操作
     */
    public static class LangChain4jImageModelModel implements LangChain4jModel {
        /**
         * 图片模型实例，用于存储和管理图片数据
         */
        private final ImageModel model;

        /**
         * 提供者实例，用于提供图片相关的服务和功能
         */
        private final Provider provider;

        /**
         * 构造函数，用于创建LangChain4jImageModelModel实例
         * @param model 图像模型对象，用于处理图像相关的AI操作
         * @param provider 提供商对象，表示模型的来源提供方
         */
        public LangChain4jImageModelModel(ImageModel model, Provider provider) {
            this.model = model;
            this.provider = provider;
        }

        /**
         * 获取模型类型
         * @return ModelType 返回图像模型类型标识
         */
        @Override
        public ModelType getModelType() {
            return ModelType.IMAGE;
        }

        /**
         * 获取模型提供商
         * @return Provider 返回模型提供商对象
         */
        @Override
        public Provider getProvider() {
            return provider;
        }

        /**
         * 获取底层模型对象
         * @return Object 返回封装的图像模型实例
         */
        @Override
        public Object getModel() {
            return model;
        }
    }


    /**
     * 模型类型枚举，定义了不同类型的AI模型
     * CHAT: 聊天模型
     * STREAMING_CHAT: 流式聊天模型
     * LANGUAGE: 语言模型
     * STREAMING_LANGUAGE: 流式语言模型
     * EMBEDDING: 嵌入模型
     * IMAGE: 图像模型
     */
    public static enum ModelType {
        CHAT, STREAMING_CHAT, LANGUAGE, STREAMING_LANGUAGE, EMBEDDING, IMAGE
    }

    /**
     * 提供商枚举，定义了不同的AI服务提供商
     * OPENAI: OpenAI提供商
     * QWEN: 通义千问提供商
     * HUGGING_FACE: Hugging Face提供商
     * OLLAMA: Ollama提供商
     * ZHIPU: 智谱提供商
     * QIANFAN: 千帆提供商
     * LOCAL_AI: 本地AI提供商
     * GEMINI: Gemini提供商
     * AZURE_OPENAI: Azure OpenAI提供商
     * ANTHROPIC: Anthropic提供商
     * ALL_MINI_LM_L6_V2_QUANTIZED: All Mini LM L6 V2量化模型提供商
     */
    public static enum Provider {
        OPENAI, QWEN, HUGGING_FACE, OLLAMA, ZHIPU, QIANFAN, LOCAL_AI, GEMINI, AZURE_OPENAI, ANTHROPIC , ALL_MINI_LM_L6_V2_QUANTIZED
    }






}
