package com.xiongx9527.fw.plus.common.validator;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class CommonsValidatorHolderTest {

    public void testGetReturnsUnstoredEmptyContext() {
        try {
            Map<String, Object> context = CommonsValidatorHolder.get();
            context.put("name", "fw-plus");

            assertNull(CommonsValidatorHolder.get("name"));
        } finally {
            CommonsValidatorHolder.clear();
        }
    }

    public void testSetEmptyMapClearsContext() {
        try {
            CommonsValidatorHolder.set("name", "fw-plus");

            CommonsValidatorHolder.set(Collections.<String, Object>emptyMap());

            assertNull(CommonsValidatorHolder.get("name"));
        } finally {
            CommonsValidatorHolder.clear();
        }
    }

    public void testSetNonEmptyMapKeepsValues() {
        try {
            Map<String, Object> context = new HashMap<String, Object>();
            context.put("name", "fw-plus");

            CommonsValidatorHolder.set(context);

            assertEquals("fw-plus", CommonsValidatorHolder.get("name"));
        } finally {
            CommonsValidatorHolder.clear();
        }
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private void assertNull(Object actual) {
        if (actual != null) {
            throw new AssertionError("expected null but was:<" + actual + ">");
        }
    }
}
