package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

final class Neo4jSqlToCypherTranslator {
    private Neo4jSqlToCypherTranslator() {
    }

    static String translate(String sql) throws SQLException {
        String trimmed = stripTrailingSemicolon(sql == null ? "" : sql.trim());
        String upper = trimmed.toUpperCase(Locale.ENGLISH);
        if (upper.startsWith("MATCH ") || upper.startsWith("RETURN ") || upper.startsWith("CREATE ")
                || upper.startsWith("MERGE ") || upper.startsWith("UNWIND ") || upper.startsWith("CALL ")) {
            return trimmed;
        }
        if (upper.startsWith("SELECT ")) {
            return select(trimmed);
        }
        if (upper.startsWith("INSERT ")) {
            return insert(trimmed);
        }
        if (upper.startsWith("UPDATE ")) {
            return update(trimmed);
        }
        if (upper.startsWith("DELETE ")) {
            return delete(trimmed);
        }
        throw unsupported(trimmed);
    }

    private static String select(String sql) throws SQLException {
        int fromIndex = keyword(sql, "FROM", 0);
        if (fromIndex < 0) {
            throw unsupported(sql);
        }
        String projection = sql.substring("SELECT".length(), fromIndex).trim();
        String rest = sql.substring(fromIndex + "FROM".length()).trim();
        Parts parts = parts(rest);
        if (parts.table.indexOf(' ') >= 0 || parts.table.indexOf(',') >= 0) {
            throw unsupported(sql);
        }
        StringBuilder cypher = new StringBuilder("MATCH (n:").append(parts.table).append(')');
        if (parts.where != null) {
            cypher.append(" WHERE ").append(qualifyExpression(parts.where));
        }
        cypher.append(" RETURN ").append(projection(projection));
        if (parts.orderBy != null) {
            cypher.append(" ORDER BY ").append(qualifyExpression(parts.orderBy));
        }
        if (parts.limit != null) {
            cypher.append(" LIMIT ").append(parts.limit);
        }
        return cypher.toString();
    }

    private static String insert(String sql) throws SQLException {
        String upper = sql.toUpperCase(Locale.ENGLISH);
        if (!upper.startsWith("INSERT INTO ")) {
            throw unsupported(sql);
        }
        int valuesIndex = keyword(sql, "VALUES", 0);
        if (valuesIndex < 0) {
            throw unsupported(sql);
        }
        String target = sql.substring("INSERT INTO".length(), valuesIndex).trim();
        int open = target.indexOf('(');
        int close = target.lastIndexOf(')');
        if (open < 0 || close < open) {
            throw unsupported(sql);
        }
        String table = target.substring(0, open).trim();
        List<String> columns = splitComma(target.substring(open + 1, close));
        String valuesPart = sql.substring(valuesIndex + "VALUES".length()).trim();
        if (!valuesPart.startsWith("(") || !valuesPart.endsWith(")")) {
            throw unsupported(sql);
        }
        List<String> values = splitComma(valuesPart.substring(1, valuesPart.length() - 1));
        if (columns.size() != values.size()) {
            throw unsupported(sql);
        }
        StringBuilder builder = new StringBuilder("CREATE (n:").append(table).append(" {");
        for (int i = 0; i < columns.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(columns.get(i)).append(": ").append(values.get(i));
        }
        builder.append("})");
        return builder.toString();
    }

