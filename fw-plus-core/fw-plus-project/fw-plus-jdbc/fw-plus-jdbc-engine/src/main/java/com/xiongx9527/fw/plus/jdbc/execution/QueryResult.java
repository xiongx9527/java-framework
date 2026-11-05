package com.xiongx9527.fw.plus.jdbc.execution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

/** SQL 执行后的结构化结果集（§6.20 EXECUTE → SHAPE）。与具体 JDBC 驱动解耦。 */
@Getter
@Builder
public class QueryResult {
    @lombok.Builder.Default
    private final List<ColumnMeta> columns = Collections.emptyList();

    @lombok.Builder.Default
    private final List<List<Object>> rows = Collections.emptyList();

    /** 是否因达到 maxRows 而截断（结果可能不完整）。 */
    @lombok.Builder.Default
    private final boolean truncated = false;

    /** 执行耗时（毫秒）。 */
    @lombok.Builder.Default
    private final long elapsedMillis = 0L;

    /** 结果自检（VERIFY）发现的可疑项提示（不影响结果本身，供上游告警 / 反问）。 */
    @lombok.Builder.Default
    private final List<String> warnings = Collections.emptyList();

    public int rowCount() {
        return rows == null ? 0 : rows.size();
    }

    public int columnCount() {
        return columns == null ? 0 : columns.size();
    }

    /** 返回带上附加自检告警的新结果（原对象不变）。 */
    public QueryResult withWarnings(List<String> extra) {
        if (extra == null || extra.isEmpty()) {
            return this;
        }
        List<String> merged = new ArrayList<>(warnings);
        merged.addAll(extra);
        return QueryResult.builder()
                .columns(columns).rows(rows)
                .truncated(truncated).elapsedMillis(elapsedMillis)
                .warnings(merged).build();
    }
}

