package com.xiongx9527.fw.plus.ai.langchain4j.adapter.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionException;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import dev.langchain4j.agent.tool.ToolParameters;
import dev.langchain4j.service.tool.ToolProviderResult;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class LangChain4jToolProviderAdapterTest {

    private static dev.langchain4j.service.tool.ToolProvider upstream(final boolean includeSearch) {
        dev.langchain4j.agent.tool.ToolSpecification spec =
                dev.langchain4j.agent.tool.ToolSpecification.builder()
                        .name("search")
                        .description("search docs")
                        .parameters(ToolParameters.builder()
                                .type("object")
                                .properties(Collections.singletonMap("q",
                                        Collections.<String, Object>singletonMap("type", "string")))
                                .required(Collections.singletonList("q"))
                                .build())
                        .build();
        Map<dev.langchain4j.agent.tool.ToolSpecification,
                dev.langchain4j.service.tool.ToolExecutor> tools = new LinkedHashMap<>();
        if (includeSearch) {
            tools.put(spec, (call, memoryId) -> "result:" + call.arguments());
        }
        return request -> new ToolProviderResult(tools);
    }

    @Test
    void upstreamProviderBecomesBaseProvider() {
        LangChain4jToolProviderAdapter adapter =
                new LangChain4jToolProviderAdapter(upstream(true));

        assertEquals(1, adapter.specifications().size());
        ToolSpecification spec = adapter.specifications().get(0);
        assertEquals("search", spec.getName());
        assertEquals("object", spec.getParameters().get("type"));
        @SuppressWarnings("unchecked")
        Map<String, Object> properties =
                (Map<String, Object>) spec.getParameters().get("properties");
        assertTrue(properties.containsKey("q"));

        String result = adapter.execute(new ToolExecutionRequest("c1", "search", "{\"q\":\"x\"}"));
        assertEquals("result:{\"q\":\"x\"}", result);
    }

    @Test
    void unknownToolFailsWithToolExecutionException() {
        LangChain4jToolProviderAdapter adapter =
                new LangChain4jToolProviderAdapter(upstream(true));
        assertThrows(ToolExecutionException.class,
                () -> adapter.execute(new ToolExecutionRequest("c1", "nope", "{}")));
    }

    @Test
    void refreshPicksUpChangedToolSet() {
        AtomicReference<Boolean> visible = new AtomicReference<>(Boolean.TRUE);
        dev.langchain4j.service.tool.ToolProvider provider =
                request -> new ToolProviderResult(visible.get()
                        ? upstream(true).provideTools(request).tools()
                        : Collections.<dev.langchain4j.agent.tool.ToolSpecification,
                                dev.langchain4j.service.tool.ToolExecutor>emptyMap());
        LangChain4jToolProviderAdapter adapter = new LangChain4jToolProviderAdapter(provider);
        assertEquals(1, adapter.specifications().size());

        visible.set(Boolean.FALSE);
        adapter.refresh();
        assertTrue(adapter.specifications().isEmpty());
    }

    @Test
    void baseProviderBecomesUpstreamProvider() {
        ToolProvider base = new ToolProvider() {
            @Override
            public List<ToolSpecification> specifications() {
                return Collections.singletonList(ToolSpecification.builder()
                        .name("calc").description("calc").build());
            }

            @Override
            public String execute(ToolExecutionRequest request) {
                return "ok:" + request.getName();
            }
        };

        dev.langchain4j.service.tool.ToolProvider provider =
                LangChain4jToolProviderAdapter.toUpstream(base);
        ToolProviderResult result = provider.provideTools(
                new dev.langchain4j.service.tool.ToolProviderRequest(null, null));

        assertEquals(1, result.tools().size());
        Map.Entry<dev.langchain4j.agent.tool.ToolSpecification,
                dev.langchain4j.service.tool.ToolExecutor> entry =
                result.tools().entrySet().iterator().next();
        assertEquals("calc", entry.getKey().name());
        dev.langchain4j.agent.tool.ToolExecutionRequest call =
                dev.langchain4j.agent.tool.ToolExecutionRequest.builder()
                        .id("c1").name("calc").arguments("{}").build();
        assertEquals("ok:calc", entry.getValue().execute(call, null));
    }
}
