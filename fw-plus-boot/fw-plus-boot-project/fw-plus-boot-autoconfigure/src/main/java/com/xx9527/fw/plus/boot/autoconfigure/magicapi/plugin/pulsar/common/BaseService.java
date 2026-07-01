package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.common;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicAPIConfigUtils;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.pulsar.model.PulsarSubscribeInfo;

public interface BaseService extends MagicBaseService {
    /**
     * 生成Topic的完整路径
     *
     * 该方法用于根据租户、命名空间和Topic名称生成一个完整的Topic路径
     * 生成的路径格式为：persistent://tenant/namespace/topic
     *
     * @param tenant 租户名称，用于区分不同的租户
     * @param namespace 命名空间名称，用于在租户内部进一步组织Topic
     * @param topic Topic名称，指定在命名空间内的唯一标识
     * @return 返回完整的Topic路径，用于在消息系统中唯一标识一个Topic
     */
    default String createTopicName(String tenant,String namespace,String topic) {
        return "persistent://" + tenant + "/" + namespace + "/" + topic;
    }

    /**
     * 检查Pulsar订阅是否启用
     * <p>
     * 该方法首先从实体对象中获取启用状态，然后尝试从配置中心获取对应的配置值进行覆盖。
     * 如果配置获取失败，则默认返回false。
     * </p>
     *
     * @param entity Pulsar订阅信息实体，包含订阅的Key、Topic等配置信息
     * @return boolean 订阅是否启用，true表示启用，false表示禁用
     */
    default boolean enable(PulsarSubscribeInfo entity) {
        boolean enable = entity.isEnabled();
        String enableStr = String.valueOf(enable);
        try {
            enableStr = MagicAPIConfigUtils.getPulsarSubscribeConfig(entity.getKey(), entity.getTopic(), "enabled", enableStr);
            enable = Boolean.parseBoolean(enableStr);
        } catch (Exception e) {
            enable = false;
        }
        return enable;
    }


}
