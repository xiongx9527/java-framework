package com.xiongx9527.fw.plus.dify.utils;

import com.xiongx9527.fw.plus.dify.types.DifyClientException;

public class DifyBuilderValidationTest {

    public void testAppBuilderShouldRejectBlankServer() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                AppClientBuilder.builder(" ", "app-key").chat().build();
            }
        });

        assertContains(exception.getMessage(), "app");
        assertContains(exception.getMessage(), "server");
    }

    public void testDatasetBuilderShouldRejectBlankApiKey() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                DatasetBuilder.builder("https://api.dify.ai", " ").build();
            }
        });

        assertContains(exception.getMessage(), "dataset");
        assertContains(exception.getMessage(), "apiKey");
    }

    public void testDatasetBuilderOfShouldRejectBlankApiKey() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                DatasetBuilder.builder("https://api.dify.ai", " ").of("dataset-id");
            }
        });

        assertContains(exception.getMessage(), "dataset");
        assertContains(exception.getMessage(), "apiKey");
    }

    public void testDatasetBuilderOfDocumentShouldRejectNullApiKey() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                DatasetBuilder.builder("https://api.dify.ai", null).ofDocument("dataset-id", "document-id");
            }
        });

        assertContains(exception.getMessage(), "dataset");
        assertContains(exception.getMessage(), "apiKey");
    }

    public void testWebBuilderShouldRejectBlankPassword() {
        DifyClientException exception = assertThrowsDifyClientException(new ThrowingRunnable() {
            @Override
            public void run() {
                WebClientBuilder.builder("https://api.dify.ai", "user@example.com", " ").connect();
            }
        });

        assertContains(exception.getMessage(), "web");
        assertContains(exception.getMessage(), "password");
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

    private interface ThrowingRunnable {
        void run();
    }
}
