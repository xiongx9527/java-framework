package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.session;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSession;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * 与传输无关的会话注册表：唯一持有 clientId 到会话列表的映射。
 *
 * <p>存储的是 {@link WsSession} 抽象，webmvc 与 webflux 各自提供适配器，
 * 因此会话管理、广播与清理逻辑可以复用同一份实现。</p>
 *
 * <p>发送锁按 sessionId 串行化：两个栈的底层会话都不保证并发发送安全。</p>
 *
 * @author zzy
 */
@Slf4j
public class WsSessionRegistry {

	private final Cache<String, List<WsSession>> sessionsByClientId;

	private final Map<String, Object> sendLocks = new ConcurrentHashMap<>();

	/**
	 * @param maxClients       缓存中 clientId 的最大数量
	 * @param expireMinutes    写入过期时间
	 * @param concurrencyLevel 并发级别
	 * @param closeOnEviction  缓存驱逐时是否回调（交由调用方决定是否关闭会话）
	 * @param evictionHandler  驱逐回调，仅在 closeOnEviction 为 true 时触发
	 */
	public WsSessionRegistry(int maxClients, int expireMinutes, int concurrencyLevel,
							 boolean closeOnEviction, Consumer<WsSession> evictionHandler) {
		CacheBuilder<Object, Object> builder = CacheBuilder.newBuilder()
				.maximumSize(maxClients)
				.expireAfterWrite(expireMinutes, TimeUnit.MINUTES)
				.concurrencyLevel(concurrencyLevel);
		if (closeOnEviction && evictionHandler != null) {
			builder.removalListener(new RemovalListener<String, List<WsSession>>() {
				@Override
				public void onRemoval(RemovalNotification<String, List<WsSession>> notification) {
					List<WsSession> sessions = notification.getValue();
					if (sessions == null) {
						return;
					}
					for (WsSession session : sessions) {
						evictionHandler.accept(session);
					}
				}
			});
		}
		this.sessionsByClientId = builder.build();
	}

	/**
	 * 登记会话；同时把元信息写入会话属性。
	 *
	 * @param session  会话
	 * @param appId    应用 ID
	 * @param clientId 客户端 ID
	 */
	public void register(WsSession session, String appId, String clientId) {
		WsSessionInfo info = new WsSessionInfo(appId, clientId);
		session.getAttributes().put(WsSessionInfo.ATTRIBUTE_KEY, info);
		List<WsSession> sessions = sessionsByClientId.asMap()
				.computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>());
		sessions.add(session);
		// 显式 put 刷新写入过期计时，避免仅通过 asMap 操作时活跃客户端被驱逐
		sessionsByClientId.put(clientId, sessions);
	}

	/**
	 * 移除指定会话；sessionId 为空时移除该 clientId 下的全部会话。
	 *
	 * @param clientId  客户端 ID
	 * @param sessionId 会话 ID，可为空
	 * @return 被移除的会话
	 */
	public List<WsSession> remove(String clientId, String sessionId) {
		List<WsSession> sessions = sessionsByClientId.getIfPresent(clientId);
		if (sessions == null) {
			return Collections.emptyList();
		}
		List<WsSession> removed = new ArrayList<>();
		for (WsSession session : sessions) {
			if (sessionId == null || sessionId.isEmpty() || sessionId.equals(session.getId())) {
				removed.add(session);
			}
		}
		sessions.removeAll(removed);
		if (sessions.isEmpty()) {
			sessionsByClientId.asMap().computeIfPresent(clientId, (k, v) -> v.isEmpty() ? null : v);
		} else {
			sessionsByClientId.put(clientId, sessions);
		}
		for (WsSession session : removed) {
			sendLocks.remove(session.getId());
			session.getAttributes().remove(WsSessionInfo.ATTRIBUTE_KEY);
		}
		return removed;
	}

	/**
	 * 查询指定 clientId 下的会话。
	 *
	 * @param clientId 客户端 ID
	 * @return 会话列表
	 */
	public List<WsSession> findByClientId(String clientId) {
		List<WsSession> sessions = sessionsByClientId.getIfPresent(clientId);
		return sessions == null ? Collections.emptyList() : sessions;
	}

	/**
	 * 查询指定 appId 下仍打开的会话。
	 *
	 * @param appId 应用 ID
	 * @return 会话列表
	 */
	public List<WsSession> findByAppId(String appId) {
		List<WsSession> matched = new ArrayList<>();
		for (List<WsSession> sessions : sessionsByClientId.asMap().values()) {
			if (sessions == null) {
				continue;
			}
			for (WsSession session : sessions) {
				WsSessionInfo info = infoOf(session);
				if (info != null && appId.equals(info.getAppId()) && session.isOpen()) {
					matched.add(session);
				}
			}
		}
		return matched;
	}

	/**
	 * 当前登记的会话总数。
	 *
	 * @return 会话总数
	 */
	public int count() {
		int total = 0;
		for (List<WsSession> sessions : sessionsByClientId.asMap().values()) {
			total += sessions == null ? 0 : sessions.size();
		}
		return total;
	}

	/**
	 * 按 sessionId 取发送锁，保证同一会话内发送串行。
	 *
	 * @param session 会话
	 * @return 锁对象
	 */
	public Object sendLock(WsSession session) {
		return sendLocks.computeIfAbsent(session.getId(), key -> new Object());
	}

	/**
	 * 释放发送锁。
	 *
	 * @param sessionId 会话 ID
	 */
	public void releaseLock(String sessionId) {
		sendLocks.remove(sessionId);
	}

	/**
	 * 取会话元信息。
	 *
	 * @param session 会话
	 * @return 元信息，不存在时返回 null
	 */
	public static WsSessionInfo infoOf(WsSession session) {
		Object value = session.getAttributes().get(WsSessionInfo.ATTRIBUTE_KEY);
		return value instanceof WsSessionInfo ? (WsSessionInfo) value : null;
	}

	/**
	 * 刷新 clientId 的缓存过期计时。
	 *
	 * @param clientId 客户端 ID
	 */
	public void touch(String clientId) {
		List<WsSession> sessions = sessionsByClientId.getIfPresent(clientId);
		if (sessions != null) {
			sessionsByClientId.put(clientId, sessions);
		}
	}

	/**
	 * 清空全部登记。
	 *
	 * @return 清空前的全部会话
	 */
	public Collection<WsSession> invalidateAll() {
		List<WsSession> all = new ArrayList<>();
		for (List<WsSession> sessions : sessionsByClientId.asMap().values()) {
			if (sessions != null) {
				all.addAll(sessions);
			}
		}
		sessionsByClientId.invalidateAll();
		sendLocks.clear();
		return all;
	}
}
