package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.core;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.AbstractMagicDynamicLangchain4jDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingChatModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jStreamingLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.moderation.ModerationModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.ToolProvider;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.springframework.beans.factory.DisposableBean;
import org.ssssssss.magicapi.core.annotation.MagicModule;

import java.util.TreeSet;
import java.util.function.Function;

/**
 * magic-api 脚本侧的 LangChain4j 模块：只按 {@code key} 读用已配置的实例，不再提供编程式构建。
 *
 * <p>实例的唯一来源是 datasource 插件按数据源生命周期创建/销毁并持有的实例；本模块只做只读委托，
 * 取不到时抛出中文异常并列出该类型已配置的 {@code key}。</p>
 */
@MagicModule("langchain4j")
public class Langchain4jModule implements DisposableBean {

    private final MagicDynamicLangchain4jChatModelDataSource chatModels;
    private final MagicDynamicLangchain4jStreamingChatModelDataSource streamingChatModels;
    private final MagicDynamicLangchain4jLanguageModelDataSource languageModels;
    private final MagicDynamicLangchain4jStreamingLanguageModelDataSource streamingLanguageModels;
    private final MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;
    private final MagicDynamicLangchain4jTokenizerDataSource tokenizers;
    private final MagicDynamicLangchain4jEmbeddingStoreDataSource embeddingStores;

    public Langchain4jModule(MagicDynamicLangchain4jChatModelDataSource chatModels,
            MagicDynamicLangchain4jStreamingChatModelDataSource streamingChatModels,
            MagicDynamicLangchain4jLanguageModelDataSource languageModels,
            MagicDynamicLangchain4jStreamingLanguageModelDataSource streamingLanguageModels,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels,
            MagicDynamicLangchain4jTokenizerDataSource tokenizers,
            MagicDynamicLangchain4jEmbeddingStoreDataSource embeddingStores) {
        this.chatModels = chatModels;
        this.streamingChatModels = streamingChatModels;
        this.languageModels = languageModels;
        this.streamingLanguageModels = streamingLanguageModels;
        this.embeddingModels = embeddingModels;
        this.tokenizers = tokenizers;
        this.embeddingStores = embeddingStores;
    }

    /** {@code langchain4j-chat-model} 数据源实例。 */
    public ChatLanguageModel getChatLanguageModel(String key) {
        return required(chatModels, Langchain4jModelTypes.CHAT_MODEL, key);
    }

    /** {@code langchain4j-streaming-chat-model} 数据源实例。 */
    public StreamingChatLanguageModel getStreamingChatLanguageModel(String key) {
        return required(streamingChatModels, Langchain4jModelTypes.STREAMING_CHAT_MODEL, key);
    }

    /** {@code langchain4j-language-model} 数据源实例。 */
    public LanguageModel getLanguageModel(String key) {
        return required(languageModels, Langchain4jModelTypes.LANGUAGE_MODEL, key);
    }

    /** {@code langchain4j-streaming-language-model} 数据源实例。 */
    public StreamingLanguageModel getStreamingLanguageModel(String key) {
        return required(streamingLanguageModels, Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL, key);
    }

    /** {@code langchain4j-embedding-model} 数据源实例。 */
    public EmbeddingModel getEmbeddingModel(String key) {
        return required(embeddingModels, Langchain4jModelTypes.EMBEDDING_MODEL, key);
    }

    /** {@code langchain4j-tokenizer} 数据源实例。 */
    public Tokenizer getTokenizer(String key) {
        return required(tokenizers, Langchain4jModelTypes.TOKENIZER, key);
    }

    /** {@code langchain4j-embedding-store} 数据源实例。 */
    public EmbeddingStore<TextSegment> getEmbeddingStore(String key) {
        return required(embeddingStores, Langchain4jModelTypes.EMBEDDING_STORE, key);
    }

