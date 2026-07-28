package com.xiongx9527.fw.plus.ai.autoconfigure.askdata.memory;

import com.xiongx9527.fw.plus.ai.askdata.context.AskDataProjectContext;
import com.xiongx9527.fw.plus.ai.askdata.memory.AskDataRecommendationService;

import java.util.List;

/**
 * Core 推荐 SPI 的自动配置层桥接。
 *
 * <p>个人推荐的读写一律使用同一个 {@code projectId + operatorId} 上下文，默认桥接不启用
 * 项目级通用推荐回退，防止个人偏好跨操作人泄漏。</p>
 */
public class AskDataRecommendationAdapter {

    private final AskDataRecommendationService recommendationService;

    public AskDataRecommendationAdapter(AskDataRecommendationService recommendationService) {
        if (recommendationService == null) {
            throw new IllegalArgumentException("recommendationService 不能为空");
        }
        this.recommendationService = recommendationService;
    }

    /**
     * 按 {@code projectId + operatorId} 隔离写入个人推荐，禁止跨项目或跨操作人共享。
     */
    public void record(AskDataProjectContext context, String recommendation) {
        recommendationService.record(context, recommendation);
    }

    /**
     * 仅按 {@code projectId + operatorId} 隔离读取当前操作人的个人推荐。
     */
    public List<String> recommend(AskDataProjectContext context) {
        return recommendationService.recommend(context);
    }
}
