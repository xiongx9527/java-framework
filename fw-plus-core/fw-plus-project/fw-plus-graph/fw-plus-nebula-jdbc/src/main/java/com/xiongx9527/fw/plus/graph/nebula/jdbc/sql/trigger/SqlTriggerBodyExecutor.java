package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.NebulaSqlParseResult;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.NebulaSqlParser;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.StatementKind;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.parser.error.NebulaSqlParseException;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.query.NebulaSchemaMappingRegistry;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationResult;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaSqlWriteConverter;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaWriteBindings;

import java.sql.SQLException;

public class SqlTriggerBodyExecutor implements TriggerBodyExecutor {
	private final String schemaName;
	private final TriggerMetadataService metadataService;
	private final TriggerDdlHandler bodyValidator;
	private final NebulaSqlParser parser;
	private final NebulaSqlWriteConverter writeConverter;
	private final NebulaSchemaMappingRegistry schemaMapping;
	private final NebulaSqlMutationExecutor delegate;
	private final int maxDepth;

	public SqlTriggerBodyExecutor(String schemaName, TriggerMetadataService metadataService, NebulaSqlParser parser,
			NebulaSqlWriteConverter writeConverter, NebulaSchemaMappingRegistry schemaMapping,
			NebulaSqlMutationExecutor delegate, int maxDepth) {
		if (schemaName == null || schemaName.trim().length() == 0) {
			throw new IllegalArgumentException("Schema name must not be blank.");
		}
		if (metadataService == null) {
			throw new IllegalArgumentException("Trigger metadata service must not be null.");
		}
		if (parser == null) {
			throw new IllegalArgumentException("SQL parser must not be null.");
		}
		if (writeConverter == null) {
			throw new IllegalArgumentException("SQL write converter must not be null.");
		}
		if (schemaMapping == null) {
			throw new IllegalArgumentException("Schema mapping must not be null.");
		}
		if (delegate == null) {
			throw new IllegalArgumentException("Mutation executor delegate must not be null.");
		}
		if (maxDepth < 1) {
			throw new IllegalArgumentException("Trigger max depth must be greater than zero.");
		}
		this.schemaName = schemaName.trim();
		this.metadataService = metadataService;
		this.bodyValidator = new TriggerDdlHandler(metadataService);
		this.parser = parser;
		this.writeConverter = writeConverter;
		this.schemaMapping = schemaMapping;
		this.delegate = delegate;
		this.maxDepth = maxDepth;
	}

	public NebulaMutationResult executeBody(TriggerDefinition definition, NebulaMutationPlan parentPlan,
			TriggerExecutionContext context) throws SQLException {
		if (definition == null) {
			throw new IllegalArgumentException("Trigger definition must not be null.");
		}
		String bodySql = definition.getBodySql();
		bodyValidator.validateBody(bodySql);
		NebulaSqlParseResult parseResult = parseBody(bodySql);
		if (!isWriteStatement(parseResult.getKind())) {
			throw new SQLException("Nebula MySQL trigger subset body error: only INSERT, UPDATE and DELETE are supported.", "42000");
		}
		NebulaMutationPlan bodyPlan = writeConverter.convert(parseResult.getStatement(),
				new NebulaWriteBindings(bodySql, null), schemaMapping);
		return new TriggerAwareMutationExecutor(schemaName, metadataService, this, delegate, maxDepth)
				.executeMutation(bodyPlan, context);
	}

	private NebulaSqlParseResult parseBody(String bodySql) throws SQLException {
		try {
			return parser.parse(bodySql);
		} catch (NebulaSqlParseException exception) {
			throw new SQLException("Nebula MySQL trigger subset body error: " + exception.getMessage(),
					"42000", exception);
		}
	}

	private boolean isWriteStatement(StatementKind kind) {
		return kind == StatementKind.INSERT || kind == StatementKind.UPDATE || kind == StatementKind.DELETE;
	}
}
