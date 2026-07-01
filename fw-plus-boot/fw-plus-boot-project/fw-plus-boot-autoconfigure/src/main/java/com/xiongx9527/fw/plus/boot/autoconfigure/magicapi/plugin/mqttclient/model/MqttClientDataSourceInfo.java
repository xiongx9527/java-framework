
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import org.ssssssss.magicapi.core.model.MagicEntity;

@Data
@EqualsAndHashCode(callSuper = true)
public class MqttClientDataSourceInfo extends MagicEntity {

    private String key;

    private String ip;

    private String port = "1883";

    private String clientId;

    private String username;

    private String password;

    private String other;

    @Override
    public MagicEntity simple() {
        MqttClientDataSourceInfo dataSourceInfo = new MqttClientDataSourceInfo();
        super.simple(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setIp(this.ip);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setClientId(this.clientId);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }

    @Override
    public MagicEntity copy() {
        MqttClientDataSourceInfo dataSourceInfo = new MqttClientDataSourceInfo();
        super.copyTo(dataSourceInfo);
        dataSourceInfo.setKey(this.key);
        dataSourceInfo.setIp(this.ip);
        dataSourceInfo.setPort(this.port);
        dataSourceInfo.setClientId(this.clientId);
        dataSourceInfo.setUsername(this.username);
        dataSourceInfo.setPassword(this.password);
        dataSourceInfo.setOther(this.other);
        return dataSourceInfo;
    }
}