package com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.trigger;

import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaDdlPlan;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationKind;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationPlan;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaMutationResult;
import com.xiongx9527.fw.plus.graph.nebula.jdbc.sql.write.NebulaSqlMutationExecutor;

import java.sql.SQLException;
import java.util.List;

public class TriggerAwareMutationExecutor implements NebulaSqlMutationExecutor {
	private final String schemaName;
	private final TriggerMetadataService metadataService;
	private final TriggerBodyExecutor bodyExecutor;
	private final NebulaSqlMutationExecutor delegate;
	private final int maxDepth;

	public TriggerAwareMutationExecutor(String schemaName, TriggerMetadataService metadataService,
			TriggerBodyExecutor bodyExecutor, NebulaSqlMutationExecutor delegate, int maxDepth) {
		if (schemaName == null || schemaName.trim().length() == 0) {
			throw new IllegalArgumentException("Schema name must not be blank.");
		}
		if (metadataService == null) {
			throw new IllegalArgumentException("Trigger metadata service must not be null.");
		}
		if (bodyExecutor == null) {
			throw new IllegalArgumentException("Trigger body executor must not be null.");
		}
		if (delegate == null) {
			throw new IllegalArgumentException("Mutation executor delegate must not be null.");
		}
		if (maxDepth < 1) {
			throw new IllegalArgumentException("Trigger max depth must be greater than zero.");
		}
		this.schemaName = schemaName.trim();
		this.metadataService = metadataService;
		this.bodyExecutor = bodyExecutor;
		this.delegate = delegate;
		this.maxDepth = maxDepth;
	}

	public NebulaMutationResult executeMutation(NebulaMutationPlan plan) throws SQLException {
		return executeMutation(plan, TriggerExecutionContext.root(maxDepth));
	}

	public NebulaMutationResult executeMutation(NebulaMutationPlan plan, TriggerExecutionContext context)
			throws SQLException {
		if (plan == null) {
			throw new IllegalArgumentException("Mutation plan must not be null.");
		}
		TriggerEvent event = toEvent(plan.getMutationKind());
		executeTriggers(plan, context, event, TriggerTiming.BEFORE);
		NebulaMutationResult result = delegate.executeMutation(plan);
		if (!result.isSuccess()) {
			return result;
		}
		executeTriggers(plan, context, event, TriggerTiming.AFTER);
		return result;
	}

	public NebulaMutationResult executeDdl(NebulaDdlPlan plan) throws SQLException {
		return delegate.executeDdl(plan);
	}

	private void executeTriggers(NebulaMutationPlan plan, TriggerExecutionContext context,
			TriggerEvent event, TriggerTiming timing) throws SQLException {
		List<TriggerDefinition> definitions = metadataService.findEnabled(schemaName, plan.getTargetName(), event, timing);
		for (TriggerDefinition definition : definitions) {
			TriggerExecutionContext childContext = context.enter(definition);
			bodyExecutor.executeBody(definition, plan, childContext);
		}
	}

	private TriggerEvent toEvent(NebulaMutationKind kind) throws SQLException {
		if (kind == NebulaMutationKind.INSERT_VERTEX || kind == NebulaMutationKind.INSERT_EDGE) {
			return TriggerEvent.INSERT;
		}
		if (kind == NebulaMutationKind.UPDATE_VERTEX || kind == NebulaMutationKind.UPDATE_EDGE) {
			return TriggerEvent.UPDATE;
		}
		if (kind == NebulaMutationKind.DELETE_VERTEX || kind == NebulaMutationKind.DELETE_EDGE) {
			return TriggerEvent.DELETE;
		}
		throw new SQLException("Nebula MySQL trigger subset error: Unsupported mutation kind for trigger.", "HY000");
	}
}
