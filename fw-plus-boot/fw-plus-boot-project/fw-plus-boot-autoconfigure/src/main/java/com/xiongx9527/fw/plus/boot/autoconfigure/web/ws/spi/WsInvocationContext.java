package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi;

import com.alibaba.fastjson2.JSONObject;

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
     * 入站消息类型。
     */
    public enum MessageKind {

        /**
         * 文本帧，业务参数以 {@link #getParams()} 承载。
         */
        TEXT,

        /**
         * 二进制帧，原始内容以 {@link #getBinaryBody()} 承载。
         */
        BINARY
    }

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

    private final MessageKind messageKind;

    private final String businessType;

    private final JSONObject params;

    private final byte[] binaryBody;

    private final WsSession session;

    private final WsReplyHandle replyHandle;

    /**
     * 文本消息上下文。
     *
     * @param phase        调用阶段
     * @param appId        应用 ID
     * @param clientId     客户端 ID
     * @param sessionId    会话 ID
     * @param businessType 业务类型
     * @param params       文本消息解析出的参数
     * @param session      会话
     * @param replyHandle  结果回发句柄
     */
    public WsInvocationContext(Phase phase, String appId, String clientId, String sessionId,
                               String businessType, JSONObject params, WsSession session,
                               WsReplyHandle replyHandle) {
        this(phase, appId, clientId, sessionId, MessageKind.TEXT, businessType, params, null, session, replyHandle);
    }

    /**
     * 任意消息类型的上下文。
     *
     * @param phase        调用阶段
     * @param appId        应用 ID
     * @param clientId     客户端 ID
     * @param sessionId    会话 ID
     * @param messageKind  消息类型
     * @param businessType 业务类型
     * @param params       文本消息解析出的参数，二进制消息为 null
     * @param binaryBody   二进制消息内容，文本消息为 null
     * @param session      会话
     * @param replyHandle  结果回发句柄
     */
    public WsInvocationContext(Phase phase, String appId, String clientId, String sessionId,
                               MessageKind messageKind, String businessType, JSONObject params,
                               byte[] binaryBody, WsSession session, WsReplyHandle replyHandle) {
        this.phase = phase;
        this.appId = appId;
        this.clientId = clientId;
        this.sessionId = sessionId;
        this.messageKind = messageKind == null ? MessageKind.TEXT : messageKind;
        this.businessType = businessType;
        this.params = params;
        this.binaryBody = binaryBody;
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

    /**
     * 入站消息类型。
     *
     * @return 消息类型
     */
    public MessageKind getMessageKind() {
        return messageKind;
    }

    /**
     * 文本消息解析出的业务参数；二进制消息为 {@code null}。
     *
     * @return 业务参数
     */
    public JSONObject getParams() {
        return params;
    }

    /**
     * 二进制消息内容；文本消息为 {@code null}。
     *
     * @return 二进制内容
     */
    public byte[] getBinaryBody() {
        return binaryBody;
    }

    public WsSession getSession() {
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
