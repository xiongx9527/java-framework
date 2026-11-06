package com.xiongx9527.fw.plus.jdbc.execution;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;

/**
 * 把 {@link QueryResult} 导出为 CSV（§6.38 结果导出）。仅做最简单 RFC4180 风格转义：
 * 含逗号/引号/换行的值用双引号包裹并将内部双引号转义。
 */
public final class CsvExporter {

    private CsvExporter() {
    }

    public static String export(QueryResult result) {
        if (result == null) {
            return "";
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        List<ColumnMeta> cols = result.getColumns();
        if (cols != null) {
            for (int i = 0; i < cols.size(); i++) {
                if (i > 0) {
                    pw.print(',');
                }
                pw.print(escape(cols.get(i).getName()));
            }
            pw.println();
        }
        for (List<Object> row : result.getRows()) {
            for (int i = 0; i < (cols == null ? row.size() : cols.size()); i++) {
                if (i > 0) {
                    pw.print(',');
                }
                Object v = i < row.size() ? row.get(i) : null;
                pw.print(escape(v == null ? "" : String.valueOf(v)));
            }
            pw.println();
        }
        pw.flush();
        return sw.toString();
    }

    private static String escape(String s) {
        if (s == null) {
            return "";
        }
        boolean need = s.indexOf(',') >= 0 || s.indexOf('"') >= 0 || s.indexOf('\n') >= 0 || s.indexOf('\r') >= 0;
        if (!need) {
            return s;
        }
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
