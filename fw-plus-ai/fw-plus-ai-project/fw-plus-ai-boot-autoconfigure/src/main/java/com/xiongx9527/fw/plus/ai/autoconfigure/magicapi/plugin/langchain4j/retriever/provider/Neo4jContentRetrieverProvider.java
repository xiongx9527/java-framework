package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverContext;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.retriever.ContentRetrieverProvider;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support.Langchain4jOptionsReader;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.neo4j.Neo4jContentRetriever;
import dev.langchain4j.store.graph.neo4j.Neo4jGraph;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;

/**
 * 图库检索：{@code Neo4jContentRetriever}，用 chat-model 把自然语言转成 Cypher 后在 Neo4j 上执行。
 *
 * <p>options：{@code chat-model-key}（必填）、{@code uri} / {@code username} / {@code password}
 * （连接参数，缺省同 langchain4j 示例值）、{@code prompt-template}（留空用内置模板）。</p>
 */
public class Neo4jContentRetrieverProvider implements ContentRetrieverProvider {

	@Override
	public String provider() {
		return "neo4j";
	}

	@Override
	public ContentRetriever build(Langchain4jOptionsReader o, ContentRetrieverContext context) {
		Driver driver = GraphDatabase.driver(o.string("uri", "bolt://localhost:7687"),
				AuthTokens.basic(o.string("username", "neo4j"), o.string("password", "password")));
		String promptTemplate = o.string("prompt-template", null);
		return Neo4jContentRetriever.builder()
				.graph(Neo4jGraph.builder().driver(driver).build())
				.chatLanguageModel(context.chatModel(o.required("chat-model-key")))
				.promptTemplate(promptTemplate == null ? null : PromptTemplate.from(promptTemplate))
				.build();
	}
}
