package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsInstanceResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.session.WsSessionInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.session.WsSessionRegistry;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsReplyHandle;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * WebFlux 栈的 WebSocket 处理器。
 *
 * <p>与 Servlet 栈的 {@code AbstractWebSocketHandler} 提供相同的能力：握手参数校验、会话登记、
 * 文本与二进制消息派发、定向发送与广播，并复用同一套 {@code fw.plus.ws.*} 配置与
 * {@link WsBusinessDispatcher} 订阅 SPI。</p>
 *
 * <p>订阅脚本是同步阻塞的，一律提交到独立有界线程池执行，绝不占用 Reactor event loop。</p>
 *
 * @author zzy
 */
@Slf4j
public class ReactiveWsHandler implements org.springframework.web.reactive.socket.WebSocketHandler, DisposableBean {

	private final WsConfig wsConfig;

	/**
	 * 配置读取委托：{@code WsBaseService#getSubProtocols()} 返回数组，与响应式
	 * {@code WebSocketHandler#getSubProtocols()} 的 {@code List} 返回类型冲突，
	 * 因此这里不实现该接口，改为持有委托对象。
	 */
	private final WsBaseService config;

	private final WsSessionRegistry registry;

	private final ThreadPoolTaskExecutor scriptExecutor;

	private WsBusinessDispatcher businessDispatcher;

	public ReactiveWsHandler(WsConfig wsConfig) {
		this.wsConfig = wsConfig;
		this.config = new WsBaseService() {
			@Override
			public Map<String, Object> getConfig() {
				return wsConfig == null ? WsBaseService.super.getConfig() : wsConfig.flatten();
			}
		};
		this.registry = new WsSessionRegistry(config.getMaxClientSessions(), config.getSessionCacheExpireMinutes(),
				config.getCacheConcurrencyLevel(), config.isCloseOnCacheEviction(), this::closeQuietly);
		this.scriptExecutor = new ThreadPoolTaskExecutor();
		this.scriptExecutor.setCorePoolSize(config.getScriptThreadPoolCoreSize());
		this.scriptExecutor.setMaxPoolSize(config.getScriptThreadPoolMaxSize());
		this.scriptExecutor.setQueueCapacity(config.getScriptThreadPoolQueueCapacity());
		this.scriptExecutor.setThreadNamePrefix("ws-script-");
		this.scriptExecutor.initialize();
	}

	/**
	 * 注入业务侧订阅调度实现；未提供实现时退化为纯 WebSocket 通道。
	 *
	 * @param businessDispatcher 订阅调度实现
	 */
	public void setBusinessDispatcher(WsBusinessDispatcher businessDispatcher) {
		this.businessDispatcher = businessDispatcher;
	}

	@Override
	public Mono<Void> handle(WebSocketSession session) {
		WsSession wsSession = new ReactiveWsSession(session);
		AuthResult auth = authenticate(session.getHandshakeInfo().getUri());
		if (!auth.accepted) {
			return session.close(new CloseStatus(auth.status.getCode(), auth.reason));
		}

		int maxTotalSessions = config.getMaxTotalSessions();
		if (maxTotalSessions > 0 && registry.count() >= maxTotalSessions) {
			return session.close(new CloseStatus(CloseStatus.SERVICE_OVERLOAD.getCode(), "连接数已达上限"));
		}
		if (config.getMaxSessionsPerClient() > 0
				&& registry.findByClientId(auth.clientId).size() >= config.getMaxSessionsPerClient()) {
			return session.close(new CloseStatus(CloseStatus.SERVICE_OVERLOAD.getCode(), "客户端连接数已达上限"));
		}

		registry.register(wsSession, auth.appId, auth.clientId);
		runOnScriptExecutor(() -> init(auth.appId, wsSession, auth.clientId));

		Flux<Void> inbound = session.receive()
				.publishOn(reactor.core.scheduler.Schedulers.fromExecutor(scriptExecutor))
				.doOnNext(message -> handleInbound(wsSession, auth, message))
				.then()
				.flux();

		return inbound.doFinally(signal -> {
			registry.remove(auth.clientId, session.getId());
			runOnScriptExecutor(() -> close(auth.appId, wsSession, auth.clientId));
		}).then();
	}

