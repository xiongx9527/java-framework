package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.service;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.rag.TinyEngineRagSearchResult;

public interface TinyEngineRagService {

	boolean isAvailable();

	String getUnavailableReason();

	TinyEngineRagSearchResult search(TinyEngineRagRequest request);
}
