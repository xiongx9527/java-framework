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

    public ChartGenerationCapability build() {
        return new DefaultChartGenerationCapability(llm, builder, guesser, validator, parser,
                degradePolicy, promptBuilder);
    }
}
