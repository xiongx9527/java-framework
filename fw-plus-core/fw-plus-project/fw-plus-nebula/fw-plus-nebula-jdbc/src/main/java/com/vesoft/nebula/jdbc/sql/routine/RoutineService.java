package com.vesoft.nebula.jdbc.sql.routine;

import java.sql.SQLException;

public class RoutineService {

	private static final InMemoryRoutineMetadataRepository DEFAULT_REPOSITORY =
			new InMemoryRoutineMetadataRepository();
	private static final RoutineService DEFAULT = new RoutineService(DEFAULT_REPOSITORY);

	private final RoutineMetadataRepository repository;

	public RoutineService(RoutineMetadataRepository repository) {
		this.repository = repository;
	}

	public static RoutineService getDefault() {
		return DEFAULT;
	}

	public static void resetDefaultRepository() {
		DEFAULT_REPOSITORY.clear();
	}

	public int create(String schema, String owner, String sql) throws SQLException {
		RoutineStatement statement = RoutineStatementParser.parse(sql);
		if (statement.getKind() != RoutineStatement.Kind.CREATE_PROCEDURE) {
			throw RoutineException.syntax("Expected CREATE PROCEDURE");
		}
		RoutineName name = RoutineName.of(schema, statement.getName());
		if (repository.find(name) != null) {
			throw RoutineException.alreadyExists(statement.getName());
		}
		repository.save(RoutineDefinition.fromStatement(schema, owner, statement));
		return 0;
	}

	public int drop(String schema, String sql) throws SQLException {
		RoutineStatement statement = RoutineStatementParser.parse(sql);
		if (statement.getKind() != RoutineStatement.Kind.DROP_PROCEDURE) {
			throw RoutineException.syntax("Expected DROP PROCEDURE");
		}
		RoutineName name = RoutineName.of(schema, statement.getName());
		boolean dropped = repository.drop(name);
		if (!dropped && !statement.isIfExists()) {
			throw RoutineException.notFound(statement.getName());
		}
		return 0;
	}
}
