package com.xiongx9527.fw.plus.ai.interactive;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class QuestionValidatorTest {

    private final QuestionValidator validator = new QuestionValidator();

    @Test
    void requiredBlankInvalid() {
        Question q = Question.builder().name("x").required(true).build();
        assertFalse(validator.validate(q, "  ").isValid());
        assertTrue(validator.validate(q, "ok").isValid());
    }

    @Test
    void patternEnforced() {
        Question q = Question.builder().name("d").pattern("^\\d{4}$").build();
        assertFalse(validator.validate(q, "ab").isValid());
        assertTrue(validator.validate(q, "2026").isValid());
    }

    @Test
    void numericRange() {
        Question q = Question.builder().name("n").min(1L).max(10L).build();
        assertFalse(validator.validate(q, "0").isValid());
        assertTrue(validator.validate(q, "5").isValid());
        assertFalse(validator.validate(q, "99").isValid());
    }
}
