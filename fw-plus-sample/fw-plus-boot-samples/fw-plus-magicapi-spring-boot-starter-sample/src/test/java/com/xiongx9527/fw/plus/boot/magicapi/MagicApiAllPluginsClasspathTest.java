package com.xiongx9527.fw.plus.boot.magicapi;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 校验 magic-api 插件聚合器与样例 classpath 一致。
 *
 * <p>两个关注点分开断言：</p>
 * <ol>
 *     <li>聚合器模块集合 == 期望清单（防止增删插件后清单/聚合漂移）</li>
 *     <li>样例 classpath 上存在其声明依赖的每个插件 artifact</li>
 * </ol>
 * 样例依赖的插件是聚合器的子集：部分插件（java-entity、mybatisplus 等）的传递依赖未纳入样例，
 * 不由本测试强制要求出现在 classpath 上。
 */
class MagicApiAllPluginsClasspathTest {

	/** 两个聚合器的全部模块。 */
	static final List<String> PLUGIN_ARTIFACTS = Arrays.asList(
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-admin",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-config",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-db",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-debezium",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-druid",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-dtp",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-elasticsearch",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-freemarker",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-influx",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-java-entity",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-kafka",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mongo",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mqtt-server",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mqtt-client",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mybatisflex",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mybatisplus",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-pulsar",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-rabbitmq",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-redis",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-rocketmq",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-s3",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-satoken",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-spring-bean",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-swagger",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-task",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-vue3",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-word",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-ws",
			"com.xiongx9527.fw.plus.ai:fw-plus-ai-magicapi-plugin-mcp",
			"com.xiongx9527.fw.plus.ai:fw-plus-ai-magicapi-plugin-langchain4j"
	);

	@Test
	void shouldMatchBootAndAiPluginAggregators() throws Exception {
		Path root = findRepositoryRoot();
		Set<String> bootModules = readModules(root.resolve(
				"fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml"));
		Set<String> aiModules = readModules(root.resolve(
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml"));

		assertEquals(28, bootModules.size());
		assertEquals(2, aiModules.size());
		assertEquals(expectedArtifacts("com.xiongx9527.fw.plus.boot"), bootModules);
		assertEquals(expectedArtifacts("com.xiongx9527.fw.plus.ai"), aiModules);
	}

	@Test
	void shouldResolveDeclaredSamplePluginArtifactsFromLocalRepository() throws Exception {
		Path root = findRepositoryRoot();
		Set<String> declaredCoordinates = readSamplePluginDependencies(root.resolve(
				"fw-plus-sample/fw-plus-boot-samples/fw-plus-magicapi-spring-boot-starter-sample/pom.xml"));

		// 判据用本地仓库而非测试 classloader：reactor 内 sibling 产物在 test 阶段没有 jar，
		// classloader 里本就没有 META-INF/maven/**.pom.properties；样例真实运行时依赖的是
		// 本地仓库（~/.m2）里已 install 的插件 artifact。
		Path localRepository = Paths.get(System.getProperty("user.home"), ".m2", "repository");
		List<String> missing = new ArrayList<String>();
		for (String coordinate : declaredCoordinates) {
			if (!isInstalled(localRepository, coordinate)) {
				missing.add(coordinate);
			}
		}

		assertTrue(missing.isEmpty(), "Missing Magic-API plugin artifacts: " + missing);
	}

	private boolean isInstalled(Path localRepository, String coordinate) {
		String[] parts = coordinate.split(":", 2);
		String groupPath = parts[0].replace('.', '/');
		Path artifactDir = localRepository.resolve(groupPath).resolve(parts[1]);
		if (!Files.isDirectory(artifactDir)) {
			return false;
		}
		try {
			for (Path versionDir : Files.newDirectoryStream(artifactDir)) {
				if (Files.isDirectory(versionDir)
						&& Files.list(versionDir).anyMatch(path -> path.getFileName().toString().endsWith(".jar"))) {
					return true;
				}
			}
		} catch (IOException ignored) {
			return false;
		}
		return false;
	}

	/**
	 * 样例 pom 里声明的 magic-api 插件 artifact（两个聚合器内的那些）。
	 */
	private Set<String> readSamplePluginDependencies(Path pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
		NodeList dependencies = document.getElementsByTagName("dependency");
		Set<String> plugins = new LinkedHashSet<String>();
		for (int index = 0; index < dependencies.getLength(); index++) {
			NodeList artifactIdElements =
					org.w3c.dom.Element.class.cast(dependencies.item(index)).getElementsByTagName("artifactId");
			if (artifactIdElements.getLength() == 0) {
				continue;
			}
			String artifactId = artifactIdElements.item(0).getTextContent().trim();
			if (artifactId.startsWith("fw-plus-magicapi-plugin-")
					|| artifactId.startsWith("fw-plus-ai-magicapi-plugin-")) {
				String groupId = artifactId.startsWith("fw-plus-ai-")
						? "com.xiongx9527.fw.plus.ai" : "com.xiongx9527.fw.plus.boot";
				plugins.add(groupId + ":" + artifactId);
			}
		}
		return plugins;
	}

	private Set<String> readModules(Path pom) throws Exception {
		List<String> lines = Files.readAllLines(pom);
		Set<String> modules = new LinkedHashSet<String>();
		for (String line : lines) {
			String trimmed = line.trim();
			if (trimmed.startsWith("<module>") && trimmed.endsWith("</module>")) {
				modules.add(trimmed.substring(8, trimmed.length() - 9));
			}
		}
		return modules;
	}

	private Path findRepositoryRoot() {
		Path current = Paths.get("").toAbsolutePath();
		while (current != null) {
			if (Files.exists(current.resolve("pom.xml"))
					&& Files.exists(current.resolve("fw-plus-boot"))
					&& Files.exists(current.resolve("fw-plus-ai"))) {
				return current;
			}
			current = current.getParent();
		}
		throw new IllegalStateException("Cannot locate repository root from user.dir");
	}

	private Set<String> expectedArtifacts(String groupId) {
		Set<String> result = new LinkedHashSet<String>();
		for (String coordinate : PLUGIN_ARTIFACTS) {
			String[] parts = coordinate.split(":", 2);
			if (groupId.equals(parts[0])) {
				result.add(parts[1]);
			}
		}
		return result;
	}
}
