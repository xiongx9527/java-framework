package com.xiongx9527.fw.plus.ai.tinyengine.starter;

import org.junit.Test;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class TinyEngineAiStarterDependencyTest {

	private static final String AI_STARTER_POM = "fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-starters/"
			+ "fw-plus-ai-tiny-engine-spring-boot-starter/pom.xml";
	private static final String AI_BOM_POM = "fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml";
	private static final String BOOT_STARTER_POM = "fw-plus-boot/fw-plus-boot-project/fw-plus-boot-starters/"
			+ "fw-plus-tiny-engine-spring-boot-starter/pom.xml";

	@Test
	public void shouldDeclareRequiredNeutralAiStarterDependenciesAsDirectCompileNonOptionalAndVersionManaged() throws Exception {
		Element aiStarter = pom(AI_STARTER_POM);

		assertDirectCompileNonOptionalVersionManaged(aiStarter,
				"com.xiongx9527.fw.plus.boot:fw-plus-tiny-engine-spring-boot-starter");
		assertDirectCompileNonOptionalVersionManaged(aiStarter,
				"com.xiongx9527.fw.plus.ai:fw-plus-ai-boot-starter");
		assertDirectCompileNonOptionalVersionManaged(aiStarter,
				"com.xiongx9527.fw.plus.ai:fw-plus-ai-model-spring-boot-starter");
		assertDirectCompileNonOptionalVersionManaged(aiStarter,
				"com.xiongx9527.fw.plus.ai:fw-plus-ai-store-embedding-spring-boot-starter");
	}

	@Test
	public void shouldKeepTinyEngineCoreAndLangChain4jStartersOutOfAiStarter() throws Exception {
		Element aiStarter = pom(AI_STARTER_POM);

		assertFalse("TinyEngine AI starter must obtain TinyEngine core transitively from the Boot starter",
				containsDirectDependency(aiStarter, "com.xiongx9527.fw.plus:fw-plus-tiny-engine-core"));
		assertFalse("TinyEngine AI starter must not depend on the legacy LangChain4j model starter",
				containsDirectDependency(aiStarter,
						"com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter"));
		assertFalse("TinyEngine AI starter must not depend on the legacy LangChain4j embedding store starter",
				containsDirectDependency(aiStarter,
						"com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-store-embeding-spring-boot-starter"));
		assertFalse("TinyEngine AI starter must not choose a concrete LangChain4j adapter for business applications",
				containsDirectDependency(aiStarter,
						"com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-adapter-spring-boot-starter"));
	}

	@Test
	public void shouldImportBootDependenciesBomFromAiBomDependencyManagement() throws Exception {
		Element aiBom = pom(AI_BOM_POM);
		Element dependencyManagement = directChild(aiBom, "dependencyManagement");
		Element bootBom = findDirectDependency(directChild(dependencyManagement, "dependencies"),
				"com.xiongx9527.fw.plus.boot:fw-plus-boot-dependencies");

		assertNotNull("AI BOM must import the Boot BOM", bootBom);
		assertEquals("AI BOM must import the matching Boot BOM version", "${project.version}",
				directChildText(bootBom, "version"));
		assertEquals("AI BOM must import the Boot BOM as a POM", "pom", directChildText(bootBom, "type"));
		assertEquals("AI BOM must import the Boot BOM into dependency management", "import",
				directChildText(bootBom, "scope"));
	}

	@Test
	public void shouldKeepTinyEngineCoreOwnedByAiFreeBootStarter() throws Exception {
		Element bootStarter = pom(BOOT_STARTER_POM);

		assertDirectCompileNonOptional(bootStarter, "com.xiongx9527.fw.plus.boot:fw-plus-boot-starter");
		assertDirectCompileNonOptional(bootStarter, "com.xiongx9527.fw.plus:fw-plus-tiny-engine-core");
		assertDirectCompileNonOptional(bootStarter, "com.baomidou:mybatis-plus-boot-starter");
		assertDirectCompileNonOptional(bootStarter, "org.springframework.boot:spring-boot-starter-web");
		for (Element dependency : directDependencies(bootStarter)) {
			assertFalse("TinyEngine Boot starter must not depend on the AI layer",
					"com.xiongx9527.fw.plus.ai".equals(directChildText(dependency, "groupId")));
		}
	}

	@Test
	public void shouldDiscoverBootAndAiAutoConfigurationsFromStarterClasspath() {
		List<String> autoConfigurations = SpringFactoriesLoader.loadFactoryNames(
				EnableAutoConfiguration.class, getClass().getClassLoader());

		assertTrue("TinyEngine Boot auto-configuration must be discoverable",
				autoConfigurations.contains(
						"com.xiongx9527.fw.plus.boot.autoconfigure.tinyengine.TinyEngineAutoConfiguration"));
		assertTrue("TinyEngine AI auto-configuration must be discoverable",
				autoConfigurations.contains(
						"com.xiongx9527.fw.plus.ai.autoconfigure.tinyengine.TinyEngineAiAutoConfiguration"));
	}

	private void assertDirectCompileNonOptionalVersionManaged(Element pom, String coordinate) {
		Element dependency = findDirectDependency(pom, coordinate);
		assertNotNull(coordinate + " must be a direct dependency", dependency);
		assertCompileNonOptional(dependency, coordinate);
		assertFalse(coordinate + " must inherit its version from dependency management",
				hasDirectChild(dependency, "version"));
	}

	private void assertDirectCompileNonOptional(Element pom, String coordinate) {
		Element dependency = findDirectDependency(pom, coordinate);
		assertNotNull(coordinate + " must be a direct dependency", dependency);
		assertCompileNonOptional(dependency, coordinate);
	}

	private void assertCompileNonOptional(Element dependency, String coordinate) {
		String scope = directChildText(dependency, "scope");
		String optional = directChildText(dependency, "optional");
		assertTrue(coordinate + " must use Maven's default compile scope or explicit compile scope",
				scope == null || "compile".equals(scope));
		assertTrue(coordinate + " must be non-optional", optional == null || "false".equals(optional));
	}

	private boolean containsDirectDependency(Element pom, String coordinate) {
		return findDirectDependency(pom, coordinate) != null;
	}

	private Element findDirectDependency(Element pom, String coordinate) {
		Element dependencies = "dependencies".equals(pom.getNodeName()) ? pom : directChild(pom, "dependencies");
		for (Element dependency : directDependencies(dependencies)) {
			if (coordinate.equals(coordinate(dependency))) {
				return dependency;
			}
		}
		return null;
	}

	private List<Element> directDependencies(Element pom) {
		Element dependencies = "dependencies".equals(pom.getNodeName()) ? pom : directChild(pom, "dependencies");
		List<Element> result = new ArrayList<Element>();
		NodeList nodes = dependencies.getChildNodes();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node.getNodeType() == Node.ELEMENT_NODE && "dependency".equals(node.getNodeName())) {
				result.add((Element) node);
			}
		}
		return result;
	}

	private String coordinate(Element dependency) {
		return directChildText(dependency, "groupId") + ":" + directChildText(dependency, "artifactId");
	}

	private Element pom(String relativePath) throws Exception {
		File file = new File(repositoryRoot(), relativePath);
		return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file).getDocumentElement();
	}

	private File repositoryRoot() throws Exception {
		File directory = new File(System.getProperty("user.dir")).getCanonicalFile();
		while (directory != null) {
			if (new File(directory, ".git").exists()) {
				return directory;
			}
			directory = directory.getParentFile();
		}
		throw new IllegalStateException("Unable to locate repository root from " + System.getProperty("user.dir"));
	}

	private boolean hasDirectChild(Element parent, String name) {
		return directChildOrNull(parent, name) != null;
	}

	private String directChildText(Element parent, String name) {
		Element child = directChildOrNull(parent, name);
		return child == null ? null : child.getTextContent().trim();
	}

	private Element directChild(Element parent, String name) {
		Element child = directChildOrNull(parent, name);
		if (child == null) {
			throw new IllegalStateException("Missing " + name + " in " + parent.getNodeName());
		}
		return child;
	}

	private Element directChildOrNull(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
				return (Element) child;
			}
		}
		return null;
	}
}
