package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.nebula.NebulaContentRetriever;
import dev.langchain4j.store.graph.nebula.NebulaGraph;

/**
 * 图库检索：{@code NebulaContentRetriever}，用 chat-model 把自然语言转成 nGQL 后在 NebulaGraph 上执行。
 *
 * <p>options：{@code addresses}（graphd 地址，多个用逗号分隔）、{@code space-name}、
 * {@code username} / {@code password}、{@code chat-model-key}（必填），
 * {@code max-session-size} / {@code timeout}（连接池）、{@code prompt-template}（留空用内置模板）。</p>
 */
public class NebulaContentRetrieverProvider implements ContentRetrieverProvider {

	@Override
	public String provider() {
		return "nebula";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		NebulaGraph.NebulaGraphBuilder graphBuilder = NebulaGraph.builder()
				.withBasicAuth(o.required("addresses"), o.required("username"), o.string("password", ""))
				.spaceName(o.required("space-name"));
		if (o.has("max-session-size")) {
			graphBuilder.maxSessionSize(o.integer("max-session-size", null));
		}
		if (o.has("timeout")) {
			graphBuilder.timeout(o.integer("timeout", null));
		}
		String promptTemplate = o.string("prompt-template", null);
		return NebulaContentRetriever.builder()
				.graph(graphBuilder.build())
				.chatLanguageModel(context.chatModel(o.required("chat-model-key")))
				.promptTemplate(promptTemplate == null ? null : PromptTemplate.from(promptTemplate))
				.build();
	}
}
