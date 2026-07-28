package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.ast.NebulaSqlStatement;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;

import java.sql.SQLException;

public interface NebulaSqlWriteConverter {
	NebulaMutationPlan convert(NebulaSqlStatement statement, NebulaWriteBindings bindings,
			NebulaSchemaMappingRegistry schemaMapping) throws SQLException;
}
