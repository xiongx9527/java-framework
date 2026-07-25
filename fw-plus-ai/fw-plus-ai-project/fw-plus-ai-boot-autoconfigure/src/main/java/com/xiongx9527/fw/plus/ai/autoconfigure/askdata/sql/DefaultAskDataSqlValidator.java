package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.sql;

import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.AskDataErrorCode;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataMetadata;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata.AskDataTable;
import com.xiongx9527.fw.plus.ai.autoconfigure.askdata.prompt.AskDataDialect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DefaultAskDataSqlValidator implements AskDataSqlValidator {

    private static final Set<String> DANGEROUS_KEYWORDS = new HashSet<String>(Arrays.asList(
            "insert", "update", "delete", "drop", "alter", "truncate", "create", "merge",
            "grant", "revoke", "call", "exec", "execute", "set", "use", "begin", "commit", "rollback"));
    private static final Set<String> SIDE_EFFECT_FUNCTIONS = new HashSet<String>(Arrays.asList(
            "setval", "nextval", "pg_advisory_lock", "pg_try_advisory_lock", "get_lock", "release_lock",
            "pg_notify", "lo_unlink", "load_file", "sleep", "benchmark"));
    private static final Set<String> FROM_TERMINATORS = new HashSet<String>(Arrays.asList(
            "where", "group", "order", "having", "limit", "offset", "fetch", "union", "intersect",
            "except", "returning", "for", "lock", "window", "qualify"));

    private final boolean enforceLimit;

    public DefaultAskDataSqlValidator() {
        this(true);
    }

    public DefaultAskDataSqlValidator(boolean enforceLimit) {
        this.enforceLimit = enforceLimit;
    }

    @Override
    public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows) {
        return validate(sql, dialect, maxRows, null);
    }

    @Override
    public AskDataSqlValidationResult validate(String sql, AskDataDialect dialect, int maxRows,
            AskDataMetadata metadata) {
        if (sql == null || sql.trim().length() == 0) {
            return rejected("SQL 不能为空");
        }
        if (dialect == null) {
            return rejected("SQL 方言不能为空");
        }
        if (maxRows <= 0) {
            return rejected("最大返回行数必须大于 0");
        }

        String trimmedSql = sql.trim();
        ScanResult scanResult = scan(trimmedSql);
        if (scanResult.hasMultipleStatements) {
            return rejected("仅允许执行单条 SQL 查询");
        }
        if (scanResult.hasExecutableComment) {
            return rejected("不允许 MySQL 可执行注释");
        }
        if (scanResult.firstKeyword == null || (!"select".equals(scanResult.firstKeyword)
                && !"with".equals(scanResult.firstKeyword))) {
            return rejected("仅允许 SELECT 或 WITH 查询");
        }
        if (scanResult.hasSelectInto) {
            return rejected("不允许 SELECT INTO 写入查询");
        }
        if (scanResult.dangerousKeyword != null) {
            return rejected("检测到危险关键字: " + scanResult.dangerousKeyword);
        }
        if (scanResult.sideEffectFunction != null) {
            return rejected("检测到副作用函数: " + scanResult.sideEffectFunction);
        }
        if (scanResult.hasLockClause) {
            return rejected("不允许锁定查询");
        }

        List<Token> tokens = tokenize(trimmedSql);
        String quotedSideEffectFunction = sideEffectFunction(tokens);
        if (quotedSideEffectFunction != null) {
            return rejected("检测到副作用函数: " + quotedSideEffectFunction);
        }
        AskDataSqlValidationResult scopeResult = validateMetadataScope(tokens, metadata);
        if (scopeResult != null) {
            return scopeResult;
        }

        String normalizedSql = stripTrailingSemicolon(trimmedSql, scanResult.trailingSemicolonIndex);
        LimitClause limit = findTopLevelLimit(tokens);
        if (limit != null && limit.all) {
            return rejected("LIMIT ALL 不满足最大返回行数约束");
        }
        if (enforceLimit) {
            if (limit != null && limit.rowCount > maxRows) {
                normalizedSql = replace(normalizedSql, limit.valueStart, limit.valueEnd, String.valueOf(maxRows));
            } else if (limit == null && !dialect.hasRowLimit(normalizedSql)) {
                normalizedSql = appendLimit(normalizedSql, scanResult, dialect, maxRows);
            }
        }
        return AskDataSqlValidationResult.accepted(normalizedSql);
    }

    private AskDataSqlValidationResult validateMetadataScope(List<Token> tokens, AskDataMetadata metadata) {
        if (metadata == null || metadata.getTables() == null || metadata.getTables().isEmpty()) {
            return null;
        }
        Set<String> allowedSimpleTables = new HashSet<String>();
        Set<String> allowedQualifiedTables = new HashSet<String>();
        for (AskDataTable table : metadata.getTables()) {
            if (table != null && table.getName() != null) {
                String normalized = normalizeIdentifier(table.getName());
                if (normalized.indexOf('.') >= 0) {
                    allowedQualifiedTables.add(normalized);
                } else {
                    allowedSimpleTables.add(normalized);
                }
            }
        }
        Set<String> cteNames = collectCteNames(tokens);
        boolean[] inFrom = new boolean[maxDepth(tokens) + 1];
        boolean[] expectTable = new boolean[inFrom.length];
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.depth >= inFrom.length) {
                return rejected("SQL 嵌套层级超出安全校验范围");
            }
            String word = token.word();
            if ("from".equals(word) || "join".equals(word)) {
                inFrom[token.depth] = true;
                expectTable[token.depth] = true;
                continue;
            }
            if (word != null && FROM_TERMINATORS.contains(word)) {
                inFrom[token.depth] = false;
                expectTable[token.depth] = false;
                continue;
            }
            if (inFrom[token.depth] && ",".equals(token.text)) {
                expectTable[token.depth] = true;
                continue;
            }
            if (!expectTable[token.depth]) {
                continue;
            }
            if ("(".equals(token.text)) {
                expectTable[token.depth] = false;
                continue;
            }
            if (!token.identifier) {
                return rejected("无法安全解析 FROM/JOIN 表达式");
            }
            StringBuilder tableName = new StringBuilder(token.text);
            int cursor = index + 1;
            while (cursor + 1 < tokens.size() && tokens.get(cursor).depth == token.depth
                    && ".".equals(tokens.get(cursor).text) && tokens.get(cursor + 1).identifier) {
                tableName.append('.').append(tokens.get(cursor + 1).text);
                cursor += 2;
            }
            if (cursor < tokens.size() && "(".equals(tokens.get(cursor).text)) {
                return rejected("不允许 FROM/JOIN 中的表函数");
            }
            index = cursor - 1;
            String normalized = normalizeIdentifier(tableName.toString());
            boolean isQualified = normalized.indexOf('.') >= 0;
            boolean authorized = isQualified ? allowedQualifiedTables.contains(normalized)
                    : allowedSimpleTables.contains(normalized) || cteNames.contains(normalized);
            if (!authorized) {
                return rejected("SQL 访问了 metadata 未授权表: " + tableName);
            }
            expectTable[token.depth] = false;
        }
        return null;
    }

    private Set<String> collectCteNames(List<Token> tokens) {
        Set<String> names = new HashSet<String>();
        if (tokens.isEmpty() || !"with".equals(tokens.get(0).word())) {
            return names;
        }
        int index = 1;
        if (index < tokens.size() && "recursive".equals(tokens.get(index).word())) {
            index++;
        }
        while (index < tokens.size()) {
            Token alias = tokens.get(index);
            if (alias.depth != 0 || !alias.identifier) {
                break;
            }
            names.add(normalizeIdentifier(alias.text));
            while (index < tokens.size() && !(tokens.get(index).depth == 0
                    && "as".equals(tokens.get(index).word()))) {
                index++;
            }
            if (index >= tokens.size()) {
                break;
            }
            index++;
            if (index >= tokens.size() || !"(".equals(tokens.get(index).text)) {
                break;
            }
            index++;
            while (index < tokens.size() && !(tokens.get(index).depth == 0
                    && ")".equals(tokens.get(index).text))) {
                index++;
            }
            index++;
            if (index >= tokens.size() || tokens.get(index).depth != 0 || !",".equals(tokens.get(index).text)) {
                break;
            }
            index++;
        }
        return names;
    }

    private LimitClause findTopLevelLimit(List<Token> tokens) {
        LimitClause found = null;
        for (int index = 0; index < tokens.size(); index++) {
            Token token = tokens.get(index);
            if (token.depth != 0 || !"limit".equals(token.word())) {
                continue;
            }
            if (found != null || index + 1 >= tokens.size()) {
                return new LimitClause(true, 0L, token.end, token.end);
            }
            Token value = tokens.get(index + 1);
            if ("all".equals(value.word())) {
                found = new LimitClause(true, 0L, value.start, value.end);
                continue;
            }
            if (!isDigits(value.text)) {
                return new LimitClause(true, 0L, value.start, value.end);
            }
            Token rowCount = value;
            if (index + 3 < tokens.size() && ",".equals(tokens.get(index + 2).text)
                    && isDigits(tokens.get(index + 3).text)) {
                rowCount = tokens.get(index + 3);
            }
            try {
                found = new LimitClause(false, Long.parseLong(rowCount.text), rowCount.start, rowCount.end);
            } catch (NumberFormatException ex) {
                return new LimitClause(true, 0L, rowCount.start, rowCount.end);
            }
        }
        return found;
    }

    private String appendLimit(String sql, ScanResult scanResult, AskDataDialect dialect, int maxRows) {
        if (scanResult.trailingSemicolonIndex < 0 && scanResult.trailingLineCommentIndex >= 0) {
            String trailingComment = sql.substring(scanResult.trailingLineCommentIndex).trim();
            String statement = sql.substring(0, scanResult.trailingLineCommentIndex).trim();
            return dialect.applyLimit(statement, maxRows) + " " + trailingComment;
        }
        return dialect.applyLimit(sql, maxRows);
    }

    private AskDataSqlValidationResult rejected(String message) {
        return AskDataSqlValidationResult.rejected(AskDataErrorCode.SQL_REJECTED, message);
    }

    private String replace(String value, int start, int end, String replacement) {
        return value.substring(0, start) + replacement + value.substring(end);
    }

    private String stripTrailingSemicolon(String sql, int trailingSemicolonIndex) {
        if (trailingSemicolonIndex >= 0) {
            return sql.substring(0, trailingSemicolonIndex).trim();
        }
        return sql;
    }

    private ScanResult scan(String sql) {
        ScanResult result = new ScanResult();
        List<String> words = new ArrayList<String>();
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (current == '\'') {
                index = skipQuoted(sql, index, '\'');
            } else if (current == '"' || current == '`') {
                index = skipQuoted(sql, index, current);
            } else if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                int commentStart = index;
                index = skipLineComment(sql, index + 2);
                if (index == sql.length()) {
                    result.trailingLineCommentIndex = commentStart;
                }
            } else if (current == '/' && index + 2 < sql.length() && sql.charAt(index + 1) == '*'
                    && sql.charAt(index + 2) == '!') {
                result.hasExecutableComment = true;
                return result;
            } else if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                index = skipBlockComment(sql, index + 2);
            } else if (current == ';') {
                if (hasCodeAfter(sql, index + 1)) {
                    result.hasMultipleStatements = true;
                    return result;
                }
                result.trailingSemicolonIndex = index++;
            } else if (isWordCharacter(current)) {
                int wordEnd = index + 1;
                while (wordEnd < sql.length() && isWordCharacter(sql.charAt(wordEnd))) {
                    wordEnd++;
                }
                String word = sql.substring(index, wordEnd).toLowerCase(Locale.ENGLISH);
                if (result.firstKeyword == null) {
                    result.firstKeyword = word;
                }
                words.add(word);
                if (DANGEROUS_KEYWORDS.contains(word)) {
                    result.dangerousKeyword = word;
                    return result;
                }
                if ("into".equals(word)) {
                    result.hasSelectInto = true;
                    return result;
                }
                if (SIDE_EFFECT_FUNCTIONS.contains(word) && nextCodeCharacter(sql, wordEnd) == '(') {
                    result.sideEffectFunction = word;
                    return result;
                }
                index = wordEnd;
            } else {
                index++;
            }
        }
        result.hasLockClause = containsSequence(words, "for", "update")
                || containsSequence(words, "for", "share")
                || containsSequence(words, "for", "no", "key", "update")
                || containsSequence(words, "for", "key", "share")
                || containsSequence(words, "lock", "in", "share", "mode");
        return result;
    }

    private List<Token> tokenize(String sql) {
        List<Token> tokens = new ArrayList<Token>();
        int depth = 0;
        int index = 0;
        while (index < sql.length()) {
            char current = sql.charAt(index);
            if (Character.isWhitespace(current)) {
                index++;
            } else if (current == '\'') {
                index = skipQuoted(sql, index, '\'');
            } else if (current == '"' || current == '`') {
                int end = skipQuoted(sql, index, current);
                tokens.add(new Token(sql.substring(index + 1, Math.max(index + 1, end - 1)), depth, index, end, true));
                index = end;
            } else if (current == '-' && index + 1 < sql.length() && sql.charAt(index + 1) == '-') {
                index = skipLineComment(sql, index + 2);
            } else if (current == '/' && index + 1 < sql.length() && sql.charAt(index + 1) == '*') {
                index = skipBlockComment(sql, index + 2);
            } else if (current == '(') {
                tokens.add(new Token("(", depth, index, index + 1, false));
                depth++;
                index++;
            } else if (current == ')') {
                depth = Math.max(0, depth - 1);
                tokens.add(new Token(")", depth, index, index + 1, false));
                index++;
            } else if (current == ',' || current == '.') {
                tokens.add(new Token(String.valueOf(current), depth, index, index + 1, false));
                index++;
            } else if (isWordCharacter(current)) {
                int end = index + 1;
                while (end < sql.length() && isWordCharacter(sql.charAt(end))) {
                    end++;
                }
                tokens.add(new Token(sql.substring(index, end), depth, index, end, true));
                index = end;
            } else {
                index++;
            }
        }
        return tokens;
    }

    private int maxDepth(List<Token> tokens) {
        int max = 0;
        for (Token token : tokens) {
            max = Math.max(max, token.depth);
        }
        return max;
    }

    private String sideEffectFunction(List<Token> tokens) {
        for (int index = 0; index + 1 < tokens.size(); index++) {
            Token token = tokens.get(index);
            String normalized = normalizeIdentifier(token.text);
            if (token.identifier && SIDE_EFFECT_FUNCTIONS.contains(normalized)
                    && "(".equals(tokens.get(index + 1).text)) {
                return normalized;
            }
        }
        return null;
    }

    private boolean containsSequence(List<String> words, String... sequence) {
        for (int index = 0; index + sequence.length <= words.size(); index++) {
            boolean matches = true;
            for (int offset = 0; offset < sequence.length; offset++) {
                if (!sequence[offset].equals(words.get(index + offset))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return true;
            }
        }
        return false;
    }

    private char nextCodeCharacter(String sql, int index) {
        while (index < sql.length() && Character.isWhitespace(sql.charAt(index))) {
            index++;
        }
        return index < sql.length() ? sql.charAt(index) : '\0';
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
        return Character.isLetterOrDigit(value) || value == '_' || value == '$';
    }

    private boolean isDigits(String value) {
        if (value == null || value.length() == 0) {
            return false;
        }
        for (int index = 0; index < value.length(); index++) {
            if (!Character.isDigit(value.charAt(index))) {
                return false;
            }
        }
        return true;
    }

    private String normalizeIdentifier(String value) {
        return value == null ? null : value.replace("`", "").replace("\"", "").toLowerCase(Locale.ENGLISH);
    }

    private static class ScanResult {
        private String firstKeyword;
        private String dangerousKeyword;
        private String sideEffectFunction;
        private boolean hasMultipleStatements;
        private boolean hasExecutableComment;
        private boolean hasSelectInto;
        private boolean hasLockClause;
        private int trailingSemicolonIndex = -1;
        private int trailingLineCommentIndex = -1;
    }

    private static class Token {
        private final String text;
        private final int depth;
        private final int start;
        private final int end;
        private final boolean identifier;

        private Token(String text, int depth, int start, int end, boolean identifier) {
            this.text = text;
            this.depth = depth;
            this.start = start;
            this.end = end;
            this.identifier = identifier;
        }

        private String word() {
            return identifier ? text.toLowerCase(Locale.ENGLISH) : null;
        }
    }

    private static class LimitClause {
        private final boolean all;
        private final long rowCount;
        private final int valueStart;
        private final int valueEnd;

        private LimitClause(boolean all, long rowCount, int valueStart, int valueEnd) {
            this.all = all;
            this.rowCount = rowCount;
            this.valueStart = valueStart;
            this.valueEnd = valueEnd;
        }
    }
}
