package com.xiongx9527.fw.plus.ai.askdata.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class DefaultResultVerifierTest {

    private QueryResult withRows(List<List<Object>> rows, boolean truncated) {
        return QueryResult.builder()
                .columns(Collections.singletonList(new ColumnMeta("id", "BIGINT", false)))
                .rows(rows)
                .truncated(truncated)
                .build();
    }

    @Test
    void flagsEmptyResult() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<String> issues = v.verify(QueryResult.builder().build(), ExecutionSettings.builder().build());
        assertFalse(issues.isEmpty());
        assertTrue(issues.get(0).contains("empty"));
    }

    @Test
    void flagsTruncated() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = Collections.singletonList(Collections.singletonList(1L));
        List<String> issues = v.verify(withRows(rows, true), ExecutionSettings.builder().build());
        assertTrue(issues.stream().anyMatch(i -> i.contains("truncated")));
    }

    @Test
    void flagsDuplicateKey() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = List.of(
                List.of(1L), List.of(1L), List.of(2L));
        ExecutionSettings settings = ExecutionSettings.builder().duplicateKeyColumn(0).build();
        List<String> issues = v.verify(withRows(rows, false), settings);
        assertTrue(issues.stream().anyMatch(i -> i.contains("duplicate key")));
    }

    @Test
    void noIssueForCleanResult() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = List.of(List.of(1L), List.of(2L));
        List<String> issues = v.verify(withRows(rows, false), ExecutionSettings.builder().build());
        assertEquals(0, issues.size());
    }
}

