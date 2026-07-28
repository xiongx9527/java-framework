package com.xiongx9527.fw.plus.ai.askdata.vector;

/** 向量文档值对象。 */
public final class AskDataVectorDocument {
	private final String id; private final String content; private final float[] vector;
	public AskDataVectorDocument(String id, String content, float[] vector) {
		if (id == null || id.trim().length() == 0 || vector == null) throw new IllegalArgumentException("id 和 vector 不能为空");
		this.id = id.trim(); this.content = content; this.vector = vector.clone();
	}
	public String getId() { return id; } public String getContent() { return content; } public float[] getVector() { return vector.clone(); }
}
