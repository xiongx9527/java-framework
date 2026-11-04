package com.xiongx9527.fw.plus.ai.trace.exporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.SpanKind;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class LangSmithTracePortTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Test
    void llmSpanMapsToLlmRunWithTokens() throws Exception {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangSmithTracePort port = new LangSmithTracePort("https://ls.test/", "key", sender);
        port.end(Span.start("11111111-1111-1111-1111-111111111111", "llm", SpanKind.LLM).modelId("gpt-4o").input("hi").output("yo").usage(new LlmUsage(7, 8, 15)).end(1000L));
        assertEquals("https://ls.test/api/v1/runs", sender.last().url);
        JsonNode run = mapper.readTree(sender.last().body);
        assertEquals("llm", run.get("run_type").asText());
        assertEquals("11111111-1111-1111-1111-111111111111", run.get("trace_id").asText());
        assertEquals(7, run.get("extra").get("tokens").get("prompt").asInt());
        assertEquals(15, run.get("extra").get("tokens").get("total").asInt());
        assertEquals("hi", run.get("inputs").get("content").asText());
    }
    @Test
    void toolSpanMapsToToolRun() throws Exception {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangSmithTracePort port = new LangSmithTracePort("https://ls.test", "key", sender);
        port.end(Span.start("t", "tool", SpanKind.TOOL).input("args").end(5L));
        JsonNode run = mapper.readTree(sender.last().body);
        assertEquals("tool", run.get("run_type").asText());
        assertNull(run.get("extra"));
    }
    @Test
    void errorFieldCarried() throws Exception {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangSmithTracePort port = new LangSmithTracePort("https://ls.test", "key", sender);
        port.end(Span.start("t", "chain", SpanKind.CHAIN).error("fail").end(5L));
        JsonNode run = mapper.readTree(sender.last().body);
        assertEquals("fail", run.get("error").asText());
        assertEquals("chain", run.get("run_type").asText());
    }
    @Test
    void setsApiKeyHeader() {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangSmithTracePort port = new LangSmithTracePort("https://ls.test", "secret-key", sender);
        port.end(Span.start("t", "llm", SpanKind.LLM).end(5L));
        assertEquals("secret-key", sender.last().headers.get("x-api-key"));
        assertNotNull(sender.last().headers.get("x-api-key"));
    }
}
