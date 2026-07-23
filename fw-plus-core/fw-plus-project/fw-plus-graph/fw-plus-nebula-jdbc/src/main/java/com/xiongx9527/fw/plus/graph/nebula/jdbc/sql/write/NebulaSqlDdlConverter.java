package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;

import java.sql.SQLException;

public interface NebulaSqlDdlConverter {
	NebulaDdlPlan convert(NebulaSqlStatement statement, NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
