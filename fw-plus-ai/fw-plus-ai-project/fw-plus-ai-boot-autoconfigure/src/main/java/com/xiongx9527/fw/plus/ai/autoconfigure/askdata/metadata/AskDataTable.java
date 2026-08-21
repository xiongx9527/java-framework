package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AskDataTable {

    private final String name;
    private final List<AskDataColumn> columns;
    private final boolean tableLevelAuthorized;

    public AskDataTable(String name, List<AskDataColumn> columns) {
        this(name, columns, false);
    }

    public AskDataTable(String name, List<AskDataColumn> columns, boolean tableLevelAuthorized) {
        this.name = name;
        this.columns = columns == null ? Collections.<AskDataColumn>emptyList()
                : Collections.unmodifiableList(new ArrayList<AskDataColumn>(columns));
        this.tableLevelAuthorized = tableLevelAuthorized;
    }

    public String getName() { return name; }
    public List<AskDataColumn> getColumns() { return columns; }
    public boolean isTableLevelAuthorized() { return tableLevelAuthorized; }
}
