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
        List<String> tokens = tokenize(ngql);
        for (String token : tokens) {
            if (UPDATE_KEYWORDS.contains(token)) {
                throw new SQLException("Query nGql must not contain update keyword: " + token);
            }
        }
        for (String token : tokens) {
            if (QUERY_KEYWORDS.contains(token)) {
                return;
            }
        }
        throw new SQLException("Query nGql must contain a query keyword: " + QUERY_KEYWORDS);
    }

    public static void checkUpdate(String ngql) throws SQLException {
        List<String> tokens = tokenize(ngql);
        for (String token : tokens) {
            if (UPDATE_KEYWORDS.contains(token)) {
                return;
            }
        }
        throw new SQLException("Update nGql must contain an update keyword: " + UPDATE_KEYWORDS);
    }

    private static List<String> tokenize(String ngql) throws SQLException {
        if (ngql == null) {
            throw new SQLException("nGQL must not be null.");
        }
        String normalized = ngql.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            throw new SQLException("nGQL must not be blank.");
        }
        return Arrays.asList(normalized.split(" "));
    }
}
