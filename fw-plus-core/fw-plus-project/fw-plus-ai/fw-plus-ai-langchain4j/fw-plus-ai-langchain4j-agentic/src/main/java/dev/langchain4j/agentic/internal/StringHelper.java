package dev.langchain4j.agentic.internal;

import java.util.Objects;

public final class StringHelper {

    private StringHelper() {
    }

    public static boolean isBlank(String value) {
        Objects.requireNonNull(value, "value");
        for (int index = 0; index < value.length(); ) {
            int codePoint = value.codePointAt(index);
            if (!Character.isWhitespace(codePoint)) {
                return false;
            }
            index += Character.charCount(codePoint);
        }
        return true;
    }
}
