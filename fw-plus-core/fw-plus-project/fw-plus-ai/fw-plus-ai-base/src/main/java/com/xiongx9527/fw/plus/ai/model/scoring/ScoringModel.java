package com.xiongx9527.fw.plus.ai.model.scoring;

import com.xiongx9527.fw.plus.ai.data.segment.TextSegment;
import com.xiongx9527.fw.plus.ai.exception.UnsupportedFeatureException;
import com.xiongx9527.fw.plus.ai.model.output.Response;
import com.xiongx9527.fw.plus.ai.model.scoring.request.ScoringRequest;
import com.xiongx9527.fw.plus.ai.model.scoring.request.ScoringRequestParameters;
import com.xiongx9527.fw.plus.ai.model.scoring.response.ScoringResponse;
import com.xiongx9527.fw.plus.ai.model.scoring.response.ScoringResponseMetadata;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 相关性打分模型端口。
 *
 * <p><b>作用</b>：给定查询与候选片段，给出相关性分数，用于 RAG 重排与候选过滤。
 * 上层只依赖本接口，换打分服务不改调用方代码。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐方法镜像
 * {@code dev.langchain4j.model.scoring.ScoringModel}。</p>
 *
 * <p><b>实现约定</b>：实现者只需覆写 {@link #scoreAll(List, String)}；
 * 异步入口会在不支持时返回失败的 Future。</p>
 *
 * @author xiongxiao
 */
public interface ScoringModel {

    /**
     * 给单段文本打分。
     *
     * @param text  文本
     * @param query 查询
     * @return 相关性分数响应
     */
    default Response<Double> score(String text, String query) {
        return score(TextSegment.from(text), query);
    }

    /**
     * 给单个片段打分。
     *
     * @param segment 文本片段
     * @param query   查询
     * @return 相关性分数响应
     */
    default Response<Double> score(TextSegment segment, String query) {
        Response<List<Double>> all = scoreAll(Collections.singletonList(segment), query);
        List<Double> scores = all == null ? null : all.content();
        Double first = (scores == null || scores.isEmpty()) ? null : scores.get(0);
        return new Response<Double>(first, all == null ? null : all.tokenUsage(), all == null ? null : all.finishReason());
    }

    /**
     * 给多个片段批量打分。
     *
     * @param segments 文本片段列表
     * @param query    查询
     * @return 分数列表响应，顺序与入参一致
     */
    Response<List<Double>> scoreAll(List<TextSegment> segments, String query);

    /**
     * 按请求异步打分。
     *
     * @param request 打分请求
     * @return 异步响应
     */
    default CompletableFuture<ScoringResponse> scoreAsync(ScoringRequest request) {
        try {
            return CompletableFuture.completedFuture(doScoreAsync(request).join());
        } catch (RuntimeException e) {
            CompletableFuture<ScoringResponse> failed = new CompletableFuture<ScoringResponse>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /**
     * 真正异步打分（由实现覆写）。
     *
     * @param request 打分请求
     * @return 异步响应
     */
    default CompletableFuture<ScoringResponse> doScoreAsync(ScoringRequest request) {
        CompletableFuture<ScoringResponse> failed = new CompletableFuture<ScoringResponse>();
        failed.completeExceptionally(new UnsupportedFeatureException(
                getClass().getName() + " does not support doScoreAsync() yet."));
        return failed;
    }

    /**
     * 默认请求参数。
     *
     * @return 默认参数（{@link ScoringRequestParameters#EMPTY}）
     */
    default ScoringRequestParameters defaultRequestParameters() {
        return ScoringRequestParameters.EMPTY;
    }

    /**
     * 便捷方法：把旧式 {@link Response} 转换为新式 {@link ScoringResponse} 元数据。
     *
     * @param response 旧式响应
     * @return 新元数据对象
     */
    static ScoringResponseMetadata toMetadata(Response<?> response) {
        return ScoringResponseMetadata.builder()
                .tokenUsage(response == null ? null : response.tokenUsage())
                .build();
    }
}
