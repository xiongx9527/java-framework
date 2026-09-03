package dev.langchain4j.agentic.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

class AggregateOverlayAllowlistContractTest {
    private static final String OWNER = "dev.langchain4j:langchain4j:${langchain4j.version}";

    @Test
    void aggregateAllowlistMatchesShadeExcludesAndPreserves035PublicAbi() throws Exception {
        Path module = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        List<String> overlays = aggregateOverlays(module.resolve("compat/overlay-allowlist.tsv"));
        Set<String> expectedExcludes = new HashSet<String>();
        for (String overlay : overlays) expectedExcludes.add(overlay.replace('.', '/') + ".class");
        assertEquals(expectedExcludes, shadeExcludes(module.resolve("pom.xml")));

        Path officialCorePath = Paths.get(System.getProperty("user.home"), ".m2", "repository", "dev",
                "langchain4j", "langchain4j-core", "0.35.0", "langchain4j-core-0.35.0.jar");
        Path officialAggregatePath = Paths.get(System.getProperty("user.home"), ".m2", "repository", "dev",
                "langchain4j", "langchain4j", "0.35.0", "langchain4j-0.35.0.jar");
        assertEquals(expectedExcludes, duplicateClasses(
                module.resolve("target/classes"), officialAggregatePath));

        URL officialCore = officialCorePath.toUri().toURL();
        URL officialAggregate = officialAggregatePath.toUri().toURL();
        URLClassLoader official = new URLClassLoader(new URL[] {officialAggregate, officialCore}, null);
        try {
            for (String overlay : overlays) {
                Class<?> baseline = Class.forName(overlay, false, official);
                Class<?> local = Class.forName(overlay);
                Set<String> baselineConstructors = new HashSet<String>();
                for (Constructor<?> constructor : baseline.getConstructors()) {
                    baselineConstructors.add(constructorDescriptor(constructor));
                }
                Set<String> localConstructors = new HashSet<String>();
                for (Constructor<?> constructor : local.getConstructors()) {
                    localConstructors.add(constructorDescriptor(constructor));
                }
                assertTrue(localConstructors.containsAll(baselineConstructors),
                        "constructor ABI is not a 0.35 superset: " + overlay);

                Set<String> baselineMethods = new HashSet<String>();
                for (Method method : baseline.getMethods()) baselineMethods.add(methodDescriptor(method));
                Set<String> localMethods = new HashSet<String>();
                for (Method method : local.getMethods()) localMethods.add(methodDescriptor(method));
                assertTrue(localMethods.containsAll(baselineMethods),
                        "method ABI is not a 0.35 superset: " + overlay + "; missing="
                                + difference(baselineMethods, localMethods));

                Set<String> baselineFields = new HashSet<String>();
                for (Field field : baseline.getFields()) baselineFields.add(fieldDescriptor(field));
                Set<String> localFields = new HashSet<String>();
                for (Field field : local.getFields()) localFields.add(fieldDescriptor(field));
                assertTrue(localFields.containsAll(baselineFields),
                        "field ABI is not a 0.35 superset: " + overlay + "; missing="
                                + difference(baselineFields, localFields));
            }
        } finally {
            official.close();
        }
    }

    static void packagedAggregateHasExactOwnerBoundaries() throws Exception {
        Path module = Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path classes = module.resolve("target/classes");
        Path packagedJar = packagedJar(module.resolve("target"), "fw-plus-ai-langchain4j-");
        Path officialAggregate = Paths.get(System.getProperty("user.home"), ".m2", "repository", "dev",
                "langchain4j", "langchain4j", "0.35.0", "langchain4j-0.35.0.jar");

        Set<String> allowlist = new HashSet<String>();
        for (String overlay : aggregateOverlays(module.resolve("compat/overlay-allowlist.tsv"))) {
            allowlist.add(overlay.replace('.', '/') + ".class");
        }
        Map<String, byte[]> localClasses = directoryClassBytes(classes);
        Map<String, byte[]> officialClasses = jarClassBytes(officialAggregate, false);
        Map<String, byte[]> packagedClasses = jarClassBytes(packagedJar, true);

        Set<String> actualOverlays = new HashSet<String>(localClasses.keySet());
        actualOverlays.retainAll(officialClasses.keySet());
        assertEquals(allowlist, actualOverlays, "actual aggregate overlays must equal the allowlist");

        for (String overlay : allowlist) {
            assertTrue(packagedClasses.containsKey(overlay), "packaged overlay is missing: " + overlay);
            assertArrayEquals(localClasses.get(overlay), packagedClasses.get(overlay),
                    "packaged overlay must come from target/classes: " + overlay);
        }

        Set<String> expectedOfficialClasses = new HashSet<String>(officialClasses.keySet());
        expectedOfficialClasses.removeAll(allowlist);
        assertTrue(packagedClasses.keySet().containsAll(expectedOfficialClasses),
                "packaged JAR is missing official aggregate classes: "
                        + difference(expectedOfficialClasses, packagedClasses.keySet()));

        for (Map.Entry<String, byte[]> entry : packagedClasses.entrySet()) {
            byte[] bytes = entry.getValue();
            int major = ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff);
            assertEquals(52, major, "packaged class must target Java 8: " + entry.getKey());
        }

