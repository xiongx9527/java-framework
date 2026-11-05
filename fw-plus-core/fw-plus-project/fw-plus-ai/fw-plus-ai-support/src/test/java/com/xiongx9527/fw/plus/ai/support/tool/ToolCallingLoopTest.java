package com.xiongx9527.fw.plus.ai.base.tool;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.ModelInfo;
import com.xiongx9527.fw.plus.ai.base.model.ModelValidation;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmTokenStream;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ToolCallingLoopTest {

    private static final class ScriptedLlm implements LlmPort {


        private final List<LlmResponse> script;

        private int index;

        ScriptedLlm(List<LlmResponse> script) {
            this.script = script;
        }

        @Override
        public LlmResponse complete(LlmRequest request) {
            return next().build();
        }

        @Override
        public LlmTokenStream stream(LlmRequest request) {
            return LlmTokenStream.create(handler -> {
                LlmResponse r = next().build();
                if (r.getContent() != null) {
                    handler.onToken(r.getContent(), null);
                }
                for (ToolExecutionRequest call : r.getToolCalls()) {
                    handler.onToolCall(call);
                }
                handler.onUsageDetail(1, 1, 2);
                handler.onComplete();
            });
        }

        private LlmResponse.Builder next() {
            LlmResponse r = script.get(Math.min(index, script.size() - 1));
            index++;
            return LlmResponse.builder().content(r.getContent()).toolCalls(r.getToolCalls()).usage(LlmUsage.ZERO);
        }
    }

    private static final class StubTools implements ToolProvider {

        @Override
        public List<ToolSpecification> specifications() {
            Map<String, Object> params = new LinkedHashMap<String, Object>();
            params.put("type", "object");
            return Collections.singletonList(ToolSpecification.builder()
                    .name("get_weather").description("weather").parameters(params).build());
        }

        @Override
        public String execute(ToolExecutionRequest request) {
            return "sunny in " + request.getArguments();
        }
    }

    @Test
    public void runsLoopThenFinalAnswer() {
        List<ChatMessage> messages = new ArrayList<ChatMessage>();
        messages.add(ChatMessage.user("weather in Paris?"));

        List<LlmResponse> script = new ArrayList<LlmResponse>();
        script.add(LlmResponse.builder()
                .toolCalls(Collections.singletonList(new ToolExecutionRequest("c1", "get_weather", "{\"city\":\"Paris\"}")))
                .build());
        script.add(LlmResponse.builder().content("It is sunny in Paris.").build());

        ToolCallingLoop loop = new ToolCallingLoop(new ScriptedLlm(script), new StubTools());
        ToolCallingResult result = loop.call(ToolCallingRequest.builder()
                .messages(messages).model("m").build());

        assertEquals(ToolStopReason.FINAL, result.getStopReason());
        assertEquals("It is sunny in Paris.", result.getAnswer());
        assertEquals(1, result.getTraces().size());
        assertTrue(result.getTraces().get(0).getName().equals("get_weather"));
    }
}
