package com.vesoft.nebula.jdbc.sql.trigger;

import java.sql.SQLException;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TriggerDdlHandler {
	private static final Pattern CREATE_TRIGGER = Pattern.compile(
			"^\\s*CREATE\\s+TRIGGER\\s+([A-Za-z_][A-Za-z0-9_$]*)\\s+"
					+ "(BEFORE|AFTER)\\s+(INSERT|UPDATE|DELETE)\\s+ON\\s+"
					+ "([A-Za-z_][A-Za-z0-9_$]*)\\s+FOR\\s+EACH\\s+ROW\\s+(.+?)\\s*;?\\s*$",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
	private static final Pattern DROP_TRIGGER = Pattern.compile(
			"^\\s*DROP\\s+TRIGGER\\s+(IF\\s+EXISTS\\s+)?([A-Za-z_][A-Za-z0-9_$]*)\\s*;?\\s*$",
			Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

	private final TriggerMetadataService metadataService;

	public TriggerDdlHandler(TriggerMetadataService metadataService) {
		if (metadataService == null) {
			throw new IllegalArgumentException("Trigger metadata service must not be null.");
		}
		this.metadataService = metadataService;
	}

	public boolean supports(String sql) {
		String normalized = normalize(sql);
		return normalized.startsWith("CREATE TRIGGER ") || normalized.startsWith("DROP TRIGGER ");
	}

	public TriggerDdlResult handle(String schemaName, String sql) throws SQLException {
		if (!supports(sql)) {
			throw syntaxError("Unsupported trigger DDL statement.");
		}
		Matcher createMatcher = CREATE_TRIGGER.matcher(sql);
		if (createMatcher.matches()) {
			return create(schemaName, createMatcher);
		}
		Matcher dropMatcher = DROP_TRIGGER.matcher(sql);
		if (dropMatcher.matches()) {
			boolean ifExists = dropMatcher.group(1) != null;
			return TriggerDdlResult.dropped(metadataService.drop(schemaName, dropMatcher.group(2), ifExists));
		}
		if (normalize(sql).startsWith("CREATE TRIGGER ")) {
			throw syntaxError("Unsupported CREATE TRIGGER syntax.");
		}
		throw syntaxError("Unsupported DROP TRIGGER syntax.");
	}

	private TriggerDdlResult create(String schemaName, Matcher matcher) throws SQLException {
		String bodySql = stripTrailingSemicolon(matcher.group(5).trim());
		validateBody(bodySql);
		TriggerDefinitionDraft draft = new TriggerDefinitionDraft(
				schemaName,
				matcher.group(1),
				matcher.group(4),
				TriggerEvent.fromSql(matcher.group(3)),
				TriggerTiming.fromSql(matcher.group(2)),
				bodySql);
		return TriggerDdlResult.created(metadataService.create(draft));
	}

	public void validateBody(String bodySql) throws SQLException {
		String normalized = normalize(bodySql);
		if (normalized.length() == 0) {
			throw syntaxError("Unsupported trigger body: body SQL must not be blank.");
		}
		if (normalized.startsWith("BEGIN ") || normalized.equals("BEGIN")
				|| normalized.startsWith("CALL ")
				|| normalized.startsWith("START TRANSACTION")
				|| normalized.startsWith("COMMIT")
				|| normalized.startsWith("ROLLBACK")
				|| normalized.startsWith("PREPARE ")
				|| normalized.startsWith("EXECUTE ")
				|| normalized.startsWith("CREATE TRIGGER ")
				|| normalized.startsWith("DROP TRIGGER ")) {
			throw syntaxError("Unsupported trigger body: " + firstKeyword(bodySql));
		}
	}

	private String stripTrailingSemicolon(String sql) {
		String trimmed = sql.trim();
		if (trimmed.endsWith(";")) {
			return trimmed.substring(0, trimmed.length() - 1).trim();
		}
		return trimmed;
	}

	private String normalize(String sql) {
		return sql == null ? "" : sql.trim().replaceAll("\\s+", " ").toUpperCase(Locale.ENGLISH);
	}

	private String firstKeyword(String sql) {
		String normalized = normalize(sql);
		int index = normalized.indexOf(' ');
		return index < 0 ? normalized : normalized.substring(0, index);
	}

	private SQLException syntaxError(String message) {
		return new SQLException("Nebula MySQL trigger subset DDL error: " + message, "42000");
	}
}
