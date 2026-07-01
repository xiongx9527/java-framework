package com.xiongx9527.fw.plus.boot.autoconfigure.trace;

import com.xiongx9527.fw.plus.boot.autoconfigure.trace.opentelemetry.OpentelemetryConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.trace.plumelog.PlumelogConfiguration;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.context.annotation.Configuration;

@Configuration
@ImportAutoConfiguration({
        OpentelemetryConfiguration.class,
        PlumelogConfiguration.class
})
public class TraceAutoConfiguration {
}
