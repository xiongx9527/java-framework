package com.vesoft.nebula.jdbc.sql.write;

import com.vesoft.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;

import java.sql.SQLException;

public interface NebulaSqlDdlConverter {
	NebulaDdlPlan convert(NebulaSqlStatement statement, NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
