package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import org.junit.jupiter.api.Test;

import javax.tools.ToolProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSourceAnalyzerTest {

    private final JavaSourceAnalyzer analyzer = new JavaSourceAnalyzer();

    @Test
    void analyzesPackageAndAnnotatedPublicClassFromSyntaxTree() {
        JavaEntityInfo info = entity("resource-user", "User",
                "package demo.entity;\n"
                        + "@Deprecated\n"
                        + "public class User {\n"
                        + "    public static class Address {}\n"
                        + "}\n");

        JavaSourceUnit unit = analyzer.analyze(info);

        assertEquals("resource-user", unit.getResourceId());
        assertEquals("User", unit.getResourceName());
        assertEquals("demo.entity", unit.getPackageName());
        assertEquals("User", unit.getSimpleName());
        assertEquals("demo.entity.User", unit.getClassName());
        assertEquals(info.getScript(), unit.getSource());
    }

    @Test
    void analyzesPublicClassInDefaultPackage() {
        JavaSourceUnit unit = analyzer.analyze(entity("resource-user", "User", "public class User {}"));

        assertEquals("", unit.getPackageName());
        assertEquals("User", unit.getClassName());
    }

    @Test
    void rejectsMultipleTopLevelPublicClasses() {
        JavaEntityCompilationException exception = assertThrows(JavaEntityCompilationException.class,
                () -> analyzer.analyze(entity("resource-user", "User",
                        "public class User {}\npublic class Other {}")));

        assertEquals("entity.public-class.count", exception.getDiagnostics().get(0).getCode());
        assertEquals("resource-user", exception.getDiagnostics().get(0).getResourceId());
    }

    @Test
    void rejectsResourceNameThatDoesNotMatchPublicClass() {
        JavaEntityCompilationException exception = assertThrows(JavaEntityCompilationException.class,
                () -> analyzer.analyze(entity("resource-user", "Account", "public class User {}")));

        assertEquals("entity.resource-name.mismatch", exception.getDiagnostics().get(0).getCode());
        assertTrue(exception.getDiagnostics().get(0).getMessage().contains("Account"));
        assertTrue(exception.getDiagnostics().get(0).getMessage().contains("User"));
    }

    @Test
    void reportsParserDiagnosticsWithResourceLocation() {
        JavaEntityCompilationException exception = assertThrows(JavaEntityCompilationException.class,
                () -> analyzer.analyze(entity("resource-user", "User",
                        "package demo;\npublic class User {\n  private String name\n}")));

        JavaEntityDiagnostic diagnostic = exception.getDiagnostics().get(0);
        assertEquals("resource-user", diagnostic.getResourceId());
        assertTrue(diagnostic.getLine() > 0);
        assertTrue(diagnostic.getColumn() > 0);
        assertFalse(diagnostic.getMessage().isEmpty());
    }

    @Test
    void reportsUnavailableSystemCompiler() {
        JavaSourceAnalyzer unavailable = new JavaSourceAnalyzer(null);

        JavaEntityCompilationException exception = assertThrows(JavaEntityCompilationException.class,
                () -> unavailable.analyze(entity("resource-user", "User", "public class User {}")));

        assertEquals("compiler.unavailable", exception.getDiagnostics().get(0).getCode());
    }

    private static JavaEntityInfo entity(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return info;
    }
}
