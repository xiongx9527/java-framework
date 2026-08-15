package com.xiongx9527.fw.plus.tinyengine.support.ai;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class OpenAiCompatibleBaseUrlTest {

	@Test
	public void shouldNormalizeHostRootTrailingSlashAndChatCompletionsEndpoint() {
		assertEquals("https://api.openai.com/v1", OpenAiCompatibleBaseUrl.normalize(" https://api.openai.com "));
		assertEquals("https://api.openai.com/v1", OpenAiCompatibleBaseUrl.normalize("https://api.openai.com/v1/"));
		assertEquals("https://api.openai.com/v1",
				OpenAiCompatibleBaseUrl.normalize("https://api.openai.com/v1/chat/completions"));
		assertEquals("https://example.com/openai/v1",
				OpenAiCompatibleBaseUrl.normalize("https://example.com/openai/v1/chat/completions/"));
	}

	@Test
	public void shouldRejectNonHttpOrRequestSpecificUrls() {
		assertThrows(IllegalArgumentException.class, () -> OpenAiCompatibleBaseUrl.normalize("file:///tmp/model"));
		assertThrows(IllegalArgumentException.class,
				() -> OpenAiCompatibleBaseUrl.normalize("https://api.openai.com/v1?token=secret"));
		assertThrows(IllegalArgumentException.class, () -> OpenAiCompatibleBaseUrl.normalize("not-a-url"));
	}
}
