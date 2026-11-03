package com.xiongx9527.fw.plus.ai.base.mcp;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.xiongx9527.fw.plus.ai.base.tool.ToolArgumentCodec;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionException;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class McpToolProviderTest {

    /** 极简编解码：参数 JSON 直接当字符串值。 */
    private static final ToolArgumentCodec CODEC = new ToolArgumentCodec() {
        @Override
        public Map<String, Object> decode(String arguments) {
            Map<String, Object> map = new HashMap<String, Object>();
            if (arguments != null && !arguments.isEmpty()) {
                map.put("q", arguments);
            }
            return map;
        }

        @Override
        public String encode(Object value) {
            return value == null ? null : String.valueOf(value);
        }
    };

    private static class FakeClient implements McpClient {

        private final int listCalls[] = {0};

        private Map<String, Object> lastArguments;

        private boolean fail;

        @Override
        public McpClientConfig config() {
            return McpClientConfig.builder().name("demo").build();
        }

        @Override
        public List<ToolSpecification> listTools() {
            listCalls[0]++;
            return Arrays.asList(ToolSpecification.builder().name("search").description("search docs").build());
        }

        @Override
        public String callTool(String toolName, Map<String, Object> arguments) {
            if (fail) {
                throw new McpException("upstream down");
            }
            this.lastArguments = arguments;
            return "result:" + toolName;
        }
    }

    @Test
    void cachesToolList() {
        FakeClient client = new FakeClient();
        McpToolProvider provider = new McpToolProvider(client, CODEC);

        assertEquals(1, provider.specifications().size());
        provider.specifications();

        assertEquals(1, client.listCalls[0]);
    }

    @Test
    void executesThroughClient() {
        FakeClient client = new FakeClient();
        McpToolProvider provider = new McpToolProvider(client, CODEC);

        String result = provider.execute(ToolExecutionRequest.of("search", "hello"));

        assertEquals("result:search", result);
        assertEquals("hello", client.lastArguments.get("q"));
    }

    @Test
    void failureIsWrappedAndNotified() {
        FakeClient client = new FakeClient();
        client.fail = true;
        final List<String> errors = new ArrayList<String>();
        McpToolProvider provider = new McpToolProvider(client, CODEC,
                java.util.Collections.singletonList(new McpListener() {
                    @Override
                    public void onError(String message) {
                        errors.add(message);
                    }
                }));

        assertThrows(ToolExecutionException.class,
                () -> provider.execute(ToolExecutionRequest.of("search", "x")));
        assertEquals(1, errors.size());
    }
}
