package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.modules.trace;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.PlumelogTrace;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(prefix = "fw.plus.plumelog", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnClass(name = {"com.plumelog.logback.appender.LiteAppender"})
@Configuration
public class TraceModuleConfiguration {

    /**
     * 创建并返回一个TraceModule实例
     * 该方法是一个Spring Bean配置方法，用于将TraceModule注册为Spring容器中的Bean
     *
     * @param trace PlumelogTrace实例，用于提供追踪功能的支持
     * @return TraceModule实例，封装了追踪功能的模块对象
     */
    @Bean
    public TraceModule traceModule(PlumelogTrace trace) {
        return new TraceModule(trace);
    }

}
