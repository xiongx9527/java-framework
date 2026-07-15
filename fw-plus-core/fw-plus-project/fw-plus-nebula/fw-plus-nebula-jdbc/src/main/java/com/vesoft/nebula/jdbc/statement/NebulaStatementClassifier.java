package com.vesoft.nebula.jdbc.statement;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class NebulaStatementClassifier {
    private static final Set<String> UPDATE_KEYWORDS = new HashSet<>(Arrays.asList(
            "update", "delete", "insert", "upsert", "create", "drop", "alter", "rebuild"));
    private static final Set<String> QUERY_KEYWORDS = new HashSet<>(Arrays.asList(
            "match", "lookup", "go", "fetch", "find", "subgraph", "show", "desc", "describe"));

    private NebulaStatementClassifier() {
    }

    public static void checkQuery(String ngql) throws SQLException {
        String leadingKeyword = leadingKeyword(ngql);
        if (UPDATE_KEYWORDS.contains(leadingKeyword)) {
            throw new SQLException("Query nGql must not contain update keyword: " + leadingKeyword);
        }
        if (QUERY_KEYWORDS.contains(leadingKeyword)) {
            return;
        }
        throw new SQLException("Query nGql must contain a query keyword: " + QUERY_KEYWORDS);
    }

    public static void checkUpdate(String ngql) throws SQLException {
        String leadingKeyword = leadingKeyword(ngql);
        if (QUERY_KEYWORDS.contains(leadingKeyword)) {
            throw new SQLException("Update nGql must not contain query keyword: " + leadingKeyword);
        }
        if (UPDATE_KEYWORDS.contains(leadingKeyword)) {
            return;
        }
        throw new SQLException("Update nGql must contain an update keyword: " + UPDATE_KEYWORDS);
    }

    private static String leadingKeyword(String ngql) throws SQLException {
        List<String> tokens = tokens(ngql);
        return tokens.get(0);
    }

    private static List<String> tokens(String ngql) throws SQLException {
        if (ngql == null || ngql.trim().isEmpty()) {
            throw new SQLException("nGql must not be null or blank");
        }
        return Arrays.asList(ngql.toLowerCase(Locale.ROOT).trim().split("\\s+|(?=[(),])|(?<=[(),])"));
    }
}
