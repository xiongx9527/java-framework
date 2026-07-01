package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.image.ImageModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;

public interface LangChain4jModel {

    ModelType getModelType();

    Provider getProvider();

    Object getModel();

    // ============ 抽象基类，消除6个子类的重复代码 ============

    abstract class AbstractModelWrapper<M> implements LangChain4jModel {
        protected final M model;
        protected final Provider provider;

        public AbstractModelWrapper(M model, Provider provider) {
            this.model = model;
            this.provider = provider;
        }

        @Override
        public Provider getProvider() { return provider; }

        @Override
        public Object getModel() { return model; }
    }

    // ============ 聊天模型 ============

    interface LangChain4jChatModel extends LangChain4jModel {
    }

    class LangChain4jGeneralChatModelImpl extends AbstractModelWrapper<ChatLanguageModel> implements LangChain4jChatModel {
        public LangChain4jGeneralChatModelImpl(ChatLanguageModel model, Provider provider) { super(model, provider); }
        @Override public ModelType getModelType() { return ModelType.CHAT; }
    }

    class LangChain4jStreamingChatModel extends AbstractModelWrapper<StreamingChatLanguageModel> implements LangChain4jChatModel {
        public LangChain4jStreamingChatModel(StreamingChatLanguageModel model, Provider provider) { super(model, provider); }
        @Override public ModelType getModelType() { return ModelType.STREAMING_CHAT; }
    }

    // ============ 语言模型 ============

    interface LangChain4jLanguageModel extends LangChain4jModel {
    }

    class LangChain4jGeneralLanguageModel extends AbstractModelWrapper<LanguageModel> implements LangChain4jLanguageModel {
        public LangChain4jGeneralLanguageModel(LanguageModel model, Provider provider) { super(model, provider); }
        @Override public ModelType getModelType() { return ModelType.LANGUAGE; }
    }

    class LangChain4jStreamingLanguageModel extends AbstractModelWrapper<StreamingLanguageModel> implements LangChain4jLanguageModel {
        public LangChain4jStreamingLanguageModel(StreamingLanguageModel model, Provider provider) { super(model, provider); }
        @Override public ModelType getModelType() { return ModelType.STREAMING_LANGUAGE; }
    }

    // ============ 嵌入模型 ============

    class LangChain4jEmbeddingModel extends AbstractModelWrapper<EmbeddingModel> implements LangChain4jModel {
        public LangChain4jEmbeddingModel(EmbeddingModel model, Provider provider) { super(model, provider); }
        @Override public ModelType getModelType() { return ModelType.EMBEDDING; }
    }

    // ============ 图像模型 ============

    class LangChain4jImageModelModel extends AbstractModelWrapper<ImageModel> implements LangChain4jModel {
        public LangChain4jImageModelModel(ImageModel model, Provider provider) { super(model, provider); }
        @Override public ModelType getModelType() { return ModelType.IMAGE; }
    }

    // ============ 枚举 ============

    enum ModelType {
        CHAT, STREAMING_CHAT, LANGUAGE, STREAMING_LANGUAGE, EMBEDDING, IMAGE
    }

    enum Provider {
        OPENAI, QWEN, HUGGING_FACE, OLLAMA, ZHIPU, QIANFAN, LOCAL_AI, GEMINI, AZURE_OPENAI, ANTHROPIC, ALL_MINI_LM_L6_V2_QUANTIZED
    }

}
