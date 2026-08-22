package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.TinyflowKnowledgeMagicResource;

public final class TinyflowDocumentMagicResourceStorage extends AbstractTinyflowKnowledgeMagicResourceStorage {
	public static final String FOLDER = "tinyflow-document";
	@Override public String folder() { return FOLDER; }
	@Override public void validate(TinyflowKnowledgeMagicResource entity) {
		common(entity); required(entity.getKnowledgeId(), "knowledgeId"); required(entity.getSourceId(), "sourceId");
		required(entity.getSourceType(), "sourceType"); required(entity.getDocumentId(), "documentId");
		required(entity.getChecksum(), "checksum");
	}
}
