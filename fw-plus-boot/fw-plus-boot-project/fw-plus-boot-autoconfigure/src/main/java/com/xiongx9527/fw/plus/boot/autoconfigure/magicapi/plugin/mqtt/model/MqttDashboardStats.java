package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.Map;

@Data
public class MqttDashboardStats {

    private Map<String, Long> connections = new LinkedHashMap<String, Long>();
    private Map<String, Long> messages = new LinkedHashMap<String, Long>();
    private Map<String, Long> nodes = new LinkedHashMap<String, Long>();
    private long sampledAt;
}