    /**
     * 从运行时可用集合中驱逐一个实例（不删除已保存的数据源配置）。
     *
     * @param type 数据源类型，如 {@code langchain4j-chat-model}
     * @param key  数据源 key
     */
    public void remove(String type, String key) {
        Langchain4jModelTypes modelType = Langchain4jModelTypes.of(type);
        switch (modelType) {
            case CHAT_MODEL:
                chatModels.evict(key);
                break;
            case STREAMING_CHAT_MODEL:
                streamingChatModels.evict(key);
                break;
            case LANGUAGE_MODEL:
                languageModels.evict(key);
                break;
            case STREAMING_LANGUAGE_MODEL:
                streamingLanguageModels.evict(key);
                break;
            case EMBEDDING_MODEL:
                embeddingModels.evict(key);
                break;
            case TOKENIZER:
                tokenizers.evict(key);
                break;
            case EMBEDDING_STORE:
                embeddingStores.evict(key);
                break;
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    /**
     * 按需组装 AiServices：只把非空组件装配进去，未提供的组件保持 LangChain4j 默认行为。
     */
    public <T> T chat(Class<T> assistanClass,
                      ChatLanguageModel chatLanguageModel,
                      StreamingChatLanguageModel streamingChatLanguageModel,
                      Function<Object, String> systemMessageProvider,
                      ChatMemoryProvider chatMemoryProvider,
                      ToolProvider toolProvider,
                      ContentRetriever contentRetriever,
                      ModerationModel moderationModel) {
        AiServices<T> aiServices = AiServices.builder(assistanClass);
        if (chatLanguageModel != null) {
            aiServices = aiServices.chatLanguageModel(chatLanguageModel);
        }
        if (streamingChatLanguageModel != null) {
            aiServices = aiServices.streamingChatLanguageModel(streamingChatLanguageModel);
        }
        if (systemMessageProvider != null) {
            aiServices = aiServices.systemMessageProvider(systemMessageProvider);
        }
        if (chatMemoryProvider != null) {
            aiServices = aiServices.chatMemoryProvider(chatMemoryProvider);
        }
        if (toolProvider != null) {
            aiServices = aiServices.toolProvider(toolProvider);
        }
        if (contentRetriever != null) {
            aiServices = aiServices.contentRetriever(contentRetriever);
        }
        if (moderationModel != null) {
            aiServices = aiServices.moderationModel(moderationModel);
        }

        return aiServices.build();
    }

    /**
     * 组装默认 {@link Assistant} 接口的 AiServices 实例。
     */
    public Assistant chat(ChatLanguageModel chatLanguageModel,
                          StreamingChatLanguageModel streamingChatLanguageModel,
                          Function<Object, String> systemMessageProvider,
                          ChatMemoryProvider chatMemoryProvider,
                          ToolProvider toolProvider,
                          ContentRetriever contentRetriever,
                          ModerationModel moderationModel) {
        return chat(Assistant.class,
                chatLanguageModel,
                streamingChatLanguageModel,
                systemMessageProvider,
                chatMemoryProvider,
                toolProvider,
                contentRetriever,
                moderationModel);
    }

    /** 应用关闭时清空全部持有者，关闭可关闭的实例。 */
    @Override
    public void destroy() throws Exception {
        chatModels.destroy();
        streamingChatModels.destroy();
        languageModels.destroy();
        streamingLanguageModels.destroy();
        embeddingModels.destroy();
        tokenizers.destroy();
        embeddingStores.destroy();
    }

    private <M> M required(AbstractMagicDynamicLangchain4jDataSource<M> holder, Langchain4jModelTypes type,
            String key) {
        M instance = holder.get(key);
        if (instance == null) {
            throw new IllegalStateException(String.format(
                    "LangChain4j 资源未配置: type=%s, key='%s'，当前已配置 key=%s",
                    type.type(), key, new TreeSet<>(holder.keys())));
        }
        return instance;
    }

    /** 脚本侧对话接口：同步返回完整回复，流式逐 token 回调。 */
    public interface Assistant {

        /**
         * 同步对话：返回完整回复。
         *
         * @param sessionId 会话标识，透传给系统提示 provider；接入 ChatMemory 时同时作为记忆键
         * @param message   用户消息
         */
        String chat(@MemoryId String sessionId, String message);

        /**
         * 流式对话：逐 token 回调。
         *
         * @param sessionId 会话标识，透传给系统提示 provider；接入 ChatMemory 时同时作为记忆键
         * @param message   用户消息
         */
        TokenStream stream(@MemoryId String sessionId, String message);
    }
}
