package com.xiongx9527.fw.plus.ai.model.scoring.request;

import java.util.Objects;

/**
 * 相关性打分请求参数。
 *
 * <p><b>作用</b>：承载「模型名」等可覆盖参数，并支持与默认参数合并。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。镜像
 * {@code dev.langchain4j.model.scoring.request.ScoringRequestParameters}。</p>
 *
 * @author xiongxiao
 */
public interface ScoringRequestParameters {

    /** 空参数集（全部未设置）。 */
    ScoringRequestParameters EMPTY = DefaultScoringRequestParameters.builder().build();

    /**
     * 模型名。
     *
     * @return 模型名；未设置时为 {@code null}
     */
    String modelName();

    /**
     * 用另一份参数覆盖本参数。
     *
     * @param that 覆盖来源
     * @return 合并后的新参数对象
     */
    ScoringRequestParameters overrideWith(ScoringRequestParameters that);

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    static DefaultScoringRequestParameters.Builder<?> builder() {
        return DefaultScoringRequestParameters.builder();
    }
}
