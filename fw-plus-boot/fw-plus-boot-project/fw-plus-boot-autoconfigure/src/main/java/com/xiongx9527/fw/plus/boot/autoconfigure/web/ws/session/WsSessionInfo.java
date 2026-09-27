package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.session;

import lombok.Data;

/**
 * 挂在会话属性上的连接元信息。
 *
 * <p>键为 {@link #ATTRIBUTE_KEY}，业务侧可从 {@code WsSession#getAttributes()} 取出。</p>
 *
 * @author zzy
 */
@Data
public class WsSessionInfo {

	/**
	 * 会话属性中的键名。
	 */
	public static final String ATTRIBUTE_KEY = "sessionInfo";

	private final String appId;

	private final String clientId;

	/**
	 * 会话创建时间。
	 */
	private final long createTime;

	/**
	 * 最近一次收到入站消息的时间；任意文本、二进制、Ping、Pong 都会刷新，
	 * 只发业务消息从不发心跳的长连接不应被判空闲。
	 */
	private volatile long lastActiveTime;

	public WsSessionInfo(String appId, String clientId) {
		this.appId = appId;
		this.clientId = clientId;
		this.createTime = System.currentTimeMillis();
		this.lastActiveTime = this.createTime;
	}

	/**
	 * 把最近活跃时间刷新为当前时间。
	 */
	public void touch() {
		this.lastActiveTime = System.currentTimeMillis();
	}
}
