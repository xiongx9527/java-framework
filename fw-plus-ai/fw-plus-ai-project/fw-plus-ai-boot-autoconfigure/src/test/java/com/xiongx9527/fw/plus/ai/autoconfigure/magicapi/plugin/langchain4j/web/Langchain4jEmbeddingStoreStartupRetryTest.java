package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.store.Langchain4jInMemoryModelFactory;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 启动重试测试：embedding-store 先于其 embedding-model 加载时，启动完成后自动恢复注册；
 * 已成功的资源不被重建。
 */
class Langchain4jEmbeddingStoreStartupRetryTest {

    private MagicResourceService magicResourceService;
    private MagicDynamicLangchain4jEmbeddingStoreDataSource stores;
    private MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModels;
    private Langchain4jStartupRegistrar registrar;

    @BeforeEach
    void setUp() {
        magicResourceService = mock(MagicResourceService.class);
        stores = new MagicDynamicLangchain4jEmbeddingStoreDataSource();
        embeddingModels = new MagicDynamicLangchain4jEmbeddingModelDataSource();
        Langchain4jModelFactoryRegistry factories = new Langchain4jModelFactoryRegistry();
        factories.register(new Langchain4jInMemoryModelFactory());
        Langchain4jEmbeddingStoreMagicResourceStorage storage = new Langchain4jEmbeddingStoreMagicResourceStorage(
                factories.providers(Langchain4jModelTypes.EMBEDDING_STORE));
        registrar = new Langchain4jStartupRegistrar(magicResourceService, stores, embeddingModels, factories, storage);
    }

    @Test
    void shouldRecoverStoreWhenModelLoadedLater() {
        when(magicResourceService.listFiles("langchain4j-embedding-store:0"))
                .thenReturn(Collections.<MagicEntity>singletonList(storeInfo("milvus-main", true)));
        embeddingModels.put("qwen-embed", embeddingModel(768));
        assertThat(stores.keys()).as("启动期 store 先到达且注册失败").isEmpty();

        registrar.start();

        assertThat(stores.get("milvus-main")).isNotNull();
        assertThat(registrar.isRunning()).isTrue();
    }

    @Test
    void shouldKeepAlreadyRegisteredInstance() {
        when(magicResourceService.listFiles("langchain4j-embedding-store:0"))
                .thenReturn(Collections.<MagicEntity>singletonList(storeInfo("milvus-main", true)));
        embeddingModels.put("qwen-embed", embeddingModel(768));
        EmbeddingStore<?> existing = mock(EmbeddingStore.class);
        stores.put("milvus-main", castStore(existing));

        registrar.start();

        assertThat(stores.get("milvus-main")).as("已成功注册的实例不应被重建").isSameAs(existing);
    }

    @Test
    void shouldSkipDisabledStore() {
        when(magicResourceService.listFiles("langchain4j-embedding-store:0"))
                .thenReturn(Collections.<MagicEntity>singletonList(storeInfo("milvus-main", false)));
        embeddingModels.put("qwen-embed", embeddingModel(768));

        registrar.start();

        assertThat(stores.keys()).isEmpty();
    }

    @Test
    void shouldTolerateRetryFailure() {
        when(magicResourceService.listFiles("langchain4j-embedding-store:0"))
                .thenReturn(Collections.<MagicEntity>singletonList(storeInfo("milvus-main", true)));

        registrar.start();

        assertThat(stores.keys()).as("模型仍缺失时重试失败但不抛异常").isEmpty();
    }

    private static Langchain4jEmbeddingStoreInfo storeInfo(String key, boolean enabled) {
        Langchain4jEmbeddingStoreInfo info = new Langchain4jEmbeddingStoreInfo();
        info.setKey(key);
        info.setProvider("in-memory");
        info.setEnabled(enabled);
        info.setEmbeddingModelKey("qwen-embed");
        info.setOptions(Collections.<String, Object>emptyMap());
        return info;
    }

    private static EmbeddingModel embeddingModel(int dimension) {
        EmbeddingModel model = mock(EmbeddingModel.class);
        when(model.dimension()).thenReturn(dimension);
        return model;
    }

    @SuppressWarnings("unchecked")
    private static EmbeddingStore<dev.langchain4j.data.segment.TextSegment> castStore(EmbeddingStore<?> store) {
        return (EmbeddingStore<dev.langchain4j.data.segment.TextSegment>) store;
    }
}
