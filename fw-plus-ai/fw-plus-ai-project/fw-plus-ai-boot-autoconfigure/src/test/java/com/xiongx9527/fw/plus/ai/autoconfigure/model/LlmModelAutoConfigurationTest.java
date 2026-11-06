package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class LlmModelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(LlmModelAutoConfiguration.class));

    @Test
    void exposesConfiguredLlmPortWhenProtocolSet() {
        runner.withPropertyValues(
                        "fw-plus.ai.llm.default.protocol=OPENAI_CHAT_COMPLETIONS",
                        "fw-plus.ai.llm.default.base-url=https://api.openai.com/v1",
                        "fw-plus.ai.llm.default.api-key=sk-xxx",
                        "fw-plus.ai.llm.default.default-model=gpt-4o")
                .run(context -> {
                    assertThat(context).hasSingleBean(LlmPort.class);
                    assertThat(context.getBean(LlmPort.class)).isInstanceOf(LlmPort.class);
                });
    }

    @Test
    void exposesUnconfiguredPlaceholderWhenDefaultAbsent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(LlmPort.class);
            assertThat(context.getBean(LlmPort.class))
                    .isInstanceOf(com.xiongx9527.fw.plus.ai.nativeimpl.llm.UnconfiguredLlmPort.class);
        });
    }

    @Test
    void exposesAllInstancesAsMapIncludingDefault() {
        runner.withPropertyValues(
                        "fw-plus.ai.llm.default.protocol=OPENAI_CHAT_COMPLETIONS",
                        "fw-plus.ai.llm.default.base-url=https://api.openai.com/v1",
                        "fw-plus.ai.llm.default.api-key=sk-xxx",
                        "fw-plus.ai.llm.default.default-model=gpt-4o",
                        "fw-plus.ai.llm.instances.fallback.protocol=OPENAI_CHAT_COMPLETIONS",
                        "fw-plus.ai.llm.instances.fallback.base-url=https://api.openai.com/v1",
                        "fw-plus.ai.llm.instances.fallback.api-key=sk-yyy",
                        "fw-plus.ai.llm.instances.fallback.default-model=gpt-4o-mini")
                .run(context -> {
                    assertThat(context).hasBean("llmPorts");
                    @SuppressWarnings("unchecked")
                    Map<String, LlmPort> ports = (Map<String, LlmPort>) context.getBean("llmPorts");
                    assertThat(ports).containsKeys("default", "fallback");
                });
    }

    @Test
    void disabledWhenExplicitlyTurnedOff() {
        runner.withPropertyValues("fw-plus.ai.llm.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(LlmPort.class));
    }
}