	private void handleInbound(WsSession wsSession, AuthResult auth, WebSocketMessage message) {
		WsSessionInfo info = WsSessionInfo.class.cast(wsSession.getAttributes().get(WsSessionInfo.ATTRIBUTE_KEY));
		if (info != null) {
			info.touch();
			registry.touch(info.getClientId());
		}
		try {
			if (message.getType() == WebSocketMessage.Type.BINARY) {
				if (!config.isBinaryEnabled()) {
					wsSession.close(org.springframework.web.socket.CloseStatus.NOT_ACCEPTABLE);
					return;
				}
				dispatchBinary(wsSession, auth, readBytes(message));
				return;
			}
			if (message.getType() != WebSocketMessage.Type.TEXT) {
				return;
			}
			String payload = message.getPayloadAsText();
			if ("ping".equals(payload)) {
				sendQuietly(wsSession, "{\"type\":\"pong\",\"message\":\"success\",\"data\":\"pong\"}");
				return;
			}
			if (!JSONValidator.from(payload).validate()) {
				sendQuietly(wsSession, errorJson("INVALID_JSON", "Invalid JSON format"));
				return;
			}
			JSONObject jsonObject = JSON.parseObject(payload);
			String businessType = jsonObject.getString("type");
			if (StringUtils.isBlank(businessType)) {
				sendQuietly(wsSession, errorJson("MISSING_TYPE", "Message type is required"));
				return;
			}
			dispatchText(wsSession, auth, businessType, jsonObject);
		} catch (Exception e) {
			log.error("Error handling reactive websocket message: {}", e.getMessage(), e);
			sendQuietly(wsSession, errorJson("PROCESSING_ERROR", "Error processing message"));
		}
	}

	private void dispatchText(WsSession wsSession, AuthResult auth, String businessType, JSONObject params) {
		if (businessDispatcher == null) {
			return;
		}
		for (WsSubscribeScript subscribe : findSubscribes(auth.appId, businessType)) {
			try {
				businessDispatcher.execute(subscribe, new WsInvocationContext(WsInvocationContext.Phase.MESSAGE,
						auth.appId, auth.clientId, wsSession.getId(), WsInvocationContext.MessageKind.TEXT,
						businessType, params, null, wsSession, createReplyHandle(wsSession)));
			} catch (Exception e) {
				log.error("Error processing subscribe message: {}", e.getMessage(), e);
				sendQuietly(wsSession, errorJson("SCRIPT_ERROR", "Error processing websocket subscribe script"));
			}
		}
	}

	private void dispatchBinary(WsSession wsSession, AuthResult auth, byte[] payload) {
		if (businessDispatcher == null) {
			return;
		}
		String businessType = config.getBinaryBusinessType();
		for (WsSubscribeScript subscribe : findSubscribes(auth.appId, businessType)) {
			try {
				businessDispatcher.execute(subscribe, new WsInvocationContext(WsInvocationContext.Phase.MESSAGE,
						auth.appId, auth.clientId, wsSession.getId(), WsInvocationContext.MessageKind.BINARY,
						businessType, null, payload, wsSession, createReplyHandle(wsSession)));
			} catch (Exception e) {
				log.error("Error processing binary subscribe message: {}", e.getMessage(), e);
			}
		}
	}

	private List<? extends WsSubscribeScript> findSubscribes(String appId, String businessType) {
		List<? extends WsSubscribeScript> subscribes = businessDispatcher.findEnabledSubscribes(appId, businessType);
		return subscribes == null ? Collections.emptyList() : subscribes;
	}

	private void init(String appId, WsSession wsSession, String clientId) {
		if (businessDispatcher == null) {
			return;
		}
		runSubscribes(appId, wsSession, clientId, config.getInitBusinessType(), WsInvocationContext.Phase.INIT);
	}

	private void close(String appId, WsSession wsSession, String clientId) {
		if (businessDispatcher == null) {
			return;
		}
		runSubscribes(appId, wsSession, clientId, config.getCloseBusinessType(), WsInvocationContext.Phase.CLOSE);
	}

	private void runSubscribes(String appId, WsSession wsSession, String clientId, String businessType,
							   WsInvocationContext.Phase phase) {
		for (WsSubscribeScript subscribe : findSubscribes(appId, businessType)) {
			try {
				businessDispatcher.execute(subscribe, new WsInvocationContext(phase, appId, clientId,
						wsSession.getId(), businessType, null, wsSession, null));
			} catch (Exception e) {
				log.error("Error processing subscribe message: {}", e.getMessage(), e);
			}
		}
	}

	private WsReplyHandle createReplyHandle(WsSession wsSession) {
		return new WsReplyHandle() {
			@Override
			public void sendText(String message) {
				ReactiveWsHandler.this.sendQuietly(wsSession, message);
			}

			@Override
			public void sendBinary(byte[] payload) {
				try {
					wsSession.sendBinary(payload);
				} catch (Exception e) {
					log.error("Failed to reply binary message to session: {}", wsSession.getId(), e);
				}
			}
		};
	}

	private void sendQuietly(WsSession wsSession, String message) {
		try {
			synchronized (registry.sendLock(wsSession)) {
				if (wsSession.isOpen()) {
					wsSession.sendText(message);
				}
			}
		} catch (Exception e) {
			log.error("Failed to send message to session: {}", wsSession.getId(), e);
		}
	}

