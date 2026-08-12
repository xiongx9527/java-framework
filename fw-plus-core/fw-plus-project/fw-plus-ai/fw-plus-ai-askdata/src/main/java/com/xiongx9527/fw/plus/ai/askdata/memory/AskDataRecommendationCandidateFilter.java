package com.xiongx9527.fw.plus.ai.askdata.memory;

import java.util.List;

/**
 * 推荐候选过滤 SPI。
 *
 * <p>实现收到的是已经绑定项目、已认证操作人、数据源和治理授权的请求。实现可以进一步收窄候选，但不得
 * 扩大授权对象、改变操作人、把通用候选转换为个人候选或返回输入中不存在的未审计候选。实现应无状态或保证
 * 线程安全；集群环境中的画像和反馈一致性由业务后端保证。最终调用方仍必须执行 Core 授权过滤。</p>
 */
public interface AskDataRecommendationCandidateFilter {

	/**
	 * 按业务策略收窄候选集合。
	 *
	 * @param request 当前授权请求
	 * @param candidates 待过滤候选
	 * @return 过滤后的候选，不得为 {@code null}
	 */
	List<AskDataRecommendationCandidate> filter(AskDataRecommendationRequest request,
			List<AskDataRecommendationCandidate> candidates);
}
