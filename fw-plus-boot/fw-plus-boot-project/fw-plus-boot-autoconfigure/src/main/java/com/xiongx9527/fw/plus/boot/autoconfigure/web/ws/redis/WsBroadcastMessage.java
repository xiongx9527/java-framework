package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.redis;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 跨实例广播消息体，经 Redis 频道传输。
 *
 * <p>{@code excludedServerId} 用于让发布方实例在收到回声时跳过自身，避免消息放大循环；
 * {@code clientId} 为空表示广播给该 appId 下的全部客户端。</p>
 *
 * @author zzy
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WsBroadcastMessage {

	/**
	 * 需要排除的服务器 ID，该实例上的客户端不会收到本条广播。
	 */
	private String excludedServerId;

	/**
	 * 应用 ID。
	 */
	private String appId;

	/**
	 * 目标客户端 ID；为空表示广播全部客户端。
	 */
	private String clientId;

	/**
	 * 消息内容。
	 */
	private String message;
}
