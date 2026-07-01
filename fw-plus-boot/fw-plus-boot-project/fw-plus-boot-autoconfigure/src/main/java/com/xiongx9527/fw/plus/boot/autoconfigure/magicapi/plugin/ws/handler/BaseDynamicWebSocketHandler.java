package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.JSONValidator;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import io.jsonwebtoken.lang.Collections;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.magicapi.utils.ScriptManager;
import org.ssssssss.script.MagicScriptContext;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseDynamicWebSocketHandler implements WebSocketHandler , BaseService {

    /**
     * 共享的延迟关闭调度器，避免每次 delayedClose 创建新线程池导致线程泄漏
     */
    private final ScheduledExecutorService scheduledExecutor = Executors.newScheduledThreadPool(2, r -> {
        Thread t = new Thread(r, "ws-delayed-close");
        t.setDaemon(true);
        return t;
    });

    /**
     * 缓存的JWT密钥和签名算法，避免每次验证/生成token时重复计算MessageDigest（延迟初始化）
     */
    private volatile com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService.SecretKeyAndSignatureAlgorithm cachedSecretKeyAndAlgorithm;

    /**
     * 存储客户端ID与会话列表的映射关系，用于管理同一客户端的多个会话
     * 使用Guava Cache限制大小并支持自动过期
     */
    private final com.google.common.cache.Cache<String, List<WebSocketSession>> clientIdSessionListMap =
            com.google.common.cache.CacheBuilder.newBuilder()
                    .maximumSize(getMaxClientSessions())
                    .expireAfterWrite(getSessionCacheExpireMinutes(), java.util.concurrent.TimeUnit.MINUTES)
                    .concurrencyLevel(getCacheConcurrencyLevel())
                    .removalListener(new com.google.common.cache.RemovalListener<String, List<WebSocketSession>>() {
                        @Override
                        public void onRemoval(com.google.common.cache.RemovalNotification<String, List<WebSocketSession>> notification) {
                            log.warn("Client session list removed from cache: {} - {}",
                                    notification.getKey(), notification.getCause());
                            try {
                                List<WebSocketSession> sessions = notification.getValue();
                                if (sessions != null) {
                                    for (WebSocketSession session : sessions) {
                                        try {
                                            log.warn("Session is still open: {}", session.getId());
                                            delayedClose(session,"SERVICE OVERLOAD", CloseStatus.SERVICE_OVERLOAD, getDelayMillis());
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

    /**
     * WebSocket数据源信息存储
     */
    private final WsDataSourceInfoMagicResourceStorage magicDataSourceStorage;

    /**
     * WebSocket订阅信息存储
     */
    private final WsSubscribeInfoMagicResourceStorage magicSubscribeStorage ;
    
    /**
     * WebSocket服务器ID，用于标识当前服务器实例
     */
    private final String webSocketServerId = generateWebSocketServerId();

    /**
     * WebSocket处理器构造函数
     * 
     * @param magicDataSourceStorage WebSocket数据源信息存储
     * @param magicSubscribeStorage WebSocket订阅信息存储
     */
    public BaseDynamicWebSocketHandler(WsDataSourceInfoMagicResourceStorage magicDataSourceStorage, WsSubscribeInfoMagicResourceStorage magicSubscribeStorage) {
        this.magicDataSourceStorage = magicDataSourceStorage;
        this.magicSubscribeStorage = magicSubscribeStorage;
        addShutdownHook();
    }

    /**
     * 添加JVM关闭钩子，确保在应用关闭时能清理所有资源
     */
    private void addShutdownHook() {
        Runtime.getRuntime().addShutdownHook(new Thread(this::cleanupAllResources));
    }

    /**
     * 清理所有资源，在应用关闭时调用
     */
    private void cleanupAllResources() {
        try {
            // 先收集所有活跃会话引用，避免 session.close() 触发 afterConnectionClosed
            // 回调中修改缓存导致迭代器行为不确定
            List<WebSocketSession> allSessions = new java.util.ArrayList<>();
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
                    session.close(CloseStatus.GOING_AWAY.withReason("Server shutting down"));
                } catch (Exception e) {
                    log.warn("Error closing session {} during shutdown", session.getId());
                }
            }
            clientIdSessionListMap.invalidateAll();
            scheduledExecutor.shutdown();
            log.info("All WebSocket resources have been cleaned up");
        } catch (Exception e) {
            log.error("Error during cleanup of all resources", e);
        }
    }

    /**
     * 返回缓存的JWT密钥和签名算法，延迟初始化避免Spring上下文未就绪问题
     */
    @Override
    public com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.common.BaseService.SecretKeyAndSignatureAlgorithm getSecretKeyAndSignatureAlgorithm() {
        if (cachedSecretKeyAndAlgorithm == null) {
            synchronized (this) {
                if (cachedSecretKeyAndAlgorithm == null) {
                    cachedSecretKeyAndAlgorithm = BaseService.super.getSecretKeyAndSignatureAlgorithm();
                }
            }
        }
        return cachedSecretKeyAndAlgorithm;
    }

    /**
     * 生成WebSocket服务器ID
     *
     * @return WebSocket服务器ID
     */
    private String generateWebSocketServerId() {
        try {
            // 尝试获取主机名和进程ID来生成唯一ID
            String hostName = java.net.InetAddress.getLocalHost().getHostName();
            String processId = java.lang.management.ManagementFactory.getRuntimeMXBean().getName().split("@")[0];
            return hostName + "-" + processId;
        } catch (Exception e) {
            // 如果无法获取主机信息，则使用UUID生成唯一ID
            log.warn("Failed to generate server ID based on host and process, using UUID instead", e);
            return "server-" + java.util.UUID.randomUUID().toString();
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
        Map<String, String> params = new java.util.HashMap<>();
        if (query != null && !query.isEmpty()) {
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                String[] keyValue = pair.split("=", 2);
                if (keyValue.length == 2) {
                    try {
                        String key = java.net.URLDecoder.decode(keyValue[0], "UTF-8");
                        String value = java.net.URLDecoder.decode(keyValue[1], "UTF-8");
                        params.put(key, value);
                    } catch (java.io.UnsupportedEncodingException e) {
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
	 * 初始化WebSocket连接
	 * 根据应用ID、会话ID和客户端ID查找并执行相应的订阅脚本
	 *
	 * @param appId 应用ID，用于匹配订阅信息
	 * @param sessionId 会话ID，用于标识WebSocket会话
	 * @param clientId 客户端ID，用于标识客户端
	 */
	private void init(String appId, String sessionId, String clientId) {
		// 获取初始化的业务类型
		String businessType = getInitBussinessType();
		List<WsSubscribeInfo> subscribeInfoList = findEnabledSubscribes(appId, businessType);
		// 遍历订阅信息列表，执行相应的脚本
		for (WsSubscribeInfo subscribeInfo : subscribeInfoList) {
            // 创建脚本执行上下文
            MagicScriptContext magicScriptContext = new MagicScriptContext();
			try {
				magicScriptContext.setScriptName(subscribeInfo.getName());
				// 设置脚本变量
				magicScriptContext.set("clientId", clientId);
				magicScriptContext.set("appId", appId);
				magicScriptContext.set("businessType", subscribeInfo.getBusinessId());
				magicScriptContext.set("sessionId", sessionId);
				// 执行脚本并记录结果
				Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
				log.info("Result of init sessionId:{} ,appId:{} script: {}", sessionId, appId, result);
			} catch (Exception e) {
				// 记录脚本执行错误
				log.error("Error processing subscribe message: {}", e.getMessage(), e);
			}
		}
	}

    /**
     * 查找启用且匹配指定appId和业务类型的订阅信息列表
     */
    private List<WsSubscribeInfo> findEnabledSubscribes(String appId, String businessType) {
        return ListUtils.emptyIfNull(magicSubscribeStorage.findAll()).stream()
                .filter(s -> Boolean.TRUE.equals(s.isEnabled())
                        && s.getKey().equals(appId)
                        && businessType.equals(s.getBusinessId()))
                .collect(Collectors.toList());
    }

    /**
     * 根据应用ID清理相关的WebSocket会话连接
     *
     * @param appId 应用ID，用于筛选需要清理的会话
     */
    public void cleanupByAppId(String appId) {
        Map<String, List<WebSocketSession>> snapshot = clientIdSessionListMap.asMap();
        // 使用 List 存储 (clientId, sessionId) 对，避免 HashMap 覆盖导致同 clientId 多会话时清理遗漏
        List<String[]> pairs = new java.util.ArrayList<>();
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
            String query = uri != null ? uri.getQuery() : null;

            // 获取Session ID
            log.info("WebSocket connection established - Session ID: {}, Client IP: {}", sessionId, clientIp);

            // 从查询参数中获取token和clientId
            Map<String, String> queryParams = parseQueryParams(query);
            String token = queryParams.get(getTokenParamName());
            String clientId = queryParams.get(getClientIdParamName());
            
            // 校验必要参数
            if (StringUtils.isBlank(token) || StringUtils.isBlank(clientId)) {
                log.error("Missing required parameters - Token: {}, Client ID: {}", token, clientId);
                delayedClose(session, "缺少必要参数", CloseStatus.BAD_DATA, getDelayMillis());
                return;
            }

            // 检查clientId长度
            int maxClientIdLength = getMaxClientIdLength();
            if (clientId.length() > maxClientIdLength) {
                log.error("Client ID too long - Length: {}, Max: {}", clientId.length(), maxClientIdLength);
                delayedClose(session, "客户端ID过长", CloseStatus.BAD_DATA, getDelayMillis());
                return;
            }
            
            // 验证token并提取appId
            String appId;
            try {
                appId = validateAndExtractAppIdFromToken(token);
                if (StringUtils.isBlank(appId)) {
                    log.error("Invalid token: {}", token);
                    delayedClose(session, "无效的token", CloseStatus.BAD_DATA, getDelayMillis());
                    return;
                }
            } catch (Exception e) {
                log.error("Failed to validate token: {}", token, e);
                delayedClose(session, "token验证失败", CloseStatus.BAD_DATA, getDelayMillis());
                return;
            }

            //判断appid 是否存在
            boolean isAppIdExist = ListUtils.emptyIfNull(magicDataSourceStorage.findAll()).stream().anyMatch(e->e.getKey().equals(appId));
            if(!isAppIdExist){
                log.error("AppId not exist: {}", appId);
                delayedClose(session, "AppId不存在", CloseStatus.BAD_DATA, getDelayMillis());
                return;
            }
            
            // 添加会话到管理映射
            addSession(appId,session,clientId);
            
            // 执行初始化脚本
            init(appId, sessionId,clientId);
            
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
        // 刷新缓存过期计时，避免仅发送业务消息不发 ping 的活跃客户端被驱逐
        refreshCacheEntry(session);

        log.debug("Received message from client - Session ID: {}, Message: {}",
                session.getId(), message.getPayload());
        try {
            // 获取消息内容
            String payload = (String) message.getPayload();
            if ("ping".equals(payload)) {
                handlePingMessage(session, null);
                return;
            }
            log.debug("payload:{}",payload);
            JSONValidator validator = JSONValidator.from(payload);
            com.alibaba.fastjson2.JSONObject jsonObject = null;
            String businessType = null;
            if (validator.validate()) {
                // 解析JSON消息
                jsonObject = com.alibaba.fastjson2.JSON.parseObject(payload);
                // 获取业务类型
                businessType = jsonObject.getString("type");
            }
            if (StringUtils.isNotBlank(businessType)) {
                // 根据业务类型处理消息，复用已解析的jsonObject避免重复解析
                handleBusinessMessage(session, businessType, jsonObject);
            } else {
                log.debug("Ignoring message without business type from session: {}", session.getId());
            }
        } catch (com.alibaba.fastjson2.JSONException e) {
            log.error("Invalid JSON format in message: {}", message.getPayload(), e);
            org.ssssssss.magicapi.core.model.JsonBean errorBean = new org.ssssssss.magicapi.core.model.JsonBean();
            errorBean.setCode(getErrorCode("INVALID_JSON", 400));
            errorBean.setMessage("Invalid JSON format");
            sendMessage(session, errorBean);
        } catch (Exception e) {
            log.error("Error processing message: {}", message.getPayload(), e);
            org.ssssssss.magicapi.core.model.JsonBean errorBean = new org.ssssssss.magicapi.core.model.JsonBean();
            errorBean.setCode(getErrorCode("PROCESSING_ERROR", 500));
            errorBean.setMessage("Error processing message: " + e.getMessage());
            sendMessage(session, errorBean);
        }
    }
    
    /**
     * 根据业务类型处理消息
     * 
     * @param session WebSocket会话对象
     * @param businessType 业务类型
     * @param jsonObject 消息内容的JSON对象
     */
    private void handleBusinessMessage(WebSocketSession session, String businessType, com.alibaba.fastjson2.JSONObject jsonObject) {
        // 根据业务类型处理不同的业务逻辑
        switch (businessType) {
            case "ping":
                handlePingMessage(session, businessType);
                break;
            default:
                // 从会话属性中获取会话信息
                SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
                if (sessionInfo == null) {
                    log.warn("Session info not found for session: {}", session.getId());
                    return;
                }
                String appId = sessionInfo.getAppId();
                List<WsSubscribeInfo> subscribeInfoList = findEnabledSubscribes(appId, businessType);
                if (CollectionUtils.isNotEmpty(subscribeInfoList)) {
                    // 处理基于订阅信息的自定义业务逻辑
                    log.info("Processing custom business logic for appId: {} with businessType: {}", appId, businessType);
                    // 这里可以添加基于subscribeInfoList的自定义业务处理
                    handleSubscribeMessage(session, appId,businessType, jsonObject,subscribeInfoList);
                }
                break;
        }
    }

    /**
     * 处理ping消息并向客户端发送pong响应
     * 
     * @param session WebSocket会话对象，用于向客户端发送响应消息
     * @param businessType 业务类型标识
     */
    private void handlePingMessage(WebSocketSession session, String businessType) {
        try {
            // 更新会话的最后ping时间
            SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
            if (sessionInfo != null) {
                sessionInfo.setLastPingTime(System.currentTimeMillis());
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
     * 处理订阅消息
     * 
     * @param session WebSocket会话对象
     * @param appId 应用ID
     * @param businessType 业务类型
     * @param jsonObject 消息内容的JSON对象
     * @param subscribeInfoList 订阅信息列表
     */
    private void handleSubscribeMessage(WebSocketSession session, String appId , String businessType, JSONObject jsonObject, List<WsSubscribeInfo> subscribeInfoList) {
        SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
        for (WsSubscribeInfo subscribeInfo : subscribeInfoList) {
            MagicScriptContext magicScriptContext = new MagicScriptContext();
            try {
                session.getAttributes().put("subscribeInfo", subscribeInfo);
                magicScriptContext.setScriptName(subscribeInfo.getName());
                magicScriptContext.set("appId", appId);
                magicScriptContext.set("clientId",sessionInfo.getClientId());
                magicScriptContext.set("type", businessType);
                magicScriptContext.set("sessionId", session.getId());
                magicScriptContext.set("params", jsonObject);
                Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
                log.info("Result of {} sessionId:{} ,appId:{} script: {}", businessType,session.getId(),appId,result);
            } catch (Exception e) {
                log.error("Error processing subscribe message: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 结束WebSocket会话，执行订阅信息中的关闭脚本
     *
     * @param appId 应用ID
     * @param sessionId 会话ID
     * @param clientId 客户端ID
     */
    private void end(String appId,String sessionId,String clientId) {
        String businessType = getCloseBussinessType();
        List<WsSubscribeInfo> subscribeInfoList = findEnabledSubscribes(appId, businessType);
        // 为每个匹配的订阅信息执行相应的关闭脚本
        for (WsSubscribeInfo subscribeInfo : subscribeInfoList) {
            MagicScriptContext magicScriptContext = new MagicScriptContext();
            try {
                magicScriptContext.setScriptName(subscribeInfo.getName());
                magicScriptContext.set("clientId", clientId);
                magicScriptContext.set("appId", appId);
                magicScriptContext.set("type", subscribeInfo.getBusinessId());
                magicScriptContext.set("sessionId", sessionId);
                Object result = ScriptManager.executeScript(subscribeInfo.getScript(), magicScriptContext);
                log.info("Result of init sessionId:{} ,appId:{} script: {}", sessionId,appId,result);
            } catch (Exception e) {
                log.error("Error processing subscribe message: {}", e.getMessage(), e);
            }
        }
    }

    /**
     * 处理WebSocket传输错误的回调方法
     *
     * @param session 发生错误的WebSocket会话对象
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
     * @param session WebSocket会话对象，代表与客户端的连接
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
            List<WebSocketSession> sessionsToRemove = new java.util.ArrayList<>();
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
                // 先执行关闭脚本，再移除 sessionInfo，确保 afterConnectionClosed 回调
                // 或直接调用 cleanSession 的路径都能正确触发关闭脚本
                SessionInfo si = (SessionInfo) session.getAttributes().get("sessionInfo");
                if (si != null) {
                    try {
                        end(si.getAppId(), session.getId(), si.getClientId());
                    } catch (Exception e) {
                        log.error("Error executing close scripts for session: {}", session.getId(), e);
                    }
                }
                delayedClose(session, "Session closed", CloseStatus.NORMAL, getDelayMillis());
                session.getAttributes().remove("sessionInfo");
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
     * @param appId 应用程序ID，用于标识会话所属的应用
     * @param session WebSocket会话对象，表示客户端与服务器的连接
     * @param clientId 客户端ID，用于标识会话所属的客户端
     */
    private void addSession(String appId, WebSocketSession session, String clientId) {
        String sessionId = session.getId();

        SessionInfo sessionInfo = new SessionInfo(appId, clientId);
        session.getAttributes().put("sessionInfo", sessionInfo);

        List<WebSocketSession> sessions = clientIdSessionListMap.asMap()
                .computeIfAbsent(clientId, k -> new java.util.concurrent.CopyOnWriteArrayList<>());
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
    private void sendMessage(WebSocketSession session, String message) throws java.io.IOException {
        if (!isSessionValid(session)) {
            log.warn("Attempted to send message to invalid session: {}", session.getId());
            return;
        }
        session.sendMessage(new org.springframework.web.socket.TextMessage(message));
    }
    
    /**
     * 检查WebSocket会话是否有效
     * 
     * @param session WebSocket会话对象
     * @return 如果会话有效返回true，否则返回false
     */
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
        Long lastPingTime = sessionInfo.getLastPingTime();
        if (lastPingTime == null) {
            return false;
        }
        long timeoutMillis = (long) getSessionCacheExpireMinutes() * 60 * 1000;
        return (System.currentTimeMillis() - lastPingTime) <= timeoutMillis;
    }

    /**
     * 向指定的WebSocket会话发送JSON格式的消息
     * 
     * @param session WebSocket会话对象
     * @param jsonBean 包含消息内容的JsonBean对象
     * @throws Exception 如果发送消息过程中发生错误
     */
    private void sendMessage(WebSocketSession session, JsonBean jsonBean) throws java.io.IOException {
        sendMessage(session, JSON.toJSONString(jsonBean));
    }

    /**
     * 向指定客户端的所有会话发送消息，仅发送给与指定appId匹配的会话
     * 
     * @param appId 应用ID，用于过滤会话
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    /**
     * 仅向本地指定客户端投递消息，不发布到Redis（供Redis消息监听器使用，避免消息反弹循环）。
     *
     * @return true 如果至少有一条消息成功发送到本地会话
     */
    boolean broadcastMessageToClientLocalOnly(String appId, String clientId, String message) {
        List<WebSocketSession> sessionList = this.clientIdSessionListMap.getIfPresent(clientId);
        boolean sent = false;
        if (!Collections.isEmpty(sessionList)) {
            List<WebSocketSession> sessionsToRemove = new java.util.ArrayList<>();
            List<WebSocketSession> sessionsToSend = new java.util.ArrayList<>();
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
                session.getAttributes().remove("sessionInfo");
                delayedClose(session, "Session invalid", CloseStatus.SESSION_NOT_RELIABLE, getDelayMillis());
            }
            sessionList.removeAll(sessionsToRemove);
            if (sessionList.isEmpty()) {
                this.clientIdSessionListMap.asMap().computeIfPresent(clientId, (k, v) -> v.isEmpty() ? null : v);
            }
        }
        return sent;
    }

    public void broadcastMessageToClient(String appId, String clientId, String message) {
        broadcastMessageToClientLocalOnly(appId, clientId, message);
        broadcastMessageToClient(this.webSocketServerId, appId, clientId, message);
    }

    /**
     * 延迟关闭WebSocket会话
     * 
     * @param session WebSocket会话对象
     * @param message 要发送的消息内容
     * @param status 关闭状态
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
            }, delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
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
     * @param appId 应用ID
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    abstract void broadcastMessageToClient(String excludedServerId , String appId, String clientId,  String message) ;
    
    /**
     * 向所有客户端广播消息，仅发送给与指定appId匹配的会话
     * 
     * @param appId 应用ID，用于过滤会话
     * @param message 要广播的消息内容
     */
    /**
     * 仅向本地客户端广播消息，不发布到Redis（供Redis消息监听器使用，避免消息放大循环）
     */
    void broadcastMessageToAllClientLocalOnly(String appId, String message) {
        if (appId == null) {
            log.warn("broadcastMessageToAllClientLocalOnly called with null appId, ignoring");
            return;
        }
        java.util.Map<String, List<WebSocketSession>> sessionsToCleanup = new java.util.LinkedHashMap<>();
        for (java.util.Map.Entry<String, List<WebSocketSession>> entry : clientIdSessionListMap.asMap().entrySet()) {
            for (WebSocketSession session : entry.getValue()) {
                try {
                    SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
                    if (sessionInfo == null || !isSessionValid(session)) {
                        sessionsToCleanup.computeIfAbsent(entry.getKey(), k -> new java.util.ArrayList<>()).add(session);
                        continue;
                    }
                    if (!appId.equals(sessionInfo.getAppId())) {
                        continue;
                    }
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("Failed to send message to session: {}", session.getId(), e);
                    sessionsToCleanup.computeIfAbsent(entry.getKey(), k -> new java.util.ArrayList<>()).add(session);
                }
            }
        }
        for (java.util.Map.Entry<String, List<WebSocketSession>> entry : sessionsToCleanup.entrySet()) {
            String clientId = entry.getKey();
            List<WebSocketSession> sessionList = clientIdSessionListMap.getIfPresent(clientId);
            if (sessionList != null) {
                for (WebSocketSession session : entry.getValue()) {
                    session.getAttributes().remove("sessionInfo");
                    delayedClose(session, "Session invalid", CloseStatus.SESSION_NOT_RELIABLE, getDelayMillis());
                }
                sessionList.removeAll(entry.getValue());
                if (sessionList.isEmpty()) {
                    clientIdSessionListMap.asMap().computeIfPresent(clientId, (k, v) -> v.isEmpty() ? null : v);
                }
            }
        }
    }

    protected void broadcastMessageToAllClient(String appId, String message) {
        broadcastMessageToAllClientLocalOnly(appId, message);
        broadcastMessageToAllClient(webSocketServerId, appId, message);
    }

    /**
     * 向所有客户端广播消息，排除指定的服务器ID
     * 
     * @param excludedServerId 需要排除的服务器ID，不会向该服务器上的客户端发送消息
     * @param appId 应用ID
     * @param message 要发送的消息内容
     */
    abstract void broadcastMessageToAllClient(String excludedServerId , String appId, String message) ;
    
    /**
     * 向所有客户端广播消息（支持分布式环境）
     * 
     * @param appId 应用ID
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
     * 业务JSON Bean类，用于封装业务相关的JSON数据
     * 继承自JsonBean，添加了业务类型字段
     */
    @Data
    public static class BusinessJsonBean extends JsonBean<Object> {
        /**
         * 业务类型
         */
        private String businessType;
        
        /**
         * BusinessJsonBean构造函数
         */
        public BusinessJsonBean() {
            super();
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
        private Long lastPingTime;
        
        /**
         * 会话创建时间
         */
        private Long createTime;
        
        public SessionInfo(String appId,String clientId) {
            this.appId = appId;
            this.clientId = clientId;
            this.createTime = System.currentTimeMillis();
            this.lastPingTime = this.createTime;
        }
    }
}
