package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.model.embedding.EmbeddingModel;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 意图识别自动配置：注册 {@link IntentClassifierFactory} 与 {@link IntentProperties}。
 *
 * <p>不预置具体意图分类器 Bean —— 意图枚举由使用方定义，工厂在调用时按使用方的枚举类型构建即可。
 * 容器中若存在 {@link EmbeddingModel} 或 {@link IntentLlm}，工厂会自动纳入可用层级。</p>
 */
@ConditionalOnClass(name = "dev.langchain4j.Experimental")
@ConditionalOnProperty(prefix = "fw.plus.ai.langchain4j", name = "enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(IntentProperties.class)
@Configuration(proxyBeanMethods = false)
public class LangChain4jIntentAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public IntentClassifierFactory intentClassifierFactory(IntentProperties properties,
			ObjectProvider<EmbeddingModel> embeddingModelProvider,
			ObjectProvider<IntentLlm> llmProvider) {
		return new IntentClassifierFactory(properties,
				embeddingModelProvider.getIfAvailable(),
				llmProvider.getIfAvailable());
	}
}
