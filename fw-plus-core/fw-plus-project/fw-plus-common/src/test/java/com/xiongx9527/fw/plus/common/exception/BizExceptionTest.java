package com.xiongx9527.fw.plus.common.exception;

public class BizExceptionTest {

	public void testShouldBeTheRuntimeExceptionRoot() {
		assertSame(RuntimeException.class, BizException.class.getSuperclass());
	}

	public void testShouldSupportGenericExceptionConstructors() {
		IllegalStateException cause = new IllegalStateException("cause");
		BizException empty = new BizException();
		BizException message = new BizException("message");
		BizException messageAndCause = new BizException("wrapped", cause);
		BizException formatted = new BizException("value=%s", "42");
		BizException causeOnly = new BizException(cause);

		assertNull(empty.getMessage());
		assertEquals("message", message.getMessage());
		assertEquals("wrapped", messageAndCause.getMessage());
		assertSame(cause, messageAndCause.getCause());
		assertEquals("value=42", formatted.getMessage());
		assertSame(cause, causeOnly.getCause());
		assertEquals(50999000, message.getCode());
		assertNull(message.getData());
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

	private void assertSame(Object expected, Object actual) {
		if (expected != actual) {
			throw new AssertionError("expected same:<" + expected + "> but was:<" + actual + ">");
		}
	}
}
