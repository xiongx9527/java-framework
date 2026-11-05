package com.xiongx9527.fw.plus.jdbc.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import org.junit.jupiter.api.Test;

public class DefaultResultVerifierTest {

    private QueryResult withRows(List<List<Object>> rows, boolean truncated) {
        return QueryResult.builder()
                .columns(Collections.singletonList(new ColumnMeta("id", "BIGINT", false)))
                .rows(rows)
                .truncated(truncated)
                .build();
    }

    private QueryResult withTwoColumns(List<List<Object>> rows) {
        return QueryResult.builder()
                .columns(Arrays.asList(new ColumnMeta("id", "BIGINT", false),
                        new ColumnMeta("name", "VARCHAR", false)))
                .rows(rows)
                .build();
    }

    @Test
    void flagsEmptyResult() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<String> issues = v.verify(QueryResult.builder().build(), ExecutionSettings.builder().build());
        assertFalse(issues.isEmpty());
        assertEquals(I18nMessageResolver.resolveKey("askdata.verify.empty_result"), issues.get(0));
    }

    @Test
    void flagsTruncated() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = Collections.singletonList(Collections.singletonList(1L));
        List<String> issues = v.verify(withRows(rows, true), ExecutionSettings.builder().build());
        assertTrue(issues.stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.verify.truncated"))));
    }

    @Test
    void flagsDuplicateKey() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1L), Arrays.asList(1L), Arrays.asList(2L));
        ExecutionSettings settings = ExecutionSettings.builder().duplicateKeyColumn(0).build();
        List<String> issues = v.verify(withRows(rows, false), settings);
        assertTrue(issues.stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.verify.dup_key", 0))));
    }

    @Test
    void flagsDuplicateByColumnName() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1L, "a"), Arrays.asList(1L, "b"), Arrays.asList(2L, "c"));
        ExecutionSettings settings = ExecutionSettings.builder().primaryKeyColumn("id").build();
        List<String> issues = v.verify(withTwoColumns(rows), settings);
        assertTrue(issues.stream().anyMatch(i -> i.equals(I18nMessageResolver.resolveKey("askdata.verify.dup_key", 0))));
    }

    @Test
    void unknownPkNameDoesNotFalsePositive() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1L, "a"), Arrays.asList(1L, "b"), Arrays.asList(2L, "c"));
        ExecutionSettings settings = ExecutionSettings.builder().primaryKeyColumn("ghost").build();
        List<String> issues = v.verify(withTwoColumns(rows), settings);
        assertTrue(issues.stream().noneMatch(i -> i.contains("主键")));
    }

    @Test
    void noIssueForCleanResult() {
        DefaultResultVerifier v = new DefaultResultVerifier();
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1L), Arrays.asList(2L));
        List<String> issues = v.verify(withRows(rows, false), ExecutionSettings.builder().build());
        assertEquals(0, issues.size());
    }
}
