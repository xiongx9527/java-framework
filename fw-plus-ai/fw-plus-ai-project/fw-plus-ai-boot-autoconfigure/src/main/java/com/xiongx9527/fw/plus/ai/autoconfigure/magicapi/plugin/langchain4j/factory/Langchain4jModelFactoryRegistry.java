package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jScoringModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingChatModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jStreamingLanguageModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jTokenizerInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.TokenCountEstimator;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * provider 短名 → 工厂的分发表：每类资源一张短名集合，表外短名构建期拒绝。
 * chat-model / streaming-chat-model 构建时把全局 {@link ChatModelListener} 一并下发给 provider。
 */
public class Langchain4jModelFactoryRegistry {

    private static final Langchain4jOptionsReader PROBE =
            new Langchain4jOptionsReader("probe", "probe", Collections.<String, Object>emptyMap());

    private final Map<String, ModelFactory> factories = new LinkedHashMap<String, ModelFactory>();

    /** 全局 ChatModelListener：构建 chat-model / streaming-chat-model 时统一交给 provider */
    private final List<ChatModelListener> listeners;

    public Langchain4jModelFactoryRegistry() {
        this(Collections.<ChatModelListener>emptyList());
    }

    public Langchain4jModelFactoryRegistry(List<ChatModelListener> listeners) {
        this.listeners = listeners == null ? Collections.<ChatModelListener>emptyList() : listeners;
    }
    public void register(ModelFactory factory) {
        factories.put(factory.provider(), factory);
    }

    public boolean supports(Langchain4jModelTypes type, String provider) {
        ModelFactory factory = factories.get(provider);
        return factory != null && factorySupports(type, factory);
    }

    public Set<String> providers(Langchain4jModelTypes type) {
        Set<String> result = new LinkedHashSet<String>();
        for (ModelFactory factory : factories.values()) {
            if (factorySupports(type, factory)) {
                result.add(factory.provider());
            }
        }
        return result;
    }

    public Object build(Langchain4jModelTypes type, Langchain4jModelInfo info) {
        return build(type, info, null);
    }

    public Object build(Langchain4jModelTypes type, Langchain4jModelInfo info, EmbeddingModel embeddingModel) {
        ModelFactory factory = factories.get(info.getProvider());
        if (factory == null) {
            throw new IllegalArgumentException(String.format(
                "资源 %s[%s] 的 provider '%s' 未注册，可用 provider: %s",
                type.type(), info.getKey(), info.getProvider(), providers(type)));
        }
        Langchain4jOptionsReader reader =
                new Langchain4jOptionsReader(type.type(), info.getKey(), info.getOptions());
        switch (type) {
            case CHAT_MODEL:
                return factory.buildChatModel(reader, listeners);
            case STREAMING_CHAT_MODEL:
                return factory.buildStreamingChatModel(reader, listeners);
            case LANGUAGE_MODEL:
                return factory.buildLanguageModel(reader);
            case STREAMING_LANGUAGE_MODEL:
                return factory.buildStreamingLanguageModel(reader);
            case EMBEDDING_MODEL:
                return factory.buildEmbeddingModel(reader);
            case SCORING_MODEL:
                return factory.buildScoringModel(reader);
            case TOKENIZER:
                return factory.buildTokenizer(reader);
            case EMBEDDING_STORE:
                return factory.buildEmbeddingStore(reader, embeddingModel);
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    /**
     * 类型化的构建入口：注册器按自身固定类型调用，返回类型由编译器保证，避免调用点各自强转。
     */
    public ChatModel buildChatModel(Langchain4jChatModelInfo info) {
        return (ChatModel) build(Langchain4jModelTypes.CHAT_MODEL, info);
    }

    public StreamingChatModel buildStreamingChatModel(Langchain4jStreamingChatModelInfo info) {
        return (StreamingChatModel) build(Langchain4jModelTypes.STREAMING_CHAT_MODEL, info);
    }

    public LanguageModel buildLanguageModel(Langchain4jLanguageModelInfo info) {
        return (LanguageModel) build(Langchain4jModelTypes.LANGUAGE_MODEL, info);
    }

    public StreamingLanguageModel buildStreamingLanguageModel(Langchain4jStreamingLanguageModelInfo info) {
        return (StreamingLanguageModel) build(Langchain4jModelTypes.STREAMING_LANGUAGE_MODEL, info);
    }
    public EmbeddingModel buildEmbeddingModel(Langchain4jEmbeddingModelInfo info) {
        return (EmbeddingModel) build(Langchain4jModelTypes.EMBEDDING_MODEL, info);
    }

    public ScoringModel buildScoringModel(Langchain4jScoringModelInfo info) {
        return (ScoringModel) build(Langchain4jModelTypes.SCORING_MODEL, info);
    }

    public TokenCountEstimator buildTokenizer(Langchain4jTokenizerInfo info) {
        return (TokenCountEstimator) build(Langchain4jModelTypes.TOKENIZER, info);
    }

    @SuppressWarnings("unchecked")
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jEmbeddingStoreInfo info,
            EmbeddingModel embeddingModel) {
        return (EmbeddingStore<TextSegment>) build(Langchain4jModelTypes.EMBEDDING_STORE, info, embeddingModel);
    }

    /**
     * 能力探测：用空 options 调一次对应方法，UnsupportedOperationException 表示未实现；
     * 其它运行时异常（如 required 缺失）说明能力已实现，仅探测参数不足。
     */
    private boolean factorySupports(Langchain4jModelTypes type, ModelFactory factory) {
        try {
            switch (type) {
                case CHAT_MODEL: factory.buildChatModel(PROBE, Collections.<ChatModelListener>emptyList()); break;
                case STREAMING_CHAT_MODEL: factory.buildStreamingChatModel(PROBE, Collections.<ChatModelListener>emptyList()); break;
                case LANGUAGE_MODEL: factory.buildLanguageModel(PROBE); break;
                case STREAMING_LANGUAGE_MODEL: factory.buildStreamingLanguageModel(PROBE); break;
                case EMBEDDING_MODEL: factory.buildEmbeddingModel(PROBE); break;
                case SCORING_MODEL: factory.buildScoringModel(PROBE); break;
                case TOKENIZER: factory.buildTokenizer(PROBE); break;
                case EMBEDDING_STORE: factory.buildEmbeddingStore(PROBE, null); break;
                default: return false;
            }
            return true;
        } catch (UnsupportedOperationException e) {
            return false;
        } catch (RuntimeException e) {
            return true;
        }
    }
}
