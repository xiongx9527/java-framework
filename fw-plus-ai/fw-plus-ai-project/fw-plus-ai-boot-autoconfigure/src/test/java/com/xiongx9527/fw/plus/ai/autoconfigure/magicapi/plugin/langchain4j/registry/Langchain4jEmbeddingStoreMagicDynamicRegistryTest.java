package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jInMemoryModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * embedding-store 注册器测试：被引用 embedding-model 不可用时注册失败并给出明确提示；
 * 维度解析成功时才落库；停用只清理。
 */
class Langchain4jEmbeddingStoreMagicDynamicRegistryTest {

    private MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;
    private MagicDynamicLangchain4jEmbeddingStoreDataSource stores;
    private Langchain4jEmbeddingStoreMagicDynamicRegistry registry;

    @BeforeEach
    void setUp() {
        embeddingModels = new MagicDynamicLangchain4jEmbeddingModelDataSource();
        stores = new MagicDynamicLangchain4jEmbeddingStoreDataSource();
        Langchain4jModelFactoryRegistry factories = new Langchain4jModelFactoryRegistry();
        factories.register(new Langchain4jInMemoryModelFactory());
        registry = new Langchain4jEmbeddingStoreMagicDynamicRegistry(
                new Langchain4jEmbeddingStoreMagicResourceStorage(
                        factories.providers(Langchain4jModelTypes.EMBEDDING_STORE)),
                stores, embeddingModels, factories);
    }

    @Test
    void shouldExplainMissingEmbeddingModel() {
        Langchain4jEmbeddingStoreInfo info = storeInfo("milvus-main", null);

        assertThatThrownBy(() -> Langchain4jEmbeddingStoreMagicDynamicRegistry.requireEmbeddingModel(info, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("请先配置并启用 embedding-model")
                .hasMessageContaining("qwen-embed");
    }

    @Test
    void shouldFailRegistrationWhenReferencedEmbeddingModelIsMissing() {
        assertThat(registry.register(storeInfo("milvus-main", null))).isFalse();
        assertThat(stores.keys()).isEmpty();
    }

    @Test
    void shouldFailEvenWhenDimensionIsConfiguredButModelMissing() {
        Langchain4jEmbeddingStoreInfo info = storeInfo("milvus-main", null);
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("dimension", 768);
        info.setOptions(options);

        assertThat(registry.register(info)).isFalse();
        assertThat(stores.keys()).isEmpty();
    }

    @Test
    void shouldRegisterWhenReferencedEmbeddingModelIsAvailable() {
        embeddingModels.put("qwen-embed", embeddingModel(768));

        assertThat(registry.register(storeInfo("milvus-main", null))).isTrue();
        assertThat(stores.get("milvus-main")).isNotNull();
    }

    @Test
    void shouldOnlyEvictWhenDisabled() {
        Langchain4jEmbeddingStoreInfo info = storeInfo("milvus-main", null);
        info.setEnabled(false);

        assertThat(registry.register(info)).isTrue();
        assertThat(stores.keys()).isEmpty();
    }

    private static Langchain4jEmbeddingStoreInfo storeInfo(String key, Integer dimension) {
        Langchain4jEmbeddingStoreInfo info = new Langchain4jEmbeddingStoreInfo();
        info.setId(key);
        info.setKey(key);
        info.setProvider("in-memory");
        info.setEnabled(true);
        info.setEmbeddingModelKey("qwen-embed");
        info.setCreateTime(1L);
        info.setUpdateTime(1L);
        if (dimension != null) {
            Map<String, Object> options = new LinkedHashMap<String, Object>();
            options.put("dimension", dimension);
            info.setOptions(options);
        }
        return info;
    }

    private static EmbeddingModel embeddingModel(int dimension) {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.dimension()).thenReturn(dimension);
        return model;
    }
}
