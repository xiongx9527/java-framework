package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.routine;

import java.util.Locale;

final class RoutineNames {

	private RoutineNames() {
	}

	static String displayName(String rawName) {
		String name = rawName == null ? "" : rawName.trim();
		int dot = name.lastIndexOf('.');
		if (dot >= 0) {
			name = name.substring(dot + 1).trim();
		}
		if (name.startsWith("`") && name.endsWith("`") && name.length() >= 2) {
			return name.substring(1, name.length() - 1);
		}
		return name;
	}

	static String normalize(String rawName) {
		return displayName(rawName).toLowerCase(Locale.ROOT);
	}
}
