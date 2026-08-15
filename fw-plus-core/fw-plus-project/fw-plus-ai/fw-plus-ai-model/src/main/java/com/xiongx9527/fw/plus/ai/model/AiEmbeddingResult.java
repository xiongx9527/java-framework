package com.xiongx9527.fw.plus.ai.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class AiEmbeddingResult {

	private final List<float[]> embeddings;

	private final String actualModelKey;

	private final AiModelUsage usage;

	public AiEmbeddingResult(List<float[]> embeddings, String actualModelKey, AiModelUsage usage) {
		if (embeddings == null || embeddings.isEmpty()) {
			throw new IllegalArgumentException("embeddings cannot be empty");
		}
		if (isBlank(actualModelKey)) {
			throw new IllegalArgumentException("actualModelKey cannot be blank");
		}
		this.embeddings = copyEmbeddings(embeddings);
		this.actualModelKey = actualModelKey;
		this.usage = usage == null ? AiModelUsage.unknown() : usage;
	}

	public List<float[]> getEmbeddings() {
		return copyEmbeddings(embeddings);
	}

	public String getActualModelKey() {
		return actualModelKey;
	}

	public AiModelUsage getUsage() {
		return usage;
	}

	private static List<float[]> copyEmbeddings(List<float[]> source) {
		List<float[]> copy = new ArrayList<float[]>(source.size());
		for (float[] embedding : source) {
			if (embedding == null) {
				throw new IllegalArgumentException("embeddings cannot contain null");
			}
			copy.add(embedding.clone());
		}
		return Collections.unmodifiableList(copy);
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().isEmpty();
	}
}
