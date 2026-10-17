package com.xiongx9527.fw.plus.boot.autoconfigure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.sensitive.SensitiveAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.webflux.WebFluxAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.webmvc.WebMvcAutoConfiguration;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import com.xiongx9527.fw.plus.common.databind.ISensitiveStrategy;
import com.xiongx9527.fw.plus.common.strategy.SensitiveStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.ser.jdk.JavaUtilDateSerializer;
import tools.jackson.databind.ser.std.ToStringSerializer;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.TimeZone;

@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "fw.plus.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
@Configuration
@Import({SensitiveAutoConfiguration.class, WebMvcAutoConfiguration.class, WebFluxAutoConfiguration.class})
public class WebAutoConfiguration {

    /**
     * 配置并返回一个MyHttpEncryptorProperties bean
     * 该bean用于配置HTTP加密相关的属性
     *
     * @return MyHttpEncryptorProperties实例，包含从配置文件中映射的HTTP加密属性
     *
     * 使用 @Bean 注解声明该方法返回一个Spring上下文中的bean
     * 使用 @ConfigurationProperties 注解指定配置属性的前缀，以便Spring Boot自动绑定配置文件中的属性到该bean
     * 使用 @ConditionalOnProperty 注解根据配置文件中的属性条件来决定是否创建该bean
     */
    @Bean
    @ConfigurationProperties(
            prefix = "fw.plus.web.http-encrypt"
    )
    @ConditionalOnProperty(prefix = "fw.plus.web.http-encrypt", name = "enabled", havingValue = "true", matchIfMissing = false)
    public MyHttpEncryptorProperties myHttpEncryptorProperties() {
        return new MyHttpEncryptorProperties();
    }


    /**
     * 创建并配置敏感信息处理策略 bean
     * 该方法利用 Spring 框架的 @Bean 注解来定义一个 bean，
     * 并使用 @ConditionalOnMissingBean 条件注解来确保只有在当前应用上下文中
     * 尚未定义相同类型的 bean 时，才创建一个新的敏感信息处理策略 bean
     *
     * @return ISensitiveStrategy 实现类的实例，用于处理敏感信息
     */
    @Bean
    @ConditionalOnMissingBean
    public ISensitiveStrategy sensitiveStrategy() {
        return new SensitiveStrategy();
    }

    /**
     * Jackson 3（Spring Boot 4 默认 JSON 栈）定制器配置。
     *
     * <p>Spring Boot 4 起 Jackson 2 定制器接口 {@code Jackson2ObjectMapperBuilderCustomizer}
     * 已被移除，统一迁移到 Jackson 3 的 {@link JsonMapperBuilderCustomizer}
     * （基于 tools.jackson 命名空间，databind 已内建 java.time 支持）。</p>
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(JsonMapperBuilderCustomizer.class)
    static class JacksonCustomizerConfiguration {

        @Value("${fw.plus.web.timezone:GMT+8}")
        private String timezone;

        /**
         * 数字类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_numbers_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeNumbersAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(Number.class, ToStringSerializer.instance));
        }

        /**
         * Long类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_longs_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeLongsAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(Long.class, ToStringSerializer.instance)
                    .addSerializer(Long.TYPE, ToStringSerializer.instance));
        }

        /**
         * BigDecimal类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_bigdecimals_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeBigDecimalsAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(BigDecimal.class, ToStringSerializer.instance));
        }

        /**
         * Integer类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_integers_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeIntegersAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(Integer.class, ToStringSerializer.instance)
                    .addSerializer(Integer.TYPE, ToStringSerializer.instance));
        }

        /**
         * Float类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_floats_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeFloatsAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(Float.class, ToStringSerializer.instance)
                    .addSerializer(Float.TYPE, ToStringSerializer.instance));
        }

        /**
         * Double类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_doubles_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeDoublesAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(Double.class, ToStringSerializer.instance)
                    .addSerializer(Double.TYPE, ToStringSerializer.instance));
        }

        /**
         * Byte类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_bytes_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeBytesAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(Byte.class, ToStringSerializer.instance)
                    .addSerializer(Byte.TYPE, ToStringSerializer.instance));
        }

        /**
         * Short类型转字符串
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_shorts_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeShortsAsStringsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.addModule(new SimpleModule()
                    .addSerializer(Short.class, ToStringSerializer.instance)
                    .addSerializer(Short.TYPE, ToStringSerializer.instance));
        }

        /**
         * Date类型转时间戳
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_dates_as_timestamps", name = "enabled", havingValue = "true", matchIfMissing = false)
        public JsonMapperBuilderCustomizer writeDatesAsTimestampsJackson2ObjectMapperBuilderCustomizer() {
            return builder -> {
                // 1. 配置时区
                builder.defaultTimeZone(TimeZone.getTimeZone(timezone));
                ZoneId zoneId = ZoneId.of(timezone);

                // 2. 自定义 java.time 序列化器（Jackson 3 databind 已内建 java.time 支持，直接注册覆盖）
                SimpleModule module = new SimpleModule();

                // LocalDateTime -> 时间戳
                module.addSerializer(LocalDateTime.class, new ValueSerializer<LocalDateTime>() {
                    @Override
                    public void serialize(LocalDateTime value, JsonGenerator gen, SerializationContext serializers) {
                        if (value != null) {
                            gen.writeNumber(value.atZone(zoneId).toInstant().toEpochMilli());
                        }
                    }
                });

                // LocalDate -> 时间戳
                module.addSerializer(LocalDate.class, new ValueSerializer<LocalDate>() {
                    @Override
                    public void serialize(LocalDate value, JsonGenerator gen, SerializationContext serializers) {
                        if (value != null) {
                            gen.writeNumber(value.atStartOfDay(zoneId).toInstant().toEpochMilli());
                        }
                    }
                });

                // LocalTime -> 时间戳
                module.addSerializer(LocalTime.class, new ValueSerializer<LocalTime>() {
                    @Override
                    public void serialize(LocalTime value, JsonGenerator gen, SerializationContext serializers) {
                        if (value != null) {
                            gen.writeNumber(value.atDate(LocalDate.now(zoneId)).atZone(zoneId).toInstant().toEpochMilli());
                        }
                    }
                });

                // Date 使用标准时间戳序列化
                module.addSerializer(Date.class, new JavaUtilDateSerializer(Boolean.TRUE, null));

                // 3. 注册模块
                builder.addModule(module);

                // 4. 关键：必须禁用 WRITE_DATES_AS_TIMESTAMPS
                // 否则 LocalDateTime 会被序列化为数组格式
                builder.disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS);
            };
        }

        /**
         * 配置 Jackson 忽略 null 值和空值
         * @return JsonMapperBuilderCustomizer实例
         */
        @Bean
        @ConditionalOnProperty(prefix = "fw.plus.web.jackson.ignore_null_and_empty_values", name = "enabled", havingValue = "true", matchIfMissing = false)
        @ConditionalOnMissingBean(name = "ignoreNullAndEmptyValuesJackson2ObjectMapperBuilderCustomizer")
        public JsonMapperBuilderCustomizer ignoreNullAndEmptyValuesJackson2ObjectMapperBuilderCustomizer() {
            return builder -> builder.changeDefaultPropertyInclusion(inclusion ->
                    inclusion.withValueInclusion(JsonInclude.Include.NON_EMPTY));
        }
    }

}
