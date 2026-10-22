package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jTokenizerDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.Tokenizer;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 分词器脚本模块：脚本用 {@code tokenizer['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("langchain4jTokenizer")
public class Langchain4jTokenizerModule extends AbstractLangchain4jModelModule<Tokenizer>
        implements DynamicAttribute<Tokenizer, Langchain4jTokenizerModule>, DynamicModule<Tokenizer> {

    public Langchain4jTokenizerModule(MagicDynamicLangchain4jTokenizerDataSource holder,
            ObjectProvider<Tokenizer> defaultInstance) {
        super(holder, Langchain4jModelTypes.TOKENIZER, defaultInstance);
    }

    @Override
    public Langchain4jTokenizerModule setDynamicAttribute(String key, Tokenizer value) {
        throw new UnsupportedOperationException(I18nMessageResolver.resolveKey(
                "fw.ai.modelModule.scriptSetUnsupported", "langchain4j-tokenizer"));
    }

    @Override
    public List<Tokenizer> defaultMappings() {
        return super.defaultMappings();
    }
}
