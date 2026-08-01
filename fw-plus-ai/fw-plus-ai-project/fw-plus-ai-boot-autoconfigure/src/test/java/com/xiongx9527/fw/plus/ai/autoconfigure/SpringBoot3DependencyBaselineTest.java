package com.xiongx9527.fw.plus.ai.autoconfigure;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SpringBoot3DependencyBaselineTest {

    @Test
    public void shouldKeepSpringBoot3DependencyBaseline() throws Exception {
        assertProperty(coreBom(), "spring-boot-dependencies.version", "3.5.8");
        assertProperty(coreBom(), "spring-cloud-dependencies.version", "2025.0.0");
        assertProperty(coreBom(), "spring-cloud-alibaba-dependencies.version", "2025.0.0.0");
        assertProperty(coreBom(), "magic-api.version", "2.2.2");
        assertProperty(coreBom(), "magic-script.version", "1.9.0");
        assertProperty(coreBom(), "mybatis-plus.version", "3.5.16");
        assertProperty(coreBom(), "langchain4j.version", "1.18.0");
        assertProperty(aiBom(), "spring-ai.version", "1.0.1");
        assertProperty(aiBom(), "spring-ai-alibaba.version", "1.0.1");
        assertCoreLangChain4jManagement();
        assertImportedBom(aiBom(), "com.xiongx9527.fw.plus", "fw-plus-dependencies", "${project.version}");
        assertNoDirectLangChain4jManagement(aiBom());
    }

    private void assertProperty(File pom, String name, String expected) throws Exception {
        Element properties = (Element) document(pom).getElementsByTagName("properties").item(0);
        Element property = directChild(properties, name);
        assertTrue(property != null, name + " should be declared in " + pom);
        assertEquals(expected, property.getTextContent().trim(), name);
    }

    private void assertCoreLangChain4jManagement() throws Exception {
        NodeList dependencies = document(coreBom()).getElementsByTagName("dependency");
        Map<String, String> managedArtifacts = new HashMap<>();
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            if (!"dev.langchain4j".equals(text(dependency, "groupId"))) {
                continue;
            }
            String artifactId = text(dependency, "artifactId");
            if ("langchain4j-bom".equals(artifactId)) {
                assertEquals("${langchain4j.version}", text(dependency, "version"));
                assertEquals("pom", text(dependency, "type"));
                assertEquals("import", text(dependency, "scope"));
            } else {
                managedArtifacts.put(artifactId, text(dependency, "version"));
            }
        }
        Map<String, String> expectedExceptions = new HashMap<>();
        expectedExceptions.put("langchain4j-redis", "0.36.2");
        expectedExceptions.put("langchain4j-qianfan", "0.36.2");
        expectedExceptions.put("langchain4j-dashscope", "0.36.2");
        assertEquals(expectedExceptions, managedArtifacts,
                "Core BOM must keep LangChain4j version overrides centralized");
    }

    private void assertImportedBom(File pom, String groupId, String artifactId, String version) throws Exception {
        NodeList dependencies = document(pom).getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            if (groupId.equals(text(dependency, "groupId")) && artifactId.equals(text(dependency, "artifactId"))) {
                assertEquals(version, text(dependency, "version"));
                assertEquals("pom", text(dependency, "type"));
                assertEquals("import", text(dependency, "scope"));
                return;
            }
        }
        assertTrue(false, artifactId + " should be imported in " + pom);
    }

    private void assertNoDirectLangChain4jManagement(File pom) throws Exception {
        NodeList dependencies = document(pom).getElementsByTagName("dependency");
        for (int index = 0; index < dependencies.getLength(); index++) {
            Element dependency = (Element) dependencies.item(index);
            assertTrue(!"dev.langchain4j".equals(text(dependency, "groupId")),
                    "AI BOM must inherit LangChain4j management from Core BOM: " + text(dependency, "artifactId"));
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