	private AuthResult authenticate(URI uri) {
		String query = uri == null ? null : uri.getQuery();
		Map<String, String> params = parseQueryParams(query);
		String clientId = params.get(config.getClientIdParamName());
		if (StringUtils.isBlank(clientId)) {
			log.error("Missing required parameter: {}", config.getClientIdParamName());
			return AuthResult.reject(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION, "缺少必要参数");
		}
		if (clientId.length() > config.getMaxClientIdLength()) {
			return AuthResult.reject(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION, "客户端ID过长");
		}
		// 按端点路径解析实例 appId：未命中实例或实例未配置 appId 时回落到默认 appId
		String appId = WsInstanceResolver.resolveAppId(wsConfig, uri == null ? null : uri.getPath());
		if (StringUtils.isBlank(appId)) {
			appId = config.getDefaultAppId();
		}
		if (businessDispatcher != null && !businessDispatcher.existsApp(appId)) {
			return AuthResult.reject(org.springframework.web.socket.CloseStatus.POLICY_VIOLATION, "AppId不存在");
		}
		return AuthResult.accept(appId, clientId);
	}

	private Map<String, String> parseQueryParams(String query) {
		Map<String, String> params = new HashMap<>();
		if (StringUtils.isBlank(query)) {
			return params;
		}
		for (String pair : query.split("&")) {
			int index = pair.indexOf('=');
			if (index <= 0) {
				continue;
			}
			try {
				params.put(java.net.URLDecoder.decode(pair.substring(0, index), "UTF-8"),
						java.net.URLDecoder.decode(pair.substring(index + 1), "UTF-8"));
			} catch (Exception e) {
				log.debug("Failed to decode query parameter: {}", pair);
			}
		}
		return params;
	}

	private static byte[] readBytes(WebSocketMessage message) {
		java.nio.ByteBuffer buffer = message.getPayload().asByteBuffer();
		byte[] payload = new byte[buffer.remaining()];
		buffer.get(payload);
		return payload;
	}

	private static String errorJson(String code, String message) {
		JSONObject error = new JSONObject();
		error.put("type", "error");
		error.put("success", false);
		error.put("code", code);
		error.put("message", message);
		error.put("timestamp", System.currentTimeMillis());
		return JSON.toJSONString(error);
	}

	private void runOnScriptExecutor(Runnable task) {
		try {
			scriptExecutor.execute(task);
		} catch (Exception e) {
			log.warn("WebSocket script executor rejected task");
		}
	}

	private void closeQuietly(WsSession session) {
		try {
			if (session.isOpen()) {
				session.close(org.springframework.web.socket.CloseStatus.SERVICE_OVERLOAD);
			}
		} catch (Exception e) {
			log.error("Error closing session: {}", session.getId(), e);
		}
	}

	/**
	 * 向指定 appId 的本地会话广播文本消息。
	 *
	 * @param appId   应用 ID
	 * @param message 消息内容
	 */
	public void broadcastToApp(String appId, String message) {
		for (WsSession session : registry.findByAppId(appId)) {
			sendQuietly(session, message);
		}
	}

	/**
	 * 向指定 clientId 的本地会话广播文本消息。
	 *
	 * @param appId    应用 ID
	 * @param clientId 客户端 ID
	 * @param message  消息内容
	 */
	public void broadcastToClient(String appId, String clientId, String message) {
		for (WsSession session : registry.findByClientId(clientId)) {
			WsSessionInfo info = WsSessionInfo.class.cast(session.getAttributes().get(WsSessionInfo.ATTRIBUTE_KEY));
			if (info != null && appId.equals(info.getAppId())) {
				sendQuietly(session, message);
			}
		}
	}

	/**
	 * 当前登记的会话总数。
	 *
	 * @return 会话总数
	 */
	public int sessionCount() {
		return registry.count();
	}

	@Override
	public void destroy() {
		for (WsSession session : registry.invalidateAll()) {
			closeQuietly(session);
		}
		scriptExecutor.shutdown();
	}

	/**
	 * 握手鉴权结果。
	 */
	private static final class AuthResult {

		private final boolean accepted;

		private final String appId;

		private final String clientId;

		private final org.springframework.web.socket.CloseStatus status;

		private final String reason;

		private AuthResult(boolean accepted, String appId, String clientId,
						   org.springframework.web.socket.CloseStatus status, String reason) {
			this.accepted = accepted;
			this.appId = appId;
			this.clientId = clientId;
			this.status = status;
			this.reason = reason;
		}

		static AuthResult accept(String appId, String clientId) {
			return new AuthResult(true, appId, clientId, null, null);
		}

		static AuthResult reject(org.springframework.web.socket.CloseStatus status, String reason) {
			return new AuthResult(false, null, null, status, reason);
		}
	}
}
