package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.module;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.sse.handler.BaseDynamicSseHandler;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.ssssssss.magicapi.core.annotation.MagicModule;
import org.ssssssss.magicapi.core.model.Options;
import org.ssssssss.magicapi.modules.DynamicModule;
import org.ssssssss.script.MagicScriptContext;
import org.ssssssss.script.functions.DynamicAttribute;

@Slf4j
@MagicModule("sse")
public class SseModule implements DynamicAttribute<SseModule, SseModule> , DynamicModule<SseModule>, BaseService {

    /**
     * SSE模块类，用于处理与SSE（Server-Sent Events）相关的操作。
     * 包括消息广播、Token生成等功能。
     */
    private final BaseDynamicSseHandler baseDynamicSseHandler;

    /**
     * 模块标识key，用于区分不同的数据源或应用。
     */
    private final String key;

    /**
     * 构造函数，初始化SseModule实例。
     *
     * @param baseDynamicSseHandler 基础动态SSE处理器
     * @param key                   模块标识key
     */
    public SseModule(BaseDynamicSseHandler baseDynamicSseHandler, String key) {
        this.baseDynamicSseHandler = baseDynamicSseHandler;
        this.key = key;
    }

    /**
     * 向指定客户端广播消息。
     *
     * @param clientId 客户端唯一标识符
     * @param eventName 事件名称，用于标识消息类型
     * @param message 要发送的消息内容
     */
    public void broadcastMessageToClient(String clientId , String eventName,String message) {
        this.baseDynamicSseHandler.broadcastMessageToClient(key, clientId, eventName, message);
    }

    /**
     * 向所有客户端广播分布式消息。
     *
     * @param eventName 事件名称，用于标识消息类型
     * @param message 要发送的消息内容
     */
    public void broadcastDistributedMessageToAllClient(String eventName,String message) {
        this.baseDynamicSseHandler.broadcastDistributedMessageToAllClient(key, eventName, message);
    }

    /**
     * 建立 SSE 连接。
     * 委托给底层处理器进行身份验证和连接建立，返回 SSE 发射器供客户端使用。
     *
     * @param appId 应用唯一标识符
     * @param secret 应用密钥，用于身份验证
     * @param clientId 客户端唯一标识符
     * @return SseEmitter 实例，用于向客户端发送事件
     */
    public SseEmitter connect(String appId,String secret, String clientId) {
        SseEmitter emitter = this.baseDynamicSseHandler.connect(appId, secret, clientId);
        return emitter;
    }

    /**
     * 处理客户端发送的消息。
     * 将会话 ID 和 JSON 消息转发给底层处理器处理。
     *
     * @param sessionId 会话唯一标识符
     * @param message JSON 格式的消息内容，将被解析为 JsonRpcMessage 对象
     * @return 处理结果 ResponseEntity，状态码表示处理成功或失败
     */
    public ResponseEntity<Void> message(String sessionId ,String message) {
        return this.baseDynamicSseHandler.message(sessionId, JSON.parseObject(message, BaseDynamicSseHandler.JsonRpcMessage.class));
    }

    /**
     * 获取动态模块实例。
     *
     * @param context 脚本上下文对象
     * @return 返回当前实例或基于上下文创建的新实例
     */
    @Override
    public SseModule getDynamicModule(MagicScriptContext context) {
        String dataSourceKey = context.getString(Options.DEFAULT_DATA_SOURCE.getValue());
        if (StringUtils.isEmpty(dataSourceKey)) {
            return this;
        }
        return new SseModule(this.baseDynamicSseHandler,dataSourceKey);
    }


    /**
     * 根据指定key获取动态属性模块实例。
     *
     * @param key 属性key
     * @return 基于指定key的新SseModule实例
     */
    @Override
    public SseModule getDynamicAttribute(String key) {
        return new SseModule(this.baseDynamicSseHandler,key);
    }

}
