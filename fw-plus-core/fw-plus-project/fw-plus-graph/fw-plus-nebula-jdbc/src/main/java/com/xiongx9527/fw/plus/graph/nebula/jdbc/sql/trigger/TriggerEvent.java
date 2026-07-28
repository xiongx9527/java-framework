package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

public enum TriggerEvent {
	INSERT,
	UPDATE,
	DELETE;

	public static TriggerEvent fromSql(String value) {
		if (value != null) {
			for (TriggerEvent event : values()) {
				if (event.name().equalsIgnoreCase(value.trim())) {
					return event;
				}
			}
		}
		throw new IllegalArgumentException("Unsupported trigger event: " + value);
	}

	public boolean matches(TriggerEvent event) {
		return this == event;
	}
}
