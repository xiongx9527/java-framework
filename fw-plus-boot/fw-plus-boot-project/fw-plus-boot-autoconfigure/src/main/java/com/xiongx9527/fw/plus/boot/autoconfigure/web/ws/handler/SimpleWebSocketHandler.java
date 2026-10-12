package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import lombok.extern.slf4j.Slf4j;

/**
 * 不支持 Redis 的简单 WebSocket 处理器：分布式广播为空实现（单机部署使用）。
 *
 * @author zzy
 */
@Slf4j
public class SimpleWebSocketHandler extends AbstractWebSocketHandler {

    public SimpleWebSocketHandler(WsConfig wsConfig) {
        super(wsConfig);
    }

    @Override
    void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message) {

    }

    @Override
    void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {

    }
}
