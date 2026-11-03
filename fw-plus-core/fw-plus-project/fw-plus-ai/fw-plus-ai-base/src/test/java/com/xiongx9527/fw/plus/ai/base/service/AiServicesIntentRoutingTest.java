package com.xiongx9527.fw.plus.ai.base.service;

import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemory;
import com.xiongx9527.fw.plus.ai.base.memory.ChatMemoryProvider;
import com.xiongx9527.fw.plus.ai.base.service.MemoryId;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import java.util.ArrayList;
import java.util.List;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AiServicesIntentRoutingTest {

    private static final class CapturingLlm implements LlmPort {
        LlmRequest last;
        @Override public LlmResponse complete(LlmRequest request) {
            this.last = request; return LlmResponse.builder().content("ok").usage(LlmUsage.ZERO).build();
        }
        @Override public void stream(LlmRequest request, LlmStreamHandler h) {
            this.last = request; h.onToken("ok", null); h.onComplete();
        }
    }

    private static final class FixedIntent implements IntentPort {
        @Override public IntentResult classify(IntentRequest request) {
            String text = request.getText() == null ? "" : request.getText();
            String ref = text.contains("sales") ? "sql" : null;
            if (ref == null) {
                return IntentResult.of(Collections.<com.xiongx9527.fw.plus.ai.base.intent.IntentMatch>emptyList());
            }
            return IntentResult.of(Collections.singletonList(
                    new com.xiongx9527.fw.plus.ai.base.intent.IntentMatch(ref, ref, 1.0)));
        }
    }

    private static ToolProvider named(String tool) {
        Map<String, Object> p = new LinkedHashMap<String, Object>();
        p.put("type", "object");
        return new DefaultToolProviderProbe(tool);
    }

    @AiService
    interface Assistant {
        @UserMessage("{{it}}")
        String ask(String q);
    }

    @Test
    public void routesToolsByIntent() {
        CapturingLlm llm = new CapturingLlm();
        Assistant assistant = AiServices.builder(Assistant.class)
                .llm(llm)
                .intentClassifier(new FixedIntent())
                .toolProvider(named("general"))                 // 全局
                .toolProviderOn(named("sql_query"), "sql")      // 仅 sql 意图
                .build();

        assistant.ask("show me sales");
        assertTrue(hasTool(llm.last, "sql_query"));
        assertTrue(hasTool(llm.last, "general"));

        assistant.ask("hello");
        assertFalse(hasTool(llm.last, "sql_query"));
        assertTrue(hasTool(llm.last, "general"));
    }

    private static boolean hasTool(LlmRequest request, String name) {
        if (request == null || request.getTools() == null) {
            return false;
        }
        for (ToolSpecification spec : request.getTools()) {
            if (spec.getName().equals(name)) {
                return true;
            }
        }
        return false;
    }

    @AiService
    interface AssistantWithMemory {
        @UserMessage("{{it}}")
        String ask(@MemoryId String id, String q);
    }

    private static final class RecordingMemoryProvider implements ChatMemoryProvider {
        private boolean touched;
        @Override public ChatMemory get(String memoryId) {
            this.touched = true;
            return new NoopMemory(memoryId);
        }
        boolean touched() { return touched; }
    }

    private static final class NoopMemory implements ChatMemory {
        private final String id;
        NoopMemory(String id) { this.id = id; }
        @Override public String id() { return id; }
        @Override public void add(ChatMessage message) { }
        @Override public List<ChatMessage> messages() { return new ArrayList<ChatMessage>(); }
        @Override public void clear() { }
    }

    @Test
    public void routesMemoryByIntent() {
        CapturingLlm llm = new CapturingLlm();
        RecordingMemoryProvider global = new RecordingMemoryProvider();
        RecordingMemoryProvider sqlMemory = new RecordingMemoryProvider();
        AssistantWithMemory assistant = AiServices.builder(AssistantWithMemory.class)
                .llm(llm)
                .intentClassifier(new FixedIntent())
                .chatMemoryProvider(global)              // 全局
                .chatMemoryProviderOn(sqlMemory, "sql")  // 仅 sql 意图
                .build();

        assistant.ask("show me sales", "m1");
        assertTrue(sqlMemory.touched());
        assertFalse(global.touched());

        assistant.ask("hello", "m2");
        assertTrue(global.touched());
    }
}
