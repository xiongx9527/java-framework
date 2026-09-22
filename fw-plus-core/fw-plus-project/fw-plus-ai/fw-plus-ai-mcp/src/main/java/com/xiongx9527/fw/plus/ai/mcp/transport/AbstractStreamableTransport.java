package com.xiongx9527.fw.plus.ai.mcp.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcRequest;
import com.xiongx9527.fw.plus.ai.mcp.protocol.McpJsonRpcResponse;
import com.xiongx9527.fw.plus.ai.mcp.server.McpAsyncServerImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Streamable HTTP 传输的框架无关基类
 *
 * <p>承载 MVC 与 WebFlux 两套传输共享的会话模型与协议行为（spec 5.1）：
 * <ul>
 *   <li>内存会话注册表（{@link ConcurrentHashMap}），不跨节点、不落库；</li>
 *   <li>{@code Mcp-Session-Id} 会话标识与过期清理（惰性清理，无后台线程）；</li>
 *   <li>SSE sink 抽象：子类只负责把事件写入具体框架通道（SseEmitter / Sinks.Many）；</li>
 *   <li>服务端通知下发（{@link #notifyClient} / {@link #notifyClients}）与事件 ID、</li>
 *   <li>基于 {@code Last-Event-ID} 的事件恢复缓冲；</li>
 *   <li>无效 {@code Origin} 校验（默认同源 + 环回放行，可配置白名单）。</li>
 * </ul>
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public abstract class AbstractStreamableTransport implements McpStreamableTransport, McpStreamableServerTransportProvider {

	protected static final Logger logger = LoggerFactory.getLogger(AbstractStreamableTransport.class);

	/** 会话标识头（spec 5.1.4） */
	public static final String SESSION_ID_HEADER = "Mcp-Session-Id";

	/** 事件恢复缓冲的最大长度 */
	private static final int MAX_EVENT_BUFFER = 256;

	/** 支持的协议版本（按 spec 3 接受 2025-11-25 与 2025-06-18） */
	private static final List<String> SUPPORTED_PROTOCOL_VERSIONS =
			Collections.unmodifiableList(Arrays.asList("2025-11-25", "2025-06-18"));

	private final String name;
	private final String path;
	protected final long sessionTimeoutMillis;
	private final ObjectMapper objectMapper;

	private final Map<String, Session> sessions = new ConcurrentHashMap<>();
	private final List<String> allowedOrigins = new ArrayList<>();
	private volatile McpAsyncServerImpl server;
	private volatile boolean running;
	private volatile SessionFactory sessionFactory;

	protected AbstractStreamableTransport(String name, String path, long sessionTimeoutMillis,
			ObjectMapper objectMapper) {
		this.name = name;
		this.path = normalizePath(path);
		this.sessionTimeoutMillis = sessionTimeoutMillis;
		this.objectMapper = objectMapper;
	}

	// ==================== McpTransportLifecycle ====================

	@Override
	public void bindServer(McpAsyncServerImpl server) {
		this.server = server;
	}

	// ==================== McpStreamableTransport ====================

	@Override
	public String getName() {
		return name;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public String createSession() {
		expireSessions();
		Session session = new Session(UUID.randomUUID().toString());
		sessions.put(session.id, session);
		logger.debug("MCP instance '{}' created session {}", name, session.id);
		return session.id;
	}

	@Override
	public boolean hasSession(String sessionId) {
		if (sessionId == null) {
			return false;
		}
		expireSessions();
		return sessions.containsKey(sessionId);
	}

	@Override
	public void closeSession(String sessionId) {
		Session session = sessionId != null ? sessions.remove(sessionId) : null;
		if (session == null) {
			return;
		}
		SseSink sink = session.sink;
		session.sink = null;
		if (sink != null) {
			try {
				sink.complete();
			} catch (Exception e) {
				logger.debug("Error completing SSE sink of session {}", sessionId, e);
			}
		}
		logger.debug("MCP instance '{}' closed session {}", name, sessionId);
	}

	@Override
	public void closeAll() {
		for (String sessionId : new ArrayList<>(sessions.keySet())) {
			closeSession(sessionId);
		}
	}

	// ==================== McpStreamableServerTransportProvider ====================

	/**
	 * 会话由本传输自管（每个 HTTP 会话一个 SSE 通道），不使用外部会话工厂。
	 */
	@Override
	public void setSessionFactory(SessionFactory sessionFactory) {
		this.sessionFactory = sessionFactory;
	}

	@Override
	public McpJsonRpcResponse handlePost(String sessionId, McpJsonRpcRequest request) {
		McpAsyncServerImpl target = this.server;
		if (target == null) {
			throw new IllegalStateException("Transport of MCP instance '" + name + "' is not bound to a server");
		}
		return target.handleRequest(request, sessionId);
	}

	/**
	 * MVC 场景由 {@code McpServletEndpoint} 覆写，返回 {@code SseEmitter}；
	 * WebFlux 场景由 {@code McpReactiveEndpoint} 直接调用
	 * {@code McpReactiveStreamableTransport#createEventStream}。
	 */
	@Override
	public Object handleGet(String sessionId) {
		return null;
	}

	@Override
	public void handleDelete(String sessionId) {
		closeSession(sessionId);
	}

	@Override
	public void notifyClients(String method, Object params) {
		for (String sessionId : new ArrayList<>(sessions.keySet())) {
			notifyClient(sessionId, method, params);
		}
	}

	@Override
	public void notifyClient(String sessionId, String method, Object params) {
		Session session = sessionId != null ? sessions.get(sessionId) : null;
		if (session == null) {
			logger.debug("Drop notification [{}]: unknown session {}", method, sessionId);
			return;
		}
		String payload = buildNotification(method, params);
		long eventId;
		SseSink sink;
		synchronized (session) {
			eventId = ++session.eventSeq;
			session.events.addLast(new SseEvent(Long.toString(eventId), payload));
			while (session.events.size() > MAX_EVENT_BUFFER) {
				session.events.removeFirst();
			}
			sink = session.sink;
		}
		if (sink != null) {
			write(sink, Long.toString(eventId), payload);
		}
	}

	@Override
	public void start() {
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
		closeAll();
	}

	@Override
	public Collection<String> protocolVersions() {
		return SUPPORTED_PROTOCOL_VERSIONS;
	}

	// ==================== 会话 / SSE 支撑 ====================

	/**
	 * 注册/替换会话的 SSE sink，并按 {@code Last-Event-ID} 恢复未送达事件
	 *
	 * @param sessionId   会话 ID
	 * @param sink        SSE 写入通道
	 * @param lastEventId 客户端上次收到的事件 ID（可空）
	 * @return 会话不存在时返回 {@code false}
	 */
	protected boolean attachSink(String sessionId, SseSink sink, String lastEventId) {
		Session session = sessionId != null ? sessions.get(sessionId) : null;
		if (session == null) {
			return false;
		}
		SseSink previous;
		List<SseEvent> replay = new ArrayList<>();
		synchronized (session) {
			previous = session.sink;
			session.sink = sink;
			session.lastAccess = System.currentTimeMillis();
			if (lastEventId != null && !lastEventId.isEmpty()) {
				for (SseEvent event : session.events) {
					if (compareEventId(event.id, lastEventId) > 0) {
						replay.add(event);
					}
				}
			}
		}
		if (previous != null && previous != sink) {
			try {
				previous.complete();
			} catch (Exception e) {
				logger.debug("Error completing replaced SSE sink of session {}", sessionId, e);
			}
		}
		for (SseEvent event : replay) {
			write(sink, event.id, event.payload);
		}
		return true;
	}

	/**
	 * 会话 SSE 通道结束时解绑（客户端断开、超时、错误）
	 */
	protected void detachSink(String sessionId, SseSink sink) {
		Session session = sessionId != null ? sessions.get(sessionId) : null;
		if (session == null) {
			return;
		}
		synchronized (session) {
			if (session.sink == sink) {
				session.sink = null;
			}
		}
	}

	/**
	 * 会话是否仍存在（供 SSE 超时回调判断）
	 */
	protected boolean isKnownSession(String sessionId) {
		return sessionId != null && sessions.containsKey(sessionId);
	}

	/**
	 * 校验 Origin（spec 5.1.6）
	 *
	 * <p>规则：
	 * <ol>
	 *   <li>未携带 {@code Origin}（CLI/SDK 场景）→ 放行；</li>
	 *   <li>白名单为空 → 同源（Origin 主机 == Host 主机）或环回地址放行；</li>
	 *   <li>白名单非空 → 命中白名单（或 {@code *}）放行；</li>
	 *   <li>Origin 无法解析为 http/https URL → 拒绝。</li>
	 * </ol>
	 *
	 * @return true 表示放行
	 */
	public boolean isOriginAllowed(String origin, String hostHeader) {
		if (origin == null || origin.trim().isEmpty() || "null".equals(origin)) {
			return true;
		}
		String originHost = hostOf(origin);
		if (originHost == null) {
			return false;
		}
		if (!allowedOrigins.isEmpty()) {
			if (allowedOrigins.contains("*")) {
				return true;
			}
			for (String allowed : allowedOrigins) {
				if (allowed.equalsIgnoreCase(origin)
						|| allowed.equalsIgnoreCase(originHost)
						|| hostOf(allowed) != null && hostOf(allowed).equalsIgnoreCase(originHost)) {
					return true;
				}
			}
			return false;
		}
		if (isLoopback(originHost)) {
			return true;
		}
		String host = hostOf("http://" + (hostHeader == null ? "" : hostHeader.trim()));
		return host != null && host.equalsIgnoreCase(originHost);
	}

	/**
	 * 配置 Origin 白名单（空集合表示放开为同源/环回策略）
	 */
	public void setAllowedOrigins(Collection<String> origins) {
		this.allowedOrigins.clear();
		if (origins != null) {
			for (String origin : origins) {
				if (origin != null && !origin.trim().isEmpty()) {
					this.allowedOrigins.add(origin.trim());
				}
			}
		}
	}

	// ==================== 内部方法 ====================

	private void write(SseSink sink, String eventId, String payload) {
		try {
			sink.send(eventId, payload);
		} catch (Exception e) {
			logger.debug("Failed to write SSE event {} to sink", eventId, e);
		}
	}

	private String buildNotification(String method, Object params) {
		Map<String, Object> notification = new LinkedHashMap<>();
		notification.put("jsonrpc", "2.0");
		if (params != null) {
			notification.put("params", params);
		}
		notification.put("method", method);
		try {
			return objectMapper.writeValueAsString(notification);
		} catch (Exception e) {
			throw new IllegalStateException("Failed to serialize MCP notification " + method, e);
		}
	}

	private void expireSessions() {
		long now = System.currentTimeMillis();
		for (Map.Entry<String, Session> entry : sessions.entrySet()) {
			Session session = entry.getValue();
			if (now - session.lastAccess > sessionTimeoutMillis) {
				closeSession(entry.getKey());
			}
		}
	}

	/**
	 * SSE 写入通道抽象：屏蔽 SseEmitter 与 reactor Sinks 差异
	 */
	protected interface SseSink {

		/**
		 * 写入一条 SSE 事件
		 */
		void send(String eventId, String jsonData);

		/**
		 * 正常结束通道
		 */
		void complete();

		/**
		 * 以异常结束通道
		 */
		void completeWithError(Throwable error);
	}

	/**
	 * 会话状态
	 */
	protected static final class Session {

		final String id;
		final Deque<SseEvent> events = new ArrayDeque<>();
		volatile SseSink sink;
		long eventSeq;
		volatile long lastAccess = System.currentTimeMillis();

		Session(String id) {
			this.id = id;
		}
	}

	/**
	 * 缓冲的 SSE 事件
	 */
	protected static final class SseEvent {

		final String id;
		final String payload;

		SseEvent(String id, String payload) {
			this.id = id;
			this.payload = payload;
		}
	}

	/**
	 * 事件 ID 比较（纯数字按数值比较，否则按字符串比较）
	 */
	private static int compareEventId(String left, String right) {
		try {
			return Long.compare(Long.parseLong(left), Long.parseLong(right));
		} catch (NumberFormatException e) {
			return left.compareTo(right);
		}
	}

	private static boolean isLoopback(String host) {
		return "localhost".equalsIgnoreCase(host)
				|| "127.0.0.1".equals(host)
				|| "::1".equals(host)
				|| "[::1]".equals(host);
	}

	private static String hostOf(String url) {
		if (url == null || url.isEmpty()) {
			return null;
		}
		try {
			URI uri = URI.create(url);
			String scheme = uri.getScheme();
			if (scheme == null) {
				return null;
			}
			if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
				return null;
			}
			return uri.getHost();
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	/**
	 * 规范化端点路径：确保以 / 开头、不以 / 结尾（根路径除外）
	 */
	public static String normalizePath(String path) {
		String normalized = path;
		if (normalized == null || normalized.trim().isEmpty()) {
			normalized = "/mcp";
		}
		normalized = normalized.trim();
		if (!normalized.startsWith("/")) {
			normalized = "/" + normalized;
		}
		while (normalized.length() > 1 && normalized.endsWith("/")) {
			normalized = normalized.substring(0, normalized.length() - 1);
		}
		return normalized;
	}

	/**
	 * 获取会话 ID（供端点读取请求头后统一使用）
	 */
	public String resolveSessionId(String headerValue) {
		return headerValue == null || headerValue.trim().isEmpty() ? null : headerValue.trim();
	}

	/**
	 * 解析 JSON-RPC 请求体
	 *
	 * @param body 原始请求体
	 * @return 请求对象；JSON 非法时返回 {@code null}（端点据此返回 -32700 Parse error）
	 */
	public McpJsonRpcRequest parseRequest(String body) {
		if (body == null || body.trim().isEmpty()) {
			return null;
		}
		try {
			return objectMapper.readValue(body, McpJsonRpcRequest.class);
		} catch (Exception e) {
			logger.debug("Failed to parse JSON-RPC request body", e);
			return null;
		}
	}

	/**
	 * 供端点序列化 JSON-RPC 响应/错误
	 */
	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	/**
	 * 状态：传输是否已启动
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * 内部调试用：当前会话数
	 */
	public int sessionCount() {
		return sessions.size();
	}
}
