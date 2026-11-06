package com.xiongx9527.fw.plus.ai.autoconfigure.memory;

import com.xiongx9527.fw.plus.ai.base.memory.ConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.InMemoryConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.Mem0ServerConversationMemoryPort;
import com.xiongx9527.fw.plus.ai.nativeimpl.memory.UnconfiguredConversationMemoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConversationMemoryAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(ConversationMemoryAutoConfiguration.class));

    @Test
    void exposesUnconfiguredPlaceholderWhenDefaultAbsent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(ConversationMemoryPort.class);
            assertThat(context.getBean(ConversationMemoryPort.class))
                    .isInstanceOf(UnconfiguredConversationMemoryPort.class);
        });
    }

    @Test
    void exposesConfiguredMem0PortWhenDefaultConfigured() {
        runner.withPropertyValues(
                        "fw-plus.ai.memory.default.protocol=MEM0",
                        "fw-plus.ai.memory.default.base-url=http://localhost:8888",
                        "fw-plus.ai.memory.default.api-key=secret")
                .run(context -> {
                    assertThat(context).hasSingleBean(ConversationMemoryPort.class);
                    assertThat(context.getBean(ConversationMemoryPort.class))
                            .isInstanceOf(Mem0ServerConversationMemoryPort.class);
                });
    }

    @Test
    void exposesInMemoryPortWhenProtocolIsInMemory() {
        runner.withPropertyValues("fw-plus.ai.memory.default.protocol=IN_MEMORY")
                .run(context -> {
                    assertThat(context).hasSingleBean(ConversationMemoryPort.class);
                    assertThat(context.getBean(ConversationMemoryPort.class))
                            .isInstanceOf(InMemoryConversationMemoryPort.class);
                });
    }

    @Test
    void exposesAllInstancesAsMapIncludingDefault() {
        runner.withPropertyValues(
                        "fw-plus.ai.memory.default.protocol=IN_MEMORY",
                        "fw-plus.ai.memory.instances.mem0.protocol=MEM0",
                        "fw-plus.ai.memory.instances.mem0.base-url=http://localhost:8888",
                        "fw-plus.ai.memory.instances.mem0.api-key=secret")
                .run(context -> {
                    assertThat(context).hasBean("conversationMemoryPorts");
                    @SuppressWarnings("unchecked")
                    Map<String, ConversationMemoryPort> ports =
                            (Map<String, ConversationMemoryPort>) context.getBean("conversationMemoryPorts");
                    assertThat(ports).containsKeys("default", "mem0");
                    assertThat(ports.get("default")).isInstanceOf(InMemoryConversationMemoryPort.class);
                    assertThat(ports.get("mem0")).isInstanceOf(Mem0ServerConversationMemoryPort.class);
                });
    }

    @Test
    void disabledWhenExplicitlyTurnedOff() {
        runner.withPropertyValues("fw-plus.ai.memory.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(ConversationMemoryPort.class));
    }
}
