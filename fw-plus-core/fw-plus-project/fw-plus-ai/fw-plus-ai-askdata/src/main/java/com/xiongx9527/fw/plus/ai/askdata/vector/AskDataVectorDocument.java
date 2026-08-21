package com.xiongx9527.fw.plus.ai.askdata.vector;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** 向量文档值对象。 */
public final class AskDataVectorDocument {
	private final String id; private final String content; private final float[] vector;
	private final Map<String, String> metadata;
	public AskDataVectorDocument(String id, String content, float[] vector) {
		this(id, content, vector, Collections.<String, String>emptyMap());
	}
	public AskDataVectorDocument(String id, String content, float[] vector, Map<String, String> metadata) {
		if (id == null || id.trim().length() == 0 || vector == null) throw new IllegalArgumentException("id 和 vector 不能为空");
		this.id = id.trim(); this.content = content; this.vector = vector.clone();
		this.metadata = metadata == null ? Collections.<String, String>emptyMap()
				: Collections.unmodifiableMap(new LinkedHashMap<String, String>(metadata));
	}
	public String getId() { return id; } public String getContent() { return content; } public float[] getVector() { return vector.clone(); }
	public Map<String, String> getMetadata() { return metadata; }
}
