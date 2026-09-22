package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi;

import com.alibaba.fastjson2.JSONObject;
import org.springframework.web.socket.WebSocketSession;

/**
 * 一次订阅脚本调用的上下文。
 *
 * <p>由 WebSocket 地基构造并交给 {@link WsBusinessDispatcher}，业务侧据此组装自己的脚本上下文，
 * 并可通过 {@link #getReplyHandle()} 把执行结果回发当前会话。</p>
 *
 * @author zzy
 */
public final class WsInvocationContext {

    /**
     * 调用阶段。
     */
    public enum Phase {
        /**
         * 连接建立后的初始化脚本
         */
        INIT,
        /**
         * 业务消息触发的脚本
         */
        MESSAGE,
        /**
         * 连接关闭时的收尾脚本
         */
        CLOSE
    }

    private final Phase phase;

    private final String appId;

    private final String clientId;

    private final String sessionId;

    private final String businessType;

    private final JSONObject params;

    private final WebSocketSession session;

    private final WsReplyHandle replyHandle;

    public WsInvocationContext(Phase phase, String appId, String clientId, String sessionId,
                               String businessType, JSONObject params, WebSocketSession session,
                               WsReplyHandle replyHandle) {
        this.phase = phase;
        this.appId = appId;
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.businessType = businessType;
        this.params = params;
        this.session = session;
        this.replyHandle = replyHandle;
    }

    public Phase getPhase() {
        return phase;
    }

    public String getAppId() {
        return appId;
    }

    public String getClientId() {
        return clientId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBusinessType() {
        return businessType;
    }

    public JSONObject getParams() {
        return params;
    }

    public WebSocketSession getSession() {
        return session;
    }

    /**
     * 结果回发句柄；地基未持有会话时为 {@code null}。
     *
     * @return 回发句柄
     */
    public WsReplyHandle getReplyHandle() {
        return replyHandle;
    }
}
