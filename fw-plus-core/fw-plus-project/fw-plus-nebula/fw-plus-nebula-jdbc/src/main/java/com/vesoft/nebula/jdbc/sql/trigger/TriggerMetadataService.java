package com.vesoft.nebula.jdbc.sql.trigger;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TriggerMetadataService {
	private final TriggerMetadataStore store;

	public TriggerMetadataService(TriggerMetadataStore store) {
		if (store == null) {
			throw new IllegalArgumentException("Trigger metadata store must not be null.");
		}
		this.store = store;
	}

	public TriggerDefinition create(TriggerDefinitionDraft draft) throws SQLException {
		if (draft == null) {
			throw new IllegalArgumentException("Trigger definition draft must not be null.");
		}
		TriggerDefinition definition = draft.toDefinition(store.nextOrder(draft.getSchemaName()));
		if (store.findByName(definition.getSchemaName(), definition.getTriggerName()) != null) {
			throw metadataError("Trigger already exists: " + definition.getTriggerName());
		}
		store.save(definition);
		return definition;
	}

	public TriggerDropResult drop(String schemaName, String triggerName, boolean ifExists) throws SQLException {
		boolean dropped = store.delete(schemaName, triggerName);
		if (!dropped && !ifExists) {
			throw metadataError("Trigger does not exist: " + triggerName);
		}
		return new TriggerDropResult(dropped);
	}

	public List<TriggerDefinition> findEnabled(String schemaName, String targetTable,
			TriggerEvent event, TriggerTiming timing) throws SQLException {
		List<TriggerDefinition> enabled = new ArrayList<TriggerDefinition>();
		for (TriggerDefinition definition : store.findByTarget(schemaName, targetTable, event, timing)) {
			if (definition.isEnabled()) {
				enabled.add(definition);
			}
		}
		return enabled;
	}

	private SQLException metadataError(String message) {
		return new SQLException("Nebula MySQL trigger subset metadata error: " + message, "42000");
	}
}
