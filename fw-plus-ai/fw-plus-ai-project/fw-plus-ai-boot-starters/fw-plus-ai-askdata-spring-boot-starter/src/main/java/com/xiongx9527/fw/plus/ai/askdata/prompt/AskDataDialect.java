package com.xiongx9527.fw.plus.ai.askdata.prompt;

public class AskDataDialect {

    private final String name;
    private final String identifierRule;
    private final String limitRule;
    private final LimitAppender limitAppender;

    public AskDataDialect(String name, String identifierRule, String limitRule) {
        this(name, identifierRule, limitRule, new LimitAppender() {
            @Override
            public String apply(String sql, int maxRows) {
                return sql + " limit " + maxRows;
            }
        });
    }

    public AskDataDialect(String name, String identifierRule, String limitRule, LimitAppender limitAppender) {
        this.name = name;
        this.identifierRule = identifierRule;
        this.limitRule = limitRule;
        this.limitAppender = limitAppender;
    }

    public String getName() { return name; }
    public String getIdentifierRule() { return identifierRule; }
    public String getLimitRule() { return limitRule; }

    public String applyLimit(String sql, int maxRows) {
        return limitAppender.apply(sql, maxRows);
    }

    public boolean hasRowLimit(String sql) {
        if (sql == null || sql.length() == 0) {
            return false;
        }

        int depth = 0;
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (current == '\'') {
                index = skipQuoted(sql, index, '\'');
            } else if (current == '"' || current == '`') {
                index = skipQuoted(sql, index, current);
            } else if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                index = skipLineComment(sql, index + 2);
            } else if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                index = skipBlockComment(sql, index + 2);
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
                if (depth == 0 && "limit".equalsIgnoreCase(sql.substring(index, wordEnd))) {
                    return true;
                }
                index = wordEnd;
            } else {
                index++;
            }
        }
        return false;
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

    private boolean isWordCharacter(char value) {
        return Character.isLetterOrDigit(value) || value == '_';
    }

    public interface LimitAppender {
        String apply(String sql, int maxRows);
    }
}
