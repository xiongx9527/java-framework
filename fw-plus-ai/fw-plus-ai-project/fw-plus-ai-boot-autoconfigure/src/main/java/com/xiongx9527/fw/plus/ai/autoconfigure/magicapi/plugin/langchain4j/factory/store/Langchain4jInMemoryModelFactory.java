package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;

/**
 * 内存向量库工厂：开发/测试用，无连接参数，不校验 dimension。
 * InMemoryEmbeddingStore 位于 dev.langchain4j:langchain4j（主 jar）。
 */
public class Langchain4jInMemoryModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "in-memory";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        return new InMemoryEmbeddingStore<TextSegment>();
    }
}
