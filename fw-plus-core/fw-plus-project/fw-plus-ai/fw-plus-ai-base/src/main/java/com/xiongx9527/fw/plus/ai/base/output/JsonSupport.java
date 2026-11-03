package com.xiongx9527.fw.plus.ai.base.output;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * 稳定提取 JSON：从模型输出（常夹杂 markdown 围栏或散文）中，可靠地取出首个 JSON 子串。
 *
 * <p>实现不依赖任何 JSON 库——只做括号匹配，且尊重字符串字面量与转义，
 * 因此可在 {@code base} 中零依赖使用；真正的反序列化交由调用方注入的解析器完成。</p>
 */
public final class JsonSupport {

    private JsonSupport() {
    }

    /**
     * 提取首个 JSON 对象或数组子串。
     *
     * @return 提取出的 JSON 文本；无法定位则返回 {@code null}
     */
    public static String extractJson(String text) {
        String stripped = stripFence(text);
        if (stripped == null) {
            return null;
        }
        int obj = indexOfBalanced(stripped, '{', '}');
        int arr = indexOfBalanced(stripped, '[', ']');
        if (obj < 0 && arr < 0) {
            // 文本中没有任何 JSON 起始括号，判定为无 JSON
            return null;
        }
        if (obj < 0) {
            return arr < 0 ? null : substring(stripped, arr);
        }
        if (arr < 0) {
            return substring(stripped, obj);
        }
        // 取更靠前的那个 JSON 起点
        int start = Math.min(obj, arr);
        return substring(stripped, start);
    }

    /** 仅提取 JSON 对象子串（以 {@code {} 起）。 */
    public static String extractJsonObject(String text) {
        String stripped = stripFence(text);
        int obj = indexOfBalanced(stripped, '{', '}');
        return obj < 0 ? null : substring(stripped, obj);
    }

    /** 仅提取 JSON 数组子串（以 {@code [} 起）。 */
    public static String extractJsonArray(String text) {
        String stripped = stripFence(text);
        int arr = indexOfBalanced(stripped, '[', ']');
        return arr < 0 ? null : substring(stripped, arr);
    }

    private static String substring(String text, int startIndex) {
        return text.substring(startIndex, endOfBalanced(text, text.charAt(startIndex), startIndex));
    }

    /**
     * 解析兜底：在 {@link #extractJson(String)} 失败时对文本做「尽力修复」再提取。
     *
     * <p>修复手段（保守、不依赖 JSON 库）：</p>
     * <ol>
     *   <li>剥离 markdown 围栏与首尾空白；</li>
     *   <li>去掉键值/数组元素后的多余逗号（如 {@code {\"a\":1,}}）；</li>
     *   <li>按括号深度补上缺失的闭合括号（处理被截断的输出）；</li>
     *   <li>再次尝试 {@link #extractJson(String)}。</li>
     * </ol>
     *
     * @return 修复后可解析的 JSON 文本；无法修复返回 {@code null}
     */
    public static String repairJson(String text) {
        if (text == null) {
            return null;
        }
        String s = stripFence(text).trim();
        if (s.isEmpty()) {
            return null;
        }
        // 1) 去掉尾部多余逗号：, 后面紧跟 } 或 ]
        s = s.replaceAll(",(\\s*[\]}])', "$1");
        // 2) 补上缺失的闭合括号（处理截断）
        s = closeBrackets(s);
        // 3) 再尝试提取
        return extractJson(s);
    }

    /** 跟踪 { [ 的深度，在结尾补上缺失的 } ]（尊重字符串字面量）。 */
    private static String closeBrackets(String text) {
        List<Character> stack = new ArrayList<Character>();
        boolean inString = false;
        boolean escaped = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == '{' || c == '[') {
                stack.add(c);
            } else if (c == '}' || c == ']') {
                if (!stack.isEmpty()) {
                    char top = stack.get(stack.size() - 1);
                    if ((c == '}' && top == '{') || (c == ']' && top == '[')) {
                        stack.remove(stack.size() - 1);
                    }
                }
            }
        }
        if (stack.isEmpty()) {
            return text;
        }
        StringBuilder sb = new StringBuilder(text);
        for (int i = stack.size() - 1; i >= 0; i--) {
            sb.append(stack.get(i) == '{' ? '}' : ']');
        }
        return sb.toString();
    }

    private static String stripFence(String text) {
        if (text == null) {
            return null;
        }
        String t = text.trim();
        if (t.startsWith("```")) {
            int firstNewline = t.indexOf('\n');
            if (firstNewline >= 0) {
                int endFence = t.lastIndexOf("```");
                if (endFence > firstNewline) {
                    return t.substring(firstNewline + 1, endFence).trim();
                }
            }
        }
        return t;
    }

    /** 找到从 {@code open} 起始且括号平衡的片段起点；找不到返回 -1。 */
    private static int indexOfBalanced(String text, char open, char close) {
        int start = text.indexOf(open);
        if (start < 0) {
            return -1;
        }
        int end = endOfBalanced(text, open, start);
        return end > start ? start : -1;
    }

    /** 从 {@code startIndex}（指向 {@code open}）出发，返回匹配 {@code close} 的下标。 */
    private static int endOfBalanced(String text, char open, int startIndex) {
        char close = open == '{' ? '}' : ']';
        int depth = 0;
        boolean inString = false;
        boolean escaped = false;
        for (int i = startIndex; i < text.length(); i++) {
            char c = text.charAt(i);
            if (inString) {
                if (escaped) {
                    escaped = false;
                } else if (c == '\\') {
                    escaped = true;
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '"') {
                inString = true;
            } else if (c == open) {
                depth++;
            } else if (c == close) {
                depth--;
                if (depth == 0) {
                    return i + 1;
                }
            }
        }
        return -1;
    }
}
