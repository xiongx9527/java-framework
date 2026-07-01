package com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.common;

import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.model.MilvusDataSourceInfo;
import com.xx9527.fw.plus.ai.autoconfigure.magicapi.plugin.milvus.starter.MagicAPIMilvueConfig;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.common.util.SpringUtils;

import java.util.Map;

public interface BaseService extends MagicBaseService {

    /**
     * 验证Milvus数据源信息实体的必填字段
     *
     * @param entity Milvus数据源信息实体对象，包含需要验证的字段信息
     */
    default void validateField(MilvusDataSourceInfo entity) {
        // 验证名称、密钥和URI等必填字段
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getUri(), "uri");
    }


    /**
     * 获取Milvus配置信息
     * 
     * @return Milvus配置信息映射
     */
    default Map<String, Object> getConfigMap() {
        return SpringUtils.getBean(MagicAPIMilvueConfig.class) ;
    }

    /**
     * 从配置中获取整型值
     *
     * @param configKey 配置键名
     * @param defaultValue 默认整型值，当配置中未找到对应键名或转换失败时使用
     * @return 从配置中获取并转换的整型值，如果获取失败则返回默认值
     */
    default long getConfigValueAsLong(String configKey, long defaultValue) {
        return getConfigValueAsLong(getConfigMap(), configKey, defaultValue);
    }

    /**
     * 获取Milvus客户端关闭时的最大等待时间（秒）
     * 从配置中获取milvus.client.close.timeout的值，如果未配置则返回默认值5秒
     *
     * @return 最大等待时间（秒）
     */
    default long getMaxWaitSeconds() {
        return getConfigValueAsLong("milvus.client.close.timeout", 5L);
    }


}
