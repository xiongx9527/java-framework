package com.xiongx9527.fw.plus.ai.sample.agentscope.web;

import com.xiongx9527.fw.plus.ai.sample.agentscope.tool.SampleTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.TextBlock;
import io.agentscope.core.message.UserMessage;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * AgentScope 对话入口。
 *
 * <p>注入的 {@link ReActAgent} 由 fw-plus-ai-boot-autoconfigure 的
 * {@code AgentscopeAutoConfiguration} 条件装配（本样例提供了 {@code Model} bean 与带工具的
 * {@code Toolkit}）；本 Controller 只负责收发消息，不感知 AgentScope 的装配细节。
 */
@RestController
@RequestMapping("/api/agentscope")
public class AgentscopeController {

    private final ReActAgent agent;

    private final SampleTools sampleTools;

    public AgentscopeController(ReActAgent agent, SampleTools sampleTools) {
        this.agent = agent;
        this.sampleTools = sampleTools;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody ChatRequest request) {
        int before = sampleTools.getCallCount();
        Msg reply = agent
                .call(List.of(new UserMessage(request.getMessage())))
                .block(Duration.ofSeconds(180));

        String text = reply.getContent().stream()
                .filter(TextBlock.class::isInstance)
                .map(block -> ((TextBlock) block).getText())
                .collect(Collectors.joining("\n"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("reply", text);
        body.put("toolCalls", sampleTools.getCallCount() - before);
        return ResponseEntity.ok(body);
    }

    /**
     * 对话请求体。
     */
    public static class ChatRequest {

        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }
}
