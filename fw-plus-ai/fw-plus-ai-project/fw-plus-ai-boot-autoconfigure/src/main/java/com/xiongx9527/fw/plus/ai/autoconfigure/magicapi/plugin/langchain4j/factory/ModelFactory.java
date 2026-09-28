package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.Tokenizer;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.chat.StreamingChatLanguageModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.language.StreamingLanguageModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

/**
 * 单家 provider 的模型构建工厂：每个短名对应一个实现（如 openai、qwen）。
 * 参数一律经 Langchain4jOptionsReader 从 datasource options 读取；
 * builder 链写法参照 core 层 fw-plus-ai-langchain4j-* 的 AutoConfig（只参考，不修改）。
 */
public interface ModelFactory {

    /** provider 短名，如 "openai" */
    String provider();

    default ChatLanguageModel buildChatModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default StreamingChatLanguageModel buildStreamingChatModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default LanguageModel buildLanguageModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default StreamingLanguageModel buildStreamingLanguageModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default EmbeddingModel buildEmbeddingModel(Langchain4jOptionsReader o) { throw unsupported(); }

    default Tokenizer buildTokenizer(Langchain4jOptionsReader o) { throw unsupported(); }

    default EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        throw unsupported();
    }

    default UnsupportedOperationException unsupported() {
        return new UnsupportedOperationException("provider " + provider() + " 不支持该资源类型");
    }
}
