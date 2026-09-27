package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Import;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class LangChain4jAutoConfigurationRegistrationTest {

	private static final String[] EXPECTED_IMPORTS = new String[] {
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jCoreAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jOpenAiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAnthropicAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jOllamaAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureAiSearchContentRetrieverAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureOpenAiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jVoyageAiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jGitHubModelsAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jVertexAiGeminiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jQianfanAutoConfiguration",
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
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureAiSearchContentRetrieverAutoConfiguration",
				new String[] {
						"dev.langchain4j.rag.content.retriever.azure.search.AzureAiSearchContentRetriever"
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
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jQianfanAutoConfiguration",
				new String[] {
						"dev.langchain4j.model.qianfan.QianfanChatModel",
						"dev.langchain4j.model.qianfan.QianfanStreamingChatModel",
						"dev.langchain4j.model.qianfan.QianfanEmbeddingModel"
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
	public void shouldRemoveLegacyEmbeddingStoreAutoConfigurationBypasses() {
		for (String className : new String[] {
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jAzureAiSearchAutoConfiguration",
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jElasticsearchEmbeddingStoreAutoConfiguration",
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jRedisEmbeddingStoreAutoConfiguration",
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.LangChain4jMilvusEmbeddingStoreAutoConfiguration"
		}) {
			try {
				Class.forName(className);
				throw new AssertionError("Legacy store auto-configuration should be absent: " + className);
			} catch (ClassNotFoundException expected) {
				// Expected: all legacy store auto-configurations have been removed.
			}
		}
	}

	@Test
	public void shouldKeepAzureRetrieverWithoutCreatingANativeStoreBean() throws Exception {
		Class<?> autoConfig = Class.forName("dev.langchain4j.azure.aisearch.spring.AutoConfig");
		boolean retrieverBean = false;
		for (java.lang.reflect.Method method : autoConfig.getDeclaredMethods()) {
			if (method.getAnnotation(org.springframework.context.annotation.Bean.class) == null) {
				continue;
			}
			assertFalse("Azure auto-configuration must not create a native embedding store",
					method.getReturnType().getName().contains("AzureAiSearchEmbeddingStore"));
			if (method.getReturnType().getName().contains("AzureAiSearchContentRetriever")) {
				retrieverBean = true;
			}
		}
		assertTrue("Azure content retriever bean should remain available", retrieverBean);
	}

	@Test
	public void shouldKeepEmbeddingStoreProviderSdksOutOfTheAutoConfigureCompileClasspath() throws Exception {
		File pom = findProjectFile("fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml");
		Map<String, String> scopes = dependencyScopes(pom);

		for (String wrapper : new String[] {
				"fw-plus-ai-langchain4j-elasticsearch",
				"fw-plus-ai-langchain4j-redis",
				"fw-plus-ai-langchain4j-milvus"
		}) {
			assertFalse("Removed store bypass wrapper should not remain: " + wrapper,
					scopes.containsKey(wrapper));
		}
		for (String providerSdk : new String[] {
				"langchain4j-elasticsearch",
				"langchain4j-milvus",
				"langchain4j-qdrant",
				"langchain4j-redis",
				"langchain4j-weaviate",
				"milvus-sdk-java"
		}) {
			assertEquals(providerSdk + " should be test-only", "test", scopes.get(providerSdk));
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

	private File findProjectFile(String relativePath) {
		File currentDirectory = new File(System.getProperty("user.dir"));
		while (currentDirectory != null) {
			File file = new File(currentDirectory, relativePath);
			if (file.isFile()) {
				return file;
			}
			currentDirectory = currentDirectory.getParentFile();
		}
		return new File(relativePath);
	}

	private Map<String, String> dependencyScopes(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		NodeList dependencies = document.getElementsByTagName("dependency");
		Map<String, String> scopes = new LinkedHashMap<String, String>();
		for (int index = 0; index < dependencies.getLength(); index++) {
			Element dependency = (Element) dependencies.item(index);
			NodeList artifactIds = dependency.getElementsByTagName("artifactId");
			if (artifactIds.getLength() == 0) {
				continue;
			}
			NodeList scopeElements = dependency.getElementsByTagName("scope");
			String scope = scopeElements.getLength() == 0
					? "compile" : scopeElements.item(0).getTextContent().trim();
			scopes.put(artifactIds.item(0).getTextContent().trim(), scope);
		}
		return scopes;
	}
}
