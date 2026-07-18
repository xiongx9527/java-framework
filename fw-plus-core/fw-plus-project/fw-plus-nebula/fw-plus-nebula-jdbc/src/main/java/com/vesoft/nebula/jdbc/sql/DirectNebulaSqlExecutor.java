package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;
import com.vesoft.nebula.jdbc.statement.NebulaStatementClassifier;
import com.vesoft.nebula.jdbc.sql.binlog.BinlogEventStore;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventDraft;
import com.vesoft.nebula.jdbc.sql.binlog.NebulaBinlogEventFactory;
import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.vesoft.nebula.jdbc.sql.parser.StatementKind;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlDdlConverter;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlWriteConverter;
import com.vesoft.nebula.jdbc.sql.write.DirectNebulaSqlMutationExecutor;
import com.vesoft.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationResult;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteBindings;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteConversionException;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteErrorCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class DirectNebulaSqlExecutor implements NebulaSqlExecutor {
	private final NebulaSchemaMappingRegistry schemaMapping;
	private final BinlogEventStore binlogEventStore;
	private final DruidNebulaSqlParser parser = new DruidNebulaSqlParser();
	private final DefaultNebulaSqlWriteConverter writeConverter = new DefaultNebulaSqlWriteConverter();
	private final DefaultNebulaSqlDdlConverter ddlConverter = new DefaultNebulaSqlDdlConverter();

	public DirectNebulaSqlExecutor() {
		this(NebulaSchemaMappingRegistry.builder().build());
	}

	public DirectNebulaSqlExecutor(NebulaSchemaMappingRegistry schemaMapping) {
		this(schemaMapping, null);
	}

	public DirectNebulaSqlExecutor(NebulaSchemaMappingRegistry schemaMapping, BinlogEventStore binlogEventStore) {
		this.schemaMapping = schemaMapping == null ? NebulaSchemaMappingRegistry.builder().build() : schemaMapping;
		this.binlogEventStore = binlogEventStore;
	}

	@Override
	public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		NebulaStatementClassifier.checkQuery(sql);
		return new NebulaResultSet(executeNative(connection, renderSql(sql, bindings)), null);
	}

	@Override
	public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		String renderedSql = renderSql(sql, bindings);
		if (isTransactionStatement(renderedSql)) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_TRANSACTION_UNSUPPORTED,
					"SQL transactions are not supported by Nebula write/DDL executor.");
		}
		if (isUnsupportedSqlDdl(renderedSql)) {
			throw NebulaWriteConversionException.of(NebulaWriteErrorCode.NEBULA_SQL_DDL_UNSUPPORTED_OPTION,
					"Complex SQL DDL is not supported by Nebula write/DDL executor.");
		}
		NebulaSqlParseResult parseResult = tryParse(renderedSql);
		if (parseResult != null && isWriteStatement(parseResult.getKind())) {
			NebulaMutationPlan plan = writeConverter.convert(
					parseResult.getStatement(),
					new NebulaWriteBindings(renderedSql, bindings),
					schemaMapping);
			NebulaMutationResult result = new DirectNebulaSqlMutationExecutor(connection).executeMutation(plan);
			appendMutationEvent(result);
			return result.getAffectedRows();
		}
		if (parseResult != null && isDdlStatement(parseResult.getKind())) {
			NebulaDdlPlan plan = ddlConverter.convert(parseResult.getStatement(), schemaMapping);
			NebulaMutationResult result = new DirectNebulaSqlMutationExecutor(connection).executeDdl(plan);
			appendDdlEvent(result);
			return result.getAffectedRows();
		}
		NebulaStatementClassifier.checkUpdate(sql);
		executeNative(connection, renderedSql);
		return 0;
	}

	private void appendMutationEvent(NebulaMutationResult result) throws SQLException {
		if (binlogEventStore == null || result == null || !result.isSuccess() || result.getMutationDescriptor() == null) {
			return;
		}
		appendEvent(NebulaBinlogEventFactory.forMutation(
				result.getMutationDescriptor(),
				"nebula-jdbc",
				result.getTraceId(),
				System.currentTimeMillis()
		));
	}

	private void appendDdlEvent(NebulaMutationResult result) throws SQLException {
		if (binlogEventStore == null || result == null || !result.isSuccess() || result.getDdlDescriptor() == null) {
			return;
		}
		appendEvent(NebulaBinlogEventFactory.forDdl(
				result.getDdlDescriptor(),
				"nebula-jdbc",
				result.getTraceId(),
				System.currentTimeMillis()
		));
	}

	private void appendEvent(NebulaBinlogEventDraft event) throws SQLException {
		try {
			binlogEventStore.append(event);
		} catch (SQLException exception) {
			throw new SQLException("binlog-compatible stream append failed: " + exception.getMessage(), exception);
		}
	}

	private NebulaSqlParseResult tryParse(String sql) throws SQLException {
		try {
			return parser.parse(sql);
		} catch (NebulaSqlParseException exception) {
			return null;
		}
	}

	private boolean isWriteStatement(StatementKind kind) {
		return kind == StatementKind.INSERT || kind == StatementKind.UPDATE || kind == StatementKind.DELETE;
	}

	private boolean isDdlStatement(StatementKind kind) {
		return kind == StatementKind.CREATE_SCHEMA || kind == StatementKind.DROP_SCHEMA || kind == StatementKind.METADATA;
	}

	private boolean isTransactionStatement(String sql) {
		String normalized = sql == null ? "" : sql.trim().toUpperCase();
		return "BEGIN".equals(normalized) || "START TRANSACTION".equals(normalized)
				|| "COMMIT".equals(normalized) || "ROLLBACK".equals(normalized);
	}

	private boolean isUnsupportedSqlDdl(String sql) {
		String normalized = sql == null ? "" : sql.trim().toUpperCase();
		return normalized.startsWith("ALTER TABLE ")
				|| normalized.startsWith("CREATE INDEX ")
				|| normalized.startsWith("DROP INDEX ")
				|| normalized.startsWith("CREATE UNIQUE INDEX ");
	}

	private com.vesoft.nebula.client.graph.data.ResultSet executeNative(NebulaConnection connection, String sql)
			throws SQLException {
		com.vesoft.nebula.client.graph.data.ResultSet resultSet = connection.execute(sql);
		if (!resultSet.isSucceeded()) {
			throw new SQLException(String.format(
					"nGql \"%s\" executed failed, error code: %d, error message: %s",
					sql,
					resultSet.getErrorCode(),
					resultSet.getErrorMessage()
			));
		}
		return resultSet;
	}

	private String renderSql(String sql, NebulaParameterBindings bindings) throws SQLException {
		if (bindings == null || bindings.size() == 0) {
			return sql;
		}
		bindings.validateComplete();
		StringBuilder rendered = new StringBuilder(sql.length());
		boolean singleQuoted = false;
		boolean doubleQuoted = false;
		boolean backtickQuoted = false;
		boolean lineComment = false;
		boolean blockComment = false;
		int parameterIndex = 1;
		for (int i = 0; i < sql.length(); i++) {
			char current = sql.charAt(i);
			char next = i + 1 < sql.length() ? sql.charAt(i + 1) : '\0';

			if (lineComment) {
				rendered.append(current);
				if (current == '\n' || current == '\r') {
					lineComment = false;
				}
				continue;
			}
			if (blockComment) {
				rendered.append(current);
				if (current == '*' && next == '/') {
					rendered.append(next);
					blockComment = false;
					i++;
				}
				continue;
			}
			if (singleQuoted) {
				rendered.append(current);
				if (current == '\\' && next != '\0') {
					rendered.append(next);
					i++;
				} else if (current == '\'' && next == '\'') {
					rendered.append(next);
					i++;
				} else if (current == '\'') {
					singleQuoted = false;
				}
				continue;
			}
			if (doubleQuoted) {
				rendered.append(current);
				if (current == '\\' && next != '\0') {
					rendered.append(next);
					i++;
				} else if (current == '"' && next == '"') {
					rendered.append(next);
					i++;
				} else if (current == '"') {
					doubleQuoted = false;
				}
				continue;
			}
			if (backtickQuoted) {
				rendered.append(current);
				if (current == '`') {
					backtickQuoted = false;
				}
				continue;
			}

			if (current == '-' && next == '-') {
				rendered.append(current).append(next);
				lineComment = true;
				i++;
			} else if (current == '/' && next == '*') {
				rendered.append(current).append(next);
				blockComment = true;
				i++;
			} else if (current == '\'') {
				rendered.append(current);
				singleQuoted = true;
			} else if (current == '"') {
				rendered.append(current);
				doubleQuoted = true;
			} else if (current == '`') {
				rendered.append(current);
				backtickQuoted = true;
			} else if (current == '?') {
				if (parameterIndex > bindings.size()) {
					throw new SQLException("More parameter markers than bound parameters.");
				}
				rendered.append(toLiteral(bindings.get(parameterIndex).getValue()));
				parameterIndex++;
			} else {
				rendered.append(current);
			}
		}
		if (parameterIndex <= bindings.size()) {
			throw new SQLException("Fewer parameter markers than bound parameters.");
		}
		return rendered.toString();
	}

	private String toLiteral(Object value) {
		if (value == null) {
			return "NULL";
		}
		if (value instanceof Number || value instanceof Boolean) {
			return String.valueOf(value);
		}
		if (value instanceof java.sql.Date) {
			return "date(\"" + value + "\")";
		}
		if (value instanceof Time) {
			return "time(\"" + value + "\")";
		}
		if (value instanceof Timestamp) {
			return "datetime(\"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(value) + "\")";
		}
		if (value instanceof java.util.Date) {
			return "datetime(\"" + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS").format(value) + "\")";
		}
		return "\"" + escapeString(String.valueOf(value)) + "\"";
	}

	private String escapeString(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}
}
