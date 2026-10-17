package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support;

import com.xiongx9527.fw.plus.common.util.SpringUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.StandardEnvironment;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Langchain4jOptionsReader JVM 参数兜底测试。
 *
 * <p>验证 options 缺失时从 JVM 参数（-D）读取，key 格式
 * {@code magic.api.langchain4j.<type>.<key>.<name>}，参考 MagicAPIConfigUtils 模式。</p>
 */
class Langchain4jOptionsReaderJvmFallbackTest {

    private static final String TYPE = "langchain4j-chat-model";
    private static final String KEY = "qwen-main";

    @AfterEach
    void tearDown() throws Exception {
        // 清理 SpringUtils 的 static context，避免影响其他测试
        Field field = SpringUtils.class.getDeclaredField("context");
        field.setAccessible(true);
        field.set(null, null);
    }

    @Test
    void shouldFallbackToJvmParameterWhenOptionsMissing() {
        String jvmKey = jvmKey("api-key");
        System.setProperty(jvmKey, "jvm-value");
        try {
            setSpringEnvironment();

            Langchain4jOptionsReader reader = new Langchain4jOptionsReader(TYPE, KEY, new LinkedHashMap<>());
            assertThat(reader.string("api-key", "default")).isEqualTo("jvm-value");
        } finally {
            System.clearProperty(jvmKey);
        }
    }

    @Test
    void shouldPreferOptionsOverJvmParameter() {
        String jvmKey = jvmKey("api-key");
        System.setProperty(jvmKey, "jvm-value");
        try {
            setSpringEnvironment();

            Map<String, Object> options = new LinkedHashMap<>();
            options.put("api-key", "options-value");
            Langchain4jOptionsReader reader = new Langchain4jOptionsReader(TYPE, KEY, options);
            assertThat(reader.string("api-key", "default")).isEqualTo("options-value");
        } finally {
            System.clearProperty(jvmKey);
        }
    }

    @Test
    void shouldReturnDefaultWhenBothMissing() {
        setSpringEnvironment();

        Langchain4jOptionsReader reader = new Langchain4jOptionsReader(TYPE, KEY, new LinkedHashMap<>());
        assertThat(reader.string("api-key", "default")).isEqualTo("default");
    }

    @Test
    void shouldFallbackForRequiredWhenOptionsMissing() {
        String jvmKey = jvmKey("api-key");
        System.setProperty(jvmKey, "jvm-value");
        try {
            setSpringEnvironment();

            Langchain4jOptionsReader reader = new Langchain4jOptionsReader(TYPE, KEY, new LinkedHashMap<>());
            assertThat(reader.required("api-key")).isEqualTo("jvm-value");
        } finally {
            System.clearProperty(jvmKey);
        }
    }

    @Test
    void shouldFallbackForBool() {
        String jvmKey = jvmKey("enable-search");
        System.setProperty(jvmKey, "true");
        try {
            setSpringEnvironment();

            Langchain4jOptionsReader reader = new Langchain4jOptionsReader(TYPE, KEY, new LinkedHashMap<>());
            assertThat(reader.bool("enable-search", false)).isTrue();
        } finally {
            System.clearProperty(jvmKey);
        }
    }

    @Test
    void shouldFallbackForInteger() {
        String jvmKey = jvmKey("top-k");
        System.setProperty(jvmKey, "42");
        try {
            setSpringEnvironment();

            Langchain4jOptionsReader reader = new Langchain4jOptionsReader(TYPE, KEY, new LinkedHashMap<>());
            assertThat(reader.integer("top-k", null)).isEqualTo(42);
        } finally {
            System.clearProperty(jvmKey);
        }
    }

    @Test
    void shouldReportJvmKeyInRequiredError() {
        setSpringEnvironment();

        Langchain4jOptionsReader reader = new Langchain4jOptionsReader(TYPE, KEY, new LinkedHashMap<>());
        try {
            reader.required("api-key");
        } catch (IllegalArgumentException e) {
            assertThat(e.getMessage()).contains("api-key");
            assertThat(e.getMessage()).contains("不能为空");
        }
    }

    private String jvmKey(String name) {
        return "magic.api.langchain4j." + TYPE + "." + KEY + "." + name;
    }

    private void setSpringEnvironment() {
        StandardEnvironment env = new StandardEnvironment();
        GenericApplicationContext context = new GenericApplicationContext();
        context.setEnvironment(env);
        new SpringUtils().setApplicationContext(context);
    }
}
