package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.listener.ChatModelListener;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.scoring.ScoringModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import java.util.List;


/**
 * 单家 provider 的模型构建工厂：每个短名对应一个实现（如 openai、qwen）。
 * 参数一律经 Langchain4jOptionsReader 从 datasource options 读取；
 * builder 链写法参照 core 层 fw-plus-ai-langchain4j-* 的 AutoConfig（只参考，不修改）。
 *
 * <p>chat-model 与 streaming-chat-model 统一接收 {@code listeners}（Spring 容器中的
 * {@link ChatModelListener} 集合），实现类把它交给 builder 的 {@code listeners(...)}，
 * 从而让所有 provider 共享同一套观测链路。0.35.0 里 local-ai / qianfan / hugging-face 的
 * builder 未暴露该参数，这三家只能忽略 listeners。</p>
 */
public interface ModelFactory {

    /** provider 短名，如 "openai" */
    String provider();

    default ChatModel buildChatModel(Langchain4jOptionsReader o, List<ChatModelListener> listeners) {
        throw unsupported();
    }

    default StreamingChatModel buildStreamingChatModel(Langchain4jOptionsReader o,
            List<ChatModelListener> listeners) {
        throw unsupported();
    }

    default EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) { throw unsupported(); }

    /** rerank 重排模型：jina / cohere 支持，其余 provider 默认不支持 */
    default ScoringModel buildScoringModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        throw unsupported();
    }

    default UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("provider " + provider() + " 不支持该资源类型");
    }
}
