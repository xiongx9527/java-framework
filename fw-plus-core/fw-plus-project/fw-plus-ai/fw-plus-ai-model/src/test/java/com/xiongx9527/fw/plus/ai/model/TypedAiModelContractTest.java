package com.xiongx9527.fw.plus.ai.model;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TypedAiModelContractTest {

	@Test
	public void shouldExposeTypedChatAndEmbeddingModelsAsNeutralModels() {
		assertTrue(AiModel.class.isAssignableFrom(AiChatModel.class));
		assertTrue(AiModel.class.isAssignableFrom(AiEmbeddingModel.class));
	}

	@Test
	public void shouldDefensivelyCopyChatRequestParameters() {
		Map<String, Object> parameters = new LinkedHashMap<String, Object>();
		parameters.put("temperature", Double.valueOf(0.2D));

		AiChatRequest request = new AiChatRequest("prompt", parameters, 3000L);
		parameters.put("temperature", Double.valueOf(0.8D));

		assertEquals("prompt", request.getInput());
		assertEquals(Double.valueOf(0.2D), request.getParameters().get("temperature"));
		assertEquals(3000L, request.getTimeoutMillis());
		assertThrows(UnsupportedOperationException.class,
				() -> request.getParameters().put("topP", Double.valueOf(0.9D)));
	}

	@Test
	public void shouldDefensivelyCopyBatchEmbeddingRequest() {
		List<String> inputs = new ArrayList<String>(Arrays.asList("first", "second"));
		Map<String, Object> parameters = new LinkedHashMap<String, Object>();
		parameters.put("dimensions", Integer.valueOf(3));

		AiEmbeddingRequest request = new AiEmbeddingRequest(inputs, parameters, 5000L);
		inputs.add("third");
		parameters.clear();

		assertEquals(Arrays.asList("first", "second"), request.getInputs());
		assertEquals(Integer.valueOf(3), request.getParameters().get("dimensions"));
		assertEquals(5000L, request.getTimeoutMillis());
		assertThrows(UnsupportedOperationException.class, () -> request.getInputs().add("third"));
		assertThrows(UnsupportedOperationException.class, () -> request.getParameters().clear());
	}

	@Test
	public void shouldRejectNegativeTimeouts() {
		assertThrows(IllegalArgumentException.class,
				() -> new AiChatRequest("prompt", null, -1L));
		assertThrows(IllegalArgumentException.class,
				() -> new AiEmbeddingRequest(Arrays.asList("text"), null, -1L));
	}

	@Test
	public void shouldPreserveActualModelKeyFinishReasonAndUsage() {
		AiModelUsage usage = new AiModelUsage(7L, 3L, 10L);
		AiChatResult result = new AiChatResult("answer", "sales-chat", "stop", usage);

		assertEquals("answer", result.getText());
		assertEquals("sales-chat", result.getActualModelKey());
		assertEquals("stop", result.getFinishReason());
		assertEquals(7L, result.getUsage().getInputTokens());
		assertEquals(3L, result.getUsage().getOutputTokens());
		assertEquals(10L, result.getUsage().getTotalTokens());
	}

	@Test
	public void shouldDefensivelyCopyEveryEmbeddingVector() {
		float[] first = new float[] { 0.1F, 0.2F };
		float[] second = new float[] { 0.3F, 0.4F };
		AiEmbeddingResult result = new AiEmbeddingResult(Arrays.asList(first, second),
				"sales-embedding", AiModelUsage.unknown());

		first[0] = 9.0F;
		List<float[]> returned = result.getEmbeddings();
		returned.get(1)[0] = 8.0F;

		assertEquals("sales-embedding", result.getActualModelKey());
		assertTrue(result.getUsage().isUnknown());
		assertArrayEquals(new float[] { 0.1F, 0.2F }, result.getEmbeddings().get(0));
		assertArrayEquals(new float[] { 0.3F, 0.4F }, result.getEmbeddings().get(1));
		assertThrows(UnsupportedOperationException.class,
				() -> result.getEmbeddings().add(new float[] { 0.5F }));
	}
}
