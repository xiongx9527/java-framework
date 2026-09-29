package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.ssssssss.magicapi.core.service.MagicResourceService;

/**
 * LangChain4j 模型资源插件的父装配：只负责把共享工厂配置与七类资源的子 Configuration
 * 纳入装配。插件标识与连接测试 Controller 的注册分属七类子配置（每类资源一个插件标识，
 * 对应各自的前端产物），Controller 由 {@link Langchain4jChatModelConfiguration} 统一承载；
 * embedding-store 的启动重试在其子配置上声明（仅当 embedding-model 一并装配时存在）。
 *
 * <p>生效条件：magic-api starter 在 classpath、{@link MagicResourceService} bean 已注册、
 * 总开关 {@code fw.plus.magicapi.langchain4j} 未关闭（默认开启）。七类资源各自的装配条件
 * （langchain4j 类与 {@code fw.plus.magicapi.langchain4j.{type}.enabled} 开关）在对应子配置上，
 * 单类型可独立停用而不影响其它类型。</p>
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.ssssssss.magicapi.spring.boot.starter.MagicAPIAutoConfiguration")
@ConditionalOnBean(MagicResourceService.class)
@ConditionalOnProperty(prefix = "fw.plus.magicapi.langchain4j", name = "enabled", havingValue = "true",
        matchIfMissing = true)
@Import({
        Langchain4jModelFactoryConfiguration.class,
        // 顺序敏感：embedding-model 必须先于 embedding-store——后者的类级 @ConditionalOnBean
        // 依赖前者在本批次中先注册的 bean 定义，重排会导致 store 类型静默缺席
        Langchain4jChatModelConfiguration.class,
        Langchain4jStreamingChatModelConfiguration.class,
        Langchain4jLanguageModelConfiguration.class,
        Langchain4jStreamingLanguageModelConfiguration.class,
        Langchain4jEmbeddingModelConfiguration.class,
        Langchain4jTokenizerConfiguration.class,
        Langchain4jEmbeddingStoreConfiguration.class
})
public class Langchain4jDatasourceConfiguration {
}
