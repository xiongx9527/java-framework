package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.intent;

import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.output.Response;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 向量分类器：最近邻命中、阈值拦截、间距（margin）拦截。
 */
class EmbeddingIntentClassifierTest {

	@Test
	void picksNearestIntent() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.embedding(model(), examples());

		IntentResult<SampleIntent> result = classifier.classify(IntentContext.of("快递"));

		assertThat(result.intent()).isEqualTo(SampleIntent.LOGISTICS);
		assertThat(result.source()).isEqualTo(IntentSource.EMBEDDING);
		assertThat(result.confidence()).isGreaterThan(0.99d);
	}

	@Test
	void rejectsWhenBelowThreshold() {
		IntentClassifier<SampleIntent> classifier = IntentClassifiers.embedding(model(), examples());

		// [0.5,0.5] 对 [1,0] 与 [0,1] 均为 0.707，低于默认阈值 0.72
		IntentResult<SampleIntent> result = classifier.classify(IntentContext.of("模糊"));

		assertThat(result.matched()).isFalse();
		assertThat(result.source()).isEqualTo(IntentSource.EMBEDDING);
	}

	@Test
	void rejectsWhenMarginTooSmall() {
		IntentClassifier<SampleIntent> classifier =
				IntentClassifiers.embedding(model(), examples(), 0.5d, 0.5d);

		// [0.6,0.8]：REFUND 0.8、LOGISTICS 0.6，间距 0.2 < 0.5
		IntentResult<SampleIntent> result = classifier.classify(IntentContext.of("投诉"));

		assertThat(result.matched()).isFalse();
	}

	@Test
	void rejectsBlankTextWithoutCallingModel() {
		IntentClassifier<SampleIntent> classifier =
				IntentClassifiers.embedding(new ThrowingEmbeddingModel(), examples());

		assertThat(classifier.classify(IntentContext.of("   ")).matched()).isFalse();
	}

	private static EmbeddingModel model() {
		return new MapEmbeddingModel(vectors());
	}

	private static Map<String, float[]> vectors() {
		Map<String, float[]> vectors = new HashMap<String, float[]>();
		vectors.put("物流", new float[]{1f, 0f});
		vectors.put("退款", new float[]{0f, 1f});
		vectors.put("你好", new float[]{-1f, 0f});
		vectors.put("快递", new float[]{1f, 0f});
		vectors.put("投诉", new float[]{0.6f, 0.8f});
		vectors.put("模糊", new float[]{0.5f, 0.5f});
		return vectors;
	}

	private static Map<SampleIntent, List<String>> examples() {
		Map<SampleIntent, List<String>> examples = new LinkedHashMap<SampleIntent, List<String>>();
		examples.put(SampleIntent.LOGISTICS, Collections.singletonList("物流"));
		examples.put(SampleIntent.REFUND, Collections.singletonList("退款"));
		examples.put(SampleIntent.CHITCHAT, Collections.singletonList("你好"));
		return examples;
	}

	/** 精确查表的假向量模型：未登记文本返回零向量。 */
	private static final class MapEmbeddingModel implements EmbeddingModel {

		private final Map<String, float[]> vectors;

		MapEmbeddingModel(Map<String, float[]> vectors) {
			this.vectors = vectors;
		}

		@Override
		public Response<Embedding> embed(String text) {
			return Response.from(Embedding.from(vectorOf(text)));
		}

		@Override
		public Response<Embedding> embed(TextSegment textSegment) {
			return Response.from(Embedding.from(vectorOf(textSegment.text())));
		}

		@Override
		public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
			List<Embedding> embeddings = new ArrayList<Embedding>();
			for (TextSegment segment : segments) {
				embeddings.add(Embedding.from(vectorOf(segment.text())));
			}
			return Response.from(embeddings);
		}

		@Override
		public int dimension() {
			return 2;
		}

		private float[] vectorOf(String text) {
			float[] vector = vectors.get(text);
			return vector == null ? new float[]{0f, 0f} : vector;
		}
	}

	/** 示例句（非空白）向量化成功，空白输入一旦被调用即失败，用于断言空文本不会触发向量化。 */
	private static final class ThrowingEmbeddingModel implements EmbeddingModel {

		@Override
		public Response<Embedding> embed(String text) {
			if (text == null || text.trim().isEmpty()) {
				throw new AssertionError("空文本不应触发向量化");
			}
			return Response.from(Embedding.from(new float[]{1f, 0f}));
		}

		@Override
		public Response<List<Embedding>> embedAll(List<TextSegment> segments) {
			List<Embedding> embeddings = new ArrayList<Embedding>();
			for (TextSegment segment : segments) {
				embeddings.add(Embedding.from(new float[]{1f, 0f}));
			}
			return Response.from(embeddings);
		}
	}
}
