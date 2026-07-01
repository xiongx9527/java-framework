package com.xx9527.fw.plus.dify.dataset.types;

import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.annotation.JSONType;
import com.xx9527.fw.plus.dify.dataset.entity.Segment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JSONType(naming = PropertyNamingStrategy.SnakeCase)
public class SegmentResult {
    private DatasetConsts.DocForm docForm;
    private List<Segment> data;
}
