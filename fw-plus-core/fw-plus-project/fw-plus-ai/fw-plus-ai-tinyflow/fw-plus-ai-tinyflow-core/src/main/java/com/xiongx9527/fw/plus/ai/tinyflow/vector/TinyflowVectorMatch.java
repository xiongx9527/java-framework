package com.xiongx9527.fw.plus.ai.tinyflow.vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TinyflowVectorMatch {

	private final String id;
	private final String content;
	private final double score;
	private final Map<String, Object> metadata;

	public TinyflowVectorMatch(String id, String content, double score, Map<String, Object> metadata) {
		this.id = id;
		this.content = content;
		this.score = score;
		this.metadata = metadata == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
	}

	public String getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public double getScore() {
		return score;
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}
}
