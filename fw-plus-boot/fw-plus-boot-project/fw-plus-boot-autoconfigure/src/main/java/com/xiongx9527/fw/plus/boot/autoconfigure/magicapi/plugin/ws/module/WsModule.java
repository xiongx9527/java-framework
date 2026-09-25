package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.module;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.AbstractWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

import java.util.Date;
import java.util.List;
import java.util.Objects;

@Slf4j
@MagicModule("ws")
public class WsModule implements DynamicAttribute<WsModule, WsModule> , DynamicModule<WsModule>, BaseService {

    /**
     * WebSocket处理器
     */
    private final AbstractWebSocketHandler webSocketHandler;

    /**
     * 数据源键名
     */
    private final String key;

    /**
     * WebSocket模块构造函数
     *
     * @param webSocketHandler WebSocket处理器
     * @param key 数据源键名
     */
    public WsModule(AbstractWebSocketHandler webSocketHandler, String key) {
        this.webSocketHandler = webSocketHandler;
        this.key = key;
    }

    /**
     * 向指定客户端广播消息
     *
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    public void broadcastMessageToClient(String clientId , String message) {
        this.webSocketHandler.broadcastMessageToClient(key, clientId, message);
    }

    /**
     * 向所有客户端广播分布式消息
     *
     * @param message 要广播的消息内容
     */
    public void broadcastDistributedMessageToAllClient(String message) {
        this.webSocketHandler.broadcastDistributedMessageToAllClient(key, message);
    }

    /**
     * 获取动态WebSocket模块
     *
     * @param context MagicScript上下文对象
     * @return WebSocket模块实例，如果未指定数据源则返回当前实例，否则返回新的WsModule实例
     */
    @Override
    public WsModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        return new WsModule(this.webSocketHandler,dataSourceKey);
    }

    /**
     * 根据键名获取动态属性
     *
     * @param key 属性键名，用作数据源键
     * @return 新的WebSocket模块实例
     */
    @Override
    public WsModule getDynamicAttribute(String key) {
        return new WsModule(this.webSocketHandler,key);
    }

}
