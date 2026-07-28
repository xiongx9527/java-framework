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

import static org.junit.Assert.assertTrue;

public class NeutralStarterDependencyTest {

	private static final String STARTERS_PATH = "fw-plus-ai-project/fw-plus-ai-boot-starters/";

	@Test
	public void shouldExposeNeutralStartersWithDefaultLangChain4jAdapterAndBomManagement() throws Exception {
		assertDependencies("fw-plus-ai-model-spring-boot-starter/pom.xml",
				"fw-plus-ai-boot-autoconfigure",
				"fw-plus-ai-model",
				"fw-plus-ai-langchain4j-model");
		assertDependencies("fw-plus-ai-store-embedding-spring-boot-starter/pom.xml",
				"fw-plus-ai-boot-autoconfigure",
				"fw-plus-ai-store-embedding",
				"fw-plus-ai-langchain4j-store-embeding");

		Set<String> bomArtifacts = dependencies(findProjectFile("fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml"), true);
		assertTrue("AI Boot BOM must manage neutral model starter",
				bomArtifacts.contains("fw-plus-ai-model-spring-boot-starter"));
		assertTrue("AI Boot BOM must manage neutral embedding store starter",
				bomArtifacts.contains("fw-plus-ai-store-embedding-spring-boot-starter"));
	}

	private void assertDependencies(String pomName, String... requiredArtifacts) throws Exception {
		Set<String> artifactIds = dependencies(findProjectFile(STARTERS_PATH + pomName), false);
		for (String requiredArtifact : requiredArtifacts) {
			assertTrue(pomName + " must directly depend on " + requiredArtifact,
					artifactIds.contains(requiredArtifact));
		}
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
