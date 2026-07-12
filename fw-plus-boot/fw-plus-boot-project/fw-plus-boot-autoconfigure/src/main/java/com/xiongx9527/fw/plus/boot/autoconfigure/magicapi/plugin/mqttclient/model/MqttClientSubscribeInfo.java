
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model;

import lombok.Data;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;

@Data
public class MqttClientSubscribeInfo extends PathMagicEntity {

    private String key;

    private String topic;

    private String qos;

    private boolean enabled;

    private String params;

    private String description;

    private String other;

    @Override
    public MagicEntity simple() {
        MqttClientSubscribeInfo info = new MqttClientSubscribeInfo();
        super.simple(info);
        info.setKey(key);
        info.setQos(qos);
        info.setEnabled(enabled);
        info.setTopic(topic);
        info.setParams(params);
        info.setDescription(description);
        info.setOther(other);
        info.setPath(null);
        return info;
    }

    @Override
    public MqttClientSubscribeInfo copy() {
        MqttClientSubscribeInfo info = new MqttClientSubscribeInfo();
        super.copyTo(info);
        info.setKey(key);
        info.setQos(qos);
        info.setEnabled(enabled);
        info.setTopic(topic);
        info.setParams(params);
        info.setDescription(description);
        info.setOther(other);
        info.setPath(null);
        return info;
    }
}