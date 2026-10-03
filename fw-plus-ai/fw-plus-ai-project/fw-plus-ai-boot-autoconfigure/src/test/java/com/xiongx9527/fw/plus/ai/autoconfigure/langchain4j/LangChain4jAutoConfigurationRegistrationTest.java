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
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
						"dev.langchain4j.service.spring.AiServicesAutoConfig",
						"dev.langchain4j.rag.spring.RagAutoConfig",
						"dev.langchain4j.service.spring.AiServiceScannerProcessor"
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

		assertNotNull(conditional, "LangChain4jAutoConfiguration should declare @ConditionalOnProperty");
		assertEquals("fw.plus.ai.langchain4j", conditional.prefix());
		assertEquals("true", conditional.havingValue());
		assertTrue(conditional.matchIfMissing(), "LangChain4jAutoConfiguration should be enabled by default");
		assertEquals("enabled", conditional.name()[0]);
	}

	@Test
	public void shouldDeclareClassConditionOnRootAutoConfiguration() {
		ConditionalOnClass conditional = LangChain4jAutoConfiguration.class.getAnnotation(ConditionalOnClass.class);

		assertNotNull(conditional, "LangChain4jAutoConfiguration should declare @ConditionalOnClass");
		// 根配置以 name 形式引用官方 starter 的标记类；value 与 name 两种形式都纳入断言。
		Set<String> actual = new LinkedHashSet<String>(classNames(conditional.value()));
		actual.addAll(Arrays.asList(conditional.name()));
		assertEquals(new LinkedHashSet<String>(Arrays.asList("dev.langchain4j.Experimental")), actual);
	}

	@Test
	public void shouldDeclareEnabledConditionOnEveryImportedConfiguration() throws Exception {
		for (String className : EXPECTED_IMPORTS) {
			Class<?> importedClass = Class.forName(className);
			ConditionalOnProperty conditional = importedClass.getAnnotation(ConditionalOnProperty.class);

			assertNotNull(conditional, className + " should declare @ConditionalOnProperty");
			assertEquals("true", conditional.havingValue());
			assertTrue(conditional.matchIfMissing(), className + " should be enabled by default");
			assertEquals("enabled", conditional.name()[0]);
		}
	}

	@Test
	public void shouldDeclareClassConditionOnEveryImportedConfiguration() throws Exception {
		for (String className : EXPECTED_IMPORTS) {
			Class<?> importedClass = Class.forName(className);
			ConditionalOnClass conditional = importedClass.getAnnotation(ConditionalOnClass.class);

			assertNotNull(conditional, className + " should declare @ConditionalOnClass");
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
			assertFalse(method.getReturnType().getName().contains("AzureAiSearchEmbeddingStore"),
					"Azure auto-configuration must not create a native embedding store");
			if (method.getReturnType().getName().contains("AzureAiSearchContentRetriever")) {
				retrieverBean = true;
			}
		}
		assertTrue(retrieverBean, "Azure content retriever bean should remain available");
	}

	@Test
	public void shouldDropRemovedStoreBypassWrappersAndKeepProviderSdksOptional() throws Exception {
		File pom = findProjectFile("fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/pom.xml");
		Map<String, String> scopes = dependencyScopes(pom);

		for (String wrapper : new String[] {
				"fw-plus-ai-langchain4j-elasticsearch",
				"fw-plus-ai-langchain4j-redis",
				"fw-plus-ai-langchain4j-milvus"
		}) {
			assertFalse(scopes.containsKey(wrapper), "Removed store bypass wrapper should not remain: " + wrapper);
		}

		// store 工厂直接在 main 里使用官方 SDK 类型，因此必须是编译期依赖；
		// 同时以 optional 声明，避免把供应商 jar 传递给下游使用方。
		// milvus-sdk-java 不再单独声明：随 langchain4j-milvus 传递（见 527d1971）。
		Map<String, Boolean> optionals = dependencyOptionals(pom);
		for (String providerSdk : new String[] {
				"langchain4j-elasticsearch",
				"langchain4j-milvus",
				"langchain4j-qdrant",
				"langchain4j-redis",
				"langchain4j-weaviate"
		}) {
			assertEquals("compile", scopes.get(providerSdk), providerSdk + " should be on the compile classpath");
			assertEquals(Boolean.TRUE, optionals.get(providerSdk),
					providerSdk + " should be optional so it is not propagated downstream");
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

	private Map<String, Boolean> dependencyOptionals(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		NodeList dependencies = document.getElementsByTagName("dependency");
		Map<String, Boolean> optionals = new LinkedHashMap<String, Boolean>();
		for (int index = 0; index < dependencies.getLength(); index++) {
			Element dependency = (Element) dependencies.item(index);
			NodeList artifactIds = dependency.getElementsByTagName("artifactId");
			if (artifactIds.getLength() == 0) {
				continue;
			}
			NodeList optionalElements = dependency.getElementsByTagName("optional");
			boolean optional = optionalElements.getLength() > 0
					&& Boolean.parseBoolean(optionalElements.item(0).getTextContent().trim());
			optionals.put(artifactIds.item(0).getTextContent().trim(), optional);
		}
		return optionals;
	}
}
