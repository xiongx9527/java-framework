package com.vesoft.nebula.jdbc.sql.trigger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class InMemoryTriggerMetadataStore implements TriggerMetadataStore {
	private final Map<String, TriggerDefinition> definitions = new LinkedHashMap<String, TriggerDefinition>();

	public synchronized TriggerDefinition findByName(String schemaName, String triggerName) {
		return definitions.get(key(schemaName, triggerName));
	}

	public synchronized void save(TriggerDefinition definition) {
		definitions.put(key(definition.getSchemaName(), definition.getTriggerName()), definition);
	}

	public synchronized boolean delete(String schemaName, String triggerName) {
		return definitions.remove(key(schemaName, triggerName)) != null;
	}

	public synchronized List<TriggerDefinition> findByTarget(String schemaName, String targetTable,
			TriggerEvent event, TriggerTiming timing) {
		List<TriggerDefinition> matches = new ArrayList<TriggerDefinition>();
		for (TriggerDefinition definition : definitions.values()) {
			if (equalsIgnoreCase(definition.getSchemaName(), schemaName)
					&& equalsIgnoreCase(definition.getTargetTable(), targetTable)
					&& definition.getEvent() == event
					&& definition.getTiming() == timing) {
				matches.add(definition);
			}
		}
		Collections.sort(matches, new Comparator<TriggerDefinition>() {
			public int compare(TriggerDefinition left, TriggerDefinition right) {
				int order = Long.compare(left.getOrder(), right.getOrder());
				if (order != 0) {
					return order;
				}
				return left.getTriggerName().compareToIgnoreCase(right.getTriggerName());
			}
		});
		return matches;
	}

	public synchronized long nextOrder(String schemaName) {
		long max = 0L;
		for (TriggerDefinition definition : definitions.values()) {
			if (equalsIgnoreCase(definition.getSchemaName(), schemaName) && definition.getOrder() > max) {
				max = definition.getOrder();
			}
		}
		return max + 1L;
	}

	private String key(String schemaName, String triggerName) {
		return normalize(schemaName) + "." + normalize(triggerName);
	}

	private String normalize(String value) {
		return value == null ? "" : value.trim().toLowerCase();
	}

	private boolean equalsIgnoreCase(String left, String right) {
		if (left == null) {
			return right == null;
		}
		return left.equalsIgnoreCase(right);
	}
}
