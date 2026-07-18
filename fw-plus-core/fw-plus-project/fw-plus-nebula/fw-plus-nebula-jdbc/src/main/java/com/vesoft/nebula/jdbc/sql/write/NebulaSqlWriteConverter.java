package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;

import java.sql.SQLException;

public interface NebulaSqlWriteConverter {
	NebulaMutationPlan convert(NebulaSqlStatement statement, NebulaWriteBindings bindings,
			NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
