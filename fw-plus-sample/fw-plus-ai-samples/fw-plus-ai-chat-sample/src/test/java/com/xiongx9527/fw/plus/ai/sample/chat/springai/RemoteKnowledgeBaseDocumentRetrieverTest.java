package com.xiongx9527.fw.plus.ai.sample.chat.springai;

import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.KnowledgeBaseProperties;
import com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge.KnowledgeSearchClient;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.ai.document.Document;
import org.springframework.ai.rag.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 远程知识库 → Spring AI {@link org.springframework.ai.rag.retrieval.search.DocumentRetriever}
 * 适配契约：Item→Document 映射（title/score 进 metadata）、min-score 阈值过滤、
 * 远端故障降级为空列表（不抛异常，Advisor 按无检索结果处理）。
 *
 * <p>用 {@link MockRestServiceServer} 模拟知识库 HTTP 接口，不发真实请求；
 * HTTP 检索逻辑位于与 LangChain4j 链路共用的 {@link KnowledgeSearchClient}。</p>
 */
class RemoteKnowledgeBaseDocumentRetrieverTest {

	private static final String SEARCH_URL = "http://kb.test/api/search";

	/** mock 服务器与被测检索器共用同一 RestTemplate。 */
	private static final class Fixture {
		final MockRestServiceServer server;
		final RemoteKnowledgeBaseDocumentRetriever retriever;

		Fixture() {
			KnowledgeBaseProperties properties = new KnowledgeBaseProperties();
			properties.setBaseUrl("http://kb.test");
			RestTemplate restTemplate = new RestTemplate();
			this.server = MockRestServiceServer.bindTo(restTemplate).build();
			this.retriever = new RemoteKnowledgeBaseDocumentRetriever(
					new KnowledgeSearchClient(restTemplate, properties), properties);
		}
	}

	@Test
	void mapsItemsToDocumentsWithMetadata() {
		Fixture fixture = new Fixture();
		fixture.server.expect(requestTo(SEARCH_URL))
				.andExpect(content().string(Matchers.containsString("\"query\":\"杭州天气\"")))
				.andRespond(withSuccess(
						"{\"items\":[{\"title\":\"杭州天气\",\"content\":\"杭州夏季均温 28℃\",\"score\":0.92}]}",
						MediaType.APPLICATION_JSON));

		List<Document> documents = fixture.retriever.retrieve(new Query("杭州天气"));

		assertEquals(1, documents.size());
		Document document = documents.get(0);
		assertEquals("杭州夏季均温 28℃", document.getText());
		assertEquals("杭州天气", document.getMetadata().get("title"));
		assertEquals(0.92, document.getMetadata().get("score"));
	}

	@Test
	void dropsItemsBelowMinScore() {
		Fixture fixture = new Fixture();
		fixture.server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(
				"{\"items\":[" +
						"{\"title\":\"无关兜底\",\"content\":\"这段与问题无关\",\"score\":0.2}," +
						"{\"title\":\"命中条目\",\"content\":\"西湖位于杭州\",\"score\":0.88}]}",
				MediaType.APPLICATION_JSON));

		List<Document> documents = fixture.retriever.retrieve(new Query("西湖在哪"));

		assertEquals(1, documents.size(), "低于 min-score 的条目应被过滤");
		assertEquals("西湖位于杭州", documents.get(0).getText());
	}

	@Test
	void returnsEmptyWhenAllItemsFilteredOut() {
		Fixture fixture = new Fixture();
		fixture.server.expect(requestTo(SEARCH_URL)).andRespond(withSuccess(
				"{\"items\":[{\"title\":\"无关\",\"content\":\"兜底内容\",\"score\":0.1}]}",
				MediaType.APPLICATION_JSON));

		assertTrue(fixture.retriever.retrieve(new Query("无关问题")).isEmpty());
	}

	@Test
	void degradesToEmptyListWhenRemoteFails() {
		Fixture fixture = new Fixture();
		fixture.server.expect(requestTo(SEARCH_URL)).andRespond(withServerError());

		// 远端故障不应抛出：对话按"无检索结果"继续
		assertTrue(fixture.retriever.retrieve(new Query("杭州天气")).isEmpty());
	}

	@Test
	void returnsEmptyWhenNoItemsOrBlankQuery() {
		Fixture fixture = new Fixture();
		fixture.server.expect(requestTo(SEARCH_URL))
				.andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));
		assertTrue(fixture.retriever.retrieve(new Query("杭州天气")).isEmpty());

		// null query（防御分支）不发请求、不抛异常
		assertTrue(fixture.retriever.retrieve(null).isEmpty());
	}
}
