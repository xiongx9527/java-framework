package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

import java.sql.SQLException;
import java.util.List;

public interface TriggerMetadataStore {
	TriggerDefinition create(TriggerDefinitionDraft draft) throws SQLException;

	TriggerDefinition findByName(String schemaName, String triggerName) throws SQLException;

	void save(TriggerDefinition definition) throws SQLException;

	boolean delete(String schemaName, String triggerName) throws SQLException;

	List<TriggerDefinition> findByTarget(String schemaName, String targetTable,
			TriggerEvent event, TriggerTiming timing) throws SQLException;

	long nextOrder(String schemaName) throws SQLException;
}
