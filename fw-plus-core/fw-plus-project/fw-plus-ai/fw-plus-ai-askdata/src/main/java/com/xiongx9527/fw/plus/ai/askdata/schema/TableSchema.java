package com.xiongx9527.fw.plus.ai.askdata.schema;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** 表及其字段、主键、关系。 */
@Getter
@Builder
public class TableSchema {
    private final String name;
    private final String comment;
    @lombok.Builder.Default
    private final List<ColumnSchema> columns = java.util.Collections.emptyList();
    @lombok.Builder.Default
    private final List<String> primaryKey = java.util.Collections.emptyList();
    @lombok.Builder.Default
    private final List<RelationSchema> relations = java.util.Collections.emptyList();

    /** 产生紧凑的表结构描述，供 prompt 使用。 */
    public String renderBrief() {
        StringBuilder sb = new StringBuilder();
        sb.append("table ").append(name);
        if (comment != null && !comment.isEmpty()) {
            sb.append(" // ").append(comment);
        }
        sb.append("\n  columns:\n");
        for (ColumnSchema c : columns) {
            sb.append("    - ").append(c.getName()).append(" : ").append(c.getType());
            if (c.isPrimaryKey()) {
                sb.append(" (PK)");
            }
            if (!c.isNullable()) {
                sb.append(" NOT NULL");
            }
            if (c.getComment() != null && !c.getComment().isEmpty()) {
                sb.append(" // ").append(c.getComment());
            }
            sb.append("\n");
        }
        if (!relations.isEmpty()) {
            sb.append("  relations:\n");
            for (RelationSchema r : relations) {
                sb.append("    - ").append(r.getFromTable()).append(".").append(r.getFromColumn())
                  .append(" -> ").append(r.getToTable()).append(".").append(r.getToColumn()).append("\n");
            }
        }
        return sb.toString();
    }
}
