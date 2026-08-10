package com.xiongx9527.fw.plus.ai.tinyflow.vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class TinyflowVectorDocument {

	private final String id;
	private final String content;
	private final float[] vector;
	private final Map<String, Object> metadata;

	public TinyflowVectorDocument(String id, String content, float[] vector, Map<String, Object> metadata) {
		this.id = id;
		this.content = content;
		this.vector = vector == null ? null : vector.clone();
		this.metadata = metadata == null ? Collections.<String, Object>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, Object>(metadata));
	}

	public String getId() {
		return id;
	}

	public String getContent() {
		return content;
	}

	public float[] getVector() {
		return vector == null ? null : vector.clone();
	}

	public Map<String, Object> getMetadata() {
		return metadata;
	}
}
