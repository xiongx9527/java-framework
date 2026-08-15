package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import lombok.Data;

@Data
public class MqttDashboardSubscriptionRequest {

    private String clientId;
    private String topic;
    private int qos;
}
