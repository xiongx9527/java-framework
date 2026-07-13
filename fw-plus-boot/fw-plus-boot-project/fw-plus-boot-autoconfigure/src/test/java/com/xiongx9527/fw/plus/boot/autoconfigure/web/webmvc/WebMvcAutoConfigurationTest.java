package com.xiongx9527.fw.plus.boot.autoconfigure.web.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.context.support.ServletRequestHandledEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

class WebMvcAutoConfigurationTest {

    @Test
    void slowRequestEventIgnoresMissingThresholdLevels() {
        WebProperties webProperties = new WebProperties();
        webProperties.getSlowRequest().setEnabled(true);
        webProperties.getSlowRequest().setThresholdLevels(null);
        WebMvcAutoConfiguration configuration = new WebMvcAutoConfiguration(webProperties);

        assertThatCode(() -> configuration.onApplicationEvent(requestHandledEvent(null)))
                .doesNotThrowAnyException();
    }

    @Test
    void exceptionTypeUsesFailureClassName() {
        WebMvcAutoConfiguration configuration = new WebMvcAutoConfiguration(new WebProperties());

        assertThatCode(() -> configuration.onApplicationEvent(requestHandledEvent(new IllegalStateException("failed"))))
                .doesNotThrowAnyException();
        assertThat(configuration.getExceptionType(new IllegalStateException("failed")))
                .isEqualTo(IllegalStateException.class.getName());
    }

    private ServletRequestHandledEvent requestHandledEvent(Throwable failureCause) {
        return new ServletRequestHandledEvent(
                this,
                "/orders",
                "127.0.0.1",
                "GET",
                "dispatcherServlet",
                null,
                "OK",
                120L,
                failureCause,
                200);
    }
}
