package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.common;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.weaviate.model.WeaviateDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;

public interface BaseService extends MagicBaseService {

    /**
     * 验证Weaviate数据源信息实体的各个字段
     *
     * 此方法用于验证Weaviate数据源信息实体对象中的所有必要字段，
     * 包括名称(name)、密钥(key)、主机(host)和协议(scheme)。
     * 同时还会检查其他配置项是否为有效的JSON格式。
     *
     * @param entity Weaviate数据源信息实体对象，包含数据源的基本配置信息
     * @throws IllegalArgumentException 当任一必填字段为空或null时抛出
     * @throws IllegalArgumentException 当其他配置不是有效JSON格式时抛出
     */
    default void validateField(WeaviateDataSourceInfo entity) {
        // 验证必填字段：确保名称、密钥、主机和协议都不为空
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getHost(), "host");
        validateRequiredField(entity.getScheme(), "scheme");

        // 验证其它配置是否为有效的JSON格式
        // 如果配置内容不为空，则进行JSON格式校验
        validateJSON(entity.getOther(),"其它配置");
    }


}
