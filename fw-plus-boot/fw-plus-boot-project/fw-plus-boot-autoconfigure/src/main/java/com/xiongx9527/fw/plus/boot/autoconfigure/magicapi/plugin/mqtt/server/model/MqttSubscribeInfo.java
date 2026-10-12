package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

@Data
public class MqttSubscribeInfo extends PathMagicEntity {

    /**
     * 数据源Key
     */
    private String key;

    /**
     * 主题
     */
    private String topic;

    /**
     * 是否启用
     */
    private boolean enabled;


    /**
     * 描述信息字段
     * 用于存储对象的描述文本
     */
    private String description;


    /**
     * 创建一个简化版的MQTT订阅信息实体对象
     * <p>
     * 该方法通过复制当前对象的基本属性来创建一个新的MqttSubscribeInfo实例，
     * 包括key、enabled、topic、description等属性，并将path属性设置为null。
     * </p>
     *
     * @return 返回配置好的MqttSubscribeInfo实体对象
     */
    @Override
    public MagicEntity simple() {
        MqttSubscribeInfo info = new MqttSubscribeInfo();
        super.simple(info);
        info.setKey(key);
        info.setEnabled(enabled);
        info.setTopic(topic);
        info.setDescription(description);
        info.setPath(null);
        return info;
    }

    /**
     * 创建一个完整的MQTT订阅信息副本
     * <p>
     * 该方法通过深度复制当前对象的所有属性来创建一个新的MqttSubscribeInfo实例，
     * 包括key、enabled、topic、description等属性，并将path属性设置为null。
     * </p>
     *
     * @return 返回复制后的MqttSubscribeInfo实体对象
     */
    @Override
    public MqttSubscribeInfo copy() {
        MqttSubscribeInfo info = new MqttSubscribeInfo();
        super.copyTo(info);
        info.setKey(key);
        info.setEnabled(enabled);
        info.setTopic(topic);
        info.setDescription(description);
        info.setPath(null);
        return info;
    }


}
