package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalCause;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.broadcast.WsBroadcastExecutor;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsBaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common.WsTokenResolver;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsReplyHandle;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webmvc.ServletWsSession;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.net.URI;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * 与业务无关的 WebSocket 处理器地基。
 *
 * <p>负责连接建立与鉴权（token/clientId 校验、appId 提取）、会话注册与心跳、
 * 文本消息解析分发、定向发送与广播、资源回收。业务语义（哪些订阅生效、脚本如何执行）
 * 通过 {@link WsBusinessDispatcher} 注入；未注入实现时退化为纯 WebSocket 通道。</p>
 *
 * @author zzy
 */
@Slf4j
public abstract class AbstractWebSocketHandler implements WebSocketHandler, WsBaseService, DisposableBean {

    /**
     * WebSocket 通用配置，构造期即可用于读取会话缓存、线程池等参数
     */
    private final WsConfig wsConfig;

    /**
     * 共享的延迟关闭调度器，避免每次 delayedClose 创建新线程池导致线程泄漏
     */
    private final ScheduledExecutorService scheduledExecutor;

    /**
     * 按密钥内容缓存的JWT签名密钥与算法，避免每次验证/生成token时重复计算MessageDigest。
     */
    private final Map<String, SecretKeyAndSignatureAlgorithm> secretKeyCache = new ConcurrentHashMap<>();

    /**
     * 异步广播投递线程池，按需创建。
     */
    private volatile WsBroadcastExecutor broadcastExecutor;

    /**
     * 存储客户端ID与会话列表的映射关系，用于管理同一客户端的多个会话
     * 使用Guava Cache限制大小并支持自动过期
     */
    private final Cache<String, List<WebSocketSession>> clientIdSessionListMap;

    /**
     * WebSocketSession 不保证并发 sendMessage 安全，按 sessionId 串行化发送。
     */
    private final Map<String, Object> sessionSendLockMap = new ConcurrentHashMap<>();

    /**
     * WebSocket服务器ID，用于标识当前服务器实例
     */
    private final String webSocketServerId = generateWebSocketServerId();

    /**
     * 业务侧订阅调度实现，允许缺失
     */
    private WsBusinessDispatcher businessDispatcher;

