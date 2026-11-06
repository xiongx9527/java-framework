package com.xiongx9527.fw.plus.ai.autoconfigure.store.vector;

import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class VectorStoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(VectorStoreAutoConfiguration.class));

    @Test
    void exposesUnconfiguredPlaceholderWhenDefaultAbsent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(VectorStorePort.class);
            VectorStorePort port = context.getBean(VectorStorePort.class);
            assertThat(port.configured()).isFalse();
            assertThat(port.name()).isEqualTo("unconfigured");
        });
    }

    @Test
    void exposesConfiguredQdrantPortWhenDefaultConfigured() {
        runner.withPropertyValues(
                        "fw-plus.ai.vector-store.default.base-url=http://localhost:6333",
                        "fw-plus.ai.vector-store.default.vendor=QDRANT")
                .run(context -> {
                    assertThat(context).hasSingleBean(VectorStorePort.class);
                    VectorStorePort port = context.getBean(VectorStorePort.class);
                    assertThat(port.configured()).isTrue();
                    assertThat(port.name()).isEqualTo("qdrant");
                });
    }

    @Test
    void exposesConfiguredPortViaProtocolRouting() {
        runner.withPropertyValues(
                        "fw-plus.ai.vector-store.default.base-url=http://localhost:19530",
                        "fw-plus.ai.vector-store.default.protocol=MILVUS")
                .run(context -> {
                    assertThat(context).hasSingleBean(VectorStorePort.class);
                    VectorStorePort port = context.getBean(VectorStorePort.class);
                    assertThat(port.configured()).isTrue();
                    assertThat(port.name()).isEqualTo("milvus");
                });
    }

    @Test
    void honorsRawPointIdStrategyPropertyOnDefault() {
        runner.withPropertyValues(
                        "fw-plus.ai.vector-store.default.base-url=http://localhost:6333",
                        "fw-plus.ai.vector-store.default.vendor=QDRANT",
                        "fw-plus.ai.vector-store.default.point-id-strategy=RAW")
                .run(context -> {
                    assertThat(context).hasSingleBean(VectorStorePort.class);
                    assertThat(context.getBean(VectorStoreProperties.class).getDefault().getPointIdStrategy())
                            .isEqualTo("RAW");
                });
    }

    @Test
    void exposesAllInstancesAsMapIncludingDefault() {
        runner.withPropertyValues(
                        "fw-plus.ai.vector-store.default.base-url=http://localhost:6333",
                        "fw-plus.ai.vector-store.default.vendor=QDRANT",
                        "fw-plus.ai.vector-store.instances.memory.base-url=http://localhost:6334",
                        "fw-plus.ai.vector-store.instances.memory.vendor=QDRANT")
                .run(context -> {
                    assertThat(context).hasBean("vectorStorePorts");
                    @SuppressWarnings("unchecked")
                    Map<String, VectorStorePort> ports =
                            (Map<String, VectorStorePort>) context.getBean("vectorStorePorts");
                    assertThat(ports).containsKeys("default", "memory");
                    assertThat(ports.get("default").configured()).isTrue();
                    assertThat(ports.get("memory").configured()).isTrue();
                });
    }

    @Test
    void disabledWhenExplicitlyTurnedOff() {
        runner.withPropertyValues("fw-plus.ai.vector-store.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(VectorStorePort.class));
    }
}
