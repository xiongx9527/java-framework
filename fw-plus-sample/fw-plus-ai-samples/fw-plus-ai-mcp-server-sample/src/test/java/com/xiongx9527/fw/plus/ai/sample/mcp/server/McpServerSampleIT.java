package com.xiongx9527.fw.plus.ai.sample.mcp.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiongx9527.fw.plus.ai.sample.mcp.server.McpServerSampleApplication;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(classes = McpServerSampleApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class McpServerSampleIT {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("default runtime（/mcp）暴露未标注 @McpServer 的能力")
    public void defaultRuntimeExposesUnannotatedCapabilitiesOnly() {
        String sessionId = initialize("/mcp", "default-client");

        JsonNode tools = tools("/mcp", sessionId);
        assertThat(tools.path("result").path("tools").findValuesAsText("name"))
                .contains("sample-echo", "sample-bean-echo")
                .doesNotContain("weather-now", "order-status", "weather-report");

        JsonNode resources = json(restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(2, "resources/list", null), sessionHeaders(sessionId)), String.class).getBody());
        assertThat(resources.path("result").path("resources").findValuesAsText("uri"))
                .contains("sample://greeting");

        JsonNode prompts = json(restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(3, "prompts/list", null), sessionHeaders(sessionId)), String.class).getBody());
        assertThat(prompts.path("result").path("prompts").findValuesAsText("name"))
                .contains("sample-greeting");
    }

    @Test
    @DisplayName("weather runtime（/mcp/weather）只暴露 @McpServer(\"weather\") 绑定的能力")
    public void weatherRuntimeExposesWeatherBoundCapabilitiesOnly() {
        String sessionId = initialize("/mcp/weather", "weather-client");

        JsonNode tools = tools("/mcp/weather", sessionId);
        assertThat(tools.path("result").path("tools").findValuesAsText("name"))
                .containsExactly("weather-now")
                .doesNotContain("sample-echo", "order-status");

        JsonNode prompts = json(restTemplate.exchange(
                "/mcp/weather", HttpMethod.POST,
                new HttpEntity<>(rpc(2, "prompts/list", null), sessionHeaders(sessionId)), String.class).getBody());
        assertThat(prompts.path("result").path("prompts").findValuesAsText("name"))
                .contains("weather-report");
    }

    @Test
    @DisplayName("order runtime（/mcp/order）只暴露 @McpServer(\"order\") 绑定的能力")
    public void orderRuntimeExposesOrderBoundCapabilitiesOnly() {
        String sessionId = initialize("/mcp/order", "order-client");

        JsonNode tools = tools("/mcp/order", sessionId);
        assertThat(tools.path("result").path("tools").findValuesAsText("name"))
                .containsExactly("order-status")
                .doesNotContain("sample-echo", "weather-now");
    }

    @Test
    @DisplayName("跨 runtime 会话伪造被拒绝：weather 的会话不可用于 order")
    public void crossRuntimeSessionIsRejected() {
        String weatherSession = initialize("/mcp/weather", "weather-client");

        ResponseEntity<String> response = restTemplate.exchange(
                "/mcp/order", HttpMethod.POST,
                new HttpEntity<>(rpc(1, "tools/list", null), sessionHeaders(weatherSession)), String.class);

        // 不属于本 runtime 的会话直接失效（404），不进入 order 的能力面
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @DisplayName("样例工具调用：工具、资源、提示词均可经注解调用")
    public void shouldRunSampleMcpServerHandshakeAndCalls() {
        String sessionId = initialize("/mcp", "sample-client");

        Map<String, Object> toolCallParams = new LinkedHashMap<>();
        toolCallParams.put("name", "sample-echo");
        toolCallParams.put("arguments", Collections.singletonMap("message", "hi"));
        JsonNode toolResult = json(restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(2, "tools/call", toolCallParams), sessionHeaders(sessionId)),
                String.class).getBody());
        assertThat(toolResult.path("result").path("content").get(0).path("text").asText())
                .isEqualTo("sample-echo: hi");
    }

    private String initialize(String path, String clientName) {
        Map<String, Object> initializeBody = new LinkedHashMap<>();
        initializeBody.put("jsonrpc", "2.0");
        initializeBody.put("id", 1);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", "2025-06-18");
        params.put("capabilities", new LinkedHashMap<>());
        Map<String, Object> clientInfo = new LinkedHashMap<>();
        clientInfo.put("name", clientName);
        clientInfo.put("version", "1.0.0");
        params.put("clientInfo", clientInfo);
        initializeBody.put("method", "initialize");
        initializeBody.put("params", params);

        ResponseEntity<String> initializeResponse = restTemplate.exchange(
                path, HttpMethod.POST, new HttpEntity<>(initializeBody, jsonHeaders()), String.class);
        assertThat(initializeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String sessionId = initializeResponse.getHeaders().getFirst("Mcp-Session-Id");
        assertThat(sessionId).isNotBlank();

        // 严格生命周期：initialize 进入 INITIALIZING，须先发 notifications/initialized 转 READY，
        // 之后 tools/list 等请求才被允许。
        ResponseEntity<String> initialized = restTemplate.exchange(
                path, HttpMethod.POST,
                new HttpEntity<>(rpc(null, "notifications/initialized", null), sessionHeaders(sessionId)), String.class);
        assertThat(initialized.getStatusCodeValue()).isEqualTo(202);

        return sessionId;
    }

    private JsonNode tools(String path, String sessionId) {
        return json(restTemplate.exchange(
                path, HttpMethod.POST,
                new HttpEntity<>(rpc(-1, "tools/list", null), sessionHeaders(sessionId)), String.class).getBody());
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private static HttpHeaders sessionHeaders(String sessionId) {
        HttpHeaders headers = jsonHeaders();
        headers.add("MCP-Protocol-Version", "2025-06-18");
        headers.add("Mcp-Session-Id", sessionId);
        return headers;
    }

    private static Map<String, Object> rpc(Object id, String method, Map<String, Object> params) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("jsonrpc", "2.0");
        if (id != null) {
            body.put("id", id);
        }
        body.put("method", method);
        body.put("params", params);
        return body;
    }

    private static JsonNode json(String body) {
        try {
            return new com.fasterxml.jackson.databind.ObjectMapper().readTree(body);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid JSON response", e);
        }
    }
}