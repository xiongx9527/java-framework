package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

public class RoutineParameter {

	private final String mode;
	private final String name;
	private final String normalizedName;
	private final String typeName;

	public RoutineParameter(String mode, String name, String typeName) {
		this.mode = mode;
		this.name = name;
		this.normalizedName = RoutineNames.normalize(name);
		this.typeName = typeName;
	}

	public String getMode() {
		return mode;
	}

	public String getName() {
		return name;
	}

	public String getNormalizedName() {
		return normalizedName;
	}

	public String getTypeName() {
		return typeName;
	}
}
