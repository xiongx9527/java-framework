package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.module;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.BaseMagicDynamicMqttDataSource;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.spring.server.MqttServerTemplate;
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
     * MagicDynamicMqttDataSource实例，用于处理动态MQTT数据源相关的业务逻辑
     * 该数据源支持运行时动态配置和管理MQTT连接
     */
    private final BaseMagicDynamicMqttDataSource magicDynamicWeaviateDataSource;

    /**
     * MqttServer实例，用于管理MQTT服务器相关的配置和操作
     * 包括服务器的启动、停止以及连接管理等功能
     */
    private final MqttServer mqttServer;

    /**
     * MQTT模块构造函数
     *
     * @param magicDynamicWeaviateDataSource 动态MQTT数据源实例，用于处理数据存储和检索
     * @param mqttServer MQTT服务器实例，用于处理MQTT协议相关的通信
     */
    public MqttModule(BaseMagicDynamicMqttDataSource magicDynamicWeaviateDataSource, MqttServer mqttServer) {
        this.magicDynamicWeaviateDataSource = magicDynamicWeaviateDataSource;
        this.mqttServer = mqttServer;
    }


    /**
     * 获取MQTT服务器模板实例
     *
     * @return MqttServerTemplate MQTT服务器模板对象
     */
    public MqttServerTemplate getMqttServerTemplate() {
        return new MqttServerTemplate(this.mqttServer);
    }

    /**
     * 根据上下文获取动态MQTT模块
     *
     * @param context MagicScript上下文对象，用于获取数据源配置
     * @return MqttModule 动态MQTT模块实例
     */
    @Override
    public MqttModule getDynamicModule(MagicScriptContext context) {
        // 从上下文中获取数据源键值
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        // 如果数据源键值为空，返回当前模块实例
        if (StringUtils.isEmpty(dataSourceKey)) return this;
        // 根据数据源键值创建新的MQTT模块实例
        return new MqttModule(this.magicDynamicWeaviateDataSource, this.magicDynamicWeaviateDataSource.getServer(dataSourceKey).getMqttServer());
    }


    /**
     * 根据键值获取动态MQTT模块属性
     *
     * @param key 数据源键值
     * @return MqttModule 动态MQTT模块实例
     */
    @Override
    public MqttModule getDynamicAttribute(String key) {
        // 根据指定键值创建新的MQTT模块实例
        return new MqttModule(this.magicDynamicWeaviateDataSource, this.magicDynamicWeaviateDataSource.getServer(key).getMqttServer());
    }



}
