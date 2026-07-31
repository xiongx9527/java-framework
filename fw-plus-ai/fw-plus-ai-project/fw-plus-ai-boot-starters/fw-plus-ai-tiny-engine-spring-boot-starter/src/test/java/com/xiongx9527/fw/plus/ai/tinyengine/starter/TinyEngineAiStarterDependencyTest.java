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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class TinyEngineAiStarterDependencyTest {

	@Test
	public void shouldAggregateTinyEngineBootAndNeutralAiStarters() throws Exception {
		Set<String> dependencies = directDependencyCoordinates(new File("pom.xml"));

		assertTrue("TinyEngine AI starter must include AI auto-configuration",
				dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-boot-autoconfigure"));
		assertTrue("TinyEngine AI starter must include the existing TinyEngine Boot starter",
				dependencies.contains("com.xiongx9527.fw.plus.boot:fw-plus-tiny-engine-spring-boot-starter"));
		assertFalse("TinyEngine AI starter must obtain TinyEngine core transitively from the Boot starter",
				dependencies.contains("com.xiongx9527.fw.plus:fw-plus-tiny-engine-core"));
		assertTrue("TinyEngine AI starter must include the neutral AI model starter",
				dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-model-spring-boot-starter"));
		assertTrue("TinyEngine AI starter must include the neutral embedding store starter",
				dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-store-embedding-spring-boot-starter"));
		assertTrue("TinyEngine AI starter must use the unified LangChain4j adapter",
				dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-adapter-spring-boot-starter"));
		assertFalse("TinyEngine AI starter must not depend on the legacy LangChain4j model starter",
				dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-model-spring-boot-starter"));
		assertFalse("TinyEngine AI starter must not depend on the legacy LangChain4j embedding store starter",
				dependencies.contains("com.xiongx9527.fw.plus.ai:fw-plus-ai-langchain4j-store-embeding-spring-boot-starter"));
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

	private Set<String> directDependencyCoordinates(File pom) throws Exception {
		Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
		Element dependencies = directChild(document.getDocumentElement(), "dependencies");
		Set<String> dependencyCoordinates = new LinkedHashSet<String>();
		NodeList dependencyNodes = dependencies.getChildNodes();
		for (int index = 0; index < dependencyNodes.getLength(); index++) {
			Node dependencyNode = dependencyNodes.item(index);
			if (dependencyNode.getNodeType() != Node.ELEMENT_NODE || !"dependency".equals(dependencyNode.getNodeName())) {
				continue;
			}
			Element dependency = (Element) dependencyNode;
			dependencyCoordinates.add(directChild(dependency, "groupId").getTextContent().trim()
					+ ":" + directChild(dependency, "artifactId").getTextContent().trim());
		}
		return dependencyCoordinates;
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
