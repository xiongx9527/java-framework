package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.metadata;

public class AskDataColumn {

    private final String name;
    private final String type;

    public AskDataColumn(String name, String type) {
        this.name = name;
        this.type = type;
    }

    public String getName() { return name; }
    public String getType() { return type; }
}
