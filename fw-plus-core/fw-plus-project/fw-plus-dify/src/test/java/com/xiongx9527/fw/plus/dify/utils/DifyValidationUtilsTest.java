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

    public void testNormalizeServerShouldNotExposeSensitiveMalformedServerInExceptionChain() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                DifyValidationUtils.normalizeServer(
                        "http://user:secret@example .com/path?apiKey=sk-xxx&password=pw",
                        "app",
                        "build"
                );
            }
        });

        assertNotContains(exception.getMessage(), "user:secret");
        assertNotContains(exception.getMessage(), "apiKey=sk-xxx");
        assertNotContains(exception.getMessage(), "password=pw");
        if (exception.getCause() != null) {
            assertNotContains(exception.getCause().getMessage(), "user:secret");
            assertNotContains(exception.getCause().getMessage(), "apiKey=sk-xxx");
            assertNotContains(exception.getCause().getMessage(), "password=pw");
        }
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

    private void assertNotContains(String actual, String unexpectedPart) {
        if (actual != null && actual.contains(unexpectedPart)) {
            throw new AssertionError("expected <" + actual + "> not to contain <" + unexpectedPart + ">");
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
