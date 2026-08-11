package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

public interface TinyflowKnowledgeManagementProvider extends TinyflowKnowledgeProvider {

	TinyflowKnowledgeDefinition create(TinyflowKnowledgeDefinition definition);

	TinyflowKnowledgeDefinition update(TinyflowKnowledgeDefinition definition);

	void delete(String tenantId, String knowledgeId);

	TinyflowIngestionTask importDocument(TinyflowDocumentImport request);
}
