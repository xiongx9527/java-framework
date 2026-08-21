package com.xiongx9527.fw.plus.ai.askdata.memory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Core 推荐过滤与排序参考实现。
 *
 * <p>执行顺序固定为：项目/操作人/数据源/授权对象过滤、通用候选策略过滤、个人证据总分排序、数量截断。
 * 该类无共享可变状态，可并发调用。它不会访问画像或治理后端，也不会接受未绑定授权的候选；业务算法可以
 * 替换候选生成方式，但不得绕过 {@link AskDataRecommendationRequest#allows(AskDataRecommendationCandidate)}
 * 所表达的 fail-closed 边界。</p>
 */
public final class AskDataRecommendationRanker {

	private AskDataRecommendationRanker() {
	}

	/**
	 * 过滤并稳定排序推荐候选。
	 *
	 * @param request 已绑定当前操作人和授权快照的请求
	 * @param candidates 业务生成的候选；允许含 {@code null}，但不得原地修改
	 * @return 不可修改的已授权候选列表
	 */
	public static List<AskDataRecommendationCandidate> rank(AskDataRecommendationRequest request,
			List<AskDataRecommendationCandidate> candidates) {
		if (request == null || candidates == null) {
			throw new IllegalArgumentException("request 和 candidates 不能为空");
		}
		List<AskDataRecommendationCandidate> result = new ArrayList<AskDataRecommendationCandidate>();
		for (AskDataRecommendationCandidate candidate : candidates) {
			if (request.allows(candidate)) {
				result.add(candidate);
			}
		}
		Collections.sort(result, new Comparator<AskDataRecommendationCandidate>() {
			@Override
			public int compare(AskDataRecommendationCandidate left, AskDataRecommendationCandidate right) {
				return Double.compare(right.getScore(), left.getScore());
			}
		});
		if (result.size() > request.getLimit()) {
			result = new ArrayList<AskDataRecommendationCandidate>(result.subList(0, request.getLimit()));
		}
		return Collections.unmodifiableList(result);
	}
}
