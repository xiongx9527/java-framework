package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.mysql.session.MysqlProtocolSession;
import com.vesoft.nebula.jdbc.mysql.security.MysqlGatewayOperation;
import com.vesoft.nebula.jdbc.mysql.security.MysqlGatewaySecurityPolicy;
import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.StatementKind;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlDdlConverter;
import com.vesoft.nebula.jdbc.sql.write.DefaultNebulaSqlWriteConverter;
import com.vesoft.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.vesoft.nebula.jdbc.sql.write.NebulaMutationResult;
import com.vesoft.nebula.jdbc.sql.write.NebulaSqlDdlConverter;
import com.vesoft.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;
import com.vesoft.nebula.jdbc.sql.write.NebulaSqlWriteConverter;
import com.vesoft.nebula.jdbc.sql.write.NebulaWriteBindings;

import java.sql.SQLException;
import java.util.Locale;

public class AdvancedQueryHandler {
	private final ReadonlyQueryHandler readonlyHandler;
	private final MysqlMetadataHandler metadataHandler;
	private final NebulaSqlParser parser;
	private final NebulaSqlWriteConverter writeConverter;
	private final NebulaSqlDdlConverter ddlConverter;
	private final NebulaSqlMutationExecutor mutationExecutor;
	private final NebulaSchemaMappingRegistry schemaMapping;
	private final MysqlGatewaySecurityPolicy securityPolicy;

	public AdvancedQueryHandler(NebulaSqlExecutionPort executionPort, MysqlMetadataHandler metadataHandler) {
		this(executionPort, metadataHandler, new DruidNebulaSqlParser(),
				new DefaultNebulaSqlWriteConverter(), new DefaultNebulaSqlDdlConverter(),
				null, NebulaSchemaMappingRegistry.builder().build());
	}

	public AdvancedQueryHandler(NebulaSqlExecutionPort executionPort, MysqlMetadataHandler metadataHandler,
			NebulaSqlParser parser, NebulaSqlWriteConverter writeConverter, NebulaSqlDdlConverter ddlConverter,
			NebulaSqlMutationExecutor mutationExecutor, NebulaSchemaMappingRegistry schemaMapping) {
		this(executionPort, metadataHandler, parser, writeConverter, ddlConverter,
				mutationExecutor, schemaMapping, MysqlGatewaySecurityPolicy.allowAll());
	}

	public AdvancedQueryHandler(NebulaSqlExecutionPort executionPort, MysqlMetadataHandler metadataHandler,
			NebulaSqlParser parser, NebulaSqlWriteConverter writeConverter, NebulaSqlDdlConverter ddlConverter,
			NebulaSqlMutationExecutor mutationExecutor, NebulaSchemaMappingRegistry schemaMapping,
			MysqlGatewaySecurityPolicy securityPolicy) {
		this.readonlyHandler = new ReadonlyQueryHandler(executionPort, metadataHandler);
		this.metadataHandler = metadataHandler;
		this.parser = parser;
		this.writeConverter = writeConverter;
		this.ddlConverter = ddlConverter;
		this.mutationExecutor = mutationExecutor;
		this.schemaMapping = schemaMapping == null ? NebulaSchemaMappingRegistry.builder().build() : schemaMapping;
		this.securityPolicy = securityPolicy == null ? MysqlGatewaySecurityPolicy.allowAll() : securityPolicy;
	}

	public MysqlQueryResult handle(MysqlProtocolSession session, String sql) throws SQLException {
		String normalized = sql == null ? "" : sql.trim();
		checkTransport(session);
		if (isTransactionStatement(normalized)) {
			throw MysqlErrorMapper.transactionUnsupported();
		}
		if (isSessionSetStatement(normalized)) {
			checkAllowed(session, MysqlGatewayOperation.READ);
			return MysqlQueryResult.ok(0, 0);
		}
		if (isAdvancedMetadataSql(normalized)) {
			checkAllowed(session, MysqlGatewayOperation.METADATA);
			return routeAdvancedMetadata(session, normalized);
		}
		NebulaSqlParseResult parseResult = parse(normalized);
		if (parseResult == null) {
			checkAllowed(session, isMetadataSql(normalized) ? MysqlGatewayOperation.METADATA : MysqlGatewayOperation.READ);
			return readonlyHandler.handle(session, sql);
		}
		if (parseResult.getKind() == StatementKind.SELECT) {
			checkAllowed(session, MysqlGatewayOperation.READ);
			return readonlyHandler.handle(session, sql);
		}
		if (parseResult.getKind() == StatementKind.METADATA) {
			checkAllowed(session, MysqlGatewayOperation.METADATA);
			return readonlyHandler.handle(session, sql);
		}
		if (isWriteStatement(parseResult.getKind())) {
			checkAllowed(session, MysqlGatewayOperation.WRITE);
			NebulaMutationPlan plan = writeConverter.convert(
					parseResult.getStatement(),
					new NebulaWriteBindings(normalized, null),
					schemaMapping);
			NebulaMutationResult result = requireMutationExecutor().executeMutation(plan);
			return MysqlQueryResult.ok(result.getAffectedRows(), result.getWarnings().size());
		}
		if (isDdlStatement(parseResult.getKind())) {
			checkAllowed(session, MysqlGatewayOperation.DDL);
			NebulaDdlPlan plan = ddlConverter.convert(parseResult.getStatement(), schemaMapping);
			NebulaMutationResult result = requireMutationExecutor().executeDdl(plan);
			return MysqlQueryResult.ok(result.getAffectedRows(), result.getWarnings().size());
		}
		throw MysqlErrorMapper.unsupported("SQL statement is not supported by Nebula MySQL gateway");
	}

