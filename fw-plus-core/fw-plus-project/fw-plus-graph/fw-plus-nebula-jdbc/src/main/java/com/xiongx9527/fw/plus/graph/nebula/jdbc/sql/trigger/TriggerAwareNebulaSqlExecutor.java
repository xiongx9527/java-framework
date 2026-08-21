package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.DirectNebulaSqlExecutor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;

public class TriggerAwareNebulaSqlExecutor extends DirectNebulaSqlExecutor {

	public TriggerAwareNebulaSqlExecutor() {
		super();
	}

	public TriggerAwareNebulaSqlExecutor(NebulaSchemaMappingRegistry schemaMapping) {
		super(schemaMapping);
	}
}
