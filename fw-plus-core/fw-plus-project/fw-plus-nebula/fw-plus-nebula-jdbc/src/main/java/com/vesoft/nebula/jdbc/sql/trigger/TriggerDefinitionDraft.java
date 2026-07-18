package com.vesoft.nebula.jdbc.sql.trigger;

public class TriggerDefinitionDraft {
	private final String schemaName;
	private final String triggerName;
	private final String targetTable;
	private final TriggerEvent event;
	private final TriggerTiming timing;
	private final String bodySql;

	public TriggerDefinitionDraft(String schemaName, String triggerName, String targetTable, TriggerEvent event,
			TriggerTiming timing, String bodySql) {
		this.schemaName = schemaName;
		this.triggerName = triggerName;
		this.targetTable = targetTable;
		this.event = event;
		this.timing = timing;
		this.bodySql = bodySql;
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

	public TriggerDefinition toDefinition(long order) {
		return new TriggerDefinition(schemaName, triggerName, targetTable, event, timing, bodySql, order, true);
	}
}
