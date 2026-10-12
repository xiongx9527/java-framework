package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler.WsHandshakeInterceptor;

/**
 * MagicAPI握手拦截器接口
 *
 * <p>继承自 WS 地基的 {@link WsHandshakeInterceptor}（其自身继承 Spring 的
 * {@code HandshakeInterceptor}），因此实现类的注册方式与自定义逻辑完全不变；
 * 地基的端点注册器会统一收集本接口的实现并挂到握手链上。</p>
 *
 * <p>需要实现 {@code HandshakeInterceptor} 定义的抽象方法：</p>
 * <ul>
 *     <li>beforeHandshake: 在握手之前执行的逻辑</li>
 *     <li>afterHandshake: 在握手之后执行的逻辑</li>
 * </ul>
 */
public interface MagicAPIHandshakeInterceptor extends WsHandshakeInterceptor {
}
