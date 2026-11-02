package com.xiongx9527.fw.plus.ai.model.embedding.request;

import java.util.Set;

/**
 * 嵌入请求参数。
 *
 * <p><b>作用</b>：承载「模型名 / 维度 / 输入用途」等可覆盖参数，并支持与默认参数合并，
 * 让适配器按「默认值 ← 请求值」的优先级取值。</p>
 *
 * <p><b>出处</b>：A 类（langchain4j 1.23.0 直抄）。逐成员镜像
 * {@code dev.langchain4j.model.embedding.request.EmbeddingRequestParameters}。</p>
 *
 * <p><b>实现约定</b>：{@link #EMPTY} 表示「什么都没设置」；
 * {@link #parameter(EmbeddingParameter)} 对未设置的参数返回 {@code null}。</p>
 *
 * @author xiongxiao
 */
public interface EmbeddingRequestParameters {

    /** 模型名参数。 */
    EmbeddingParameter<String> MODEL_NAME = new EmbeddingParameter<String>("modelName", String.class);

    /** 输出维度参数。 */
    EmbeddingParameter<Integer> DIMENSIONS = new EmbeddingParameter<Integer>("dimensions", Integer.class);

    /** 输入用途参数。 */
    EmbeddingParameter<EmbeddingInputType> INPUT_TYPE =
            new EmbeddingParameter<EmbeddingInputType>("inputType", EmbeddingInputType.class);

    /** 空参数集（全部未设置）。 */
    EmbeddingRequestParameters EMPTY = DefaultEmbeddingRequestParameters.builder().build();

    /**
     * 模型名。
     *
     * @return 模型名；未设置时为 {@code null}
     */
    String modelName();

    /**
     * 输出维度。
     *
     * @return 维度；未设置时为 {@code null}
     */
    Integer dimensions();

    /**
     * 输入用途。
     *
     * @return 用途；未设置时为 {@code null}
     */
    EmbeddingInputType inputType();

    /**
     * 读取指定参数。
     *
     * @param parameter 参数声明
     * @param <T>       参数值类型
     * @return 参数值；未设置时为 {@code null}
     */
    <T> T parameter(EmbeddingParameter<T> parameter);

    /**
     * 已显式设置的参数集合。
     *
     * @return 参数集合
     */
    Set<EmbeddingParameter<?>> presentParameters();

    /**
     * 用另一份参数覆盖本参数（{@code other} 中已设置的项优先）。
     *
     * @param other 覆盖来源
     * @return 合并后的新参数对象
     */
    EmbeddingRequestParameters overrideWith(EmbeddingRequestParameters other);

    /**
     * 创建构建器。
     *
     * @return 构建器
     */
    static DefaultEmbeddingRequestParameters.Builder<?> builder() {
        return DefaultEmbeddingRequestParameters.builder();
    }
}
