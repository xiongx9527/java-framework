package com.xiongx9527.fw.plus.ai.tinyflow.workflow;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryTinyflowWorkflowRepository implements TinyflowWorkflowRepository {

	private final Map<String, TinyflowWorkflow> workflows = new LinkedHashMap<String, TinyflowWorkflow>();

	@Override
	public synchronized TinyflowWorkflow findById(String id) {
		return workflows.get(id);
	}

	@Override
	public synchronized TinyflowWorkflow findByPath(String path) {
		TinyflowWorkflow match = null;
		List<String> matchingIds = new ArrayList<String>();
		for (TinyflowWorkflow workflow : workflows.values()) {
			if (equals(path, workflow.getPath())) {
				match = workflow;
				matchingIds.add(workflow.getId());
			}
		}
		if (matchingIds.size() > 1) {
			Collections.sort(matchingIds);
			throw new TinyflowException("Workflow path is ambiguous: " + path + ", matching ids: " + matchingIds);
		}
		return match;
	}

	@Override
	public synchronized void save(TinyflowWorkflow workflow) {
		if (workflow == null) {
			throw new IllegalArgumentException("workflow must not be null");
		}
		workflows.put(workflow.getId(), workflow);
	}

	@Override
	public synchronized void delete(String id) {
		workflows.remove(id);
	}

	private boolean equals(String left, String right) {
		return left == null ? right == null : left.equals(right);
	}
}
