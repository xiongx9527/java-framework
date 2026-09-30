package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.community.store.embedding.memfile.MemFileEmbeddingStore;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;

import java.nio.file.Path;

/**
 * MemFile（本地内存/文件持久化）向量库工厂：零部署，可选 {@code path}（默认纯内存）、{@code flush-interval}。
 */
public class Langchain4jMemFileModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "memfile";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        String path = o.string("path", null);
        Integer flushInterval = o.integer("flush-interval", null);
        if (path != null) {
            if (flushInterval != null) {
                return new MemFileEmbeddingStore(Path.of(path), flushInterval);
            }
            return new MemFileEmbeddingStore(Path.of(path));
        }
        return new MemFileEmbeddingStore();
    }
}
