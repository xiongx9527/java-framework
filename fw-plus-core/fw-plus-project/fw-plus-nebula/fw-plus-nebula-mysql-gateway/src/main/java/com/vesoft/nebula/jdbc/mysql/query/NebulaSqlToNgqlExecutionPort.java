package com.vesoft.nebula.jdbc.mysql.query;

import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.vesoft.nebula.jdbc.sql.parser.NebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.druid.DruidNebulaSqlParser;
import com.vesoft.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import com.vesoft.nebula.jdbc.sql.query.DefaultNebulaSqlQueryConverter;
import com.vesoft.nebula.jdbc.sql.query.NebulaQueryStatement;
import com.vesoft.nebula.jdbc.sql.query.NebulaQueryStatementAdapter;
import com.vesoft.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.vesoft.nebula.jdbc.sql.query.NebulaSqlQueryConverter;
import com.vesoft.nebula.jdbc.sql.query.NgqlQueryPlan;

import java.sql.SQLException;

public class NebulaSqlToNgqlExecutionPort implements NebulaSqlExecutionPort {
	private final NebulaSqlParser parser;
	private final NebulaQueryStatementAdapter statementAdapter;
	private final NebulaSqlQueryConverter queryConverter;
	private final NebulaSchemaMappingRegistry schemaMapping;
	private final NgqlQueryExecutor ngqlExecutor;

	public NebulaSqlToNgqlExecutionPort(NebulaSchemaMappingRegistry schemaMapping, NgqlQueryExecutor ngqlExecutor) {
		this(new DruidNebulaSqlParser(),
				new NebulaQueryStatementAdapter(),
				new DefaultNebulaSqlQueryConverter(),
				schemaMapping,
				ngqlExecutor);
	}

	public NebulaSqlToNgqlExecutionPort(NebulaSqlParser parser,
										NebulaQueryStatementAdapter statementAdapter,
										NebulaSqlQueryConverter queryConverter,
										NebulaSchemaMappingRegistry schemaMapping,
										NgqlQueryExecutor ngqlExecutor) {
		if (parser == null) {
			throw new IllegalArgumentException("parser must not be null");
		}
		if (statementAdapter == null) {
			throw new IllegalArgumentException("statementAdapter must not be null");
		}
		if (queryConverter == null) {
			throw new IllegalArgumentException("queryConverter must not be null");
		}
		if (schemaMapping == null) {
			throw new IllegalArgumentException("schemaMapping must not be null");
		}
		if (ngqlExecutor == null) {
			throw new IllegalArgumentException("ngqlExecutor must not be null");
		}
		this.parser = parser;
		this.statementAdapter = statementAdapter;
		this.queryConverter = queryConverter;
		this.schemaMapping = schemaMapping;
		this.ngqlExecutor = ngqlExecutor;
	}

	@Override
	public MysqlQueryResult executeQuery(MysqlGatewaySessionContext context, String sql) throws SQLException {
		NebulaSqlParseResult parseResult;
		try {
			parseResult = parser.parse(sql);
		}
		catch (NebulaSqlParseException exception) {
			throw new SQLException("Failed to parse SQL for Nebula query conversion: " + exception.getMessage(), exception);
		}
		NebulaQueryStatement statement = statementAdapter.adapt(parseResult);
		NgqlQueryPlan plan = queryConverter.convert(statement, com.vesoft.nebula.jdbc.sql.query.NebulaQueryBindings.empty(), schemaMapping);
		return ngqlExecutor.execute(context, plan);
	}

	public interface NgqlQueryExecutor {
		MysqlQueryResult execute(MysqlGatewaySessionContext context, NgqlQueryPlan plan) throws SQLException;
	}
}
