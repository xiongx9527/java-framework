package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.types.DifyClientException;

public class DifyValidationUtilsTest {

    public void testRequireTextShouldRejectBlankValueWithContext() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                DifyValidationUtils.requireText("  ", "apiKey", "app", "build");
            }
        });

        assertContains(exception.getMessage(), "app");
        assertContains(exception.getMessage(), "build");
        assertContains(exception.getMessage(), "apiKey");
    }

    public void testRequireTextShouldReturnTrimmedValue() {
        assertEquals("https://api.dify.ai", DifyValidationUtils.requireText(
                " https://api.dify.ai ", "server", "dataset", "build"
        ));
    }

    public void testNormalizeServerShouldRejectMalformedServerWithContext() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                DifyValidationUtils.normalizeServer("not-a-url", "app", "build");
            }
        });

        assertContains(exception.getMessage(), "app");
        assertContains(exception.getMessage(), "build");
        assertContains(exception.getMessage(), "server");
    }

    private DifyClientException assertThrowsDifyClientException(ThrowingRunnable runnable) {
        try {
            runnable.run();
        } catch (DifyClientException e) {
            return e;
        }
        throw new AssertionError("expected DifyClientException");
    }

    private void assertContains(String actual, String expectedPart) {
        if (actual == null || !actual.contains(expectedPart)) {
            throw new AssertionError("expected <" + actual + "> to contain <" + expectedPart + ">");
        }
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private interface ThrowingRunnable {
        void run();
    }
}
