package dev.langchain4j.agentic.compat;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.security.CodeSource;
import java.net.URLClassLoader;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.regex.Pattern;
import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/** Final packaged-owner and runtime classpath contract for the Agentic backport. */
class AgenticOverlayPackagingIT {

    private static final String VERSION = "0.35.0";
    private static final String CORE_OWNER = "dev.langchain4j:langchain4j-core:${langchain4j.version}";
    private static final String AGGREGATE_OWNER = "dev.langchain4j:langchain4j:${langchain4j.version}";
    private static final String AGGREGATE_ARTIFACT = "dev.langchain4j:langchain4j";
    private static final String CORE_ARTIFACT = "dev.langchain4j:langchain4j-core";
    private static final String PARAMETER_RESOLVER = "dev.langchain4j.service.ParameterNameResolver";
    private static final String PROVIDER = "dev.langchain4j.agentic.internal.AgenticParameterNameResolver";
    private static final Pattern BINARY_NAME =
            Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*");
    private static final Pattern ANCHOR =
            Pattern.compile("[A-Za-z_$][A-Za-z0-9_$]*(\\.[A-Za-z_$][A-Za-z0-9_$]*)*#[A-Za-z_$][A-Za-z0-9_$]*");

    @Test
    void packagedOwnersHaveExactJava8AndAgenticBoundaries() throws Exception {
        Path module = requiredDirectory("agentic.module.root");
        Path agenticJar = requiredFile("agentic.jar");
        Path aggregateJar = requiredFile("agentic.aggregate.jar");
        Path coreJar = requiredFile("agentic.core.jar");
        assertCurrentWorktreePaths(module, agenticJar, aggregateJar, coreJar);
        assertCurrentWorktreeProperties(module);

        assertEquals(agenticJar, packagedJar(module.resolve("target"), "fw-plus-ai-langchain4j-agentic-"));
        assertEquals(52, assertJarClasses(agenticJar, true, "Agentic"));
        assertEquals(52, assertJarClasses(aggregateJar, false, "aggregate"));
        assertEquals(52, assertJarClasses(coreJar, false, "Core"));

        Map<String, byte[]> agenticClasses = jarClassBytes(agenticJar, true);
        for (String name : agenticClasses.keySet()) {
            assertTrue(name.startsWith("dev/langchain4j/agentic/"),
                    "Agentic JAR contains a non-Agentic class: " + name + "; source=" + agenticJar);
        }
        assertFalse(agenticClasses.containsKey("dev/langchain4j/service/ParameterNameResolver.class"));
        assertFalse(agenticClasses.containsKey("dev/langchain4j/service/AiServices.class"));
        assertFalse(agenticClasses.containsKey("dev/langchain4j/spring/LangChain4jAutoConfig.class"));

        assertSourceManifest(module);
        assertOwnerBoundaries(aggregateJar, coreJar);
        assertSpiDescriptor(agenticJar);
    }

