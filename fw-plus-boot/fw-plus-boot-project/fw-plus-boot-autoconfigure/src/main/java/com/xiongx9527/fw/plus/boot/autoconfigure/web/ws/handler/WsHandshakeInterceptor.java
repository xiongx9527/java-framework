package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * WebSocket 握手拦截器标记接口。
 *
 * <p>实现该接口的 Bean 会被自动收集，并按 Bean 名称注册到 WebSocket 端点握手上。
 * 常用于握手阶段做鉴权、租户识别、埋点等扩展。</p>
 *
 * @author zzy
 */
public interface WsHandshakeInterceptor extends HandshakeInterceptor {
}
