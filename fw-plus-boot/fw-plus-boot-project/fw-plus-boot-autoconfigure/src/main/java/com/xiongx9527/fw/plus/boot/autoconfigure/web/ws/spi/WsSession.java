package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi;

import org.springframework.web.socket.CloseStatus;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * 与传输无关的 WebSocket 会话抽象。
 *
 * <p>webmvc（Servlet 栈）与 webflux（响应式栈）的 {@code WebSocketSession} 是两个完全无关的类型，
 * 地基若直接依赖其中任何一个都无法同时支持两种栈。这里抽出两者共有的能力，
 * 由各栈提供适配器，使会话管理、广播、脚本派发等逻辑与传输方式解耦。</p>
 *
 * <p>需要访问栈特有能力的实现方（例如 Servlet 栈的 {@code getRemoteAddress}）
 * 可通过 {@link #unwrap(Class)} 取得原生会话对象。</p>
 *
 * @author zzy
 */
public interface WsSession {

	/**
	 * 会话唯一标识。
	 *
	 * @return 会话 ID
	 */
	String getId();

	/**
	 * 会话是否仍然打开。
	 *
	 * @return true 表示打开
	 */
	boolean isOpen();

	/**
	 * 会话属性表，业务侧可用来在一次连接的多次调用之间传递数据。
	 *
	 * @return 属性表
	 */
	Map<String, Object> getAttributes();

	/**
	 * 握手请求的 URI，用于解析 token、clientId 等查询参数。
	 *
	 * @return 握手 URI
	 */
	URI getUri();

	/**
	 * 发送文本消息。
	 *
	 * @param message 文本内容
	 * @throws IOException 发送失败时抛出
	 */
	void sendText(String message) throws IOException;

	/**
	 * 发送二进制消息。
	 *
	 * @param payload 二进制内容
	 * @throws IOException 发送失败时抛出
	 */
	void sendBinary(byte[] payload) throws IOException;

	/**
	 * 以指定状态关闭会话。
	 *
	 * @param status 关闭状态
	 * @throws IOException 关闭失败时抛出
	 */
	void close(CloseStatus status) throws IOException;

	/**
	 * 取得底层栈的原生会话对象。
	 *
	 * @param type 期望的原生类型
	 * @param <T>  原生类型
	 * @return 原生会话，类型不匹配时返回 null
	 */
	<T> T unwrap(Class<T> type);
}
