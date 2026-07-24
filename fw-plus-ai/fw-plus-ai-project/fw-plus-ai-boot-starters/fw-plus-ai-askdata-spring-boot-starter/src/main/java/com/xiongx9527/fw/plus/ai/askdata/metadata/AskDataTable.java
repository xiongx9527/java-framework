package com.xiongx9527.fw.plus.ai.askdata.metadata;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AskDataTable {

    private final String name;
    private final List<AskDataColumn> columns;

    public AskDataTable(String name, List<AskDataColumn> columns) {
        this.name = name;
        this.columns = columns == null ? Collections.<AskDataColumn>emptyList()
                : Collections.unmodifiableList(new ArrayList<AskDataColumn>(columns));
    }

    public String getName() { return name; }
    public List<AskDataColumn> getColumns() { return columns; }
}
