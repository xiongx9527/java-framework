package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompilationClasspathResolverTest {

    @Test
    void extractsAndCleansSpringBootExecutableArchiveClasspath() throws Exception {
        Path archive = Files.createTempFile("magic-java-entity-boot-", ".jar");
        URLClassLoader classLoader = null;
        CompilationClasspathResolver.ResolvedClasspath classpath = null;
        Path extractedRoot = null;
        try {
            writeBootArchive(archive);
            classLoader = new URLClassLoader(new java.net.URL[]{archive.toUri().toURL()}, null);
            classpath = new CompilationClasspathResolver(classLoader).resolve();

            Path classes = findEntryContaining(classpath, "demo/marker.txt");
            Path library = findEntry(classpath, "dependency.jar");
            assertNotNull(classes);
            assertNotNull(library);
            assertTrue(Files.exists(classes.resolve("demo/marker.txt")));
            assertTrue(Files.isRegularFile(library));
            extractedRoot = classes.getParent();
            assertTrue(Files.isDirectory(extractedRoot));
        } finally {
            if (classpath != null) {
                classpath.close();
            }
            if (classLoader != null) {
                classLoader.close();
            }
            Files.deleteIfExists(archive);
        }
        assertNotNull(extractedRoot);
        assertFalse(Files.exists(extractedRoot));
    }

    private static Path findEntry(CompilationClasspathResolver.ResolvedClasspath classpath, String fileName) {
        return Arrays.stream(classpath.asPathString().split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(File::new)
                .map(File::toPath)
                .filter(path -> fileName.equals(path.getFileName().toString()))
                .findFirst()
                .orElse(null);
    }

    private static Path findEntryContaining(CompilationClasspathResolver.ResolvedClasspath classpath, String relativePath) {
        return Arrays.stream(classpath.asPathString().split(java.util.regex.Pattern.quote(File.pathSeparator)))
                .map(File::new)
                .map(File::toPath)
                .filter(path -> Files.exists(path.resolve(relativePath)))
                .findFirst()
                .orElse(null);
    }

    private static void writeBootArchive(Path archive) throws Exception {
        try (JarOutputStream output = new JarOutputStream(Files.newOutputStream(archive))) {
            output.putNextEntry(new JarEntry("BOOT-INF/classes/"));
            output.closeEntry();
            output.putNextEntry(new JarEntry("BOOT-INF/classes/demo/marker.txt"));
            output.write("marker".getBytes(StandardCharsets.UTF_8));
            output.closeEntry();
            output.putNextEntry(new JarEntry("BOOT-INF/lib/dependency.jar"));
            output.write(emptyJar());
            output.closeEntry();
        }
    }

    private static byte[] emptyJar() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (JarOutputStream ignored = new JarOutputStream(bytes)) {
            // Closing the stream writes a valid empty JAR.
        }
        return bytes.toByteArray();
    }
}
