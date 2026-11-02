package com.xiongx9527.fw.plus.ai.model.decision;

import com.xiongx9527.fw.plus.ai.model.decision.request.DecisionRequest;
import com.xiongx9527.fw.plus.ai.model.decision.response.DecisionResponse;
import com.xiongx9527.fw.plus.ai.model.decision.response.DecisionResponseMetadata;

import java.util.Collections;

/**
 * 决策响应校验工具（包内可见）。
 *
 * <p><b>作用</b>：检查响应是否覆盖了请求中的每一个问题、是否出现未知问题，
 * 把「模型漏答 / 多答」这类隐性错误变成明确的异常，而不是静默返回残缺结果。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.decision.DecisionResponseValidator}。</p>
 *
 * @author xiongxiao
 */
final class DecisionResponseValidator {

    private DecisionResponseValidator() {
    }

    /**
     * 校验响应与请求一致。
     *
     * @param request  决策请求
     * @param response 决策响应
     * @return 通过校验的响应
     * @throws IllegalArgumentException 问题集合不一致时抛出
     */
    static DecisionResponse validate(DecisionRequest request, DecisionResponse response) {
        java.util.Set<String> requested = request.questions().keySet();
        java.util.Set<String> answered = response.answers().keySet();
        if (!requested.equals(answered)) {
            throw new IllegalArgumentException("Decision response does not match request: requested="
                    + requested + ", answered=" + answered);
        }
        return response;
    }

    /**
     * 构建空的响应元数据（工具类便捷方法）。
     *
     * @return 空元数据
     */
    static DecisionResponseMetadata emptyMetadata() {
        return DecisionResponseMetadata.builder().build();
    }
}
