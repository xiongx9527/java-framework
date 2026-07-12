package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.common;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.LangChain4jModel;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.model.Langchain4jDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;

public interface BaseService extends MagicBaseService {

    /**
     * 验证Langchain4j数据源信息实体的必填字段
     *
     * @param entity Langchain4j数据源信息实体对象，包含数据源的相关配置信息
     */
    default void validateField(Langchain4jDataSourceInfo entity) {
        // 验证数据源信息实体中的必填字段
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getBaseUrl(), "baseUrl");
        if(!LangChain4jModel.Provider.OLLAMA.toString().equalsIgnoreCase(entity.getProvider())) {
            validateRequiredField(entity.getApiKey(), "apiKey");
        }
        validateRequiredField(entity.getModelName(), "modelName");
        validateJSON(entity.getOther(),"其它配置");
    }


}
