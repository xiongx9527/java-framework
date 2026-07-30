package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LangChain4jAutoConfigurationRegistrationTest {

	private static final String MODEL_AUTO_CONFIGURATION =
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter.LangChain4jAdapterAutoConfiguration";

	private static final String[] REQUIRED_STARTER_DEPENDENCIES = new String[] {
			"fw-plus-ai-model-spring-boot-starter",
			"fw-plus-ai-store-embedding-spring-boot-starter",
			"fw-plus-ai-langchain4j-adapter",
			"fw-plus-ai-boot-autoconfigure"
	};

	private static final String[] PROVIDER_SDK_ARTIFACTS = new String[] {
			"langchain4j-open-ai",
			"langchain4j-ollama",
			"langchain4j-dashscope",
			"langchain4j-anthropic",
			"langchain4j-azure-open-ai"
	};

	private static final String[] EXPECTED_IMPORTS = new String[] {
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jCoreAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jOpenAiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAnthropicAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jOllamaAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureAiSearchAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureOpenAiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jVoyageAiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jGitHubModelsAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jVertexAiGeminiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jElasticsearchEmbeddingStoreAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jRedisEmbeddingStoreAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jQianfanAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jMilvusEmbeddingStoreAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jDashScopeAutoConfiguration"
	};

	private static final Map<String, String[]> EXPECTED_CLASS_CONDITIONS = new LinkedHashMap<String, String[]>();

	static {
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jCoreAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.chat.ChatLanguageModel",
						"dev.langchain4j.model.chat.StreamingChatLanguageModel",
						"dev.langchain4j.service.spring.AiService"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jOpenAiAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.openai.OpenAiChatModel",
						"dev.langchain4j.model.openai.OpenAiStreamingChatModel",
						"dev.langchain4j.model.openai.OpenAiEmbeddingModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAnthropicAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.anthropic.AnthropicChatModel",
						"dev.langchain4j.model.anthropic.AnthropicStreamingChatModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jOllamaAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.ollama.OllamaChatModel",
						"dev.langchain4j.model.ollama.OllamaStreamingChatModel",
						"dev.langchain4j.model.ollama.OllamaEmbeddingModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureAiSearchAutoConfiguration",
				new String[] {
						"dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchContentRetriever",
						"dev.langchain4j.store.embedding.azure.search.AzureAiSearchEmbeddingStore"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureOpenAiAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.azure.AzureOpenAiChatModel",
						"dev.langchain4j.model.azure.AzureOpenAiStreamingChatModel",
						"dev.langchain4j.model.azure.AzureOpenAiEmbeddingModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jVoyageAiAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.voyageai.VoyageAiEmbeddingModel",
						"dev.langchain4j.model.voyageai.VoyageAiScoringModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jGitHubModelsAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.github.GitHubModelsChatModel",
						"dev.langchain4j.model.github.GitHubModelsStreamingChatModel",
						"dev.langchain4j.model.github.GitHubModelsEmbeddingModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jVertexAiGeminiAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.vertexai.VertexAiGeminiChatModel",
						"dev.langchain4j.model.vertexai.VertexAiGeminiStreamingChatModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jElasticsearchEmbeddingStoreAutoConfiguration",
				new String[] {
						"dev.langchain4j.store.embedding.elasticsearch.ElasticsearchEmbeddingStore"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jRedisEmbeddingStoreAutoConfiguration",
				new String[] {
						"dev.langchain4j.store.embedding.redis.RedisEmbeddingStore"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jQianfanAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.qianfan.QianfanChatModel",
						"dev.langchain4j.model.qianfan.QianfanStreamingChatModel",
						"dev.langchain4j.model.qianfan.QianfanEmbeddingModel"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jMilvusEmbeddingStoreAutoConfiguration",
				new String[] {
						"dev.langchain4j.store.embedding.milvus.MilvusEmbeddingStore"
				});
		EXPECTED_CLASS_CONDITIONS.put(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jDashScopeAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.dashscope.QwenChatModel",
						"dev.langchain4j.model.dashscope.QwenStreamingChatModel",
						"dev.langchain4j.model.dashscope.QwenEmbeddingModel"
				});
	}

	@Test
	public void shouldImportAllLocalLangChain4jAutoConfigurations() {
		Set<String> actualImports = importClassNames(LangChain4jAutoConfiguration.class);

		assertEquals(new LinkedHashSet<String>(Arrays.asList(EXPECTED_IMPORTS)), actualImports);
	}

	@Test
	public void shouldDeclareEnabledConditionOnRootAutoConfiguration() {
		ConditionalOnProperty conditional = LangChain4jAutoConfiguration.class.getAnnotation(ConditionalOnProperty.class);

		assertNotNull("LangChain4jAutoConfiguration should declare @ConditionalOnProperty", conditional);
		assertEquals("fw.plus.ai.langchain4j", conditional.prefix());
		assertEquals("true", conditional.havingValue());
		assertTrue("LangChain4jAutoConfiguration should be enabled by default", conditional.matchIfMissing());
		assertEquals("enabled", conditional.name()[0]);
	}

	@Test
	public void shouldDeclareClassConditionOnRootAutoConfiguration() {
		ConditionalOnClass conditional = LangChain4jAutoConfiguration.class.getAnnotation(ConditionalOnClass.class);

		assertNotNull("LangChain4jAutoConfiguration should declare @ConditionalOnClass", conditional);
		assertEquals(new LinkedHashSet<String>(Arrays.asList("dev.langchain4j.spring.LangChain4jAutoConfig")),
				classNames(conditional.value()));
	}

	@Test
	public void shouldDeclareEnabledConditionOnEveryImportedConfiguration() throws Exception {
		for (String className : EXPECTED_IMPORTS) {
			Class<?> importedClass = Class.forName(className);
			ConditionalOnProperty conditional = importedClass.getAnnotation(ConditionalOnProperty.class);

			assertNotNull(className + " should declare @ConditionalOnProperty", conditional);
			assertEquals("true", conditional.havingValue());
			assertTrue(className + " should be enabled by default", conditional.matchIfMissing());
			assertEquals("enabled", conditional.name()[0]);
		}
	}

	@Test
	public void shouldDeclareClassConditionOnEveryImportedConfiguration() throws Exception {
		for (String className : EXPECTED_IMPORTS) {
			Class<?> importedClass = Class.forName(className);
			ConditionalOnClass conditional = importedClass.getAnnotation(ConditionalOnClass.class);

			assertNotNull(className + " should declare @ConditionalOnClass", conditional);
			assertEquals(new LinkedHashSet<String>(Arrays.asList(EXPECTED_CLASS_CONDITIONS.get(className))),
					classNames(conditional.value()));
		}
	}

	@Test
	public void shouldRegisterAdapterAutoConfigurationInSpringFactories() throws Exception {
		String factoriesContent = read("META-INF/spring.factories");
		String importsContent = read("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports");
		Properties factories = new Properties();
		factories.load(new java.io.StringReader(factoriesContent));

		String autoConfigurations = factories.getProperty("org.springframework.boot.autoconfigure.EnableAutoConfiguration");
		assertNotNull("EnableAutoConfiguration entries should be present", autoConfigurations);
		assertTrue("LangChain4jAdapterAutoConfiguration should be registered",
				Arrays.asList(autoConfigurations.replace("\\n", "").split("\\s*,\\s*")).contains(MODEL_AUTO_CONFIGURATION));
		assertEquals("LangChain4jAdapterAutoConfiguration should occur once in spring.factories", 1,
				occurrences(factoriesContent, MODEL_AUTO_CONFIGURATION));
		assertEquals("LangChain4jAdapterAutoConfiguration should occur once in AutoConfiguration.imports", 1,
				occurrences(importsContent, MODEL_AUTO_CONFIGURATION));
		assertTrue("Adapter root should not import legacy provider autoconfiguration eagerly",
				!importClassNames(LangChain4jAdapterAutoConfiguration.class)
						.contains(LangChain4jAutoConfiguration.class.getName()));
	}

	private String read(String location) throws Exception {
		InputStream inputStream = getClass().getClassLoader().getResourceAsStream(location);
		assertNotNull(location + " should be present", inputStream);
		try {
			ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int length;
			while ((length = inputStream.read(buffer)) >= 0) {
				outputStream.write(buffer, 0, length);
			}
			return new String(outputStream.toByteArray(), StandardCharsets.UTF_8);
		} finally {
			inputStream.close();
		}
	}

	private int occurrences(String content, String value) {
		int count = 0;
		int index = 0;
		while ((index = content.indexOf(value, index)) >= 0) {
			count++;
			index += value.length();
		}
		return count;
	}

	@Test
	public void shouldKeepAdapterStarterProviderSdkDependenciesOptIn() throws Exception {
		File starterPom = findAdapterStarterPom();
		assertTrue("Adapter starter POM should exist: " + starterPom.getAbsolutePath(), starterPom.isFile());

		Set<String> artifactIds = dependencyArtifactIds(starterPom);
		for (String requiredDependency : REQUIRED_STARTER_DEPENDENCIES) {
			assertTrue("Adapter starter should depend on " + requiredDependency, artifactIds.contains(requiredDependency));
		}
		for (String providerSdkArtifact : PROVIDER_SDK_ARTIFACTS) {
			assertTrue("Adapter starter must not directly depend on " + providerSdkArtifact,
					!artifactIds.contains(providerSdkArtifact));
		}
	}

	private Set<String> importClassNames(Class<?> type) {
		Import importAnnotation = type.getAnnotation(Import.class);
		assertNotNull(importAnnotation);

		Set<String> classNames = new LinkedHashSet<String>();
		for (Class<?> importClass : importAnnotation.value()) {
			classNames.add(importClass.getName());
		}
		return classNames;
	}

	private Set<String> classNames(Class<?>[] classes) {
		Set<String> classNames = new LinkedHashSet<String>();
		for (Class<?> type : classes) {
			classNames.add(type.getName());
		}
		return classNames;
	}

	private File findAdapterStarterPom() {
		File currentDirectory = new File(System.getProperty("user.dir"));
		while (currentDirectory != null) {
			File starterPom = new File(currentDirectory,
					"fw-plus-ai-project/fw-plus-ai-boot-starters/"
							+ "fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml");
			if (starterPom.isFile()) {
				return starterPom;
			}
			currentDirectory = currentDirectory.getParentFile();
		}
		return new File("fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml");
	}

	private Set<String> dependencyArtifactIds(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		NodeList dependencies = document.getElementsByTagName("dependency");
		Set<String> artifactIds = new LinkedHashSet<String>();
		for (int index = 0; index < dependencies.getLength(); index++) {
			Element dependency = (Element) dependencies.item(index);
			NodeList artifactIdElements = dependency.getElementsByTagName("artifactId");
			if (artifactIdElements.getLength() > 0) {
				artifactIds.add(artifactIdElements.item(0).getTextContent().trim());
			}
		}
		return artifactIds;
	}
}
