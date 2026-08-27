package com.xiongx9527.fw.plus.boot.starter.magicapi.javaentity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.CompiledGeneration;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.InMemoryJavaEntityCompiler;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceAnalyzer;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.compiler.JavaSourceUnit;
import com.xiongx9527.fw.plus.boot.autoconfigure.magicapi.plugin.javaentity.model.JavaEntityInfo;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DynamicEntityAnnotationCompatibilityTest {

    private final JavaSourceAnalyzer analyzer = new JavaSourceAnalyzer();
    private final InMemoryJavaEntityCompiler compiler = new InMemoryJavaEntityCompiler();

    @Test
    void lombokAndBothOrmAnnotationFamiliesWorkOnCompiledEntity() throws Exception {
        JavaSourceUnit source = source("annotated-user", "AnnotatedUser",
                "package demo;\n"
                        + "@lombok.Data\n"
                        + "@lombok.Builder\n"
                        + "@lombok.NoArgsConstructor\n"
                        + "@lombok.AllArgsConstructor\n"
                        + "@com.baomidou.mybatisplus.annotation.TableName(\"sys_user\")\n"
                        + "@com.mybatisflex.annotation.Table(\"sys_user\")\n"
                        + "public class AnnotatedUser {\n"
                        + "  @com.baomidou.mybatisplus.annotation.TableField(\"user_name\")\n"
                        + "  @com.mybatisflex.annotation.Column(\"user_name\")\n"
                        + "  private String userName;\n"
                        + "}\n");

        Class<?> type = compiler.compile(1L, Collections.singletonList(source))
                .getClasses().get("demo.AnnotatedUser");
        Object builder = type.getMethod("builder").invoke(null);
        builder.getClass().getMethod("userName", String.class).invoke(builder, "Alice");
        Object user = builder.getClass().getMethod("build").invoke(builder);

        assertEquals("Alice", type.getMethod("getUserName").invoke(user));
        type.getMethod("setUserName", String.class).invoke(user, "Bob");
        assertEquals("Bob", type.getMethod("getUserName").invoke(user));
        assertEquals("sys_user", annotationValue(type, "com.baomidou.mybatisplus.annotation.TableName"));
        assertEquals("sys_user", annotationValue(type, "com.mybatisflex.annotation.Table"));
        Field field = type.getDeclaredField("userName");
        assertEquals("user_name", annotationValue(field, "com.baomidou.mybatisplus.annotation.TableField"));
        assertEquals("user_name", annotationValue(field, "com.mybatisflex.annotation.Column"));
    }

    @Test
    void fastjsonAndJacksonAnnotationsAffectRealSerialization() throws Exception {
        JavaSourceUnit fastjson1 = source("fastjson1-user", "Fastjson1User",
                beanSource("Fastjson1User",
                        "@com.alibaba.fastjson.annotation.JSONField(name=\"user_name\")",
                        "@com.alibaba.fastjson.annotation.JSONField(serialize=false)", ""));
        JavaSourceUnit fastjson2 = source("fastjson2-user", "Fastjson2User",
                beanSource("Fastjson2User",
                        "@com.alibaba.fastjson2.annotation.JSONField(name=\"user_name\")",
                        "@com.alibaba.fastjson2.annotation.JSONField(serialize=false)", ""));
        JavaSourceUnit jackson = source("jackson-user", "JacksonUser",
                beanSource("JacksonUser",
                        "@com.fasterxml.jackson.annotation.JsonProperty(\"user_name\")\n"
                                + "  @com.fasterxml.jackson.annotation.JsonAlias({\"name\"})\n"
                                + "  @com.fasterxml.jackson.annotation.JsonFormat(shape=com.fasterxml.jackson.annotation.JsonFormat.Shape.STRING)\n"
                                + "  @com.fasterxml.jackson.annotation.JsonInclude(com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)\n"
                                + "  @com.fasterxml.jackson.databind.annotation.JsonSerialize\n"
                                + "  @com.fasterxml.jackson.databind.annotation.JsonDeserialize",
                        "@com.fasterxml.jackson.annotation.JsonIgnore", ""));

        CompiledGeneration generation = compiler.compile(2L, Arrays.asList(fastjson1, fastjson2, jackson));
        Object legacyUser = user(generation.getClasses().get("demo.Fastjson1User"));
        Object modernUser = user(generation.getClasses().get("demo.Fastjson2User"));
        Object jacksonUser = user(generation.getClasses().get("demo.JacksonUser"));

        String legacyJson = com.alibaba.fastjson.JSON.toJSONString(legacyUser);
        String modernJson = com.alibaba.fastjson2.JSON.toJSONString(modernUser);
        String jacksonJson = new ObjectMapper().writeValueAsString(jacksonUser);

        assertSerializedNameAndIgnoredPassword(legacyJson);
        assertSerializedNameAndIgnoredPassword(modernJson);
        assertSerializedNameAndIgnoredPassword(jacksonJson);
        Field jacksonName = jacksonUser.getClass().getDeclaredField("userName");
        assertNotNull(jacksonName.getAnnotation(com.fasterxml.jackson.annotation.JsonAlias.class));
        assertNotNull(jacksonName.getAnnotation(com.fasterxml.jackson.annotation.JsonFormat.class));
        assertNotNull(jacksonName.getAnnotation(com.fasterxml.jackson.annotation.JsonInclude.class));
        assertNotNull(jacksonName.getAnnotation(com.fasterxml.jackson.databind.annotation.JsonSerialize.class));
        assertNotNull(jacksonName.getAnnotation(com.fasterxml.jackson.databind.annotation.JsonDeserialize.class));
    }

    private static Object user(Class<?> type) throws Exception {
        Object user = type.getDeclaredConstructor().newInstance();
        type.getMethod("setUserName", String.class).invoke(user, "Alice");
        type.getMethod("setPassword", String.class).invoke(user, "secret");
        return user;
    }

    private static void assertSerializedNameAndIgnoredPassword(String json) {
        assertTrue(json.contains("\"user_name\":\"Alice\""), json);
        assertFalse(json.contains("password"), json);
        assertFalse(json.contains("secret"), json);
    }

    private static String beanSource(String className, String nameAnnotation,
                                     String passwordAnnotation, String classAnnotation) {
        return "package demo;\n"
                + classAnnotation + "\n"
                + "public class " + className + " {\n"
                + "  " + nameAnnotation + "\n"
                + "  private String userName;\n"
                + "  " + passwordAnnotation + "\n"
                + "  private String password;\n"
                + "  public String getUserName() { return userName; }\n"
                + "  public void setUserName(String value) { this.userName = value; }\n"
                + "  public String getPassword() { return password; }\n"
                + "  public void setPassword(String value) { this.password = value; }\n"
                + "}\n";
    }

    private static String annotationValue(Class<?> type, String annotationName) throws Exception {
        for (Annotation annotation : type.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                return (String) annotation.annotationType().getMethod("value").invoke(annotation);
            }
        }
        throw new AssertionError("未找到注解: " + annotationName);
    }

    private static String annotationValue(Field field, String annotationName) throws Exception {
        for (Annotation annotation : field.getAnnotations()) {
            if (annotation.annotationType().getName().equals(annotationName)) {
                Method value = annotation.annotationType().getMethod("value");
                return (String) value.invoke(annotation);
            }
        }
        throw new AssertionError("未找到注解: " + annotationName);
    }

    private JavaSourceUnit source(String id, String name, String source) {
        JavaEntityInfo info = new JavaEntityInfo();
        info.setId(id);
        info.setName(name);
        info.setScript(source);
        return analyzer.analyze(info);
    }
}
