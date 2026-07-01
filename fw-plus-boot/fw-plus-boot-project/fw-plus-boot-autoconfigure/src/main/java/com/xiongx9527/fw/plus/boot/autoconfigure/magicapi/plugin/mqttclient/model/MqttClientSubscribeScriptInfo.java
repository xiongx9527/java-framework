
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model;

import lombok.Getter;

@Getter
public class MqttClientSubscribeScriptInfo {

    private final MqttClientSubscribeInfo subscribeInfo;
    private final String scriptName;

    public MqttClientSubscribeScriptInfo(MqttClientSubscribeInfo subscribeInfo, String scriptName) {
        this.subscribeInfo = subscribeInfo;
        this.scriptName = scriptName;
    }

    public String getQos() {
        return subscribeInfo.getQos();
    }

    public String getKey() {
        return subscribeInfo.getKey();
    }

    public String getTopic() {
        return subscribeInfo.getTopic();
    }

    public boolean isEnabled() {
        return subscribeInfo.isEnabled();
    }

    public String getOther() {
        return subscribeInfo.getOther();
    }
}