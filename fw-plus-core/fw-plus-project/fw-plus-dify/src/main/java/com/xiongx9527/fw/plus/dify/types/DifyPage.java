package com.xiongx9527.fw.plus.dify.types;

import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.annotation.JSONType;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.List;

@Getter
@Setter
@JSONType(naming = PropertyNamingStrategy.SnakeCase)
public class DifyPage<T> implements Serializable {

    private int page;
    private int limit;
    private long total;
    private long totalPages;
    private List<T> data;
    private boolean hasMore;
}
