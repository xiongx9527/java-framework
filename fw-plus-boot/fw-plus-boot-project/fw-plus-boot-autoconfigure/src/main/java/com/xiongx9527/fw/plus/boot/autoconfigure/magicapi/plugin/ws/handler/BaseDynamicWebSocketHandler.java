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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public abstract class BaseDynamicWebSocketHandler implements WebSocketHandler , BaseService {

    /**
     * 存储客户端ID与会话列表的映射关系，用于管理同一客户端的多个会话
     * 使用Guava Cache限制大小并支持自动过期
     */
    private final com.google.common.cache.Cache<String, List<WebSocketSession>> clientIdSessionListMap =
            com.google.common.cache.CacheBuilder.newBuilder()
                    .maximumSize(getMaxClientSessions()) // 配置的最大客户端会话数
                    .expireAfterWrite(getSessionCacheExpireMinutes(), java.util.concurrent.TimeUnit.MINUTES) // 配置的过期时间
                    .concurrencyLevel(getCacheConcurrencyLevel()) // 配置的并发级别
                    //.weakKeys() // 使用弱引用键，允许垃圾回收器回收不再使用的键
                    .removalListener(new com.google.common.cache.RemovalListener<String, List<WebSocketSession>>() {
                        @Override
                        public void onRemoval(com.google.common.cache.RemovalNotification<String, List<WebSocketSession>> notification) {
                            log.warn("Client session list removed from cache: {} - {}",
                                    notification.getKey(), notification.getCause());
                            try {
                                List<WebSocketSession> sessions = notification.getValue();
                                if (sessions != null) {
                                    // 遍历并关闭所有被移除的会话
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
        // 添加JVM关闭钩子，确保应用关闭时能清理资源
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
            // 清理所有会话相关映射
            clientIdSessionListMap.invalidateAll();
            log.info("All WebSocket resources have been cleaned up");
        } catch (Exception e) {
            log.error("Error during cleanup of all resources", e);
        }
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
                String[] keyValue = pair.split("=");
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
		// 查找符合条件的订阅信息列表（启用状态、匹配的应用ID和业务ID）
		List<WsSubscribeInfo> subscribeInfoList = ListUtils.emptyIfNull(magicSubscribeStorage.findAll()).stream()
				.filter(subscribeInfo -> Boolean.TRUE.equals(subscribeInfo.isEnabled()) && subscribeInfo.getKey().equals(appId)
						&& businessType.equals(subscribeInfo.getBusinessId())).collect(Collectors.toList());
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
     * 根据应用ID清理相关的WebSocket会话连接
     * <p>
     * 该方法会遍历所有客户端的会话列表，找到与指定应用ID匹配的会话并进行清理操作。
     * 为了避免在遍历过程中出现并发修改异常，首先创建了会话映射表的副本。
     *
     * @param appId 应用ID，用于筛选需要清理的会话
     */
    public void cleanupByAppId(String appId) {
        // 创建副本避免并发修改异常
        Map<String, List<WebSocketSession>> snapshot = clientIdSessionListMap.asMap();
        Map<String,String> clientSessionIdMap = new HashMap<>();
        // 遍历所有客户端的会话列表，查找并清理匹配应用ID的会话
        for (Map.Entry<String, List<WebSocketSession>> e : snapshot.entrySet()) {
            for (WebSocketSession session : e.getValue()) {
                try {
                    Object sessionInfoObj = session.getAttributes().get("sessionInfo");
                    if (null != sessionInfoObj) {
                        if (sessionInfoObj instanceof SessionInfo) {
                            SessionInfo sessionInfo = (SessionInfo) sessionInfoObj;
                            if (sessionInfo != null && appId.equals(sessionInfo.getAppId())) {
                                //cleanSession(sessionInfo.getClientId(), session.getId());
                                clientSessionIdMap.put(sessionInfo.getClientId(), session.getId());
                            }
                        }
                    }
                } catch (Exception ex) {
                    // 处理其他可能的异常
                    log.error("Error processing session: {}", session.getId(), ex);
                }
            }
        }
        if (clientSessionIdMap.size() > 0) {
            for (Map.Entry<String, String> entry : clientSessionIdMap.entrySet()) {
                String clientId = entry.getKey();
                String sessionId = entry.getValue();
                cleanSession(clientId, sessionId);
            }
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
            // 获取客户端IP地址
            String clientIp = session.getRemoteAddress().getAddress().getHostAddress();
            
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
                // 根据业务类型处理消息
                handleBusinessMessage(session, businessType, com.alibaba.fastjson2.JSON.parseObject(payload));
            } else {
                sendMessage(session, payload);
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
                List<WsSubscribeInfo> subscribeInfoList = this.magicSubscribeStorage.findAll().stream()
                        .filter(subscribeInfo -> Boolean.TRUE.equals(subscribeInfo.isEnabled())
                                && subscribeInfo.getBusinessId().equals(businessType) && subscribeInfo.getKey().equals(appId))
                        .collect(Collectors.toList());
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
            
            // 通过重新放入缓存项来续期
            String clientId = sessionInfo != null ? sessionInfo.getClientId() : null;
            if (clientId != null) {
                List<WebSocketSession> cachedSessionList = clientIdSessionListMap.getIfPresent(clientId);
                if (cachedSessionList != null) {
                    // 更新session列表中的特定session信息
                    boolean sessionFound = false;
                    // 更新session列表中的session信息
                    for (int i = 0; i < cachedSessionList.size(); i++) {
                        WebSocketSession cachedSession = cachedSessionList.get(i);
                        if (cachedSession.getId().equals(session.getId())) {
                            // 找到匹配的session并替换
                            cachedSessionList.set(i, session);
                            sessionFound = true;
                            break;
                        }
                    }
                    if (!sessionFound) {
                        // 如果未找到匹配的session，则添加新的session
                        cachedSessionList.add(session);
                    }
                    // 重新放入缓存项以续期
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
        // 查找启用的且与当前应用ID和业务类型匹配的订阅信息
        List<WsSubscribeInfo> subscribeInfoList = ListUtils.emptyIfNull(magicSubscribeStorage.findAll()).stream()
                .filter(subscribeInfo ->Boolean.TRUE.equals(subscribeInfo.isEnabled()) && subscribeInfo.getKey().equals(appId)
                        && businessType.equals(subscribeInfo.getBusinessId())).collect(Collectors.toList());
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
        // 记录WebSocket传输错误日志，包含会话ID和异常堆栈信息
        log.error("WebSocket transport error - Session ID: {}", session.getId(), exception);
        // 延迟关闭WebSocket连接，返回服务器错误状态码
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
        try {
            // 从会话属性中获取会话信息
            SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
            if (sessionInfo == null) {
                log.warn("Session info not found for closed session: {}", sessionId);
                return;
            }
            String appId = sessionInfo.getAppId();
            String clientId = sessionInfo.getClientId();
            log.info("WebSocket connection closed - Session ID: {}, Client ID: {}, App ID: {}, Close status: {}", 
                    sessionId, clientId, appId, closeStatus);
            // 先执行业务结束逻辑
            end(appId,sessionId,clientId);
            // 再清理会话资源
            cleanSession(clientId,sessionId);
        } catch (Exception e) {
            log.error("Error during connection closed handling for session: {}", sessionId, e);
            throw e;
        }
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
        List<WebSocketSession> sessionList = clientIdSessionListMap.asMap().get(clientId);
        if (sessionList != null) {
            // 创建需要移除的会话列表
            List<WebSocketSession> sessionsToRemove = new java.util.ArrayList<>();
            // 遍历并收集需要清理的会话
            for (WebSocketSession session : sessionList) {
                if (StringUtils.isNotBlank(sessionId)) {
                    // 如果指定了sessionId，只收集匹配的会话
                    if (session.getId().equals(sessionId)) {
                        sessionsToRemove.add(session);
                    }
                } else {
                    // 如果未指定sessionId，收集所有会话
                    sessionsToRemove.add(session);
                }
            }

            // 清理收集到的会话
            for (WebSocketSession session : sessionsToRemove) {
                delayedClose(session, "Session closed", CloseStatus.NORMAL, getDelayMillis());
                // 清理会话属性
                session.getAttributes().remove("sessionInfo");
                // 从列表中移除会话
                sessionList.remove(session);
            }

            // 根据清理情况更新缓存
            if(StringUtils.isNotBlank(sessionId) && sessionList.isEmpty()) {
                clientIdSessionListMap.invalidate(clientId);
            } else if (StringUtils.isNotBlank(sessionId) && sessionList.size() > 0) {
                // 如果清理了特定会话，更新缓存
                clientIdSessionListMap.put(clientId, sessionList);
            } else if (StringUtils.isBlank(sessionId)) {
                // 如果清理了所有会话，更新缓存
                clientIdSessionListMap.put(clientId, sessionList);
            }
        } else {
            clientIdSessionListMap.invalidate(clientId);
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

        // 添加会话信息到会话属性中
        SessionInfo sessionInfo = new SessionInfo(appId, clientId);
        session.getAttributes().put("sessionInfo", sessionInfo);

        // 添加客户端ID与会话列表的映射
        try {
            List<WebSocketSession> sessions = clientIdSessionListMap.get(clientId,
                () -> new java.util.concurrent.CopyOnWriteArrayList<>());
            sessions.add(session);
        } catch (java.util.concurrent.ExecutionException e) {
            log.error("Error adding session for client: {}", clientId, e);
            // Fallback: 直接操作
            List<WebSocketSession> sessions = clientIdSessionListMap.getIfPresent(clientId);
            if (sessions == null) {
                sessions = new java.util.concurrent.CopyOnWriteArrayList<>();
                clientIdSessionListMap.put(clientId, sessions);
            }
            sessions.add(session);
        }

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
    private void sendMessage(WebSocketSession session, String message) throws Exception {
        // 检查会话是否仍然有效
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
        
        // 检查会话是否超时（即使在缓存中）
        long currentTime = System.currentTimeMillis();
        long lastPingTime = sessionInfo.getLastPingTime();
        long timeoutMillis = getSessionCacheExpireMinutes() * 60 * 1000;
        return (currentTime - lastPingTime) <= timeoutMillis;
    }

    /**
     * 向指定的WebSocket会话发送JSON格式的消息
     * 
     * @param session WebSocket会话对象
     * @param jsonBean 包含消息内容的JsonBean对象
     * @throws Exception 如果发送消息过程中发生错误
     */
    private void sendMessage(WebSocketSession session, JsonBean jsonBean) throws Exception {
        sendMessage(session, JSON.toJSONString(jsonBean));
    }

    /**
     * 向指定客户端的所有会话发送消息，仅发送给与指定appId匹配的会话
     * 
     * @param appId 应用ID，用于过滤会话
     * @param clientId 客户端ID
     * @param message 要发送的消息内容
     */
    public void broadcastMessageToClient(String appId, String clientId, String message) {
        List<WebSocketSession> sessionList = this.clientIdSessionListMap.getIfPresent(clientId);
        if (!Collections.isEmpty(sessionList)) {
            // 创建需要移除的会话列表
            List<WebSocketSession> sessionsToRemove = new java.util.ArrayList<>();
            // 创建需要发送消息的会话列表
            List<WebSocketSession> sessionsToSend = new java.util.ArrayList<>();
            // 筛选出与指定appId匹配的会话
            for (WebSocketSession session : sessionList) {
                SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
                if (sessionInfo != null && appId.equals(sessionInfo.getAppId())) {
                    if (isSessionValid(session)) {
                        sessionsToSend.add(session);
                    } else {
                        // 如果会话无效，记录需要移除的会话
                        sessionsToRemove.add(session);
                    }
                }
            }
            // 向筛选出的会话发送消息
            for (WebSocketSession session : sessionsToSend) {
                try {
                    sendMessage(session, message);
                } catch (Exception e) {
                    log.error("Failed to send message to session: {}", session.getId(), e);
                    // 如果发送失败，记录需要移除的会话
                    sessionsToRemove.add(session);
                }
            }

            // 移除无效或发送失败的会话
            for (WebSocketSession session : sessionsToRemove) {
                sessionList.remove(session);
                // 清理会话属性
                session.getAttributes().remove("sessionInfo");
            }

            // 如果该clientId没有有效会话了，则从map中移除
            if (sessionList.isEmpty()) {
                this.clientIdSessionListMap.invalidate(clientId);
            }
        } else {
            log.warn("No active sessions found for client: {}", clientId);
            broadcastMessageToClient(this.webSocketServerId,clientId, appId, message);
        }
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
            // 创建延迟任务来关闭连接
            java.util.concurrent.ScheduledExecutorService delayedCloser = 
                java.util.concurrent.Executors.newSingleThreadScheduledExecutor();
            delayedCloser.schedule(() -> {
                try {
                    if (session.isOpen()) {
                        session.close(status.withReason(message));
                    }
                } catch (Exception e) {
                    log.error("Error closing session: {}", session.getId(), e);
                } finally {
                    // 清理资源
                    delayedCloser.shutdown();
                }
            }, delayMillis, java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            log.error("Error during delayed close operation for session: {}", session.getId(), e);
            // 如果延迟关闭失败，立即关闭连接
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
    protected void broadcastMessageToAllClient(String appId, String message) {
        // 遍历所有客户端
        for (java.util.Map.Entry<String, List<WebSocketSession>> entry : clientIdSessionListMap.asMap().entrySet()) {
            for (WebSocketSession session : entry.getValue()) {
                try {
                    SessionInfo sessionInfo = (SessionInfo) session.getAttributes().get("sessionInfo");
                    if (!isSessionValid(session) || sessionInfo == null || !appId.equals(sessionInfo.getAppId())) {
                        log.warn("Attempted to send message to invalid session: {}", session.getId());
                        continue;
                    }
                    sendMessage(session, message);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
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