	private void checkTransport(MysqlProtocolSession session) throws SQLException {
		if (!securityPolicy.isTransportAllowed(session)) {
			throw MysqlErrorMapper.sslRequired();
		}
	}

	private void checkAllowed(MysqlProtocolSession session, MysqlGatewayOperation operation) throws SQLException {
		if (!securityPolicy.isAllowed(session, operation)) {
			throw MysqlErrorMapper.commandDenied(operation.name());
		}
	}

	private NebulaSqlParseResult parse(String sql) throws SQLException {
		try {
			return parser.parse(sql);
		} catch (NebulaSqlParseException exception) {
			return null;
		}
	}

	private NebulaSqlMutationExecutor requireMutationExecutor() throws SQLException {
		if (mutationExecutor == null) {
			throw MysqlErrorMapper.unsupported("Mutation executor is not configured");
		}
		return mutationExecutor;
	}

	private boolean isWriteStatement(StatementKind kind) {
		return kind == StatementKind.INSERT || kind == StatementKind.UPDATE || kind == StatementKind.DELETE;
	}

	private boolean isDdlStatement(StatementKind kind) {
		return kind == StatementKind.CREATE_SCHEMA || kind == StatementKind.DROP_SCHEMA;
	}

	private boolean isMetadataSql(String sql) {
		String upper = sql == null ? "" : sql.trim().toUpperCase(Locale.ENGLISH);
		return upper.startsWith("SHOW ") || upper.startsWith("DESCRIBE ") || upper.startsWith("DESC ");
	}

	private boolean isAdvancedMetadataSql(String sql) {
		String upper = sql == null ? "" : sql.trim().toUpperCase(Locale.ENGLISH);
		return "SHOW VARIABLES".equals(upper)
				|| "SELECT DATABASE()".equals(upper)
				|| upper.indexOf("INFORMATION_SCHEMA.TABLES") >= 0
				|| upper.indexOf("INFORMATION_SCHEMA.COLUMNS") >= 0;
	}

	private MysqlQueryResult routeAdvancedMetadata(MysqlProtocolSession session, String sql) throws SQLException {
		String upper = sql == null ? "" : sql.trim().toUpperCase(Locale.ENGLISH);
		if ("SHOW VARIABLES".equals(upper)) {
			return metadataHandler.showVariables(session);
		}
		if ("SELECT DATABASE()".equals(upper)) {
			return metadataHandler.selectDatabase(session);
		}
		if (upper.indexOf("INFORMATION_SCHEMA.TABLES") >= 0) {
			return metadataHandler.informationSchemaTables(session);
		}
		if (upper.indexOf("INFORMATION_SCHEMA.COLUMNS") >= 0) {
			return metadataHandler.informationSchemaColumns(session, extractTableName(sql));
		}
		throw MysqlErrorMapper.unsupported("Metadata query is not supported");
	}

	private String extractTableName(String sql) {
		String marker = "TABLE_NAME";
		String upper = sql == null ? "" : sql.toUpperCase(Locale.ENGLISH);
		int index = upper.indexOf(marker);
		if (index < 0) {
			return null;
		}
		int equals = upper.indexOf('=', index + marker.length());
		if (equals < 0) {
			return null;
		}
		String value = sql.substring(equals + 1).trim();
		if (value.startsWith("'") && value.indexOf("'", 1) > 1) {
			return value.substring(1, value.indexOf("'", 1));
		}
		if (value.startsWith("\"") && value.indexOf("\"", 1) > 1) {
			return value.substring(1, value.indexOf("\"", 1));
		}
		String[] parts = value.split("\\s+");
		return parts.length == 0 ? null : parts[0].replace("`", "");
	}

	private boolean isTransactionStatement(String sql) {
		String upper = sql == null ? "" : sql.trim().toUpperCase(Locale.ENGLISH);
		return "BEGIN".equals(upper) || "START TRANSACTION".equals(upper)
				|| "COMMIT".equals(upper) || "ROLLBACK".equals(upper);
	}

	private boolean isSessionSetStatement(String sql) {
		String upper = sql == null ? "" : sql.trim().toUpperCase(Locale.ENGLISH);
		return upper.startsWith("SET NAMES ")
				|| upper.startsWith("SET AUTOCOMMIT=")
				|| upper.startsWith("SET SQL_MODE=");
	}
}
