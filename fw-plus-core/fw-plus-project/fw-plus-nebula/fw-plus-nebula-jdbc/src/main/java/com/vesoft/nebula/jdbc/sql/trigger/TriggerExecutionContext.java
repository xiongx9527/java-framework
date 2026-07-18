package com.vesoft.nebula.jdbc.sql.trigger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TriggerExecutionContext {
	private final int depth;
	private final int maxDepth;
	private final List<String> triggerChain;

	private TriggerExecutionContext(int depth, int maxDepth, List<String> triggerChain) {
		if (maxDepth < 1) {
			throw new IllegalArgumentException("Trigger max depth must be greater than zero.");
		}
		this.depth = depth;
		this.maxDepth = maxDepth;
		this.triggerChain = Collections.unmodifiableList(new ArrayList<String>(triggerChain));
	}

	public static TriggerExecutionContext root(int maxDepth) {
		return new TriggerExecutionContext(0, maxDepth, Collections.<String>emptyList());
	}

	public TriggerExecutionContext enter(TriggerDefinition definition) throws SQLException {
		if (definition == null) {
			throw new IllegalArgumentException("Trigger definition must not be null.");
		}
		if (depth >= maxDepth) {
			throw new SQLException("Nebula MySQL trigger subset error: Trigger recursion depth exceeded.", "HY000");
		}
		List<String> nextChain = new ArrayList<String>(triggerChain);
		nextChain.add(triggerId(definition));
		return new TriggerExecutionContext(depth + 1, maxDepth, nextChain);
	}

	public int getDepth() {
		return depth;
	}

	public int getMaxDepth() {
		return maxDepth;
	}

	public List<String> getTriggerChain() {
		return triggerChain;
	}

	public boolean isTriggeredBy(String triggerId) {
		return triggerChain.contains(triggerId);
	}

	public static String triggerId(TriggerDefinition definition) {
		return definition.getSchemaName() + "." + definition.getTriggerName();
	}
}
