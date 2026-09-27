package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.server.model.MqttSubscribeInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.ssssssss.magicapi.core.event.FileEvent;
import org.ssssssss.magicapi.core.event.GroupEvent;
import org.ssssssss.magicapi.core.service.AbstractMagicDynamicRegistry;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;

/**
 * Kafka 订阅 动态注册器
 *
 * @author zzy
 */
@Slf4j
public class MqttSubscribeMagicDynamicRegistry extends AbstractMagicDynamicRegistry<MqttSubscribeInfo> {

    /**
     * 构造函数，初始化MQTT订阅魔法动态注册器
     * @param magicResourceStorage 魔法资源存储器，用于存储MQTT订阅信息
     */
    public MqttSubscribeMagicDynamicRegistry(MagicResourceStorage<MqttSubscribeInfo> magicResourceStorage) {
        // 调用父类构造函数初始化魔法资源存储
        super(magicResourceStorage);
    }

    /**
     * 处理文件事件的监听器，当事件类型为mqtt-subscribe时触发
     * @param event 文件事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'mqtt-subscribe'")
    public void onFileEvent(FileEvent event) {
        try {
            processEvent(event);
        } catch (Exception ignored) {
        }
    }

    /**
     * 处理组事件的监听器，当事件类型为mqtt-subscribe时触发
     * @param event 组事件对象，包含事件相关信息
     */
    @EventListener(condition = "#event.type == 'mqtt-subscribe'")
    public void onGroupEvent(GroupEvent event) {
            try {
               processEvent(event);
            } catch (Exception ignored) {
            }
    }

    /**
     * 注册MQTT订阅信息
     * @param entity MQTT订阅信息实体
     * @return 注册成功返回true，失败返回false
     */
    @Override
    public boolean register(MqttSubscribeInfo entity) {
        try {
            return super.register(entity);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注册映射节点中的MQTT订阅信息
     * @param mappingNode 映射节点，包含MQTT订阅信息
     * @return 注册成功返回true，失败返回false
     */
    @Override
    protected boolean register(MappingNode<MqttSubscribeInfo> mappingNode) {
        try {
            return super.register(mappingNode);
        } catch (Exception ignored) {
            return false;
        }
    }

    /**
     * 注销映射节点中的MQTT订阅信息
     * @param mappingNode 映射节点，包含需要注销的MQTT订阅信息
     */
    @Override
    protected void unregister(MappingNode<MqttSubscribeInfo> mappingNode) {
        try {
            super.unregister(mappingNode);
        } catch (Exception ignored) {
        }
    }


}
