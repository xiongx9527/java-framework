package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import org.junit.jupiter.api.Test;
import org.ssssssss.script.MagicScriptContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class BaseDynamicWebSocketHandlerTest {

	@Test
	void putBusinessTypeShouldExposeTypeAndBusinessType() {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		MagicScriptContext context = new MagicScriptContext();

		handler.putBusinessType(context, "init");

		assertThat(context.get("type")).isEqualTo("init");
		assertThat(context.get("businessType")).isEqualTo("init");
	}

	@Test
	void putBusinessMessageContextShouldKeepParams() {
		TestDynamicWebSocketHandler handler = new TestDynamicWebSocketHandler();
		MagicScriptContext context = new MagicScriptContext();
		JSONObject params = new JSONObject();
		params.put("type", "order-refresh");

		handler.putBusinessMessageContext(context, "order-refresh", params);

		assertThat(context.get("type")).isEqualTo("order-refresh");
		assertThat(context.get("businessType")).isEqualTo("order-refresh");
		assertThat(context.get("params")).isSameAs(params);
	}

	private static class TestDynamicWebSocketHandler extends BaseDynamicWebSocketHandler {

		TestDynamicWebSocketHandler() {
			super(mock(WsDataSourceInfoMagicResourceStorage.class), mock(WsSubscribeInfoMagicResourceStorage.class));
		}

		@Override
		void broadcastMessageToClient(String excludedServerId, String appId, String clientId, String message) {
		}

		@Override
		void broadcastMessageToAllClient(String excludedServerId, String appId, String message) {
		}

		@Override
		public int getMaxClientSessions() {
			return 100;
		}

		@Override
		public int getSessionCacheExpireMinutes() {
			return 30;
		}

		@Override
		public int getCacheConcurrencyLevel() {
			return 4;
		}
	}
}
