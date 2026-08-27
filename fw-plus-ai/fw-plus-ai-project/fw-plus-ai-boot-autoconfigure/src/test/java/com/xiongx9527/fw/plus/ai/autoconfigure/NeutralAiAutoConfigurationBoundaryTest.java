package com.xiongx9527.fw.plus.ai.autoconfigure;

import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class NeutralAiAutoConfigurationBoundaryTest {

	private static final List<String> PUBLIC_AUTOCONFIGURATION_MARKERS = Arrays.asList(
			"dev.langchain4j",
			"dev.tinyflow.langchain4j",
			"com.xiongx9527.fw.plus.ai.langchain4j",
			"fw-plus.ai.langchain4j",
			"fw.plus.ai.langchain4j");

	private static final String[] REMOVED_PUBLIC_REGISTRATIONS = new String[] {
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.openapi.Langchain4jOpenApiAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.model.LangChain4jModelAutoConfiguration",
			"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.store.embeding.EmbeddingStoreAutoConfiguration"
	};

	@Test
	public void shouldKeepPublicAutoConfigurationFreeOfLangChain4jImplementation() throws Exception {
		File moduleDirectory = findModuleDirectory();
		File sourceRoot = new File(moduleDirectory, "src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure");
		List<String> violations = new ArrayList<String>();
		scanProductionJava(moduleDirectory, sourceRoot, violations);

		assertTrue(violations.isEmpty(),
				"Public neutral auto-configuration contains concrete framework references: " + violations);
	}

	@Test
	public void shouldRegisterOnlyNeutralModelAndStoreAutoConfigurations() throws Exception {
		File moduleDirectory = findModuleDirectory();
		File factories = new File(moduleDirectory, "src/main/resources/META-INF/spring.factories");
		String content = read(factories);

		assertTrue(content.contains("com.xiongx9527.fw.plus.ai.autoconfigure.model.AiModelAutoConfiguration"),
				"AiModelAutoConfiguration should be registered");
		assertTrue(content.contains("com.xiongx9527.fw.plus.ai.autoconfigure.store.embedding.AiEmbeddingStoreAutoConfiguration"),
				"AiEmbeddingStoreAutoConfiguration should be registered");
		assertTrue(content.contains(
				"com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.adapter."
						+ "LangChain4jAdapterAutoConfiguration"),
				"LangChain4jAdapterAutoConfiguration should be registered");
		for (String removedPublicRegistration : REMOVED_PUBLIC_REGISTRATIONS) {
			assertTrue(!content.contains(removedPublicRegistration),
					"Public spring.factories still registers concrete framework auto-configuration: "
							+ removedPublicRegistration);
		}
	}

	private void scanProductionJava(File moduleDirectory, File file, List<String> violations) throws IOException {
		if (!file.exists() || isIgnored(file)) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					scanProductionJava(moduleDirectory, child, violations);
				}
			}
			return;
		}
		if (!file.getName().endsWith(".java")) {
			return;
		}
		String relativePath = relativePath(moduleDirectory, file);
		for (String marker : PUBLIC_AUTOCONFIGURATION_MARKERS) {
			if (read(file).contains(marker)) {
				violations.add(relativePath + " -> " + marker);
			}
		}
	}

	private boolean isIgnored(File file) {
		String path = file.getPath().replace(File.separatorChar, '/');
		return file.isDirectory() && ("target".equals(file.getName())
				|| path.contains("/autoconfigure/langchain4j/")
				|| path.endsWith("/autoconfigure/langchain4j"));
	}

	private String read(File file) throws IOException {
		return new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
	}

	private String relativePath(File root, File file) {
		return root.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
	}

	private File findModuleDirectory() {
		File currentDirectory = new File(System.getProperty("user.dir"));
		while (currentDirectory != null) {
			File moduleDirectory = new File(currentDirectory,
					"fw-plus-ai-project/fw-plus-ai-boot-autoconfigure");
			if (new File(moduleDirectory, "pom.xml").isFile()) {
				return moduleDirectory;
			}
			currentDirectory = currentDirectory.getParentFile();
		}
		throw new IllegalStateException("Cannot locate fw-plus-ai-boot-autoconfigure module");
	}
}
