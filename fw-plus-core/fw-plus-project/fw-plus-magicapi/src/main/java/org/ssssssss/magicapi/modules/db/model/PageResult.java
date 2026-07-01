package org.ssssssss.magicapi.modules.db.model;

import java.util.List;

public class PageResult<T> extends com.xiongx9527.fw.plus.common.model.PageResult {

    public PageResult(long total, List<T> list) {
        this();
        setTotal(total);
        setList(list);
    }

    public PageResult() {
        super("total", "list");
    }
}