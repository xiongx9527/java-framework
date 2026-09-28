package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jEmbeddingStoreInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jModelHolder;
import dev.langchain4j.model.embedding.EmbeddingModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * 七类模型资源的动态注册器骨架：资源托管规则（规则 A）——
 * 先驱逐旧实例 → enabled 检查 → 显式工厂构建 → 成功才落库；构建失败不注册、不遗留半成品。
 *
 * @param <T> 具体资源实体
 */
public abstract class AbstractLangchain4jModelMagicDynamicRegistry<T extends Langchain4jModelInfo>
        extends AbstractMagicDynamicRegistry<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLangchain4jModelMagicDynamicRegistry.class);

    protected final Langchain4jModelHolder holder;
    protected final Langchain4jModelFactoryRegistry factories;
    protected final Langchain4jModelTypes type;

    protected AbstractLangchain4jModelMagicDynamicRegistry(MagicResourceStorage<T> magicResourceStorage,
            Langchain4jModelHolder holder, Langchain4jModelFactoryRegistry factories, Langchain4jModelTypes type) {
        super(magicResourceStorage);
        this.holder = holder;
        this.factories = factories;
        this.type = type;
    }

    @Override
    protected boolean register(MappingNode<T> node) {
        T entity = node.getEntity();
        holder.evict(type, entity.getKey());
        if (!entity.isEnabled()) {
            return true;
        }
        Object instance;
        try {
            instance = buildInstance(entity);
        } catch (RuntimeException e) {
            logger.warn("构建 LangChain4j 实例失败: type={}, key={}", type.type(), entity.getKey(), e);
            return false;
        }
        if (instance == null) {
            return false;
        }
        holder.put(type, entity.getKey(), instance);
        return super.register(node);
    }

    @Override
    protected void unregister(MappingNode<T> node) {
        holder.evict(type, node.getEntity().getKey());
        super.unregister(node);
    }

    private Object buildInstance(T entity) {
        if (type == Langchain4jModelTypes.EMBEDDING_STORE) {
            Langchain4jEmbeddingStoreInfo storeInfo = (Langchain4jEmbeddingStoreInfo) entity;
            EmbeddingModel embeddingModel = holder.get(Langchain4jModelTypes.EMBEDDING_MODEL,
                    storeInfo.getEmbeddingModelKey());
            return factories.build(type, entity, embeddingModel);
        }
        return factories.build(type, entity);
    }
}
