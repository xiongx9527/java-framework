package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineKnowledgeBaseDocument;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineKnowledgeBaseIndexResult;

public interface TinyEngineKnowledgeBaseService {

	TinyEngineKnowledgeBaseIndexResult index(TinyEngineKnowledgeBaseDocument document);
}
