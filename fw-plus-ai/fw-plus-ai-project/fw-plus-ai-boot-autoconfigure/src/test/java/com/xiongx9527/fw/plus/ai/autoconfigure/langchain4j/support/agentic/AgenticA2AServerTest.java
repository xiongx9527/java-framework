package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import com.sun.net.httpserver.HttpServer;
import dev.langchain4j.agentic.UntypedAgent;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A2A 远程客户端端到端（本地 stub）：启动一个仅提供 agent card 的本地 HTTP 服务端，
 * 验证门面 {@code a2a(...)} 构建的远程句柄确实可构建、可挂载进编排。
 *
 * <p>说明：A2A 客户端在 {@code build()} 时抓取 {@code /.well-known/agent-card.json}
 * 以确定对端能力，因此真实构建需要可访问的对端；本测试用本地 stub 提供最小卡片，
 * 避免依赖外网。真正的远程调用（JSON-RPC）不在本用例范围。</p>
 */
class AgenticA2AServerTest {

    private HttpServer server;
    private String baseUrl;

    @BeforeEach
    void startStubServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/.well-known/agent-card.json", exchange -> {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            String card = "{\n"
                    + "  \"name\": \"echo-remote\",\n"
                    + "  \"description\": \"本地 stub 远程 agent\",\n"
                    + "  \"url\": \"" + base + "\",\n"
                    + "  \"version\": \"1.0.0\",\n"
                    + "  \"capabilities\": {\"streaming\": false},\n"
                    + "  \"defaultInputModes\": [\"text/plain\"],\n"
                    + "  \"defaultOutputModes\": [\"text/plain\"],\n"
                    + "  \"skills\": [],\n"
                    + "  \"supportedInterfaces\": [{\"protocolBinding\": \"JSONRPC\", \"url\": \""
                    + base + "\", \"protocolVersion\": \"1.0\"}]\n"
                    + "}\n";
            byte[] body = card.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stopStubServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void a2aClientShouldBuildAndBeMountable() {
        UntypedAgent remote = AgenticWorkflows.a2a(baseUrl)
                .inputKeys("request")
                .outputKey("response")
                .build();

        assertThat(remote).isNotNull();

        // 作为 subAgent 挂入既有编排：构建不再触网，证明其可作为普通 agent 参与编排
        UntypedAgent pipeline = AgenticWorkflows.sequence()
                .subAgents(remote)
                .outputKey("response")
                .build();

        assertThat(pipeline).isNotNull();
    }

    @Test
    void a2aBuilderShouldExposeFrameworkBuilderWithoutInternalTypes() {
        assertThat(AgenticWorkflows.a2a(baseUrl)).isInstanceOf(AgenticA2ABuilder.class);
        assertThat(AgenticWorkflows.a2aBuilder(baseUrl)).isInstanceOf(AgenticA2ABuilder.class);
    }

    @Test
    void a2aUntypedBuildWithoutInputKeysShouldFailFast() {
        // 未指定 inputKeys 时，底层 A2A 客户端在 build 阶段快速失败
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        AgenticWorkflows.a2a(baseUrl).build())
                .isInstanceOf(IllegalArgumentException.class);
    }
}
