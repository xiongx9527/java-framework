package com.xiongx9527.fw.plus.ai.tinyengine.starter;

import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TinyEngineAiStarterDependencyTest {

	@Test
	public void shouldUseNeutralAiStartersWithoutLegacyLangChain4jStarters() throws Exception {
		Set<String> dependencies = directDependencies(new File("pom.xml"));

		assertTrue("TinyEngine starter must include AI auto-configuration",
				dependencies.contains("fw-plus-ai-boot-autoconfigure"));
		assertTrue("TinyEngine starter must include TinyEngine core",
				dependencies.contains("fw-plus-tiny-engine-core"));
		assertTrue("TinyEngine starter must include the neutral AI model starter",
				dependencies.contains("fw-plus-ai-model-spring-boot-starter"));
		assertTrue("TinyEngine starter must include the neutral embedding store starter",
				dependencies.contains("fw-plus-ai-store-embedding-spring-boot-starter"));
		assertTrue("TinyEngine starter must use the unified LangChain4j adapter",
				dependencies.contains("fw-plus-ai-langchain4j-adapter-spring-boot-starter"));
		assertFalse("TinyEngine starter must not depend on the legacy LangChain4j model starter",
				dependencies.contains("fw-plus-ai-langchain4j-model-spring-boot-starter"));
		assertFalse("TinyEngine starter must not depend on the legacy LangChain4j embedding store starter",
				dependencies.contains("fw-plus-ai-langchain4j-store-embeding-spring-boot-starter"));
	}

	private Set<String> directDependencies(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		Element dependencies = directChild(document.getDocumentElement(), "dependencies");
		Set<String> artifactIds = new LinkedHashSet<String>();
		NodeList dependencyNodes = dependencies.getChildNodes();
		for (int index = 0; index < dependencyNodes.getLength(); index++) {
			Node dependencyNode = dependencyNodes.item(index);
			if (dependencyNode.getNodeType() != Node.ELEMENT_NODE || !"dependency".equals(dependencyNode.getNodeName())) {
				continue;
			}
			artifactIds.add(directChild((Element) dependencyNode, "artifactId").getTextContent().trim());
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
}
