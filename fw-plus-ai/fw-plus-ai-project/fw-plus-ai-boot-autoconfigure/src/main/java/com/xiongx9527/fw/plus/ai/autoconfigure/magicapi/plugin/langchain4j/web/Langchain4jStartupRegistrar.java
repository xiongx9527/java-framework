package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.web;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jEmbeddingStoreDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry.Langchain4jEmbeddingStoreMagicDynamicRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.resource.Langchain4jEmbeddingStoreMagicResourceStorage;
import java.util.List;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.store.embedding.EmbeddingStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * 启动完成兜底：embedding-store 的 FileEvent 可能先于其 embedding-model 到达，
 * 该次注册失败；启动完成（所有资源加载结束）后，对启动期失败的 store 统一重试一次注册。
 */
public class Langchain4jStartupRegistrar implements SmartLifecycle {

    private static final Logger logger = LoggerFactory.getLogger(Langchain4jStartupRegistrar.class);

    private final MagicResourceService magicResourceService;
    private final MagicDynamicLangchain4jEmbeddingStoreDataSource storeDataSource;
    private final MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModelDataSource;
    private final Langchain4jModelFactoryRegistry factories;
    private final Langchain4jEmbeddingStoreMagicResourceStorage storeStorage;

    private volatile boolean running;

    public Langchain4jStartupRegistrar(MagicResourceService magicResourceService,
            MagicDynamicLangchain4jEmbeddingStoreDataSource storeDataSource,
            MagicDynamicLangchain4jEmbeddingModelDataSource embeddingModelDataSource,
            Langchain4jModelFactoryRegistry factories,
            Langchain4jEmbeddingStoreMagicResourceStorage storeStorage) {
        this.magicResourceService = magicResourceService;
        this.storeDataSource = storeDataSource;
        this.embeddingModelDataSource = embeddingModelDataSource;
        this.factories = factories;
        this.storeStorage = storeStorage;
    }

    @Override
    public void start() {
        retryFailedStores();
        running = true;
    }

    @Override
    public void stop() {
        running = false;
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    /** 最晚执行：待 embedding-model 注册器完成之后再兜底 */
    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    private void retryFailedStores() {
        List<?> entities;
        try {
            entities = magicResourceService.listFiles(storeStorage.folder() + ":0");
        } catch (RuntimeException e) {
            // 全新工程尚未创建 embedding-store 分组时 listFiles 会抛「找不到分组信息」，
            // 兜底重试钩子不允许阻断应用启动
            logger.warn("LangChain4j embedding-store 启动重试跳过: 无法读取资源目录, 原因: {}", e.getMessage());
            return;
        }
        for (Object entity : entities) {
            if (!(entity instanceof Langchain4jEmbeddingStoreInfo)) {
                continue;
            }
            Langchain4jEmbeddingStoreInfo store = (Langchain4jEmbeddingStoreInfo) entity;
            if (!store.isEnabled()) {
                continue;
            }
            if (storeDataSource.get(store.getKey()) != null) {
                continue;
            }
            try {
                EmbeddingStore<TextSegment> instance = factories.buildEmbeddingStore(store,
                        Langchain4jEmbeddingStoreMagicDynamicRegistry.requireEmbeddingModel(store,
                                embeddingModelDataSource.get(store.getEmbeddingModelKey())));
                if (instance != null) {
                    storeDataSource.put(store.getKey(), instance);
                    logger.info("LangChain4j embedding-store 启动重试成功: key={}, embeddingModel={}",
                            store.getKey(), store.getEmbeddingModelKey());
                }
            } catch (RuntimeException e) {
                logger.warn("LangChain4j embedding-store 启动重试失败: key={}, 原因: {}",
                        store.getKey(), e.getMessage());
            }
        }
    }
}
