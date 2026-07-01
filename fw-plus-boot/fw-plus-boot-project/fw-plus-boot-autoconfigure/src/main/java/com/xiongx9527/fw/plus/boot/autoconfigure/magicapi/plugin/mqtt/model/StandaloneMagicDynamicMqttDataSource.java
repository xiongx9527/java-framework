package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageDownReceiver;
import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.broker.cluster.RedisMqttMessageExchangeReceiver;
import com.xiongx9527.fw.plus.common.util.SpringUtils;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.core.server.auth.IMqttServerAuthHandler;
import net.dreamlu.iot.mqtt.core.server.broker.DefaultMqttBrokerDispatcher;
import net.dreamlu.iot.mqtt.core.server.cluster.MqttClusterMessageListener;
import net.dreamlu.iot.mqtt.core.server.dispatcher.IMqttMessageDispatcher;
import net.dreamlu.iot.mqtt.core.server.session.IMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.session.InMemoryMqttSessionManager;
import net.dreamlu.iot.mqtt.core.server.store.IMqttMessageStore;
import net.dreamlu.iot.mqtt.core.server.store.InMemoryMqttMessageStore;

public class StandaloneMagicDynamicMqttDataSource extends BaseMagicDynamicMqttDataSource {


    /**
     * 获取MagicAPI MQTT服务器唯一ID服务实例
     *
     * @param key 用于标识MQTT服务器的键值
     * @return 返回一个新的MagicAPIMqttServerUniqueIdService实例
     */
    @Override
    public MagicAPIMqttServerUniqueIdService getMagicAPIMqttServerUniqueIdService(String key) {
        return new MagicAPIMqttServerUniqueIdService(key);
    }

    /**
     * 获取MagicAPI MQTT连接状态监听器实例
     *
     * @param key 用于标识监听器的键值
     * @return 返回一个新的MagicAPIMqttConnectStatusListener实例
     */
    @Override
    public MagicAPIMqttConnectStatusListener getMagicAPIMqttConnectStatusListener(String key) {
        return new MagicAPIMqttConnectStatusListener(key);
    }


    /**
     * 获取MQTT集群消息监听器
     *
     * @param mqttServer MQTT服务器实例
     * @return MQTT集群消息监听器实例，当前实现返回null
     */
    @Override
    public MqttClusterMessageListener getMqttClusterMessageListener(MqttServer mqttServer) {
        return null;
    }


    /**
     * 获取MQTT消息交换接收器
     *
     * @param mqttServer MQTT服务器实例
     * @param key 接收器标识键
     * @return RedisMqttMessageDownReceiver MQTT消息下行接收器实例，当前实现返回null
     */
    @Override
    public RedisMqttMessageDownReceiver getMqttMessageExchangeReceiver(MqttServer mqttServer, String key) {
        return null;
    }

    /**
     * 获取MQTT消息下行接收器
     *
     * @param mqttServer MQTT服务器实例
     * @param key 接收器标识键
     * @return RedisMqttMessageExchangeReceiver MQTT消息交换接收器实例，当前实现返回null
     */
    @Override
    public RedisMqttMessageExchangeReceiver getMqttMessageDownReceiver(MqttServer mqttServer, String key) {
        return null;
    }

    /**
     * 获取MQTT服务器认证处理器
     *
     * @param key 认证处理器的键值标识
     * @return 返回IMqttServerAuthHandler认证处理器实例，如果获取失败则返回null
     */
    @Override
    public IMqttServerAuthHandler getIMqttServerAuthHandler(String key) {
        try {
            // 从Spring容器中获取IMqttServerAuthHandler类型的Bean实例
            return SpringUtils.getBean(IMqttServerAuthHandler.class);
        } catch (Exception e) {
            // 捕获异常，避免因获取Bean失败导致程序中断
        }
        return null;
    }

    /**
     * 获取MQTT消息分发器实例
     *
     * @param key 分发器标识键值
     * @return MQTT消息分发器实例
     */
    @Override
    public IMqttMessageDispatcher getIMqttMessageDispatcher(String key) {
        // 创建并返回一个匿名内部类实现的MQTT消息分发器
        return new DefaultMqttBrokerDispatcher();
    }


    /**
     * 根据指定的键获取MQTT会话管理器实例
     *
     * @param key 用于获取会话管理器的键值
     * @return 返回一个新的内存MQTT会话管理器实例
     */
    @Override
    public IMqttSessionManager getIMqttSessionManager(String key) {
        return new InMemoryMqttSessionManager();
    }

    /**
     * 根据指定的键获取MQTT消息存储实例
     *
     * @param key 用于获取消息存储的键值
     * @return 返回一个新的内存MQTT消息存储实例
     */
    @Override
    public IMqttMessageStore getIMqttMessageStore(String key) {
        return new InMemoryMqttMessageStore();
    }

}
