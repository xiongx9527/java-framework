package com.vesoft.nebula.jdbc.sql;

import com.vesoft.nebula.jdbc.NebulaConnection;
import com.vesoft.nebula.jdbc.NebulaResultSet;
import com.vesoft.nebula.jdbc.statement.NebulaStatementClassifier;
import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.vesoft.nebula.jdbc.sql.parser.StatementKind;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.trigger.InMemoryTriggerMetadataStore;
import com.vesoft.nebula.jdbc.sql.trigger.SqlTriggerBodyExecutor;
import com.vesoft.nebula.jdbc.sql.trigger.TriggerAwareMutationExecutor;
import com.vesoft.nebula.jdbc.sql.trigger.TriggerDdlHandler;
import com.vesoft.nebula.jdbc.sql.trigger.TriggerExecutionContext;
import com.vesoft.nebula.jdbc.sql.trigger.TriggerMetadataService;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlDdlConverter;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlWriteConverter;
import com.vesoft.nebula.jdbc.sql.write.DirectNebulaSqlMutationExecutor;
import com.vesoft.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationResult;
import com.vesoft.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteBindings;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteConversionException;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteErrorCode;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class DirectNebulaSqlExecutor implements NebulaSqlExecutor {
	private static final String DEFAULT_SCHEMA_NAME = "default";
	private static final String TRIGGER_MAX_DEPTH_PROPERTY = "nebula.jdbc.sql.trigger.maxDepth";
	private final NebulaSchemaMappingRegistry schemaMapping;
	private final TriggerMetadataService triggerMetadataService;
	private final int triggerMaxDepth;
	private final DruidNebulaSqlParser parser = new DruidNebulaSqlParser();
	private final DefaultNebulaSqlWriteConverter writeConverter = new DefaultNebulaSqlWriteConverter();
	private final DefaultNebulaSqlDdlConverter ddlConverter = new DefaultNebulaSqlDdlConverter();

	public DirectNebulaSqlExecutor() {
		this(NebulaSchemaMappingRegistry.builder().build());
	}

	public DirectNebulaSqlExecutor(NebulaSchemaMappingRegistry schemaMapping) {
		this(schemaMapping, new TriggerMetadataService(new InMemoryTriggerMetadataStore()), configuredTriggerMaxDepth());
	}

	public DirectNebulaSqlExecutor(NebulaSchemaMappingRegistry schemaMapping,
			TriggerMetadataService triggerMetadataService, int triggerMaxDepth) {
		this.schemaMapping = schemaMapping == null ? NebulaSchemaMappingRegistry.builder().build() : schemaMapping;
		if (triggerMetadataService == null) {
			throw new IllegalArgumentException("Trigger metadata service must not be null.");
		}
		if (triggerMaxDepth < 1) {
			throw new IllegalArgumentException("Trigger max depth must be greater than zero.");
		}
		this.triggerMetadataService = triggerMetadataService;
		this.triggerMaxDepth = triggerMaxDepth;
	}

	@Override
	public ResultSet executeQuery(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		NebulaStatementClassifier.checkQuery(sql);
		return new NebulaResultSet(executeNative(connection, renderSql(sql, bindings)), null);
	}

	@Override
	public int executeUpdate(NebulaConnection connection, String sql, NebulaParameterBindings bindings) throws SQLException {
		String renderedSql = renderSql(sql, bindings);
		TriggerDdlHandler triggerDdlHandler = new TriggerDdlHandler(triggerMetadataService);
		if (triggerDdlHandler.supports(renderedSql)) {
			triggerDdlHandler.handle(schemaName(connection), renderedSql);
			return 0;
		}
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
			NebulaMutationResult result = mutationExecutor(connection).executeMutation(plan);
			return result.getAffectedRows();
		}
		if (parseResult != null && isDdlStatement(parseResult.getKind())) {
			NebulaDdlPlan plan = ddlConverter.convert(parseResult.getStatement(), schemaMapping);
			NebulaMutationResult result = new DirectNebulaSqlMutationExecutor(connection).executeDdl(plan);
			return result.getAffectedRows();
		}
		NebulaStatementClassifier.checkUpdate(sql);
		executeNative(connection, renderedSql);
		return 0;
	}

	private NebulaSqlMutationExecutor mutationExecutor(NebulaConnection connection) throws SQLException {
		String schemaName = schemaName(connection);
		NebulaSqlMutationExecutor delegate = new DirectNebulaSqlMutationExecutor(connection);
		SqlTriggerBodyExecutor bodyExecutor = new SqlTriggerBodyExecutor(schemaName, triggerMetadataService,
				parser, writeConverter, schemaMapping, delegate, triggerMaxDepth);
		return new TriggerAwareMutationExecutor(schemaName, triggerMetadataService, bodyExecutor, delegate, triggerMaxDepth);
	}

	private String schemaName(NebulaConnection connection) throws SQLException {
		if (connection == null) {
			return DEFAULT_SCHEMA_NAME;
		}
		String schemaName = connection.getSchema();
		if (schemaName == null || schemaName.trim().length() == 0) {
			return DEFAULT_SCHEMA_NAME;
		}
		return schemaName.trim();
	}

	private static int configuredTriggerMaxDepth() {
		return Integer.getInteger(TRIGGER_MAX_DEPTH_PROPERTY, TriggerExecutionContext.DEFAULT_MAX_DEPTH).intValue();
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
