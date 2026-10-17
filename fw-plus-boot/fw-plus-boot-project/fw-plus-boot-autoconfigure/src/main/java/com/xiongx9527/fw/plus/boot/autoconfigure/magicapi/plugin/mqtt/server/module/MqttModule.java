package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model.BaseMagicDynamicMqttDataSource;
import lombok.extern.slf4j.Slf4j;
import org.dromara.mica.mqtt.core.server.MqttServer;
import org.dromara.mica.mqtt.spring.server.MqttServerTemplate;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Slf4j
@MagicModule("mqttServer")
public class MqttModule implements DynamicAttribute<MqttModule, MqttModule>, DynamicModule<MqttModule> {

    /**
     * 内置 MQTT 服务端动态数据源，用于按 Magic-API 资源配置管理多个 MqttServer。
     */
    private final BaseMagicDynamicMqttDataSource magicDynamicMqttDataSource;

    /**
     * 当前模块绑定的 MQTT 服务端实例；默认模块可能为空，按 key 切换后绑定具体服务端。
     */
    private final MqttServer mqttServer;

    public MqttModule(BaseMagicDynamicMqttDataSource magicDynamicMqttDataSource, MqttServer mqttServer) {
        this.magicDynamicMqttDataSource = magicDynamicMqttDataSource;
        this.mqttServer = mqttServer;
    }


    /**
     * 获取当前绑定服务端的模板。
     * 调用方需要确保当前模块已通过默认服务端或动态 key 绑定到 MqttServer。
     */
    public MqttServerTemplate getMqttServerTemplate() {
        return new MqttServerTemplate(this.mqttServer);
    }

    @Override
    public MqttModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        return new MqttModule(this.magicDynamicMqttDataSource, this.magicDynamicMqttDataSource.getServer(dataSourceKey).getMqttServer());
    }


    @Override
    public MqttModule getDynamicAttribute(String key) {
        return new MqttModule(this.magicDynamicMqttDataSource, this.magicDynamicMqttDataSource.getServer(key).getMqttServer());
    }
}
