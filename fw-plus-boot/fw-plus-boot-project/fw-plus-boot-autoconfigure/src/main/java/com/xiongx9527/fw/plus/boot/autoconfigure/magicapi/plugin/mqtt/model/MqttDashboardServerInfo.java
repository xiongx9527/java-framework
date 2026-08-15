package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import lombok.Data;

@Data
public class MqttDashboardServerInfo {

    private String id;
    private String key;
    private String name;
    private String ip;
    private int port;
    private int webPort;
    private boolean websocketEnabled;
    private boolean sslEnabled;
    private boolean running;
    private boolean defaultServer;
}
