package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi;

import com.alibaba.fastjson2.JSON;

/**
 * 结果回发句柄。
 *
 * <p>由 WebSocket 地基在构造 {@link WsInvocationContext} 时一并给出，业务侧在执行订阅脚本期间
 * 可直接通过它把数据推回当前会话，无需自己关心会话有效性校验与并发发送加锁。</p>
 *
 * <p>地基未持有会话（如初始化/关闭阶段无会话、或会话已失效）时，句柄为 {@code null}，
 * 业务侧需自行判空；此时应改用广播接口投递。</p>
 *
 * @author zzy
 */
public interface WsReplyHandle {

    /**
     * 向当前会话发送文本消息。
     *
     * @param message 文本内容
     */
    void sendText(String message);

    /**
     * 把对象序列化为 JSON 后发送。
     *
     * @param payload 待发送对象
     */
    default void sendJson(Object payload) {
        sendText(payload == null ? null : JSON.toJSONString(payload));
    }

    /**
     * 向当前会话发送二进制消息。
     *
     * <p>默认实现不支持二进制（例如只实现了文本通道的宿主），实现方可覆写。</p>
     *
     * @param payload 二进制内容
     */
    default void sendBinary(byte[] payload) {
        throw new UnsupportedOperationException("Binary reply is not supported by this handle");
    }
}
