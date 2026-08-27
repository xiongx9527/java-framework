package com.xiongx9527.fw.plus.ai.tinyflow.knowledge;

import java.util.List;

public interface TinyflowReranker {

	List<TinyflowKnowledgeMatch> rerank(TinyflowKnowledgeRequest request,
			List<TinyflowKnowledgeMatch> matches);
}
