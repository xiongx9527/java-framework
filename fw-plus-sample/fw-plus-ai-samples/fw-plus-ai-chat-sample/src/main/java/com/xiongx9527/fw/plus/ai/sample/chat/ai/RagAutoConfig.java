package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.KnowledgeBaseProperties;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.RemoteKnowledgeBaseContentRetriever;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.injector.DefaultContentInjector;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RAG 装配：把远程知识库的检索结果注入 prompt。
 *
 * <p>未配置 {@code fw.plus.sample.chat.knowledge-base.base-url} 时两个 Bean 都不创建，
 * {@code AiServices} 检测不到 {@link ContentRetriever}，样例自动退化为纯对话。</p>
 */
@Configuration
@EnableConfigurationProperties(KnowledgeBaseProperties.class)
public class RagAutoConfig {

	@Bean
	@ConditionalOnExpression("!'${fw.plus.sample.chat.knowledge-base.base-url:}'.isEmpty()")
	public ContentRetriever contentRetriever(KnowledgeBaseProperties properties) {
		return RemoteKnowledgeBaseContentRetriever.of(properties);
	}

	@Bean
	@ConditionalOnExpression("!'${fw.plus.sample.chat.knowledge-base.base-url:}'.isEmpty()")
	public RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever) {
		// DefaultContentInjector 使用 LangChain4j 默认 prompt 模板，无需自行拼接
		return DefaultRetrievalAugmentor.builder()
				.contentRetriever(contentRetriever)
				.contentInjector(new DefaultContentInjector())
				.build();
	}
}
