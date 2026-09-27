package com.xiongx9527.fw.plus.boot.ws.sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * WebSocket 地基样例应用。
 *
 * <p>启动后即具备 {@code fw.plus.ws.path}（默认 {@code /ws/api}）上的 WebSocket 端点：
 * 握手用 query 参数 {@code clientId}；连接建立后可发送 {@code {"type":"echo",...}} 或二进制帧，
 * 由 {@link DemoWsDispatcher} 原样回发。</p>
 *
 * @author zzy
 */
@SpringBootApplication
public class WsSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(WsSampleApplication.class, args);
	}
}
