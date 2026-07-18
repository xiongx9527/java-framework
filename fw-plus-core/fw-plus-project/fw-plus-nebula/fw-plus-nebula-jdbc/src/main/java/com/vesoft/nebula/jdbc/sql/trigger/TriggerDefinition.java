package com.vesoft.nebula.jdbc.sql.trigger;

public class TriggerDefinition {
	private final String schemaName;
	private final String triggerName;
	private final String targetTable;
	private final TriggerEvent event;
	private final TriggerTiming timing;
	private final String bodySql;
	private final long order;
	private final boolean enabled;

	public TriggerDefinition(String schemaName, String triggerName, String targetTable, TriggerEvent event,
			TriggerTiming timing, String bodySql, long order, boolean enabled) {
		this.schemaName = requireText(schemaName, "Schema name");
		this.triggerName = requireText(triggerName, "Trigger name");
		this.targetTable = requireText(targetTable, "Target table");
		if (event == null) {
			throw new IllegalArgumentException("Trigger event must not be null.");
		}
		if (timing == null) {
			throw new IllegalArgumentException("Trigger timing must not be null.");
		}
		this.event = event;
		this.timing = timing;
		this.bodySql = requireText(bodySql, "Trigger body SQL");
		this.order = order;
		this.enabled = enabled;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public String getTriggerName() {
		return triggerName;
	}

	public String getTargetTable() {
		return targetTable;
	}

	public TriggerEvent getEvent() {
		return event;
	}

	public TriggerTiming getTiming() {
		return timing;
	}

	public String getBodySql() {
		return bodySql;
	}

	public long getOrder() {
		return order;
	}

	public boolean isEnabled() {
		return enabled;
	}

	private static String requireText(String value, String label) {
		if (value == null || value.trim().length() == 0) {
			throw new IllegalArgumentException(label + " must not be blank.");
		}
		return value.trim();
	}
}
