package com.xiongx9527.fw.plus.ai.support.retrieval;

/** 融合方式：WEIGHTED（加权，默认，分数 0~1 无需归一化）/ RRF（排名倒数融合）。 */
public enum FusionMode {
    WEIGHTED,
    RRF
}
