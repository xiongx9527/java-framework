package dev.langchain4j.agentic.internal;

public final class StringHelperTest {

    private StringHelperTest() {
    }

    public static void main(String[] args) {
        testIsBlankPreservesWhitespaceSemantics();
        System.out.println("StringHelper contract passed");
    }

    private static void testIsBlankPreservesWhitespaceSemantics() {
        assertTrue(StringHelper.isBlank(""), "empty string must be blank");
        assertTrue(StringHelper.isBlank(" \t\n"), "ASCII whitespace must be blank");
        assertTrue(StringHelper.isBlank(" "), "Unicode EM SPACE must be blank");
        assertFalse(StringHelper.isBlank(" "), "non-breaking space is not Character whitespace");
        assertFalse(StringHelper.isBlank("  value  "), "non-whitespace content must not be blank");
        try {
            StringHelper.isBlank(null);
            throw new AssertionError("null must be rejected");
        } catch (NullPointerException expected) {
            // expected
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
    }
}
