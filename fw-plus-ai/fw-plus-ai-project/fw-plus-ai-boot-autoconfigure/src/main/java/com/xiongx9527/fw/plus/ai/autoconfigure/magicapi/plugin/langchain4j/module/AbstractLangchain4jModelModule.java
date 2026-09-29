package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.AbstractMagicDynamicLangchain4jDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.script.MagicScriptContext;

import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * 七类脚本模块的公共骨架，两种取用方式：
 *
 * <ul>
 *   <li>{@code chatModel['qwen-main']} —— 按 datasource key 取控制台维护的实例；</li>
 *   <li>{@code chatModel.embed(...)}（不指定 key）—— 默认取 Spring 容器中由 langchain4j
 *       自动配置注入的同类型实例（{@link org.springframework.beans.factory.ObjectProvider}）。</li>
 * </ul>
 *
 * <p>key 未配置且容器中亦无默认实例时，抛中文异常说明两条路径均未就绪。只读语义。</p>
 *
 * @param <M> 该类型对应的模型实例
 */
public abstract class AbstractLangchain4jModelModule<M> {

    protected final AbstractMagicDynamicLangchain4jDataSource<M> holder;
    protected final Langchain4jModelTypes type;
    /** Spring 容器中的默认实例（core 层 langchain4j 自动配置注入），可能不存在 */
    private final org.springframework.beans.factory.ObjectProvider<M> defaultInstance;

    protected AbstractLangchain4jModelModule(AbstractMagicDynamicLangchain4jDataSource<M> holder,
            Langchain4jModelTypes type, org.springframework.beans.factory.ObjectProvider<M> defaultInstance) {
        this.holder = holder;
        this.type = type;
        this.defaultInstance = defaultInstance;
    }

    /** 脚本按 key 取实例；key 为空时回落到 Spring 默认实例 */
    public M getDynamicAttribute(String key) {
        if (key == null || key.trim().isEmpty()) {
            return requireDefaultInstance();
        }
        M instance = holder.get(key);
        if (instance == null) {
            throw new IllegalArgumentException(String.format(
                    "未配置 LangChain4j %s: %s，已配置: %s", type.type(), key, new TreeSet<>(holder.keys())));
        }
        return instance;
    }

    /**
     * 不指定 datasource key 时返回 Spring 默认实例；脚本用 {@code chatModel['key']} 指定 key 时
     * 由 {@link #getDynamicAttribute(String)} 路由。
     */
    public M getDynamicModule(MagicScriptContext context) {
        String key = context == null ? null : context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (key != null && !key.trim().isEmpty()) {
            return getDynamicAttribute(key);
        }
        return requireDefaultInstance();
    }

    /** 本插件无静态默认数据源：实例全部来自控制台保存的 datasource（均带 id） */
    public List<M> defaultMappings() {
        return Collections.emptyList();
    }

    private M requireDefaultInstance() {
        M instance = defaultInstance == null ? null : defaultInstance.getIfAvailable();
        if (instance == null) {
            throw new IllegalArgumentException(String.format(
                    "未指定 LangChain4j %s 的 datasource key，且 Spring 容器中没有默认实例"
                            + "（请启用 langchain4j 对应自动配置，或在控制台维护 %s 数据源后用 ['key'] 取用）",
                    type.type(), type.type()));
        }
        return instance;
    }
}
