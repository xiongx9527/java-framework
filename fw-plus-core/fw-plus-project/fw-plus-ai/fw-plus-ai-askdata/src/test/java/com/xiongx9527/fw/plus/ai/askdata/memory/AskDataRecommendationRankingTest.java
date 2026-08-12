package com.xiongx9527.fw.plus.ai.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.security.AskDataAuthorizedObject;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AskDataRecommendationRankingTest {

	@Test
	void 候选必须先按项目操作人数据源和权限过滤再按个人证据排序() {
		Set<AskDataAuthorizedObject> authorizedObjects = Collections.singleton(
				new AskDataAuthorizedObject("sales", "orders", "amount"));
		AskDataRecommendationRequest request = new AskDataRecommendationRequest(
				new AskDataProjectContext("project", "operator"), "sales", authorizedObjects,
				"policy-v1", "business-governance", false, 2);
		List<AskDataRecommendationCandidate> candidates = AskDataRecommendationRanker.rank(request, Arrays.asList(
				candidate("generic", null, "sales", authorizedObjects, 10D, 0D, 0D, 0D, true),
				candidate("other-operator", "other", "sales", authorizedObjects, 20D, 0D, 0D, 0D, false),
				candidate("other-datasource", "operator", "finance", Collections.singleton(
						new AskDataAuthorizedObject("finance", "payments", "amount")), 20D, 0D, 0D, 0D, false),
				candidate("unauthorized", "operator", "sales", Collections.singleton(
						new AskDataAuthorizedObject("sales", "orders", "secret")), 20D, 0D, 0D, 0D, false),
				candidate("second", "operator", "sales", authorizedObjects, 1D, 0.5D, 0.25D, 0.25D, false),
				candidate("first", "operator", "sales", authorizedObjects, 1D, 1D, 1D, 1D, false)));

		assertEquals(Arrays.asList("first", "second"), Arrays.asList(candidates.get(0).getContent(), candidates.get(1).getContent()));
		assertEquals("confirmed-query", candidates.get(0).getSource());
		assertEquals("candidate:first", candidates.get(0).getAuditSummary());
	}

	private AskDataRecommendationCandidate candidate(String content, String operatorId, String datasourceKey,
			Set<AskDataAuthorizedObject> requiredObjects, double relevance, double preference, double feedback,
			double memory, boolean generic) {
		return new AskDataRecommendationCandidate("candidate-" + content, content, "project", operatorId,
				datasourceKey, requiredObjects, "confirmed-query", relevance, preference, feedback, memory,
				generic, "candidate:" + content);
	}
}
