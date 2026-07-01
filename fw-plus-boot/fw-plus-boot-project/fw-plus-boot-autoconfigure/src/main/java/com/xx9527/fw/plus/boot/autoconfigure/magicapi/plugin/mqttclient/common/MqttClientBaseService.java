
package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.common;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.model.MqttClientDataSourceInfo;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqttclient.starter.MagicAPIMqttClientConfig;
import com.xx9527.fw.plus.common.util.SpringUtils;

import java.util.Map;

public interface MqttClientBaseService extends MagicBaseService {

    default void validateField(MqttClientDataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getIp(), "ip");
        validateRequiredField(entity.getPort(), "port");
        validateJSON(entity.getOther(), "其它配置");
    }

    default Map<String, Object> getConfig() {
        return SpringUtils.getBean(MagicAPIMqttClientConfig.class);
    }
}