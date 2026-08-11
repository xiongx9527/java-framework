package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import lombok.Data;

@Data
public class MqttDashboardClientInfo {

    private String clientId;
    private String username;
    private long keepAlive;
    private boolean connected;
    private String protoName;
    private int protoVer;
    private String protoFullName;
    private boolean webSocket;
    private boolean ssl;
    private String ipAddress;
    private int port;
    private long connectedAt;
    private long createdAt;
}
