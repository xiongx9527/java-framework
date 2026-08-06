package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import com.xiongx9527.fw.plus.ai.tinyflow.TinyflowException;
import dev.tinyflow.core.knowledge.Knowledge;
import dev.tinyflow.core.knowledge.KnowledgeProvider;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TinyflowKnowledgeManagerAdapter implements KnowledgeProvider {

	private final TinyflowKnowledgeProviderRegistry registry;

	public TinyflowKnowledgeManagerAdapter(TinyflowKnowledgeProviderRegistry registry) {
		if (registry == null) {
			throw new IllegalArgumentException("registry must not be null");
		}
		this.registry = registry;
	}

	@Override
	public Knowledge getKnowledge(Object id) {
		final TinyflowKnowledgeReference reference = TinyflowKnowledgeReference.from(id);
		final TinyflowKnowledgeProvider provider = registry.require(reference.getProvider());
		return (keyword, limit, node, chain) -> toMaps(provider.retrieve(
				TinyflowKnowledgeRequest.from(reference, keyword, limit, chain)));
	}

	private List<Map<String, Object>> toMaps(TinyflowKnowledgeResult result) {
		if (result == null) {
			throw new IllegalStateException("Tinyflow knowledge provider returned null result");
		}
		if (!result.isKnowledgeExists()) {
			throw new TinyflowException("Tinyflow knowledge does not exist");
		}
		List<Map<String, Object>> values = new ArrayList<Map<String, Object>>();
		for (TinyflowKnowledgeMatch match : result.getMatches()) {
			Map<String, Object> value = new LinkedHashMap<String, Object>();
			value.put("sourceId", match.getSourceId());
			value.put("sourceType", match.getSourceType());
			value.put("documentId", match.getDocumentId());
			value.put("chunkId", match.getChunkId());
			value.put("content", match.getContent());
			value.put("score", match.getScore());
			value.put("sourceUri", match.getSourceUri());
			value.put("sourceVersion", match.getSourceVersion());
			value.put("location", match.getLocation());
			value.put("position", match.getPosition());
			value.put("parentChunkId", match.getParentChunkId());
			value.put("tokenCount", match.getTokenCount());
			value.put("metadata", match.getMetadata());
			value.put("enabled", match.isEnabled());
			value.put("vectorIds", match.getVectorIds());
			values.add(value);
		}
		return values;
	}
}
