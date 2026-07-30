package com.xiongx9527.fw.plus.ai.autoconfigure;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringBoot3DependencyBaselineTest {

    @Test
    public void shouldKeepSpringBoot3DependencyBaseline() throws Exception {
        assertProperty(coreBom(), "spring-boot-dependencies.version", "3.5.8");
        assertProperty(coreBom(), "spring-cloud-dependencies.version", "2025.0.0");
        assertProperty(coreBom(), "spring-cloud-alibaba-dependencies.version", "2025.0.0.0");
        assertProperty(coreBom(), "magic-api.version", "2.2.2");
        assertProperty(coreBom(), "magic-script.version", "1.9.0");
        assertProperty(coreBom(), "langchain4j.version", "1.18.1");
        assertProperty(aiBom(), "langchain4j.version", "1.18.1");
        assertProperty(aiBom(), "spring-ai.version", "1.0.1");
        assertProperty(aiBom(), "spring-ai-alibaba.version", "1.0.1");
        assertLangChain4jArtifactsUseBom(aiBom());
    }

    private void assertProperty(File pom, String name, String expected) throws Exception {
        Element properties = (Element) document(pom).getElementsByTagName("properties").item(0);
        Element property = directChild(properties, name);
        assertTrue(property != null, name + " should be declared in " + pom);
        assertEquals(expected, property.getTextContent().trim(), name);
    }

    private void assertLangChain4jArtifactsUseBom(File pom) throws Exception {
        NodeList dependencies = document(pom).getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            if (!"dev.langchain4j".equals(text(dependency, "groupId"))) {
                continue;
            }
            String artifactId = text(dependency, "artifactId");
            Element version = directChild(dependency, "version");
            if ("langchain4j-bom".equals(artifactId)) {
                assertEquals("${langchain4j.version}", version.getTextContent().trim());
            } else {
                assertNull(version, artifactId + " must inherit its version from langchain4j-bom");
            }
        }
    }

    private String text(Element parent, String name) {
        Element child = directChild(parent, name);
        return child == null ? null : child.getTextContent().trim();
    }

    private Element directChild(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private Document document(File pom) throws Exception {
        return DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom);
    }

    private File coreBom() {
        return new File(repositoryRoot(), "fw-plus-core/fw-plus-project/fw-plus-dependencies/pom.xml");
    }

    private File aiBom() {
        return new File(repositoryRoot(), "fw-plus-ai/fw-plus-ai-project/fw-plus-ai-boot-dependencies/pom.xml");
    }

    private File repositoryRoot() {
        File current = new File(System.getProperty("basedir", System.getProperty("user.dir")));
        while (current != null) {
            if (new File(current, "fw-plus-core/pom.xml").isFile()
                    && new File(current, "fw-plus-ai/pom.xml").isFile()) {
                return current;
            }
            current = current.getParentFile();
        }
        throw new IllegalStateException("Cannot locate repository root");
    }
}
