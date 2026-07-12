package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.common;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.qdrant.model.QdrantDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;

public interface BaseService extends MagicBaseService {

    /**
     * 验证Qdrant数据源信息的必填字段
     * 
     * @param entity Qdrant数据源信息对象
     */
    default void validateField(QdrantDataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getHost(), "host");
        validateJSON(entity.getOther(),"其它配置");
    }

}
