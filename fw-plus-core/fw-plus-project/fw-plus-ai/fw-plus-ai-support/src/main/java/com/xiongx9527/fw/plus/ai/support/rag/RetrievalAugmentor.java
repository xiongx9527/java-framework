package com.xiongx9527.fw.plus.ai.support.rag;

/**
 * 检索增强编排：问题 → 检索 → 聚合 → 注入。
 *
 * <p><b>兼容别名</b>：推荐改用 {@link RagAugmentor}。保留本名字是为了不破坏既有调用方；
 * 注意它与 base 包下同名的
 * {@code com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalAugmentor} 签名不同、
 * 职责也不同（base 是消息级轻量挂钩，本接口是结构化编排入口）。</p>
 */
@Deprecated
public interface RetrievalAugmentor extends RagAugmentor {
}
