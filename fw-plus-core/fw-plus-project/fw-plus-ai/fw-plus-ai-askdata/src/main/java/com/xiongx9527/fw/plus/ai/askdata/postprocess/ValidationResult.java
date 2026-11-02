package com.xiongx9527.fw.plus.ai.askdata.postprocess;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** SQL 校验结果（§6.14 L1 / §6.36）。 */
public class ValidationResult {
    private final boolean ok;
    private final List<String> issues;
    private final List<String> referencedTables;

    public ValidationResult(boolean ok, List<String> issues, List<String> referencedTables) {
        this.ok = ok;
        this.issues = issues == null ? Collections.emptyList() : issues;
        this.referencedTables = referencedTables == null ? Collections.emptyList() : referencedTables;
    }

    public static ValidationResult ok(List<String> referencedTables) {
        return new ValidationResult(true, Collections.emptyList(), referencedTables);
    }

    public static ValidationResult fail(List<String> issues, List<String> referencedTables) {
        return new ValidationResult(false, new ArrayList<>(issues), referencedTables);
    }

    public boolean isOk() {
        return ok;
    }

    public List<String> getIssues() {
        return issues;
    }

    public List<String> getReferencedTables() {
        return referencedTables;
    }
}
