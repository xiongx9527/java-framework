package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.KnowledgeBaseProperties;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.RemoteKnowledgeBaseContentRetriever;
import dev.langchain4j.rag.DefaultRetrievalAugmentor;
import dev.langchain4j.rag.RetrievalAugmentor;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 知识库装配：ContentRetriever + RetrievalAugmentor（检索挂在 AiServices 上，LangChain4j 标准用法）。
 *
 * <p>链路：</p>
 * <pre>
 * AiServices.retrievalAugmentor(augmentor)
 *   ├ FilteringContentRetriever               score 阈值 + metadata 过滤 + 《title》渲染
 *   │   └ RemoteKnowledgeBaseContentRetriever 按 query 与 filters 调远程接口
 *   └ RagContentInjector                      自声明模板，分隔串即 memory 剥离锚点
 * </pre>
 *
 * <p><b>RAG 不进记忆</b>：框架把检索结果拼进 user message 后会整条写进 memory；
 * {@link EphemeralChatMemory} 按 {@link RagContentInjector#SEPARATOR} 剥掉检索内容，
 * 只让用户原话进记忆，本轮请求仍带完整内容。锚点必须与模板一致（由
 * {@code RagContentInjectorTest} 锁死）。</p>
 *
 * <p>未配置 {@code fw.plus.sample.chat.knowledge-base.base-url} 时两个 Bean 都不创建，
 * {@code AiServices} 便不启用检索增强，样例退化为纯对话。</p>
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
	public RetrievalAugmentor retrievalAugmentor(ContentRetriever contentRetriever,
			KnowledgeBaseProperties properties) {
		return DefaultRetrievalAugmentor.builder()
				// 过滤/渲染在 retriever 层；注入模板自声明，分隔串即 memory 的剥离锚点
				.contentRetriever(new FilteringContentRetriever(
						contentRetriever, properties.getMinScore(), properties.getFilters()))
				.contentInjector(new RagContentInjector())
				.build();
	}
}
