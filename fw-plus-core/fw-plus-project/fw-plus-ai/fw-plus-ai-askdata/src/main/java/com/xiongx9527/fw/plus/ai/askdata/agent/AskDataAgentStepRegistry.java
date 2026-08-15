package com.xiongx9527.fw.plus.ai.askdata.agent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 稳定步骤 Agent 的确定性注册表。
 *
 * <p>同一步骤存在多个候选时选择唯一最高优先级；最高优先级并列会在构造期失败，避免运行时静默选择。</p>
 */
public final class AskDataAgentStepRegistry {

	private final Map<String, AskDataAgent> selectedAgents;

	public AskDataAgentStepRegistry(Collection<? extends AskDataAgent> agents) {
		Map<String, List<AgentNode>> candidates = new LinkedHashMap<String, List<AgentNode>>();
		Set<String> agentKeys = new LinkedHashSet<String>();
		if (agents != null) {
			for (AskDataAgent agent : agents) {
				if (agent == null) {
					throw new IllegalArgumentException("Agent 不能为 null");
				}
				String agentKey = requireNonBlank(agent.getKey(), "Agent key");
				if (!agentKeys.add(agentKey)) {
					throw new IllegalArgumentException("Agent key 重复: " + agentKey);
				}
				String declaredStepKey = agent.getStepKey();
				if (declaredStepKey == null || declaredStepKey.trim().length() == 0) {
					continue;
				}
				String stepKey = declaredStepKey.trim();
				if (!AskDataAgentStepKey.isSupported(stepKey)) {
					throw new IllegalArgumentException("不支持的 Agent 步骤 key: " + stepKey + "，Agent: " + agentKey);
				}
				List<AgentNode> stepCandidates = candidates.get(stepKey);
				if (stepCandidates == null) {
					stepCandidates = new ArrayList<AgentNode>();
					candidates.put(stepKey, stepCandidates);
				}
				stepCandidates.add(new AgentNode(agent, agentKey, agent.getPriority()));
			}
		}
		Map<String, AskDataAgent> selected = new LinkedHashMap<String, AskDataAgent>();
		for (Map.Entry<String, List<AgentNode>> entry : candidates.entrySet()) {
			selected.put(entry.getKey(), select(entry.getKey(), entry.getValue()));
		}
		this.selectedAgents = Collections.unmodifiableMap(selected);
	}

	public AskDataAgent get(String stepKey) {
		if (!AskDataAgentStepKey.isSupported(stepKey)) {
			throw new IllegalArgumentException("不支持的 Agent 步骤 key: " + stepKey);
		}
		return selectedAgents.get(stepKey.trim());
	}

	private static AskDataAgent select(String stepKey, List<AgentNode> candidates) {
		AgentNode selected = null;
		List<String> conflicts = new ArrayList<String>();
		for (AgentNode candidate : candidates) {
			if (selected == null || candidate.priority > selected.priority) {
				selected = candidate;
				conflicts.clear();
				conflicts.add(candidate.description());
			} else if (candidate.priority == selected.priority) {
				conflicts.add(candidate.description());
			}
		}
		if (conflicts.size() > 1) {
			throw new IllegalArgumentException("步骤 " + stepKey + " 存在同优先级 Agent 冲突: " + conflicts);
		}
		return selected.agent;
	}

	private static String requireNonBlank(String value, String fieldName) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(fieldName + " 不能为空白");
		}
		return value.trim();
	}

	private static final class AgentNode {

		private final AskDataAgent agent;
		private final String key;
		private final int priority;

		private AgentNode(AskDataAgent agent, String key, int priority) {
			this.agent = agent;
			this.key = key;
			this.priority = priority;
		}

		private String description() {
			return key + "(" + agent.getClass().getName() + ", priority=" + priority + ")";
		}
	}
}
