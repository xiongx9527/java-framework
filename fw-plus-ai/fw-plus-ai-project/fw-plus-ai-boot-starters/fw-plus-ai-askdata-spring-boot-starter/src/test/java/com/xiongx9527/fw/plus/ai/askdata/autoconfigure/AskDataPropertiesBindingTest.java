package com.xiongx9527.fw.plus.ai.askdata.autoconfigure;

import com.xiongx9527.fw.plus.ai.askdata.AskDataProperties;
import org.junit.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.junit.Assert.assertEquals;

public class AskDataPropertiesBindingTest {

    @Test
    public void shouldBindAskDataProperties() {
        new ApplicationContextRunner()
                .withUserConfiguration(AskDataAutoConfiguration.class)
                .withPropertyValues(
                        "fw.plus.ai.askdata.enabled=true",
                        "fw.plus.ai.askdata.model.default-chat-model=chat-default",
                        "fw.plus.ai.askdata.model.default-embedding-model=embedding-default",
                        "fw.plus.ai.askdata.sql.execute-enabled=false",
                        "fw.plus.ai.askdata.sql.default-max-rows=200",
                        "fw.plus.ai.askdata.conversation.compression-threshold-chars=8000")
                .run(context -> {
                    AskDataProperties properties = context.getBean(AskDataProperties.class);
                    assertEquals("chat-default", properties.getModel().getDefaultChatModel());
                    assertEquals("embedding-default", properties.getModel().getDefaultEmbeddingModel());
                    assertEquals(Integer.valueOf(200), properties.getSql().getDefaultMaxRows());
                    assertEquals(Integer.valueOf(8000), properties.getConversation().getCompressionThresholdChars());
                });
    }
}