    @Test
    void allowlistOwnersHaveExactOfficialOverlaysAndPackagedBytes() throws Exception {
        Path module = requiredDirectory("agentic.module.root");
        Path aggregateJar = requiredFile("agentic.aggregate.jar");
        Path coreJar = requiredFile("agentic.core.jar");
        Path agenticJar = requiredFile("agentic.jar");
        assertCurrentWorktreePaths(module, agenticJar, aggregateJar, coreJar);
        Path aggregateClasses = requiredDirectory("agentic.aggregate.classes");
        Path coreClasses = requiredDirectory("agentic.core.classes");
        Path allowlist = requiredFile("agentic.allowlist");
        List<Row> rows = parseAllowlist(allowlist);

        Map<String, Set<String>> expected = rowsByOwner(rows);
        Map<String, byte[]> aggregateLocal = directoryClassBytes(aggregateClasses);
        Map<String, byte[]> coreLocal = directoryClassBytes(coreClasses);
        Map<String, byte[]> aggregateOfficial = jarClassBytes(officialJar(AGGREGATE_ARTIFACT), false);
        Map<String, byte[]> coreOfficial = jarClassBytes(officialJar(CORE_ARTIFACT), false);
        Set<String> aggregateActual = intersection(aggregateLocal.keySet(), aggregateOfficial.keySet());
        Set<String> coreActual = intersection(coreLocal.keySet(), coreOfficial.keySet());
        assertOverlaySet(expected.get(AGGREGATE_OWNER), aggregateActual, AGGREGATE_OWNER);
        assertOverlaySet(expected.get(CORE_OWNER), coreActual, CORE_OWNER);

        Map<String, byte[]> aggregatePackaged = jarClassBytes(aggregateJar, true);
        Map<String, byte[]> corePackaged = jarClassBytes(coreJar, true);
        for (Row row : rows) {
            String entry = row.binaryName.replace('.', '/') + ".class";
            Map<String, byte[]> expectedLocal = CORE_OWNER.equals(row.owner) ? coreLocal : aggregateLocal;
            Map<String, byte[]> expectedPackaged = CORE_OWNER.equals(row.owner) ? corePackaged : aggregatePackaged;
            Map<String, byte[]> otherPackaged = CORE_OWNER.equals(row.owner) ? aggregatePackaged : corePackaged;
            assertTrue(expectedPackaged.containsKey(entry), failure(row, entry, "missing packaged overlay"));
            assertFalse(otherPackaged.containsKey(entry), failure(row, entry, "overlay is present in the other owner"));
            assertTrue(expectedLocal.containsKey(entry), failure(row, entry, "missing target/classes overlay"));
            assertArrayEquals(expectedLocal.get(entry), expectedPackaged.get(entry), failure(row, entry,
                    "packaged bytes do not match current target/classes"));
        }

        assertContainsAllOfficialExceptOverlays(aggregateOfficial, aggregatePackaged,
                expected.get(AGGREGATE_OWNER), AGGREGATE_OWNER);
        assertContainsAllOfficialExceptOverlays(coreOfficial, corePackaged,
                expected.get(CORE_OWNER), CORE_OWNER);
        assertEquals(Collections.<String>emptySet(), intersection(aggregatePackaged.keySet(), corePackaged.keySet()),
                "aggregate and Core packaged JARs contain overlapping classes");
        assertPomExcludesMatch(module, rows);
    }

    @Test
    void aggregateAndCorePackagedJarsRejectForeignClassEntries() throws Exception {
        Map<String, byte[]> foreignFixture = new HashMap<String, byte[]>();
        foreignFixture.put("com/example/Foreign.class", new byte[] {0});
        assertThrows(AssertionError.class,
                () -> assertForeignEntriesRejected(foreignFixture, Paths.get("synthetic-aggregate.jar"), "aggregate"));

        Path aggregateJar = requiredFile("agentic.aggregate.jar");
        Path coreJar = requiredFile("agentic.core.jar");
        assertForeignEntriesRejected(jarClassBytes(aggregateJar, false), aggregateJar, "aggregate");
        assertForeignEntriesRejected(jarClassBytes(coreJar, false), coreJar, "Core");
    }

