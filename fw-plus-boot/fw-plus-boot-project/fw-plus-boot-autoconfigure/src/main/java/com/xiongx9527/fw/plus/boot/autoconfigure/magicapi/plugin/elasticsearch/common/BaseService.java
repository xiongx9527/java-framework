package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.ElasticSearchDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;

public interface BaseService extends MagicBaseService {

    /**
     * 校验字段
     *
     * @param entity
     */
    default void validateField(ElasticSearchDataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getHostname(), "hostname");
        validateRequiredField(entity.getPort(), "port");
    }


}
