package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.tools;

import com.ajaxjs.mcp.protocol.common.Content;
import com.ajaxjs.mcp.protocol.common.ContentText;
import com.ajaxjs.mcp.protocol.tools.StructuredToolResult;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.mcp.config.MagicApiMcpProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.ssssssss.magicapi.core.model.ApiInfo;
import org.ssssssss.magicapi.core.model.MagicEntity;
import org.ssssssss.magicapi.core.model.PathMagicEntity;
import org.ssssssss.magicapi.core.service.MagicResourceService;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MagicApiMcpToolInvokerTest {

    private MagicResourceService magicResourceService;

    @BeforeEach
    void setUp() {
        magicResourceService = mock(MagicResourceService.class);
        MagicEntity echo = new PathMagicEntity();
        echo.setId("function-echo");
        echo.setName("echo");
        ((PathMagicEntity) echo).setPath("/public/echo");
        ApiInfo list = new ApiInfo();
        list.setId("api-list");
        list.setName("订单列表");
        list.setGroupId("group-admin");
        list.setPath("/orders/list");
        list.setMethod("GET");
        list.setScript("return 1;");
        when(magicResourceService.<MagicEntity>files("function")).thenReturn(Collections.singletonList(echo));
        when(magicResourceService.<ApiInfo>files("api")).thenReturn(Collections.singletonList(list));
        when(magicResourceService.getGroupPath(anyString())).thenReturn("/admin");
        when(magicResourceService.getGroupPath("group-public")).thenReturn("/public");
    }

    @Test
    void shouldReturnNullForUnknownTool() {
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, args) -> "ok", (info, args) -> "ok");

        assertThat(invoker.invoke("not-exist", Collections.emptyMap())).isNull();
    }

    @Test
    void shouldNotInvokeApiWhenDisabled() {
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, args) -> {
            throw new AssertionError("function should not be invoked");
        }, (info, args) -> "should-not-run");

        assertThat(invoker.invoke("GET /orders/list", Collections.emptyMap())).isNull();
    }

    @Test
    void shouldReturnStructuredContentForMapResult() {
        Map<String, Object> returned = new HashMap<>();
        returned.put("text", "hello");
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, args) -> returned, (info, args) -> returned);

        StructuredToolResult result = invoker.invoke("echo", Collections.emptyMap());

        assertThat(result.isError()).isFalse();
        assertThat(result.getStructuredContent()).isEqualTo(returned);
        assertThat(result.getContent()).hasSize(1);
        Content content = result.getContent().get(0);
        assertThat(content).isInstanceOf(ContentText.class);
        assertThat(content.getType()).isEqualTo("text");
        assertThat(((ContentText) content).getText()).isEqualTo(String.valueOf(returned));
    }

    @Test
    void shouldWrapNonMapResultIntoStructuredContent() {
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, args) -> 42, (info, args) -> 42);

        StructuredToolResult result = invoker.invoke("echo", Collections.emptyMap());

        assertThat(result.isError()).isFalse();
        assertThat(result.getStructuredContent()).isEqualTo(single("result", (Object) 42));
        assertThat(((ContentText) result.getContent().get(0)).getText()).isEqualTo("42");
    }

    @Test
    void shouldReturnErrorResultWhenInvocationFails() {
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, args) -> {
            throw new IllegalStateException("boom");
        }, (info, args) -> "ok");

        StructuredToolResult result = invoker.invoke("echo", Collections.emptyMap());

        assertThat(result.isError()).isTrue();
        assertThat(result.getStructuredContent()).containsEntry("message", "boom");
        assertThat(((ContentText) result.getContent().get(0)).getText()).isEqualTo("boom");
    }

    @Test
    void shouldInvokeApiToolWhenApiEnabled() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getApi().setEnabled(true);
        boolean[] invoked = new boolean[1];
        MagicApiMcpToolInvoker invoker = invoker(properties, (tool, args) -> "function", (info, args) -> {
            invoked[0] = true;
            return "api-result";
        });

        StructuredToolResult result = invoker.invoke("GET /orders/list", Collections.singletonMap("status", "paid"));

        assertThat(invoked[0]).isTrue();
        assertThat(result.isError()).isFalse();
        assertThat(result.getStructuredContent()).isEqualTo(single("result", (Object) "api-result"));
    }

    @Test
    void shouldPassArgumentsToFunctionInvoker() {
        Map<String, Object> args = Collections.singletonMap("text", "hi");
        Object[] captured = new Object[1];
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, arguments) -> {
            captured[0] = arguments;
            return "ok";
        }, (info, arguments) -> "ok");

        invoker.invoke("echo", args);

        assertThat(captured[0]).isSameAs(args);
    }

    @Test
    void shouldTolerateNullArguments() {
        Object[] captured = new Object[1];
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, arguments) -> {
            captured[0] = arguments;
            return arguments;
        }, (info, arguments) -> "ok");

        StructuredToolResult result = invoker.invoke("echo", null);

        assertThat(captured[0]).isEqualTo(Collections.emptyMap());
        assertThat(result.isError()).isFalse();
        assertThat(result.getStructuredContent()).isEmpty();
    }

    @Test
    void shouldUseSnapshotFilteringForLookup() {
        MagicApiMcpProperties properties = new MagicApiMcpProperties();
        properties.getTools().getFunctions().getExcludePaths().add("/public/**");
        MagicApiMcpToolInvoker invoker = invoker(properties, (tool, args) -> "should-not-run", (info, args) -> "ok");

        assertThat(invoker.invoke("echo", Collections.emptyMap())).isNull();
    }

    @Test
    void shouldTolerateNullToolName() {
        MagicApiMcpToolInvoker invoker = invoker(new MagicApiMcpProperties(), (tool, args) -> "ok", (info, args) -> "ok");

        assertThat(invoker.invoke(null, Collections.emptyMap())).isNull();
    }

    private MagicApiMcpToolInvoker invoker(MagicApiMcpProperties properties,
            MagicApiMcpToolInvoker.FunctionInvoker functionInvoker,
            MagicApiMcpToolInvoker.ApiInvoker apiInvoker) {
        MagicApiMcpToolRegistry registry = new MagicApiMcpToolRegistry(magicResourceService, properties);
        return new MagicApiMcpToolInvoker(registry, functionInvoker, apiInvoker);
    }

    private static Map<String, Object> single(String key, Object value) {
        Map<String, Object> map = new HashMap<>();
        map.put(key, value);
        return map;
    }
}
