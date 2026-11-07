package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.base.model.LlmPort;

/** {@link ChartGenerationCapability} 的流式构建器。llm 可选（缺省走启发式）。 */
public class ChartGenerationCapabilityBuilder {
    private LlmPort llm;
    private ChartConfigBuilder builder;
    private ChartTypeGuesser guesser;
    private ChartConfigValidator validator;
    private ChartLlmParser parser;
    private ChartDegradePolicy degradePolicy;
    private ChartPromptBuilder promptBuilder;
    private ChartFormat chartFormat;

    public static ChartGenerationCapabilityBuilder builder() {
        return new ChartGenerationCapabilityBuilder();
    }

    public ChartGenerationCapabilityBuilder llm(LlmPort v) {
        this.llm = v;
        return this;
    }

    public ChartGenerationCapabilityBuilder configBuilder(ChartConfigBuilder v) {
        this.builder = v;
        return this;
    }

    public ChartGenerationCapabilityBuilder guesser(ChartTypeGuesser v) {
        this.guesser = v;
        return this;
    }

    public ChartGenerationCapabilityBuilder validator(ChartConfigValidator v) {
        this.validator = v;
        return this;
    }

    public ChartGenerationCapabilityBuilder parser(ChartLlmParser v) {
        this.parser = v;
        return this;
    }

    public ChartGenerationCapabilityBuilder degradePolicy(ChartDegradePolicy v) {
        this.degradePolicy = v;
        return this;
    }

    public ChartGenerationCapabilityBuilder promptBuilder(ChartPromptBuilder v) {
        this.promptBuilder = v;
        return this;
    }

    /** 输出格式（默认 ECHARTS）；设为 {@link ChartFormat#G2} 时提示词切换为 G2 并要求模型直接产出 G2 配置。 */
    public ChartGenerationCapabilityBuilder chartFormat(ChartFormat v) {
        this.chartFormat = v;
        return this;
    }

    public ChartGenerationCapability build() {
        ChartPromptBuilder pb = promptBuilder == null ? new ChartPromptBuilder() : promptBuilder;
        if (chartFormat != null) {
            pb.chartFormat(chartFormat);
        }
        return new DefaultChartGenerationCapability(llm, builder, guesser, validator, parser,
                degradePolicy, pb);
    }
}
