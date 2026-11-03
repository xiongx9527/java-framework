package com.xiongx9527.fw.plus.ai.askdata.execution;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 默认 {@link ResultVerifier}：空结果 / 截断 / 主键重复三项自检。 */
public class DefaultResultVerifier implements ResultVerifier {

    @Override
    public List<String> verify(QueryResult result, ExecutionSettings settings) {
        List<String> issues = new ArrayList<>();
        if (result == null || result.rowCount() == 0) {
            issues.add("empty result: no rows returned");
            return issues;
        }
        if (result.isTruncated()) {
            issues.add("result truncated: exceeds maxRows, possibly incomplete");
        }
        Integer keyIdx = resolveKeyIndex(result, settings);
        if (keyIdx != null) {
            if (keyIdx < 0 || keyIdx >= result.columnCount()) {
                issues.add("duplicateKeyColumn out of range: " + keyIdx);
            } else {
                Set<Object> seen = new HashSet<>();
                boolean dup = false;
                for (List<Object> row : result.getRows()) {
                    Object v = keyIdx < row.size() ? row.get(keyIdx) : null;
                    if (v != null && !seen.add(v)) {
                        dup = true;
                        break;
                    }
                }
                if (dup) {
                    issues.add("duplicate key detected in column index " + keyIdx);
                }
            }
        }
        return issues;
    }

    /** 解析重复键检查列：优先按名称（primaryKeyColumn）解析，否则用列下标（duplicateKeyColumn）。 */
    private Integer resolveKeyIndex(QueryResult result, ExecutionSettings settings) {
        String pkName = settings.getPrimaryKeyColumn();
        if (pkName != null && !pkName.trim().isEmpty()) {
            int idx = indexOfColumn(result, pkName.trim());
            if (idx < 0) {
                return null;
            }
            return idx;
        }
        return settings.getDuplicateKeyColumn();
    }

    private int indexOfColumn(QueryResult result, String name) {
        if (result.getColumns() == null) {
            return -1;
        }
        String target = name.toLowerCase(java.util.Locale.ROOT);
        for (int i = 0; i < result.getColumns().size(); i++) {
            String cn = result.getColumns().get(i).getName();
            if (cn != null && cn.toLowerCase(java.util.Locale.ROOT).equals(target)) {
                return i;
            }
        }
        return -1;
    }
}
