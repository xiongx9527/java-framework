package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataModelUsage;
import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelRegistry;
import com.xiongx9527.fw.plus.ai.model.AiModelType;

/** 将中立模型注册表单向适配为 Core AskData Chat SPI。 */
public final class AiModelRegistryAskDataChatModel
		implements com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatModel {

	private static final String MODEL_NOT_FOUND = "MODEL_NOT_FOUND";

	private final AiModelRegistry modelRegistry;

	public AiModelRegistryAskDataChatModel(AiModelRegistry modelRegistry) {
		if (modelRegistry == null) {
			throw new IllegalArgumentException("modelRegistry 不能为空");
		}
		this.modelRegistry = modelRegistry;
	}

	@Override
	public AskDataChatResult chat(AskDataChatRequest request) {
		if (request == null || isBlank(request.getModelKey())) {
			throw new IllegalArgumentException("AskData Chat 请求必须携带模型 key");
		}
		String modelKey = request.getModelKey();
		if (!modelRegistry.contains(modelKey)) {
			return AskDataChatResult.failure(modelKey, MODEL_NOT_FOUND, "未找到聊天模型", false);
		}
		AiModel model = modelRegistry.get(modelKey);
		if (!(model instanceof AiChatModel) || AiModelType.CHAT != model.getType()) {
			return AskDataChatResult.failure(modelKey, AiModelException.MODEL_REQUEST_REJECTED,
					"模型不支持类型化 Chat 契约", false);
		}
		try {
			AiChatResult result = ((AiChatModel) model).chat(new AiChatRequest(request.getPrompt(),
					request.getParameters(), request.getTimeoutMillis()));
			if (result == null) {
				return AskDataChatResult.failure(modelKey, AiModelException.MODEL_ERROR, "聊天模型未返回结果", false);
			}
			return AskDataChatResult.success(result.getText(), result.getActualModelKey(), usage(result));
		} catch (AiModelException ex) {
			return AskDataChatResult.failure(modelKey, ex.getErrorCode(), safeMessage(ex), ex.isRetryable());
		} catch (RuntimeException ex) {
			return AskDataChatResult.failure(modelKey, AiModelException.MODEL_ERROR, "聊天模型执行失败", false);
		}
	}

	private AskDataModelUsage usage(AiChatResult result) {
		if (result == null || result.getUsage() == null || result.getUsage().isUnknown()) {
			return AskDataModelUsage.unknown();
		}
		return new AskDataModelUsage(result.getUsage().getInputTokens(), result.getUsage().getOutputTokens(),
				result.getUsage().getTotalTokens(), null);
	}

	private static String safeMessage(AiModelException exception) {
		return isBlank(exception.getMessage()) ? "聊天模型执行失败" : exception.getMessage();
	}

	private static boolean isBlank(String value) {
		return value == null || value.trim().length() == 0;
	}
}