        Path worktreeCoreTarget = module.resolve("fw-plus-ai-langchain4j-core/target").normalize();
        Path coreOwner = packagedJar(worktreeCoreTarget, "fw-plus-ai-langchain4j-core-");
        assertTrue(coreOwner.toAbsolutePath().normalize().startsWith(worktreeCoreTarget),
                "Core owner must come from the current worktree: " + coreOwner);
        Set<String> coreClasses = classNames(coreOwner);
        Set<String> ownerOverlap = new HashSet<String>(packagedClasses.keySet());
        ownerOverlap.retainAll(coreClasses);
        assertEquals(Collections.<String>emptySet(), ownerOverlap,
                "aggregate and Core owners must not package the same class");
    }

    private static Path packagedJar(Path target, final String artifactPrefix) throws IOException {
        assertTrue(Files.isDirectory(target), "packaged owner target directory is missing: " + target);
        List<Path> candidates = new ArrayList<Path>();
        try (Stream<Path> paths = Files.list(target)) {
            paths.filter(path -> path.getFileName().toString().startsWith(artifactPrefix))
                    .filter(path -> path.getFileName().toString().endsWith(".jar"))
                    .filter(path -> !path.getFileName().toString().endsWith("-tests.jar"))
                    .filter(path -> !path.getFileName().toString().startsWith("original-"))
                    .forEach(candidates::add);
        }
        assertEquals(1, candidates.size(), "expected exactly one packaged owner JAR for " + artifactPrefix);
        return candidates.get(0);
    }

    private static Set<String> classNames(Path artifact) throws IOException {
        return artifact.toFile().isDirectory()
                ? directoryClassBytes(artifact).keySet()
                : jarClassBytes(artifact, false).keySet();
    }

    private static Map<String, byte[]> directoryClassBytes(Path classes) throws IOException {
        final Map<String, byte[]> result = new HashMap<String, byte[]>();
        try (Stream<Path> paths = Files.walk(classes)) {
            for (Path path : (Iterable<Path>) paths.filter(
                    candidate -> candidate.toString().endsWith(".class"))::iterator) {
                result.put(classes.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
            }
        }
        return result;
    }

    private static Map<String, byte[]> jarClassBytes(Path jarPath, boolean rejectDuplicateEntries)
            throws IOException {
        Map<String, byte[]> result = new HashMap<String, byte[]>();
        Set<String> seenEntries = new HashSet<String>();
        try (JarFile jar = new JarFile(jarPath.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (rejectDuplicateEntries) {
                    assertTrue(seenEntries.add(entry.getName()),
                            "packaged JAR contains duplicate entry: " + entry.getName());
                }
                if (!entry.getName().endsWith(".class")) continue;
                try (InputStream input = jar.getInputStream(entry)) {
                    result.put(entry.getName(), readAllBytes(input));
                }
            }
        }
        return result;
    }

    private static byte[] readAllBytes(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static List<String> aggregateOverlays(Path path) throws IOException {
        List<String> result = new ArrayList<String>();
        for (String line : Files.readAllLines(path, StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] columns = line.split("\\t", -1);
            assertEquals(5, columns.length, "allowlist rows must have five columns");
            if (OWNER.equals(columns[1])) result.add(columns[0]);
        }
        assertTrue(!result.isEmpty());
        return result;
    }

    private static Set<String> duplicateClasses(Path classes, Path officialJar) throws IOException {
        Set<String> localClasses = new HashSet<String>();
        try (Stream<Path> paths = Files.walk(classes)) {
            paths.filter(path -> path.toString().endsWith(".class"))
                    .forEach(path -> localClasses.add(classes.relativize(path).toString().replace('\\', '/')));
        }
        Set<String> duplicates = new HashSet<String>();
        try (JarFile jar = new JarFile(officialJar.toFile())) {
            java.util.Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class") && localClasses.contains(name)) duplicates.add(name);
            }
        }
        return duplicates;
    }

    private static Set<String> shadeExcludes(Path pom) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        NodeList filters = document.getElementsByTagName("filter");
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            NodeList artifacts = filter.getElementsByTagName("artifact");
            if (artifacts.getLength() == 0 || !"dev.langchain4j:langchain4j"
                    .equals(artifacts.item(0).getTextContent().trim())) continue;
            Set<String> result = new HashSet<String>();
            NodeList excludes = filter.getElementsByTagName("exclude");
            for (int j = 0; j < excludes.getLength(); j++) {
                result.add(excludes.item(j).getTextContent().trim());
            }
            return result;
        }
        throw new AssertionError("missing aggregate owner shade filter");
    }

    private static Set<String> difference(Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<String>(expected);
        missing.removeAll(actual);
        return missing;
    }

    private static String fieldDescriptor(Field field) {
        return field.getName() + ":" + typeDescriptor(field.getType());
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
}
