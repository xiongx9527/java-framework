package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.module.Langchain4jLanguageModelModule;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import com.xiongx9527.fw.plus.common.util.StringUtils;
import dev.langchain4j.model.language.LanguageModel;
import dev.langchain4j.model.output.Response;

/**
 * {@link LanguageModel} 的动态代理：持有脚本模块与资源 key，每次调用实时解析当前实例，
 * magic 资源热更新/注销后不持有失效引用。解析失败回落容器中的语言模型 bean
 * （key 空取默认、非空按 bean 名）。
 */
public class MagicDynamicLanguageModel implements LanguageModel {

    private final Langchain4jLanguageModelModule languageModelModule;

    private final String key;

    public MagicDynamicLanguageModel(Langchain4jLanguageModelModule languageModelModule, String key) {
        this.languageModelModule = languageModelModule;
        this.key = key;
    }

    private LanguageModel getLanguageModel() {
        try {
            return languageModelModule.getDynamicAttribute(key);
        } catch (Exception e) {
            if (StringUtils.isEmpty(key)) {
                return SpringUtils.getBean(LanguageModel.class);
            }
            return SpringUtils.getBean(key, LanguageModel.class);
        }
    }

    @Override
    public Response<String> generate(String message) {
        return getLanguageModel().generate(message);
    }
}
