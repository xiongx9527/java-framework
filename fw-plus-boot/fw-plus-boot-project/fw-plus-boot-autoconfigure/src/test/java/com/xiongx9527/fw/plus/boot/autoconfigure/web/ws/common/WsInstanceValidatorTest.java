package com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.common;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.ws.config.WsConfig.WsInstance;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * 命名实例启动期校验：与 MCP {@code McpRuntimeFactory#validateKeys} / {@code validateEndpoints}
 * 同一套口径，「配了却不生效」的静默误导按配置错误处理。
 */
class WsInstanceValidatorTest {

    @Test
    void absentServersShouldPass() {
        WsInstanceValidator.validate(null);
        WsInstanceValidator.validate(new WsConfig());

        WsConfig config = new WsConfig();
        config.setServers(null);
        WsInstanceValidator.validate(config);

        WsConfig empty = new WsConfig();
        empty.setServers(Collections.<WsInstance>emptyList());
        WsInstanceValidator.validate(empty);
    }

    @Test
    void validServersShouldPass() {
        WsConfig config = new WsConfig();
        config.setServers(Arrays.asList(
                server("report", "/ws/api2"),
                server("trade", "/ws/api3", "/ws/api4")));

        WsInstanceValidator.validate(config);
    }

    @Test
    void blankKeyShouldFail() {
        WsConfig missing = new WsConfig();
        missing.setServers(Arrays.asList(server(null, "/ws/api2")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(missing))
                .withMessageContaining("fw.plus.ws.servers[].key must not be blank")
                .withMessageContaining("fw.plus.ws.servers[0].key");

        WsConfig blank = new WsConfig();
        blank.setServers(Arrays.asList(server("   ", "/ws/api2")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(blank))
                .withMessageContaining("fw.plus.ws.servers[0].key");
    }

    @Test
    void duplicateKeyShouldFail() {
        WsConfig config = new WsConfig();
        config.setServers(Arrays.asList(
                server("report", "/ws/api2"),
                server("report", "/ws/api3")));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(config))
                .withMessageContaining("'report'")
                .withMessageContaining("fw.plus.ws.servers[1].key")
                .withMessageContaining("fw.plus.ws.servers[0].key");
    }

    @Test
    void endpointConflictingWithDefaultInstanceShouldFail() {
        WsConfig config = new WsConfig();
        config.setServers(Arrays.asList(server("report", "/ws/api")));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(config))
                .withMessageContaining("server key 'report'")
                .withMessageContaining("'/ws/api'")
                .withMessageContaining("the default instance");
    }

    @Test
    void endpointConflictingWithDefaultInstanceByTrailingSlashOrPathsShouldFail() {
        WsConfig trailingSlash = new WsConfig();
        trailingSlash.setServers(Arrays.asList(server("report", "/ws/api/")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(trailingSlash));

        WsConfig additionalPaths = new WsConfig();
        additionalPaths.setPaths(Arrays.asList("/ws/api2", "/ws/api3"));
        additionalPaths.setServers(Arrays.asList(server("report", null, "/ws/api3")));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(additionalPaths))
                .withMessageContaining("'/ws/api3'")
                .withMessageContaining("the default instance");
    }

    @Test
    void endpointConflictingWithAnotherServerShouldFail() {
        WsConfig config = new WsConfig();
        config.setServers(Arrays.asList(
                server("report", "/ws/api2"),
                server("trade", "/ws/api2")));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(config))
                .withMessageContaining("server key 'trade'")
                .withMessageContaining("'/ws/api2'")
                .withMessageContaining("server key 'report'");
    }

    @Test
    void serverWithoutEndpointShouldFail() {
        WsConfig config = new WsConfig();
        config.setServers(Arrays.asList(server("report", null)));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(config))
                .withMessageContaining("server key 'report'")
                .withMessageContaining("fw.plus.ws.servers[0].path")
                .withMessageContaining("fw.plus.ws.servers[0].paths");
    }

    @Test
    void repeatedEndpointsInsideOneServerShouldPass() {
        WsConfig config = new WsConfig();
        config.setServers(Arrays.asList(server("report", "/ws/api2", "/ws/api2", " ws/api2 ")));

        WsInstanceValidator.validate(config);
    }

    @Test
    void nullServerEntryShouldFail() {
        WsConfig config = new WsConfig();
        config.setServers(Arrays.asList((WsInstance) null));

        assertThatIllegalArgumentException()
                .isThrownBy(() -> WsInstanceValidator.validate(config))
                .withMessageContaining("fw.plus.ws.servers[0] must not be null");
    }

    private static WsInstance server(String key, String path, String... additionalPaths) {
        WsInstance server = new WsInstance();
        server.setKey(key);
        server.setPath(path);
        server.setPaths(new java.util.ArrayList<>(Arrays.asList(additionalPaths)));
        return server;
    }
}
