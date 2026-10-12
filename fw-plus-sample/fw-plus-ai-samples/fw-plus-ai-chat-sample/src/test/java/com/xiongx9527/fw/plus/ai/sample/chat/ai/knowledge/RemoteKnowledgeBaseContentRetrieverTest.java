package com.xiongx9527.fw.plus.ai.sample.chat.ai.knowledge;

import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.rag.content.Content;
import dev.langchain4j.rag.query.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * 远程知识库检索器测试：请求格式、结果转换、故障降级（A2 / A6）。
 */
class RemoteKnowledgeBaseContentRetrieverTest {

	private static final String URL = "http://knowledge.test/api/search";

	private RestTemplate restTemplate;
	private MockRestServiceServer server;
	private RemoteKnowledgeBaseContentRetriever retriever;

	@BeforeEach
	void setUp() {
		KnowledgeBaseProperties properties = new KnowledgeBaseProperties();
		properties.setBaseUrl("http://knowledge.test");
		properties.setSearchPath("/api/search");
		properties.setTopK(3);
		restTemplate = new RestTemplate();
		retriever = new RemoteKnowledgeBaseContentRetriever(restTemplate, properties);
		server = MockRestServiceServer.createServer(restTemplate);
	}

	@Test
	void sendsQueryAndTopKAndConvertsItems() {
		server.expect(once(), requestTo(URL))
				.andExpect(method(org.springframework.http.HttpMethod.POST))
				.andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
				.andExpect(content().json("{\"query\":\"杭州夏天热吗\",\"topK\":3}"))
				.andRespond(withSuccess("{\"items\":[{\"title\":\"杭州气候\",\"content\":\"杭州夏季炎热潮湿\",\"score\":0.91}]}",
						MediaType.APPLICATION_JSON));

		List<Content> contents = retriever.retrieve(Query.from("杭州夏天热吗"));

		server.verify();
		assertEquals(1, contents.size());
		TextSegment segment = contents.get(0).textSegment();
		assertEquals("杭州夏季炎热潮湿", segment.text());
		assertNotNull(segment.metadata("title"));
		assertEquals("杭州气候", segment.metadata("title"));
	}

	@Test
	void returnsEmptyWhenServiceFails() {
		server.expect(once(), requestTo(URL))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		List<Content> contents = retriever.retrieve(Query.from("任意问题"));

		server.verify();
		assertTrue(contents.isEmpty(), "远程故障应降级为空列表，不得抛异常");
	}

	@Test
	void returnsEmptyWhenNoItems() {
		server.expect(once(), requestTo(URL))
				.andRespond(withSuccess("{\"items\":[]}", MediaType.APPLICATION_JSON));

		List<Content> contents = retriever.retrieve(Query.from("任意问题"));

		server.verify();
		assertTrue(contents.isEmpty());
	}

	@Test
	void skipsItemsWithoutContent() {
		server.expect(once(), requestTo(URL))
				.andRespond(withSuccess("{\"items\":[{\"title\":\"无内容\"},{\"content\":\"有效\"}]}",
						MediaType.APPLICATION_JSON));

		List<Content> contents = retriever.retrieve(Query.from("任意问题"));

		assertEquals(1, contents.size());
		assertEquals("有效", contents.get(0).textSegment().text());
	}
}
