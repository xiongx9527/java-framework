package com.xiongx9527.fw.plus.ai.sample.chat.ai;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.memory.EphemeralChatMemory;
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
 *   ├ RemoteKnowledgeBaseContentRetriever  按 query 与 filters 调远程接口
 *   └ DefaultContentInjector                框架默认注入（原话 + 检索内容）
 * </pre>
 *
 * <p><b>RAG 不进记忆</b>：{@code AssistantBuilder} 已把 langchain4j 的
 * {@code storeRetrievedContentInChatMemory} 置为 {@code false}——写进 memory 的是用户原话，
 * 检索结果只拼在本轮请求上（工具续调时框架用
 * {@code UserMessage.replaceLast} 把增强后的原话换回请求）。{@link EphemeralChatMemory}
 * 另外按 {@link EphemeralChatMemory#RAG_SEPARATOR} 兜底剥掉历史用户消息里的检索段。</p>
 * <p><b>RAG 不进记忆</b>：框架把检索结果拼进 user message 后会整条写进 memory；
 * {@code EphemeralChatMemory} 按 {@code EphemeralChatMemory.DEFAULT_RAG_SEPARATOR} 剥掉检索内容，
 * 只让用户原话进记忆，本轮请求仍带完整内容。锚点必须与默认注入模板的分隔段一致
 * （上游改默认模板时需同步 {@code EphemeralChatMemory.DEFAULT_RAG_SEPARATOR}）。</p>
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
				.contentRetriever(contentRetriever)
				.build();
	}
}
