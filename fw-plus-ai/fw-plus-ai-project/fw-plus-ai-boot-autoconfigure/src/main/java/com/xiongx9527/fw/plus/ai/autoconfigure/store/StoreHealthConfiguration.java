package com.xiongx9527.fw.plus.ai.autoconfigure.store;

import com.xiongx9527.fw.plus.ai.base.store.graph.GraphStorePort;
import com.xiongx9527.fw.plus.ai.base.store.vector.VectorStorePort;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 存储后端统一健康检查装配：仅当 Actuator（{@link HealthIndicator}）在 classpath 上、
 * 且对应默认端口 Bean 存在时生效；向量库与图库共用 {@link StoreHealthIndicator}。
 */
@Configuration
@ConditionalOnClass(HealthIndicator.class)
public class StoreHealthConfiguration {

    @Bean
    @ConditionalOnMissingBean(name = "vectorStoreHealthIndicator")
    @ConditionalOnBean(VectorStorePort.class)
    public HealthIndicator vectorStoreHealthIndicator(VectorStorePort port) {
        return new StoreHealthIndicator(port);
    }

    @Bean
    @ConditionalOnMissingBean(name = "graphStoreHealthIndicator")
    @ConditionalOnBean(GraphStorePort.class)
    public HealthIndicator graphStoreHealthIndicator(GraphStorePort port) {
        return new StoreHealthIndicator(port);
    }
}
