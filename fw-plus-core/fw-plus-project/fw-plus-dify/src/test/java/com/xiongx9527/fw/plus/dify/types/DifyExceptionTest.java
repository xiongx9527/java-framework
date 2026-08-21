package com.xiongx9527.fw.plus.dify.types;

public class DifyExceptionTest {

    public void testShouldParseJsonErrorAndKeepHttpStatus() {
        DifyException exception = new DifyException(
                "{\"status\":400,\"code\":\"invalid_param\",\"message\":\"bad request\",\"params\":\"name\"}",
                422
        );

        assertEquals(Integer.valueOf(422), exception.getHttpStatus());
        assertEquals(Integer.valueOf(400), exception.getDifyStatus());
        assertEquals(Integer.valueOf(400), exception.getStatus());
        assertEquals("invalid_param", exception.getDifyCode());
        assertEquals("bad request", exception.getMessage());
        assertEquals("name", exception.getParams());
        assertContains(exception.getOriginal(), "invalid_param");
    }

    public void testShouldKeepNonJsonErrorBody() {
        DifyException exception = new DifyException("Bad Gateway", 502);

        assertEquals(Integer.valueOf(502), exception.getHttpStatus());
        assertEquals(Integer.valueOf(502), exception.getStatus());
        assertEquals("Bad Gateway", exception.getMessage());
        assertEquals("Bad Gateway", exception.getOriginal());
    }

    public void testShouldUseStableMessageForEmptyErrorBody() {
        DifyException exception = new DifyException(null, 500);

        assertEquals(Integer.valueOf(500), exception.getHttpStatus());
        assertEquals(Integer.valueOf(500), exception.getStatus());
        assertEquals("Dify request failed with HTTP status 500", exception.getMessage());
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
}
