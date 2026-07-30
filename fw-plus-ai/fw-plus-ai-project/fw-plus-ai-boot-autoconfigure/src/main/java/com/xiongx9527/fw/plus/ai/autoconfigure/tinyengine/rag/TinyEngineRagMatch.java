package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class TinyEngineRagMatch {

	private final double score;
	private final String content;
	private final Map<String, Object> metadata;

	public TinyEngineRagMatch(double score, String content, Map<String, Object> metadata) {
		this.score = score;
		this.content = content;
		this.metadata = metadata == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
	}

	public double getScore() { return score; }
	public String getContent() { return content; }
	public Map<String, Object> getMetadata() { return metadata; }
}
