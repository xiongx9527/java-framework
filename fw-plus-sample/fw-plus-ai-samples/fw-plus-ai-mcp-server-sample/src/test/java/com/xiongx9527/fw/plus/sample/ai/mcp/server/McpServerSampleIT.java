package com.xiongx9527.fw.plus.sample.ai.mcp.server;

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
    @DisplayName("样例应用无需自定义配置即可握手，并可通过注解/SPI 扩展调用工具、资源、提示词")
    public void shouldRunSampleMcpServerHandshakeAndCalls() {
        Map<String, Object> initializeBody = new LinkedHashMap<>();
        initializeBody.put("jsonrpc", "2.0");
        initializeBody.put("id", 1);
        Map<String, Object> params = new LinkedHashMap<>();
        params.put("protocolVersion", "2025-11-25");
        params.put("capabilities", new LinkedHashMap<>());
        params.put("clientInfo", Collections.singletonMap("name", "sample-client"));
        initializeBody.put("method", "initialize");
        initializeBody.put("params", params);

        ResponseEntity<String> initializeResponse = restTemplate.exchange(
                "/mcp", HttpMethod.POST, new HttpEntity<>(initializeBody, jsonHeaders()), String.class);
        assertThat(initializeResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        String sessionId = initializeResponse.getHeaders().getFirst("Mcp-Session-Id");
        assertThat(sessionId).isNotBlank();

        HttpHeaders sessionHeaders = jsonHeaders();
        sessionHeaders.add("Mcp-Session-Id", sessionId);

        ResponseEntity<String> initialized = restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(null, "notifications/initialized", null), sessionHeaders), String.class);
        assertThat(initialized.getStatusCodeValue()).isEqualTo(202);

        JsonNode tools = json(restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(2, "tools/list", null), sessionHeaders), String.class).getBody());
        assertThat(tools.path("result").path("tools").findValuesAsText("name"))
                .contains("sample-echo", "sample-bean-echo");

        JsonNode toolResult = json(restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(3, "tools/call", Collections.singletonMap("name", "sample-echo")), sessionHeaders),
                String.class).getBody());
        assertThat(toolResult.path("result").path("content").get(0).path("text").asText())
                .isEqualTo("sample-echo: null");

        JsonNode resources = json(restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(4, "resources/list", null), sessionHeaders), String.class).getBody());
        assertThat(resources.path("result").path("resources").findValuesAsText("uri"))
                .contains("sample://greeting");

        JsonNode prompts = json(restTemplate.exchange(
                "/mcp", HttpMethod.POST,
                new HttpEntity<>(rpc(5, "prompts/list", null), sessionHeaders), String.class).getBody());
        assertThat(prompts.path("result").path("prompts").findValuesAsText("name"))
                .contains("sample-greeting");
    }

    private static HttpHeaders jsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
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
