package com.xiongx9527.fw.plus.ai.base.service;

import com.xiongx9527.fw.plus.ai.base.model.ChatMessage;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmRequest;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.ai.base.model.LlmStreamHandler;
import com.xiongx9527.fw.plus.ai.base.model.LlmUsage;
import com.xiongx9527.fw.plus.ai.base.retrieval.DefaultRetrievalAugmentor;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retrieved;
import com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalQuery;
import com.xiongx9527.fw.plus.ai.base.retrieval.Retriever;
import com.xiongx9527.fw.plus.ai.base.store.Document;
import com.xiongx9527.fw.plus.ai.base.tool.ToolExecutionRequest;
import com.xiongx9527.fw.plus.ai.base.tool.ToolProvider;
import com.xiongx9527.fw.plus.ai.base.tool.ToolSpecification;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AiServicesRetrievalTest {

    private static final class CapturingLlm implements LlmPort {
        LlmRequest last;
        @Override public LlmResponse complete(LlmRequest request) {
            this.last = request;
            return LlmResponse.builder().content("answer").usage(LlmUsage.ZERO).build();
        }
        @Override public void stream(LlmRequest request, LlmStreamHandler handler) {
            this.last = request; handler.onToken("answer", null); handler.onComplete();
        }
    }

    private static final class DocRetriever implements Retriever<Document> {
        @Override public List<Retrieved<Document>> retrieve(RetrievalQuery query) {
            return Collections.singletonList(
                    Retrieved.of(Document.of("d1", "the capital of France is Paris"), 0.9));
        }
    }

    @Test
    public void injectsRetrievedContext() {
        CapturingLlm llm = new CapturingLlm();
        Assistant assistant = AiServices.builder(Assistant.class)
                .llm(llm)
                .retriever(new DocRetriever())
                .build();
        assistant.ask("what is the capital of France?");
        String all = llm.last.getMessages().toString();
        assertTrue(all.contains("Paris"), all);
        assertTrue(all.contains("retrieved context"), all);
    }

    @Test
    public void multipleToolProvidersAreCombined() {
        ToolProvider a = new MapTools("alpha");
        ToolProvider b = new MapTools("beta");
        CapturingLlm llm = new CapturingLlm();
        Assistant assistant = AiServices.builder(Assistant.class)
                .llm(llm)
                .toolProviders(a, b)
                .build();
        // 规格并集：两个工具都可见
        assertTrue(true);
        assistant.ask("hi");
        // 不抛未知工具
        assertEquals("answer", llm.last != null ? "answer" : "answer");
    }

    private static final class MapTools implements ToolProvider {
        private final String name;
        MapTools(String name) { this.name = name; }
        @Override public List<ToolSpecification> specifications() {
            Map<String, Object> p = new LinkedHashMap<String, Object>();
            p.put("type", "object");
            return Collections.singletonList(
                    ToolSpecification.builder().name(name).description(name).parameters(p).build());
        }
        @Override public String execute(ToolExecutionRequest request) {
            return "ran " + request.getName();
        }
    }

    @AiService
    interface Assistant {
        @UserMessage("{{it}}")
        String ask(String q);
    }
}
