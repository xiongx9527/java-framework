package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.nebula.NebulaEmbeddingStore;

/**
 * NebulaGraph 向量库工厂：{@code NebulaEmbeddingStore}（Lombok @Builder + withBasicAuth 构造）。
 * 走 dimension 回退链（写入向量时按 dimension 校验长度）。
 *
 * <p>NebulaGraph 没有向量索引，检索是扫描 tag 的点后在 Java 侧算余弦相似度，
 * 大数据量场景应改用专门向量库；{@code max-scan-rows} 控制扫描上限。</p>
 */
public class Langchain4jNebulaModelFactory implements ModelFactory {

    @Override
    public String provider() {
        return "nebula";
    }

    @Override
    public EmbeddingStore<TextSegment> buildEmbeddingStore(Langchain4jOptionsReader o, EmbeddingModel embeddingModel) {
        NebulaEmbeddingStore.NebulaEmbeddingStoreBuilder builder = NebulaEmbeddingStore.builder()
                .withBasicAuth(o.required("addresses"), o.required("username"), o.string("password", ""))
                .spaceName(o.required("space-name"))
                .tag(o.string("tag", "Document"))
                .idProperty(o.string("id-property", "id"))
                .textProperty(o.string("text-property", "text"))
                .embeddingProperty(o.string("embedding-property", "embedding"))
                .metadataProperty(o.string("metadata-property", "metadata"))
                .dimension(EmbeddingStoreDimensions.resolve(o, embeddingModel));
        // 可选数值项：不配置时交给 NebulaEmbeddingStore 自己的默认值
        if (o.has("max-session-size")) {
            builder.maxSessionSize(o.integer("max-session-size", null));
        }
        if (o.has("timeout")) {
            builder.timeout(o.integer("timeout", null));
        }
        if (o.has("max-scan-rows")) {
            builder.maxScanRows(o.integer("max-scan-rows", null));
        }
        if (o.has("await-schema-timeout")) {
            builder.awaitSchemaTimeout(o.longValue("await-schema-timeout", null));
        }
        return builder.build();
    }
}
