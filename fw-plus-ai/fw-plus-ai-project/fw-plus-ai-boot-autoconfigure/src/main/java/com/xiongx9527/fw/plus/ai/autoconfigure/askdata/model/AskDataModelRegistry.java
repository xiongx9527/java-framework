package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;

public interface AskDataModelRegistry {

	AskDataChatModel getChatModel(String modelKey);

	default AskDataModelResponse generate(AskDataRequest request, String prompt) {
		AskDataChatModel chatModel = getChatModel(request == null ? null : request.getModelKey());
		if (chatModel == null) {
			throw new AskDataModelException(AskDataErrorCode.MODEL_NOT_FOUND, "AskData chat model was not found");
		}
		return chatModel.generate(prompt);
	}

	AskDataEmbeddingModel getEmbeddingModel(String modelKey);

	String getDefaultChatModelKey();

	String getDefaultEmbeddingModelKey();
}
