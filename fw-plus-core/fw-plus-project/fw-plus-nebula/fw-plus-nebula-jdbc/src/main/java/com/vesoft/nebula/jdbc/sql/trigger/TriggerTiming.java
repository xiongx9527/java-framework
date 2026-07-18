package com.vesoft.nebula.jdbc.sql.trigger;

public enum TriggerTiming {
	BEFORE,
	AFTER;

	public static TriggerTiming fromSql(String value) {
		if (value != null) {
			for (TriggerTiming timing : values()) {
				if (timing.name().equalsIgnoreCase(value.trim())) {
					return timing;
				}
			}
		}
		throw new IllegalArgumentException("Unsupported trigger timing: " + value);
	}
}
