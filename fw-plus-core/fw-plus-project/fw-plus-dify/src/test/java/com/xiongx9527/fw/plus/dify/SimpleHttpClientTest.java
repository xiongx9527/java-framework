package com.xiongx9527.fw.plus.dify;

import com.xiongx9527.fw.plus.dify.types.DifyClientException;
public class SimpleHttpClientTest {

    public void testRequestJsonExceptionShouldNotExposeSensitiveQueryParameters() {
        SimpleHttpClient client = SimpleHttpClient.newHttpClient("https://api.dify.ai");
        DifyClientException exception = expectDifyClientException(new ThrowingRequest() {
            @Override
            public void run() {
                client.requestJson(
                        "/v1/messages?apiKey=secret-key&password=pwd&token=raw-token&authorization=Bearer%20abc&safe=value",
                        null,
                        null,
                        null
                );
            }
        });

        assertMessageIsSanitized(exception.getMessage());
        assertContains(exception.getMessage(), "/v1/messages");
    }

    public void testRequestFileExceptionShouldNotExposeSensitiveQueryParameters() {
        SimpleHttpClient client = SimpleHttpClient.newHttpClient("https://api.dify.ai");
        DifyClientException exception = expectDifyClientException(new ThrowingRequest() {
            @Override
            public void run() {
                client.requestFile(
                        "/v1/audio?apiKey=secret-key&password=pwd&token=raw-token&authorization=Bearer%20abc&safe=value",
                        null,
                        null,
                        null
                );
            }
        });

        assertMessageIsSanitized(exception.getMessage());
        assertContains(exception.getMessage(), "/v1/audio");
    }

    private DifyClientException expectDifyClientException(ThrowingRequest request) {
        try {
            request.run();
        } catch (DifyClientException e) {
            return e;
        }
        throw new AssertionError("expected DifyClientException");
    }

    private void assertMessageIsSanitized(String message) {
        assertNotContains(message, "secret-key");
        assertNotContains(message, "pwd");
        assertNotContains(message, "raw-token");
        assertNotContains(message, "Bearer%20abc");
        assertNotContains(message, "safe=value");
        assertNotContains(message, "?");
    }

    private void assertContains(String value, String expectedFragment) {
        if (value == null || !value.contains(expectedFragment)) {
            throw new AssertionError("expected message to contain:<" + expectedFragment + "> but was:<" + value + ">");
        }
    }

    private void assertNotContains(String value, String unexpectedFragment) {
        if (value != null && value.contains(unexpectedFragment)) {
            throw new AssertionError("expected message not to contain:<" + unexpectedFragment + "> but was:<" + value + ">");
        }
    }

    private interface ThrowingRequest {
        void run();
    }
}
