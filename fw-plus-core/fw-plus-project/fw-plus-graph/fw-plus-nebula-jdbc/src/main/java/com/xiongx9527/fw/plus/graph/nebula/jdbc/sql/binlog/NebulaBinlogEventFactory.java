package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.binlog;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaDdlDescriptor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationDescriptor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationKind;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public final class NebulaBinlogEventFactory {
	private NebulaBinlogEventFactory() {
	}

	public static NebulaBinlogEventDraft forMutation(NebulaMutationDescriptor descriptor, String source,
			String traceId, long createdAt) {
		return forMutation(descriptor, source, traceId, createdAt, NebulaBinlogEventOptions.defaults());
	}

	public static NebulaBinlogEventDraft forMutation(NebulaMutationDescriptor descriptor, String source,
			String traceId, long createdAt, NebulaBinlogEventOptions options) {
		if (descriptor == null) {
			throw new IllegalArgumentException("Mutation descriptor must not be null.");
		}
		NebulaBinlogEventOptions captureOptions = options == null ? NebulaBinlogEventOptions.defaults() : options;
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("mutationKind", descriptor.getMutationKind().name());
		payload.put("targetName", descriptor.getTargetName());
		payload.put("keys", payloadValue(descriptor.getKeys(), captureOptions));
		payload.put("properties", payloadValue(descriptor.getProperties(), captureOptions));
		payload.put("beforeImageAvailable", Boolean.FALSE);
		payload.put("traceId", traceId);
		return new NebulaBinlogEventDraft(
				eventId(traceId, descriptor.getMutationKind().name(), descriptor.getTargetName(), createdAt),
				toEventType(descriptor.getMutationKind()),
				source,
				statementText(descriptor.getSourceSql(), captureOptions),
				statementText(descriptor.getNgql(), captureOptions),
				descriptor.getTargetName(),
				payload,
				createdAt
		);
	}

	public static NebulaBinlogEventDraft forDdl(NebulaDdlDescriptor descriptor, String source, String traceId,
			long createdAt) {
		return forDdl(descriptor, source, traceId, createdAt, NebulaBinlogEventOptions.defaults());
	}

	public static NebulaBinlogEventDraft forDdl(NebulaDdlDescriptor descriptor, String source, String traceId,
			long createdAt, NebulaBinlogEventOptions options) {
		if (descriptor == null) {
			throw new IllegalArgumentException("DDL descriptor must not be null.");
		}
		NebulaBinlogEventOptions captureOptions = options == null ? NebulaBinlogEventOptions.defaults() : options;
		Map<String, Object> payload = new LinkedHashMap<String, Object>();
		payload.put("ddlKind", descriptor.getKind().name());
		payload.put("schemaName", descriptor.getSchemaName());
		payload.put("columns", descriptor.getColumns());
		payload.put("traceId", traceId);
		return new NebulaBinlogEventDraft(
				eventId(traceId, descriptor.getKind().name(), descriptor.getSchemaName(), createdAt),
				NebulaBinlogEventType.SCHEMA_CHANGE,
				source,
				statementText(descriptor.getSourceSql(), captureOptions),
				statementText(descriptor.getNgql(), captureOptions),
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

	private static Object payloadValue(Object value, NebulaBinlogEventOptions options) {
		if (!options.isCapturePayloadValues() && value instanceof Map) {
			Map<?, ?> values = (Map<?, ?>) value;
			Map<String, Object> redacted = new LinkedHashMap<String, Object>();
			for (Map.Entry<?, ?> entry : values.entrySet()) {
				redacted.put(String.valueOf(entry.getKey()), redactScalar(entry.getValue()));
			}
			return redacted;
		}
		if (!options.isCapturePayloadValues()) {
			return "<redacted>";
		}
		return value;
	}

	private static Object redactScalar(Object value) {
		if (value == null) {
			return null;
		}
		if (value instanceof Number) {
			return "<redacted:number>";
		}
		if (value instanceof Boolean) {
			return "<redacted:boolean>";
		}
		if (value instanceof Date) {
			return "<redacted:temporal>";
		}
		return "<redacted:string>";
	}

	private static String statementText(String value, NebulaBinlogEventOptions options) {
		if (!options.isCaptureStatementText()) {
			return null;
		}
		return truncate(redactQuotedLiterals(value), options.getMaxStatementLength());
	}

	private static String redactQuotedLiterals(String value) {
		if (value == null) {
			return null;
		}
		StringBuilder builder = new StringBuilder(value.length());
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean redactedCurrentLiteral = false;
		for (int i = 0; i < value.length(); i++) {
			char current = value.charAt(i);
			char next = i + 1 < value.length() ? value.charAt(i + 1) : '\0';
			if (singleQuoted) {
				if (!redactedCurrentLiteral) {
					builder.append("<redacted>");
					redactedCurrentLiteral = true;
				}
				if (current == '\\' && next != '\0') {
					i++;
				} else if (current == '\'' && next == '\'') {
					i++;
				} else if (current == '\'') {
					builder.append(current);
					singleQuoted = false;
					redactedCurrentLiteral = false;
				}
				continue;
			}
			if (doubleQuoted) {
				if (!redactedCurrentLiteral) {
					builder.append("<redacted>");
					redactedCurrentLiteral = true;
				}
				if (current == '\\' && next != '\0') {
					i++;
				} else if (current == '"' && next == '"') {
					i++;
				} else if (current == '"') {
					builder.append(current);
					doubleQuoted = false;
					redactedCurrentLiteral = false;
				}
				continue;
			}
			if (current == '\'') {
				builder.append(current);
				singleQuoted = true;
			} else if (current == '"') {
				builder.append(current);
				doubleQuoted = true;
			} else {
				builder.append(current);
			}
		}
		return builder.toString();
	}

	private static String truncate(String value, int maxLength) {
		if (value == null || maxLength == 0 || value.length() <= maxLength) {
			return maxLength == 0 ? null : value;
		}
		return value.substring(0, maxLength) + "...";
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
