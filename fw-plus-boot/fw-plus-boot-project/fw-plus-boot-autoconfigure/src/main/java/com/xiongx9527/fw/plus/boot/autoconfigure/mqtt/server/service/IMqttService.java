package com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.service;

import com.xiongx9527.fw.plus.boot.autoconfigure.mqtt.server.model.ServerNode;

import java.util.List;

public interface IMqttService {
    /**
     * 获取服务节点
     *
     * @return 服务节点集合
     */
    List<ServerNode> getNodes();

    /**
     * 获取所有在线的客户端
     *
     * @return 在线数
     */
    long getOnlineClientSize();

    /**
     * 获取所有在线的客户端
     *
     * @return 客户端集合
     */
    List<String> getOnlineClients();

    /**
     * 获取所有在线的客户端
     *
     * @param nodeName 集群节点
     * @return 在线数
     */
    long getOnlineClientSize(String nodeName);

    /**
     * 获取所有在线的客户端
     *
     * @param nodeName 集群节点
     * @return 客户端集合
     */
    List<String> getOnlineClients(String nodeName);
}
