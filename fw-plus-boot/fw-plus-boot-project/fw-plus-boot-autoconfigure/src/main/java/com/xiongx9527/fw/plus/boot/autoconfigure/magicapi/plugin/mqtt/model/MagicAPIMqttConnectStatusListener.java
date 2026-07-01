package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import lombok.extern.slf4j.Slf4j;
import net.dreamlu.iot.mqtt.core.server.event.IMqttConnectStatusListener;
import org.tio.core.ChannelContext;

@Slf4j
public class MagicAPIMqttConnectStatusListener implements IMqttConnectStatusListener {

    /**
     * MagicAPIMqttConnectStatusListener构造函数
     *
     * @param key 用于标识MQTT连接状态监听器的键值
     */
    private final String key ;

    /**
     * 构造函数，初始化MagicAPIMqttConnectStatusListener实例
     * @param key 用于标识监听器的键值
     */
    public MagicAPIMqttConnectStatusListener(String key) {
        this.key = key;
    }

    /**
     * MQTT客户端上线回调方法
     * 当MQTT客户端成功连接并上线时触发此方法，记录客户端上线日志信息
     * @param context 通道上下文对象
     * @param clientId MQTT客户端ID
     * @param username 连接用户名
     */
    @Override
    public void online(ChannelContext context, String clientId, String username) {
        log.info("key:{} Mqtt clientId:{} username:{} online.", key, clientId, username);
    }

    /**
     * MQTT客户端下线回调方法
     * 当MQTT客户端断开连接并下线时触发此方法，记录客户端下线日志信息及原因
     * @param context 通道上下文对象
     * @param clientId MQTT客户端ID
     * @param username 连接用户名
     * @param reason 客户端下线原因
     */
    @Override
    public void offline(ChannelContext context, String clientId, String username, String reason) {
        log.info("key:{} Mqtt clientId:{} username:{} offline reason:{}.", new Object[]{key,clientId, username, reason});
    }

}
