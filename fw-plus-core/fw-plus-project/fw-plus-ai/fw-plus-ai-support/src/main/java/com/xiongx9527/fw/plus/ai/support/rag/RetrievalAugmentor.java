package com.xiongx9527.fw.plus.ai.support.rag;

/**
 * 检索增强编排：问题 → 检索 → 聚合 → 注入。
 */
public interface RetrievalAugmentor {

    AugmentationResult augment(AugmentationRequest request);
}
