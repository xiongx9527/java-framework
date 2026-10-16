package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.facts;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.envelope.Langchain4jAssistantProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.ConfigurationPropertySources;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 任务 4.2：使用文档（{@code docs/openspec/changes/langchain4j-fact-memory/usage.md}）中的
 * 配置样例必须真实可加载——本测试以内嵌的同一份 YAML 走 Spring Boot 绑定并全字段断言，
 * 防止文档写出无法绑定的配置。
 */
class FactMemoryUsageSampleConfigTest {

	/** 与 usage.md「配置样例」一节保持同步 */
	private static final String SAMPLE_YAML = ""
			+ "fw.plus.ai.langchain4j.assistant:\n"
			+ "  session:\n"
			+ "    enabled: true\n"
			+ "    facts:\n"
			+ "      enabled: true\n"
			+ "      base-url: http://localhost:8000\n"
			+ "      api-key: \"\"\n"
			+ "      connect-timeout: 5s\n"
			+ "      read-timeout: 10s\n"
			+ "      extractor: hybrid\n"
			+ "      write-mode: batched\n"
			+ "      batch-size: 3\n"
			+ "      retriever: rerank\n"
			+ "      top-k: 8\n"
			+ "      max-facts-tokens: 800\n"
			+ "      extract-async: false\n"
			+ "      extract-model: my-chat-model\n"
			+ "      rerank-model: my-scoring-model\n";

	@Test
	void sampleYamlLoadsAndValidates() throws IOException {
		YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
		Resource resource = new ByteArrayResource(SAMPLE_YAML.getBytes(StandardCharsets.UTF_8));

		List<PropertySource<?>> sources = loader.load("usage-sample", resource);
		Langchain4jAssistantProperties properties = new Binder(ConfigurationPropertySources.from(sources))
				.bind(Langchain4jAssistantProperties.PREFIX, Langchain4jAssistantProperties.class)
				.get();

		assertThat(properties.getSession().isEnabled()).isTrue();
		Langchain4jAssistantProperties.Session.Facts facts = properties.getSession().getFacts();
		assertThat(facts.isEnabled()).isTrue();
		assertThat(facts.getBaseUrl()).isEqualTo("http://localhost:8000");
		assertThat(facts.getApiKey()).isEmpty();
		assertThat(facts.getConnectTimeout()).isEqualTo(Duration.ofSeconds(5));
		assertThat(facts.getReadTimeout()).isEqualTo(Duration.ofSeconds(10));
		assertThat(facts.getExtractor()).isEqualTo("hybrid");
		assertThat(facts.getWriteMode()).isEqualTo("batched");
		assertThat(facts.getBatchSize()).isEqualTo(3);
		assertThat(facts.getRetriever()).isEqualTo("rerank");
		assertThat(facts.getTopK()).isEqualTo(8);
		assertThat(facts.getMaxFactsTokens()).isEqualTo(800);
		assertThat(facts.isExtractAsync()).isFalse();
		assertThat(facts.getExtractModel()).isEqualTo("my-chat-model");
		assertThat(facts.getRerankModel()).isEqualTo("my-scoring-model");

		// 样例必须是合法配置（非法值在启动校验中快速失败）
		facts.validate();
	}

}
