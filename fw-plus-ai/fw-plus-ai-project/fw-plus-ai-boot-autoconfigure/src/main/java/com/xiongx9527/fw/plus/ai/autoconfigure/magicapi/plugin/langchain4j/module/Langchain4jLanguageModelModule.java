package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.MagicDynamicLangchain4jLanguageModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jModelTypes;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import dev.langchain4j.model.language.LanguageModel;
import org.springframework.beans.factory.ObjectProvider;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.script.functions.DynamicAttribute;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;

import java.util.List;

/**
 * 语言模型脚本模块：脚本用 {@code languageModel['key']} 直接取模型实例后调用 langchain4j 方法。
 */
@MagicModule("langchain4jLanguageModel")
public class Langchain4jLanguageModelModule extends AbstractLangchain4jModelModule<LanguageModel>
        implements DynamicAttribute<LanguageModel, Langchain4jLanguageModelModule>, DynamicModule<LanguageModel> {

    public Langchain4jLanguageModelModule(MagicDynamicLangchain4jLanguageModelDataSource holder,
            ObjectProvider<LanguageModel> defaultInstance) {
        super(holder, Langchain4jModelTypes.LANGUAGE_MODEL, defaultInstance);
    }

    @Override
    public Langchain4jLanguageModelModule setDynamicAttribute(String key, LanguageModel value) {
        throw new UnsupportedOperationException(I18nMessageResolver.resolveKey(
                "fw.ai.modelModule.scriptSetUnsupported", "langchain4j-language-model"));
    }

    @Override
    public List<LanguageModel> defaultMappings() {
        return super.defaultMappings();
    }
}
