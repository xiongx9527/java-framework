package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AskDataPropertiesBindingTest {

    @Test
    public void shouldBindAskDataProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .withPropertyValues(
                        "fw.plus.ai.askdata.enabled=true",
                        "fw.plus.ai.askdata.model.provider=ollama",
                        "fw.plus.ai.askdata.model.default-chat-model=chat-default",
                        "fw.plus.ai.askdata.model.default-embedding-model=embedding-default",
                        "fw.plus.ai.askdata.model.chat-candidates=chat-default,chat-fallback",
						"fw.plus.ai.askdata.model.embedding-candidates=embedding-default,embedding-fallback",
                        "fw.plus.ai.askdata.model.fallback-enabled=false",
                        "fw.plus.ai.askdata.model.infrastructure-failure-timeout-millis=15000",
                        "fw.plus.ai.askdata.model.request-timeout-millis=12000",
                        "fw.plus.ai.askdata.sql.execute-enabled=false",
                        "fw.plus.ai.askdata.sql.default-max-rows=200",
                        "fw.plus.ai.askdata.sql.query-timeout-seconds=12",
                        "fw.plus.ai.askdata.sql.read-only-connection-required=false",
                        "fw.plus.ai.askdata.flow.multi-question-max-concurrency=6",
                        "fw.plus.ai.askdata.flow.multi-question-timeout-millis=45000",
                        "fw.plus.ai.askdata.conversation.compression-threshold-chars=8000",
                        "fw.plus.ai.askdata.conversation.max-checkpoints-per-conversation=10",
                        "fw.plus.ai.askdata.state.monitoring-log-capacity=20")
                .run(context -> {
                    AskDataProperties properties = context.getBean(AskDataProperties.class);
                    assertEquals("chat-default", properties.getModel().getDefaultChatModel());
                    assertEquals("embedding-default", properties.getModel().getDefaultEmbeddingModel());
                    assertEquals("ollama", properties.getModel().getProvider());
                    assertEquals(java.util.Arrays.asList("chat-default", "chat-fallback"),
                            properties.getModel().getChatCandidates());
					assertEquals(java.util.Arrays.asList("embedding-default", "embedding-fallback"),
							properties.getModel().getEmbeddingCandidates());
                    assertEquals(false, properties.getModel().isFallbackEnabled());
                    assertEquals(Integer.valueOf(15000), properties.getModel().getInfrastructureFailureTimeoutMillis());
                    assertEquals(Integer.valueOf(12000), properties.getModel().getRequestTimeoutMillis());
                    assertEquals(Integer.valueOf(200), properties.getSql().getDefaultMaxRows());
                    assertEquals(Integer.valueOf(12), properties.getSql().getQueryTimeoutSeconds());
                    assertEquals(false, properties.getSql().isReadOnlyConnectionRequired());
                    assertEquals(Integer.valueOf(6), properties.getFlow().getMultiQuestionMaxConcurrency());
                    assertEquals(Integer.valueOf(45000), properties.getFlow().getMultiQuestionTimeoutMillis());
                    assertEquals(Integer.valueOf(8000), properties.getConversation().getCompressionThresholdChars());
                    assertEquals(Integer.valueOf(10), properties.getConversation().getMaxCheckpointsPerConversation());
                    assertEquals(Integer.valueOf(20), properties.getState().getMonitoringLogCapacity());
                });
    }

	@Test
	public void shouldUseOllamaDefaultsWhenModelPropertiesAreNotOverridden() {
		new ApplicationContextRunner()
				.withUserConfiguration(AskDataAutoConfiguration.class)
				.run(context -> {
					AskDataProperties properties = context.getBean(AskDataProperties.class);
					assertEquals("ollama", properties.getModel().getProvider());
					assertEquals("qwen3:32b", properties.getModel().getDefaultChatModel());
					assertEquals("bge-m3", properties.getModel().getDefaultEmbeddingModel());
					assertEquals(java.util.Arrays.asList("qwen3:32b"), properties.getModel().getChatCandidates());
					assertEquals(java.util.Arrays.asList("bge-m3"), properties.getModel().getEmbeddingCandidates());
					assertEquals(Integer.valueOf(30), properties.getSql().getQueryTimeoutSeconds());
					assertEquals(true, properties.getSql().isReadOnlyConnectionRequired());
					assertEquals(Integer.valueOf(4), properties.getFlow().getMultiQuestionMaxConcurrency());
					assertEquals(Integer.valueOf(30000), properties.getFlow().getMultiQuestionTimeoutMillis());
				});
	}
}
