package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import java.sql.SQLException;

public interface NebulaSqlMutationExecutor {
	NebulaMutationResult executeMutation(NebulaMutationPlan plan) throws SQLException;

	NebulaMutationResult executeDdl(NebulaDdlPlan plan) throws SQLException;
}
