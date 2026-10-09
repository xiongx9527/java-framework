package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * 装配收口回归（A.6）：springboot3 分支删除了 dev 的顶层 {@code LangChain4jAutoConfiguration}
 * 及 11 个 {@code LangChain4j*AutoConfiguration}，{@code AutoConfiguration.imports} 中
 * LangChain4j 条目只剩 {@code support.LangChain4jSupportAutoConfiguration}。
 */
public class LangChain4jAutoConfigurationRegistrationTest {

	/** 收口后 imports 中唯一的 LangChain4j 自动配置。 */
	private static final String SUPPORT_CONFIG =
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.LangChain4jSupportAutoConfiguration";

	private static final String IMPORTS_RESOURCE =
			"META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports";

	/** 收口时删除的顶层配置（含根配置），按包名前缀校验。 */
	private static final String LANGCHAIN4J_PACKAGE = "com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.";

	/** 根配置类名（已删除）。 */
	private static final String ROOT_CONFIG = LANGCHAIN4J_PACKAGE + "LangChain4jAutoConfiguration";

	/** 旧装配体系的 11 个模型/检索器配置（已删除）。 */
	private static final String[] REMOVED_CONFIGS = new String[] {
			LANGCHAIN4J_PACKAGE + "LangChain4jCoreAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jOpenAiAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jAnthropicAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jOllamaAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jAzureAiSearchContentRetrieverAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jAzureOpenAiAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jVoyageAiAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jGitHubModelsAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jVertexAiGeminiAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jQianfanAutoConfiguration",
			LANGCHAIN4J_PACKAGE + "LangChain4jDashScopeAutoConfiguration"
	};

	@Test
	public void shouldRegisterOnlySupportAutoConfigurationInImports() throws Exception {
		Set<String> langchain4jImports = new LinkedHashSet<String>();
		for (String className : readImports()) {
			if (className.startsWith(LANGCHAIN4J_PACKAGE)) {
				langchain4jImports.add(className);
			}
		}

		Set<String> expected = new LinkedHashSet<String>();
		expected.add(SUPPORT_CONFIG);
		assertEquals(expected, langchain4jImports,
				"AutoConfiguration.imports 中 LangChain4j 条目应只剩 LangChain4jSupportAutoConfiguration");
	}

	@Test
	public void shouldRemoveLegacyLangChain4jAutoConfigurations() {
		String[] removed = new String[REMOVED_CONFIGS.length + 1];
		System.arraycopy(REMOVED_CONFIGS, 0, removed, 0, REMOVED_CONFIGS.length);
		removed[REMOVED_CONFIGS.length] = ROOT_CONFIG;

		for (String className : removed) {
			try {
				Class.forName(className);
				throw new AssertionError("收口的自动配置类应已删除: " + className);
			} catch (ClassNotFoundException expected) {
				// Expected: legacy auto-configurations have been removed on this branch.
			}
		}
	}

	@Test
	public void shouldDeclareEnabledConditionOnSupportAutoConfiguration() throws Exception {
		Class<?> supportConfig = Class.forName(SUPPORT_CONFIG);
		ConditionalOnProperty conditional = supportConfig.getAnnotation(ConditionalOnProperty.class);

		assertNotNull(conditional, "LangChain4jSupportAutoConfiguration should declare @ConditionalOnProperty");
		assertEquals("fw.plus.ai.langchain4j", conditional.prefix());
		assertEquals("true", conditional.havingValue());
		assertTrue(conditional.matchIfMissing(), "LangChain4jSupportAutoConfiguration should be enabled by default");
		assertEquals("enabled", conditional.name()[0]);
	}

	@Test
	public void shouldDeclareClassConditionOnSupportAutoConfiguration() throws Exception {
		Class<?> supportConfig = Class.forName(SUPPORT_CONFIG);
		ConditionalOnClass conditional = supportConfig.getAnnotation(ConditionalOnClass.class);

		assertNotNull(conditional, "LangChain4jSupportAutoConfiguration should declare @ConditionalOnClass");
		Set<String> actual = new LinkedHashSet<String>();
		for (Class<?> type : conditional.value()) {
			actual.add(type.getName());
		}
		actual.addAll(java.util.Arrays.asList(conditional.name()));
		assertEquals(new LinkedHashSet<String>(java.util.Arrays.asList("dev.langchain4j.Experimental")), actual);
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
		// langchain4j-redis 已随 langchain4j 1.20.2 基线升级移除（13d03f86c），不再期望。
		Map<String, Boolean> optionals = dependencyOptionals(pom);
		for (String providerSdk : new String[] {
				"langchain4j-elasticsearch",
				"langchain4j-milvus",
				"langchain4j-qdrant",
				"langchain4j-weaviate"
		}) {
			assertEquals("compile", scopes.get(providerSdk), providerSdk + " should be on the compile classpath");
			assertEquals(Boolean.TRUE, optionals.get(providerSdk),
					providerSdk + " should be optional so it is not propagated downstream");
		}
	}

	/** 读取 classpath 上的 {@code AutoConfiguration.imports} 全部条目。 */
	private List<String> readImports() throws Exception {
		InputStream in = getClass().getClassLoader().getResourceAsStream(IMPORTS_RESOURCE);
		assertNotNull(in, IMPORTS_RESOURCE + " not found on classpath");
		List<String> entries = new ArrayList<String>();
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
			String line;
			while ((line = reader.readLine()) != null) {
				String trimmed = line.trim();
				if (!trimmed.isEmpty() && !trimmed.startsWith("#")) {
					entries.add(trimmed);
				}
			}
		}
		if (entries.isEmpty()) {
			fail(IMPORTS_RESOURCE + " should not be empty");
		}
		return entries;
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
