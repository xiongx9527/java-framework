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
                        "fw.plus.ai.askdata.model.fallback-enabled=false",
                        "fw.plus.ai.askdata.model.infrastructure-failure-timeout-millis=15000",
                        "fw.plus.ai.askdata.sql.execute-enabled=false",
                        "fw.plus.ai.askdata.sql.default-max-rows=200",
                        "fw.plus.ai.askdata.conversation.compression-threshold-chars=8000")
                .run(context -> {
                    AskDataProperties properties = context.getBean(AskDataProperties.class);
                    assertEquals("chat-default", properties.getModel().getDefaultChatModel());
                    assertEquals("embedding-default", properties.getModel().getDefaultEmbeddingModel());
                    assertEquals("ollama", properties.getModel().getProvider());
                    assertEquals(java.util.Arrays.asList("chat-default", "chat-fallback"),
                            properties.getModel().getChatCandidates());
                    assertEquals(false, properties.getModel().isFallbackEnabled());
                    assertEquals(Integer.valueOf(15000), properties.getModel().getInfrastructureFailureTimeoutMillis());
                    assertEquals(Integer.valueOf(200), properties.getSql().getDefaultMaxRows());
                    assertEquals(Integer.valueOf(8000), properties.getConversation().getCompressionThresholdChars());
                });
    }
}
