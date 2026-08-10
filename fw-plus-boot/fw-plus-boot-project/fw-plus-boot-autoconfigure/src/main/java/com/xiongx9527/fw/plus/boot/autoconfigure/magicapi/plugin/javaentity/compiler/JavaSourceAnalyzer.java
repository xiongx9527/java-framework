package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;

import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class JavaSourceAnalyzer {

    private final JavaCompiler compiler;

    public JavaSourceAnalyzer() {
        this(ToolProvider.getSystemJavaCompiler());
    }

    public JavaSourceAnalyzer(JavaCompiler compiler) {
        this.compiler = compiler;
    }

    public JavaSourceUnit analyze(JavaEntityInfo entity) {
        if (compiler == null) {
            throw failure(entity, 0, 0, "compiler.unavailable", "当前运行环境未提供 JDK JavaCompiler");
        }
        if (entity == null || entity.getScript() == null || entity.getScript().trim().isEmpty()) {
            throw failure(entity, 0, 0, "entity.source.empty", "动态实体 Java 源码不能为空");
        }

        DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
        StandardJavaFileManager fileManager = compiler.getStandardFileManager(collector, Locale.ROOT, null);
        try {
            JavaFileObject source = new SourceFileObject(entity.getName(), entity.getScript());
            JavaCompiler.CompilationTask task = compiler.getTask(null, fileManager, collector,
                    Arrays.asList("-proc:none", "-source", "8"), null, Collections.singletonList(source));
            Iterable<?> parsed = (Iterable<?>) invokeTreeMethod(task,
                    "com.sun.source.util.JavacTask", "parse");
            List<JavaEntityDiagnostic> parserErrors = parserErrors(entity, collector);
            if (!parserErrors.isEmpty()) {
                throw new JavaEntityCompilationException(parserErrors);
            }

            Object unit = parsed.iterator().next();
            List<Object> publicClasses = new ArrayList<>();
            Iterable<?> declarations = (Iterable<?>) invokeTreeMethod(unit,
                    "com.sun.source.tree.CompilationUnitTree", "getTypeDecls");
            for (Object declaration : declarations) {
                Object kind = invokeTreeMethod(declaration, "com.sun.source.tree.Tree", "getKind");
                if ("CLASS".equals(String.valueOf(kind)) && isPublicClass(declaration)) {
                    publicClasses.add(declaration);
                }
            }
            if (publicClasses.size() != 1) {
                throw failure(entity, 0, 0, "entity.public-class.count",
                        "每个动态实体 Resource 必须且只能包含一个顶级公开类");
            }

            String simpleName = String.valueOf(invokeTreeMethod(publicClasses.get(0),
                    "com.sun.source.tree.ClassTree", "getSimpleName"));
            if (!simpleName.equals(entity.getName())) {
                throw failure(entity, 0, 0, "entity.resource-name.mismatch",
                        "Resource 名称 " + entity.getName() + " 必须与公开类 " + simpleName + " 一致");
            }
            Object packageNode = invokeTreeMethod(unit,
                    "com.sun.source.tree.CompilationUnitTree", "getPackageName");
            String packageName = packageNode == null ? "" : packageNode.toString();
            String className = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
            return new JavaSourceUnit(entity.getId(), entity.getName(), entity.getScript(),
                    packageName, simpleName, className);
        } catch (JavaEntityCompilationException exception) {
            throw exception;
        } catch (Exception exception) {
            throw failure(entity, 0, 0, "compiler.source.parse", "无法解析动态实体 Java 语法树");
        } finally {
            try {
                fileManager.close();
            } catch (IOException ignored) {
            }
        }
    }

    private static boolean isPublicClass(Object declaration) throws Exception {
        Object modifiers = invokeTreeMethod(declaration,
                "com.sun.source.tree.ClassTree", "getModifiers");
        Set<?> flags = (Set<?>) invokeTreeMethod(modifiers,
                "com.sun.source.tree.ModifiersTree", "getFlags");
        for (Object flag : flags) {
            if ("public".equalsIgnoreCase(String.valueOf(flag))) {
                return true;
            }
        }
        return false;
    }

    private static Object invokeTreeMethod(Object target, String interfaceName, String methodName) throws Exception {
        ClassLoader classLoader = target.getClass().getClassLoader();
        Class<?> interfaceType = Class.forName(interfaceName, false, classLoader);
        Method method = interfaceType.getMethod(methodName);
        try {
            return method.invoke(target);
        } catch (InvocationTargetException exception) {
            Throwable cause = exception.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw exception;
        }
    }

    private static List<JavaEntityDiagnostic> parserErrors(JavaEntityInfo entity,
                                                            DiagnosticCollector<JavaFileObject> collector) {
        List<JavaEntityDiagnostic> errors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : collector.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR) {
                errors.add(new JavaEntityDiagnostic(resourceId(entity), diagnostic.getLineNumber(),
                        diagnostic.getColumnNumber(), diagnostic.getCode(), sanitize(diagnostic.getMessage(Locale.ROOT))));
            }
        }
        return errors;
    }

    private static JavaEntityCompilationException failure(JavaEntityInfo entity, long line, long column,
                                                           String code, String message) {
        return new JavaEntityCompilationException(
                new JavaEntityDiagnostic(resourceId(entity), line, column, code, sanitize(message)));
    }

    private static String resourceId(JavaEntityInfo entity) {
        return entity == null ? "" : entity.getId();
    }

    private static String sanitize(String message) {
        String sanitized = message == null ? "" : message.replace('\r', ' ').replace('\n', ' ');
        return sanitized.length() <= 500 ? sanitized : sanitized.substring(0, 500);
    }

    private static final class SourceFileObject extends SimpleJavaFileObject {

        private final String source;

        private SourceFileObject(String resourceName, String source) {
            super(URI.create("string:///" + (resourceName == null ? "Entity" : resourceName) + Kind.SOURCE.extension),
                    Kind.SOURCE);
            this.source = source;
        }

        @Override
        public CharSequence getCharContent(boolean ignoreEncodingErrors) {
            return source;
        }
    }
}
