package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.factory.ollama;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatRequestParameters;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Langchain4jOllamaModelFactoryTest {

	private final Langchain4jOllamaModelFactory factory = new Langchain4jOllamaModelFactory();

	@Test
	void shouldNotApplyStreamingThinkingOptionsToChatModel() throws Exception {
		OllamaChatModel modelWithOptions = factory.buildChatModel(
				reader("langchain4j-chat-model", options()), Collections.emptyList());
		OllamaChatModel modelWithDefaults = factory.buildChatModel(
				reader("langchain4j-chat-model", Collections.<String, Object>emptyMap()), Collections.emptyList());

		assertThat(field(modelWithOptions, "returnThinking"))
				.isEqualTo(field(modelWithDefaults, "returnThinking"));
		OllamaChatRequestParameters configured =
				(OllamaChatRequestParameters) field(modelWithOptions, "defaultRequestParameters");
		OllamaChatRequestParameters defaults =
				(OllamaChatRequestParameters) field(modelWithDefaults, "defaultRequestParameters");
		assertThat(configured.think()).isEqualTo(defaults.think());
	}

	@Test
	void shouldPassThinkingOptionsToStreamingChatModel() throws Exception {
		OllamaStreamingChatModel model = factory.buildStreamingChatModel(
				reader("langchain4j-streaming-chat-model", options()), Collections.emptyList());

		assertThinkingOptions(model);
	}

	@Test
	void shouldKeepSdkThinkingDefaultsWhenStreamingOptionsAreUnset() throws Exception {
		OllamaStreamingChatModel model = factory.buildStreamingChatModel(
				reader("langchain4j-streaming-chat-model", Collections.<String, Object>emptyMap()),
				Collections.emptyList());
		OllamaStreamingChatModel sdkDefaults = OllamaStreamingChatModel.builder()
				.baseUrl("http://localhost:11434")
				.modelName("qwen2.5")
				.build();

		assertThat(field(model, "returnThinking")).isEqualTo(field(sdkDefaults, "returnThinking"));
		OllamaChatRequestParameters parameters =
				(OllamaChatRequestParameters) field(model, "defaultRequestParameters");
		OllamaChatRequestParameters defaultParameters =
				(OllamaChatRequestParameters) field(sdkDefaults, "defaultRequestParameters");
		assertThat(parameters.think()).isEqualTo(defaultParameters.think());
	}

	private static void assertThinkingOptions(Object model) throws Exception {
		assertThat(field(model, "returnThinking")).isEqualTo(true);
		OllamaChatRequestParameters parameters =
				(OllamaChatRequestParameters) field(model, "defaultRequestParameters");
		assertThat(parameters.think()).isTrue();
	}

	private static Langchain4jOptionsReader reader(String type, Map<String, Object> options) {
		return new Langchain4jOptionsReader(type, "ollama-main", options);
	}

	private static Map<String, Object> options() {
		Map<String, Object> options = new LinkedHashMap<String, Object>();
		options.put("think", true);
		options.put("return-thinking", true);
		return options;
	}

	private static Object field(Object target, String name) throws Exception {
		Class<?> type = target.getClass();
		while (type != null) {
			try {
				Field field = type.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(target);
			} catch (NoSuchFieldException e) {
				type = type.getSuperclass();
			}
		}
		throw new NoSuchFieldException(name);
	}
}
