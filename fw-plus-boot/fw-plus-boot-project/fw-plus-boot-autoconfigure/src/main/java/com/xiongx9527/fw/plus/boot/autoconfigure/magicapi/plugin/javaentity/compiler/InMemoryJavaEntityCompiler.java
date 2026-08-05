package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import javax.annotation.processing.Processor;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class InMemoryJavaEntityCompiler {

    private static final String LOMBOK_PROCESSOR = "lombok.launch.AnnotationProcessorHider$AnnotationProcessor";

    private final JavaCompiler compiler;
    private final ClassLoader parentClassLoader;
    private final CompilationClasspathResolver classpathResolver;

    public InMemoryJavaEntityCompiler() {
        this(ToolProvider.getSystemJavaCompiler(), contextClassLoader());
    }

    public InMemoryJavaEntityCompiler(JavaCompiler compiler, ClassLoader parentClassLoader) {
        this.compiler = compiler;
        this.parentClassLoader = parentClassLoader;
        this.classpathResolver = new CompilationClasspathResolver(parentClassLoader);
    }

    public CompiledGeneration compile(long version, List<JavaSourceUnit> sourceUnits) {
        if (compiler == null) {
            throw new JavaEntityCompilationException(new JavaEntityDiagnostic("", 0, 0,
                    "compiler.unavailable", "当前运行环境未提供 JDK JavaCompiler"));
        }
        List<JavaSourceUnit> units = sourceUnits == null
                ? Collections.<JavaSourceUnit>emptyList() : new ArrayList<>(sourceUnits);
        if (units.isEmpty()) {
            GenerationClassLoader classLoader = new GenerationClassLoader(
                    parentClassLoader, Collections.<String, byte[]>emptyMap());
            return new CompiledGeneration(version, classLoader, Collections.<String, Class<?>>emptyMap());
        }
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        StandardJavaFileManager standardFileManager = compiler.getStandardFileManager(diagnostics, Locale.ROOT, null);
        MemoryJavaFileManager fileManager = new MemoryJavaFileManager(standardFileManager);
        CompilationClasspathResolver.ResolvedClasspath classpath = null;
        GenerationClassLoader generationClassLoader = null;
        URLClassLoader processorClassLoader = null;
        ClassLoader originalContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            classpath = classpathResolver.resolve();
            Map<URI, JavaSourceUnit> sourcesByUri = new LinkedHashMap<>();
            List<JavaFileObject> sources = new ArrayList<>();
            for (JavaSourceUnit unit : units) {
                SourceFileObject source = new SourceFileObject(unit);
                sources.add(source);
                sourcesByUri.put(source.toUri(), unit);
            }

            List<String> options = new ArrayList<>(Arrays.asList(
                    "-source", "8", "-target", "8", "-encoding", "UTF-8",
                    "-classpath", classpath.asPathString()));

            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, diagnostics, options, null, sources);
            processorClassLoader = new LombokProcessorClassLoader(
                    classpath.asUrls(), compiler.getClass().getClassLoader());
            Processor lombokProcessor = createLombokProcessor(processorClassLoader);
            if (lombokProcessor != null) {
                task.setProcessors(Collections.singletonList(lombokProcessor));
                Thread.currentThread().setContextClassLoader(processorClassLoader);
            }
            Boolean success = task.call();
            if (!Boolean.TRUE.equals(success)) {
                throw diagnostics(diagnostics, sourcesByUri);
            }

            generationClassLoader = new GenerationClassLoader(parentClassLoader, fileManager.getByteCodes());
            Map<String, Class<?>> classes = new LinkedHashMap<>();
            for (JavaSourceUnit unit : units) {
                classes.put(unit.getClassName(), Class.forName(unit.getClassName(), false, generationClassLoader));
            }
            return new CompiledGeneration(version, generationClassLoader, classes);
        } catch (JavaEntityCompilationException exception) {
            closeCandidate(generationClassLoader);
            throw exception;
        } catch (Exception exception) {
            closeCandidate(generationClassLoader);
            throw new JavaEntityCompilationException(new JavaEntityDiagnostic("", 0, 0,
                    "compiler.internal", safeMessage(exception)));
        } finally {
            Thread.currentThread().setContextClassLoader(originalContextClassLoader);
            if (processorClassLoader != null) {
                try {
                    processorClassLoader.close();
                } catch (IOException ignored) {
                }
            }
            try {
                fileManager.close();
            } catch (IOException ignored) {
            }
            if (classpath != null) {
                try {
                    classpath.close();
                } catch (IOException ignored) {
                }
            }
        }
    }

    private JavaEntityCompilationException diagnostics(DiagnosticCollector<JavaFileObject> collector,
                                                        Map<URI, JavaSourceUnit> sourcesByUri) {
        List<JavaEntityDiagnostic> result = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
            if (diagnostic.getKind() != Diagnostic.Kind.ERROR) {
                continue;
            }
            JavaSourceUnit unit = diagnostic.getSource() == null
                    ? null : sourcesByUri.get(diagnostic.getSource().toUri());
            result.add(new JavaEntityDiagnostic(unit == null ? "" : unit.getResourceId(),
                    diagnostic.getLineNumber(), diagnostic.getColumnNumber(), diagnostic.getCode(),
                    sanitize(diagnostic.getMessage(Locale.ROOT))));
        }
        if (result.isEmpty()) {
            result.add(new JavaEntityDiagnostic("", 0, 0, "compiler.failed", "动态实体编译失败"));
        }
        return new JavaEntityCompilationException(result);
    }

    private Processor createLombokProcessor(ClassLoader processorClassLoader) throws Exception {
        try {
            Class<?> processorType = Class.forName(LOMBOK_PROCESSOR, true, processorClassLoader);
            return (Processor) processorType.newInstance();
        } catch (ClassNotFoundException exception) {
            return null;
        }
    }

    private static String safeMessage(Exception exception) {
        String message = exception.getMessage();
        return sanitize(message == null ? exception.getClass().getSimpleName() : message);
    }

    private static String sanitize(String message) {
        String sanitized = message == null ? "" : message.replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }

    private static void closeCandidate(GenerationClassLoader classLoader) {
        if (classLoader != null) {
            classLoader.close();
        }
    }

    private static ClassLoader contextClassLoader() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        return classLoader == null ? InMemoryJavaEntityCompiler.class.getClassLoader() : classLoader;
    }

    private static final class SourceFileObject extends SimpleJavaFileObject {

        private final JavaSourceUnit unit;

        private SourceFileObject(JavaSourceUnit unit) {
            super(URI.create("string:///" + unit.getClassName().replace('.', '/') + Kind.SOURCE.extension), Kind.SOURCE);
            this.unit = unit;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return unit.getSource();
        }
    }

    private static final class LombokProcessorClassLoader extends URLClassLoader {

        private LombokProcessorClassLoader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        @Override
        protected synchronized Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("lombok.")) {
                return super.loadClass(name, resolve);
            }
            Class<?> loaded = findLoadedClass(name);
            if (loaded == null) {
                try {
                    loaded = findClass(name);
                } catch (ClassNotFoundException ignored) {
                    loaded = super.loadClass(name, false);
                }
            }
            if (resolve) {
                resolveClass(loaded);
            }
            return loaded;
        }
    }
}
