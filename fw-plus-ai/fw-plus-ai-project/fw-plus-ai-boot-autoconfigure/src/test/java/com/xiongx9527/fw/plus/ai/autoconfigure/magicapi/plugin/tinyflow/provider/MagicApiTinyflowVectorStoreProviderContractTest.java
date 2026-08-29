package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.provider;

import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.tinyflow.AbstractLangChain4jTinyflowVectorStoreContractTest;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreAdapter;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreFactory;
import com.xiongx9527.fw.plus.ai.store.embedding.DefaultAiEmbeddingStoreRegistry;

import java.util.Collections;

public class MagicApiTinyflowVectorStoreProviderContractTest
		extends AbstractLangChain4jTinyflowVectorStoreContractTest {

	@Override
	protected ProviderFixture createProviderFixture() {
		AiEmbeddingStoreAdapter langChain4jAdapter = createLangChain4jInMemoryAdapter();
		MagicDynamicVectorDataSource dataSource = new MagicDynamicVectorDataSource(
				new DefaultAiEmbeddingStoreRegistry(Collections.<String, AiEmbeddingStore>emptyMap(), null),
				new DefaultAiEmbeddingStoreFactory(), Collections.singletonList(langChain4jAdapter));
		VectorDataSourceInfo info = new VectorDataSourceInfo();
		info.setKey(STORE_KEY);
		info.setFramework("langchain4j");
		info.setProvider("in-memory");
		info.setEnabled(true);
		dataSource.createClient(info);
		AiEmbeddingStore store = dataSource.get(STORE_KEY);
		return new ProviderFixture(store, new MagicApiTinyflowVectorStoreProvider(dataSource));
	}
}
