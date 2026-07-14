package com.xiongx9527.fw.plus.dify.types;

public class ApiConfigTest {

    public void testBuilderShouldKeepDocumentedDefaultsWhenOnlyServerAndApiKeyAreProvided() {
        ApiConfig config = ApiConfig.builder()
                .server("https://api.dify.ai")
                .apiKey("app-key")
                .build();

        assertEquals(30L, config.getConnectTimeoutSeconds());
        assertEquals(30L, config.getReadTimeoutSeconds());
        assertEquals(30L, config.getWriteTimeoutSeconds());
        assertEquals(5, config.getMaxIdleConnections());
        assertEquals(5, config.getKeepAliveDurationMinutes());
        assertTrue(config.isRetryOnConnectionFailure());
    }

    private void assertEquals(Object expected, Object actual) {
        if (expected == null ? actual != null : !expected.equals(actual)) {
            throw new AssertionError("expected:<" + expected + "> but was:<" + actual + ">");
        }
    }

    private void assertTrue(boolean actual) {
        if (!actual) {
            throw new AssertionError("expected:<true> but was:<false>");
        }
    }
}
