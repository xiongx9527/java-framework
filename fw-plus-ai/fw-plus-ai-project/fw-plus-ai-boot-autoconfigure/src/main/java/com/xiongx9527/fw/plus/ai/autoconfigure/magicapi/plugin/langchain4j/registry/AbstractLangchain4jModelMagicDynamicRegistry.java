package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.AbstractMagicDynamicLangchain4jDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * 七类模型资源的动态注册器骨架：资源托管规则（规则 A）——
 * 先驱逐旧实例 → enabled 检查 → 显式工厂构建 → 成功才落库；构建失败不注册、不遗留半成品。
 *
 * @param <T> 具体资源实体
 * @param <M> 该资源类型对应的实例类型
 */
public abstract class AbstractLangchain4jModelMagicDynamicRegistry<T extends Langchain4jModelInfo, M>
        extends AbstractMagicDynamicRegistry<T> {

    private static final Logger logger = LoggerFactory.getLogger(AbstractLangchain4jModelMagicDynamicRegistry.class);

    /** 本类型的实例持有者（每类资源一个，不跨类型共享） */
    protected final AbstractMagicDynamicLangchain4jDataSource<M> holder;
    protected final Langchain4jModelFactoryRegistry factories;
    protected final Langchain4jModelTypes type;

    protected AbstractLangchain4jModelMagicDynamicRegistry(MagicResourceStorage<T> magicResourceStorage,
            AbstractMagicDynamicLangchain4jDataSource<M> holder, Langchain4jModelFactoryRegistry factories,
            Langchain4jModelTypes type) {
        super(magicResourceStorage);
        this.holder = holder;
        this.factories = factories;
        this.type = type;
    }

    @Override
    protected boolean register(MappingNode<T> node) {
        T entity = node.getEntity();
        holder.evict(entity.getKey());
        if (!entity.isEnabled()) {
            return true;
        }
        M instance;
        try {
            instance = buildInstance(entity);
        } catch (RuntimeException e) {
            logger.warn("构建 LangChain4j 实例失败: type={}, key={}", type.type(), entity.getKey(), e);
            return false;
        }
        if (instance == null) {
            return false;
        }
        holder.put(entity.getKey(), instance);
        return super.register(node);
    }

    @Override
    protected void unregister(MappingNode<T> node) {
        holder.evict(node.getEntity().getKey());
        super.unregister(node);
    }

    /** 按实体构建本类型实例；返回 {@code null} 表示构建失败、不注册。 */
    protected abstract M buildInstance(T entity);
}
