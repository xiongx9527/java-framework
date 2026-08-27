package com.xiongx9527.fw.plus.dify.dataset.types.external;

import com.google.common.collect.Lists;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class KnowledgeResult {
    /**
     * 从知识库查询的记录列表
     */
    private List<Record> records;

    /**
     * constructor
     */
    public KnowledgeResult() {

    }

    /**
     * constructor
     * @param data Record
     */
    public KnowledgeResult(Record data) {
        this.records = Lists.newArrayList(data);
    }

    /**
     * constructor
     * @param data List<Record>
     */
    public KnowledgeResult(List<Record> data) {
        this.records = data;
    }

    @Getter
    @Setter
    @Builder
    public static class Record {
        /**
         * 包含知识库中数据源的文本块
         */
        private String content;

        /**
         * 结果与查询的相关性分数，范围：0~1
         */
        private Double score;

        /**
         * 文档标题
         */
        private String title;

        /**
         * 包含数据源中文档的元数据属性及其值
         */
        private Map<String, Object> metadata;

        public Map<String, Object> getMetadata() {
            if (metadata == null) {
                metadata = new HashMap<>();
            }
            return metadata;
        }

        public void setMetadata(Map<String, Object> metadata) {
            this.metadata = metadata == null ? new HashMap<>() : metadata;
        }
    }
}
