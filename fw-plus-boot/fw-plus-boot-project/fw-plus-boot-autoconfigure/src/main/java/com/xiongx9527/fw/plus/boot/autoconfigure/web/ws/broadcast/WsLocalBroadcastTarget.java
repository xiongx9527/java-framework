package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.broadcast;

/**
 * 本地广播投递目标。
 *
 * <p>Redis 广播监听器位于 {@code web.ws.redis} 包，与持有会话的
 * {@code web.ws.handler.AbstractWebSocketHandler} 不共包，无法直接调用其包私有的
 * {@code broadcastMessageToClientLocalOnly} / {@code broadcastMessageToAllClientLocalOnly}
 * （这两个方法需保持包私有可覆写）。因此由处理器侧实现本接口并注入监听器，
 * 使监听器触发的本地投递仍然经过处理器可覆写的方法，不扩大处理器的可见性。</p>
 *
 * @author zzy
 */
public interface WsLocalBroadcastTarget {

	/**
	 * 向本实例上指定客户端投递消息，不再回发 Redis。
	 *
	 * @param appId    应用 ID
	 * @param clientId 客户端 ID
	 * @param message  消息内容
	 * @return true 表示至少成功发送一条（同步模式）或任务已被接受（异步模式）
	 */
	boolean sendToClient(String appId, String clientId, String message);

	/**
	 * 向本实例上指定应用的全部客户端投递消息，不再回发 Redis。
	 *
	 * @param appId   应用 ID
	 * @param message 消息内容
	 */
	void sendToAllClients(String appId, String message);
}
