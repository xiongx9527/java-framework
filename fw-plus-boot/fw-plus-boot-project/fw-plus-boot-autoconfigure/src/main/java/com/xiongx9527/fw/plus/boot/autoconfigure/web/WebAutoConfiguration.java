package com.xiongx9527.fw.plus.boot.autoconfigure.web;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.properties.WebProperties;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.sensitive.SensitiveAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.webflux.WebFluxAutoConfiguration;
import com.xiongx9527.fw.plus.boot.autoconfigure.web.webmvc.WebMvcAutoConfiguration;
import com.xiongx9527.fw.plus.boot.common.web.MyHttpEncryptorProperties;
import com.xiongx9527.fw.plus.common.databind.ISensitiveStrategy;
import com.xiongx9527.fw.plus.common.strategy.SensitiveStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.*;
import java.util.Date;
import java.util.TimeZone;

@EnableConfigurationProperties(WebProperties.class)
@ConditionalOnProperty(prefix = "fw.plus.web", name = "enabled", havingValue = "true", matchIfMissing = true)
@ConditionalOnWebApplication
@Configuration
@Import({SensitiveAutoConfiguration.class, WebMvcAutoConfiguration.class, WebFluxAutoConfiguration.class})
public class WebAutoConfiguration {

    @Value("${fw.plus.web.timezone:GMT+8}")
    private String timezone ;

    /**
     * 数字类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_numbers_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeNumbersAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Number.class, ToStringSerializer.instance);
    }

    /**
     * Long类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_longs_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeLongsAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Long.class, ToStringSerializer.instance)
                .serializerByType(Long.TYPE, ToStringSerializer.instance);
    }

    /**
     * BigDecimal类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_bigdecimas_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeBigDecimalsAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(BigDecimal.class, ToStringSerializer.instance);
    }

    /**
     * Integer类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_integers_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeIntegersAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Integer.class, ToStringSerializer.instance)
                .serializerByType(Integer.TYPE, ToStringSerializer.instance);
    }

    /**
     * Float类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_floats_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeFloatsAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Float.class, ToStringSerializer.instance)
                .serializerByType(Float.TYPE, ToStringSerializer.instance);
    }

    /**
     * Double类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_doubles_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeDoublesAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Double.class, ToStringSerializer.instance)
                .serializerByType(Double.TYPE, ToStringSerializer.instance);
    }

    /**
     * Byte类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.write_bytes_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeBytesAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Byte.class, ToStringSerializer.instance)
                .serializerByType(Byte.TYPE, ToStringSerializer.instance);
    }

    /**
     * Short类型转字符串
     * @return
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.write_shorts_as_strings", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeShortsAsStringsJackson2ObjectMapperBuilderCustomizer() {
        return jacksonObjectMapperBuilder -> jacksonObjectMapperBuilder
                .serializerByType(Short.class, ToStringSerializer.instance)
                .serializerByType(Short.TYPE, ToStringSerializer.instance);
    }

    /**
     * Date类型转时间戳
     * @return Jackson2ObjectMapperBuilderCustomizer实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.write_dates_as_timestamps", name = "enabled", havingValue = "true", matchIfMissing = false)
    public Jackson2ObjectMapperBuilderCustomizer writeDatesAsTimestampsJackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 1. 配置时区
            builder.timeZone(TimeZone.getTimeZone(timezone));
            ZoneId zoneId = ZoneId.of(timezone);

            // 2. 自定义 JavaTimeModule
            JavaTimeModule javaTimeModule = new JavaTimeModule();

            // LocalDateTime -> 时间戳
            javaTimeModule.addSerializer(LocalDateTime.class, new JsonSerializer<LocalDateTime>() {
                @Override
                public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers)
                        throws IOException {
                    if (value != null) {
                        gen.writeNumber(value.atZone(zoneId).toInstant().toEpochMilli());
                    }
                }
            });

            // LocalDate -> 时间戳
            javaTimeModule.addSerializer(LocalDate.class, new JsonSerializer<LocalDate>() {
                @Override
                public void serialize(LocalDate value, JsonGenerator gen, SerializerProvider serializers)
                        throws IOException {
                    if (value != null) {
                        gen.writeNumber(value.atStartOfDay(zoneId).toInstant().toEpochMilli());
                    }
                }
            });

            // LocalTime -> 时间戳
            javaTimeModule.addSerializer(LocalTime.class, new JsonSerializer<LocalTime>() {
                @Override
                public void serialize(LocalTime value, JsonGenerator gen, SerializerProvider serializers)
                        throws IOException {
                    if (value != null) {
                        gen.writeNumber(value.atDate(LocalDate.now()).atZone(zoneId).toInstant().toEpochMilli());
                    }
                }
            });

            // 3. 注册模块
            builder.modules(javaTimeModule);

            // 4. Date 使用标准时间戳序列化
            builder.serializerByType(Date.class, new DateSerializer(true, null));

            // 5. 关键：必须禁用 WRITE_DATES_AS_TIMESTAMPS
            // 否则 LocalDateTime 会被序列化为数组格式
            builder.featuresToDisable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        };
    }

    /**
     * 配置 Jackson 忽略 null 值和空值
     * @return Jackson2ObjectMapperBuilderCustomizer实例
     */
    @Bean
    @ConditionalOnProperty(prefix = "fw.plus.web.jackson.ignore_null_and_empty_values", name = "enabled", havingValue = "true", matchIfMissing = false)
    @ConditionalOnMissingBean(name = "ignoreNullAndEmptyValuesJackson2ObjectMapperBuilderCustomizer")
    public Jackson2ObjectMapperBuilderCustomizer ignoreNullAndEmptyValuesJackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            // 忽略 null 值
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
            // 忽略空字符串
            builder.serializationInclusion(JsonInclude.Include.NON_EMPTY);
        };
    }

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

}
