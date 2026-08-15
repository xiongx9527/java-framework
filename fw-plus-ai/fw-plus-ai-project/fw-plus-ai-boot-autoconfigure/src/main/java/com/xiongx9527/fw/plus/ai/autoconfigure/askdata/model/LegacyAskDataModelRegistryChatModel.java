package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult;

/** 让旧模型注册表也必须经过 Core Router 后再执行的单向兼容适配器。 */
public final class LegacyAskDataModelRegistryChatModel
		implements com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel {

	private final AskDataModelRegistry modelRegistry;

	public LegacyAskDataModelRegistryChatModel(AskDataModelRegistry modelRegistry) {
		if (modelRegistry == null) throw new IllegalArgumentException("modelRegistry 不能为空");
		this.modelRegistry = modelRegistry;
	}

	@Override
	public AskDataChatResult chat(AskDataChatRequest request) {
		if (request == null || blank(request.getModelKey())) {
			throw new IllegalArgumentException("AskData Chat 请求必须携带模型 key");
		}
		String modelKey = request.getModelKey();
		try {
			com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest legacyRequest =
					new com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataRequest();
			legacyRequest.setQuestion(request.getQuestion());
			legacyRequest.setProjectContext(request.getContext());
			legacyRequest.setModelKey(modelKey);
			legacyRequest.setOptions(request.getParameters());
			AskDataModelResponse response = modelRegistry.generate(legacyRequest, request.getPrompt());
			if (response == null) {
				return AskDataChatResult.failure(modelKey, "MODEL_ERROR", "聊天模型未返回结果", false);
			}
			String actualModelKey = blank(response.getModelKey()) ? modelKey : response.getModelKey().trim();
			return AskDataChatResult.success(response.getText(), actualModelKey);
		} catch (AskDataModelException ex) {
			return AskDataChatResult.failure(modelKey, ex.getErrorCode(), safe(ex.getMessage()), false);
		} catch (RuntimeException ex) {
			return AskDataChatResult.failure(modelKey, "MODEL_ERROR", "聊天模型执行失败", false);
		}
	}

	private static String safe(String value) { return blank(value) ? "聊天模型执行失败" : value; }
	private static boolean blank(String value) { return value == null || value.trim().length() == 0; }
}
