package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.kafka.model.KafkaSubscribeInfo;

public interface BaseService extends MagicBaseService {
    /**
     * 检查Kafka订阅是否启用
     * <p>
     * 该方法首先从实体对象中获取启用状态，然后尝试从配置中心获取对应的配置值进行覆盖。
     * 如果配置获取失败，则默认返回false。
     * </p>
     *
     * @param entity Kafka订阅信息实体，包含订阅的Key、Topic等配置信息
     * @return boolean 订阅是否启用，true表示启用，false表示禁用
     */
    default boolean enable(KafkaSubscribeInfo entity) {
        boolean enable = entity.isEnabled();
        String enableStr = String.valueOf(enable);
        try {
            enableStr = MagicAPIConfigUtils.getKafkaSubscribeConfig(entity.getKey(), entity.getTopic(), "enabled", enableStr);
            enable = Boolean.parseBoolean(enableStr);
        } catch (Exception e) {
            enable = false;
        }
        return enable;
    }
}