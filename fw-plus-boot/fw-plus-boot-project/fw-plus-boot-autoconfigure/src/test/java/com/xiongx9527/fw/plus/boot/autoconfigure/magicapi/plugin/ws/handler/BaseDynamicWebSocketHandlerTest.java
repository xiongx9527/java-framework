package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.handler;

import com.alibaba.fastjson2.JSONObject;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsDataSourceInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.model.WsSubscribeInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsDataSourceInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.ws.service.WsSubscribeInfoMagicResourceStorage;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsInvocationContext;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.webmvc.ServletWsSession;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.spi.WsSubscribeScript;
import org.junit.jupiter.api.Test;
import org.springframework.web.socket.WebSocketSession;
import org.ssssssss.script.MagicScriptContext;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * magic 侧业务调度（SPI 实现）的单元测试：订阅来自 magic 资源存储，执行走 magic-script。
 *
 * @author zzy
 */
class BaseDynamicWebSocketHandlerTest {

    @Test
    void putBusinessTypeShouldExposeTypeAndBusinessType() {
        BaseDynamicWebSocketHandler handler = handler(Collections.emptyList(), Collections.emptyList());
        MagicScriptContext context = new MagicScriptContext();

        handler.putBusinessType(context, "init");

        assertThat(context.get("type")).isEqualTo("init");
        assertThat(context.get("businessType")).isEqualTo("init");
    }

    @Test
    void putBusinessMessageContextShouldKeepParams() {
        BaseDynamicWebSocketHandler handler = handler(Collections.emptyList(), Collections.emptyList());
        MagicScriptContext context = new MagicScriptContext();
        JSONObject params = new JSONObject();
        params.put("type", "order-refresh");

        handler.putBusinessMessageContext(context, "order-refresh", params);

        assertThat(context.get("type")).isEqualTo("order-refresh");
        assertThat(context.get("businessType")).isEqualTo("order-refresh");
        assertThat(context.get("params")).isSameAs(params);
    }

    @Test
    void existsAppShouldReflectDataSourceStorage() {
        BaseDynamicWebSocketHandler handler = handler(
                Collections.singletonList(dataSourceInfo("default")), Collections.emptyList());

        assertThat(handler.existsApp("default")).isTrue();
        assertThat(handler.existsApp("missing")).isFalse();
    }

    @Test
    void findEnabledSubscribesShouldFilterByEnabledKeyAndBusinessId() {
        WsSubscribeInfo initEnabled = subscribeInfo("init", "default", "init", true, "return null;");
        WsSubscribeInfo initDisabled = subscribeInfo("init", "default", "init", false, "return null;");
        WsSubscribeInfo otherApp = subscribeInfo("init", "other", "init", true, "return null;");
        WsSubscribeInfo otherType = subscribeInfo("refresh", "default", "refresh", true, "return null;");
        BaseDynamicWebSocketHandler handler = handler(Collections.emptyList(),
                Arrays.asList(initEnabled, initDisabled, otherApp, otherType));

        List<? extends WsSubscribeScript> result = handler.findEnabledSubscribes("default", "init");

        assertThat(result).containsExactly(initEnabled);
    }

    @Test
    void executeOnInitShouldExposeSubscribeBusinessId() {
        WsSubscribeInfo subscribeInfo = subscribeInfo("init", "default", "init", true,
                "return type + ':' + businessType + ':' + appId + ':' + clientId + ':' + sessionId;");
        BaseDynamicWebSocketHandler handler = handler(Collections.emptyList(),
                Collections.singletonList(subscribeInfo));
        WsInvocationContext context = new WsInvocationContext(WsInvocationContext.Phase.INIT,
                "default", "client-a", "sess-1", "init", null, null, null);

        Object result = handler.execute(subscribeInfo, context);

        assertThat(result).isEqualTo("init:init:default:client-a:sess-1");
    }

    @Test
    void executeOnMessageShouldExposeParamsAndSubscribeAttribute() {
        WsSubscribeInfo subscribeInfo = subscribeInfo("refresh", "default", "order-refresh", true,
                "return params.keyword + ':' + type;");
        BaseDynamicWebSocketHandler handler = handler(Collections.emptyList(),
                Collections.singletonList(subscribeInfo));
        WebSocketSession session = mock(WebSocketSession.class);
        Map<String, Object> attributes = new HashMap<>();
        when(session.getAttributes()).thenReturn(attributes);
        JSONObject params = new JSONObject();
        params.put("keyword", "订单");
        WsInvocationContext context = new WsInvocationContext(WsInvocationContext.Phase.MESSAGE,
                "default", "client-a", "sess-1", "order-refresh", params, new ServletWsSession(session), null);

        Object result = handler.execute(subscribeInfo, context);

        assertThat(result).isEqualTo("订单:order-refresh");
        assertThat(attributes.get("subscribeInfo")).isSameAs(subscribeInfo);
    }

    @Test
    void executeShouldRejectSubscribeFromForeignImplementation() {
        BaseDynamicWebSocketHandler handler = handler(Collections.emptyList(), Collections.emptyList());
        WsSubscribeScript foreign = new WsSubscribeScript() {
            @Override
            public String getName() {
                return "foreign";
            }

            @Override
            public String getBusinessId() {
                return "foreign";
            }
        };
        WsInvocationContext context = new WsInvocationContext(WsInvocationContext.Phase.MESSAGE,
                "default", "client-a", "sess-1", "foreign", null, null, null);

        assertThatThrownBy(() -> handler.execute(foreign, context))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BaseDynamicWebSocketHandler handler(List<WsDataSourceInfo> dataSources,
                                                       List<WsSubscribeInfo> subscribes) {
        WsDataSourceInfoMagicResourceStorage dataSourceStorage =
                mock(WsDataSourceInfoMagicResourceStorage.class);
        when(dataSourceStorage.findAll()).thenReturn(dataSources);
        WsSubscribeInfoMagicResourceStorage subscribeStorage =
                mock(WsSubscribeInfoMagicResourceStorage.class);
        when(subscribeStorage.findAll()).thenReturn(subscribes);
        return new BaseDynamicWebSocketHandler(dataSourceStorage, subscribeStorage);
    }

    private static WsDataSourceInfo dataSourceInfo(String key) {
        WsDataSourceInfo info = new WsDataSourceInfo();
        info.setKey(key);
        info.setName(key);
        info.setSecret("secret");
        return info;
    }

    private static WsSubscribeInfo subscribeInfo(String id, String key, String businessId,
                                                 boolean enabled, String script) {
        WsSubscribeInfo info = new WsSubscribeInfo();
        info.setId(id);
        info.setName("测试脚本");
        info.setKey(key);
        info.setBusinessId(businessId);
        info.setEnabled(enabled);
        info.setScript(script);
        return info;
    }
}
