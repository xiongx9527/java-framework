package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.BaseMagicDynamicMqttDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MagicAPIMqttServer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDashboardServerInfo;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mqtt.model.MqttDataSourceInfo;
import net.dreamlu.iot.mqtt.core.server.MqttServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MqttDashboardServiceTest {

    private final List<AsynchronousServerSocketChannel> serverChannels = new ArrayList<AsynchronousServerSocketChannel>();

    @AfterEach
    void closeServerChannels() throws IOException {
        for (AsynchronousServerSocketChannel channel : serverChannels) {
            channel.close();
        }
    }

    @Test
    void listsDefaultThenDynamicServersWithoutCredentials() throws Exception {
        BaseMagicDynamicMqttDataSource dynamicDataSource = mock(BaseMagicDynamicMqttDataSource.class);
        MqttServer defaultServer = createServer("default-server", 1883, 8083);
        MqttServer alphaServer = createServer("alpha-server", 2883, 9083);
        MqttDataSourceInfo alpha = dataSource("alpha", "Alpha", "secret-password");
        MqttDataSourceInfo missing = dataSource("missing", "Missing", "other-secret");

        when(dynamicDataSource.datasourceNodes()).thenReturn(Arrays.asList(missing, alpha));
        when(dynamicDataSource.getServer("alpha")).thenReturn(new MagicAPIMqttServer(alphaServer, null, null));

        MqttDashboardService service = new MqttDashboardService(dynamicDataSource, defaultServer);
        List<MqttDashboardServerInfo> servers = service.listServers();

        assertThat(servers).extracting(MqttDashboardServerInfo::getId)
                .containsExactly("default", "datasource:alpha");
        assertThat(servers.get(0).isDefaultServer()).isTrue();
        assertThat(servers.get(1).getName()).isEqualTo("Alpha");
        assertThat(servers.get(1).getKey()).isEqualTo("alpha");
        assertThat(servers.get(1).getPort()).isEqualTo(2883);
        String json = new ObjectMapper().writeValueAsString(servers);
        assertThat(json).doesNotContain("secret-password", "other-secret", "password", "username");
    }

    @Test
    void returnsAnEmptyListWhenNoServerIsRunning() {
        BaseMagicDynamicMqttDataSource dynamicDataSource = mock(BaseMagicDynamicMqttDataSource.class);
        when(dynamicDataSource.datasourceNodes()).thenReturn(Collections.<MqttDataSourceInfo>emptyList());

        assertThat(new MqttDashboardService(dynamicDataSource, null).listServers()).isEmpty();
    }

    @Test
    void excludesConfiguredServersThatHaveNotStarted() {
        MqttServer notStarted = MqttServer.create()
                .name("not-started")
                .ip("127.0.0.1")
                .port(1883)
                .httpEnable(false)
                .websocketEnable(false)
                .build();

        assertThat(new MqttDashboardService(null, notStarted).listServers()).isEmpty();
    }

    @Test
    void rejectsStoppedServerWithoutFallingBack() throws Exception {
        MqttServer stoppedServer = createServer("stopped-server", 1883, 8083);
        stoppedServer.getServerConfig().setStopped(true);
        MqttDashboardService service = new MqttDashboardService(null, stoppedServer);

        assertThat(service.listServers()).isEmpty();
        assertThatThrownBy(() -> service.stats("default"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("未运行");
    }

    @Test
    void keepsDynamicDefaultKeyDistinctFromTheDefaultServer() throws Exception {
        BaseMagicDynamicMqttDataSource dynamicDataSource = mock(BaseMagicDynamicMqttDataSource.class);
        MqttServer defaultServer = createServer("default-server", 1883, 8083);
        MqttServer dynamicServer = createServer("dynamic-default", 2883, 9083);
        MqttDataSourceInfo dynamicInfo = dataSource("default", "Dynamic Default", "secret-password");
        when(dynamicDataSource.datasourceNodes()).thenReturn(Collections.singletonList(dynamicInfo));
        when(dynamicDataSource.getServer("default"))
                .thenReturn(new MagicAPIMqttServer(dynamicServer, null, null));

        MqttDashboardService service = new MqttDashboardService(dynamicDataSource, defaultServer);

        assertThat(service.listServers()).extracting(MqttDashboardServerInfo::getId)
                .containsExactly("default", "datasource:default");
    }

    @Test
    void rejectsUnknownServerWithoutFallingBackToDefault() throws Exception {
        BaseMagicDynamicMqttDataSource dynamicDataSource = mock(BaseMagicDynamicMqttDataSource.class);
        MqttDashboardService service = new MqttDashboardService(dynamicDataSource,
                createServer("default-server", 1883, 8083));

        assertThatThrownBy(() -> service.stats("unknown"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unknown");
    }

    private MqttServer createServer(String name, int port, int webPort) throws IOException {
        MqttServer server = MqttServer.create()
                .name(name)
                .ip("127.0.0.1")
                .port(port)
                .webPort(webPort)
                .httpEnable(false)
                .websocketEnable(false)
                .build();
        AsynchronousServerSocketChannel channel = AsynchronousServerSocketChannel.open();
        ReflectionTestUtils.setField(server.getTioServer(), "serverSocketChannel", channel);
        server.getServerConfig().startTime = System.currentTimeMillis();
        serverChannels.add(channel);
        return server;
    }

    private MqttDataSourceInfo dataSource(String key, String name, String password) {
        MqttDataSourceInfo info = new MqttDataSourceInfo();
        info.setKey(key);
        info.setName(name);
        info.setIp("127.0.0.1");
        info.setPort("2883");
        info.setWebPort("9083");
        info.setUsername("dashboard-user");
        info.setPassword(password);
        return info;
    }
}