    protected AbstractWebSocketHandler(WsConfig wsConfig) {
        this.wsConfig = wsConfig;
        this.scheduledExecutor = Executors.newScheduledThreadPool(getDelayedClosePoolSize(), r -> {
            Thread t = new Thread(r, "ws-delayed-close");
            t.setDaemon(true);
            return t;
        });
        this.clientIdSessionListMap = CacheBuilder.newBuilder()
                .maximumSize(getMaxClientSessions())
                .expireAfterWrite(getSessionCacheExpireMinutes(), TimeUnit.MINUTES)
                .concurrencyLevel(getCacheConcurrencyLevel())
                .removalListener(new RemovalListener<String, List<WebSocketSession>>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, List<WebSocketSession>> notification) {
                        if (notification.getCause() == RemovalCause.REPLACED) {
                            log.debug("Client session list refreshed in cache: {}", notification.getKey());
                            return;
                        }
                        log.warn("Client session list removed from cache: {} - {}",
                                notification.getKey(), notification.getCause());
                        // 缓存驱逐不等于连接失效：默认只摘除登记、不主动关闭仍打开的长连接，
                        // 否则只发业务消息从不发 ping 的客户端会在 expireMinutes 后被误杀。
                        if (!isCloseOnCacheEviction()) {
                            return;
                        }
                        try {
                            List<WebSocketSession> sessions = notification.getValue();
                            if (sessions != null) {
                                for (WebSocketSession session : sessions) {
                                    try {
                                        log.warn("Session is still open: {}", session.getId());
                                        cleanupSessionResources(session, "SERVICE OVERLOAD", CloseStatus.SERVICE_OVERLOAD, true);
                                    } catch (Exception e) {
                                        log.error("Error closing session: {}", session.getId(), e);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            log.error("Error processing cache removal for client: {}", notification.getKey(), e);
                        }
                    }
                })
                .build();
    }

    /**
     * 注入业务侧订阅调度实现；未提供实现时地基退化为纯 WebSocket 通道。
     *
     * @param businessDispatcher 订阅调度实现
     */
    @Autowired(required = false)
    public void setBusinessDispatcher(WsBusinessDispatcher businessDispatcher) {
        this.businessDispatcher = businessDispatcher;
    }

    /**
     * 获取业务侧订阅调度实现，可能为 null。
     *
     * @return 订阅调度实现
     */
    protected WsBusinessDispatcher dispatcher() {
        return businessDispatcher;
    }

    /**
     * 构造注入的配置优先，避免依赖宿主容器的静态工具 Bean。
     */
    @Override
    public Map<String, Object> getConfig() {
        if (wsConfig != null) {
            return wsConfig.flatten();
        }
        return WsBaseService.super.getConfig();
    }

    /**
     * 容器销毁时清理所有资源。
     *
     * <p>早前这里注册的是 JVM shutdown hook：每创建一个处理器就多一个无法注销的钩子，
     * 且不受 Spring 关闭顺序管理。改为 {@link DisposableBean} 后由容器统一驱动。</p>
     */
    @Override
    public void destroy() {
        cleanupAllResources();
    }

    /**
     * 清理所有资源，在应用关闭时调用
     */
    private void cleanupAllResources() {
        try {
            // 先收集所有活跃会话引用，避免 session.close() 触发 afterConnectionClosed
            // 回调中修改缓存导致迭代器行为不确定
            List<WebSocketSession> allSessions = new ArrayList<>();
            for (Map.Entry<String, List<WebSocketSession>> entry : clientIdSessionListMap.asMap().entrySet()) {
                for (WebSocketSession session : entry.getValue()) {
                    if (session.isOpen()) {
                        allSessions.add(session);
                    }
                }
            }
            // 统一关闭，此时 afterConnectionClosed 中的 cleanSession 操作缓存是安全的
            for (WebSocketSession session : allSessions) {
                try {
                    sessionSendLockMap.remove(session.getId());
                    session.close(CloseStatus.GOING_AWAY.withReason("Server shutting down"));
                } catch (Exception e) {
                    log.warn("Error closing session {} during shutdown", session.getId());
                }
            }
            clientIdSessionListMap.invalidateAll();
            scheduledExecutor.shutdown();
            if (broadcastExecutor != null) {
                broadcastExecutor.shutdown();
            }
            log.info("All WebSocket resources have been cleaned up");
        } catch (Exception e) {
            log.error("Error during cleanup of all resources", e);
        }
    }

    /**
     * 返回密钥对应的签名密钥与算法，按密钥内容缓存以避免重复计算摘要。
     *
     * <p>缓存以密钥字符串为键：配置刷新（{@code @RefreshScope}）更换
     * {@code fw.plus.ws.token.secret} 后会自然命中新条目，无需重启进程。</p>
     */
    @Override
    public SecretKeyAndSignatureAlgorithm getSecretKeyAndSignatureAlgorithm() {
        return getSecretKeyAndSignatureAlgorithm(getTokenSecret());
    }

    /**
     * 按指定密钥获取签名密钥与算法，多端点场景下同一进程并存多套密钥，仍按密钥字符串去重计算。
     */
    @Override
    public SecretKeyAndSignatureAlgorithm getSecretKeyAndSignatureAlgorithm(String secret) {
        SecretKeyAndSignatureAlgorithm cached = secretKeyCache.get(secret);
        if (cached == null) {
            synchronized (secretKeyCache) {
                cached = secretKeyCache.get(secret);
                if (cached == null) {
                    cached = WsBaseService.super.getSecretKeyAndSignatureAlgorithm(secret);
                    secretKeyCache.put(secret, cached);
                }
            }
        }
        return cached;
    }

    /**
     * 按请求路径解析 token 密钥：命中 {@code fw.plus.ws.tokens} 规则用规则密钥，否则回落全局。
     *
     * <p>直接使用构造注入的 {@link WsConfig}，避免每次握手走容器查找。</p>
     */
    @Override
    public String getTokenSecret(String requestPath) {
        String pathSecret = wsConfig == null ? null : WsTokenResolver.resolveSecret(wsConfig, requestPath);
        return StringUtils.isNotBlank(pathSecret) ? pathSecret : getTokenSecret();
    }

    /**
     * 按请求路径解析 token 有效期，未显式配置的路径继承全局值。
     */
    @Override
    public long getTokenExpireHours(String requestPath) {
        Long pathExpireHours = wsConfig == null ? null : WsTokenResolver.resolveExpireHours(wsConfig, requestPath);
        return pathExpireHours == null ? getTokenExpireHours() : pathExpireHours;
    }

    /**
     * 生成WebSocket服务器ID
     *
     * @return WebSocket服务器ID
     */
    private String generateWebSocketServerId() {
        try {
            // 尝试获取主机名和进程ID来生成唯一ID
            String hostName = InetAddress.getLocalHost().getHostName();
            String processId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            return hostName + "-" + processId;
        } catch (Exception e) {
            // 如果无法获取主机信息，则使用UUID生成唯一ID
            log.warn("Failed to generate server ID based on host and process, using UUID instead", e);
            return "server-" + UUID.randomUUID().toString();
        }
    }

    /**
     * 获取WebSocket服务器ID
     *
     * @return WebSocket服务器ID
     */
    public String getWebSocketServerId() {
        return webSocketServerId;
    }

    /**
     * 解析查询参数字符串为键值对映射
     *
     * @param query 查询参数字符串，格式如 "param1=value1&param2=value2"
     * @return 包含参数键值对的映射
     */
    private Map<String, String> parseQueryParams(String query) {
        Map<String, String> params = new HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        String key = URLDecoder.decode(keyValue[0], "UTF-8");
                        String value = URLDecoder.decode(keyValue[1], "UTF-8");
                        params.put(key, value);
                    } catch (UnsupportedEncodingException e) {
                        log.warn("Failed to decode query parameter: {}", pair, e);
                        // 如果解码失败，仍然使用原始值
                        params.put(keyValue[0], keyValue[1]);
                    }
                }
            }
        }
        return params;
    }

    /**
     * 解析指定 appId 与业务类型下启用中的订阅；无调度实现时返回空列表。
     *
     * @param appId        应用 ID
     * @param businessType 业务类型
     * @return 订阅列表
     */
    private List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
        if (businessDispatcher == null) {
            return Collections.emptyList();
        }
        List<? extends WsSubscribeScript> subscribes = businessDispatcher.findEnabledSubscribes(appId, businessType);
        return subscribes == null ? Collections.emptyList() : subscribes;
    }

    /**
     * 初始化WebSocket连接：查找并执行初始化订阅脚本
     *
     * @param appId     应用ID
     * @param sessionId 会话ID
     * @param clientId  客户端ID
     */
    private void init(String appId, String sessionId, String clientId) {
        String businessType = getInitBusinessType();
        List<? extends WsSubscribeScript> subscribeInfoList = findEnabledSubscribes(appId, businessType);
        for (WsSubscribeScript subscribeInfo : subscribeInfoList) {
            try {
                WsInvocationContext context = new WsInvocationContext(WsInvocationContext.Phase.INIT,
                        appId, clientId, sessionId, businessType, null, null, null);
                Object result = businessDispatcher.execute(subscribeInfo, context);
                log.info("Result of init sessionId:{} ,appId:{} script: {}", sessionId, appId, result);
            } catch (Exception e) {
                log.error("Error processing subscribe message: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 结束WebSocket会话：执行关闭订阅脚本
     *
     * @param appId     应用ID
     * @param sessionId 会话ID
     * @param clientId  客户端ID
     */
    private void end(String appId, String sessionId, String clientId) {
        String businessType = getCloseBusinessType();
        List<? extends WsSubscribeScript> subscribeInfoList = findEnabledSubscribes(appId, businessType);
        for (WsSubscribeScript subscribeInfo : subscribeInfoList) {
            try {
                WsInvocationContext context = new WsInvocationContext(WsInvocationContext.Phase.CLOSE,
                        appId, clientId, sessionId, businessType, null, null, null);
                Object result = businessDispatcher.execute(subscribeInfo, context);
                log.info("Result of close sessionId:{} ,appId:{} script: {}", sessionId, appId, result);
            } catch (Exception e) {
                log.error("Error processing subscribe message: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 根据应用ID清理相关的WebSocket会话连接
     *
     * @param appId 应用ID，用于筛选需要清理的会话
     */
    public void cleanupByAppId(String appId) {
        Map<String, List<WebSocketSession>> snapshot = clientIdSessionListMap.asMap();
        // 使用 List 存储 (clientId, sessionId) 对，避免 HashMap 覆盖导致同 clientId 多会话时清理遗漏
        List<String[]> pairs = new ArrayList<>();
        for (Map.Entry<String, List<WebSocketSession>> e : snapshot.entrySet()) {
            for (WebSocketSession session : e.getValue()) {
                try {
                    Object sessionInfoObj = session.getAttributes().get("sessionInfo");
                    if (sessionInfoObj instanceof SessionInfo) {
                        SessionInfo sessionInfo = (SessionInfo) sessionInfoObj;
                        if (appId.equals(sessionInfo.getAppId())) {
                            pairs.add(new String[]{sessionInfo.getClientId(), session.getId()});
                        }
                    }
                } catch (Exception ex) {
                    log.error("Error processing session: {}", session.getId(), ex);
                }
            }
        }
        for (String[] pair : pairs) {
            cleanSession(pair[0], pair[1]);
        }
    }

    /**
     * 在WebSocket连接建立后调用此方法，用于处理客户端连接信息和身份验证
     *
     * @param session WebSocket会话对象，代表与客户端的连接
     * @throws Exception 如果处理过程中发生错误
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();

        // 限制单帧大小，避免超大帧把内存打满
        session.setTextMessageSizeLimit(getMaxTextMessageSize());
        session.setBinaryMessageSizeLimit(getMaxBinaryMessageSize());

        try {
            // 获取客户端IP地址（getRemoteAddress 可能在连接异常关闭时返回 null）
            String clientIp = null;
            try {
                if (session.getRemoteAddress() != null) {
                    clientIp = session.getRemoteAddress().getAddress().getHostAddress();
                }
            } catch (Exception e) {
                log.debug("Failed to get remote address for session: {}", sessionId, e);
            }

            // 获取URI参数
            URI uri = session.getUri();
            String requestPath = uri != null ? uri.getPath() : null;
            String query = uri != null ? uri.getQuery() : null;

            // 获取Session ID
            log.info("WebSocket connection established - Session ID: {}, Client IP: {}", sessionId, clientIp);

            // 从查询参数中获取token和clientId
            Map<String, String> queryParams = parseQueryParams(query);
            String token = queryParams.get(getTokenParamName());
            String clientId = queryParams.get(getClientIdParamName());

            // 校验必要参数
            if (StringUtils.isBlank(token) || StringUtils.isBlank(clientId)) {
                log.error("Missing required parameters - Token present: {}, Client ID present: {}",
                        StringUtils.isNotBlank(token), StringUtils.isNotBlank(clientId));
                closeSession(session, "缺少必要参数", CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 检查clientId长度
            int maxClientIdLength = getMaxClientIdLength();
            if (clientId.length() > maxClientIdLength) {
                log.error("Client ID too long - Length: {}, Max: {}", clientId.length(), maxClientIdLength);
                closeSession(session, "客户端ID过长", CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 验证token并提取appId
            String appId;
            try {
                appId = validateAndExtractAppIdFromToken(token, getTokenSecret(requestPath));
                if (StringUtils.isBlank(appId)) {
                    log.error("Invalid token: {}", maskSensitiveValue(token));
                    closeSession(session, "无效的token", CloseStatus.POLICY_VIOLATION);
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to validate token: {}", maskSensitiveValue(token), e);
                closeSession(session, "token验证失败", CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 判断appid 是否存在（未提供业务调度实现时不做校验，保持纯 WebSocket 通道可用）
            if (businessDispatcher != null && !businessDispatcher.existsApp(appId)) {
                log.error("AppId not exist: {}", appId);
                closeSession(session, "AppId不存在", CloseStatus.POLICY_VIOLATION);
                return;
            }

            // 连接数上限：超限时拒绝握手，避免单实例会话无上限增长
            int maxTotalSessions = getMaxTotalSessions();
            if (maxTotalSessions > 0 && sessionCount() >= maxTotalSessions) {
                log.error("Max total sessions reached - limit: {}", maxTotalSessions);
                closeSession(session, "连接数已达上限", CloseStatus.SERVICE_OVERLOAD);
                return;
            }
            int maxSessionsPerClient = getMaxSessionsPerClient();
            if (maxSessionsPerClient > 0 && sessionsOf(clientId).size() >= maxSessionsPerClient) {
                log.error("Max sessions per client reached - clientId: {}, limit: {}", clientId, maxSessionsPerClient);
                closeSession(session, "客户端连接数已达上限", CloseStatus.SERVICE_OVERLOAD);
                return;
            }

            // 添加会话到管理映射
            addSession(appId, session, clientId);

            // 执行初始化脚本
            init(appId, sessionId, clientId);

            log.info("WebSocket connection fully established - Session ID: {}, Client ID: {}, App ID: {}",
                    sessionId, clientId, appId);
        } catch (Exception e) {
            log.error("Error during connection establishment - Session ID: {}", sessionId, e);
            try {
                if (session.isOpen()) {
                    session.close(CloseStatus.SERVER_ERROR.withReason("服务器内部错误"));
                }
            } catch (Exception closeException) {
                log.error("Error closing session after establishment failure - Session ID: {}", sessionId, closeException);
            }
        }
    }

    /**
     * 处理从客户端接收到的WebSocket消息
     *
     * @param session WebSocket会话对象，代表与客户端的连接
     * @param message 从客户端接收到的消息
     * @throws Exception 如果处理过程中发生错误
     */
    @Override
    public void handleMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
        // 检查会话是否仍然有效
        if (!isSessionValid(session)) {
            log.warn("Received message from invalid session: {}", session.getId());
            return;
        }
        // 任意入站消息都视为活跃：只发业务消息、从不发 ping 的长连接不应被判超时
        updateLastActiveTime(session);
        // 刷新缓存过期计时，避免仅发送业务消息不发 ping 的活跃客户端被驱逐
        refreshCacheEntry(session);

        log.debug("Received message from client - Session ID: {}, Message: {}",
                session.getId(), message.getPayload());
        if (message instanceof PingMessage) {
            sendPongMessage(session, (PingMessage) message);
            return;
        }
        if (message instanceof PongMessage) {
            updateLastActiveTime(session);
            return;
        }
        if (message instanceof BinaryMessage) {
            if (!isBinaryEnabled()) {
                session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unsupported binary message"));
                return;
            }
            ByteBuffer buffer = ((BinaryMessage) message).getPayload();
            byte[] payload = new byte[buffer.remaining()];
            buffer.get(payload);
            handleBinaryMessage(session, payload);
            return;
        }
        if (!(message instanceof TextMessage)) {
            session.close(CloseStatus.NOT_ACCEPTABLE.withReason("Unsupported websocket message"));
            return;
        }
        String payload = ((TextMessage) message).getPayload();
        try {
            // 获取消息内容
            if ("ping".equals(payload)) {
                handlePingMessage(session, null);
                return;
            }
            log.debug("payload:{}", payload);
            JSONValidator validator = JSONValidator.from(payload);
            JSONObject jsonObject = null;
            String businessType = null;
            if (validator.validate()) {
                // 解析JSON消息
                jsonObject = JSON.parseObject(payload);
                // 获取业务类型
                businessType = jsonObject.getString("type");
            } else {
                sendErrorMessage(session, "INVALID_JSON", "Invalid JSON format");
                return;
            }
            if (StringUtils.isNotBlank(businessType)) {
                // 根据业务类型处理消息，复用已解析的jsonObject避免重复解析
                handleBusinessMessage(session, businessType, jsonObject);
            } else {
                log.debug("Ignoring message without business type from session: {}", session.getId());
                sendErrorMessage(session, "MISSING_TYPE", "Message type is required");
            }
        } catch (com.alibaba.fastjson2.JSONException e) {
            log.error("Invalid JSON format in message: {}", payload, e);
            sendErrorMessage(session, "INVALID_JSON", "Invalid JSON format");
        } catch (Exception e) {
            log.error("Error processing message: {}", payload, e);
            sendErrorMessage(session, "PROCESSING_ERROR", "Error processing message: " + e.getMessage());
        }
    }

    /**
     * 处理二进制消息：按 {@code fw.plus.ws.binary.type} 声明的业务类型进入订阅派发链路。
     *
     * @param session WebSocket会话对象
     * @param payload 二进制内容
     */
    private void handleBinaryMessage(WebSocketSession session, byte[] payload) {
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        if (sessionInfo == null) {
            return;
        }
        String businessType = getBinaryBusinessType();
        List<? extends WsSubscribeScript> subscribeInfoList =
                findEnabledSubscribes(sessionInfo.getAppId(), businessType);
        for (WsSubscribeScript subscribeInfo : subscribeInfoList) {
            try {
                WsInvocationContext context = new WsInvocationContext(WsInvocationContext.Phase.MESSAGE,
                        sessionInfo.getAppId(), sessionInfo.getClientId(), session.getId(),
                        WsInvocationContext.MessageKind.BINARY, businessType, null, payload,
                        new ServletWsSession(session), createReplyHandle(session));
                businessDispatcher.execute(subscribeInfo, context);
            } catch (Exception e) {
                log.error("Error processing binary subscribe message: {}", e.getMessage(), e);
            }
        }
    }

    private void sendErrorMessage(WebSocketSession session, String code, String message) throws IOException {
        JSONObject errorObject = new JSONObject();
        errorObject.put("type", "error");
        errorObject.put("success", false);
        errorObject.put("code", code);
        errorObject.put("message", message);
        errorObject.put("timestamp", System.currentTimeMillis());
        sendMessage(session, JSON.toJSONString(errorObject));
    }

    private void closeSession(WebSocketSession session, String reason, CloseStatus status) {
        try {
            if (session.isOpen()) {
                session.close(status.withReason(reason));
            }
        } catch (Exception e) {
            log.error("Error closing session: {}", session.getId(), e);
        }
    }

    private void cleanupSessionResources(WebSocketSession session, String reason, CloseStatus status, boolean closeSession) {
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        if (sessionInfo != null && !Boolean.TRUE.equals(session.getAttributes().get("closeScriptExecuted"))) {
            session.getAttributes().put("closeScriptExecuted", Boolean.TRUE);
            try {
                end(sessionInfo.getAppId(), session.getId(), sessionInfo.getClientId());
            } catch (Exception e) {
                log.error("Error executing close scripts for session: {}", session.getId(), e);
            }
        }
        sessionSendLockMap.remove(session.getId());
        session.getAttributes().remove("sessionInfo");
        if (closeSession) {
            delayedClose(session, reason, status, getDelayMillis());
        }
    }

    private void sendPongMessage(WebSocketSession session, PingMessage message) throws IOException {
        updateLastActiveTime(session);
        Object sendLock = sessionSendLockMap.computeIfAbsent(session.getId(), key -> new Object());
        synchronized (sendLock) {
            if (!isSessionValid(session)) {
                return;
            }
            session.sendMessage(new PongMessage(message.getPayload()));
        }
    }

    private void updateLastActiveTime(WebSocketSession session) {
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        if (sessionInfo != null) {
            sessionInfo.setLastActiveTime(System.currentTimeMillis());
            session.getAttributes().put("sessionInfo", sessionInfo);
        }
    }

    /**
     * 根据业务类型处理消息
     *
     * @param session      WebSocket会话对象
     * @param businessType 业务类型
     * @param jsonObject   消息内容的JSON对象
     */
    private void handleBusinessMessage(WebSocketSession session, String businessType, JSONObject jsonObject) {
        // 根据业务类型处理不同的业务逻辑
        if ("ping".equals(businessType)) {
            handlePingMessage(session, businessType);
            return;
        }
        // 从会话属性中获取会话信息
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        if (sessionInfo == null) {
            log.warn("Session info not found for session: {}", session.getId());
            return;
        }
        String appId = sessionInfo.getAppId();
        List<? extends WsSubscribeScript> subscribeInfoList = findEnabledSubscribes(appId, businessType);
        if (CollectionUtils.isNotEmpty(subscribeInfoList)) {
            // 处理基于订阅信息的自定义业务逻辑
            log.info("Processing custom business logic for appId: {} with businessType: {}", appId, businessType);
            handleSubscribeMessage(session, appId, businessType, jsonObject, subscribeInfoList);
        } else {
            try {
                sendErrorMessage(session, "UNKNOWN_TYPE", "No enabled websocket subscribe script matched message type");
            } catch (IOException e) {
                log.error("Failed to send unknown type error message", e);
            }
        }
    }

    /**
     * 处理ping消息并向客户端发送pong响应
     *
     * @param session      WebSocket会话对象，用于向客户端发送响应消息
     * @param businessType 业务类型标识
     */
    private void handlePingMessage(WebSocketSession session, String businessType) {
        try {
            // 更新会话的最后ping时间
            SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
            if (sessionInfo != null) {
                sessionInfo.setLastActiveTime(System.currentTimeMillis());
                // 更新会话属性
                session.getAttributes().put("sessionInfo", sessionInfo);
            }

            // 通过重新放入缓存项来续期（session 对象是同一个引用，无需替换列表元素）
            String clientId = sessionInfo != null ? sessionInfo.getClientId() : null;
            if (clientId != null) {
                List<WebSocketSession> cachedSessionList = clientIdSessionListMap.getIfPresent(clientId);
                if (cachedSessionList != null) {
                    clientIdSessionListMap.put(clientId, cachedSessionList);
                }
            }
            if (StringUtils.isNotBlank(businessType)) {
                // 回复pong消息
                BusinessJsonBean jsonBean = new BusinessJsonBean();
                jsonBean.setBusinessType(businessType);
                jsonBean.setData("pong");
                jsonBean.setTimestamp(System.currentTimeMillis());
                sendMessage(session, JSON.toJSONString(jsonBean));
            } else {
                sendMessage(session, "pong");
            }
        } catch (Exception e) {
            log.error("Failed to send pong message", e);
        }
    }

    /**
     * 处理订阅消息：交由业务侧执行对应脚本
     *
     * @param session           WebSocket会话对象
     * @param appId             应用ID
     * @param businessType      业务类型
     * @param jsonObject        消息内容的JSON对象
     * @param subscribeInfoList 订阅信息列表
     */
    private void handleSubscribeMessage(WebSocketSession session, String appId, String businessType,
                                       JSONObject jsonObject, List<? extends WsSubscribeScript> subscribeInfoList) {
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        for (WsSubscribeScript subscribeInfo : subscribeInfoList) {
            try {
                WsInvocationContext context = new WsInvocationContext(WsInvocationContext.Phase.MESSAGE,
                        appId, sessionInfo == null ? null : sessionInfo.getClientId(), session.getId(),
                        businessType, jsonObject, new ServletWsSession(session), createReplyHandle(session));
                Object result = businessDispatcher.execute(subscribeInfo, context);
                log.info("Result of {} sessionId:{} ,appId:{} script: {}", businessType, session.getId(), appId, result);
            } catch (Exception e) {
                log.error("Error processing subscribe message: {}", e.getMessage(), e);
                try {
                    sendErrorMessage(session, "SCRIPT_ERROR", "Error processing websocket subscribe script: " + e.getMessage());
                } catch (IOException sendException) {
                    log.error("Failed to send script error message", sendException);
                }
            }
        }
    }

    /**
     * 处理WebSocket传输错误的回调方法
     *
     * @param session   发生错误的WebSocket会话对象
     * @param exception 抛出的异常对象
     */
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("WebSocket transport error - Session ID: {}", session.getId(), exception);
        try {
            SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
            if (sessionInfo != null) {
                cleanSession(sessionInfo.getClientId(), session.getId());
                return;
            }
        } catch (Exception e) {
            log.warn("Failed to clean session after transport error: {}", session.getId(), e);
        }
        delayedClose(session, "Error occurred", CloseStatus.PROTOCOL_ERROR, getDelayMillis());
    }

    /**
     * 在WebSocket连接关闭后调用此方法，用于清理会话相关的资源
     *
     * @param session     WebSocket会话对象，代表与客户端的连接
     * @param closeStatus 连接关闭的状态信息
     * @throws Exception 如果处理过程中发生错误
     */
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus closeStatus) throws Exception {
        String sessionId = session.getId();
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        if (sessionInfo == null) {
            log.warn("Session info not found for closed session: {}", sessionId);
            return;
        }
        String appId = sessionInfo.getAppId();
        String clientId = sessionInfo.getClientId();
        log.info("WebSocket connection closed - Session ID: {}, Client ID: {}, App ID: {}, Close status: {}",
                sessionId, clientId, appId, closeStatus);
        cleanSession(clientId, sessionId);
    }

    /**
     * 清理指定客户端的WebSocket会话。
     * <p>
     * 根据传入的客户端ID和可选的会话ID，关闭并清理对应的WebSocket会话。
     * 如果未指定会话ID，则清理该客户端的所有会话；
     * 如果指定了会话ID，则仅清理匹配的会话。
     * 同时更新或清理内部缓存状态。
     * </p>
     *
     * @param clientId  客户端唯一标识符，用于定位对应的会话列表
     * @param sessionId 可选的会话ID，用于精确指定要清理的会话；
     *                  若为null或空白字符串，则表示清理该客户端的所有会话
     */
    private void cleanSession(String clientId, String sessionId) {
        List<WebSocketSession> sessionList = clientIdSessionListMap.getIfPresent(clientId);
        if (sessionList != null) {
            List<WebSocketSession> sessionsToRemove = new ArrayList<>();
            for (WebSocketSession session : sessionList) {
                if (StringUtils.isNotBlank(sessionId)) {
                    if (session.getId().equals(sessionId)) {
                        sessionsToRemove.add(session);
                    }
                } else {
                    sessionsToRemove.add(session);
                }
            }

            for (WebSocketSession session : sessionsToRemove) {
                cleanupSessionResources(session, "Session closed", CloseStatus.NORMAL, true);
            }
            sessionList.removeAll(sessionsToRemove);

            if (sessionList.isEmpty()) {
                clientIdSessionListMap.asMap().computeIfPresent(clientId, (k, v) -> v.isEmpty() ? null : v);
            } else {
                clientIdSessionListMap.put(clientId, sessionList);
            }
        }
    }

    /**
     * 添加WebSocket会话到管理系统
     *
     * @param appId    应用程序ID，用于标识会话所属的应用
     * @param session  WebSocket会话对象，表示客户端与服务器的连接
     * @param clientId 客户端ID，用于标识会话所属的客户端
     */
    private void addSession(String appId, WebSocketSession session, String clientId) {
        String sessionId = session.getId();

        SessionInfo sessionInfo = new SessionInfo(appId, clientId);
        session.getAttributes().put("sessionInfo", sessionInfo);

        List<WebSocketSession> sessions = clientIdSessionListMap.asMap()
                .computeIfAbsent(clientId, k -> new CopyOnWriteArrayList<>());
        sessions.add(session);
        // 显式 put 以刷新 Guava Cache 的写入过期计时，避免活跃客户端因仅通过 asMap 操作而被驱逐
        clientIdSessionListMap.put(clientId, sessions);

        log.debug("Added session - Session ID: {}, Client ID: {}, App ID: {}",
                sessionId, clientId, appId);
    }

    /**
     * 向指定的WebSocket会话发送文本消息
     *
     * @param session WebSocket会话对象
     * @param message 要发送的消息内容
     * @throws Exception 如果发送消息过程中发生错误
     */
    private void sendMessage(WebSocketSession session, String message) throws IOException {
        if (!isSessionValid(session)) {
            log.warn("Attempted to send message to invalid session: {}", session.getId());
            return;
        }
        Object sendLock = sessionSendLockMap.computeIfAbsent(session.getId(), key -> new Object());
        synchronized (sendLock) {
            if (!isSessionValid(session)) {
                log.warn("Attempted to send message to invalid session after waiting for send lock: {}", session.getId());
                return;
            }
            session.sendMessage(new TextMessage(message));
        }
    }

    /**
     * 为指定会话构造结果回发句柄，复用与内部发送相同的会话校验与加锁逻辑。
     *
     * @param session 目标会话
     * @return 回发句柄
     */
    private WsReplyHandle createReplyHandle(WebSocketSession session) {
        return new WsReplyHandle() {
            @Override
            public void sendText(String message) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("Failed to reply message to session: {}", session.getId(), e);
                }
            }

            @Override
            public void sendBinary(byte[] payload) {
                try {
                    sendBinaryMessage(session, payload);
                } catch (Exception e) {
                    log.error("Failed to reply binary message to session: {}", session.getId(), e);
                }
            }
        };
    }

    /**
     * 向指定的WebSocket会话发送二进制消息。
     *
     * @param session WebSocket会话对象
     * @param payload 要发送的二进制内容
     * @throws IOException 发送失败时抛出
     */
    private void sendBinaryMessage(WebSocketSession session, byte[] payload) throws IOException {
        if (!isSessionValid(session)) {
            log.warn("Attempted to send binary message to invalid session: {}", session.getId());
            return;
        }
        Object sendLock = sessionSendLockMap.computeIfAbsent(session.getId(), key -> new Object());
        synchronized (sendLock) {
            if (!isSessionValid(session)) {
                log.warn("Attempted to send binary message to invalid session after waiting for send lock: {}",
                        session.getId());
                return;
            }
            session.sendMessage(new BinaryMessage(payload));
        }
    }

    /**
     * 异步广播投递线程池，仅在 {@code fw.plus.ws.broadcast.async=true} 时创建。
     *
     * @return 广播线程池
     */
    private synchronized WsBroadcastExecutor broadcastExecutor() {
        if (broadcastExecutor == null) {
            broadcastExecutor = new WsBroadcastExecutor(
                    getScriptThreadPoolCoreSize(), getScriptThreadPoolMaxSize(), getScriptThreadPoolQueueCapacity());
        }
        return broadcastExecutor;
    }

    /**
     * 当前登记的会话总数，用于全局连接数上限判断。
     *
     * @return 会话总数
     */
    public int sessionCount() {
        int total = 0;
        for (List<WebSocketSession> sessions : clientIdSessionListMap.asMap().values()) {
            total += sessions == null ? 0 : sessions.size();
        }
        return total;
    }

    /**
     * 指定 clientId 下登记的会话列表。
     *
     * @param clientId 客户端 ID
     * @return 会话列表，未登记时返回空列表
     */
    public List<WebSocketSession> sessionsOf(String clientId) {
        List<WebSocketSession> sessions = clientIdSessionListMap.getIfPresent(clientId);
        return sessions == null ? Collections.emptyList() : sessions;
    }

    /**
     * 刷新指定会话所属 clientId 的缓存过期计时
     */
    private void refreshCacheEntry(WebSocketSession session) {
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        if (sessionInfo != null) {
            String clientId = sessionInfo.getClientId();
            if (clientId != null) {
                List<WebSocketSession> sessionList = clientIdSessionListMap.getIfPresent(clientId);
                if (sessionList != null) {
                    clientIdSessionListMap.put(clientId, sessionList);
                }
            }
        }
    }

    private boolean isSessionValid(WebSocketSession session) {
        // 检查会话是否打开以及是否仍在缓存中（未过期）
        if (session == null || !session.isOpen()) {
            return false;
        }

        // 检查会话信息是否存在
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        if (sessionInfo == null) {
            return false;
        }

        // 检查会话是否超时
        Long lastActiveTime = sessionInfo.getLastActiveTime();
        if (lastActiveTime == null) {
            return false;
        }
        long timeoutMillis = (long) getSessionCacheExpireMinutes() * 60 * 1000;
        return (System.currentTimeMillis() - lastActiveTime) <= timeoutMillis;
    }

    /**
     * 仅向本地指定客户端投递消息，不发布到 Redis（供Redis消息监听器使用，避免消息反弹循环）。
     *
     * <p>开启 {@code fw.plus.ws.broadcast.async} 后投递改为尽力而为的异步提交，
     * 返回值只表示任务已被接受，不再表示已实际发出。</p>
     *
     * @return true 同步模式下表示至少成功发送一条；异步模式下表示任务已被接受
     */
    boolean broadcastMessageToClientLocalOnly(String appId, String clientId, String message) {
        if (isBroadcastAsync()) {
            return broadcastExecutor().submit(() -> doBroadcastClientLocalOnly(appId, clientId, message));
        }
        return doBroadcastClientLocalOnly(appId, clientId, message);
    }

    private boolean doBroadcastClientLocalOnly(String appId, String clientId, String message) {
        List<WebSocketSession> sessionList = this.clientIdSessionListMap.getIfPresent(clientId);
        boolean sent = false;
        if (CollectionUtils.isNotEmpty(sessionList)) {
            List<WebSocketSession> sessionsToRemove = new ArrayList<>();
            List<WebSocketSession> sessionsToSend = new ArrayList<>();
            for (WebSocketSession session : sessionList) {
                SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
                if (sessionInfo != null && appId.equals(sessionInfo.getAppId())) {
                    if (isSessionValid(session)) {
                        sessionsToSend.add(session);
                    } else {
                        sessionsToRemove.add(session);
                    }
                }
            }
            for (WebSocketSession session : sessionsToSend) {
                try {
                    sendMessage(session, message);
                    sent = true;
                } catch (Exception e) {
                    log.error("Failed to send message to session: {}", session.getId(), e);
                    sessionsToRemove.add(session);
                }
            }
            for (WebSocketSession session : sessionsToRemove) {
                cleanupSessionResources(session, "Session invalid", CloseStatus.SESSION_NOT_RELIABLE, true);
            }
            sessionList.removeAll(sessionsToRemove);
            if (sessionList.isEmpty()) {
                this.clientIdSessionListMap.asMap().computeIfPresent(clientId, (k, v) -> v.isEmpty() ? null : v);
            }
        }
        return sent;
    }

    /**
     * 向指定客户端的所有会话广播消息（本地 + 分布式）。
     *
     * @param appId    应用ID
     * @param clientId 客户端ID
     * @param message  消息内容
     */
    public void broadcastMessageToClient(String appId, String clientId, String message) {
        broadcastMessageToClientLocalOnly(appId, clientId, message);
        broadcastMessageToClient(this.webSocketServerId, appId, clientId, message);
    }

    /**
     * 延迟关闭WebSocket会话
     *
     * @param session     WebSocket会话对象
     * @param message     要发送的消息内容
     * @param status      关闭状态
     * @param delayMillis 延迟时间（毫秒）
     */
    private void delayedClose(WebSocketSession session, String message, CloseStatus status, int delayMillis) {
        try {
            scheduledExecutor.schedule(() -> {
                try {
                    if (session.isOpen()) {
                        session.close(status.withReason(message));
                    }
                } catch (Exception e) {
                    log.error("Error closing session: {}", session.getId(), e);
                }
            }, delayMillis, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Error during delayed close operation for session: {}", session.getId(), e);
            try {
                if (session.isOpen()) {
                    session.close(status);
                }
            } catch (Exception closeException) {
                log.error("Error closing session immediately: {}", session.getId(), closeException);
            }
        }
    }

    /**
     * 向指定客户端发送消息，排除指定的服务器ID
     *
     * @param excludedServerId 需要排除的服务器ID，不会向该服务器上的客户端发送消息
     * @param appId            应用ID
     * @param clientId         客户端ID
     * @param message          要发送的消息内容
     */
    abstract void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message);

    /**
     * 仅向本地客户端广播消息，不发布到Redis（供Redis消息监听器使用，避免消息放大循环）
     */
    public void broadcastMessageToAllClientLocalOnly(String appId, String message) {
        if (appId == null) {
            log.warn("broadcastMessageToAllClientLocalOnly called with null appId, ignoring");
            return;
        }
        if (isBroadcastAsync()) {
            broadcastExecutor().submit(() -> doBroadcastAllClientLocalOnly(appId, message));
            return;
        }
        doBroadcastAllClientLocalOnly(appId, message);
    }

    private void doBroadcastAllClientLocalOnly(String appId, String message) {
        Map<String, List<WebSocketSession>> sessionsToCleanup = new LinkedHashMap<>();
        for (Map.Entry<String, List<WebSocketSession>> entry : clientIdSessionListMap.asMap().entrySet()) {
            for (WebSocketSession session : entry.getValue()) {
                try {
                    SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
                    if (sessionInfo == null || !isSessionValid(session)) {
                        sessionsToCleanup.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(session);
                        continue;
                    }
                    if (!appId.equals(sessionInfo.getAppId())) {
                        continue;
                    }
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("Failed to send message to session: {}", session.getId(), e);
                    sessionsToCleanup.computeIfAbsent(entry.getKey(), k -> new ArrayList<>()).add(session);
                }
            }
        }
        for (Map.Entry<String, List<WebSocketSession>> entry : sessionsToCleanup.entrySet()) {
            String clientId = entry.getKey();
            List<WebSocketSession> sessionList = clientIdSessionListMap.getIfPresent(clientId);
            if (sessionList != null) {
                for (WebSocketSession session : entry.getValue()) {
                    cleanupSessionResources(session, "Session invalid", CloseStatus.SESSION_NOT_RELIABLE, true);
                }
                sessionList.removeAll(entry.getValue());
                if (sessionList.isEmpty()) {
                    clientIdSessionListMap.asMap().computeIfPresent(clientId, (k, v) -> v.isEmpty() ? null : v);
                }
            }
        }
    }

    /**
     * 向所有客户端广播消息（本地 + 分布式）。
     *
     * @param appId   应用ID
     * @param message 消息内容
     */
    protected void broadcastMessageToAllClient(String appId, String message) {
        broadcastMessageToAllClientLocalOnly(appId, message);
        broadcastMessageToAllClient(webSocketServerId, appId, message);
    }

    /**
     * 向所有客户端广播消息，排除指定的服务器ID
     *
     * @param excludedServerId 需要排除的服务器ID，不会向该服务器上的客户端发送消息
     * @param appId            应用ID
     * @param message          要广播的消息内容
     */
    abstract void broadcastMessageToAllClient(String excludedServerId, String appId, String message);

    /**
     * 向所有客户端广播消息（支持分布式环境）
     *
     * @param appId   应用ID
     * @param message 消息内容
     */
    public void broadcastDistributedMessageToAllClient(String appId, String message) {
        // 先向本地客户端广播消息
        broadcastMessageToAllClient(appId, message);
    }

    /**
     * 指示WebSocket处理器是否支持部分消息
     *
     * @return boolean值，true表示支持部分消息，false表示不支持
     */
    @Override
    public boolean supportsPartialMessages() {
        // 从配置中获取是否支持部分消息的设置，默认为false
        return getSupportsPartialMessages();
    }

    /**
     * 业务 JSON Bean 类，用于封装业务相关的 JSON 数据。
     *
     * <p>字段与 magic-api 的 {@code JsonBean} 对齐（{@code code=1}、{@code message="success"}、
     * {@code data}、{@code timestamp}、{@code executeTime}），保证 ping/pong 等响应体的
     * JSON 结构在拆分前后一致。</p>
     */
    @Data
    public static class BusinessJsonBean {
        /**
         * 状态码
         */
        private int code = 1;

        /**
         * 状态说明
         */
        private String message = "success";

        /**
         * 数据
         */
        private Object data;

        /**
         * 时间戳
         */
        private long timestamp = System.currentTimeMillis();

        /**
         * 执行耗时
         */
        private Integer executeTime;

        /**
         * 业务类型
         */
        private String businessType;

        public BusinessJsonBean() {
        }
    }

    /**
     * 会话信息类，包含与会话相关的所有元数据
     */
    @Data
    private static class SessionInfo {
        /**
         * 客户端ID
         */
        private String clientId;

        /**
         * 应用ID
         */
        private String appId;

        /**
         * 最后一次ping时间
         */
        private Long lastActiveTime;

        /**
         * 会话创建时间
         */
        private Long createTime;

        public SessionInfo(String appId, String clientId) {
            this.appId = appId;
            this.clientId = clientId;
            this.createTime = System.currentTimeMillis();
            this.lastActiveTime = this.createTime;
        }
    }
}
