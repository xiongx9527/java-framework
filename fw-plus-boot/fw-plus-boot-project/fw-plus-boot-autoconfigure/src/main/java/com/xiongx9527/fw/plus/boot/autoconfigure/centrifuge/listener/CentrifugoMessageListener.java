package com.xiongx9527.fw.plus.boot.autoconfigure.centrifuge.listener;

/**
 * Centrifugo 消息监听器。
 */
public interface CentrifugoMessageListener {

    /**
     * 连接成功
     */
    void onConnected(String clientId);

    /**
     * 连接断开
     */
    void onDisconnected(int code, String reason);

    /**
     * 连接错误
     */
    void onError(Throwable error);

    /**
     * 收到消息
     */
    void onMessage(Object subscription, byte[] data);

    /**
     * 客户端加入频道
     */
    default void onJoin(String channel, Object info) {
    }

    /**
     * 客户端离开频道
     */
    default void onLeave(String channel, Object info) {
    }
}
