package com.xiongx9527.fw.plus.ai.support.rag;

/**
 * RAG 编排契约：问题 → 检索 → 聚合 → 注入，返回结构化 {@link AugmentationResult}。
 *
 * <p>与 base 的 {@code com.xiongx9527.fw.plus.ai.base.retrieval.RetrievalAugmentor} <b>不是同一个抽象</b>：
 * 后者是「已有消息 + 查询 → 增强后的消息列表」的轻量挂钩（供 {@code AiServices} 直接挂检索，
 * 不依赖 support）；本接口是 support 侧完整编排管线的入口。</p>
 *
 * <p>本接口是消除两组同名 {@code RetrievalAugmentor} 撞名后的推荐名字；
 * 旧的 {@link RetrievalAugmentor} 作为兼容别名继续保留。</p>
 */
public interface RagAugmentor {

    AugmentationResult augment(AugmentationRequest request);
}
