package com.vesoft.nebula.jdbc.sql.routine;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RoutineStatement {

	public enum Kind {
		CREATE_PROCEDURE,
		DROP_PROCEDURE,
		CALL_PROCEDURE
	}

	private final Kind kind;
	private final String name;
	private final String normalizedName;
	private final boolean ifExists;
	private final List<RoutineParameter> parameters;
	private final List<String> bodyStatements;
	private final List<Object> arguments;
	private final String originalSql;

	public RoutineStatement(Kind kind, String name, boolean ifExists,
			List<RoutineParameter> parameters, List<String> bodyStatements,
			List<Object> arguments, String originalSql) {
		this.kind = kind;
		this.name = name;
		this.normalizedName = RoutineNames.normalize(name);
		this.ifExists = ifExists;
		this.parameters = copy(parameters);
		this.bodyStatements = copy(bodyStatements);
		this.arguments = copy(arguments);
		this.originalSql = originalSql;
	}

	public Kind getKind() {
		return kind;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public boolean isIfExists() {
		return ifExists;
	}

	public List<RoutineParameter> getParameters() {
		return parameters;
	}

	public List<String> getBodyStatements() {
		return bodyStatements;
	}

	public List<Object> getArguments() {
		return arguments;
	}

	public String getOriginalSql() {
		return originalSql;
	}

	private static <T> List<T> copy(List<T> values) {
		if (values == null || values.isEmpty()) {
			return Collections.emptyList();
		}
		return Collections.unmodifiableList(new ArrayList<T>(values));
	}
}
