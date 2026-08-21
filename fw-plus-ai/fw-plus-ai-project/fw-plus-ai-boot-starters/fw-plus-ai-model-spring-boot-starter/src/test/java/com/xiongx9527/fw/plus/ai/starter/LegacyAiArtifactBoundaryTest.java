package com.xiongx9527.fw.plus.ai.starter;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LegacyAiArtifactBoundaryTest {

	private static final List<String> LEGACY_MARKERS = Arrays.asList(
			"fw-plus-ai-langchain4j-model",
			"fw-plus-ai-langchain4j-store-embeding",
			"fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure",
			"fw-plus.ai.langchain4j.model",
			"fw-plus.ai.langchain4j.embedding-store",
			"com.xiongx9527.fw.plus.ai.langchain4j.model",
			"com.xiongx9527.fw.plus.ai.langchain4j.store.embeding");

	private static final List<String> FRAMEWORK_MARKERS = Arrays.asList(
			"dev.langchain4j",
			"org.springframework.ai",
			"com.alibaba.cloud.ai",
			"io.agentscope");

	@Test
	public void shouldRemoveLegacyArtifactDirectoriesAndProductionReferences() throws Exception {
		File projectRoot = findProjectRoot();
		List<String> legacyDirectories = Arrays.asList(
				"fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-model",
				"fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-langchain4j/fw-plus-ai-langchain4j-store-embeding",
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-model-spring-boot-starter",
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-langchain4j-store-embeding-spring-boot-starter",
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure");
		for (String legacyDirectory : legacyDirectories) {
			assertFalse("Legacy artifact directory must be deleted: " + legacyDirectory,
					new File(projectRoot, legacyDirectory).exists());
		}
		File langChain4jPackage = new File(projectRoot,
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/"
						+ "com/xiongx9527/fw/plus/ai/autoconfigure/langchain4j");
		assertTrue("Unified autoconfigure must own LangChain4j package", langChain4jPackage.isDirectory());

		List<String> violations = new ArrayList<String>();
		scanProductionFiles(projectRoot, new File(projectRoot, "fw-plus-core"), LEGACY_MARKERS, violations);
		scanProductionFiles(projectRoot, new File(projectRoot, "fw-plus-ai"), LEGACY_MARKERS, violations);
		scanProductionFiles(projectRoot, new File(projectRoot, "fw-plus-sample"), LEGACY_MARKERS, violations);
		assertTrue("Production POM/Java/YAML files retain legacy AI references: " + violations,
				violations.isEmpty());
	}

	@Test
	public void shouldKeepNeutralModulesFreeOfConcreteFrameworkReferences() throws Exception {
		File projectRoot = findProjectRoot();
		List<String> neutralRoots = Arrays.asList(
				"fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-model/src/main",
				"fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-store-embedding/src/main",
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-model-spring-boot-starter/src/main",
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-store-embedding-spring-boot-starter/src/main",
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/model",
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-autoconfigure/src/main/java/com/xiongx9527/fw/plus/ai/autoconfigure/store");
		List<String> violations = new ArrayList<String>();
		for (String neutralRoot : neutralRoots) {
			scanTextFiles(projectRoot, new File(projectRoot, neutralRoot), FRAMEWORK_MARKERS, violations);
		}
		assertTrue("Neutral AI modules contain concrete framework references: " + violations, violations.isEmpty());
	}

	private void scanProductionFiles(File projectRoot, File file, List<String> markers,
			List<String> violations) throws IOException {
		if (!file.exists() || isIgnoredDirectory(file)) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					scanProductionFiles(projectRoot, child, markers, violations);
				}
			}
			return;
		}
		String normalizedPath = file.getPath().replace(File.separatorChar, '/');
		boolean productionJava = normalizedPath.contains("/src/main/java/") && file.getName().endsWith(".java");
		boolean productionYaml = normalizedPath.contains("/src/main/resources/")
				&& (file.getName().endsWith(".yml") || file.getName().endsWith(".yaml"));
		if ("pom.xml".equals(file.getName()) || productionJava || productionYaml) {
			checkFile(projectRoot, file, markers, violations);
		}
	}

	private void scanTextFiles(File projectRoot, File file, List<String> markers,
			List<String> violations) throws IOException {
		if (!file.exists() || isIgnoredDirectory(file)) {
			return;
		}
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if (children != null) {
				for (File child : children) {
					scanTextFiles(projectRoot, child, markers, violations);
				}
			}
			return;
		}
		if (file.getName().endsWith(".java") || file.getName().endsWith(".yml")
				|| file.getName().endsWith(".yaml")) {
			checkFile(projectRoot, file, markers, violations);
		}
	}

	private void checkFile(File projectRoot, File file, List<String> markers,
			List<String> violations) throws IOException {
		String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
		for (String marker : markers) {
			if (content.contains(marker)) {
				violations.add(relativePath(projectRoot, file) + " -> " + marker);
			}
		}
	}

	private boolean isIgnoredDirectory(File file) {
		return file.isDirectory() && ("target".equals(file.getName()) || ".git".equals(file.getName()));
	}

	private String relativePath(File projectRoot, File file) {
		return projectRoot.toPath().relativize(file.toPath()).toString().replace(File.separatorChar, '/');
	}

	private File findProjectRoot() {
		File currentDirectory = new File(System.getProperty("user.dir"));
		while (currentDirectory != null) {
			if (new File(currentDirectory, "fw-plus-core/pom.xml").isFile()
					&& new File(currentDirectory, "fw-plus-ai/pom.xml").isFile()) {
				return currentDirectory;
			}
			currentDirectory = currentDirectory.getParentFile();
		}
		throw new IllegalStateException("Cannot locate project root from " + System.getProperty("user.dir"));
	}
}
