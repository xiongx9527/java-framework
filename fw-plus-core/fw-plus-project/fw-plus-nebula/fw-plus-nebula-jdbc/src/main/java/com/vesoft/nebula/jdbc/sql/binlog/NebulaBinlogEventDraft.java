package com.vesoft.nebula.jdbc.sql.binlog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NebulaBinlogEventDraft {
	private final String eventId;
	private final NebulaBinlogEventType eventType;
	private final String source;
	private final String sql;
	private final String ngql;
	private final String schemaName;
	private final Map<String, Object> payload;
	private final long createdAt;

	public NebulaBinlogEventDraft(String eventId, NebulaBinlogEventType eventType, String source, String sql,
			String ngql, String schemaName, Map<String, Object> payload, long createdAt) {
		if (isBlank(eventId)) {
			throw new IllegalArgumentException("Event id must not be blank.");
		}
		if (eventType == null) {
			throw new IllegalArgumentException("Event type must not be null.");
		}
		this.eventId = eventId;
		this.eventType = eventType;
		this.source = source;
		this.sql = sql;
		this.ngql = ngql;
		this.schemaName = schemaName;
		this.payload = copy(payload);
		this.createdAt = createdAt;
	}

	public String getEventId() {
		return eventId;
	}

	public NebulaBinlogEventType getEventType() {
		return eventType;
	}

	public String getSource() {
		return source;
	}

	public String getSql() {
		return sql;
	}

	public String getNgql() {
		return ngql;
	}

	public String getSchemaName() {
		return schemaName;
	}

	public Map<String, Object> getPayload() {
		return payload;
	}

	public long getCreatedAt() {
		return createdAt;
	}

	private static Map<String, Object> copy(Map<String, Object> source) {
		if (source == null) {
			return Collections.emptyMap();
		}
		return Collections.unmodifiableMap(new LinkedHashMap<String, Object>(source));
	}

	private boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
