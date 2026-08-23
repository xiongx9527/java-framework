package com.xiongx9527.fw.plus.ai.spring.adapter;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SpringAiAdapterDependencyBoundaryTest {

    @Test
    void shouldKeepSpringAggregateFocusedOnSingleAdapter() throws Exception {
        Document pom = parse(findProjectFile(
                "fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-spring/pom.xml"));
        assertEquals(Set.of("fw-plus-ai-spring-adapter"), childTexts(directChild(pom.getDocumentElement(), "modules"),
                "module"));
    }

    @Test
    void shouldDependOnlyOnNeutralModelAndSpringAiPublicApi() throws Exception {
        Document pom = parse(findProjectFile(
                "fw-plus-core/fw-plus-project/fw-plus-ai/fw-plus-ai-spring/fw-plus-ai-spring-adapter/pom.xml"));
        Set<String> artifacts = dependencies(pom);

        assertTrue(artifacts.contains("fw-plus-ai-model"));
        assertTrue(artifacts.contains("spring-ai-model"));
        assertFalse(artifacts.contains("spring-ai-starter-model-openai"));
        assertFalse(artifacts.contains("spring-ai-alibaba-starter-dashscope"));
        assertFalse(artifacts.contains("spring-ai-alibaba-dashscope"));
        assertFalse(artifacts.contains("fw-plus-ai-store-embedding"));
        for (String artifact : artifacts) {
            assertFalse(artifact.startsWith("fw-plus-ai-langchain4j"));
        }
    }

    @Test
    void shouldManageVerifiedVersionsAndInternalAdapterInCoreBom() throws Exception {
        Document pom = parse(findProjectFile("fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml"));
        Element project = pom.getDocumentElement();
        Element properties = directChild(project, "properties");

        assertEquals("1.1.2", directChild(properties, "spring-ai.version").getTextContent().trim());
        assertEquals("1.1.2.2", directChild(properties, "spring-ai-alibaba.version").getTextContent().trim());
        assertEquals("1.1.2.2", directChild(properties,
                "spring-ai-alibaba-extensions.version").getTextContent().trim());
        assertTrue(dependencies(pom).contains("fw-plus-ai-spring-adapter"));
    }

    private Document parse(File file) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file);
    }

    private Set<String> dependencies(Document document) {
        NodeList nodes = document.getElementsByTagName("dependency");
        Set<String> artifacts = new LinkedHashSet<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            artifacts.add(directChild((Element) nodes.item(index), "artifactId").getTextContent().trim());
        }
        return artifacts;
    }

    private Set<String> childTexts(Element parent, String name) {
        Set<String> values = new LinkedHashSet<>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                values.add(child.getTextContent().trim());
            }
        }
        return values;
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
        File current = new File(System.getProperty("user.dir"));
        while (current != null) {
            File file = new File(current, relativePath);
            if (file.isFile()) {
                return file;
            }
            current = current.getParentFile();
        }
        throw new IllegalStateException("Cannot find " + relativePath);
    }
}
