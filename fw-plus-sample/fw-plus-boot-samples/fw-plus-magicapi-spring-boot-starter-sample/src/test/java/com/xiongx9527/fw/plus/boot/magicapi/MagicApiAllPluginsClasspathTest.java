package com.xiongx9527.fw.plus.boot.magicapi;

import org.junit.jupiter.api.Test;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MagicApiAllPluginsClasspathTest {

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
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-kafka",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mcp",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mongo",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mqtt",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-mqttclient",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-pulsar",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-rabbitmq",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-redis",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-rocketmq",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-s3",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-satoken",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-swagger",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-task",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-vue3",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-word",
			"com.xiongx9527.fw.plus.boot:fw-plus-magicapi-plugin-ws",
			"com.xiongx9527.fw.plus.ai:fw-plus-ai-magicapi-plugin-model",
			"com.xiongx9527.fw.plus.ai:fw-plus-ai-magicapi-plugin-vector"
	);

	@Test
	void shouldExposeEveryMagicApiPluginArtifactToTheSampleRuntime() {
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		List<String> missing = new ArrayList<String>();

		for (String coordinate : PLUGIN_ARTIFACTS) {
			String[] parts = coordinate.split(":", 2);
			String resource = "META-INF/maven/" + parts[0] + "/" + parts[1] + "/pom.properties";
			URL pomProperties = classLoader.getResource(resource);
			if (pomProperties == null) {
				missing.add(coordinate);
			}
		}

		assertTrue(missing.isEmpty(), "Missing Magic-API plugin artifacts: " + missing);
	}

	@Test
	void shouldMatchBootAndAiPluginAggregators() throws Exception {
		Path root = findRepositoryRoot();
		Set<String> bootModules = readModules(root.resolve(
				"fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/fw-plus-magicapi-plugin/pom.xml"));
		Set<String> aiModules = readModules(root.resolve(
				"fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/fw-plus-ai-magicapi-plugin/pom.xml"));

		assertEquals(25, bootModules.size());
		assertEquals(2, aiModules.size());
		assertEquals(expectedArtifacts("com.xiongx9527.fw.plus.boot"), bootModules);
		assertEquals(expectedArtifacts("com.xiongx9527.fw.plus.ai"), aiModules);
	}

	private Path findRepositoryRoot() {
		Path current = Paths.get(System.getProperty("user.dir")).toAbsolutePath();
		while (current != null) {
			if (Files.isRegularFile(current.resolve("fw-plus-sample/pom.xml"))
					&& Files.isDirectory(current.resolve("fw-plus-boot"))) {
				return current;
			}
			current = current.getParent();
		}
		throw new IllegalStateException("Cannot locate repository root from user.dir");
	}

	private Set<String> readModules(Path pom) throws Exception {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
		NodeList modules = factory.newDocumentBuilder().parse(pom.toFile()).getElementsByTagName("module");
		Set<String> result = new LinkedHashSet<String>();
		for (int i = 0; i < modules.getLength(); i++) {
			result.add(modules.item(i).getTextContent().trim());
		}
		return result;
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
