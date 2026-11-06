package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.base.model.EmbeddingPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class EmbeddingModelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(EmbeddingModelAutoConfiguration.class));

    @Test
    void exposesConfiguredEmbeddingPortWhenProtocolSet() {
        runner.withPropertyValues(
                        "fw-plus.ai.embedding.default.protocol=OPENAI_COMPATIBLE",
                        "fw-plus.ai.embedding.default.base-url=https://api.openai.com/v1",
                        "fw-plus.ai.embedding.default.api-key=sk-xxx",
                        "fw-plus.ai.embedding.default.default-model=text-embedding-3-small")
                .run(context -> {
                    assertThat(context).hasSingleBean(EmbeddingPort.class);
                    assertThat(context.getBean(EmbeddingPort.class)).isInstanceOf(EmbeddingPort.class);
                });
    }

    @Test
    void exposesUnconfiguredPlaceholderWhenDefaultAbsent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(EmbeddingPort.class);
            assertThat(context.getBean(EmbeddingPort.class))
                    .isInstanceOf(com.xiongx9527.fw.plus.ai.nativeimpl.embedding.UnconfiguredEmbeddingPort.class);
        });
    }

    @Test
    void exposesAllInstancesAsMapIncludingDefault() {
        runner.withPropertyValues(
                        "fw-plus.ai.embedding.default.protocol=OPENAI_COMPATIBLE",
                        "fw-plus.ai.embedding.default.base-url=https://api.openai.com/v1",
                        "fw-plus.ai.embedding.default.api-key=sk-xxx",
                        "fw-plus.ai.embedding.default.default-model=text-embedding-3-small",
                        "fw-plus.ai.embedding.instances.alt.protocol=OPENAI_COMPATIBLE",
                        "fw-plus.ai.embedding.instances.alt.base-url=https://api.openai.com/v1",
                        "fw-plus.ai.embedding.instances.alt.api-key=sk-yyy",
                        "fw-plus.ai.embedding.instances.alt.default-model=text-embedding-3-large")
                .run(context -> {
                    assertThat(context).hasBean("embeddingPorts");
                    @SuppressWarnings("unchecked")
                    Map<String, EmbeddingPort> ports =
                            (Map<String, EmbeddingPort>) context.getBean("embeddingPorts");
                    assertThat(ports).containsKeys("default", "alt");
                });
    }

    @Test
    void disabledWhenExplicitlyTurnedOff() {
        runner.withPropertyValues("fw-plus.ai.embedding.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(EmbeddingPort.class));
    }
}
