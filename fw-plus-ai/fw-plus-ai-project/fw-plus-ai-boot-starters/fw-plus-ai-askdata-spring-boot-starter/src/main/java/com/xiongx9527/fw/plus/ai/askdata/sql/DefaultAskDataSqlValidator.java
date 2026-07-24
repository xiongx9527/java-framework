package com.xiongx9527.fw.plus.ai.askdata.sql;

import com.xiongx9527.fw.plus.ai.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.askdata.prompt.AskDataDialect;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class DefaultAskDataSqlValidator implements AskDataSqlValidator {

    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<String>(Arrays.asList(
            "insert", "update", "delete", "drop", "alter", "truncate", "create", "merge",
            "grant", "revoke", "call", "exec", "execute", "set", "use", "begin", "commit", "rollback"));

    @Override
    public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
        if (sql == null || sql.trim().length() == 0) {
            return rejected("SQL 不能为空");
        }
        if (maxRows <= 0) {
            return rejected("最大返回行数必须大于 0");
        }

        ScanResult scanResult = scan(sql.trim());
        if (scanResult.hasMultipleStatements) {
            return rejected("仅允许执行单条 SQL 查询");
        }
        if (scanResult.firstKeyword == null || (!"select".equals(scanResult.firstKeyword)
                && !"with".equals(scanResult.firstKeyword))) {
            return rejected("仅允许 SELECT 或 WITH 查询");
        }
        if (scanResult.dangerousKeyword != null) {
            return rejected("检测到危险关键字: " + scanResult.dangerousKeyword);
        }

        String normalizedSql = stripTrailingSemicolon(sql.trim(), scanResult.trailingSemicolonIndex);
        if (!scanResult.hasTopLevelLimit) {
            normalizedSql = normalizedSql + " limit " + maxRows;
        }
        return AskDataSqlValidationResult.accepted(normalizedSql);
    }

    private AskDataSqlValidationResult rejected(String message) {
        return AskDataSqlValidationResult.rejected(AskDataErrorCode.SQL_REJECTED, message);
    }

    private String stripTrailingSemicolon(String sql, int trailingSemicolonIndex) {
        if (trailingSemicolonIndex >= 0) {
            return sql.substring(0, trailingSemicolonIndex).trim();
        }
        int index = sql.length() - 1;
        while (index >= 0 && Character.isWhitespace(sql.charAt(index))) {
            index--;
        }
        return index >= 0 && sql.charAt(index) == ';' ? sql.substring(0, index).trim() : sql;
    }

    private ScanResult scan(String sql) {
        ScanResult result = new ScanResult();
        int depth = 0;
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (current == '\'') {
                index = skipQuoted(sql, index, '\'');
            } else if (current == '"') {
                index = skipQuoted(sql, index, '"');
            } else if (current == '`') {
                index = skipQuoted(sql, index, '`');
            } else if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                index = skipLineComment(sql, index + 2);
            } else if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                index = skipBlockComment(sql, index + 2);
            } else if (current == ';') {
                if (hasCodeAfter(sql, index + 1)) {
                    result.hasMultipleStatements = true;
                    return result;
                }
                result.trailingSemicolonIndex = index;
                index++;
            } else if (current == '(') {
                depth++;
                index++;
            } else if (current == ')') {
                if (depth > 0) {
                    depth--;
                }
                index++;
            } else if (isWordCharacter(current)) {
                int wordEnd = index + 1;
                while (wordEnd < sql.length() && isWordCharacter(sql.charAt(wordEnd))) {
                    wordEnd++;
                }
                String word = sql.substring(index, wordEnd).toLowerCase();
                if (result.firstKeyword == null) {
                    result.firstKeyword = word;
                }
                if (DANGEROUS_KEYWORDS.contains(word)) {
                    result.dangerousKeyword = word;
                    return result;
                }
                if (depth == 0 && "limit".equals(word)) {
                    result.hasTopLevelLimit = true;
                }
                index = wordEnd;
            } else {
                index++;
            }
        }
        return result;
    }

    private int skipQuoted(String sql, int index, char quote) {
        int cursor = index + 1;
        while (cursor < sql.length()) {
            if (sql.charAt(cursor) == quote) {
                if (cursor + 1 < sql.length() && sql.charAt(cursor + 1) == quote) {
                    cursor += 2;
                } else {
                    return cursor + 1;
                }
            } else if (quote == '\'' && sql.charAt(cursor) == '\\' && cursor + 1 < sql.length()) {
                cursor += 2;
            } else {
                cursor++;
            }
        }
        return cursor;
    }

    private int skipLineComment(String sql, int index) {
        while (index < sql.length() && sql.charAt(index) != '\n' && sql.charAt(index) != '\r') {
            index++;
        }
        return index;
    }

    private int skipBlockComment(String sql, int index) {
        while (index + 1 < sql.length()) {
            if (sql.charAt(index) == '*' && sql.charAt(index + 1) == '/') {
                return index + 2;
            }
            index++;
        }
        return sql.length();
    }

    private boolean hasCodeAfter(String sql, int index) {
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                index = skipLineComment(sql, index + 2);
            } else if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                index = skipBlockComment(sql, index + 2);
            } else {
                return true;
            }
        }
        return false;
    }

    private boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    private static class ScanResult {
        private String firstKeyword;
        private String dangerousKeyword;
        private boolean hasMultipleStatements;
        private boolean hasTopLevelLimit;
        private int trailingSemicolonIndex = -1;
    }
}
