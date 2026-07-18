package com.vesoft.nebula.jdbc.sql.binlog;

import com.vesoft.nebula.jdbc.sql.write.NebulaDdlDescriptor;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationDescriptor;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationKind;

import java.util.LinkedHashMap;
import java.util.Map;

public final class NebulaBinlogEventFactory {
	private NebulaBinlogEventFactory() {
	}

	public static NebulaBinlogEventDraft forMutation(NebulaMutationDescriptor descriptor, String source,
			String traceId, long createdAt) {
		if (descriptor == null) {
			throw new IllegalArgumentException("Mutation descriptor must not be null.");
		}
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("mutationKind", descriptor.getMutationKind().name());
		payload.put("targetName", descriptor.getTargetName());
		payload.put("keys", descriptor.getKeys());
		payload.put("properties", descriptor.getProperties());
		payload.put("beforeImageAvailable", Boolean.FALSE);
		payload.put("traceId", traceId);
		return new NebulaBinlogEventDraft(
				eventId(traceId, descriptor.getMutationKind().name(), descriptor.getTargetName(), createdAt),
				toEventType(descriptor.getMutationKind()),
				source,
				descriptor.getSourceSql(),
				descriptor.getNgql(),
				descriptor.getTargetName(),
				payload,
				createdAt
		);
	}

	public static NebulaBinlogEventDraft forDdl(NebulaDdlDescriptor descriptor, String source, String traceId,
			long createdAt) {
		if (descriptor == null) {
			throw new IllegalArgumentException("DDL descriptor must not be null.");
		}
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("ddlKind", descriptor.getKind().name());
		payload.put("schemaName", descriptor.getSchemaName());
		payload.put("columns", descriptor.getColumns());
		payload.put("traceId", traceId);
		return new NebulaBinlogEventDraft(
				eventId(traceId, descriptor.getKind().name(), descriptor.getSchemaName(), createdAt),
				NebulaBinlogEventType.SCHEMA_CHANGE,
				source,
				descriptor.getSourceSql(),
				descriptor.getNgql(),
				descriptor.getSchemaName(),
				payload,
				createdAt
		);
	}

	private static NebulaBinlogEventType toEventType(NebulaMutationKind kind) {
		if (kind == NebulaMutationKind.INSERT_VERTEX || kind == NebulaMutationKind.INSERT_EDGE) {
			return NebulaBinlogEventType.WRITE_ROWS;
		}
		if (kind == NebulaMutationKind.UPDATE_VERTEX || kind == NebulaMutationKind.UPDATE_EDGE) {
			return NebulaBinlogEventType.UPDATE_ROWS;
		}
		if (kind == NebulaMutationKind.DELETE_VERTEX || kind == NebulaMutationKind.DELETE_EDGE) {
			return NebulaBinlogEventType.DELETE_ROWS;
		}
		throw new IllegalArgumentException("Unsupported mutation kind: " + kind);
	}

	private static String eventId(String traceId, String kind, String targetName, long createdAt) {
		String trace = isBlank(traceId) ? "no-trace" : traceId.trim();
		return trace + "-" + kind + "-" + targetName + "-" + createdAt;
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
