package com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.service.impl;

import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.model.ServerNode;
import com.xx9527.fw.plus.boot.autoconfigure.mqtt.server.service.IMqttService;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import net.dreamlu.iot.mqtt.spring.server.event.MqttClientOfflineEvent;
import net.dreamlu.iot.mqtt.spring.server.event.MqttClientOnlineEvent;
import net.dreamlu.mica.core.utils.CharPool;
import net.dreamlu.mica.core.utils.INetUtil;
import org.springframework.context.event.EventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class MqttServiceImpl implements IMqttService {

    /**
     * 存储在线客户端信息的线程安全映射表
     *
     * 该字段用于维护当前在线客户端的集合，使用ConcurrentHashMap确保线程安全
     * key: 客户端唯一标识符
     * value: 客户端相关信息
     */
    private final Map<String, String> onlineClients = new java.util.concurrent.ConcurrentHashMap<>();

    /** 节点名称，标识当前节点的唯一名称 */
    private final String nodeName;

    /** 主机名称，表示当前服务部署的主机名 */
    private final String hostName;

    /**
     * RedisMqttServerManage构造函数
     *
     * @param mqttServer MQTT服务器实例，用于获取服务器创建者信息
     */
    public MqttServiceImpl(MqttServer mqttServer) {
        this.nodeName = mqttServer.getServerCreator().getNodeName();
        this.hostName = INetUtil.getHostIp() + CharPool.COLON + mqttServer.getServerCreator().getPort();
    }

    /**
     * 获取服务器节点列表
     *
     * @return 包含当前服务器节点信息的列表
     */
    @Override
    public List<ServerNode> getNodes() {
        ServerNode node = new ServerNode(this.nodeName, this.hostName);
        return Collections.singletonList(node);
    }

    /**
     * 获取在线客户端总数
     *
     * @return 当前在线客户端的数量
     */
    @Override
    public long getOnlineClientSize() {
        return this.onlineClients.size();
    }

    /**
     * 获取所有在线客户端ID列表
     *
     * @return 包含所有在线客户端ID的列表
     */
    @Override
    public List<String> getOnlineClients() {
        return new ArrayList<>(this.onlineClients.keySet());
    }

    /**
     * 根据节点名称获取在线客户端数量
     *
     * @param nodeName 节点名称
     * @return 指定节点上的在线客户端数量
     */
    @Override
    public long getOnlineClientSize(String nodeName) {
        return getOnlineClients().size();
    }

    /**
     * 根据节点名称获取在线客户端列表
     *
     * @param nodeName 节点名称
     * @return 指定节点上的在线客户端ID列表
     */
    @Override
    public List<String> getOnlineClients(String nodeName) {
        return getOnlineClients();
    }

    /**
     * 处理MQTT客户端上线事件
     *
     * @param onlineEvent MQTT客户端上线事件对象
     */
    @EventListener
    public void handleMqttClientOnlineEvent(MqttClientOnlineEvent onlineEvent) {
        // 处理MQTT连接事件
        this.onlineClients.put(onlineEvent.getClientId(), onlineEvent.getUsername());
    }

    /**
     * 处理MQTT客户端下线事件
     *
     * @param offlineEvent MQTT客户端下线事件对象
     */
    @EventListener
    public void handleMqttClientOfflineEvent(MqttClientOfflineEvent offlineEvent) {
        // 处理MQTT连接事件
        this.onlineClients.remove(offlineEvent.getClientId());
    }

}
