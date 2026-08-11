package com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow;

import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreRegistry;

import java.util.Collections;

public class RegistryTinyflowVectorStoreProviderContractTest
		extends AbstractLangChain4jTinyflowVectorStoreContractTest {

	@Override
	protected ProviderFixture createProviderFixture() {
		AiEmbeddingStore store = createLangChain4jInMemoryStore();
		DefaultAiEmbeddingStoreRegistry registry = new DefaultAiEmbeddingStoreRegistry(
				Collections.singletonMap(STORE_KEY, store), STORE_KEY);
		return new ProviderFixture(store, new RegistryTinyflowVectorStoreProvider(registry));
	}
}
