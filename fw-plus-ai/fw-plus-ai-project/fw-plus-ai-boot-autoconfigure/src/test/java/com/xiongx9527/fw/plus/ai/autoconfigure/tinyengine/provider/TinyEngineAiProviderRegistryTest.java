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
	public void shouldUseOnlyProviderAsDefaultProviderKey() {
		TinyEngineAiProviderRegistry registry = new TinyEngineAiProviderRegistry(
				Collections.<TinyEngineAiProvider>singletonList(new TestProvider("business", true)), null);

		assertEquals("business", registry.getDefaultProviderKey());
		assertEquals("business", registry.getDefaultProvider().getKey());
	}

	@Test
	public void shouldRequireConfiguredProviderWhenMultipleProvidersExist() {
		TinyEngineAiProviderRegistry registry = new TinyEngineAiProviderRegistry(Arrays.<TinyEngineAiProvider>asList(
				new TestProvider("business-a", true), new TestProvider("business-b", true)), null);

		IllegalStateException exception = assertThrows(IllegalStateException.class, registry::getDefaultProvider);

		assertEquals("TinyEngine AI provider must be configured when multiple providers are available: business-a,business-b",
				exception.getMessage());
	}

	@Test
	public void shouldPreferConfiguredCustomProvider() {
		TinyEngineAiProviderRegistry registry = new TinyEngineAiProviderRegistry(Arrays.<TinyEngineAiProvider>asList(
				new TestProvider("business", true), new TestProvider("custom", true)), "custom");

		assertEquals("custom", registry.getDefaultProviderKey());
		assertEquals("custom", registry.getDefaultProvider().getKey());
	}

	@Test
	public void shouldReportProviderAndCapabilityForUnsupportedCapability() {
		TinyEngineAiProviderRegistry registry = new TinyEngineAiProviderRegistry(
				Collections.<TinyEngineAiProvider>singletonList(new TestProvider("business", false)), null);

		IllegalStateException exception = assertThrows(IllegalStateException.class,
				() -> registry.requireCapability(TinyEngineAiCapability.STREAMING_CHAT));

		assertEquals("TinyEngine AI provider 'business' does not support capability 'STREAMING_CHAT'",
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
