package com.xiongx9527.fw.plus.jdbc.execution;

import java.util.ArrayList;
import java.util.List;

/**
 * 列级脱敏装饰器（§6.38 列级权限）。委托底层 {@link SqlExecutorPort} 执行，
 * 对返回结果逐格调用 {@link ColumnMasker} 改写/打码，原始执行器本身无脱敏逻辑。
 *
 * <p>产品侧按可见 schema 的敏感列元数据构建 {@link ColumnMasker} 注入即可；
 * 引擎层只负责在结果收口前调用，不内置任何脱敏规则。</p>
 */
public class MaskingSqlExecutorPort implements SqlExecutorPort {

    private final SqlExecutorPort delegate;
    private final ColumnMasker masker;

    public MaskingSqlExecutorPort(SqlExecutorPort delegate, ColumnMasker masker) {
        if (delegate == null) {
            throw new IllegalArgumentException("delegate executor is required");
        }
        if (masker == null) {
            throw new IllegalArgumentException("column masker is required");
        }
        this.delegate = delegate;
        this.masker = masker;
    }

    @Override
    public QueryResult execute(String sql, ExecutionSettings settings) {
        return mask(delegate.execute(sql, settings));
    }

    @Override
    public QueryResult execute(String sql, List<Object> params, ExecutionSettings settings) {
        return mask(delegate.execute(sql, params, settings));
    }

    private QueryResult mask(QueryResult r) {
        if (r == null) {
            return null;
        }
        List<ColumnMeta> cols = r.getColumns();
        List<List<Object>> rows = new ArrayList<List<Object>>(r.rowCount());
        for (List<Object> row : r.getRows()) {
            List<Object> nr = new ArrayList<Object>(row.size());
            for (int i = 0; i < row.size(); i++) {
                String name = (cols != null && i < cols.size()) ? cols.get(i).getName() : null;
                nr.add(masker.mask(name, row.get(i)));
            }
            rows.add(nr);
        }
        return QueryResult.builder()
                .columns(cols)
                .rows(rows)
                .truncated(r.isTruncated())
                .elapsedMillis(r.getElapsedMillis())
                .warnings(r.getWarnings())
                .build();
    }
}
