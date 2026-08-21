package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.storage;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge.TinyflowKnowledgeMagicResource;

public final class TinyflowKnowledgeKeyMagicResourceStorage extends AbstractTinyflowKnowledgeMagicResourceStorage {
	public static final String FOLDER = "tinyflow-knowledge-key";
	@Override public String folder() { return FOLDER; }
	@Override public void validate(TinyflowKnowledgeMagicResource entity) {
		common(entity); required(entity.getKnowledgeId(), "knowledgeId"); required(entity.getAccessKeyId(), "accessKeyId");
		required(entity.getPrefix(), "prefix"); required(entity.getHashAlgorithm(), "hashAlgorithm");
		required(entity.getSalt(), "salt"); required(entity.getAccessKeyHash(), "accessKeyHash");
		if (entity.getCost() < 100000) throw new IllegalArgumentException("cost is too small");
	}
}
