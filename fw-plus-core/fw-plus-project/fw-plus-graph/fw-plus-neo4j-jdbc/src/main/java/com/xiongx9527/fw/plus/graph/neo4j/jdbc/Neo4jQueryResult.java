package com.xiongx9527.fw.plus.graph.neo4j.jdbc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class Neo4jQueryResult {
    private final List<String> columns;
    private final List<Map<String, Object>> rows;
    private final int updateCount;

    public Neo4jQueryResult(List<String> columns, List<Map<String, Object>> rows, int updateCount) {
        this.columns = columns == null ? Collections.<String>emptyList() : new ArrayList<String>(columns);
        this.rows = new ArrayList<Map<String, Object>>();
        if (rows != null) {
            for (Map<String, Object> row : rows) {
                this.rows.add(new LinkedHashMap<String, Object>(row));
            }
        }
        this.updateCount = updateCount;
    }

    public List<String> getColumns() {
        return new ArrayList<String>(columns);
    }

    public List<Map<String, Object>> getRows() {
        List<Map<String, Object>> copy = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            copy.add(new LinkedHashMap<String, Object>(row));
        }
        return copy;
    }

    public int getUpdateCount() {
        return updateCount;
    }
}
