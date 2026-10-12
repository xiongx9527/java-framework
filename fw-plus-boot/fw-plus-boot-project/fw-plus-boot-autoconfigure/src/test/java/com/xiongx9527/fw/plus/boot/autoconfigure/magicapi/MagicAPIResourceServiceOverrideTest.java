package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.resource.MyDefaultMagicResourceService;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.ssssssss.magicapi.core.resource.Resource;
import org.ssssssss.magicapi.core.service.MagicResourceService;
import org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource;
import org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * 校验 servlet 模式下本模块的 {@link MyDefaultMagicResourceService} 确实是容器里
 * 唯一的 {@link MagicResourceService}，即上游 {@code MagicAPIAutoConfiguration}
 * 的同名 bean 已被成功覆盖。
 *
 * <p>背景与排序不变量见 {@link MagicAPIAutoConfigurationOrderTest}。
 * 本测试负责的是覆盖这件事本身在 servlet 上下文里成立：
 * 上游与本模块各自用 {@code @ConditionalOnMissingBean} 声明了同名 bean
 * {@code magicResourceService}，只能存在一个，谁先注册谁生效。
 * 一旦退化，上游的 {@code DefaultMagicResourceService} 会顶替本模块的
 * {@link MyDefaultMagicResourceService}，丢掉它的写锁保护，
 * 并让 {@code MagicAPIController} 的硬转在请求期抛 ClassCastException。</p>
 *
 * <p><b>覆盖范围说明</b>：本测试把上游自动配置一并交给 {@link AutoConfigurations}，
 * 以保证冲突真实存在（只放本模块入口则该测试永远绿）；但它<b>不能</b>守住排序本身——
 * {@code AutoConfigurationSorter} 的输出依赖 classpath 组成，本模块的测试 classpath
 * 恰好是上游在前就会赢的那一侧之外的排列，摘掉排序注解后本测试仍会通过。
 * 排序由 {@link MagicAPIAutoConfigurationOrderTest} 守住，
 * 端到端验证在 {@code fw-plus-ai-magicapi-sample} 上做。</p>
 */
class MagicAPIResourceServiceOverrideTest {

    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    MagicAPIBaseAutoConfiguration.class,
                    MagicAPIAutoConfiguration.class))
            .withPropertyValues(
                    "magic.api.mybatis-flex.enabled=false",
                    "magic.api.mybatis-plus.enabled=false",
                    "fw.plus.plumelog.enabled=false",
                    "fw.plus.ws.enabled=false",
                    "magic-api.plugin.config.enabled=false",
                    "magic-api.plugin.freemarker.enabled=false",
                    "magic-api.plugin.vue3.enabled=false",
                    "magic-api.plugin.swagger.enabled=false",
                    "magic-api.plugin.elasticsearch.enabled=false",
                    "magic-api.plugin.redis.enabled=false",
                    "magic-api.plugin.mongo.enabled=false",
                    "magic-api.plugin.rocketmq.enabled=false",
                    "magic-api.plugin.rabbitmq.enabled=false",
                    "magic-api.plugin.kafka.enabled=false",
                    "magic-api.plugin.pulsar.enabled=false",
                    "magic-api.plugin.db.enabled=false",
                    "magic-api.plugin.debezium.enabled=false",
                    "magic-api.plugin.s3.enabled=false",
                    "magic-api.plugin.task.enabled=false",
                    "magic-api.plugin.dtp.enabled=false",
                    "magic-api.plugin.mqtt.enabled=false",
                    "magic-api.plugin.mqttclient.enabled=false",
                    "magic-api.plugin.druid.enabled=false",
                    "magic-api.plugin.influx.enabled=false",
                    "magic-api.plugin.admin.server.enabled=false",
                    "magic-api.plugin.satoken.enabled=false",
                    "magic-api.plugin.mcp.enabled=false")
            .withUserConfiguration(MagicApiConfiguration.class);

    @Test
    void overridesUpstreamMagicResourceServiceInServletMode() {
        runner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context).hasSingleBean(MagicResourceService.class);
            assertThat(context.getBean(MagicResourceService.class))
                    .isInstanceOf(MyDefaultMagicResourceService.class);
        });
    }

    @Configuration(proxyBeanMethods = false)
    static class MagicApiConfiguration {

        // MagicAPIProperties / MagicResourceStorage 交给上游 @EnableConfigurationProperties 与
        // MagicDynamicRegistryConfiguration 生成：手工再声明会与之构成两个同类型 bean，
        // 导致按类型注入 NoUniqueBeanDefinitionException。
        @Bean
        MagicDynamicDataSource magicDynamicDataSource() {
            return mock(MagicDynamicDataSource.class);
        }

        @Bean
        Resource resource() {
            return mock(Resource.class);
        }

        @Bean
        RequestMappingHandlerMapping requestMappingHandlerMapping() {
            return new RequestMappingHandlerMapping();
        }

        @Bean
        MultipartResolver multipartResolver() {
            return new StandardServletMultipartResolver();
        }
    }
}