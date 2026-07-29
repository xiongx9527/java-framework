package com.xiongx9527.fw.plus.ai.starter;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class NeutralStarterDependencyTest {

	private static final String STARTERS_PATH = "fw-plus-ai-project/fw-plus-ai-boot-starters/";

	@Test
	public void shouldKeepNeutralStartersFreeOfLangChain4jAndAdapterDependencies() throws Exception {
		assertDependencies("fw-plus-ai-model-spring-boot-starter/pom.xml",
				"fw-plus-ai-boot-autoconfigure",
				"fw-plus-ai-model");
		assertDependencies("fw-plus-ai-store-embedding-spring-boot-starter/pom.xml",
				"fw-plus-ai-boot-autoconfigure",
				"fw-plus-ai-store-embedding");
		assertDoesNotDependOn("fw-plus-ai-model-spring-boot-starter/pom.xml",
				"fw-plus-ai-langchain4j-model", "fw-plus-ai-langchain4j-adapter");
		assertDoesNotDependOn("fw-plus-ai-store-embedding-spring-boot-starter/pom.xml",
				"fw-plus-ai-langchain4j-store-embeding", "fw-plus-ai-langchain4j-adapter");
		assertDoesNotDependOnGroup("fw-plus-ai-model-spring-boot-starter/pom.xml", "dev.langchain4j");
		assertDoesNotDependOnGroup("fw-plus-ai-store-embedding-spring-boot-starter/pom.xml", "dev.langchain4j");
	}

	@Test
	public void shouldExposeLangChain4jAdapterThroughSingleStarterWithoutProviderSdkDependencies() throws Exception {
		assertDependencies("fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml",
				"fw-plus-ai-model-spring-boot-starter",
				"fw-plus-ai-store-embedding-spring-boot-starter",
				"fw-plus-ai-langchain4j-adapter",
				"fw-plus-ai-langchain4j-adapter-spring-boot-autoconfigure");
		assertDoesNotDependOnGroup("fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml", "dev.langchain4j");
		assertDoesNotDependOnGroup("fw-plus-ai-langchain4j-adapter-spring-boot-starter/pom.xml", "io.milvus");

		Set<String> bomArtifacts = dependencies(findProjectFile("fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml"), true);
		assertTrue("AI Boot BOM must manage neutral model starter",
				bomArtifacts.contains("fw-plus-ai-model-spring-boot-starter"));
		assertTrue("AI Boot BOM must manage neutral embedding store starter",
				bomArtifacts.contains("fw-plus-ai-store-embedding-spring-boot-starter"));
		assertTrue("AI Boot BOM must manage the unified LangChain4j adapter starter",
				bomArtifacts.contains("fw-plus-ai-langchain4j-adapter-spring-boot-starter"));
		Set<String> starterModules = modules(findProjectFile(STARTERS_PATH + "pom.xml"));
		assertTrue("AI starters aggregator must include the unified LangChain4j adapter starter",
				starterModules.contains("fw-plus-ai-langchain4j-adapter-spring-boot-starter"));
	}

	private void assertDependencies(String pomName, String... requiredArtifacts) throws Exception {
		Set<String> artifactIds = dependencies(findProjectFile(STARTERS_PATH + pomName), false);
		for (String requiredArtifact : requiredArtifacts) {
			assertTrue(pomName + " must directly depend on " + requiredArtifact,
					artifactIds.contains(requiredArtifact));
		}
	}

	private void assertDoesNotDependOn(String pomName, String... prohibitedArtifacts) throws Exception {
		Set<String> artifactIds = dependencies(findProjectFile(STARTERS_PATH + pomName), false);
		for (String prohibitedArtifact : prohibitedArtifacts) {
			assertFalse(pomName + " must not directly depend on " + prohibitedArtifact,
					artifactIds.contains(prohibitedArtifact));
		}
	}

	private void assertDoesNotDependOnGroup(String pomName, String prohibitedGroupId) throws Exception {
		Set<String> groupIds = groups(findProjectFile(STARTERS_PATH + pomName));
		assertFalse(pomName + " must not directly depend on group " + prohibitedGroupId,
				groupIds.contains(prohibitedGroupId));
	}

	private Set<String> dependencies(File pom, boolean dependencyManagement) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		Element project = document.getDocumentElement();
		Element parent = dependencyManagement ? directChild(project, "dependencyManagement") : project;
		Element dependencies = directChild(parent, "dependencies");
		Set<String> artifactIds = new LinkedHashSet<String>();
		NodeList dependencyNodes = dependencies.getChildNodes();
		for (int index = 0; index < dependencyNodes.getLength(); index++) {
			Node dependencyNode = dependencyNodes.item(index);
			if (dependencyNode.getNodeType() != Node.ELEMENT_NODE || !"dependency".equals(dependencyNode.getNodeName())) {
				continue;
			}
			Element artifactId = directChild((Element) dependencyNode, "artifactId");
			artifactIds.add(artifactId.getTextContent().trim());
		}
		return artifactIds;
	}

	private Set<String> groups(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		Element dependencies = directChild(document.getDocumentElement(), "dependencies");
		Set<String> groupIds = new LinkedHashSet<String>();
		NodeList dependencyNodes = dependencies.getChildNodes();
		for (int index = 0; index < dependencyNodes.getLength(); index++) {
			Node dependencyNode = dependencyNodes.item(index);
			if (dependencyNode.getNodeType() != Node.ELEMENT_NODE || !"dependency".equals(dependencyNode.getNodeName())) {
				continue;
			}
			Element groupId = directChild((Element) dependencyNode, "groupId");
			groupIds.add(groupId.getTextContent().trim());
		}
		return groupIds;
	}

	private Set<String> modules(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		Element modules = directChild(document.getDocumentElement(), "modules");
		Set<String> moduleNames = new LinkedHashSet<String>();
		NodeList moduleNodes = modules.getChildNodes();
		for (int index = 0; index < moduleNodes.getLength(); index++) {
			Node moduleNode = moduleNodes.item(index);
			if (moduleNode.getNodeType() == Node.ELEMENT_NODE && "module".equals(moduleNode.getNodeName())) {
				moduleNames.add(moduleNode.getTextContent().trim());
			}
		}
		return moduleNames;
	}

	private Element directChild(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
				return (Element) child;
			}
		}
		throw new IllegalStateException("Missing " + name + " in " + parent.getNodeName());
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
		throw new IllegalStateException("Cannot find " + relativePath + " from " + Arrays.toString(new String[] {System.getProperty("user.dir")}));
	}
}
