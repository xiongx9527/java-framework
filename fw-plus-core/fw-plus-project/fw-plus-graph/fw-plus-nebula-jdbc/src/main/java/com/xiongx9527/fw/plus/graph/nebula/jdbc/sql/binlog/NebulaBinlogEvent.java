package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class NebulaBinlogEvent {
	private final BinlogPosition position;
	private final String eventId;
	private final NebulaBinlogEventType eventType;
	private final String source;
	private final String sql;
	private final String ngql;
	private final String schemaName;
	private final Map<String, Object> payload;
	private final long createdAt;

	public NebulaBinlogEvent(BinlogPosition position, NebulaBinlogEventDraft draft) {
		if (position == null) {
			throw new IllegalArgumentException("Position must not be null.");
		}
		if (draft == null) {
			throw new IllegalArgumentException("Event draft must not be null.");
		}
		this.position = position;
		this.eventId = draft.getEventId();
		this.eventType = draft.getEventType();
		this.source = draft.getSource();
		this.sql = draft.getSql();
		this.ngql = draft.getNgql();
		this.schemaName = draft.getSchemaName();
		this.payload = copy(draft.getPayload());
		this.createdAt = draft.getCreatedAt();
	}

	public BinlogPosition getPosition() {
		return position;
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
}
