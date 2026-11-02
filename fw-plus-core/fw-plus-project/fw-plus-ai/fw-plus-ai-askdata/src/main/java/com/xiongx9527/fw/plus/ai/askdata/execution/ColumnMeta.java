package com.xiongx9527.fw.plus.ai.askdata.execution;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 结果集的单个列元数据。 */
@Getter
@AllArgsConstructor
public class ColumnMeta {
    private final String name;
    private final String type;
    private final boolean nullable;
}
