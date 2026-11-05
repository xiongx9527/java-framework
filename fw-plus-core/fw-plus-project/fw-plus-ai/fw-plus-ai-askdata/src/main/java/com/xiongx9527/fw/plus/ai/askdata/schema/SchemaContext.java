package com.xiongx9527.fw.plus.ai.askdata.schema;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/**
 * 可见 schema 上下文（已由上游权限注入 / 召回结果提供，见 §6.38.2）。
 * 本模块只消费它，不再做权限判断。
 */
@Getter
@Builder
public class SchemaContext {
    @lombok.Builder.Default
    private final List<TableSchema> tables = new ArrayList<>();

    public boolean isEmpty() {
        return tables == null || tables.isEmpty();
    }

    public Set<String> allowedTableNames() {
        Set<String> names = new HashSet<>();
        if (tables == null) {
            return names;
        }
        for (TableSchema t : tables) {
            String full = normalize(t.getName());
            names.add(full);
            names.add(lastSegment(full));
        }
        return names;
    }

    public boolean containsTable(String tableName) {
        if (tableName == null) {
            return false;
        }
        String n = normalize(tableName);
        return allowedTableNames().contains(n) || allowedTableNames().contains(lastSegment(n));
    }

    /** 返回某表的主键列名（取 primaryKey 列表首个），找不到或表无主键时返回 null。
     * 供执行期 VERIFY 注入 {@code SqlExecInput.primaryKeyColumn} 以检测主键重复（§6.38.2）。 */
    public String primaryKeyColumn(String tableName) {
        if (tables == null || tableName == null) {
            return null;
        }
        String n = normalize(tableName);
        for (TableSchema t : tables) {
            String full = normalize(t.getName());
            if (full.equals(n) || lastSegment(full).equals(lastSegment(n))) {
                if (t.getPrimaryKey() != null && !t.getPrimaryKey().isEmpty()) {
                    return t.getPrimaryKey().get(0);
                }
            }
        }
        return null;
    }

    public String render() {
        if (isEmpty()) {
            return "(no schema provided)";
        }
        StringBuilder sb = new StringBuilder();
        for (TableSchema t : tables) {
            sb.append(t.renderBrief()).append("\n");
        }
        return sb.toString();
    }

    static String normalize(String s) {
        if (s == null) {
            return "";
        }
        return s.trim().toLowerCase().replace(String.valueOf('"'), "").replace(String.valueOf('`'), "");
    }

    static String lastSegment(String normalized) {
        int idx = normalized.lastIndexOf('.');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }
}
