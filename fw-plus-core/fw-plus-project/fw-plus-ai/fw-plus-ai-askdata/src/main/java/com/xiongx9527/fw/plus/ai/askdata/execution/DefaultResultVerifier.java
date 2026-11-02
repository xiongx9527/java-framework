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
        Integer keyIdx = settings.getDuplicateKeyColumn();
        if (keyIdx != null) {
            int idx = keyIdx;
            if (idx < 0 || idx >= result.columnCount()) {
                issues.add("duplicateKeyColumn out of range: " + idx);
            } else {
                Set<Object> seen = new HashSet<>();
                boolean dup = false;
                for (List<Object> row : result.getRows()) {
                    Object v = idx < row.size() ? row.get(idx) : null;
                    if (v != null && !seen.add(v)) {
                        dup = true;
                        break;
                    }
                }
                if (dup) {
                    issues.add("duplicate key detected in column index " + idx);
                }
            }
        }
        return issues;
    }
}

