package com.vesoft.nebula.jdbc.sql.routine;

import java.util.Locale;

public class RoutineName {

	private final String schema;
	private final String normalizedSchema;
	private final String name;
	private final String normalizedName;

	private RoutineName(String schema, String name) {
		this.schema = schema == null ? "" : schema;
		this.normalizedSchema = this.schema.toLowerCase(Locale.ROOT);
		this.name = RoutineNames.displayName(name);
		this.normalizedName = RoutineNames.normalize(name);
	}

	public static RoutineName of(String schema, String name) {
		return new RoutineName(schema, name);
	}

	public String getSchema() {
		return schema;
	}

	public String getNormalizedSchema() {
		return normalizedSchema;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public String key() {
		return normalizedSchema + "." + normalizedName;
	}
}
