package com.xiongx9527.fw.plus.ai.interactive;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DefaultAnswerExtractorTest {

    @Test
    void textNoPatternTakesWhole() {
        Question q = Question.builder().name("x").kind(Question.Kind.TEXT).build();
        ExtractionResult r = new DefaultAnswerExtractor().extract(q, "随便说的内容");
        assertTrue(r.isCertain());
        assertEquals("随便说的内容", r.getValue());
    }

    @Test
    void textPatternCapturesGroup() {
        Question q = Question.builder().name("d").kind(Question.Kind.TEXT)
                .pattern("(\\d{4}-\\d{2}-\\d{2})").build();
        ExtractionResult r = new DefaultAnswerExtractor().extract(q, "日期2026-11-10比较好");
        assertTrue(r.isCertain());
        assertEquals("2026-11-10", r.getValue());
    }

    @Test
    void singleChoiceOneHit() {
        List<ClarifyOption> opts = Arrays.asList(
                ClarifyOption.builder().id("am").label("上午").build(),
                ClarifyOption.builder().id("pm").label("下午").build());
        Question q = Question.builder().name("t").kind(Question.Kind.SINGLE_CHOICE).options(opts).build();
        ExtractionResult r = new DefaultAnswerExtractor().extract(q, "上午开会");
        assertTrue(r.isCertain());
        assertEquals("am", r.getValue());
    }

    @Test
    void singleChoiceAmbiguousUncertain() {
        List<ClarifyOption> opts = Arrays.asList(
                ClarifyOption.builder().id("am").label("上午").build(),
                ClarifyOption.builder().id("pm").label("下午").build());
        Question q = Question.builder().name("t").kind(Question.Kind.SINGLE_CHOICE).options(opts).build();
        ExtractionResult r = new DefaultAnswerExtractor().extract(q, "上午和下午都行");
        assertFalse(r.isCertain());
    }

    @Test
    void multiChoiceCollectsAll() {
        List<ClarifyOption> opts = Arrays.asList(
                ClarifyOption.builder().id("a").label("A").build(),
                ClarifyOption.builder().id("b").label("B").build(),
                ClarifyOption.builder().id("c").label("C").build());
        Question q = Question.builder().name("m").kind(Question.Kind.MULTI_CHOICE).options(opts).build();
        ExtractionResult r = new DefaultAnswerExtractor().extract(q, "选 A 和 C");
        assertTrue(r.isCertain());
        assertEquals(Arrays.asList("a", "c"), r.getValue());
    }
}
