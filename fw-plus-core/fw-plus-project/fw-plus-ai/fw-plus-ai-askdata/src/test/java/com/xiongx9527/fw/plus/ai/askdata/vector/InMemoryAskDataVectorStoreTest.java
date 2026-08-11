package com.xiongx9527.fw.plus.ai.askdata.vector;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class InMemoryAskDataVectorStoreTest {

	@Test
	public void shouldSearchBySimilarityWithinAuthorizedScope() {
		InMemoryAskDataVectorStore store = new InMemoryAskDataVectorStore();
		AskDataProjectContext owner = new AskDataProjectContext("project-a", "operator-a");
		AskDataProjectContext other = new AskDataProjectContext("project-a", "operator-b");
		store.upsert(owner, new AskDataVectorDocument("sales", "sales", new float[] { 1F, 0F },
				Collections.singletonMap("datasourceKey", "warehouse")));
		store.upsert(owner, new AskDataVectorDocument("cost", "cost", new float[] { 0F, 1F },
				Collections.singletonMap("datasourceKey", "warehouse")));
		store.upsert(other, new AskDataVectorDocument("private", "private", new float[] { 1F, 0F },
				Collections.singletonMap("datasourceKey", "warehouse")));

		List<AskDataVectorDocument> result = store.search(owner, new AskDataVectorSearchRequest(
				new float[] { 0.9F, 0.1F }, 2, Collections.singletonMap("datasourceKey", "warehouse")));

		assertEquals(Arrays.asList("sales", "cost"), Arrays.asList(result.get(0).getId(), result.get(1).getId()));
		assertTrue(result.stream().noneMatch(document -> "private".equals(document.getId())));
	}

	@Test
	public void shouldRejectUnsupportedFilterInsteadOfIgnoringIt() {
		InMemoryAskDataVectorStore store = new InMemoryAskDataVectorStore();
		try {
			store.search(new AskDataProjectContext("project-a", "operator-a"), new AskDataVectorSearchRequest(
					new float[] { 1F }, 1, Collections.singletonMap("unknown", "value")));
		} catch (IllegalArgumentException exception) {
			assertTrue(exception.getMessage().contains("filter"));
			return;
		}
		throw new AssertionError("未知 filter 必须 fail-closed");
	}
}
