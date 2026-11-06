package com.xiongx9527.fw.plus.ai.autoconfigure.model;

import com.xiongx9527.fw.plus.ai.base.model.RerankPort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RerankModelAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(RerankModelAutoConfiguration.class));

    @Test
    void exposesConfiguredRerankPortWhenProtocolSet() {
        runner.withPropertyValues(
                        "fw-plus.ai.rerank.default.protocol=TEI",
                        "fw-plus.ai.rerank.default.base-url=http://localhost:8080",
                        "fw-plus.ai.rerank.default.api-key=sk-xxx",
                        "fw-plus.ai.rerank.default.default-model=bge-reranker-v2-m3")
                .run(context -> {
                    assertThat(context).hasSingleBean(RerankPort.class);
                    assertThat(context.getBean(RerankPort.class)).isInstanceOf(RerankPort.class);
                });
    }

    @Test
    void exposesUnconfiguredPlaceholderWhenDefaultAbsent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(RerankPort.class);
            assertThat(context.getBean(RerankPort.class))
                    .isInstanceOf(com.xiongx9527.fw.plus.ai.nativeimpl.rerank.UnconfiguredRerankPort.class);
        });
    }

    @Test
    void exposesAllInstancesAsMapIncludingDefault() {
        runner.withPropertyValues(
                        "fw-plus.ai.rerank.default.protocol=TEI",
                        "fw-plus.ai.rerank.default.base-url=http://localhost:8080",
                        "fw-plus.ai.rerank.default.api-key=sk-xxx",
                        "fw-plus.ai.rerank.default.default-model=bge-reranker-v2-m3",
                        "fw-plus.ai.rerank.instances.alt.protocol=TEI",
                        "fw-plus.ai.rerank.instances.alt.base-url=http://localhost:8081",
                        "fw-plus.ai.rerank.instances.alt.api-key=sk-yyy",
                        "fw-plus.ai.rerank.instances.alt.default-model=bge-reranker-v2-m3")
                .run(context -> {
                    assertThat(context).hasBean("rerankPorts");
                    @SuppressWarnings("unchecked")
                    Map<String, RerankPort> ports = (Map<String, RerankPort>) context.getBean("rerankPorts");
                    assertThat(ports).containsKeys("default", "alt");
                });
    }

    @Test
    void disabledWhenExplicitlyTurnedOff() {
        runner.withPropertyValues("fw-plus.ai.rerank.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(RerankPort.class));
    }
}
