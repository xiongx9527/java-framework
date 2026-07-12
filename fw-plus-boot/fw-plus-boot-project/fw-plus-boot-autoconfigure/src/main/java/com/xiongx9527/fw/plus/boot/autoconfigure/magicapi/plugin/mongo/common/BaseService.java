package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MongoDataSourceInfo;

public interface BaseService extends MagicBaseService {
    /**
     * 验证Debezium数据源配置信息的必填字段
     * 
     * @param entity Debezium数据源配置信息实体
     * @throws IllegalArgumentException 当任何必填字段为空或端口无效时抛出此异常
     */
    default void validateField(MongoDataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getHostname(), "hostname");
        validateRequiredField(entity.getPassword(), "password");
        validateRequiredField(entity.getPort(), "port");
        validateJSON(entity.getOther(), "other");
        validateRequiredField(entity.getMode(), "mode");
    }


}
