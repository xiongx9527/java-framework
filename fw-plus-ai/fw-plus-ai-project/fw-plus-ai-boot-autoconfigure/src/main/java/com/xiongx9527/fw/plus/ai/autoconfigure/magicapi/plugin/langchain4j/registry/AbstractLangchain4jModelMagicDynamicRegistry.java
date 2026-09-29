package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.registry;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.Langchain4jModelFactoryRegistry;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.AbstractMagicDynamicLangchain4jDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.event.MagicEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * 七类模型资源的动态注册器骨架，写法对齐仓库既有数据源插件（如 MongoDataSourceMagicDynamicRegistry）：
 *
 * <ul>
 *   <li>重写 {@link #register(Langchain4jModelInfo)}：注册失败只记日志并返回 {@code false}，
 *       不让异常外溢影响其它插件/其它资源的资源加载。</li>
 *   <li>重写 {@link #register(AbstractMagicDynamicRegistry.MappingNode)}：先驱逐旧实例 → enabled 检查 →
 *       显式工厂构建 → 成功才落库；构建失败不注册、不遗留半成品。</li>
 *   <li>重写 {@link #unregister(AbstractMagicDynamicRegistry.MappingNode)}：先驱逐实例（关闭可关闭资源），
 *       再交给父类维护映射。</li>
 *   <li>事件入口统一经 {@link #safelyProcess(MagicEvent)}，单个资源异常不影响其它资源。</li>
 * </ul>
 *
 * <p>实例驱逐只收敛在会重建实例的 {@code MappingNode} 钩子里：magic-api 的
 * {@code register(T)} 在「同一实体重复注册」时会直接返回而不回调钩子，若在该入口先行驱逐，
 * 会出现「实例被销毁但不再重建」的空窗，故不在入口驱逐。</p>
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
    public boolean register(T entity) {
        try {
            return super.register(entity);
        } catch (Exception e) {
            logger.warn("注册 LangChain4j 资源失败: type={}, key={}", type.type(), entity.getKey(), e);
            return false;
        }
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
        } catch (Exception e) {
            logger.warn("构建 LangChain4j 实例失败: type={}, key={}", type.type(), entity.getKey(), e);
            return false;
        }
        if (instance == null) {
            return false;
        }
        boolean registered;
        try {
            registered = super.register(node);
        } catch (Exception e) {
            logger.warn("登记 LangChain4j 资源映射失败: type={}, key={}", type.type(), entity.getKey(), e);
            return false;
        }
        if (registered) {
            // 登记成功后才放入实例：避免「实例已可取用但映射登记失败」的不一致状态
            holder.put(entity.getKey(), instance);
        }
        return registered;
    }

    @Override
    protected void unregister(MappingNode<T> node) {
        T entity = node.getEntity();
        try {
            holder.evict(entity.getKey());
        } catch (Exception e) {
            logger.warn("驱逐 LangChain4j 实例失败: type={}, key={}", type.type(), entity.getKey(), e);
        }
        try {
            super.unregister(node);
        } catch (Exception e) {
            logger.warn("注销 LangChain4j 资源映射失败: type={}, key={}", type.type(), entity.getKey(), e);
        }
    }

    /** 文件/分组事件入口：单个资源处理失败不影响其它资源与其它插件 */
    protected void safelyProcess(MagicEvent event) {
        try {
            if (event instanceof FileEvent) {
                processEvent((FileEvent) event);
            } else if (event instanceof GroupEvent) {
                processEvent((GroupEvent) event);
            }
        } catch (Exception e) {
            logger.warn("处理 LangChain4j 资源事件失败: type={}, action={}", type.type(), event.getAction(), e);
        }
    }

    /** 按实体构建本类型实例；返回 {@code null} 表示构建失败、不注册。 */
    protected abstract M buildInstance(T entity);
}
