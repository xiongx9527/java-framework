package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * MagicAPI握手拦截器接口
 *
 * 该接口继承自HandshakeInterceptor，用于在MagicAPI的WebSocket握手过程中进行拦截处理。
 * 实现该接口的类可以自定义握手前后的逻辑处理，如身份验证、权限检查等操作。
 *
 * 由于该接口继承自HandshakeInterceptor，因此需要实现其定义的抽象方法：
 * - beforeHandshake: 在握手之前执行的逻辑
 * - afterHandshake: 在握手之后执行的逻辑
 */
public interface MagicAPIHandshakeInterceptor extends HandshakeInterceptor {
}

