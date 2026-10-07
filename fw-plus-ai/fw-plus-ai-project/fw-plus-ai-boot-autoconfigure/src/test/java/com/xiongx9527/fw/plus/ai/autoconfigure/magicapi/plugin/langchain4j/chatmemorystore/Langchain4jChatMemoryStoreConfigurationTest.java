package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.chatmemorystore.model.ChatMemoryStoreProviderFactory;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource;
import org.elasticsearch.client.RestClient;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.FilteredClassLoader;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import java.lang.reflect.Method;
import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 回归：chat-memory-store 装配链不得因 optional 依赖缺失而崩溃。
 *
 * <p>曾出现的真实故障：{@code chatMemoryStoreProviderFactory} 的 {@code @Bean} 签名声明
 * {@code ObjectProvider<StringRedisTemplate>}，Spring 创建该 Bean 时反射解析泛型参数
 * {@code T}，使用方 classpath 无 spring-data-redis（autoconfigure 里为 optional、
 * 不传递）即抛 {@code TypeNotPresentException: StringRedisTemplate not present}，
 * 导致整个应用启动失败（fw-plus-ai-magicapi-sample 启动崩溃）。</p>
 *
 * <p>修复约定：可选客户端（JdbcTemplate / StringRedisTemplate / MongoTemplate /
 * ElasticsearchClient / RestClient）与 magic 插件持有者（MagicDynamic*DataSource、
 * MagicDynamicDataSource）不得出现在 {@code @Bean} 方法/构造器签名里，一律经
 * {@link org.springframework.context.ApplicationContext} 按需懒解析。本测试用
 * {@link FilteredClassLoader} 隐藏这些类复现缺类场景。</p>
 */
public class Langchain4jChatMemoryStoreConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(Langchain4jChatMemoryStoreConfiguration.class);

    /** 可选客户端类全部缺失时，工厂 Bean 必须照常创建（原故障场景）。 */
    @Test
    public void factoryCreatedWhenOptionalClientClassesMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(
                        StringRedisTemplate.class, MongoTemplate.class, JdbcTemplate.class,
                        ElasticsearchClient.class, RestClient.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ChatMemoryStoreProviderFactory.class);
                });
    }

    /** boot 层插件持有者（fw-plus-boot-autoconfigure 亦为 optional）缺失时同样不能崩。 */
    @Test
    public void factoryCreatedWhenMagicPluginHolderClassesMissing() {
        contextRunner
                .withClassLoader(new FilteredClassLoader(
                        MagicDynamicRedisDataSource.class, MagicDynamicMongoDataSource.class,
                        MagicDynamicElasticSearchDataSource.class))
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context).hasSingleBean(ChatMemoryStoreProviderFactory.class);
                });
    }

    /** 签名约束本身：配置类的 @Bean 方法与工厂类（bean）的方法描述符不得引用 optional 类型（防回归）。 */
    @Test
    public void beanMethodSignaturesMustNotReferenceOptionalDependencyTypes() {
        String[] forbidden = {
                "org.springframework.data.redis.core.StringRedisTemplate",
                "org.springframework.data.redis.connection.RedisConnectionFactory",
                "org.springframework.data.mongodb.core.MongoTemplate",
                "org.springframework.jdbc.core.JdbcTemplate",
                "co.elastic.clients.elasticsearch.ElasticsearchClient",
                "org.elasticsearch.client.RestClient",
                "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.redis.model.MagicDynamicRedisDataSource",
                "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.mongo.model.MagicDynamicMongoDataSource",
                "com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.elasticsearch.model.MagicDynamicElasticSearchDataSource",
                "org.ssssssss.magicapi.datasource.model.MagicDynamicDataSource"
        };
        for (Method method : Langchain4jChatMemoryStoreConfiguration.class.getDeclaredMethods()) {
            if (method.getAnnotation(Bean.class) == null) {
                continue;
            }
            // 解析泛型签名即原崩溃触发点（Method#getGenericParameterTypes），此处不能抛 TypeNotPresentException
            for (Type type : method.getGenericParameterTypes()) {
                String typeName = type.getTypeName();
                for (String banned : forbidden) {
                    assertThat(typeName)
                            .as("@Bean 方法 %s 的参数不得引用 optional 依赖类型", method.getName())
                            .doesNotContain(banned);
                }
            }
        }
        // 工厂类是 bean：Spring 启动期对其调用 Class#getDeclaredMethods()，会解析全部方法的
        // 擦除描述符（参数/返回类型，含 lambda 合成方法），引用缺失类即 NoClassDefFoundError。
        // 本断言即原第二次崩溃点（buildStringRedisTemplate(RedisConnectionFactory)）的回归防线。
        for (Method method : ChatMemoryStoreProviderFactory.class.getDeclaredMethods()) {
            for (Type type : method.getGenericParameterTypes()) {
                String typeName = type.getTypeName();
                for (String banned : forbidden) {
                    assertThat(typeName)
                            .as("工厂方法 %s 的参数不得引用 optional 依赖类型", method.getName())
                            .doesNotContain(banned);
                }
            }
            String returnTypeName = method.getGenericReturnType().getTypeName();
            for (String banned : forbidden) {
                assertThat(returnTypeName)
                        .as("工厂方法 %s 的返回类型不得引用 optional 依赖类型", method.getName())
                        .doesNotContain(banned);
            }
        }
    }
}
