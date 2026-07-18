package com.vesoft.nebula.jdbc.sql.routine;

import com.vesoft.nebula.jdbc.sql.NebulaParameterBindings;
import com.vesoft.nebula.jdbc.statement.NebulaStatementClassifier;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Locale;

public class RoutineInterpreter {

	public RoutineCallResult execute(RoutineDefinition definition, RoutineStatement call,
			RoutineExecutionContext context) throws SQLException {
		if (definition.getParameters().size() != call.getArguments().size()) {
			throw RoutineException.invalidArguments(definition.getName());
		}
		if (definition.getBodyStatements().size() > context.getMaxStatements()) {
			throw RoutineException.executionLimit("Routine execution statement limit exceeded");
		}

		ResultSet lastResultSet = null;
		int updateCount = 0;
		for (int i = 0; i < definition.getBodyStatements().size(); i++) {
			String sql = definition.getBodyStatements().get(i);
			if (startsWithCall(sql)) {
				throw RoutineException.unsupported("routine recursion or nested CALL");
			}
			if (isQuery(sql)) {
				lastResultSet = context.getExecutor().executeQuery(context.getConnection(),
						sql, NebulaParameterBindings.empty());
			}
			else {
				updateCount += context.getExecutor().executeUpdate(context.getConnection(),
						sql, NebulaParameterBindings.empty());
			}
		}
		if (lastResultSet != null) {
			return RoutineCallResult.resultSet(lastResultSet);
		}
		return RoutineCallResult.updateCount(updateCount);
	}

	private boolean isQuery(String sql) throws SQLException {
		try {
			NebulaStatementClassifier.checkQuery(sql);
			return true;
		}
		catch (SQLException queryException) {
			NebulaStatementClassifier.checkUpdate(sql);
			return false;
		}
	}

	private boolean startsWithCall(String sql) {
		return sql != null && sql.trim().toUpperCase(Locale.ROOT).startsWith("CALL ");
	}
}
