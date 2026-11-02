package com.xiongx9527.fw.plus.ai.askdata.chart.data;

import com.xiongx9527.fw.plus.ai.askdata.execution.ColumnMeta;
import com.xiongx9527.fw.plus.ai.askdata.execution.QueryResult;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import lombok.Builder;
import lombok.Getter;

/** 图表生成的通用数据集（与 JDBC 结果集解耦）。可由 {@link QueryResult} 转换而来。 */
@Getter
@Builder
public class ChartDataset {
    @Builder.Default
    private final List<ChartColumn> columns = Collections.emptyList();

    @Builder.Default
    private final List<List<Object>> rows = Collections.emptyList();

    public int columnCount() {
        return columns == null ? 0 : columns.size();
    }

    public int rowCount() {
        return rows == null ? 0 : rows.size();
    }

    public boolean isNumericColumn(int i) {
        if (i < 0 || i >= columnCount()) {
            return false;
        }
        String t = columns.get(i).getType() == null ? "" : columns.get(i).getType().toLowerCase(Locale.ROOT);
        if (t.matches(".*(int|decimal|double|float|numeric|real|number|bigint|smallint|tinyint|bit).*")) {
            return true;
        }
        for (List<Object> row : rows) {
            Object v = i < row.size() ? row.get(i) : null;
            if (v != null) {
                return v instanceof Number;
            }
        }
        return false;
    }

    public int cardinality(int i) {
        Set<Object> seen = new HashSet<>();
        for (List<Object> row : rows) {
            Object v = i < row.size() ? row.get(i) : null;
            if (v != null) {
                seen.add(v);
            }
        }
        return seen.size();
    }

    public List<Object> columnValues(int i) {
        List<Object> vals = new ArrayList<>();
        for (List<Object> row : rows) {
            vals.add(i < row.size() ? row.get(i) : null);
        }
        return vals;
    }

    public List<String> numericFieldNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < columnCount(); i++) {
            if (isNumericColumn(i)) {
                names.add(columns.get(i).getName());
            }
        }
        return names;
    }

    public List<String> categoricalFieldNames() {
        List<String> names = new ArrayList<>();
        for (int i = 0; i < columnCount(); i++) {
            if (!isNumericColumn(i)) {
                names.add(columns.get(i).getName());
            }
        }
        return names;
    }

    public static ChartDataset from(QueryResult qr) {
        if (qr == null) {
            return ChartDataset.builder().build();
        }
        List<ChartColumn> cols = new ArrayList<>();
        if (qr.getColumns() != null) {
            for (ColumnMeta cm : qr.getColumns()) {
                cols.add(new ChartColumn(cm.getName(), cm.getType()));
            }
        }
        List<List<Object>> rows = new ArrayList<>();
        if (qr.getRows() != null) {
            for (List<Object> r : qr.getRows()) {
                rows.add(new ArrayList<>(r));
            }
        }
        return ChartDataset.builder().columns(cols).rows(rows).build();
    }
}
