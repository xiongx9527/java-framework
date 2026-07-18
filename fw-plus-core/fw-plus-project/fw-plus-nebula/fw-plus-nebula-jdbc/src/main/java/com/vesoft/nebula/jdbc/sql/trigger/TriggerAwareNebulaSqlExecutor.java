package com.vesoft.nebula.jdbc.sql.trigger;

import com.vesoft.nebula.jdbc.sql.DirectNebulaSqlExecutor;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;

public class TriggerAwareNebulaSqlExecutor extends DirectNebulaSqlExecutor {

	public TriggerAwareNebulaSqlExecutor() {
		super();
	}

	public TriggerAwareNebulaSqlExecutor(NebulaSchemaMappingRegistry schemaMapping) {
		super(schemaMapping);
	}
}
