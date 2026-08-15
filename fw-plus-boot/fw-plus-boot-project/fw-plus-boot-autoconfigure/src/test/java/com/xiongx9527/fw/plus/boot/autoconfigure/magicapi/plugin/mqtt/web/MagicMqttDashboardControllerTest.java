package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.web;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardClientInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardPage;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardServerInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardStats;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardSubscriptionRequest;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.service.MqttDashboardService;
import net.dreamlu.iot.mqtt.core.server.model.Subscribe;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.model.JsonBean;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MagicMqttDashboardControllerTest {

    @Test
    void exposesDashboardManagementContract() throws Exception {
        assertMapping("servers", GetMapping.class, "/mqtt/dashboard/servers");
        assertMapping("stats", GetMapping.class, "/mqtt/dashboard/{serverId}/stats", String.class);
        assertMapping("clients", GetMapping.class, "/mqtt/dashboard/{serverId}/clients", String.class, Integer.class, Integer.class);
        assertMapping("clientInfo", GetMapping.class, "/mqtt/dashboard/{serverId}/clients/info", String.class, String.class);
        assertMapping("subscriptions", GetMapping.class, "/mqtt/dashboard/{serverId}/client/subscriptions", String.class, String.class);
        assertMapping("deleteClient", PostMapping.class, "/mqtt/dashboard/{serverId}/clients/delete", String.class, String.class);
        assertMapping("subscribe", PostMapping.class, "/mqtt/dashboard/{serverId}/mqtt/subscribe", String.class, MqttDashboardSubscriptionRequest.class);
        assertMapping("unsubscribe", PostMapping.class, "/mqtt/dashboard/{serverId}/mqtt/unsubscribe", String.class, MqttDashboardSubscriptionRequest.class);
    }

    @Test
    void exposesAllUpstreamClientDetailFields() throws Exception {
        assertThat(MqttDashboardClientInfo.class.getMethod("getDecodeQueueSize")).isNotNull();
        assertThat(MqttDashboardClientInfo.class.getMethod("getHandlerQueueSize")).isNotNull();
        assertThat(MqttDashboardClientInfo.class.getMethod("getSendQueueSize")).isNotNull();
    }

    @Test
    void delegatesEveryDashboardOperationToTheInProcessService() {
        MqttDashboardService service = mock(MqttDashboardService.class);
        List<MqttDashboardServerInfo> servers = Collections.singletonList(new MqttDashboardServerInfo());
        MqttDashboardStats stats = new MqttDashboardStats();
        MqttDashboardPage<MqttDashboardClientInfo> clients = new MqttDashboardPage<MqttDashboardClientInfo>();
        MqttDashboardClientInfo client = new MqttDashboardClientInfo();
        List<Subscribe> subscriptions = Collections.emptyList();
        MqttDashboardSubscriptionRequest request = new MqttDashboardSubscriptionRequest();
        request.setClientId("client-1");
        request.setTopic("devices/1");
        request.setQos(1);

        when(service.listServers()).thenReturn(servers);
        when(service.stats("default")).thenReturn(stats);
        when(service.clients("default", 1, 20)).thenReturn(clients);
        when(service.clientInfo("default", "client-1")).thenReturn(client);
        when(service.subscriptions("default", "client-1")).thenReturn(subscriptions);

        MagicMqttController controller = new MagicMqttController(mock(MagicConfiguration.class), service);

        assertThat(controller.servers().getData()).isSameAs(servers);
        assertThat(controller.stats("default").getData()).isSameAs(stats);
        assertThat(controller.clients("default", 1, 20).getData()).isSameAs(clients);
        assertThat(controller.clientInfo("default", "client-1").getData()).isSameAs(client);
        assertThat(controller.subscriptions("default", "client-1").getData()).isSameAs(subscriptions);
        controller.deleteClient("default", "client-1");
        controller.subscribe("default", request);
        controller.unsubscribe("default", request);

        verify(service).deleteClient("default", "client-1");
        verify(service).subscribe("default", request);
        verify(service).unsubscribe("default", request);
    }

    @Test
    void reportsInvalidDashboardTargetsAsClientErrors() {
        MagicMqttController controller = new MagicMqttController(mock(MagicConfiguration.class),
                mock(MqttDashboardService.class));

        Object result = controller.exceptionHandler(new IllegalArgumentException("MQTT 服务端不存在: missing"));

        assertThat(result).isInstanceOf(JsonBean.class);
        assertThat(((JsonBean<?>) result).getCode()).isEqualTo(400);
        assertThat(((JsonBean<?>) result).getMessage()).contains("missing");
    }

    private void assertMapping(String methodName, Class<?> annotationType, String path, Class<?>... parameterTypes) throws Exception {
        Method method = MagicMqttController.class.getMethod(methodName, parameterTypes);
        if (annotationType == GetMapping.class) {
            assertThat(method.getAnnotation(GetMapping.class).value()).containsExactly(path);
        } else {
            assertThat(method.getAnnotation(PostMapping.class).value()).containsExactly(path);
        }
    }
}
