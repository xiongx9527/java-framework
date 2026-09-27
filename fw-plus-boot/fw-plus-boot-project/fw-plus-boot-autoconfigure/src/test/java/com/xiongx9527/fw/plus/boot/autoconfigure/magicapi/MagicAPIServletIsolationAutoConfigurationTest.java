package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.controller.MagicAPIController;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.MagicAPIDynamicDataSourceAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.datasource.service.ManagedMagicDynamicDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.filter.auth.AuthConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.filter.auth.AuthFilter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.core.config.MagicAPIProperties;
import org.ssssssss.magicapi.core.config.MagicConfiguration;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceStorage;
import org.ssssssss.magicapi.datasource.model.DataSourceInfo;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class MagicAPIServletIsolationAutoConfigurationTest {

    private final ReactiveWebApplicationContextRunner reactiveContextRunner = new ReactiveWebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(AuthConfiguration.class))
            .withUserConfiguration(MagicApiConfiguration.class);

    private final WebApplicationContextRunner servletContextRunner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(MagicAPIBaseAutoConfiguration.class))
            .withPropertyValues(
                    "magic.api.mybatis-flex.enabled=false",
                    "magic.api.mybatis-plus.enabled=false",
                    "fw.plus.plumelog.enabled=false",
                    "magic-api.plugin.config.enabled=false",
                    "magic-api.plugin.freemarker.enabled=false",
                    "magic-api.plugin.vue3.enabled=false",
                    "magic-api.plugin.swagger.enabled=false",
                    "magic-api.plugin.elasticsearch.enabled=false",
                    "magic-api.plugin.redis.enabled=false",
                    "magic-api.plugin.mogno.enabled=false",
                    "magic-api.plugin.rocketmq.enabled=false",
                    "magic-api.plugin.rabbitmq.enabled=false",
                    "magic-api.plugin.kafka.enabled=false",
                    "magic-api.plugin.pulsar.enabled=false",
                    "magic-api.plugin.db.enabled=false",
                    "magic-api.plugin.debezium.enabled=false",
                    "magic-api.plugin.s3.enabled=false",
                    "magic-api.plugin.task.enabled=false",
                    "magic-api.plugin.dtp.enabled=false",
                    // WS 配置前缀已由 magic-api.plugin.ws 迁移为 fw.plus.ws
                    "fw.plus.ws.enabled=false",
                    "magic-api.plugin.mqtt.enabled=false",
                    "magic-api.plugin.mqttclient.enabled=false",
                    "magic-api.plugin.druid.enabled=false",
                    "magic-api.plugin.influx.enabled=false",
                    "magic-api.plugin.admin.server.enabled=false",
                    "magic-api.plugin.satoken.enabled=false",
                    "magic-api.plugin.mcp.enabled=false")
            .withUserConfiguration(MagicApiConfiguration.class);

    @Test
    void usesManagedDynamicDataSourceByDefault() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MagicAPIAutoConfiguration.class,
                        MagicAPIDynamicDataSourceAutoConfiguration.class))
                .withBean(StandardServletMultipartResolver.class,
                        StandardServletMultipartResolver::new)
                .withBean(RequestMappingHandlerMapping.class,
                        RequestMappingHandlerMapping::new)
                .run(context -> {
                    assertThat(context).hasSingleBean(MagicDynamicDataSource.class);
                    assertThat(context.getBean(MagicDynamicDataSource.class))
                            .isInstanceOf(ManagedMagicDynamicDataSource.class);
                });
    }

    @Test
    void backsOffForAUserProvidedDynamicDataSource() {
        new WebApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        MagicAPIAutoConfiguration.class,
                        MagicAPIDynamicDataSourceAutoConfiguration.class))
                .withBean(StandardServletMultipartResolver.class,
                        StandardServletMultipartResolver::new)
                .withBean(RequestMappingHandlerMapping.class,
                        RequestMappingHandlerMapping::new)
                .withUserConfiguration(UserDynamicDataSourceConfiguration.class)
                .run(context -> {
                    assertThat(context).hasSingleBean(MagicDynamicDataSource.class);
                    assertThat(context.getBean(MagicDynamicDataSource.class))
                            .isSameAs(UserDynamicDataSourceConfiguration.DYNAMIC_DATA_SOURCE);
                });
    }

    @Test
    void excludesServletAuthBeansInReactiveWebContext() {
        reactiveContextRunner.run(context -> assertThat(context)
                .doesNotHaveBean(FilterRegistrationBean.class)
                .doesNotHaveBean(AuthFilter.class));
    }

    @Test
    void retainsServletAuthBeansAndMagicApiControllerInServletWebContext() {
        servletContextRunner.run(context -> assertThat(context)
                .hasSingleBean(FilterRegistrationBean.class)
                .hasSingleBean(AuthFilter.class)
                .hasSingleBean(MagicAPIController.class));
    }

    @Configuration
    static class MagicApiConfiguration {

        @Bean
        MagicAPIProperties magicAPIProperties() {
            return new MagicAPIProperties();
        }

        @Bean
        MagicConfiguration magicConfiguration() {
            return mock(MagicConfiguration.class);
        }

        @Bean
        MagicDynamicDataSource magicDynamicDataSource() {
            return mock(MagicDynamicDataSource.class);
        }

        @Bean
        MagicResourceStorage<DataSourceInfo> dataSourceMagicResourceStorage() {
            return mock(MagicResourceStorage.class);
        }

        @Bean
        Resource resource() {
            return mock(Resource.class);
        }

    }

    @Configuration(proxyBeanMethods = false)
    static class UserDynamicDataSourceConfiguration {

        private static final MagicDynamicDataSource DYNAMIC_DATA_SOURCE =
                new MagicDynamicDataSource();

        @Bean
        MagicDynamicDataSource userMagicDynamicDataSource() {
            return DYNAMIC_DATA_SOURCE;
        }
    }
}
