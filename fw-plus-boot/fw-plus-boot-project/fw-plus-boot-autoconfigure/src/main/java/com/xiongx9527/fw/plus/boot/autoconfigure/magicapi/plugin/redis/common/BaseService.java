package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceInfo;

public interface BaseService extends MagicBaseService {

    /**
     * 校验字段
     * @param entity
     */
    default void validateField(RedisDataSourceInfo entity) {
        com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.RedisDataSourceValidator.validate(entity);
    }


}
