package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.NebulaConnection;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.NebulaSqlExecutor;

import java.sql.SQLException;

public class RoutineService {

	private static final int DEFAULT_MAX_STATEMENTS = 64;
	private static final long DEFAULT_MAX_EXECUTION_MILLIS = 30000L;
	private static final InMemoryRoutineMetadataRepository DEFAULT_REPOSITORY =
			new InMemoryRoutineMetadataRepository();
	private static final RoutineService DEFAULT = new RoutineService(DEFAULT_REPOSITORY);

	private final RoutineMetadataRepository repository;
	private final int maxStatements;
	private final long maxExecutionMillis;
	private final RoutinePermissionChecker permissionChecker;
	private final RoutineInterpreter interpreter = new RoutineInterpreter();

	public RoutineService(RoutineMetadataRepository repository) {
		this(repository, DEFAULT_MAX_STATEMENTS);
	}

	public RoutineService(RoutineMetadataRepository repository, int maxStatements) {
		this(repository, maxStatements, DEFAULT_MAX_EXECUTION_MILLIS);
	}

	public RoutineService(RoutineMetadataRepository repository, int maxStatements, long maxExecutionMillis) {
		this(repository, maxStatements, maxExecutionMillis, RoutinePermissionChecker.allowAll());
	}

	public RoutineService(RoutineMetadataRepository repository, int maxStatements,
			long maxExecutionMillis, RoutinePermissionChecker permissionChecker) {
		this.repository = repository;
		this.maxStatements = maxStatements;
		this.maxExecutionMillis = maxExecutionMillis;
		this.permissionChecker = permissionChecker == null
				? RoutinePermissionChecker.allowAll() : permissionChecker;
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
		permissionChecker.check(schema, owner, statement.getName(), RoutinePermissionChecker.Action.MANAGE);
		RoutineName name = RoutineName.of(schema, statement.getName());
		if (repository.find(name) != null) {
			throw RoutineException.alreadyExists(statement.getName());
		}
		repository.save(RoutineDefinition.fromStatement(schema, owner, statement));
		return 0;
	}

	public int drop(String schema, String user, String sql) throws SQLException {
		RoutineStatement statement = RoutineStatementParser.parse(sql);
		if (statement.getKind() != RoutineStatement.Kind.DROP_PROCEDURE) {
			throw RoutineException.syntax("Expected DROP PROCEDURE");
		}
		permissionChecker.check(schema, user, statement.getName(), RoutinePermissionChecker.Action.MANAGE);
		RoutineName name = RoutineName.of(schema, statement.getName());
		boolean dropped = repository.drop(name);
		if (!dropped && !statement.isIfExists()) {
			throw RoutineException.notFound(statement.getName());
		}
		return 0;
	}

	public RoutineCallResult call(String schema, String user, String sql,
			NebulaConnection connection, NebulaSqlExecutor executor) throws SQLException {
		RoutineStatement statement = RoutineStatementParser.parse(sql);
		if (statement.getKind() != RoutineStatement.Kind.CALL_PROCEDURE) {
			throw RoutineException.syntax("Expected CALL");
		}
		RoutineDefinition definition = repository.find(RoutineName.of(schema, statement.getName()));
		if (definition == null) {
			throw RoutineException.notFound(statement.getName());
		}
		permissionChecker.check(schema, user, statement.getName(), RoutinePermissionChecker.Action.EXECUTE);
		return interpreter.execute(definition, statement,
				new RoutineExecutionContext(schema, user, connection, executor,
						maxStatements, maxExecutionMillis));
	}
}
