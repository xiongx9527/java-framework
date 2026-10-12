package com.xiongx9527.fw.plus.ai.autoconfigure.langchain4j.support.rag;

import com.xiongx9527.fw.plus.dify.dataset.types.RetrievalModel;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link DifyRagProperties} 的地址归一与 RetrievalModel 默认值——两项均由真实 dify 联调发现：
 * 不带 {@code /v1} 的地址会落到 Web 控制台页；dify 端三个无默认值的必填 pydantic 字段
 * 发 null 会 400。
 */
class DifyRagPropertiesTest {

	@Test
	void toBaseUrlAppendsV1AndStripsTrailingSlash() {
		assertThat(baseUrl("http://192.168.1.100:8010")).isEqualTo("http://192.168.1.100:8010/v1");
		assertThat(baseUrl("http://192.168.1.100:8010/")).isEqualTo("http://192.168.1.100:8010/v1");
		assertThat(baseUrl("http://192.168.1.100:8010/v1")).isEqualTo("http://192.168.1.100:8010/v1");
		assertThat(baseUrl("http://192.168.1.100:8010/v1/")).isEqualTo("http://192.168.1.100:8010/v1");
		assertThat(baseUrl("https://api.dify.ai/v1")).isEqualTo("https://api.dify.ai/v1");
	}

	@Test
	void toRetrievalModelFillsDifyRequiredDefaultsWhenUnset() {
		RetrievalModel model = new DifyRagProperties().toRetrievalModel();

		// dify pydantic 必填：search_method / reranking_enable / score_threshold_enabled
		assertThat(model.getSearchMethod()).isEqualTo(RetrievalModel.SearchMethod.semantic_search);
		assertThat(model.getRerankingEnable()).isFalse();
		assertThat(model.getScoreThresholdEnabled()).isFalse();
		assertThat(model.getTopK()).isEqualTo(3);
	}

	@Test
	void toRetrievalModelKeepsExplicitConfiguration() {
		DifyRagProperties properties = new DifyRagProperties();
		properties.setSearchMethod("full_text_search");
		properties.setRerankingEnable(true);
		properties.setScoreThresholdEnabled(true);
		properties.setScoreThreshold(0.8);

		RetrievalModel model = properties.toRetrievalModel();
		assertThat(model.getSearchMethod()).isEqualTo(RetrievalModel.SearchMethod.full_text_search);
		assertThat(model.getRerankingEnable()).isTrue();
		assertThat(model.getScoreThresholdEnabled()).isTrue();
		assertThat(model.getScoreThreshold()).isEqualTo(0.8);
	}

	@Test
	void toRetrievalModelRejectsIllegalSearchMethod() {
		DifyRagProperties properties = new DifyRagProperties();
		properties.setSearchMethod("vector_search");

		assertThatThrownBy(properties::toRetrievalModel)
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("search-method 取值非法");
	}

	private static String baseUrl(String value) {
		DifyRagProperties properties = new DifyRagProperties();
		properties.setBaseUrl(value);
		return properties.toBaseUrl();
	}
}
