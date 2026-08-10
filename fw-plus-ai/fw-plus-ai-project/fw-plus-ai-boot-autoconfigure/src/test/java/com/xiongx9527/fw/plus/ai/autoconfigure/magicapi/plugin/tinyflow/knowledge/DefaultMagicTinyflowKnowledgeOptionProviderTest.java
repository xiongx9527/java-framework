package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.tinyflow.knowledge;

import com.alibaba.fastjson2.JSON;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.MagicDynamicModelDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.model.model.ModelDataSourceInfo;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.MagicDynamicVectorDataSource;
import com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.vector.model.VectorDataSourceInfo;
import com.xiongx9527.fw.plus.ai.model.AiModel;
import com.xiongx9527.fw.plus.ai.model.AiModelType;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStore;
import com.xiongx9527.fw.plus.ai.store.embedding.AiEmbeddingStoreType;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.MagicDynamicS3DataSource;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.s3.model.S3DataSourceInfo;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultMagicTinyflowKnowledgeOptionProviderTest {

	@Test
	public void shouldReturnOnlyEmbeddingModelsWithStableSafeFields() {
		MagicDynamicModelDataSource models = mock(MagicDynamicModelDataSource.class);
		MagicDynamicVectorDataSource vectors = mock(MagicDynamicVectorDataSource.class);
		MagicDynamicS3DataSource s3 = mock(MagicDynamicS3DataSource.class);
		AiModel embeddingB = model("embedding-b", "openai", AiModelType.EMBEDDING);
		AiModel embeddingA = model("embedding-a", "ollama", AiModelType.EMBEDDING);
		AiModel chat = model("chat-a", "openai", AiModelType.CHAT);
		when(models.keys()).thenReturn(new LinkedHashSet<String>(Arrays.asList("embedding-b", "chat-a", "embedding-a")));
		when(models.getModel("embedding-b")).thenReturn(embeddingB);
		when(models.getModel("embedding-a")).thenReturn(embeddingA);
		when(models.getModel("chat-a")).thenReturn(chat);
		ModelDataSourceInfo named = new ModelDataSourceInfo();
		named.setKey("embedding-a"); named.setName("本地 Embedding"); named.setApiKey("model-secret");
		when(models.datasourceNodes()).thenReturn(Collections.singletonList(named));
		DefaultMagicTinyflowKnowledgeOptionProvider provider =
				new DefaultMagicTinyflowKnowledgeOptionProvider(models, vectors, s3);

		List<MagicTinyflowKnowledgeOptionProvider.Option> options = provider.listModels(20);

		assertEquals(2, options.size());
		assertEquals("embedding-a", options.get(0).getKey());
		assertEquals("本地 Embedding", options.get(0).getName());
		assertEquals("EMBEDDING", options.get(0).getType());
		assertEquals("ollama", options.get(0).getProvider());
		assertEquals("embedding-b", options.get(1).getKey());
		assertSafe(JSON.toJSONString(options));
	}

	@Test
	public void shouldReturnVectorAndS3OptionsWithLimitAndNoSecrets() {
		MagicDynamicModelDataSource models = mock(MagicDynamicModelDataSource.class);
		MagicDynamicVectorDataSource vectors = mock(MagicDynamicVectorDataSource.class);
		MagicDynamicS3DataSource s3 = mock(MagicDynamicS3DataSource.class);
		AiEmbeddingStore vectorB = vector("vector-b", "qdrant");
		AiEmbeddingStore vectorA = vector("vector-a", "milvus");
		when(vectors.keys()).thenReturn(new LinkedHashSet<String>(Arrays.asList("vector-b", "vector-a")));
		when(vectors.get("vector-b")).thenReturn(vectorB);
		when(vectors.get("vector-a")).thenReturn(vectorA);
		VectorDataSourceInfo vectorName = new VectorDataSourceInfo();
		vectorName.setKey("vector-a"); vectorName.setName("主向量库");
		vectorName.getProperties().put("password", "vector-secret");
		when(vectors.datasourceNodes()).thenReturn(Collections.singletonList(vectorName));
		S3DataSourceInfo s3B = s3("s3-b", "备份存储", "minio", "s3-secret-b", "true");
		S3DataSourceInfo s3A = s3("s3-a", "主存储", "aliyun-oss", "s3-secret-a", "true");
		S3DataSourceInfo disabled = s3("s3-disabled", "禁用", "minio", "s3-disabled-secret", "false");
		when(s3.datasourceNodes()).thenReturn(Arrays.asList(s3B, disabled, s3A));
		DefaultMagicTinyflowKnowledgeOptionProvider provider =
				new DefaultMagicTinyflowKnowledgeOptionProvider(models, vectors, s3);

		List<MagicTinyflowKnowledgeOptionProvider.Option> vectorOptions = provider.listVectors(1);
		List<MagicTinyflowKnowledgeOptionProvider.Option> s3Options = provider.listS3(20);

		assertEquals(1, vectorOptions.size());
		assertEquals("vector-a", vectorOptions.get(0).getKey());
		assertEquals("主向量库", vectorOptions.get(0).getName());
		assertEquals("VECTOR", vectorOptions.get(0).getType());
		assertEquals("milvus", vectorOptions.get(0).getProvider());
		assertEquals(Arrays.asList("s3-a", "s3-b"), Arrays.asList(
				s3Options.get(0).getKey(), s3Options.get(1).getKey()));
		assertEquals("OBJECT_STORAGE", s3Options.get(0).getType());
		assertEquals("aliyun-oss", s3Options.get(0).getProvider());
		assertSafe(JSON.toJSONString(vectorOptions));
		assertSafe(JSON.toJSONString(s3Options));
		assertThrows(IllegalArgumentException.class, () -> provider.listS3(0));
	}

	private AiModel model(String key, String provider, AiModelType type) {
		AiModel model = mock(AiModel.class);
		when(model.getKey()).thenReturn(key);
		when(model.getProvider()).thenReturn(provider);
		when(model.getType()).thenReturn(type);
		return model;
	}

	private AiEmbeddingStore vector(String key, String provider) {
		AiEmbeddingStore store = mock(AiEmbeddingStore.class);
		when(store.getKey()).thenReturn(key);
		when(store.getProvider()).thenReturn(provider);
		when(store.getType()).thenReturn(AiEmbeddingStoreType.VECTOR);
		return store;
	}

	private S3DataSourceInfo s3(String key, String name, String platform, String secret, String enabled) {
		S3DataSourceInfo info = new S3DataSourceInfo();
		info.setKey(key); info.setName(name); info.setPlatform(platform); info.setSecretKey(secret);
		info.setAccessKey("access-" + secret); info.setEnableStorage(enabled);
		info.getProperties().put("token", "property-" + secret);
		return info;
	}

	private void assertSafe(String json) {
		assertFalse(json.contains("apiKey"));
		assertFalse(json.contains("secretKey"));
		assertFalse(json.contains("password"));
		assertFalse(json.contains("properties"));
		assertFalse(json.contains("secret"));
		assertFalse(json.contains("token"));
	}
}
