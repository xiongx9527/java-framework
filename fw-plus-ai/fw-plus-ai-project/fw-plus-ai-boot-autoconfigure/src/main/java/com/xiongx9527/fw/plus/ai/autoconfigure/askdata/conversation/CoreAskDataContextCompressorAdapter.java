package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.conversation;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.flow.AskDataStepContext;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressionRequest;
import com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressionResult;

import java.util.LinkedHashMap;
import java.util.Map;

/** 将 Core 压缩 SPI 单向桥接到旧 Flow 压缩接口。 */
public final class CoreAskDataContextCompressorAdapter implements AskDataContextCompressor {
	private final com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressor delegate;
	private final AskDataContextCompressor deterministic;
	public CoreAskDataContextCompressorAdapter(
			com.xiongx9527.fw.plus.ai.askdata.context.AskDataContextCompressor delegate,
			AskDataContextCompressor deterministic) {
		if (delegate == null || deterministic == null) { throw new IllegalArgumentException("压缩器不能为空"); }
		this.delegate = delegate;
		this.deterministic = deterministic;
	}
	@Override public AskDataCompressedContext compress(AskDataStepContext context) {
		AskDataCompressedContext safe = deterministic.compress(context);
		AskDataRequest request = context == null ? null : context.getRequest();
		if (request == null || request.getProjectContext() == null) {
			throw new IllegalArgumentException("压缩上下文缺少项目和操作人");
		}
		Map<String, String> fields = structuredFields(safe);
		AskDataContextCompressionResult semantic = delegate.compress(new AskDataContextCompressionRequest(
				request.getProjectContext(), safe.getCompressedSummary(), fields, 30000L));
		if (semantic == null || !fields.equals(semantic.getStructuredFields())) {
			throw new IllegalStateException("语义压缩器修改了恢复安全字段");
		}
		safe.setCompressedSummary(semantic.getSummary());
		return safe;
	}
	private Map<String, String> structuredFields(AskDataCompressedContext safe) {
		Map<String, String> fields = new LinkedHashMap<String, String>();
		put(fields, "projectId", safe.getProjectId()); put(fields, "operatorId", safe.getOperatorId());
		put(fields, "conversationId", safe.getConversationId()); put(fields, "checkpointId", safe.getCheckpointId());
		put(fields, "resumeToken", safe.getResumeToken()); put(fields, "datasourceKey", safe.getDatasourceKey());
		put(fields, "currentStep", safe.getCurrentStep()); put(fields, "executionState", safe.getExecutionState());
		put(fields, "sqlValidated", safe.getSqlValidated()); put(fields, "sqlExecuted", safe.getSqlExecuted());
		put(fields, "rowCount", safe.getRowCount()); put(fields, "errorCode", safe.getErrorCode());
		return fields;
	}
	private void put(Map<String, String> fields, String key, Object value) {
		if (value != null) { fields.put(key, String.valueOf(value)); }
	}
}
