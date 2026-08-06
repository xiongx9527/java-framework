package com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler;

import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryJavaEntityCompilerTest {

    private final JavaSourceAnalyzer analyzer = new JavaSourceAnalyzer();
    private final InMemoryJavaEntityCompiler compiler = new InMemoryJavaEntityCompiler();

    @Test
    void compilesCrossResourceEntitiesIntoOneGenerationClassLoader() throws Exception {
        JavaSourceUnit user = source("user", "User",
                "package demo; public class User { public String name; }");
        JavaSourceUnit dept = source("dept", "Dept",
                "package demo; public class Dept { public User owner; }");

        CompiledGeneration generation = compiler.compile(7L, Arrays.asList(user, dept));
        Class<?> userClass = generation.getClasses().get("demo.User");
        Class<?> deptClass = generation.getClasses().get("demo.Dept");
        Object userObject = userClass.getDeclaredConstructor().newInstance();
        Object deptObject = deptClass.getDeclaredConstructor().newInstance();
        userClass.getField("name").set(userObject, "Alice");
        deptClass.getField("owner").set(deptObject, userObject);

        assertEquals(7L, generation.getVersion());
        assertSame(generation.getClassLoader(), userClass.getClassLoader());
        assertSame(generation.getClassLoader(), deptClass.getClassLoader());
        assertEquals("Alice", userClass.getField("name").get(deptClass.getField("owner").get(deptObject)));
    }

    @Test
    void compilerCanResolveTypesFromApplicationClasspath() throws Exception {
        JavaSourceUnit type = source("typed", "TypedEntity",
                "package demo;\n"
                        + "import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;\n"
                        + "public class TypedEntity {\n"
                        + "  public Class<?> modelType() { return JavaEntityInfo.class; }\n"
                        + "}");

        CompiledGeneration generation = compiler.compile(1L, Collections.singletonList(type));
        Object instance = generation.getClasses().get("demo.TypedEntity").getDeclaredConstructor().newInstance();
        Method method = instance.getClass().getMethod("modelType");

        assertEquals(JavaEntityInfo.class, method.invoke(instance));
    }

    @Test
    void mapsCompilerDiagnosticsBackToResource() {
        JavaSourceUnit broken = source("broken-resource", "Broken",
                "package demo; import java.lang.NotAType; public class Broken { NotAType value; }");

        JavaEntityCompilationException exception = assertThrows(JavaEntityCompilationException.class,
                () -> compiler.compile(1L, Collections.singletonList(broken)));

        JavaEntityDiagnostic diagnostic = exception.getDiagnostics().get(0);
        assertEquals("broken-resource", diagnostic.getResourceId());
        assertTrue(diagnostic.getLine() > 0);
        assertTrue(diagnostic.getColumn() > 0);
        assertTrue(diagnostic.getMessage().contains("NotAType"));
    }

    @Test
    void recompilingSameClassCreatesNewGenerationClass() throws Exception {
        JavaSourceUnit first = source("user", "User",
                "package demo; public class User { public String first; }");
        JavaSourceUnit second = source("user", "User",
                "package demo; public class User { public String second; }");

        CompiledGeneration firstGeneration = compiler.compile(1L, Collections.singletonList(first));
        CompiledGeneration secondGeneration = compiler.compile(2L, Collections.singletonList(second));
        Class<?> firstClass = firstGeneration.getClasses().get("demo.User");
        Class<?> secondClass = secondGeneration.getClasses().get("demo.User");

        assertNotSame(firstGeneration.getClassLoader(), secondGeneration.getClassLoader());
        assertNotSame(firstClass, secondClass);
        assertEquals("first", firstClass.getField("first").getName());
        assertEquals("second", secondClass.getField("second").getName());
    }

    @Test
    void compilesEmptySourceSetAsEmptyGeneration() {
        CompiledGeneration generation = compiler.compile(3L, Collections.<JavaSourceUnit>emptyList());

        assertEquals(3L, generation.getVersion());
        assertTrue(generation.getClasses().isEmpty());
    }

    private JavaSourceUnit source(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return analyzer.analyze(info);
    }
}
