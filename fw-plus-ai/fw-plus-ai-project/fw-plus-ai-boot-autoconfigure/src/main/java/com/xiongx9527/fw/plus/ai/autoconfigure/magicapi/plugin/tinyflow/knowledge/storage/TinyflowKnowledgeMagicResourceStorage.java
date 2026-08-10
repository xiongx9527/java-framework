package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.TinyflowKnowledgeMagicResource;

public final class TinyflowKnowledgeMagicResourceStorage extends AbstractTinyflowKnowledgeMagicResourceStorage {
	public static final String FOLDER = "tinyflow-knowledge";
	@Override public String folder() { return FOLDER; }
	@Override public void validate(TinyflowKnowledgeMagicResource entity) {
		common(entity); required(entity.getKnowledgeId(), "knowledgeId"); required(entity.getProviderKey(), "providerKey");
		required(entity.getDisplayName() == null ? entity.getName() : entity.getDisplayName(), "displayName");
		if (entity.getVersion() < 1L) throw new IllegalArgumentException("version must be positive");
	}
}
