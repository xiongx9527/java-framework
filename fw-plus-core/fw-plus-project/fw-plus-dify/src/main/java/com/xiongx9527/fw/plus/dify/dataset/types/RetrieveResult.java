package com.xiongx9527.fw.plus.dify.dataset.types;

import com.alibaba.fastjson2.JSONObject;
import com.alibaba.fastjson2.PropertyNamingStrategy;
import com.alibaba.fastjson2.annotation.JSONType;
import com.xiongx9527.fw.plus.dify.dataset.entity.Segment;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@JSONType(naming = PropertyNamingStrategy.SnakeCase)
public class RetrieveResult {
    private JSONObject query;
    private List<Record> records;

    @Getter
    @Setter
    public static class Record {
        private Double score;
        private String tsnePosition;
        private Segment segment;
    }
}
