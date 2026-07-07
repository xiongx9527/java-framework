package com.xiongx9527.fw.plus.boot.autoconfigure.mate;

import com.xiongx9527.fw.plus.easytrans.service.MateEasyTransTransService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class MateEasyTransAutoConfigurationTest {

    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MateEasyTransAutoConfiguration.class));

    @Test
    void registersDefaultTransService() {
        contextRunner.run(context -> assertThat(context).hasSingleBean(MateEasyTransTransService.class));
    }

    @Test
    void backsOffWhenTransServiceIsProvidedByUser() {
        contextRunner.withUserConfiguration(CustomTransServiceConfiguration.class)
                .run(context -> assertThat(context).hasSingleBean(MateEasyTransTransService.class));
    }

    @Configuration
    static class CustomTransServiceConfiguration {
        @Bean
        MateEasyTransTransService customTransService() {
            return new MateEasyTransTransService();
        }
    }
}
