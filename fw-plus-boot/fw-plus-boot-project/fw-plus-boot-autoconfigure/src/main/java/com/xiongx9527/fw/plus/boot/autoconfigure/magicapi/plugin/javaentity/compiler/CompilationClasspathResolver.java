package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.stream.Stream;

public class CompilationClasspathResolver {

    private static final String[] MARKER_CLASSES = {
            "lombok.Data",
            "com.baomidou.mybatisplus.annotation.TableName",
            "com.mybatisflex.annotation.Table",
            "com.alibaba.fastjson.annotation.JSONField",
            "com.alibaba.fastjson2.annotation.JSONField",
            "com.fasterxml.jackson.annotation.JsonProperty",
            "com.fasterxml.jackson.databind.annotation.JsonSerialize"
    };

    private final ClassLoader classLoader;

    public CompilationClasspathResolver(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    public ResolvedClasspath resolve() throws IOException {
        Set<Path> candidates = new LinkedHashSet<>();
        addJavaClasspath(candidates);
        addClassLoaderUrls(candidates);
        addCodeSource(candidates, CompilationClasspathResolver.class);
        for (String markerClass : MARKER_CLASSES) {
            try {
                addCodeSource(candidates, Class.forName(markerClass, false, classLoader));
            } catch (ClassNotFoundException ignored) {
            }
        }

        Path temporaryDirectory = null;
        Set<Path> resolved = new LinkedHashSet<>();
        for (Path candidate : candidates) {
            if (Files.isRegularFile(candidate) && candidate.toString().endsWith(".jar") && isBootArchive(candidate)) {
                if (temporaryDirectory == null) {
                    temporaryDirectory = Files.createTempDirectory("magic-java-entity-classpath-");
                }
                resolved.addAll(extractBootArchive(candidate, temporaryDirectory));
            } else if (Files.exists(candidate)) {
                resolved.add(candidate);
            }
        }
        return new ResolvedClasspath(resolved, temporaryDirectory);
    }

    private static void addJavaClasspath(Set<Path> paths) {
        String value = System.getProperty("java.class.path", "");
        for (String entry : value.split(File.pathSeparator)) {
            if (!entry.trim().isEmpty()) {
                paths.add(new File(entry).toPath().toAbsolutePath().normalize());
            }
        }
    }

    private void addClassLoaderUrls(Set<Path> paths) {
        ClassLoader current = classLoader;
        while (current != null) {
            if (current instanceof URLClassLoader) {
                for (URL url : ((URLClassLoader) current).getURLs()) {
                    addUrl(paths, url);
                }
            }
            current = current.getParent();
        }
    }

    private static void addCodeSource(Set<Path> paths, Class<?> type) {
        CodeSource source = type.getProtectionDomain() == null ? null : type.getProtectionDomain().getCodeSource();
        if (source != null) {
            addUrl(paths, source.getLocation());
        }
    }

    private static void addUrl(Set<Path> paths, URL url) {
        try {
            if ("file".equals(url.getProtocol())) {
                paths.add(new File(url.toURI()).toPath().toAbsolutePath().normalize());
            } else if ("jar".equals(url.getProtocol())) {
                URL jarUrl = ((JarURLConnection) url.openConnection()).getJarFileURL();
                if ("file".equals(jarUrl.getProtocol())) {
                    paths.add(new File(jarUrl.toURI()).toPath().toAbsolutePath().normalize());
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static boolean isBootArchive(Path path) {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            return jarFile.getEntry("BOOT-INF/classes/") != null || jarFile.getEntry("BOOT-INF/lib/") != null;
        } catch (IOException ignored) {
            return false;
        }
    }

    private static List<Path> extractBootArchive(Path archive, Path temporaryDirectory) throws IOException {
        List<Path> entries = new ArrayList<>();
        Path classesDirectory = temporaryDirectory.resolve("classes");
        Path librariesDirectory = temporaryDirectory.resolve("lib");
        boolean hasClasses = false;
        try (JarFile jarFile = new JarFile(archive.toFile())) {
            Enumeration<JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry entry = jarEntries.nextElement();
                if (entry.isDirectory()) {
                    continue;
                }
                String name = entry.getName();
                Path target;
                if (name.startsWith("BOOT-INF/classes/")) {
                    target = safeResolve(classesDirectory, name.substring("BOOT-INF/classes/".length()));
                    hasClasses = true;
                } else if (name.startsWith("BOOT-INF/lib/") && name.endsWith(".jar")) {
                    target = safeResolve(librariesDirectory, name.substring("BOOT-INF/lib/".length()));
                    entries.add(target);
                } else {
                    continue;
                }
                Files.createDirectories(target.getParent());
                try (InputStream input = jarFile.getInputStream(entry)) {
                    Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        if (hasClasses) {
            entries.add(0, classesDirectory);
        }
        return entries;
    }

    private static Path safeResolve(Path root, String relative) throws IOException {
        Path target = root.resolve(relative).normalize();
        if (!target.startsWith(root)) {
            throw new IOException("Spring Boot archive 包含非法路径");
        }
        return target;
    }

    public static final class ResolvedClasspath implements Closeable {

        private final List<Path> entries;
        private final Path temporaryDirectory;

        private ResolvedClasspath(Set<Path> entries, Path temporaryDirectory) {
            this.entries = Collections.unmodifiableList(new ArrayList<>(entries));
            this.temporaryDirectory = temporaryDirectory;
        }

        public String asPathString() {
            StringBuilder value = new StringBuilder();
            for (Path entry : entries) {
                if (value.length() > 0) {
                    value.append(File.pathSeparatorChar);
                }
                value.append(entry.toString());
            }
            return value.toString();
        }

        public URL[] asUrls() throws IOException {
            URL[] urls = new URL[entries.size()];
            for (int index = 0; index < entries.size(); index++) {
                urls[index] = entries.get(index).toUri().toURL();
            }
            return urls;
        }

        @Override
        public void close() throws IOException {
            if (temporaryDirectory == null || !Files.exists(temporaryDirectory)) {
                return;
            }
            List<Path> paths = new ArrayList<>();
            try (Stream<Path> stream = Files.walk(temporaryDirectory)) {
                stream.forEach(paths::add);
            }
            Collections.sort(paths, Collections.reverseOrder());
            IOException failure = null;
            for (Path path : paths) {
                try {
                    Files.deleteIfExists(path);
                } catch (IOException exception) {
                    failure = exception;
                }
            }
            if (failure != null) {
                throw failure;
            }
        }
    }
}
