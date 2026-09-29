package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.modules.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.AbstractMagicDynamicLangchain4jDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import org.ssssssss.script.MagicScriptContext;

import java.util.Collections;
import java.util.List;
import java.util.TreeSet;

/**
 * 七类脚本模块的公共骨架：脚本按 key 直接取模型实例（{@code chatModel['qwen-main']}），
 * key 未配置时抛中文异常并列出该类型已配置的 key；只读语义，不支持脚本设置实例。
 *
 * @param <M> 该类型对应的模型实例
 */
public abstract class AbstractLangchain4jModelModule<M> {

    protected final AbstractMagicDynamicLangchain4jDataSource<M> holder;
    protected final Langchain4jModelTypes type;

    protected AbstractLangchain4jModelModule(AbstractMagicDynamicLangchain4jDataSource<M> holder,
            Langchain4jModelTypes type) {
        this.holder = holder;
        this.type = type;
    }

    /** 脚本按 key 取实例；未配置时抛中文异常并列出已配置 key */
    public M getDynamicAttribute(String key) {
        M instance = holder.get(key);
        if (instance == null) {
            throw new IllegalArgumentException(String.format(
                    "未配置 LangChain4j %s: %s，已配置: %s", type.type(), key, new TreeSet<>(holder.keys())));
        }
        return instance;
    }

    /** 无默认数据源概念：未通过脚本上下文指定 key 时返回模块自身 */
    public Object getDynamicModule(MagicScriptContext context) {
        return this;
    }

    /** 本插件无静态默认数据源：实例全部来自控制台保存的 datasource（均带 id） */
    public List<M> defaultMappings() {
        return Collections.emptyList();
    }
}
