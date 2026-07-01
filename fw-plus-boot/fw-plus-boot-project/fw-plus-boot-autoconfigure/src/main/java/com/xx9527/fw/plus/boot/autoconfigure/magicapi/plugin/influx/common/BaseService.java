package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.common;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.influx.model.InfluxDataSourceInfo;

public interface BaseService extends MagicBaseService {

    /**
     * 校验字段
     *
     * @param entity
     */
    default void validateField(InfluxDataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getUrl(), "url");
        validateRequiredField(entity.getPassword(), "password");
    }


}
