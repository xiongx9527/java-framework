/**
 * 文档 / 片段 / 图像 / 音频 / 向量 等数据模型。
 *
 * <p>出处：A 类，逐包镜像 langchain4j 1.23.0 的 {@code dev.langchain4j.data.*}：
 * <ul>
 *   <li>{@code document}：Document / DefaultDocument / Metadata / 装载解析切分转换链路</li>
 *   <li>{@code segment}：TextSegment / TextSegmentTransformer</li>
 *   <li>{@code image} / {@code audio} / {@code video} / {@code pdf}：多模态载体</li>
 *   <li>{@code embedding}：Embedding</li>
 * </ul>
 * 向量召回匹配项在 {@code store.embedding.EmbeddingMatch}（与官方位置一致）。
 * Spring AI 等价 {@code org.springframework.ai.document.Document} / {@code Embedding}。
 *
 * @author xiongxiao
 */
package com.xiongx9527.fw.plus.ai.data;
