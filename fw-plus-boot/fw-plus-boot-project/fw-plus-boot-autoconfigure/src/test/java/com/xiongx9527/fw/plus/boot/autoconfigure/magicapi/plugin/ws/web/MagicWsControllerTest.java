package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler.BaseDynamicWebSocketHandler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;
import org.ssssssss.script.MagicScriptContext;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicWsControllerTest {

	@Test
	void executeShouldReturnFailureWhenSubscribeScriptNotFound() {
		WsSubscribeInfoMagicResourceStorage subscribeStorage = mock(WsSubscribeInfoMagicResourceStorage.class);
		when(subscribeStorage.findAll()).thenReturn(Collections.emptyList());
		MagicWsController controller = controller(subscribeStorage, null, null);

		JsonBean<Object> result = controller.execute("missing", null);

		assertThat(result.getCode()).isEqualTo(-1);
		assertThat(result.getMessage()).contains("missing");
	}

	@Test
	void executeShouldRunSubscribeScriptWithSimulatedContext() {
		WsSubscribeInfo subscribeInfo = subscribeInfo("script-1", "default", "order-refresh", "return type + ':' + businessType;");
		WsSubscribeInfoMagicResourceStorage subscribeStorage = mock(WsSubscribeInfoMagicResourceStorage.class);
		when(subscribeStorage.findAll()).thenReturn(Collections.singletonList(subscribeInfo));
		MagicWsController controller = controller(subscribeStorage, null, null);

		JsonBean<Object> result = controller.execute("script-1", null);

		assertThat(result.getCode()).isEqualTo(1);
		assertThat(result.getData()).isEqualTo("order-refresh:order-refresh");
	}

	@Test
	void buildTestContextShouldExposeCompatibleVariables() {
		WsSubscribeInfo subscribeInfo = subscribeInfo("script-1", "default", "order-refresh", "return null;");
		MagicWsController controller = controller(null, null, null);

		MagicScriptContext context = controller.buildTestContext(subscribeInfo, null);

		assertThat(context.get("appId")).isEqualTo("default");
		assertThat(context.get("clientId")).isEqualTo("test-client");
		assertThat(context.get("sessionId")).isEqualTo("test-session");
		assertThat(context.get("type")).isEqualTo("order-refresh");
		assertThat(context.get("businessType")).isEqualTo("order-refresh");
		assertThat(context.get("params")).isInstanceOf(Map.class);
	}

	@Test
	void datasourceTestShouldRejectMissingKey() {
		JsonBean<Object> result = controller(null, null, null).test(new WsDataSourceInfo());

		assertThat(result.getCode()).isEqualTo(-1);
		assertThat(result.getMessage()).contains("key");
	}

	@Test
	void datasourceTestShouldRejectInvalidOtherJson() {
		WsDataSourceInfo dataSourceInfo = dataSourceInfo();
		dataSourceInfo.setOther("not-json");

		JsonBean<Object> result = controller(null, null, null).test(dataSourceInfo);

		assertThat(result.getCode()).isEqualTo(-1);
		assertThat(result.getMessage()).contains("other");
	}

	@Test
	void datasourceTestShouldReturnOkWhenTokenSecretIsConfigured() {
		MagicWsController controller = new MagicWsController(new MagicConfiguration(), null, null, null) {
			@Override
			public Map<String, Object> getConfig() {
				Map<String, Object> config = new HashMap<>();
				config.put("token.secret", "01234567890123456789012345678901");
				return config;
			}
		};

		JsonBean<Object> result = controller.test(dataSourceInfo());

		assertThat(result.getCode()).isEqualTo(1);
		assertThat(result.getData()).isEqualTo("ok");
	}

	private MagicWsController controller(WsSubscribeInfoMagicResourceStorage subscribeStorage,
	                                    WsDataSourceInfoMagicResourceStorage dataSourceStorage,
	                                    BaseDynamicWebSocketHandler handler) {
		return new MagicWsController(new MagicConfiguration(), subscribeStorage, dataSourceStorage, handler) {
			@Override
			public Map<String, Object> getConfig() {
				Map<String, Object> config = new HashMap<>();
				config.put("token.secret", "01234567890123456789012345678901");
				return config;
			}
		};
	}

	private WsSubscribeInfo subscribeInfo(String id, String key, String businessId, String script) {
		WsSubscribeInfo subscribeInfo = new WsSubscribeInfo();
		subscribeInfo.setId(id);
		subscribeInfo.setName("测试脚本");
		subscribeInfo.setKey(key);
		subscribeInfo.setBusinessId(businessId);
		subscribeInfo.setScript(script);
		subscribeInfo.setEnabled(true);
		return subscribeInfo;
	}

	private WsDataSourceInfo dataSourceInfo() {
		WsDataSourceInfo dataSourceInfo = new WsDataSourceInfo();
		dataSourceInfo.setName("默认数据源");
		dataSourceInfo.setKey("default");
		dataSourceInfo.setSecret("secret");
		dataSourceInfo.setOther("{}");
		return dataSourceInfo;
	}
}
