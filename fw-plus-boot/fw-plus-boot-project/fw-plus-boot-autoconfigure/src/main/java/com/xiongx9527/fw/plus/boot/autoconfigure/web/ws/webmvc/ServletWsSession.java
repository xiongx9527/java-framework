package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSession;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * Servlet（webmvc）栈的 {@link WsSession} 适配器。
 *
 * @author zzy
 */
public class ServletWsSession implements WsSession {

	private final WebSocketSession delegate;

	public ServletWsSession(WebSocketSession delegate) {
		this.delegate = delegate;
	}

	@Override
	public String getId() {
		return delegate.getId();
	}

	@Override
	public boolean isOpen() {
		return delegate.isOpen();
	}

	@Override
	public Map<String, Object> getAttributes() {
		return delegate.getAttributes();
	}

	@Override
	public URI getUri() {
		return delegate.getUri();
	}

	@Override
	public void sendText(String message) throws IOException {
		delegate.sendMessage(new TextMessage(message));
	}

	@Override
	public void sendBinary(byte[] payload) throws IOException {
		delegate.sendMessage(new BinaryMessage(payload));
	}

	@Override
	public void close(CloseStatus status) throws IOException {
		delegate.close(status);
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		return type.isInstance(delegate) ? type.cast(delegate) : null;
	}
}
