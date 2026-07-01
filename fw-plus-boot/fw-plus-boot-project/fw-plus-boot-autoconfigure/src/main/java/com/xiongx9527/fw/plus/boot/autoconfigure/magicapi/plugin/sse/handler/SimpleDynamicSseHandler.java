package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.service.SseSubscribeInfoMagicResourceStorage;
import lombok.extern.slf4j.Slf4j;


@Slf4j
public class SimpleDynamicSseHandler extends BaseDynamicSseHandler {

    /**
     * 构造函数，初始化SimpleDynamicSseHandler实例
     * @param magicDataSourceStorage 数据源信息存储器，用于管理SSE数据源相关信息
     * @param magicSubscribeStorage 订阅信息存储器，用于管理SSE订阅相关信息
     */
    public SimpleDynamicSseHandler(SseDataSourceInfoMagicResourceStorage magicDataSourceStorage, SseSubscribeInfoMagicResourceStorage magicSubscribeStorage) {
        super(magicDataSourceStorage,magicSubscribeStorage);
    }

    /**
     * 向指定应用的除排除服务器外的所有客户端广播消息
     * @param excludedServerId 需要排除的服务器ID，该服务器下的客户端不会收到消息
     * @param appId 应用ID，指定消息发送的应用范围
     * @param clientId 客户端ID，指定消息发送的具体客户端
     * @param message 要广播的消息内容
     */
    @Override
    void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String eventName,String message) {

    }

    /**
     * 向指定应用的除排除服务器外的所有客户端广播消息
     * @param excludedServerId 需要排除的服务器ID，该服务器下的客户端不会收到消息
     * @param appId 应用ID，指定消息发送的应用范围
     * @param message 要广播的消息内容
     */
    @Override
    void broadcastMessageToAllClient(String excludedServerId, String appId, String eventName,String message) {

    }

}
