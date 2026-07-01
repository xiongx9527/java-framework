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

    @Bean
    public PlumelogFilterService plumelogFilterService() {
        return new PlumelogFilterService();
    }

    @Primary
    @Bean
    public PlumelogTrace plumelogTrace() {
        return new PlumelogTrace();
    }

    @Bean
    public PlumelogAspect plumelogAspect(PlumelogTrace plumelogTrace) {
        return new PlumelogAspect(plumelogTrace);
    }

}