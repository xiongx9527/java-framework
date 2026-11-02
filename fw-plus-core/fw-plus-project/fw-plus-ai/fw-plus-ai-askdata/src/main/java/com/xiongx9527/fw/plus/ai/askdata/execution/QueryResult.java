package com.xiongx9527.fw.plus.ai.askdata.execution;

import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** SQL 执行后的结构化结果集（§6.20 EXECUTE → SHAPE）。与具体 JDBC 驱动解耦。 */
@Getter
@Builder
public class QueryResult {
    @Builder.Default
    private final List<ColumnMeta> columns = Collections.emptyList();

    @Builder.Default
    private final List<List<Object>> rows = Collections.emptyList();

    /** 是否因达到 maxRows 而截断（结果可能不完整）。 */
    @Builder.Default
    private final boolean truncated = false;

    /** 执行耗时（毫秒）。 */
    @Builder.Default
    private final long elapsedMillis = 0L;

    public int rowCount() {
        return rows == null ? 0 : rows.size();
    }

    public int columnCount() {
        return columns == null ? 0 : columns.size();
    }
}
