package com.xiongx9527.fw.plus.ai.askdata.memory;
import java.util.List;
/** 推荐候选过滤 SPI。 */
public interface AskDataRecommendationCandidateFilter { List<AskDataRecommendationCandidate> filter(AskDataRecommendationRequest request,List<AskDataRecommendationCandidate> candidates); }
