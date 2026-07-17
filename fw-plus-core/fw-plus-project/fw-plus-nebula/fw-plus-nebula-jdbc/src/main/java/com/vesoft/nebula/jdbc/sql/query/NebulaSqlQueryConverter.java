package com.vesoft.nebula.jdbc.sql.query;

import java.sql.SQLException;

public interface NebulaSqlQueryConverter {
	NgqlQueryPlan convert(NebulaQueryStatement statement,
						  NebulaQueryBindings bindings,
						  NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
