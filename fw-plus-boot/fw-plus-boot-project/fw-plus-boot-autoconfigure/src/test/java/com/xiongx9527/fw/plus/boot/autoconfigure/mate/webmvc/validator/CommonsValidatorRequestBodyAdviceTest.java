package com.xiongx9527.fw.plus.boot.autoconfigure.mate.webmvc.validator;

import com.xiongx9527.fw.plus.common.validator.CommonsValidator;
import com.xiongx9527.fw.plus.common.validator.CommonsValidatorHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonsValidatorRequestBodyAdviceTest {

    private final CommonsValidatorRequestBodyAdvice advice = new CommonsValidatorRequestBodyAdvice();

    @AfterEach
    void tearDown() {
        CommonsValidatorHolder.clear();
    }

    @Test
    void afterBodyReadClearsContextWhenBodyIsNull() {
        CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, "stale");

        Object result = advice.afterBodyRead(null, null, null, null, null);

        assertNull(result);
        assertNull(CommonsValidatorHolder.get(CommonsValidator.FORM_DATA_KEY));
    }

    @Test
    void handleEmptyBodyClearsContext() {
        CommonsValidatorHolder.set(CommonsValidator.FORM_DATA_KEY, "stale");

        Object result = advice.handleEmptyBody(null, null, null, null, null);

        assertNull(result);
        assertNull(CommonsValidatorHolder.get(CommonsValidator.FORM_DATA_KEY));
    }
}
