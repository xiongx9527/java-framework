package com.xiongx9527.fw.plus.dify.dataset.types;

/**
 * 文档索引状态状态机取值（对齐 Dify 文档「添加和更新文档」索引状态轮询语义）。
 * <p>状态机：waiting → parsing → cleaning → splitting → indexing → completed，
 * 任一处理环节失败进入 error。服务端以字符串返回，对应
 * {@link BatchStatus#getIndexingStatus()} 字段。</p>
 */
public enum IndexingStatus {
    waiting("waiting"),
    parsing("parsing"),
    cleaning("cleaning"),
    splitting("splitting"),
    indexing("indexing"),
    completed("completed"),
    error("error");

    private final String value;

    IndexingStatus(String value) {
        this.value = value;
    }

    /**
     * 服务端返回的状态字符串
     */
    public String getValue() {
        return value;
    }
}
