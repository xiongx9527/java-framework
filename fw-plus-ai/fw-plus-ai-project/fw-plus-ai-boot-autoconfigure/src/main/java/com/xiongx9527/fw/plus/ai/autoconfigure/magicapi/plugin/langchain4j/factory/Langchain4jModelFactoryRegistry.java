package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.EmbeddingModel;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * provider 短名 → 工厂的分发表：每类资源一张短名集合，表外短名构建期拒绝。
 */
public class Langchain4jModelFactoryRegistry {

    private static final Langchain4jOptionsReader PROBE =
            new Langchain4jOptionsReader("probe", "probe", Collections.<String, Object>emptyMap());

    private final Map<String, ModelFactory> factories = new LinkedHashMap<String, ModelFactory>();

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
                return factory.buildChatModel(reader);
            case STREAMING_CHAT_MODEL:
                return factory.buildStreamingChatModel(reader);
            case LANGUAGE_MODEL:
                return factory.buildLanguageModel(reader);
            case STREAMING_LANGUAGE_MODEL:
                return factory.buildStreamingLanguageModel(reader);
            case EMBEDDING_MODEL:
                return factory.buildEmbeddingModel(reader);
            case TOKENIZER:
                return factory.buildTokenizer(reader);
            case EMBEDDING_STORE:
                return factory.buildEmbeddingStore(reader, embeddingModel);
            default:
                throw new IllegalArgumentException("不支持的资源类型: " + type);
        }
    }

    /**
     * 能力探测：用空 options 调一次对应方法，UnsupportedOperationException 表示未实现；
     * 其它运行时异常（如 required 缺失）说明能力已实现，仅探测参数不足。
     */
    private boolean factorySupports(Langchain4jModelTypes type, ModelFactory factory) {
        try {
            switch (type) {
                case CHAT_MODEL: factory.buildChatModel(PROBE); break;
                case STREAMING_CHAT_MODEL: factory.buildStreamingChatModel(PROBE); break;
                case LANGUAGE_MODEL: factory.buildLanguageModel(PROBE); break;
                case STREAMING_LANGUAGE_MODEL: factory.buildStreamingLanguageModel(PROBE); break;
                case EMBEDDING_MODEL: factory.buildEmbeddingModel(PROBE); break;
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
