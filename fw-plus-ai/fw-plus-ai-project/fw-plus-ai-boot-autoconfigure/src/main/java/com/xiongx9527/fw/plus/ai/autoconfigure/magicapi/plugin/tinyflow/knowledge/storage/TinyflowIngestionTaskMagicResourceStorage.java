package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.TinyflowKnowledgeMagicResource;

public final class TinyflowIngestionTaskMagicResourceStorage extends AbstractTinyflowKnowledgeMagicResourceStorage {
	public static final String FOLDER = "tinyflow-ingestion-task";
	public static final String COMPENSATION = "compensation";
	@Override public String folder() { return FOLDER; }
	@Override public void validate(TinyflowKnowledgeMagicResource entity) {
		common(entity); required(entity.getKnowledgeId(), "knowledgeId");
		if (COMPENSATION.equals(entity.getRecordType())) {
			required(entity.getCompensationId(), "compensationId"); required(entity.getSourceId(), "sourceId");
			required(entity.getDocumentId(), "documentId"); required(entity.getOperation(), "operation"); required(entity.getStage(), "stage");
		} else {
			required(entity.getTaskId(), "taskId"); required(entity.getProviderKey(), "providerKey");
			required(entity.getDocumentId(), "documentId"); required(entity.getStatus(), "status");
		}
	}
}
