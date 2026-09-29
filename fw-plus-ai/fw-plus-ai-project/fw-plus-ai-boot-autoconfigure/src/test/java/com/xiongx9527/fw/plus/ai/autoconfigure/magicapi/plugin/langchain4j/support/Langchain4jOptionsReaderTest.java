package com.xiongx9527.fw.plus.ai.autoconfigure.magicapi.plugin.langchain4j.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * options 强类型读取测试：缺失必填项的完整路径报错、各类型转换、可空数值语义。
 */
class Langchain4jOptionsReaderTest {

    private Langchain4jOptionsReader reader;

    @BeforeEach
    void setUp() {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put("api-key", "sk-test");
        options.put("top-p", 0.8);
        options.put("top-k", 20);
        options.put("enable-search", "true");
        options.put("timeout", "60s");
        options.put("stops", "a, b");
        options.put("flags", Arrays.asList("x", "y"));
        reader = new Langchain4jOptionsReader("langchain4j-chat-model", "qwen-main", options);
    }

    @Test
    void shouldReportFullPathWhenRequiredIsMissing() {
        assertThatThrownBy(() -> reader.required("model-name"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("langchain4j-chat-model[qwen-main].options.model-name");
    }

    @Test
    void shouldReportFullPathWhenRequiredIsBlank() {
        Langchain4jOptionsReader blank = new Langchain4jOptionsReader("langchain4j-tokenizer", "t1",
                single("api-key", "   "));

        assertThatThrownBy(() -> blank.required("api-key"))
                .hasMessageContaining("langchain4j-tokenizer[t1].options.api-key");
    }

    @Test
    void shouldReadStringWithDefault() {
        assertThat(reader.string("api-key", null)).isEqualTo("sk-test");
        assertThat(reader.string("absent", "fallback")).isEqualTo("fallback");
        assertThat(reader.required("api-key")).isEqualTo("sk-test");
    }

    @Test
    void shouldKeepNullDefaultForOptionalDouble() {
        assertThat(reader.dbl("top-p", null)).isEqualTo(0.8d);
        assertThat(reader.dbl("absent", null)).as("缺省且默认值为 null 时不应退化为 0").isNull();
        assertThat(reader.dbl("absent", 0.7d)).isEqualTo(0.7d);
    }

    @Test
    void shouldRejectNonNumericDouble() {
        assertThatThrownBy(() -> reader.dbl("api-key", null))
                .hasMessageContaining("langchain4j-chat-model[qwen-main].options.api-key");
    }

    @Test
    void shouldReadIntegersAndRejectOutOfRange() {
        assertThat(reader.integer("top-k", null)).isEqualTo(20);
        assertThat(reader.longValue("top-k", null)).isEqualTo(20L);

        assertThatThrownBy(() -> new Langchain4jOptionsReader("langchain4j-chat-model", "k",
                single("max-tokens", "99999999999")).integer("max-tokens", null))
                .hasMessageContaining("超出 int 范围");
    }

    @Test
    void shouldReadBooleansAndRejectInvalid() {
        assertThat(reader.bool("enable-search", false)).isTrue();

        assertThatThrownBy(() -> new Langchain4jOptionsReader("langchain4j-chat-model", "k",
                single("enable-search", "yes")).bool("enable-search", false))
                .hasMessageContaining("不是合法布尔值");
    }

    @Test
    void shouldParseDurationFormats() {
        assertThat(reader.duration("timeout", null)).isEqualTo(Duration.ofSeconds(60));
        assertThat(new Langchain4jOptionsReader("langchain4j-chat-model", "k", single("t", "2m"))
                .duration("t", null)).isEqualTo(Duration.ofMinutes(2));
        assertThat(new Langchain4jOptionsReader("langchain4j-chat-model", "k", single("t", "PT1M"))
                .duration("t", null)).isEqualTo(Duration.ofMinutes(1));
        assertThat(new Langchain4jOptionsReader("langchain4j-chat-model", "k", single("t", "1500"))
                .duration("t", null)).isEqualTo(Duration.ofMillis(1500));

        assertThatThrownBy(() -> new Langchain4jOptionsReader("langchain4j-chat-model", "k", single("t", "60x"))
                .duration("t", null))
                .hasMessageContaining("不是合法时长");
    }

    @Test
    void shouldParseStringListFromCsvAndList() {
        assertThat(reader.stringList("stops", null)).containsExactly("a", "b");
        assertThat(reader.stringList("flags", null)).containsExactly("x", "y");
        assertThat(reader.stringList("absent", null)).isEmpty();
    }

    @Test
    void shouldParseStringMapFromObjectJsonAndPairs() {
        Map<String, Object> filters = new LinkedHashMap<String, Object>();
        filters.put("category", "faq");
        assertThat(reader.stringMap("absent", null)).isEmpty();
        assertThat(new Langchain4jOptionsReader("t", "k", single("filters", filters)).stringMap("filters", null))
                .containsEntry("category", "faq");
        assertThat(new Langchain4jOptionsReader("t", "k", single("filters", "{\"category\":\"faq\",\"score\":0.5}"))
                .stringMap("filters", null))
                .containsEntry("category", "faq").containsEntry("score", "0.5");
        assertThat(new Langchain4jOptionsReader("t", "k", single("filters", "category=faq; lang=zh"))
                .stringMap("filters", null))
                .containsEntry("category", "faq").containsEntry("lang", "zh");

        assertThatThrownBy(() -> new Langchain4jOptionsReader("t", "k", single("filters", "category"))
                .stringMap("filters", null))
                .hasMessageContaining("不是合法键值表");
    }

    @Test
    void shouldParseEnumCaseInsensitively() {
        Langchain4jOptionsReader enums = new Langchain4jOptionsReader("langchain4j-chat-model", "k", single("mode", "second"));

        assertThat(enums.enumValue("mode", Sample.class, Sample.FIRST)).isEqualTo(Sample.SECOND);
        assertThat(enums.enumValue("absent", Sample.class, Sample.FIRST)).isEqualTo(Sample.FIRST);

        assertThatThrownBy(() -> enums.enumValue("mode", Other.class, Other.A))
                .hasMessageContaining("不是合法枚举值");
    }

    @Test
    void shouldExposeConfiguredNamesAndPresence() {
        assertThat(reader.has("api-key")).isTrue();
        assertThat(reader.has("absent")).isFalse();
        assertThat(reader.names()).contains("api-key", "top-p");
    }

    enum Sample { FIRST, SECOND }

    enum Other { A, B }

    private static Map<String, Object> single(String name, Object value) {
        Map<String, Object> options = new LinkedHashMap<String, Object>();
        options.put(name, value);
        return options;
    }
}
