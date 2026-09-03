package dev.langchain4j.agentic.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Strict contract for the Core/aggregate overlay allowlist and Shade class-entry filters. */
class OverlayAllowlistContractTest {

    private static final String CORE_OWNER = "dev.langchain4j:langchain4j-core:${langchain4j.version}";
    private static final String AGGREGATE_OWNER = "dev.langchain4j:langchain4j:${langchain4j.version}";
    private static final String CORE_ARTIFACT = "dev.langchain4j:langchain4j-core";
    private static final String AGGREGATE_ARTIFACT = "dev.langchain4j:langchain4j";
    private static final Pattern BINARY_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final Pattern ANCHOR = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*#[A-Za-z_$][A-Za-z0-9_$]*");

    @Test
    void allowlistIsStrictAndMatchesBothOwnerPoms() throws Exception {
        Path module = Paths.get(".").toAbsolutePath().normalize();
        Path allowlist = module.resolve("../compat/overlay-allowlist.tsv").normalize();
        List<Row> rows = parse(allowlist);
        Set<String> expectedEntries = new HashSet<String>();
        for (Row row : rows) {
            assertTrue(expectedEntries.add(row.binaryName), "duplicate allowlist FQCN: " + row.binaryName);
            assertTrue(BINARY_NAME.matcher(row.binaryName).matches(), "invalid binary FQCN: " + row.binaryName);
            assertTrue(row.owner.equals(CORE_OWNER) || row.owner.equals(AGGREGATE_OWNER), "invalid owner: " + row.owner);
            assertFalse(row.reason.isEmpty(), "empty reason: " + row.binaryName);
            assertAnchor(module, row.apiAnchor);
            assertAnchor(module, row.behaviorAnchor);
        }

        Map<String, Set<String>> excludesByOwner = new LinkedHashMap<String, Set<String>>();
        excludesByOwner.put(CORE_OWNER, readShadeExcludes(module.resolve("../fw-plus-ai-langchain4j-core/pom.xml").normalize(), CORE_ARTIFACT));
        excludesByOwner.put(AGGREGATE_OWNER, readShadeExcludes(module.resolve("../pom.xml").normalize(), AGGREGATE_ARTIFACT));

        Map<String, Set<String>> expectedByOwner = new LinkedHashMap<String, Set<String>>();
        expectedByOwner.put(CORE_OWNER, new HashSet<String>());
        expectedByOwner.put(AGGREGATE_OWNER, new HashSet<String>());
        for (Row row : rows) {
            expectedByOwner.get(row.owner).add(row.binaryName.replace('.', '/') + ".class");
        }
        assertEquals(expectedByOwner, excludesByOwner, "allowlist and owner Shade excludes differ");
    }

    @Test
    void failsafeLifecycleHasSeparateIntegrationTestAndVerifyExecutions() throws Exception {
        Path pom = Paths.get(".").toAbsolutePath().normalize().resolve("pom.xml");
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        Element plugin = findPlugin(document, "maven-failsafe-plugin");
        Element executions = directChild(plugin, "executions");
        assertEquals("override", executions.getAttribute("combine.self"),
                "Failsafe executions must replace the inherited Spring Boot execution");

        List<Element> lifecycleExecutions = directChildren(executions, "execution");
        assertEquals(3, lifecycleExecutions.size(),
                "Failsafe must disable one inherited execution and declare two active executions");
        Element integrationExecution = null;
        Element verifyExecution = null;
        boolean inheritedExecutionDisabled = false;
        for (Element execution : lifecycleExecutions) {
            String id = childText(execution, "id");
            String phase = childText(execution, "phase");
            if (id == null || id.isEmpty()) {
                assertEquals("none", phase,
                        "the inherited anonymous Failsafe execution must be disabled");
                inheritedExecutionDisabled = true;
                continue;
            }
            Set<String> goals = new HashSet<String>();
            Element goalsElement = directChild(execution, "goals");
            for (Element goal : directChildren(goalsElement, "goal")) {
                goals.add(goal.getTextContent().trim());
            }
            assertEquals(1, goals.size(), "each active Failsafe execution must bind exactly one goal");
            if (goals.contains("integration-test")) {
                integrationExecution = execution;
                assertEquals("agentic-integration-test", id, "integration-test execution id differs");
                assertEquals("integration-test", phase, "integration-test execution phase differs");
            } else if (goals.contains("verify")) {
                verifyExecution = execution;
                assertEquals("agentic-verify", id, "verify execution id differs");
                assertEquals("verify", phase, "verify execution phase differs");
            } else {
                throw new AssertionError("unsupported Failsafe goal: " + goals);
            }
        }
        assertTrue(inheritedExecutionDisabled, "inherited Failsafe execution remains active");
        assertNotNull(integrationExecution, "missing Failsafe integration-test execution");
        assertNotNull(verifyExecution, "missing Failsafe verify execution");

        Element integrationConfiguration = directChild(integrationExecution, "configuration");
        assertTrue(integrationConfiguration.getTextContent().contains("**/AgenticOverlayPackagingIT.java"),
                "integration-test execution must select AgenticOverlayPackagingIT");
        Element verifyConfiguration = optionalDirectChild(verifyExecution, "configuration");
        assertTrue(verifyConfiguration == null
                        || !verifyConfiguration.getTextContent().contains("AgenticOverlayPackagingIT"),
                "verify execution must not run AgenticOverlayPackagingIT again");
    }

