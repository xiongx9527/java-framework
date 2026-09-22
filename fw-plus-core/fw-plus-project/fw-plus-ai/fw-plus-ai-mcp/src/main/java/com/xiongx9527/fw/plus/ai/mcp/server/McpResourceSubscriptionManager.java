package com.xiongx9527.fw.plus.ai.mcp.server;

import com.xiongx9527.fw.plus.ai.mcp.spi.McpResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 资源订阅管理器
 *
 * <p>管理客户端对特定资源 URI 的订阅，当资源变更时通过 SSE 发送通知。
 *
 * @author xiongxiao
 * @date 2025-09-21
 */
public class McpResourceSubscriptionManager {
    private static final Logger logger = LoggerFactory.getLogger(McpResourceSubscriptionManager.class);

    private final Map<String, Set<String>> uriSubscriptions = new ConcurrentHashMap<>();
    private final Map<String, McpResourceProvider> resourceProviders = new ConcurrentHashMap<>();

    /**
     * 订阅资源 URI
     */
    public void subscribe(String sessionId, String uri) {
        uriSubscriptions.computeIfAbsent(uri, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        logger.debug("Session {} subscribed to resource: {}", sessionId, uri);
    }

    /**
     * 取消订阅资源 URI
     */
    public void unsubscribe(String sessionId, String uri) {
        Set<String> sessions = uriSubscriptions.get(uri);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                uriSubscriptions.remove(uri);
            }
        }
        logger.debug("Session {} unsubscribed from resource: {}", sessionId, uri);
    }

    /**
     * 取消订阅该 Session 的所有资源
     */
    public void unsubscribeAll(String sessionId) {
        uriSubscriptions.values().forEach(sessions -> sessions.remove(sessionId));
        uriSubscriptions.values().removeIf(sessions -> sessions.isEmpty());
        logger.debug("Session {} unsubscribed from all resources", sessionId);
    }

    /**
     * 通知订阅者资源已变更
     */
    public void notifyResourceChanged(String uri, Object transportProvider) {
        Set<String> sessions = uriSubscriptions.get(uri);
        if (sessions != null && !sessions.isEmpty()) {
            logger.info("Notifying {} sessions about resource change: {}", sessions.size(), uri);
            // 实际实现中需要通过 transportProvider 发送 SSE 通知
        }
    }

    /**
     * 检查 URI 是否有订阅者
     */
    public boolean hasSubscribers(String uri) {
        Set<String> sessions = uriSubscriptions.get(uri);
        return sessions != null && !sessions.isEmpty();
    }

    /**
     * 获取指定 URI 的所有订阅会话
     *
     * @return 订阅会话集合，可能为空
     */
    public Set<String> getSubscribers(String uri) {
        Set<String> sessions = uriSubscriptions.get(uri);
        if (sessions == null) {
            return java.util.Collections.emptySet();
        }
        return new java.util.HashSet<>(sessions);
    }
}
