package com.xiongx9527.fw.plus.boot.ws.sample;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsBusinessDispatcher;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsReplyHandle;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * 演示用订阅调度：声明 {@code echo} 与 {@code fw.plus.ws.binary.type} 两个订阅，
 * 把收到的文本参数或二进制内容原样回发。
 *
 * <p>业务订阅通常由 magic-api 的 ws 插件注入；这里用最小实现说明地基的 SPI：
 * {@link #findEnabledSubscribes} 决定某个业务类型下有哪些订阅生效，
 * {@link #execute} 执行订阅并通过 {@link WsReplyHandle} 回发结果。</p>
 *
 * @author zzy
 */
@Component
public class DemoWsDispatcher implements WsBusinessDispatcher {

	/**
	 * 文本订阅的业务类型，消息体形如 {@code {"type":"echo","id":1}}。
	 */
	public static final String ECHO_TYPE = "echo";

	/**
	 * 二进制帧的业务类型，取自 {@code fw.plus.ws.binary.type}（默认 {@code binary}）：
	 * 地基把二进制帧按该业务类型送进订阅派发链路。
	 */
	private final String binaryBusinessType;

	public DemoWsDispatcher(WsConfig wsConfig) {
		this.binaryBusinessType = wsConfig.getBinary().getType();
	}

	@Override
	public List<? extends WsSubscribeScript> findEnabledSubscribes(String appId, String businessType) {
		if (!ECHO_TYPE.equals(businessType) && !binaryBusinessType.equals(businessType)) {
			return Collections.emptyList();
		}
		return Collections.singletonList(new WsSubscribeScript() {
			@Override
			public String getName() {
				return businessType;
			}

			@Override
			public String getBusinessId() {
				return businessType;
			}
		});
	}

	@Override
	public Object execute(WsSubscribeScript subscribe, WsInvocationContext context) {
		WsReplyHandle replyHandle = context.getReplyHandle();
		if (replyHandle == null) {
			return null;
		}
		if (context.getMessageKind() == WsInvocationContext.MessageKind.BINARY) {
			replyHandle.sendBinary(context.getBinaryBody());
			return context.getBusinessType() + "-echo:" + context.getBinaryBody().length + " bytes";
		} else {
			replyHandle.sendJson(context.getParams());
			return context.getBusinessType() + "-echo:" + context.getParams();
		}
	}
}
