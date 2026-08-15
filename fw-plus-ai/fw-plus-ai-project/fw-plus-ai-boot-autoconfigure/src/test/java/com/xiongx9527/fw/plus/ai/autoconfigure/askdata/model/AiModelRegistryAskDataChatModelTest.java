package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.model;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatRequest;
import com.xiongx9527.fw.plus.ai.askdata.model.AskDataChatResult;
import com.xiongx9527.fw.plus.ai.model.AiChatModel;
import com.xiongx9527.fw.plus.ai.model.AiChatRequest;
import com.xiongx9527.fw.plus.ai.model.AiChatResult;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelException;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.model.AiModelUsage;
import com.xiongx9527.fw.plus.ai.model.DefaultAiModelRegistry;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiModelRegistryAskDataChatModelTest {

	@Test
	void shouldAdaptTypedNeutralChatModelToCoreAskDataContract() {
		CapturingChatModel model = new CapturingChatModel("logical-chat");
		model.usage = new AiModelUsage(12L, 4L, 16L);
		AiModelRegistryAskDataChatModel adapter = adapter(model);
		Map<String, Object> parameters = Collections.<String, Object>singletonMap("temperature", Double.valueOf(0.2D));

		AskDataChatResult result = adapter.chat(new AskDataChatRequest(
				new AskDataProjectContext("project", "operator"), "question", "prompt", "logical-chat",
				parameters, 1800L));

		assertTrue(result.isSuccess());
		assertEquals("provider-chat", result.getActualModelKey());
		assertEquals("answer", result.getAnswer());
		assertEquals("prompt", model.request.getInput());
		assertEquals(Double.valueOf(0.2D), model.request.getParameters().get("temperature"));
		assertEquals(1800L, model.request.getTimeoutMillis());
		assertEquals(Long.valueOf(12L), result.getUsage().getInputTokens());
		assertEquals(Long.valueOf(4L), result.getUsage().getOutputTokens());
		assertEquals(Long.valueOf(16L), result.getUsage().getTotalTokens());
	}

	@Test
	void shouldMapNeutralInfrastructureFailureWithoutLeakingFrameworkException() {
		CapturingChatModel model = new CapturingChatModel("logical-chat");
		model.failure = AiModelException.infrastructureFailure("temporary outage", null);

		AskDataChatResult result = adapter(model).chat(new AskDataChatRequest(
				new AskDataProjectContext("project", "operator"), "prompt", "logical-chat", null, 0L));

		assertFalse(result.isSuccess());
		assertEquals("logical-chat", result.getActualModelKey());
		assertEquals(AiModelException.MODEL_INFRASTRUCTURE_FAILURE, result.getErrorCode());
		assertTrue(result.isRetryable());
	}

	private AiModelRegistryAskDataChatModel adapter(AiModel model) {
		Map<String, AiModel> models = new LinkedHashMap<String, AiModel>();
		models.put(model.getKey(), model);
		return new AiModelRegistryAskDataChatModel(new DefaultAiModelRegistry(models, model.getKey()));
	}

	private static final class CapturingChatModel implements AiChatModel {
		private final String key;
		private AiChatRequest request;
		private AiModelException failure;
		private AiModelUsage usage = AiModelUsage.unknown();

		private CapturingChatModel(String key) {
			this.key = key;
		}

		@Override
		public AiChatResult chat(AiChatRequest request) {
			this.request = request;
			if (failure != null) {
				throw failure;
			}
			return new AiChatResult("answer", "provider-chat", "stop", usage);
		}

		@Override
		public String getKey() { return key; }

		@Override
		public String getFramework() { return "test"; }

		@Override
		public String getProvider() { return "test"; }

		@Override
		public AiModelType getType() { return AiModelType.CHAT; }
	}
}
