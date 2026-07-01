package com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import lombok.extern.slf4j.Slf4j;

/**
 * 不支持Redis的简单动态WebSocket处理器
 */
@Slf4j
public class SimpleDynamicWebSocketHandler extends BaseDynamicWebSocketHandler {
    

    /**
     * 简单WebSocket处理器构造函数
     * 
     * @param magicDataSourceStorage WebSocket数据源信息存储
     * @param magicSubscribeStorage WebSocket订阅信息存储
     */
    public SimpleDynamicWebSocketHandler(WsDataSourceInfoMagicResourceStorage magicDataSourceStorage, WsSubscribeInfoMagicResourceStorage magicSubscribeStorage) {
        super(magicDataSourceStorage,magicSubscribeStorage);
    }

    @Override
    void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message) {

    }

    @Override
    void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {

    }

}
