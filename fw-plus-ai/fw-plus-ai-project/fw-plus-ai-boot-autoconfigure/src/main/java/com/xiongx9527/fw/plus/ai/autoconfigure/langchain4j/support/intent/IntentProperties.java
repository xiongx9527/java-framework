package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 意图识别配置项。
 *
 * <pre>{@code
 * fw:
 *   plus:
 *     ai:
 *       langchain4j:
 *         intent:
 *           embedding-threshold: 0.72
 *           embedding-margin: 0.05
 *           llm-confidence-threshold: 0.6
 *           cascade-min-confidence: 0.0
 * }</pre>
 */
@ConfigurationProperties(IntentProperties.PREFIX)
public class IntentProperties {

	public static final String PREFIX = "fw.plus.ai.langchain4j.intent";

	/** 向量判定阈值（最优意图余弦相似度下限）。 */
	private double embeddingThreshold = IntentClassifiers.DEFAULT_EMBEDDING_THRESHOLD;

	/** 向量判定间距（最优与次优相似度之差下限）。 */
	private double embeddingMargin = IntentClassifiers.DEFAULT_EMBEDDING_MARGIN;

	/** 大模型判定置信度阈值。 */
	private double llmConfidenceThreshold = IntentClassifiers.DEFAULT_LLM_CONFIDENCE_THRESHOLD;

	/** 级联最低置信度。 */
	private double cascadeMinConfidence = IntentClassifiers.DEFAULT_CASCADE_MIN_CONFIDENCE;

	public double getEmbeddingThreshold() {
		return embeddingThreshold;
	}

	public void setEmbeddingThreshold(double embeddingThreshold) {
		this.embeddingThreshold = embeddingThreshold;
	}

	public double getEmbeddingMargin() {
		return embeddingMargin;
	}

	public void setEmbeddingMargin(double embeddingMargin) {
		this.embeddingMargin = embeddingMargin;
	}

	public double getLlmConfidenceThreshold() {
		return llmConfidenceThreshold;
	}

	public void setLlmConfidenceThreshold(double llmConfidenceThreshold) {
		this.llmConfidenceThreshold = llmConfidenceThreshold;
	}

	public double getCascadeMinConfidence() {
		return cascadeMinConfidence;
	}

	public void setCascadeMinConfidence(double cascadeMinConfidence) {
		this.cascadeMinConfidence = cascadeMinConfidence;
	}
}
