package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AskDataRecommendationRankingTest {

	@Test
	void 候选过滤后必须按分值稳定排序并限制数量() {
		AskDataRecommendationRequest request = new AskDataRecommendationRequest(
				new AskDataProjectContext("project", "operator"), false, 2);
		List<AskDataRecommendationCandidate> candidates = AskDataRecommendationRanker.rank(request, Arrays.asList(
				new AskDataRecommendationCandidate("generic", 10D, true),
				new AskDataRecommendationCandidate("second", 2D, false),
				new AskDataRecommendationCandidate("first", 3D, false)));

		assertEquals(Arrays.asList("first", "second"), Arrays.asList(candidates.get(0).getContent(), candidates.get(1).getContent()));
	}
}
