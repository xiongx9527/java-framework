package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.starter;

import com.ajaxjs.mcp.protocol.McpRequestRawInfo;
import com.ajaxjs.mcp.protocol.McpResponse;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.tools.CallToolResult;
import com.ajaxjs.mcp.protocol.tools.CallToolResultDetail;
import com.ajaxjs.mcp.server.McpServer;
import com.ajaxjs.mcp.server.feature.FeatureMgr;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolInvoker;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools.MagicApiMcpToolRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicApiMcpServerRegistryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final List<Object[]> calls = new ArrayList<>();

    private MagicApiMcpToolRegistry toolRegistry;

    @BeforeEach
    void setUp() {
        MagicResourceService magicResourceService = mock(MagicResourceService.class);
        PathMagicEntity echo = new PathMagicEntity();
        echo.setId("function-echo");
        echo.setName("echo");
        echo.setPath("/public/echo");
        ApiInfo list = new ApiInfo();
        list.setId("api-list");
        list.setName("订单列表");
        list.setGroupId("group-admin");
        list.setPath("/orders/list");
        list.setMethod("GET");
        when(magicResourceService.<MagicEntity>files("function")).thenReturn(Collections.singletonList(echo));
        when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.singletonList(list));
        when(magicResourceService.getGroupPath(anyString())).thenReturn("/public");
        when(magicResourceService.getGroupPath("group-admin")).thenReturn("/admin");
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        toolRegistry = new MagicApiMcpToolRegistry(magicResourceService, properties);
    }

    @Test
    void shouldExecuteManagedToolOnToolsCall() throws Exception {
        McpResponse response = registry().process(dummyResponse(),
                request("tools/call", "echo", "{\"name\":\"echo\",\"arguments\":{\"text\":\"hi\"}}"));

        CallToolResultDetail detail = detailOf(response);
        assertThat(response.getId()).isEqualTo(1);
        assertThat(detail.getIsError()).isFalse();
        assertThat(detail.getStructuredContent()).containsEntry("result", "echo:{text=hi}");
        assertThat(((ContentText) detail.getContent().get(0)).getText()).isEqualTo("echo:{text=hi}");
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0)[0]).isEqualTo("echo");
        assertThat(calls.get(0)[1]).isEqualTo(Collections.singletonMap("text", "hi"));
    }

    @Test
    void shouldExecuteManagedApiTool() throws Exception {
        McpResponse response = registry().process(dummyResponse(),
                request("tools/call", "GET /orders/list", "{\"name\":\"GET /orders/list\",\"arguments\":{\"page\":2}}"));

        CallToolResultDetail detail = detailOf(response);
        assertThat(detail.getIsError()).isFalse();
        assertThat(detail.getStructuredContent()).containsEntry("result", "api:GET /orders/list:2");
    }

    @Test
    void shouldPassThroughResponseForUnmanagedTool() throws Exception {
        McpResponse original = dummyResponse();

        McpResponse response = registry().process(original, request("tools/call", "foreign", "{\"name\":\"foreign\"}"));

        assertThat(response).isSameAs(original);
        assertThat(calls).isEmpty();
    }

    @Test
    void shouldPassThroughNonToolsCallResponse() throws Exception {
        McpResponse original = dummyResponse();

        McpResponse response = registry().process(original, request("prompts/get", "echo", "{\"name\":\"echo\"}"));

        assertThat(response).isSameAs(original);
    }

    @Test
    void shouldTolerateToolsCallWithoutArguments() throws Exception {
        McpResponse response = registry().process(dummyResponse(), request("tools/call", "echo", "{\"name\":\"echo\"}"));

        CallToolResultDetail detail = detailOf(response);
        assertThat(detail.getIsError()).isFalse();
        assertThat(detail.getStructuredContent()).containsEntry("result", "echo:{}");
    }

    @Test
    void shouldReturnErrorResultWhenInvocationFails() throws Exception {
        MagicApiMcpToolInvoker invoker = new MagicApiMcpToolInvoker(toolRegistry, (tool, arguments) -> {
            throw new IllegalStateException("boom");
        }, (info, arguments) -> "ok");

        McpResponse response = new MagicApiMcpServerRegistry(invoker)
                .process(dummyResponse(), request("tools/call", "echo", "{\"name\":\"echo\"}"));

        CallToolResultDetail detail = detailOf(response);
        assertThat(detail.getIsError()).isTrue();
        assertThat(detail.getStructuredContent()).containsEntry("message", "boom");
    }

    @Test
    void shouldDelegateToSuperWhenInvokerAbsent() throws Exception {
        McpResponse original = dummyResponse();

        McpResponse response = new MagicApiMcpServerRegistry(null)
                .process(original, request("tools/call", "echo", "{\"name\":\"echo\"}"));

        assertThat(response).isSameAs(original);
    }

    @Test
    void shouldKeepKernelBehaviourForToolsList() throws Exception {
        McpServer server = registry();
        server.setFeatureMgr(new FeatureMgr());

        McpResponse response = server.processMessage(request("tools/list", null, "{}"));

        assertThat(response).isNotNull();
        assertThat(calls).isEmpty();
    }

    private MagicApiMcpServerRegistry registry() {
        MagicApiMcpToolInvoker invoker = new MagicApiMcpToolInvoker(toolRegistry,
                (tool, arguments) -> {
                    calls.add(new Object[]{tool.getName(), arguments});
                    return tool.getName() + ":" + arguments;
                },
                (info, arguments) -> {
                    calls.add(new Object[]{info.getPath(), arguments});
                    return "api:" + info.getMethod() + " " + info.getPath() + ":" + arguments.get("page");
                });
        return new MagicApiMcpServerRegistry(invoker);
    }

    private static CallToolResultDetail detailOf(McpResponse response) {
        assertThat(response).isInstanceOf(CallToolResult.class);
        return (CallToolResultDetail) ((CallToolResult) response).getResult();
    }

    private static McpResponse dummyResponse() {
        McpResponse response = new McpResponse();
        response.setId(1);
        return response;
    }

    private static McpRequestRawInfo request(String method, String toolName, String params) throws Exception {
        String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"" + method + "\",\"params\":" + params + "}";
        return new McpRequestRawInfo(1, method, MAPPER.readTree(body));
    }
}
