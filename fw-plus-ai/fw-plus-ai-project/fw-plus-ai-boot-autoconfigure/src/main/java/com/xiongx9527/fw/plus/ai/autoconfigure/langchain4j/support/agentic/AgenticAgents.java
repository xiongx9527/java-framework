package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.agentic;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 框架自有句柄拆包工具：将隔离层句柄还原为底层原生实例，便于透传给 agentic 原生 API。
 */
final class AgenticAgents {

    private AgenticAgents() {
    }

    static Object unwrap(Object agent) {
        if (agent == null) {
            return null;
        }
        if (agent instanceof AgenticSupervisor) {
            return ((AgenticSupervisor) agent).delegate();
        }
        if (agent instanceof AgenticHumanInTheLoop) {
            return ((AgenticHumanInTheLoop) agent).delegate();
        }
        return agent;
    }

    static Object[] unwrapAll(Object... agents) {
        if (agents == null || agents.length == 0) {
            return agents;
        }
        Object[] result = new Object[agents.length];
        for (int i = 0; i < agents.length; i++) {
            result[i] = unwrap(agents[i]);
        }
        return result;
    }

    static List<Object> unwrapAll(Collection<?> agents) {
        if (agents == null || agents.isEmpty()) {
            return Collections.emptyList();
        }
        List<Object> result = new ArrayList<>(agents.size());
        for (Object agent : agents) {
            result.add(unwrap(agent));
        }
        return result;
    }
}