    @Test
    void packagingContractHasOwnerJarsAndExcludesOfficialOverlayEntries() throws Exception {
        Path module = Paths.get(".").toAbsolutePath().normalize();
        Path aggregateJar = module.resolve("../target/fw-plus-ai-langchain4j-0.1.1-SNAPSHOT.jar").normalize();
        Path coreJar = module.resolve("../fw-plus-ai-langchain4j-core/target/fw-plus-ai-langchain4j-core-0.1.1-SNAPSHOT.jar").normalize();
        assertTrue(Files.isRegularFile(aggregateJar), "missing packaged aggregate owner JAR: " + aggregateJar);
        assertTrue(Files.isRegularFile(coreJar), "missing packaged Core owner JAR: " + coreJar);
        Set<String> overlays = new HashSet<String>();
        for (Row row : parse(module.resolve("../compat/overlay-allowlist.tsv").normalize())) {
            overlays.add(row.binaryName.replace('.', '/') + ".class");
        }
        java.util.jar.JarFile aggregate = new java.util.jar.JarFile(aggregateJar.toFile());
        java.util.jar.JarFile core = new java.util.jar.JarFile(coreJar.toFile());
        try {
            for (Row row : parse(module.resolve("../compat/overlay-allowlist.tsv").normalize())) {
                String entry = row.binaryName.replace('.', '/') + ".class";
                if (CORE_OWNER.equals(row.owner)) {
                    assertTrue(core.getJarEntry(entry) != null, "Core overlay missing from packaged JAR: " + entry);
                    assertTrue(aggregate.getJarEntry(entry) == null, "aggregate unexpectedly contains Core overlay: " + entry);
                } else {
                    assertTrue(aggregate.getJarEntry(entry) != null, "aggregate overlay missing from packaged JAR: " + entry);
                    assertTrue(core.getJarEntry(entry) == null, "Core unexpectedly contains aggregate overlay: " + entry);
                }
            }
        } finally {
            aggregate.close();
            core.close();
        }
    }

    private static List<Row> parse(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing overlay allowlist: " + path);
        byte[] bytes = Files.readAllBytes(path);
        assertFalse(bytes.length >= 3 && bytes[0] == (byte) 0xef && bytes[1] == (byte) 0xbb && bytes[2] == (byte) 0xbf,
                "overlay allowlist must not contain UTF-8 BOM");
        String text = new String(bytes, StandardCharsets.UTF_8);
        List<Row> rows = new ArrayList<Row>();
        String[] lines = text.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty()) {
                continue;
            }
            if (line.charAt(0) == '#') {
                continue;
            }
            String[] columns = line.split("\\t", -1);
            assertEquals(5, columns.length, "allowlist line " + (i + 1) + " must have five TSV columns");
            for (String column : columns) {
                assertFalse(column.isEmpty(), "allowlist line " + (i + 1) + " contains an empty column");
            }
            rows.add(new Row(columns[0], columns[1], columns[2], columns[3], columns[4]));
        }
        assertFalse(rows.isEmpty(), "overlay allowlist must contain at least one row");
        return rows;
    }

    private static void assertAnchor(Path module, String anchor) throws Exception {
        assertTrue(ANCHOR.matcher(anchor).matches(), "invalid test anchor: " + anchor);
        int hash = anchor.indexOf('#');
        Class<?> testClass = Class.forName(anchor.substring(0, hash));
        java.lang.reflect.Method method = testClass.getDeclaredMethod(anchor.substring(hash + 1));
        assertNotNull(method.getAnnotation(Test.class), "anchor is not a JUnit @Test method: " + anchor);
    }

    private static Set<String> readShadeExcludes(Path pom, String owner) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        NodeList filters = document.getElementsByTagName("filter");
        Set<String> excludes = new HashSet<String>();
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            if (!owner.equals(childText(filter, "artifact"))) {
                continue;
            }
            NodeList entries = filter.getElementsByTagName("exclude");
            for (int j = 0; j < entries.getLength(); j++) {
                excludes.add(entries.item(j).getTextContent().trim());
            }
        }
        return excludes;
    }

    private static Element findPlugin(Document document, String artifactId) {
        NodeList plugins = document.getElementsByTagName("plugin");
        for (int i = 0; i < plugins.getLength(); i++) {
            Element plugin = (Element) plugins.item(i);
            if (artifactId.equals(childText(plugin, "artifactId"))) {
                return plugin;
            }
        }
        throw new AssertionError("missing Maven plugin: " + artifactId);
    }

    private static Element directChild(Element parent, String name) {
        Element child = optionalDirectChild(parent, name);
        assertNotNull(child, "missing direct XML child " + name + " under " + parent.getNodeName());
        return child;
    }

    private static Element optionalDirectChild(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                return (Element) child;
            }
        }
        return null;
    }

    private static List<Element> directChildren(Element parent, String name) {
        List<Element> result = new ArrayList<Element>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                result.add((Element) child);
            }
        }
        return result;
    }

    private static String childText(Element parent, String name) {
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && name.equals(child.getNodeName())) {
                return child.getTextContent().trim();
            }
        }
        return null;
    }

    private static final class Row {
        private final String binaryName;
        private final String owner;
        private final String reason;
        private final String apiAnchor;
        private final String behaviorAnchor;

        private Row(String binaryName, String owner, String reason, String apiAnchor, String behaviorAnchor) {
            this.binaryName = binaryName;
            this.owner = owner;
            this.reason = reason;
            this.apiAnchor = apiAnchor;
            this.behaviorAnchor = behaviorAnchor;
        }
    }
}
