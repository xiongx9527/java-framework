package com.xiongx9527.fw.plus.ai.base.retrieval;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 轻量分词：英文 / 数字按连续串切分，CJK 按单字切分。用于词法检索的 TF-IDF 计算。
 */
public final class TextTokenizer {

    private TextTokenizer() {
    }

    public static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<String>();
        if (text == null || text.isEmpty()) {
            return tokens;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < lower.length(); i++) {
            char c = lower.charAt(i);
            if (isCjk(c)) {
                flush(buf, tokens);
                tokens.add(String.valueOf(c));
            } else if (Character.isLetterOrDigit(c)) {
                buf.append(c);
            } else {
                flush(buf, tokens);
            }
        }
        flush(buf, tokens);
        return tokens;
    }

    private static void flush(StringBuilder buf, List<String> tokens) {
        if (buf.length() > 0) {
            tokens.add(buf.toString());
            buf.setLength(0);
        }
    }

    private static boolean isCjk(char c) {
        return c >= 0x4E00 && c <= 0x9FFF;
    }
}
