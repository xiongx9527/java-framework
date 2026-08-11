package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.TinyflowKnowledgeMagicResource;

public final class TinyflowDifyConnectionMagicResourceStorage extends AbstractTinyflowKnowledgeMagicResourceStorage {
	public static final String FOLDER = "tinyflow-dify-connection";
	@Override public String folder() { return FOLDER; }
	@Override public void validate(TinyflowKnowledgeMagicResource entity) {
		common(entity); required(entity.getConnectionId(), "connectionId"); required(entity.getKnowledgeId(), "knowledgeId");
		required(entity.getBaseUrl(), "baseUrl"); required(entity.getDatasetId(), "datasetId");
		required(entity.getCredentialRef(), "credentialRef");
	}
}
