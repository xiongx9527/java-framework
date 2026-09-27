package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.support;

import org.springframework.http.HttpHeaders;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.InetSocketAddress;
import java.net.URI;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 测试用 WebSocket 会话桩：记录发送的文本消息与关闭状态，并可自定义握手 URI。
 *
 * <p>握手参数校验与 Redis 广播的验收测试需要真实走一遍 {@code afterConnectionEstablished}，
 * 因此这里提供一个可跨包复用的最小实现。</p>
 *
 * @author zzy
 */
public class TestWebSocketSession implements WebSocketSession {

    private final String id;

    private final Map<String, Object> attributes = new HashMap<>();

    private final List<String> textMessages = new ArrayList<>();

    private URI uri = URI.create("ws://localhost/ws/api");

    private boolean open = true;

    private CloseStatus closeStatus;

    public TestWebSocketSession(String id) {
        this.id = id;
    }

    /**
     * 设置握手 URI（可携带 clientId 等查询参数）。
     *
     * @param uri 握手 URI
     * @return 当前会话，便于链式调用
     */
    public TestWebSocketSession handshakeUri(String uri) {
        this.uri = URI.create(uri);
        return this;
    }

    /**
     * 获取最后一次关闭状态，未关闭时为 null。
     *
     * @return 关闭状态
     */
    public CloseStatus closeStatus() {
        return closeStatus;
    }

    /**
     * 获取已发送的文本消息。
     *
     * @return 文本消息列表
     */
    public List<String> textMessages() {
        return textMessages;
    }

    /**
     * 获取会话属性映射。
     *
     * @return 会话属性
     */
    public Map<String, Object> attributesMap() {
        return attributes;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public HttpHeaders getHandshakeHeaders() {
        return new HttpHeaders();
    }

    @Override
    public URI getUri() {
        return uri;
    }

    @Override
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public String getAcceptedProtocol() {
        return null;
    }

    @Override
    public void setTextMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getTextMessageSizeLimit() {
        return 8192;
    }

    @Override
    public void setBinaryMessageSizeLimit(int messageSizeLimit) {
    }

    @Override
    public int getBinaryMessageSizeLimit() {
        return 8192;
    }

    @Override
    public List<WebSocketExtension> getExtensions() {
        return Collections.emptyList();
    }

    @Override
    public void sendMessage(WebSocketMessage<?> message) {
        if (message instanceof TextMessage) {
            textMessages.add(((TextMessage) message).getPayload());
        }
    }

    @Override
    public boolean isOpen() {
        return open;
    }

    @Override
    public void close() {
        close(CloseStatus.NORMAL);
    }

    @Override
    public void close(CloseStatus status) {
        this.closeStatus = status;
        this.open = false;
    }
}
