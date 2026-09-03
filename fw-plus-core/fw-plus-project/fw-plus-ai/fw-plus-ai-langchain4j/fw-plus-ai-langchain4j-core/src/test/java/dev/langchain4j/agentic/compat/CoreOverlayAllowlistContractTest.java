package dev.langchain4j.agentic.compat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.InputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Executable Core-side overlay, API-superset, and packaging contract. */
class CoreOverlayAllowlistContractTest {

    private static final String VERSION = "0.35.0";
    private static final String CORE_OWNER = "dev.langchain4j:langchain4j-core:${langchain4j.version}";
    private static final String AGGREGATE_OWNER = "dev.langchain4j:langchain4j:${langchain4j.version}";
    private static final Pattern BINARY_NAME = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final Pattern ANCHOR = Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*#[A-Za-z_$][A-Za-z0-9_$]*");

    @Test
    void parsesAllowlistAndMatchesOwnerShadeExcludes() throws Exception {
        Path module = moduleRoot();
        List<Row> rows = parse(module.resolve("../compat/overlay-allowlist.tsv").normalize());
        Set<String> names = new HashSet<String>();
        for (Row row : rows) {
            assertTrue(names.add(row.binaryName), "duplicate allowlist FQCN: " + row.binaryName);
            assertTrue(BINARY_NAME.matcher(row.binaryName).matches(), "invalid binary FQCN: " + row.binaryName);
            assertTrue(CORE_OWNER.equals(row.owner) || AGGREGATE_OWNER.equals(row.owner), "invalid owner: " + row.owner);
            assertFalse(row.reason.isEmpty(), "empty reason: " + row.binaryName);
            assertTrue(ANCHOR.matcher(row.apiAnchor).matches(), "invalid API anchor: " + row.apiAnchor);
            assertTrue(ANCHOR.matcher(row.behaviorAnchor).matches(), "invalid behavior anchor: " + row.behaviorAnchor);
            if (CORE_OWNER.equals(row.owner)) {
                assertAnchor(row.apiAnchor);
                assertAnchor(row.behaviorAnchor);
            }
        }

        Map<String, Set<String>> actual = new LinkedHashMap<String, Set<String>>();
        actual.put(CORE_OWNER, readShadeExcludes(module.resolve("pom.xml"), "dev.langchain4j:langchain4j-core"));
        actual.put(AGGREGATE_OWNER, readShadeExcludes(module.resolve("../pom.xml"), "dev.langchain4j:langchain4j"));
        Map<String, Set<String>> expected = new LinkedHashMap<String, Set<String>>();
        expected.put(CORE_OWNER, new HashSet<String>());
        expected.put(AGGREGATE_OWNER, new HashSet<String>());
        for (Row row : rows) {
            expected.get(row.owner).add(row.binaryName.replace('.', '/') + ".class");
        }
        assertEquals(expected, actual, "allowlist and owner POM excludes differ");
    }

    @Test
    void serviceLoaderDiscoversRegistrarFactoryFromCoreOwner() {
        int count = 0;
        for (Object factory : ServiceLoader.load(dev.langchain4j.spi.observability.AiServiceListenerRegistrarFactory.class)) {
            assertNotNull(factory);
            count++;
        }
        assertTrue(count >= 1, "Core SPI registrar factory descriptor was not discovered");
    }

    public static void packagedCoreHasUniqueEntriesAndExactOverlayBytes() throws Exception {
        Path module = moduleRoot();
        Path coreJarPath = module.resolve("target/fw-plus-ai-langchain4j-core-0.1.1-SNAPSHOT.jar").normalize();
        assertTrue(Files.isRegularFile(coreJarPath), "missing packaged Core owner JAR: " + coreJarPath);
        List<Row> rows = parse(module.resolve("../compat/overlay-allowlist.tsv").normalize());
        Set<String> coreOverlays = new HashSet<String>();
        for (Row row : rows) {
            if (CORE_OWNER.equals(row.owner)) coreOverlays.add(row.binaryName.replace('.', '/') + ".class");
        }
        JarFile coreJar = new JarFile(coreJarPath.toFile());
        try {
            assertAllClassEntriesAreUnique(coreJar);
            for (String entry : coreOverlays) {
                JarEntry packaged = coreJar.getJarEntry(entry);
                assertNotNull(packaged, "Core overlay missing from packaged JAR: " + entry + "; source=" + coreJarPath);
                Path localClass = module.resolve("target/classes").resolve(entry);
                assertTrue(Files.isRegularFile(localClass), "missing local class bytes: " + localClass);
                assertTrue(java.util.Arrays.equals(Files.readAllBytes(localClass), read(coreJar, packaged)),
                        "packaged bytes differ from target/classes for " + entry + "; sources=" + coreJarPath + "," + localClass);
            }
            assertCoreJarOwnerBoundary(coreJar, coreJarPath);
        } finally {
            coreJar.close();
        }
    }

