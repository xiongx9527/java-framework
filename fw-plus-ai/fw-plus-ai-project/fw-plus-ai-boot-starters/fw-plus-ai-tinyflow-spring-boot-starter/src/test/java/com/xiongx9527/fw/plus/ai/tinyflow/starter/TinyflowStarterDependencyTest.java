package com.xiongx9527.fw.plus.ai.tinyflow.starter;

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

public class TinyflowStarterDependencyTest {

	@Test
	public void shouldDeclareOnlyRequiredJava8TinyflowDependencies() throws Exception {
		Set<String> dependencies = dependencies(new File("pom.xml"));

		assertTrue(dependencies.contains("com.xiongx9527.fw.plus:fw-plus-ai-tinyflow-core"));
		assertTrue(dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-model-spring-boot-starter"));
		assertTrue(dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-store-embedding-spring-boot-starter"));
		assertTrue(dependencies.contains("com.xiongx9527.fw.plus:fw-plus-ai-langchain4j-adapter"));
		assertTrue(dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-boot-autoconfigure"));
		assertFalse(dependencies.contains("dev.tinyflow:tinyflow-support-langchain4j"));
		assertFalse(dependencies.contains("dev.langchain4j:langchain4j-spring-boot-starter"));
	}

	private Set<String> dependencies(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		NodeList nodes = directChild(document.getDocumentElement(), "dependencies").getChildNodes();
		Set<String> coordinates = new LinkedHashSet<String>();
		for (int index = 0; index < nodes.getLength(); index++) {
			Node node = nodes.item(index);
			if (node.getNodeType() == Node.ELEMENT_NODE && "dependency".equals(node.getNodeName())) {
				Element dependency = (Element) node;
				coordinates.add(text(dependency, "groupId") + ":" + text(dependency, "artifactId"));
			}
		}
		return coordinates;
	}

	private Element directChild(Element parent, String name) {
		NodeList children = parent.getChildNodes();
		for (int index = 0; index < children.getLength(); index++) {
			Node child = children.item(index);
			if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
				return (Element) child;
			}
		}
		throw new IllegalStateException("Missing " + name);
	}

	private String text(Element parent, String name) {
		return directChild(parent, name).getTextContent().trim();
	}
}
