package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

public interface TinyflowKnowledgeProvider {

	String getKey();

	TinyflowKnowledgeCapabilities capabilities();

	TinyflowKnowledgeResult retrieve(TinyflowKnowledgeRequest request);
}
