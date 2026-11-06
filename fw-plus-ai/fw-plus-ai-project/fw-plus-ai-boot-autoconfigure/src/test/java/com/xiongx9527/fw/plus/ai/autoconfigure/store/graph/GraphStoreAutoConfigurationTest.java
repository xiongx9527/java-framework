package com.xiongx9527.fw.plus.ai.autoconfigure.store.graph;

import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GraphStoreAutoConfigurationTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(GraphStoreAutoConfiguration.class));

    @Test
    void exposesUnconfiguredPlaceholderWhenDefaultAbsent() {
        runner.run(context -> {
            assertThat(context).hasSingleBean(GraphStorePort.class);
            GraphStorePort port = context.getBean(GraphStorePort.class);
            assertThat(port.configured()).isFalse();
            assertThat(port.name()).isEqualTo("unconfigured");
        });
    }

    @Test
    void exposesConfiguredNeo4jPortWhenDefaultConfigured() {
        runner.withPropertyValues(
                        "fw-plus.ai.graph-store.default.base-url=bolt://localhost:7687",
                        "fw-plus.ai.graph-store.default.vendor=NEO4J")
                .run(context -> {
                    assertThat(context).hasSingleBean(GraphStorePort.class);
                    GraphStorePort port = context.getBean(GraphStorePort.class);
                    assertThat(port.configured()).isTrue();
                    assertThat(port.name()).isEqualTo("neo4j");
                });
    }

    @Test
    void exposesConfiguredPortViaProtocolRouting() {
        runner.withPropertyValues(
                        "fw-plus.ai.graph-store.default.base-url=bolt://localhost:7687",
                        "fw-plus.ai.graph-store.default.protocol=NEBULA")
                .run(context -> {
                    assertThat(context).hasSingleBean(GraphStorePort.class);
                    GraphStorePort port = context.getBean(GraphStorePort.class);
                    assertThat(port.configured()).isTrue();
                    assertThat(port.name()).isEqualTo("nebula");
                });
    }

    @Test
    void exposesAllInstancesAsMapIncludingDefault() {
        runner.withPropertyValues(
                        "fw-plus.ai.graph-store.default.base-url=bolt://localhost:7687",
                        "fw-plus.ai.graph-store.default.vendor=NEO4J",
                        "fw-plus.ai.graph-store.instances.kg.base-url=bolt://localhost:7688",
                        "fw-plus.ai.graph-store.instances.kg.vendor=NEO4J")
                .run(context -> {
                    assertThat(context).hasBean("graphStorePorts");
                    @SuppressWarnings("unchecked")
                    Map<String, GraphStorePort> ports =
                            (Map<String, GraphStorePort>) context.getBean("graphStorePorts");
                    assertThat(ports).containsKeys("default", "kg");
                    assertThat(ports.get("default").configured()).isTrue();
                    assertThat(ports.get("kg").configured()).isTrue();
                });
    }

    @Test
    void disabledWhenExplicitlyTurnedOff() {
        runner.withPropertyValues("fw-plus.ai.graph-store.enabled=false")
                .run(context -> assertThat(context).doesNotHaveBean(GraphStorePort.class));
    }
}
