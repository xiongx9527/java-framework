package com.xiongx9527.fw.plus.boot.autoconfigure.mate.webmvc;

import com.xiongx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator.CommonsValidatorRequestBodyAdvice;
import com.xiongx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator.CommonsValidatorResponseBodyAdvice;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MateWebMvcValidatorAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MateWebMvcValidatorAutoConfiguration.class));

    @Test
    void registersDefaultAdvicesInServletWebContext() {
        contextRunner.run(context -> assertThat(context)
                .hasSingleBean(CommonsValidatorRequestBodyAdvice.class)
                .hasSingleBean(CommonsValidatorResponseBodyAdvice.class));
    }

    @Test
    void backsOffWhenRequestAdviceIsProvidedByUser() {
        contextRunner.withUserConfiguration(CustomRequestAdviceConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(CommonsValidatorRequestBodyAdvice.class));
    }

    @Test
    void backsOffWhenResponseAdviceIsProvidedByUser() {
        contextRunner.withUserConfiguration(CustomResponseAdviceConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(CommonsValidatorResponseBodyAdvice.class));
    }

    @Configuration
    static class CustomRequestAdviceConfiguration {
        @Bean
        CommonsValidatorRequestBodyAdvice customRequestAdvice() {
            return new CommonsValidatorRequestBodyAdvice();
        }
    }

    @Configuration
    static class CustomResponseAdviceConfiguration {
        @Bean
        CommonsValidatorResponseBodyAdvice customResponseAdvice() {
            return new CommonsValidatorResponseBodyAdvice();
        }
    }
}
