
package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.common.MagicBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.model.MqttClientDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.client.starter.MagicAPIMqttClientConfig;
import com.xiongx9527.fw.plus.common.util.SpringUtils;

import java.util.Map;

public interface MqttClientBaseService extends MagicBaseService {

    default void validateField(MqttClientDataSourceInfo entity) {
        validateRequiredField(entity.getName(), "name");
        validateRequiredField(entity.getKey(), "key");
        validateRequiredField(entity.getIp(), "ip");
        validateRequiredField(entity.getPort(), "port");
    }

    default Map<String, Object> getConfig() {
        return SpringUtils.getBean(MagicAPIMqttClientConfig.class);
    }
}
