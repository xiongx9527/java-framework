package com.vesoft.nebula.jdbc.sql.trigger;

import com.vesoft.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationResult;

import java.sql.SQLException;

public interface TriggerBodyExecutor {
	NebulaMutationResult executeBody(TriggerDefinition definition, NebulaMutationPlan parentPlan,
			TriggerExecutionContext context) throws SQLException;
}