    @Test
    void runtimeArtifactValidationRejectsAnyLangChain4jOnePointNineArtifact() throws Exception {
        Path fakeArtifact = Files.createTempFile("provider-", ".jar");
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(fakeArtifact))) {
            output.putNextEntry(new JarEntry("META-INF/maven/dev.langchain4j/provider/pom.properties"));
            Properties properties = new Properties();
            properties.setProperty("groupId", "dev.langchain4j");
            properties.setProperty("artifactId", "provider");
            properties.setProperty("version", "1.9.0");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            properties.store(bytes, "synthetic provider fixture");
            output.write(bytes.toByteArray());
            output.closeEntry();
        }
        try {
            assertThrows(AssertionError.class, () -> rejectUnsupportedRuntimeArtifact(fakeArtifact));
        } finally {
            Files.deleteIfExists(fakeArtifact);
        }
    }

    @Test
    void currentWorktreePathsCannotBeReplacedBySiblingTargetPaths() throws Exception {
        Path module = Paths.get("/tmp/current-agentic-worktree").toAbsolutePath().normalize();
        Path currentTarget = module.resolve("target");
        Path sibling = Files.createTempDirectory("foreign-agentic-worktree");
        try {
            assertThrows(AssertionError.class, () -> assertCurrentWorktreePaths(module,
                    sibling.resolve("target/fake-agentic.jar"), currentTarget.resolve("aggregate.jar"),
                    currentTarget.resolve("core.jar")));
        } finally {
            Files.deleteIfExists(sibling);
        }

        Path configuredModule = requiredDirectory("agentic.module.root");
        assertCurrentWorktreePaths(configuredModule, requiredFile("agentic.jar"),
                requiredFile("agentic.aggregate.jar"), requiredFile("agentic.core.jar"));
    }

    @Test
    void finalRuntimeClasspathHasCurrentOwnersNoDuplicateLangChainClasses() throws Exception {
        Path agenticJar = requiredFile("agentic.jar");
        Path aggregateJar = requiredFile("agentic.aggregate.jar");
        Path coreJar = requiredFile("agentic.core.jar");
        Path classpathFile = requiredFile("agentic.runtime.classpath");
        List<Path> runtimeJars = runtimeJars(classpathFile);
        Path normalizedAggregate = aggregateJar.toAbsolutePath().normalize();
        Path normalizedCore = coreJar.toAbsolutePath().normalize();
        assertEquals(1, count(runtimeJars, normalizedAggregate), "runtime classpath must contain current aggregate owner exactly once");
        assertEquals(1, count(runtimeJars, normalizedCore), "runtime classpath must contain current Core owner exactly once");

        List<Path> all = new ArrayList<Path>();
        all.add(agenticJar.toAbsolutePath().normalize());
        all.addAll(runtimeJars);
        Map<String, List<Path>> sources = new LinkedHashMap<String, List<Path>>();
        for (Path jar : all) {
            rejectUnsupportedRuntimeArtifact(jar);
            Map<String, byte[]> classes = jarClassBytes(jar, true);
            for (String entry : classes.keySet()) {
                if (!entry.startsWith("dev/langchain4j/")) continue;
                List<Path> owners = sources.get(entry);
                if (owners == null) {
                    owners = new ArrayList<Path>();
                    sources.put(entry, owners);
                }
                owners.add(jar);
            }
        }
        for (Map.Entry<String, List<Path>> entry : sources.entrySet()) {
            assertEquals(1, entry.getValue().size(), duplicateDiagnostic(entry.getKey(), entry.getValue()));
            if (entry.getKey().startsWith("dev/langchain4j/agentic/")) {
                assertEquals(agenticJar.toAbsolutePath().normalize(), entry.getValue().get(0));
            }
        }
    }

    @Test
    void isolatedRuntimeDiscoversExactlyTheFixedParameterResolverProvider() throws Exception {
        Path agenticJar = requiredFile("agentic.jar");
        List<Path> runtimeJars = runtimeJars(requiredFile("agentic.runtime.classpath"));
        List<URL> urls = new ArrayList<URL>();
        urls.add(agenticJar.toUri().toURL());
        for (Path runtimeJar : runtimeJars) urls.add(runtimeJar.toUri().toURL());
        URLClassLoader loader = new URLClassLoader(urls.toArray(new URL[urls.size()]), null);
        try {
            Class<?> resolver = Class.forName(PARAMETER_RESOLVER, false, loader);
            Class<?> provider = Class.forName(PROVIDER, false, loader);
            java.util.ServiceLoader<?> services = java.util.ServiceLoader.load(resolver, loader);
            List<Object> discovered = new ArrayList<Object>();
            for (Object service : services) discovered.add(service);
            assertEquals(1, discovered.size(), "unexpected ParameterNameResolver providers: " + discovered);
            assertEquals(PROVIDER, discovered.get(0).getClass().getName());
            assertEquals(loader, discovered.get(0).getClass().getClassLoader());
            CodeSource source = discovered.get(0).getClass().getProtectionDomain().getCodeSource();
            assertNotNull(source, "provider has no CodeSource");
            assertEquals(agenticJar.toUri().toURL(), source.getLocation(), "provider must come from current Agentic JAR");
            assertEquals(resolver.getClassLoader(), provider.getClassLoader());
        } finally {
            loader.close();
        }
    }

    @Test
    void everyAllowlistApiAndBehaviorAnchorExecutedExactlyOnce() throws Exception {
        Path allowlist = requiredFile("agentic.allowlist");
        List<Row> rows = parseAllowlist(allowlist);
        Map<String, Integer> executions = new HashMap<String, Integer>();
        List<Path> reportDirectories = new ArrayList<Path>();
        reportDirectories.add(requiredDirectory("agentic.surefire.reports"));
        reportDirectories.add(requiredDirectory("agentic.aggregate.surefire.reports"));
        reportDirectories.add(requiredDirectory("agentic.core.surefire.reports"));
        for (Path reports : reportDirectories) readSurefireReports(reports, executions);
        Set<String> anchors = new LinkedHashSet<String>();
        for (Row row : rows) {
            anchors.add(row.apiAnchor);
            anchors.add(row.behaviorAnchor);
        }
        for (String anchor : anchors) {
            assertEquals(Integer.valueOf(1), executions.get(anchor),
                    "allowlist anchor must execute exactly once and pass: " + anchor
                            + "; actualCount=" + executions.get(anchor));
        }
    }

    private static void assertForeignEntriesRejected(Map<String, byte[]> classes, Path jar, String owner) {
        List<String> foreign = new ArrayList<String>();
        for (String entry : classes.keySet()) {
            if (entry.endsWith(".class") && !entry.startsWith("dev/langchain4j/")) foreign.add(entry);
        }
        assertTrue(foreign.isEmpty(), owner + " JAR contains foreign class entries: " + foreign
                + "; source=" + jar);
    }

    private static void assertCurrentWorktreePaths(Path module, Path agenticJar,
                                                    Path aggregateJar, Path coreJar) {
        Path moduleRoot = module.toAbsolutePath().normalize();
        Path aggregateRoot = moduleRoot.getParent();
        Path agenticTarget = moduleRoot.resolve("target").normalize();
        Path aggregateTarget = aggregateRoot.resolve("target").normalize();
        Path coreTarget = aggregateRoot.resolve("fw-plus-ai-langchain4j-core/target").normalize();
        assertEquals(agenticTarget.resolve("fw-plus-ai-langchain4j-agentic-0.1.1-SNAPSHOT.jar"),
                agenticJar.toAbsolutePath().normalize(), "Agentic JAR is not the current module target artifact");
        assertTrue(Files.isRegularFile(agenticJar), "current Agentic artifact is missing: " + agenticJar);
        assertEquals(aggregateTarget.resolve("fw-plus-ai-langchain4j-0.1.1-SNAPSHOT.jar"),
                aggregateJar.toAbsolutePath().normalize(), "aggregate JAR is not the current worktree artifact");
        assertEquals(coreTarget.resolve("fw-plus-ai-langchain4j-core-0.1.1-SNAPSHOT.jar"),
                coreJar.toAbsolutePath().normalize(), "Core JAR is not the current worktree artifact");
    }

    private static void assertCurrentWorktreeProperties(Path module) {
        Path moduleRoot = module.toAbsolutePath().normalize();
        Path aggregateRoot = moduleRoot.getParent();
        Path agenticTarget = moduleRoot.resolve("target").normalize();
        Path aggregateTarget = aggregateRoot.resolve("target").normalize();
        Path coreTarget = aggregateRoot.resolve("fw-plus-ai-langchain4j-core/target").normalize();
        assertEquals(moduleRoot, Paths.get(System.getProperty("user.dir")).toAbsolutePath().normalize(),
                "Maven test user.dir must be the current Agentic module");
        assertEquals(moduleRoot.resolve("src/main/java"), pathProperty("agentic.source.root"));
        assertEquals(moduleRoot.resolve("UPSTREAM-MANIFEST.txt"), pathProperty("agentic.manifest"));
        assertEquals(aggregateRoot.resolve("compat/overlay-allowlist.tsv"), pathProperty("agentic.allowlist"));
        assertEquals(aggregateTarget.resolve("classes"), pathProperty("agentic.aggregate.classes"));
        assertEquals(coreTarget.resolve("classes"), pathProperty("agentic.core.classes"));
        assertEquals(agenticTarget.resolve("agentic-runtime-classpath.txt"), pathProperty("agentic.runtime.classpath"));
        assertEquals(agenticTarget.resolve("surefire-reports"), pathProperty("agentic.surefire.reports"));
        assertEquals(aggregateTarget.resolve("surefire-reports"), pathProperty("agentic.aggregate.surefire.reports"));
        assertEquals(coreTarget.resolve("surefire-reports"), pathProperty("agentic.core.surefire.reports"));
    }

    private static Path pathProperty(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "missing Maven system property: " + property);
        return Paths.get(value).toAbsolutePath().normalize();
    }

    private static Path requiredFile(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "missing Maven system property: " + property);
        Path path = Paths.get(value).toAbsolutePath().normalize();
        assertTrue(Files.isRegularFile(path), "expected regular file for " + property + ": " + path);
        return path;
    }

    private static Path requiredDirectory(String property) {
        String value = System.getProperty(property);
        assertNotNull(value, "missing Maven system property: " + property);
        Path path = Paths.get(value).toAbsolutePath().normalize();
        assertTrue(Files.isDirectory(path), "expected directory for " + property + ": " + path);
        return path;
    }

    private static Path packagedJar(Path target, String prefix) throws IOException {
        List<Path> candidates = new ArrayList<Path>();
        try (DirectoryStream<Path> entries = Files.newDirectoryStream(target)) {
            for (Path entry : entries) {
                String name = entry.getFileName().toString();
                if (name.startsWith(prefix) && name.endsWith(".jar") && !name.endsWith("-tests.jar")
                        && !name.startsWith("original-")) candidates.add(entry.toAbsolutePath().normalize());
            }
        }
        assertEquals(1, candidates.size(), "expected one packaged JAR with prefix " + prefix);
        return candidates.get(0);
    }

    private static int assertJarClasses(Path jarPath, boolean agentic, String label) throws IOException {
        Map<String, byte[]> classes = jarClassBytes(jarPath, true);
        for (Map.Entry<String, byte[]> entry : classes.entrySet()) {
            byte[] bytes = entry.getValue();
            assertTrue(bytes.length >= 8 && bytes[0] == (byte) 0xca && bytes[1] == (byte) 0xfe
                    && bytes[2] == (byte) 0xba && bytes[3] == (byte) 0xbe,
                    label + " class has invalid bytecode header: " + entry.getKey());
            int major = ((bytes[6] & 0xff) << 8) | (bytes[7] & 0xff);
            assertEquals(52, major, label + " class must target Java 8: " + entry.getKey());
            if (agentic) assertTrue(entry.getKey().startsWith("dev/langchain4j/agentic/"));
        }
        return 52;
    }

    private static Map<String, byte[]> jarClassBytes(Path path, boolean rejectDuplicateEntries) throws IOException {
        Map<String, byte[]> classes = new LinkedHashMap<String, byte[]>();
        Set<String> entriesSeen = new HashSet<String>();
        try (JarFile jar = new JarFile(path.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (rejectDuplicateEntries) {
                    assertTrue(entriesSeen.add(entry.getName()),
                            "duplicate ZIP entry " + entry.getName() + "; source=" + path);
                }
                if (!entry.getName().endsWith(".class")) continue;
                try (InputStream input = jar.getInputStream(entry)) {
                    classes.put(entry.getName(), readAll(input));
                }
            }
        }
        return classes;
    }

    private static Map<String, byte[]> directoryClassBytes(Path root) throws IOException {
        Map<String, byte[]> result = new LinkedHashMap<String, byte[]>();
        java.nio.file.Files.walk(root).forEach(path -> {
            if (Files.isRegularFile(path) && path.toString().endsWith(".class")) {
                try {
                    result.put(root.relativize(path).toString().replace('\\', '/'), Files.readAllBytes(path));
                } catch (IOException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        });
        return result;
    }

    private static byte[] readAll(InputStream input) throws IOException {
        java.io.ByteArrayOutputStream output = new java.io.ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int read;
        while ((read = input.read(buffer)) != -1) output.write(buffer, 0, read);
        return output.toByteArray();
    }

    private static void assertSourceManifest(Path module) throws IOException {
        Path manifest = requiredFile("agentic.manifest");
        Path sourceRoot = requiredDirectory("agentic.source.root");
        Set<String> expected = new LinkedHashSet<String>();
        for (String line : Files.readAllLines(manifest, StandardCharsets.UTF_8)) {
            if (!line.isEmpty() && line.charAt(0) != '#') expected.add(line);
        }
        assertEquals(153, expected.size(), "upstream manifest must contain exactly 153 files");
        Set<String> actual = new LinkedHashSet<String>();
        List<Path> javaFiles = new ArrayList<Path>();
        java.nio.file.Files.walk(sourceRoot).forEach(path -> {
            if (Files.isRegularFile(path) && path.toString().endsWith(".java")) javaFiles.add(path);
        });
        for (Path javaFile : javaFiles) {
            String relative = sourceRoot.relativize(javaFile).toString().replace('\\', '/');
            actual.add(relative);
            assertTrue(relative.startsWith("dev/langchain4j/agentic/"), "source outside Agentic package: " + relative);
        }
        Set<String> allowedHelpers = new HashSet<String>();
        allowedHelpers.add("dev/langchain4j/agentic/internal/AgenticRecordComponentMetadata.java");
        allowedHelpers.add("dev/langchain4j/agentic/internal/CollectionHelper.java");
        allowedHelpers.add("dev/langchain4j/agentic/internal/StringHelper.java");
        Set<String> allowed = new LinkedHashSet<String>(expected);
        allowed.addAll(allowedHelpers);
        assertEquals(156, actual.size(), "Agentic production source count must be 153 upstream plus 3 helpers");
        assertEquals(allowed, actual, "Agentic production source differs from manifest plus helper allowlist");
    }

    private static void assertOwnerBoundaries(Path aggregateJar, Path coreJar) throws IOException {
        Map<String, byte[]> aggregate = jarClassBytes(aggregateJar, false);
        Map<String, byte[]> core = jarClassBytes(coreJar, false);
        assertForeignEntriesRejected(aggregate, aggregateJar, "aggregate");
        assertForeignEntriesRejected(core, coreJar, "Core");
        for (String entry : aggregate.keySet()) {
            assertFalse(entry.contains("/spring/"), "aggregate owner contains Spring class: " + entry
                    + "; source=" + aggregateJar);
            assertFalse(entry.startsWith("dev/langchain4j/agentic/"),
                    "aggregate owner contains Agentic class: " + entry + "; source=" + aggregateJar);
            assertFalse(core.containsKey(entry), "aggregate/Core owner overlap: " + entry
                    + "; aggregate=" + aggregateJar + "; Core=" + coreJar);
        }
        for (String entry : core.keySet()) {
            assertFalse(entry.startsWith("dev/langchain4j/agentic/"), "Core owner contains Agentic class: " + entry
                    + "; source=" + coreJar);
            assertFalse(entry.contains("/spring/"), "Core owner contains Spring class: " + entry
                    + "; source=" + coreJar);
        }
    }

    private static void assertSpiDescriptor(Path agenticJar) throws IOException {
        try (JarFile jar = new JarFile(agenticJar.toFile())) {
            JarEntry entry = jar.getJarEntry("META-INF/services/" + PARAMETER_RESOLVER);
            assertNotNull(entry, "missing ParameterNameResolver SPI descriptor");
            String text;
            try (InputStream input = jar.getInputStream(entry)) {
                text = decodeUtf8(readAll(input));
            }
            List<String> providers = new ArrayList<String>();
            for (String line : text.split("\\r?\\n", -1)) {
                if (!line.isEmpty() && line.charAt(0) != '#') providers.add(line);
            }
            assertEquals(Collections.singletonList(PROVIDER), providers,
                    "SPI descriptor must contain exactly the Agentic provider");
        }
    }

    private static List<Row> parseAllowlist(Path path) throws IOException {
        String text = decodeUtf8(Files.readAllBytes(path));
        byte[] bytes = Files.readAllBytes(path);
        assertFalse(bytes.length >= 3 && (bytes[0] & 0xff) == 0xef && (bytes[1] & 0xff) == 0xbb
                && (bytes[2] & 0xff) == 0xbf, "allowlist contains UTF-8 BOM");
        List<Row> rows = new ArrayList<Row>();
        Set<String> names = new HashSet<String>();
        String[] lines = text.split("\\r?\\n", -1);
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] columns = line.split("\\t", -1);
            assertEquals(5, columns.length, "allowlist line " + (i + 1) + " must have five columns");
            for (String column : columns) assertFalse(column.isEmpty(), "allowlist line has empty column: " + (i + 1));
            assertTrue(names.add(columns[0]), "duplicate allowlist FQCN: " + columns[0]);
            assertTrue(BINARY_NAME.matcher(columns[0]).matches(), "invalid binary FQCN: " + columns[0]);
            assertTrue(CORE_OWNER.equals(columns[1]) || AGGREGATE_OWNER.equals(columns[1]), "invalid owner: " + columns[1]);
            assertTrue(ANCHOR.matcher(columns[3]).matches(), "invalid API anchor: " + columns[3]);
            assertTrue(ANCHOR.matcher(columns[4]).matches(), "invalid behavior anchor: " + columns[4]);
            rows.add(new Row(columns[0], columns[1], columns[2], columns[3], columns[4]));
        }
        assertFalse(rows.isEmpty(), "allowlist must contain rows");
        return rows;
    }

    private static String decodeUtf8(byte[] bytes) throws IOException {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder()
                .onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        try {
            CharBuffer chars = decoder.decode(ByteBuffer.wrap(bytes));
            return chars.toString();
        } catch (CharacterCodingException exception) {
            throw new IOException("allowlist or descriptor is not valid UTF-8", exception);
        }
    }

    private static Path officialJar(String artifact) {
        String artifactId = artifact.substring(artifact.indexOf(':') + 1);
        Path path = Paths.get(System.getProperty("user.home"), ".m2", "repository", "dev", "langchain4j",
                artifactId, VERSION, artifactId + "-" + VERSION + ".jar");
        assertTrue(Files.isRegularFile(path), "missing fixed LangChain4j 0.35 baseline JAR: " + path);
        return path.toAbsolutePath().normalize();
    }

    private static Map<String, Set<String>> rowsByOwner(List<Row> rows) {
        Map<String, Set<String>> result = new LinkedHashMap<String, Set<String>>();
        result.put(CORE_OWNER, new LinkedHashSet<String>());
        result.put(AGGREGATE_OWNER, new LinkedHashSet<String>());
        for (Row row : rows) result.get(row.owner).add(row.binaryName.replace('.', '/') + ".class");
        return result;
    }

    private static Set<String> intersection(Set<String> left, Set<String> right) {
        Set<String> result = new HashSet<String>(left);
        result.retainAll(right);
        return result;
    }

    private static void assertOverlaySet(Set<String> expected, Set<String> actual, String owner) {
        Set<String> unexpected = new HashSet<String>(actual);
        unexpected.removeAll(expected);
        Set<String> unregisteredSynthetic = new HashSet<String>();
        for (String entry : unexpected) {
            int suffix = entry.indexOf('$');
            String parent = suffix > 0 ? entry.substring(0, suffix) + ".class" : entry;
            if (suffix <= 0 || !entry.substring(suffix + 1).matches("[0-9]+\\.class")
                    || !expected.contains(parent)) {
                unregisteredSynthetic.add(entry);
            }
        }
        assertTrue(unregisteredSynthetic.isEmpty(), overlayDiagnostic(owner, expected, actual));
        Set<String> missing = new HashSet<String>(expected);
        missing.removeAll(actual);
        assertTrue(missing.isEmpty(), overlayDiagnostic(owner, expected, actual));
    }

    private static Set<String> overlayRoots(Set<String> entries, Set<String> expectedRoots) {
        Set<String> result = new HashSet<String>();
        for (String entry : entries) {
            int suffix = entry.indexOf('$');
            if (suffix > 0 && entry.substring(suffix + 1).matches("[0-9]+\\.class")
                    && expectedRoots.contains(entry.substring(0, suffix) + ".class")) {
                result.add(entry.substring(0, suffix) + ".class");
            } else if (suffix <= 0 || !expectedRoots.contains(entry.substring(0, suffix) + ".class")) {
                result.add(entry);
            }
        }
        return result;
    }

    private static String overlayDiagnostic(String owner, Set<String> expected, Set<String> actual) {
        Set<String> missing = new HashSet<String>(expected);
        missing.removeAll(actual);
        Set<String> unexpected = new HashSet<String>(actual);
        unexpected.removeAll(expected);
        return "overlay owner=" + owner + "; missing=" + missing + "; unexpected=" + unexpected;
    }

    private static void assertContainsAllOfficialExceptOverlays(Map<String, byte[]> official,
                                                                  Map<String, byte[]> packaged,
                                                                  Set<String> overlays,
                                                                  String owner) {
        Set<String> missing = new HashSet<String>(official.keySet());
        missing.removeAll(overlays);
        missing.removeAll(packaged.keySet());
        assertTrue(missing.isEmpty(), "packaged " + owner + " JAR misses official classes: " + missing);
    }

    private static void assertPomExcludesMatch(Path module, List<Row> rows) throws Exception {
        Map<String, Set<String>> expected = rowsByOwner(rows);
        Path aggregatePom = module.resolve("../pom.xml").normalize();
        Path corePom = module.resolve("../fw-plus-ai-langchain4j-core/pom.xml").normalize();
        assertEquals(expected.get(AGGREGATE_OWNER), readShadeExcludes(aggregatePom, AGGREGATE_ARTIFACT));
        assertEquals(expected.get(CORE_OWNER), readShadeExcludes(corePom, CORE_ARTIFACT));
    }

    private static Set<String> readShadeExcludes(Path pom, String owner) throws Exception {
        Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(pom.toFile());
        NodeList filters = document.getElementsByTagName("filter");
        Set<String> result = new HashSet<String>();
        for (int i = 0; i < filters.getLength(); i++) {
            Element filter = (Element) filters.item(i);
            if (!owner.equals(childText(filter, "artifact"))) continue;
            NodeList excludes = filter.getElementsByTagName("exclude");
            for (int j = 0; j < excludes.getLength(); j++) result.add(excludes.item(j).getTextContent().trim());
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

    private static List<Path> runtimeJars(Path classpathFile) throws IOException {
        String text = new String(Files.readAllBytes(classpathFile), StandardCharsets.UTF_8).trim();
        assertFalse(text.isEmpty(), "runtime classpath file is empty: " + classpathFile);
        Set<Path> unique = new LinkedHashSet<Path>();
        String[] entries = text.split(java.util.regex.Pattern.quote(java.io.File.pathSeparator));
        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            Path path = Paths.get(entry).toAbsolutePath().normalize();
            assertTrue(Files.isRegularFile(path), "runtime classpath entry is not a file: " + path);
            assertTrue(path.getFileName().toString().endsWith(".jar"), "runtime entry is not a JAR: " + path);
            assertFalse(path.getFileName().toString().endsWith("-tests.jar"), "runtime contains test JAR: " + path);
            assertFalse(path.toString().contains("/test-classes/"), "runtime contains test-classes: " + path);
            assertFalse(path.toString().endsWith("/target/classes"), "runtime contains target/classes: " + path);
            unique.add(path);
        }
        return new ArrayList<Path>(unique);
    }

    private static void rejectUnsupportedRuntimeArtifact(Path path) {
        String name = path.getFileName().toString();
        assertFalse(name.startsWith("langchain4j-agentic-"), "official Java 17 Agentic artifact in runtime: " + path);
        assertFalse(name.matches("langchain4j(-core)?-1\\..*\\.jar"), "LangChain4j 1.x artifact in runtime: " + path);
        if (!Files.isRegularFile(path) || !name.endsWith(".jar")) return;
        try (JarFile jar = new JarFile(path.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith("pom.properties")) continue;
                Properties properties = new Properties();
                try (InputStream input = jar.getInputStream(entry)) {
                    properties.load(input);
                }
                String groupId = properties.getProperty("groupId");
                String version = properties.getProperty("version");
                if ("dev.langchain4j".equals(groupId) && version != null && version.matches("1\\..*")) {
                    throw new AssertionError("LangChain4j 1.x artifact in runtime: " + path
                            + "; metadata=" + entry.getName() + "; groupId=" + groupId
                            + "; artifactId=" + properties.getProperty("artifactId")
                            + "; version=" + version);
                }
            }
        } catch (IOException exception) {
            throw new AssertionError("cannot inspect runtime artifact metadata: " + path, exception);
        }
    }

    private static String duplicateDiagnostic(String entry, List<Path> sources) {
        return "duplicate dev.langchain4j class " + entry + "; all sources=" + sources
                + "; expected owner=" + (entry.startsWith("dev/langchain4j/agentic/") ? "Agentic JAR" : "allowlist owner");
    }

    private static int count(List<Path> paths, Path expected) {
        int count = 0;
        for (Path path : paths) if (expected.equals(path)) count++;
        return count;
    }

    private static String failure(Row row, String entry, String reason) {
        return reason + "; binary FQCN=" + row.binaryName + "; entry=" + entry + "; owner=" + row.owner
                + "; allowlist row=" + row.binaryName + "\\t" + row.owner + "\\t" + row.reason
                + "\\t" + row.apiAnchor + "\\t" + row.behaviorAnchor;
    }

    private static void readSurefireReports(Path reports, Map<String, Integer> executions) throws Exception {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(reports, "TEST-*.xml")) {
            for (Path report : files) {
                Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(report.toFile());
                NodeList cases = document.getElementsByTagName("testcase");
                for (int i = 0; i < cases.getLength(); i++) {
                    Element testcase = (Element) cases.item(i);
                    NodeList failures = testcase.getElementsByTagName("failure");
                    NodeList errors = testcase.getElementsByTagName("error");
                    NodeList skipped = testcase.getElementsByTagName("skipped");
                    if (failures.getLength() != 0 || errors.getLength() != 0 || skipped.getLength() != 0) continue;
                    String className = testcase.getAttribute("classname");
                    String methodName = testcase.getAttribute("name");
                    String anchor = className + "#" + methodName;
                    Integer count = executions.get(anchor);
                    executions.put(anchor, Integer.valueOf(count == null ? 1 : count.intValue() + 1));
                }
            }
        }
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
