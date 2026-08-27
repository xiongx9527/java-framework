package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.service.MqttSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.codec.MqttPublishMessage;
import net.dreamlu.iot.mqtt.codec.MqttQoS;
import net.dreamlu.iot.mqtt.core.server.event.IMqttMessageListener;
import org.apache.commons.collections4.ListUtils;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;
import org.tio.core.ChannelContext;

import java.util.List;
import java.util.stream.Collectors;


@Slf4j
public class MagicAPIMqttMessageListener implements IMqttMessageListener {

    /**
     * 私有的final字符串变量，用于存储键值
     */
    private final String key ;


    /**
     * 构造函数，初始化MagicAPIMqttMessageListener实例
     * @param key 用于标识监听器的键值
     */
    public MagicAPIMqttMessageListener(String key) {
        this.key = key;
    }

    /**
     * MQTT消息回调处理方法，当接收到MQTT消息时被调用
     * @param context 通道上下文对象
     * @param clientId 客户端ID
     * @param topic 消息主题
     * @param qoS 服务质量等级
     * @param message MQTT发布消息对象
     */
    @Override
    public void onMessage(ChannelContext context, String clientId, String topic, MqttQoS qoS, MqttPublishMessage message) {
        List<MqttSubscribeInfo> subscribeInfoList = subscribeInfos(key, topic);
        // 遍历订阅信息列表，执行相应的脚本
        for (MqttSubscribeInfo subscribeInfo : subscribeInfoList) {
            try {
                // 创建脚本执行上下文
                MagicScriptContext magicScriptContext = new MagicScriptContext();
                magicScriptContext.setScriptName(subscribeInfo.getName());
                // 设置脚本变量
                magicScriptContext.set("key", key);
                magicScriptContext.set("context", context);
                magicScriptContext.set("clientId", clientId);
                magicScriptContext.set("topic", topic);
                magicScriptContext.set("qoS", qoS);
                magicScriptContext.set("message", message);
                // 执行脚本并记录结果
                Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
                log.info("Result of subscribe message: {}", result);
            } catch (Exception e) {
                log.error("Error executing script: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 根据键值和主题获取匹配的MQTT订阅信息列表
     * @param key 订阅信息的键值
     * @param topic 订阅的主题
     * @return 匹配条件的MQTT订阅信息列表
     */
    private List<MqttSubscribeInfo> subscribeInfos(String key, String topic) {
        List<MqttSubscribeInfo> mqttSubscribeInfos = ListUtils.emptyIfNull(SpringUtils.getBean(MqttSubscribeInfoMagicResourceStorage.class).findAll()) ;
        return mqttSubscribeInfos.stream().filter(mqttSubscribeInfo -> mqttSubscribeInfo.getKey().equals(key)
                && mqttSubscribeInfo.getTopic().equals(topic)).collect(Collectors.toList());
    }

}
