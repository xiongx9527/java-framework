package com.xiongx9527.fw.plus.ai.interactive;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FormCollectorTest {

    @Test
    void collectsRequiredThenComplete() {
        Form form = Form.builder().id("f").titleKey("t").questions(Arrays.asList(
                Question.builder().name("a").titleKey("ta").required(true).build(),
                Question.builder().name("b").titleKey("tb").required(true).build())).build();
        FormCollector c = new FormCollector(form);

        CollectionResult r1 = c.proceed();
        assertEquals(CollectionResult.Status.INCOMPLETE, r1.getStatus());
        assertNotNull(r1.getClarifyFrame());

        CollectionResult r2 = c.submitAnswer("a", "x");
        assertEquals(CollectionResult.Status.INCOMPLETE, r2.getStatus());

        CollectionResult r3 = c.submitAnswer("b", "y");
        assertEquals(CollectionResult.Status.COMPLETE, r3.getStatus());
        assertEquals("x", c.getAnswers().get("a"));
        assertEquals("y", c.getAnswers().get("b"));
    }

    @Test
    void extractsFromUtterance() {
        Question date = Question.builder().name("d").kind(Question.Kind.TEXT).required(true)
                .pattern("(\\d{4}-\\d{2}-\\d{2})").build();
        Form form = Form.builder().id("f").titleKey("t").questions(Arrays.asList(date)).build();
        FormCollector c = new FormCollector(form);

        CollectionResult r = c.submitUtterance("会议定在2026-11-10", new DefaultAnswerExtractor());
        assertEquals(CollectionResult.Status.COMPLETE, r.getStatus());
        assertEquals("2026-11-10", c.getAnswers().get("d"));
    }

    @Test
    void degradesAfterMaxRounds() {
        Form form = Form.builder().id("f").titleKey("t")
                .questions(Arrays.asList(Question.builder().name("a").required(true).build())).build();
        FormCollector c = new FormCollector(form, 3, 3);

        CollectionResult last = null;
        for (int i = 0; i < 5; i++) {
            last = c.proceed();
        }
        assertEquals(CollectionResult.Status.DEGRADED, last.getStatus());
        assertFalse(c.isComplete());
    }
}
