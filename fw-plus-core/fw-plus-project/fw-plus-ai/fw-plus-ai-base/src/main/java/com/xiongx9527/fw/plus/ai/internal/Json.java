package com.xiongx9527.fw.plus.ai.internal;

import java.util.LinkedHashMap;
import java.util.Map;

/** 极简 JSON 对象解析（扁平），供工具参数绑定用；复杂嵌套按原始字符串保留。 */
public final class Json {
    private Json() {}
    public static Map<String, Object> parseObject(String json) {
        Map<String, Object> map = new LinkedHashMap<>();
        if (json == null) return map;
        String s = json.trim();
        if (s.isEmpty() || s.charAt(0) != '{') return map;
        int i = 1, n = s.length();
        while (i < n) {
            while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ',')) i++;
            if (i >= n || s.charAt(i) == '}') break;
            if (s.charAt(i) != '"') break;
            StringBuilder key = new StringBuilder();
            i = readString(s, i, key);
            while (i < n && (Character.isWhitespace(s.charAt(i)) || s.charAt(i) == ':')) i++;
            if (i >= n) break;
            char c = s.charAt(i);
            Object value;
            if (c == '"') { StringBuilder vs = new StringBuilder(); i = readString(s, i, vs); value = vs.toString(); }
            else if (c == 't' || c == 'f' || c == 'n') { int st = i; while (i < n && Character.isLetter(s.charAt(i))) i++; String lit = s.substring(st, i); value = "true".equals(lit) ? Boolean.TRUE : "false".equals(lit) ? Boolean.FALSE : null; }
            else if (c == '{' || c == '[') { int depth = 0, st = i; while (i < n) { char ch = s.charAt(i); if (ch == '{' || ch == '[') depth++; else if (ch == '}' || ch == ']') { depth--; if (depth == 0) { i++; break; } } i++; } value = s.substring(st, i); }
            else { int st = i; while (i < n && ",}]".indexOf(s.charAt(i)) < 0) i++; value = number(s.substring(st, i).trim()); }
            map.put(key.toString(), value);
            while (i < n && Character.isWhitespace(s.charAt(i))) i++;
        }
        return map;
    }
    private static int readString(String s, int i, StringBuilder out) {
        i++; int n = s.length();
        while (i < n) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < n) { char e = s.charAt(i + 1); switch (e) { case 'n': out.append('\n'); break; case 't': out.append('\t'); break; case 'r': out.append('\r'); break; case '"': out.append('"'); break; case '\\': out.append('\\'); break; default: out.append(e); } i += 2; }
            else if (c == '"') { i++; break; }
            else { out.append(c); i++; }
        }
        return i;
    }
    private static Object number(String t) {
        try { if (t.indexOf('.') >= 0 || t.indexOf('e') >= 0 || t.indexOf('E') >= 0) return Double.parseDouble(t); return Long.parseLong(t); }
        catch (NumberFormatException e) { return t; }
    }
}
