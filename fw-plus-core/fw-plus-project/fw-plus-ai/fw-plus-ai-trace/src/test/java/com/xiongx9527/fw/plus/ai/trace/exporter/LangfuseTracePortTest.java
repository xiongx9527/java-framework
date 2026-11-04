package com.xiongx9527.fw.plus.ai.trace.exporter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.trace.Span;
import com.xiongx9527.fw.plus.ai.base.trace.SpanKind;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LangfuseTracePortTest {
    private final ObjectMapper mapper = new ObjectMapper();
    private Span.Builder llmSpan() {
        return Span.start("11111111-1111-1111-1111-111111111111", "llm", SpanKind.LLM).modelId("gpt-4o").input("hello").output("world").usage(new LlmUsage(10, 20, 30)).end(1000L);
    }
    @Test
    void emitsTraceAndGenerationBatch() throws Exception {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangfuseTracePort port = new LangfuseTracePort("https://langfuse.test/", "pk", "sk", sender);
        port.end(llmSpan());
        CapturingHttpSender.Record rec = sender.last();
        assertEquals("https://langfuse.test/api/public/ingestion", rec.url);
        JsonNode root = mapper.readTree(rec.body);
        JsonNode batch = root.get("batch");
        assertEquals(2, batch.size());
        assertEquals("trace.create", batch.get(0).get("type").asText());
        assertEquals("generation.create", batch.get(1).get("type").asText());
        JsonNode gen = batch.get(1).get("body");
        assertEquals("gpt-4o", gen.get("model").asText());
        assertEquals("GENERATION", gen.get("type").asText());
        assertEquals(10, gen.get("usage").get("input").asInt());
        assertEquals(20, gen.get("usage").get("output").asInt());
        assertEquals(30, gen.get("usage").get("total").asInt());
        assertEquals("11111111-1111-1111-1111-111111111111", gen.get("traceId").asText());
    }
    @Test
    void nonGenerationKindBecomesSpan() throws Exception {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangfuseTracePort port = new LangfuseTracePort("https://langfuse.test", "pk", "sk", sender);
        port.end(Span.start("t", "retrieval", SpanKind.RETRIEVAL).input("q").end(5L));
        JsonNode obs = mapper.readTree(sender.last().body).get("batch").get(1).get("body");
        assertEquals("SPAN", obs.get("type").asText());
        assertTrue(obs.get("usage") == null);
    }
    @Test
    void errorMarksLevel() throws Exception {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangfuseTracePort port = new LangfuseTracePort("https://langfuse.test", "pk", "sk", sender);
        port.end(Span.start("t", "llm", SpanKind.LLM).error("boom").end(5L));
        JsonNode obs = mapper.readTree(sender.last().body).get("batch").get(1).get("body");
        assertEquals("ERROR", obs.get("level").asText());
        assertEquals("boom", obs.get("statusMessage").asText());
    }
    @Test
    void setsBasicAuth() {
        CapturingHttpSender sender = new CapturingHttpSender();
        LangfuseTracePort port = new LangfuseTracePort("https://langfuse.test", "pk", "sk", sender);
        port.end(llmSpan());
        String auth = sender.last().headers.get("Authorization");
        assertNotNull(auth);
        String decoded = new String(Base64.getDecoder().decode(auth.substring("Basic ".length())), StandardCharsets.UTF_8);
        assertEquals("pk:sk", decoded);
    }
}
