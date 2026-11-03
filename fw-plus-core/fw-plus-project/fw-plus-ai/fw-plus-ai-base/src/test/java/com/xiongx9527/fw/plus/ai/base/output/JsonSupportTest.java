package com.xiongx9527.fw.plus.ai.base.output;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class JsonSupportTest {

    @Test
    void extractsPlainObject() {
        assertEquals("{\"a\":1}", JsonSupport.extractJson("{\"a\":1}"));
    }

    @Test
    void extractsNestedObjectWithArray() {
        String text = "{\"a\":{\"b\":[1,2]}}";
        assertEquals(text, JsonSupport.extractJson(text));
    }

    @Test
    void stripsMarkdownFence() {
        String input = "```json\n{\"a\":1}\n```";
        assertEquals("{\"a\":1}", JsonSupport.extractJson(input));
    }

    @Test
    void ignoresProseAroundJson() {
        String input = "Sure! Here is the result: {\"a\":1} Hope that helps.";
        assertEquals("{\"a\":1}", JsonSupport.extractJson(input));
    }

    @Test
    void respectsBracesInsideStrings() {
        // 字符串内部出现 } 不应提前终止匹配
        String input = "{\"note\":\"a}b}c\",\"ok\":true}";
        assertEquals(input, JsonSupport.extractJson(input));
    }

    @Test
    void respectsEscapedQuotesInsideStrings() {
        String input = "{\"s\":\"he said \\\"hi\\\"\"}";
        assertEquals(input, JsonSupport.extractJson(input));
    }

    @Test
    void extractsArray() {
        assertEquals("[1,2,3]", JsonSupport.extractJson("[1,2,3]"));
        assertEquals("[1,2,3]", JsonSupport.extractJsonArray("[1,2,3]"));
    }

    @Test
    void extractJsonObjectIgnoresArrays() {
        assertNull(JsonSupport.extractJsonObject("[1,2,3]"));
    }

    @Test
    void returnsNullWhenNoJson() {
        assertNull(JsonSupport.extractJson("no json here"));
        assertNull(JsonSupport.extractJson(null));
    }

    @Test
    void takesEarlierStartBetweenObjectAndArray() {
        String input = "x {\"a\":1} y [2,3] z";
        assertEquals("{\"a\":1}", JsonSupport.extractJson(input));
    }

    @Test
    void repairFixesTrailingComma() {
        assertEquals("{\"a\":1}", JsonSupport.repairJson("{\"a\":1,}"));
    }

    @Test
    void repairFixesTruncatedObject() {
        assertEquals("{\"a\":1,\"b\":2}", JsonSupport.repairJson("{\"a\":1,\"b\":2"));
    }

    @Test
    void repairReturnsNullWhenUnrecoverable() {
        assertNull(JsonSupport.repairJson("no json at all"));
    }
}
