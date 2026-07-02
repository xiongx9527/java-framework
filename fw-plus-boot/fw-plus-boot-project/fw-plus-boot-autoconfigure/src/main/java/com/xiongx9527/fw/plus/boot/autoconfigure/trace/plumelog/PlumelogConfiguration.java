package com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.aspect.PlumelogAspect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@ConditionalOnProperty(prefix = "fw.plus.plumelog", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"com.plumelog.logback.appender.LiteAppender"})
@Configuration
public class PlumelogConfiguration {

    /**
     * 创建并注册Plumelog过滤器服务Bean
     *
     * @return PlumelogFilterService实例，用于处理Plumelog相关的过滤器逻辑
     */
    @Bean
    public PlumelogFilterService plumelogFilterService() {
        return new PlumelogFilterService();
    }


    /**
     * 创建并配置PlumelogTrace实例
     *
     * 该方法创建一个PlumelogTrace对象作为Spring容器中的Bean，
     * 并标记为首选Bean（@Primary），用于系统中的链路追踪功能
     *
     * @return 返回新创建的PlumelogTrace实例
     */
    @Primary
    @Bean
    public PlumelogTrace plumelogTrace() {
        return new PlumelogTrace();
    }

    /**
     * 创建Plumelog切面Bean
     * 该方法用于在Spring容器中注册Plumelog切面，用于处理日志追踪相关的AOP操作
     *
     * @param plumelogTrace PlumelogTrace实例，用于提供日志追踪功能
     * @return 返回PlumelogAspect实例，该实例将被Spring容器管理并用于AOP切面处理
     */
    @Bean
    public PlumelogAspect plumelogAspect(PlumelogTrace plumelogTrace) {
        return new PlumelogAspect(plumelogTrace);
    }

}
