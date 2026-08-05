package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.JavacTask;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;

import javax.lang.model.element.Modifier;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

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
            JavacTask task = (JavacTask) compiler.getTask(null, fileManager, collector,
                    Arrays.asList("-proc:none", "-source", "8"), null, Collections.singletonList(source));
            Iterable<? extends CompilationUnitTree> parsed = task.parse();
            List<JavaEntityDiagnostic> parserErrors = parserErrors(entity, collector);
            if (!parserErrors.isEmpty()) {
                throw new JavaEntityCompilationException(parserErrors);
            }

            CompilationUnitTree unit = parsed.iterator().next();
            List<ClassTree> publicClasses = new ArrayList<>();
            for (Tree declaration : unit.getTypeDecls()) {
                if (declaration.getKind() == Tree.Kind.CLASS
                        && ((ClassTree) declaration).getModifiers().getFlags().contains(Modifier.PUBLIC)) {
                    publicClasses.add((ClassTree) declaration);
                }
            }
            if (publicClasses.size() != 1) {
                throw failure(entity, 0, 0, "entity.public-class.count",
                        "每个动态实体 Resource 必须且只能包含一个顶级公开类");
            }

            String simpleName = publicClasses.get(0).getSimpleName().toString();
            if (!simpleName.equals(entity.getName())) {
                throw failure(entity, 0, 0, "entity.resource-name.mismatch",
                        "Resource 名称 " + entity.getName() + " 必须与公开类 " + simpleName + " 一致");
            }
            String packageName = unit.getPackageName() == null ? "" : unit.getPackageName().toString();
            String className = packageName.isEmpty() ? simpleName : packageName + "." + simpleName;
            return new JavaSourceUnit(entity.getId(), entity.getName(), entity.getScript(),
                    packageName, simpleName, className);
        } catch (IOException exception) {
            throw failure(entity, 0, 0, "compiler.source.read", "无法读取动态实体 Java 源码");
        } finally {
            try {
                fileManager.close();
            } catch (IOException ignored) {
            }
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
