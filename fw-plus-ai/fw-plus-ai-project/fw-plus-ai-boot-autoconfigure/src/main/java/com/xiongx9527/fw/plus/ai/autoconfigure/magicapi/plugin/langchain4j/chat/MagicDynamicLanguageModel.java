package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chat;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.output.Response;

public class MagicDynamicLanguageModel implements LanguageModel {

    private final Langchain4jLanguageModelModule languageModel;

    private final String key;

    public MagicDynamicLanguageModel(Langchain4jLanguageModelModule languageModel, String key) {
        this.languageModel = languageModel;
        this.key = key;
    }
    @Override
    public Response<String> generate(String prompt) {
        try{
            return languageModel.getDynamicAttribute(key).generate(prompt);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(LanguageModel.class).generate(prompt);
            }
            return SpringUtils.getBean(key,LanguageModel.class).generate(prompt);
        }
    }
}
