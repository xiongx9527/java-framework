package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * dimension 决策链测试：options 配置优先 → 被引用模型维度 → 中文报错。
 */
class EmbeddingStoreDimensionsTest {

    @Test
    void shouldPreferConfiguredDimension() {
        Integer resolved = EmbeddingStoreDimensions.resolve(reader(768), embeddingModel(1536));

        assertThat(resolved).isEqualTo(768);
    }

    @Test
    void shouldFallBackToEmbeddingModelDimension() {
        Integer resolved = EmbeddingStoreDimensions.resolve(reader(null), embeddingModel(1536));

        assertThat(resolved).isEqualTo(1536);
    }

    @Test
    void shouldRejectZeroAsUnset() {
        Integer resolved = EmbeddingStoreDimensions.resolve(reader(0), embeddingModel(1536));

        assertThat(resolved).as("dimension=0 视为未配置").isEqualTo(1536);
    }

    @Test
    void shouldFailWhenNeitherSideProvidesDimension() {
        assertThatThrownBy(() -> EmbeddingStoreDimensions.resolve(reader(null), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未配置 dimension");
    }

    private static Langchain4jOptionsReader reader(Integer dimension) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        if (dimension != null) {
            options.put("dimension", dimension);
        }
        return new Langchain4jOptionsReader("embedding-store", "milvus-main",
                dimension == null ? Collections.<String, Object>emptyMap() : options);
    }

    private static EmbeddingModel embeddingModel(int dimension) {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.dimension()).thenReturn(dimension);
        return model;
    }
}
