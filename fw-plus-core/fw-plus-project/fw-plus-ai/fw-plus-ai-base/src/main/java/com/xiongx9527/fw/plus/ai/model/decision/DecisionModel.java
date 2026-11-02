package com.xiongx9527.fw.plus.ai.model.decision;

import com.xiongx9527.fw.plus.ai.exception.UnsupportedFeatureException;
import com.xiongx9527.fw.plus.ai.model.ModelProvider;
import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelListener;
import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelErrorContext;
import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelRequestContext;
import com.xiongx9527.fw.plus.ai.model.decision.listener.DecisionModelResponseContext;
import com.xiongx9527.fw.plus.ai.model.decision.request.DecisionRequest;
import com.xiongx9527.fw.plus.ai.model.decision.request.DecisionRequestParameters;
import com.xiongx9527.fw.plus.ai.model.decision.response.DecisionResponse;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * 决策模型端口。
 *
 * <p><b>作用</b>：把「基于输入做多类型判定（是否 / 选择 / 量表）」统一成一种能力，
 * 上层只依赖本接口，换决策服务（风控模型 / 分类器 / 大模型提示词）不改调用方代码。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐方法镜像
 * {@code dev.langchain4j.model.decision.DecisionModel}。</p>
 *
 * <p><b>实现约定</b>：实现者只需覆写 {@link #doDecide(DecisionRequest)}；
 * {@link #decide(DecisionRequest)} 已负责监听器派发、异常回调与响应校验。</p>
 *
 * @author xiongxiao
 */
public interface DecisionModel {

    /**
     * 执行决策请求（含监听器派发与响应校验）。
     *
     * @param request 决策请求
     * @return 决策响应
     */
    default DecisionResponse decide(DecisionRequest request) {
        List<DecisionModelListener> listeners = listeners();
        DecisionModelRequestContext requestContext =
                new DecisionModelRequestContext(request, provider(), Collections.<Object, Object>emptyMap());
        DecisionModelListenerUtils.onRequest(requestContext, listeners);
        try {
            DecisionResponse response = DecisionResponseValidator.validate(request, doDecide(request));
            DecisionModelListenerUtils.onResponse(new DecisionModelResponseContext(
                    response, request, provider(), Collections.<Object, Object>emptyMap()), listeners);
            return response;
        } catch (RuntimeException e) {
            DecisionModelListenerUtils.onError(new DecisionModelErrorContext(
                    e, request, provider(), Collections.<Object, Object>emptyMap()), listeners);
            throw e;
        }
    }

    /**
     * 真正执行决策（由实现覆写）。
     *
     * @param request 决策请求
     * @return 决策响应
     */
    default DecisionResponse doDecide(DecisionRequest request) {
        throw new UnsupportedFeatureException("Not supported yet.");
    }

    /**
     * 异步执行决策请求。
     *
     * @param request 决策请求
     * @return 异步响应
     */
    default CompletableFuture<DecisionResponse> decideAsync(DecisionRequest request) {
        try {
            return CompletableFuture.completedFuture(decide(request));
        } catch (RuntimeException e) {
            CompletableFuture<DecisionResponse> failed = new CompletableFuture<DecisionResponse>();
            failed.completeExceptionally(e);
            return failed;
        }
    }

    /**
     * 真正异步决策（由实现覆写）。
     *
     * @param request 决策请求
     * @return 异步响应
     */
    default CompletableFuture<DecisionResponse> doDecideAsync(DecisionRequest request) {
        CompletableFuture<DecisionResponse> failed = new CompletableFuture<DecisionResponse>();
        failed.completeExceptionally(new UnsupportedFeatureException(
                getClass().getName() + " does not support doDecideAsync() yet."));
        return failed;
    }

    /**
     * 默认请求参数。
     *
     * @return 默认参数（{@link DecisionRequestParameters#EMPTY}）
     */
    default DecisionRequestParameters defaultRequestParameters() {
        return DecisionRequestParameters.EMPTY;
    }

    /**
     * 已注册的监听器。
     *
     * @return 不可变监听器列表，默认空
     */
    default List<DecisionModelListener> listeners() {
        return Collections.emptyList();
    }

    /**
     * 厂商标识。
     *
     * @return 厂商标识；未声明时为 {@code null}
     */
    default ModelProvider provider() {
        return null;
    }

    /**
     * 模型名。
     *
     * @return 模型名；未知时为 {@code null}
     */
    default String modelName() {
        return null;
    }
}
