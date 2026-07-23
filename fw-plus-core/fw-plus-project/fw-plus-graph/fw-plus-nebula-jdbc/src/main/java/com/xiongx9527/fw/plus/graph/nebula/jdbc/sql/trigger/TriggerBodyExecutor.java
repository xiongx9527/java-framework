package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationResult;

import java.sql.SQLException;

public interface TriggerBodyExecutor {
	NebulaMutationResult executeBody(TriggerDefinition definition, NebulaMutationPlan parentPlan,
			TriggerExecutionContext context) throws SQLException;
}
