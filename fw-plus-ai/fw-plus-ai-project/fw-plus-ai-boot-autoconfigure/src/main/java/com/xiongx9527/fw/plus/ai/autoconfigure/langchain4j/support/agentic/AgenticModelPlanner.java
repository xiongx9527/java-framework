package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import dev.langchain4j.agentic.planner.Action;
import dev.langchain4j.agentic.planner.AgentInstance;
import dev.langchain4j.agentic.planner.InitPlanningContext;
import dev.langchain4j.agentic.planner.Planner;
import dev.langchain4j.agentic.planner.PlanningContext;
import dev.langchain4j.agentic.scope.AgenticScope;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 内部 LLM 驱动的 Planner：基于 ChatModel 自主选择下一步 agent 或结束。
 * <p>响应格式约定：{"agentId": "&lt;agentId&gt;", "arguments": {...}} 或 {"agentId": "done", "response": "&lt;最终答复&gt;"}</p>
 */
final class AgenticModelPlanner implements Planner {

    private static final Pattern AGENT_ID_PATTERN = Pattern.compile("\"agentId\"\\s*:\\s*\"([^\"]+)\"");
    private static final Pattern RESPONSE_PATTERN = Pattern.compile("\"response\"\\s*:\\s*\"([^\"]*)\"");

    private final ChatModel chatModel;
    private final String outputKey;
    private final int maxDecisions;
    private List<AgentInstance> agents = Collections.emptyList();
    private int decisions;

    AgenticModelPlanner(ChatModel chatModel, String outputKey, int maxDecisions) {
        if (chatModel == null) {
            throw new IllegalArgumentException("chatModel must not be null");
        }
        if (maxDecisions <= 0) {
            throw new IllegalArgumentException("maxDecisions must be > 0");
        }
        this.chatModel = chatModel;
        this.outputKey = outputKey;
        this.maxDecisions = maxDecisions;
    }

    AgenticModelPlanner(ChatModel chatModel, String outputKey) {
        this(chatModel, outputKey, 20);
    }

    @Override
    public void init(InitPlanningContext ctx) {
        this.agents = new ArrayList<>(ctx.subagents());
    }

    @Override
    public Action nextAction(PlanningContext ctx) {
        if (++decisions > maxDecisions) {
            return done();
        }
        String prompt = buildPrompt(ctx);
        ChatRequest request = ChatRequest.builder()
                .messages(Collections.<ChatMessage>singletonList(UserMessage.from(prompt)))
                .build();
        ChatResponse response = chatModel.chat(request);
        AiMessage aiMessage = response != null ? response.aiMessage() : null;
        String raw = aiMessage != null && aiMessage.text() != null ? aiMessage.text() : "";
        Decision decision = parseDecision(raw);
        if (decision.isDone()) {
            String resp = decision.response == null ? raw : decision.response;
            if (outputKey != null) {
                AgenticScope scope = ctx.agenticScope();
                if (scope != null) {
                    scope.writeState(outputKey, resp);
                }
            }
            return done(resp);
        }
        AgentInstance target = findAgent(decision.agentId);
        if (target == null) {
            if (outputKey != null) {
                AgenticScope scope = ctx.agenticScope();
                if (scope != null) {
                    scope.writeState(outputKey, raw);
                }
            }
            return done(raw);
        }
        return call(target);
    }

    private String buildPrompt(PlanningContext ctx) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an LLM planner for langchain4j-agentic.\n");
        sb.append("Available agents (agentId: description):\n");
        for (AgentInstance agent : agents) {
            String id = agent.agentId();
            String desc = agent.description();
            if (desc == null) {
                desc = "";
            }
            sb.append("- ").append(id).append(": ").append(desc).append("\n");
        }
        sb.append("\nRespond with JSON: {\"agentId\":\"<agentId>\",\"arguments\":{...}} or {\"agentId\":\"done\",\"response\":\"<final answer>\"}\n");
        AgenticScope scope = ctx.agenticScope();
        if (scope != null && scope.readState("lastResult") != null) {
            sb.append("lastResult: ").append(scope.readState("lastResult")).append("\n");
        }
        return sb.toString();
    }

    private Decision parseDecision(String raw) {
        String agentId = extract(AGENT_ID_PATTERN, raw);
        String response = extract(RESPONSE_PATTERN, raw);
        if (agentId == null) {
            return Decision.done(response);
        }
        if ("done".equalsIgnoreCase(agentId)) {
            return Decision.done(response);
        }
        return Decision.call(agentId);
    }

    private static String extract(Pattern pattern, String raw) {
        if (raw == null) {
            return null;
        }
        Matcher m = pattern.matcher(raw);
        if (m.find()) {
            return m.group(1);
        }
        return null;
    }

    private AgentInstance findAgent(String agentId) {
        if (agentId == null) {
            return null;
        }
        for (AgentInstance agent : agents) {
            if (agentId.equals(agent.agentId())) {
                return agent;
            }
            if (agentId.equals(agent.name())) {
                return agent;
            }
        }
        return null;
    }

    private static class Decision {
        final String agentId;
        final String response;
        final boolean done;

        private Decision(String agentId, String response, boolean done) {
            this.agentId = agentId;
            this.response = response;
            this.done = done;
        }

        static Decision call(String agentId) {
            return new Decision(agentId, null, false);
        }

        static Decision done(String response) {
            return new Decision(null, response, true);
        }

        boolean isDone() {
            return done;
        }
    }
}