    private static void assertCoreJarOwnerBoundary(JarFile coreJar, Path coreJarPath) {
        Enumeration<JarEntry> entries = coreJar.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (!name.endsWith(".class")) {
                continue;
            }
            assertFalse(name.startsWith("dev/langchain4j/agentic/"),
                    "Core packaged JAR contains Agentic class: " + name + "; source=" + coreJarPath);
            assertFalse(name.contains("/spring/"),
                    "Core packaged JAR contains Spring overlay class: " + name + "; source=" + coreJarPath);
            assertTrue(name.startsWith("dev/langchain4j/"),
                    "Core packaged JAR contains non-owner class: " + name + "; source=" + coreJarPath);
        }
    }

    @Test
    void eachOverlayContainsTheOfficial035PublicApi() throws Exception {
        Path module = moduleRoot();
        List<Row> rows = parse(module.resolve("../compat/overlay-allowlist.tsv").normalize());
        URLClassLoader officialLoader = new URLClassLoader(new URL[] {officialCoreJar().toUri().toURL()}, null);
        try {
            for (Row row : rows) {
                if (!CORE_OWNER.equals(row.owner)) {
                    continue;
                }
                Class<?> official = Class.forName(row.binaryName, false, officialLoader);
                Class<?> local = Class.forName(row.binaryName);
                Set<String> officialConstructors = new HashSet<String>();
                for (Constructor<?> constructor : official.getConstructors()) {
                    officialConstructors.add(constructorDescriptor(constructor));
                }
                Set<String> localConstructors = new HashSet<String>();
                for (Constructor<?> constructor : local.getConstructors()) {
                    localConstructors.add(constructorDescriptor(constructor));
                }
                assertTrue(localConstructors.containsAll(officialConstructors),
                        "local overlay constructor ABI is not a 0.35 superset: " + row.binaryName);

                Set<String> officialMethods = new HashSet<String>();
                for (Method method : official.getMethods()) {
                    officialMethods.add(methodDescriptor(method));
                }
                Set<String> localMethods = new HashSet<String>();
                for (Method method : local.getMethods()) {
                    localMethods.add(methodDescriptor(method));
                }
                assertTrue(localMethods.containsAll(officialMethods),
                        "local overlay method ABI is not a 0.35 superset: " + row.binaryName);
            }
        } finally {
            officialLoader.close();
        }
    }

    private static Path moduleRoot() {
        return Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
    }

    private static Path officialCoreJar() {
        return Paths.get(System.getProperty("user.home"), ".m2", "repository", "dev", "langchain4j",
                "langchain4j-core", VERSION, "langchain4j-core-" + VERSION + ".jar");
    }

    private static List<Row> parse(Path path) throws IOException {
        assertTrue(Files.isRegularFile(path), "missing allowlist: " + path);
        byte[] bytes = Files.readAllBytes(path);
        assertFalse(bytes.length >= 3 && bytes[0] == (byte) 0xef && bytes[1] == (byte) 0xbb && bytes[2] == (byte) 0xbf,
                "allowlist contains UTF-8 BOM");
        String text = new String(bytes, StandardCharsets.UTF_8);
        List<Row> rows = new ArrayList<Row>();
        String[] lines = text.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty() || line.charAt(0) == '#') {
                continue;
            }
            String[] columns = line.split("\\t", -1);
            assertEquals(5, columns.length, "allowlist line " + (i + 1) + " must have five columns");
            for (String column : columns) {
                assertFalse(column.isEmpty(), "allowlist line " + (i + 1) + " has an empty column");
            }
            rows.add(new Row(columns[0], columns[1], columns[2], columns[3], columns[4]));
        }
        assertFalse(rows.isEmpty(), "allowlist has no rows");
        return rows;
    }

    private static void assertAnchor(String anchor) throws Exception {
        int hash = anchor.indexOf('#');
        Class<?> type = Class.forName(anchor.substring(0, hash));
        Method method = type.getDeclaredMethod(anchor.substring(hash + 1));
        assertNotNull(method.getAnnotation(Test.class), "anchor is not a JUnit test: " + anchor);
    }

    private static Set<String> readShadeExcludes(Path pom, String owner) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        NodeList filters = document.getElementsByTagName("filter");
        Set<String> result = new HashSet<String>();
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            if (!owner.equals(childText(filter, "artifact"))) {
                continue;
            }
            NodeList excludes = filter.getElementsByTagName("exclude");
            for (int j = 0; j < excludes.getLength(); j++) {
                result.add(excludes.item(j).getTextContent().trim());
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

    @Test
    void localCoreSourceDuplicatesMatchAllowlistExactly() throws Exception {
        Path module = moduleRoot();
        Set<String> local = localSourceTypes(module.resolve("src/main/java"));
        Set<String> official = new HashSet<String>();
        JarFile jar = new JarFile(officialCoreJar().toFile());
        try {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.startsWith("dev/langchain4j/") && name.endsWith(".class") && name.indexOf('$') < 0) {
                    official.add(name.substring(0, name.length() - 6).replace('/', '.'));
                }
            }
        } finally {
            jar.close();
        }
        Set<String> actual = new HashSet<String>(local);
        actual.retainAll(official);
        Set<String> expected = new HashSet<String>();
        for (Row row : parse(module.resolve("../compat/overlay-allowlist.tsv").normalize())) {
            if (CORE_OWNER.equals(row.owner) && row.binaryName.indexOf('$') < 0) {
                expected.add(row.binaryName);
            }
        }
        assertEquals(expected, actual, "local Core duplicate sources differ from allowlist; actual=" + actual
                + ", expected=" + expected + ", official=" + officialCoreJar());
    }

    private static Set<String> localSourceTypes(Path root) throws IOException {
        Set<String> result = new HashSet<String>();
        java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(root);
        try {
            collectSourceTypes(root, stream, result);
        } finally {
            stream.close();
        }
        return result;
    }

    private static void collectSourceTypes(Path root, java.nio.file.DirectoryStream<Path> stream, Set<String> result)
            throws IOException {
        for (Path path : stream) {
            if (Files.isDirectory(path)) {
                java.nio.file.DirectoryStream<Path> child = Files.newDirectoryStream(path);
                try {
                    collectSourceTypes(root, child, result);
                } finally {
                    child.close();
                }
            } else if (path.toString().endsWith(".java")) {
                Path relative = root.relativize(path);
                String name = relative.toString().substring(0, relative.toString().length() - 5).replace('\n', '.').replace('/', '.');
                name = name.replace('\\', '.');
                result.add(name);
            }
        }
    }

    private static void assertAllClassEntriesAreUnique(JarFile jar) {
        Map<String, Integer> counts = new HashMap<String, Integer>();
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            String name = entries.nextElement().getName();
            if (name.endsWith(".class")) {
                Integer count = counts.get(name);
                counts.put(name, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
            }
        }
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            assertEquals(Integer.valueOf(1), entry.getValue(), "duplicate packaged class entry: " + entry.getKey());
        }
    }

    private static byte[] read(JarFile jar, JarEntry entry) throws IOException {
        InputStream input = jar.getInputStream(entry);
        try {
            java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int length;
            while ((length = input.read(buffer)) >= 0) {
                if (length > 0) output.write(buffer, 0, length);
            }
            return output.toByteArray();
        } finally {
            input.close();
        }
    }

    private static String constructorDescriptor(Constructor<?> constructor) {
        StringBuilder descriptor = new StringBuilder("<init>(");
        for (Class<?> parameter : constructor.getParameterTypes()) descriptor.append(typeDescriptor(parameter));
        return descriptor.append(")V").toString();
    }

    private static String methodDescriptor(Method method) {
        StringBuilder descriptor = new StringBuilder(method.getName()).append('(');
        for (Class<?> parameter : method.getParameterTypes()) descriptor.append(typeDescriptor(parameter));
        return descriptor.append(')').append(typeDescriptor(method.getReturnType())).toString();
    }

    private static String typeDescriptor(Class<?> type) {
        if (type == void.class) return "V";
        if (type == boolean.class) return "Z";
        if (type == byte.class) return "B";
        if (type == char.class) return "C";
        if (type == short.class) return "S";
        if (type == int.class) return "I";
        if (type == long.class) return "J";
        if (type == float.class) return "F";
        if (type == double.class) return "D";
        if (type.isArray()) return type.getName().replace('.', '/');
        return "L" + type.getName().replace('.', '/') + ";";
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