    private static String update(String sql) throws SQLException {
        int setIndex = keyword(sql, "SET", 0);
        if (setIndex < 0) {
            throw unsupported(sql);
        }
        String table = sql.substring("UPDATE".length(), setIndex).trim();
        String rest = sql.substring(setIndex + "SET".length()).trim();
        int whereIndex = keyword(rest, "WHERE", 0);
        String assignments = whereIndex >= 0 ? rest.substring(0, whereIndex).trim() : rest;
        String where = whereIndex >= 0 ? rest.substring(whereIndex + "WHERE".length()).trim() : null;
        StringBuilder builder = new StringBuilder("MATCH (n:").append(table).append(')');
        if (where != null && where.length() > 0) {
            builder.append(" WHERE ").append(qualifyExpression(where));
        }
        builder.append(" SET ");
        List<String> items = splitComma(assignments);
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            builder.append(qualifyAssignment(items.get(i)));
        }
        return builder.toString();
    }

    private static String delete(String sql) throws SQLException {
        int fromIndex = keyword(sql, "FROM", 0);
        if (fromIndex < 0) {
            throw unsupported(sql);
        }
        String rest = sql.substring(fromIndex + "FROM".length()).trim();
        Parts parts = parts(rest);
        StringBuilder builder = new StringBuilder("MATCH (n:").append(parts.table).append(')');
        if (parts.where != null) {
            builder.append(" WHERE ").append(qualifyExpression(parts.where));
        }
        builder.append(" DELETE n");
        return builder.toString();
    }

    private static Parts parts(String rest) throws SQLException {
        int whereIndex = keyword(rest, "WHERE", 0);
        int orderIndex = keyword(rest, "ORDER BY", 0);
        int limitIndex = keyword(rest, "LIMIT", 0);
        int endTable = minPositive(whereIndex, orderIndex, limitIndex, rest.length());
        String table = rest.substring(0, endTable).trim();
        if (table.length() == 0) {
            throw unsupported(rest);
        }
        String where = null;
        String orderBy = null;
        String limit = null;
        if (whereIndex >= 0) {
            int end = minPositive(orderIndex, limitIndex, rest.length());
            where = rest.substring(whereIndex + "WHERE".length(), end).trim();
        }
        if (orderIndex >= 0) {
            int end = minPositive(limitIndex, rest.length());
            orderBy = rest.substring(orderIndex + "ORDER BY".length(), end).trim();
        }
        if (limitIndex >= 0) {
            limit = rest.substring(limitIndex + "LIMIT".length()).trim();
        }
        return new Parts(table, where, orderBy, limit);
    }

    private static String projection(String projection) {
        if ("*".equals(projection)) {
            return "n";
        }
        List<String> items = splitComma(projection);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            String item = items.get(i);
            int asIndex = keyword(item, "AS", 0);
            if (asIndex >= 0) {
                String expression = item.substring(0, asIndex).trim();
                String alias = item.substring(asIndex + "AS".length()).trim();
                builder.append(qualifyExpression(expression)).append(" AS ").append(alias);
            }
            else {
                builder.append("n.").append(item).append(" AS ").append(item);
            }
        }
        return builder.toString();
    }

    private static String qualifyAssignment(String assignment) {
        int equals = assignment.indexOf('=');
        if (equals < 0) {
            return assignment;
        }
        String left = assignment.substring(0, equals).trim();
        String right = assignment.substring(equals + 1).trim();
        return "n." + left + " = " + right;
    }

    private static String qualifyExpression(String expression) {
        List<String> tokens = splitByWhitespaceKeepingOperators(expression);
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < tokens.size(); i++) {
            if (i > 0) {
                builder.append(' ');
            }
            String token = tokens.get(i);
            if (isIdentifier(token) && !isKeywordLiteral(token) && token.indexOf('.') < 0) {
                builder.append("n.").append(token);
            }
            else {
                builder.append(token);
            }
        }
        return builder.toString();
    }

    private static List<String> splitByWhitespaceKeepingOperators(String expression) {
        List<String> tokens = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < expression.length(); i++) {
            char c = expression.charAt(i);
            if (c == '\'') {
                current.append(c);
                inString = !inString;
                continue;
            }
            if (!inString && Character.isWhitespace(c)) {
                if (current.length() > 0) {
                    tokens.add(current.toString());
                    current.setLength(0);
                }
            }
            else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            tokens.add(current.toString());
        }
        return tokens;
    }

    private static List<String> splitComma(String value) {
        List<String> values = new ArrayList<String>();
        StringBuilder current = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '\'') {
                inString = !inString;
            }
            if (!inString && c == ',') {
                values.add(current.toString().trim());
                current.setLength(0);
            }
            else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());
        return values;
    }

    private static boolean isIdentifier(String token) {
        return token.matches("[A-Za-z_][A-Za-z0-9_]*");
    }

    private static boolean isKeywordLiteral(String token) {
        String upper = token.toUpperCase(Locale.ENGLISH);
        return "AND".equals(upper) || "OR".equals(upper) || "NOT".equals(upper)
                || "IS".equals(upper) || "NULL".equals(upper) || "TRUE".equals(upper)
                || "FALSE".equals(upper) || "ASC".equals(upper) || "DESC".equals(upper)
                || "IN".equals(upper) || "LIKE".equals(upper);
    }

    private static int keyword(String value, String keyword, int start) {
        String upper = value.toUpperCase(Locale.ENGLISH);
        String target = keyword.toUpperCase(Locale.ENGLISH);
        int index = upper.indexOf(target, start);
        while (index >= 0) {
            boolean before = index == 0 || !isIdentifierPart(upper.charAt(index - 1));
            int afterIndex = index + target.length();
            boolean after = afterIndex >= upper.length() || !isIdentifierPart(upper.charAt(afterIndex));
            if (before && after) {
                return index;
            }
            index = upper.indexOf(target, index + 1);
        }
        return -1;
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static int minPositive(int first, int second, int third, int fallback) {
        return Math.min(minPositive(first, fallback), Math.min(minPositive(second, fallback), minPositive(third, fallback)));
    }

    private static int minPositive(int first, int second, int fallback) {
        return Math.min(minPositive(first, fallback), minPositive(second, fallback));
    }

    private static int minPositive(int value, int fallback) {
        return value >= 0 ? value : fallback;
    }

    private static String stripTrailingSemicolon(String value) {
        return value.endsWith(";") ? value.substring(0, value.length() - 1).trim() : value;
    }

    private static SQLException unsupported(String sql) {
        return new SQLException("Unsupported Neo4j SQL: " + sql);
    }

    private static class Parts {
        private final String table;
        private final String where;
        private final String orderBy;
        private final String limit;

        private Parts(String table, String where, String orderBy, String limit) {
            this.table = table;
            this.where = where;
            this.orderBy = orderBy;
            this.limit = limit;
        }
    }
}
