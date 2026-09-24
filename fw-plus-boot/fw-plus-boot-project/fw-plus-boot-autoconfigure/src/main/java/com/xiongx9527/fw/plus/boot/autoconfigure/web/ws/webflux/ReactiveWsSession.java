package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webflux;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSession;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.URI;
import java.util.Map;

/**
 * WebFlux（响应式）栈的 {@link WsSession} 适配器。
 *
 * <p>响应式发送是非阻塞的：{@code send} 返回 {@code Mono}，这里就地订阅完成投递，
 * 与 Servlet 栈 {@code sendMessage} 的同步语义对齐，便于地基复用同一套派发逻辑。
 * 调用方始终在脚本线程池而非 Reactor event loop 上，就地订阅不会阻塞事件循环。</p>
 *
 * @author zzy
 */
public class ReactiveWsSession implements WsSession {

	private final WebSocketSession delegate;

	public ReactiveWsSession(WebSocketSession delegate) {
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
		return delegate.getHandshakeInfo().getUri();
	}

	@Override
	public void sendText(String message) throws IOException {
		delegate.send(Mono.just(delegate.textMessage(message))).subscribe();
	}

	@Override
	public void sendBinary(byte[] payload) throws IOException {
		WebSocketMessage message = delegate.binaryMessage(factory -> factory.wrap(payload));
		delegate.send(Mono.just(message)).subscribe();
	}

	@Override
	public void close(org.springframework.web.socket.CloseStatus status) throws IOException {
		delegate.close(toReactiveStatus(status)).subscribe();
	}

	/**
	 * 把 Servlet 栈的关闭状态转换为响应式栈的关闭状态。
	 *
	 * @param status Servlet 栈关闭状态
	 * @return 响应式栈关闭状态
	 */
	private static CloseStatus toReactiveStatus(org.springframework.web.socket.CloseStatus status) {
		if (status == null) {
			return CloseStatus.NORMAL;
		}
		return new CloseStatus(status.getCode(), status.getReason());
	}

	@Override
	public <T> T unwrap(Class<T> type) {
		return type.isInstance(delegate) ? type.cast(delegate) : null;
	}
}
