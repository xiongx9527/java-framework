package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query;

import java.sql.SQLException;

public interface NebulaSqlQueryConverter {
	NgqlQueryPlan convert(NebulaQueryStatement statement,
						  NebulaQueryBindings bindings,
						  NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
