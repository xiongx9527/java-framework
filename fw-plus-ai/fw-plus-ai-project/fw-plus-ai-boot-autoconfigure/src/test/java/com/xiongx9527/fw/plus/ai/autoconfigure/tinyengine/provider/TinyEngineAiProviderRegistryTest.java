package com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatRequest;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatMessage;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatResponse;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.chat.TinyEngineChatStreamChunk;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class TinyEngineAiProviderRegistryTest {

	@Test
	public void shouldUseLangChain4jAsDefaultProviderKey() {
		TinyEngineAiProviderRegistry registry = new TinyEngineAiProviderRegistry(
				Collections.<TinyEngineAiProvider>singletonList(new TestProvider("langchain4j", true)), null);

		assertEquals("langchain4j", registry.getDefaultProviderKey());
		assertEquals("langchain4j", registry.getDefaultProvider().getKey());
	}

	@Test
	public void shouldPreferConfiguredCustomProvider() {
		TinyEngineAiProviderRegistry registry = new TinyEngineAiProviderRegistry(Arrays.<TinyEngineAiProvider>asList(
				new TestProvider("langchain4j", true), new TestProvider("custom", true)), "custom");

		assertEquals("custom", registry.getDefaultProviderKey());
		assertEquals("custom", registry.getDefaultProvider().getKey());
	}

	@Test
	public void shouldReportProviderAndCapabilityForUnsupportedCapability() {
		TinyEngineAiProviderRegistry registry = new TinyEngineAiProviderRegistry(
				Collections.<TinyEngineAiProvider>singletonList(new TestProvider("langchain4j", false)), null);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> registry.requireCapability(TinyEngineAiCapability.STREAMING_CHAT));

		assertEquals("TinyEngine AI provider 'langchain4j' does not support capability 'STREAMING_CHAT'",
				exception.getMessage());
	}

	@Test
	public void shouldExposeAllProviderCapabilityBoundaries() {
		Set<TinyEngineAiCapability> expectedCapabilities = new HashSet<TinyEngineAiCapability>(Arrays.asList(
				TinyEngineAiCapability.CHAT,
				TinyEngineAiCapability.STREAMING_CHAT,
				TinyEngineAiCapability.RAG_RETRIEVAL,
				TinyEngineAiCapability.KNOWLEDGE_INDEXING,
				TinyEngineAiCapability.EMBEDDING,
				TinyEngineAiCapability.VECTOR_STORE,
				TinyEngineAiCapability.AGENT_RUNTIME));

		assertEquals(expectedCapabilities, new HashSet<TinyEngineAiCapability>(Arrays.asList(
				TinyEngineAiCapability.values())));
	}

	@Test
	public void shouldKeepTinyEngineDtosAndSpiFreeOfLangChain4jReferences() throws IOException {
		assertFalse(classFileContains(TinyEngineAiProvider.class, "dev/langchain4j"));
		assertFalse(classFileContains(TinyEngineChatRequest.class, "dev/langchain4j"));
		assertFalse(classFileContains(TinyEngineChatMessage.class, "dev/langchain4j"));
		assertFalse(classFileContains(TinyEngineChatResponse.class, "dev/langchain4j"));
		assertFalse(classFileContains(TinyEngineChatStreamChunk.class, "dev/langchain4j"));
	}

	private boolean classFileContains(Class<?> type, String text) throws IOException {
		InputStream inputStream = type.getResourceAsStream(type.getSimpleName() + ".class");
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[256];
			int length;
			while ((length = inputStream.read(buffer)) != -1) {
				outputStream.write(buffer, 0, length);
			}
			return new String(outputStream.toByteArray(), "ISO-8859-1").contains(text);
		} finally {
			inputStream.close();
		}
	}

	private static final class TestProvider implements TinyEngineAiProvider {
		private final String key;
		private final boolean streamingSupported;

		private TestProvider(String key, boolean streamingSupported) {
			this.key = key;
			this.streamingSupported = streamingSupported;
		}

		@Override
		public String getKey() {
			return key;
		}

		@Override
		public boolean supports(TinyEngineAiCapability capability) {
			return capability != TinyEngineAiCapability.STREAMING_CHAT || streamingSupported;
		}

		@Override
		public TinyEngineChatResponse chat(TinyEngineChatRequest request) {
			throw new UnsupportedOperationException();
		}

		@Override
		public List<TinyEngineChatStreamChunk> stream(TinyEngineChatRequest request) {
			throw new UnsupportedOperationException();
		}
	}
}
