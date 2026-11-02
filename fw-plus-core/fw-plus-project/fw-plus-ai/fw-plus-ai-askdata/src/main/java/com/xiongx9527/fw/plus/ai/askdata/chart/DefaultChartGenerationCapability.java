package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 默认 {@link ChartGenerationCapability} 实现（§6.38 / §6.20）：决策类型 → 构造配置 → 校验 →（可选）降级。
 * 缺模型时走 {@link ChartTypeGuesser} 启发式；提供模型时优先采用其决策。
 */
public class DefaultChartGenerationCapability implements ChartGenerationCapability {

    private final LlmChatPort llm;
    private final ChartConfigBuilder builder;
    private final ChartTypeGuesser guesser;
    private final ChartConfigValidator validator;
    private final ChartLlmParser parser;
    private final ChartDegradePolicy degradePolicy;
    private final ChartPromptBuilder promptBuilder;

    public DefaultChartGenerationCapability(LlmChatPort llm) {
        this(llm, new ChartConfigBuilder(), new ChartTypeGuesser(), new ChartConfigValidator(),
                new ChartLlmParser(), ChartDegradePolicy.builder().build(), new ChartPromptBuilder());
    }

    public DefaultChartGenerationCapability(LlmChatPort llm, ChartConfigBuilder builder,
            ChartTypeGuesser guesser, ChartConfigValidator validator, ChartLlmParser parser,
            ChartDegradePolicy degradePolicy, ChartPromptBuilder promptBuilder) {
        this.llm = llm;
        this.builder = builder == null ? new ChartConfigBuilder() : builder;
        this.guesser = guesser == null ? new ChartTypeGuesser() : guesser;
        this.validator = validator == null ? new ChartConfigValidator() : validator;
        this.parser = parser == null ? new ChartLlmParser() : parser;
        this.degradePolicy = degradePolicy == null ? ChartDegradePolicy.builder().build() : degradePolicy;
        this.promptBuilder = promptBuilder == null ? new ChartPromptBuilder() : promptBuilder;
    }

    @Override
    public ChartGenResult generate(ChartGenInput input) {
        if (input == null) {
            throw new ChartGenerationException("input is null");
        }
        input.validate();
        ChartDataset ds = input.getData();

        ChartSuggestion suggestion;
        boolean usedLlm = false;
        if (input.getPreferredType() != null) {
            suggestion = withAxesForType(input.getPreferredType(), ds);
        } else if (llm != null) {
            try {
                String raw = llm.complete(promptBuilder.buildSystem(), promptBuilder.buildUser(input));
                ChartSuggestion parsed = parser.parse(raw);
                if (parsed != null && parsed.getType() != ChartType.UNKNOWN) {
                    suggestion = parsed;
                    usedLlm = true;
                } else {
                    suggestion = guesser.guess(ds);
                }
            } catch (RuntimeException e) {
                suggestion = guesser.guess(ds);
            }
        } else {
            suggestion = guesser.guess(ds);
        }

        Map<String, Object> config = builder.build(suggestion, ds, input.getQuestion());
        boolean degraded = false;
        if (!validator.isValid(config)) {
            if (degradePolicy.isEnabled()) {
                suggestion = new ChartSuggestion(ChartType.TABLE, ChartAxes.builder().build());
                config = builder.build(suggestion, ds, input.getQuestion());
                degraded = true;
            } else {
                degraded = true;
            }
        }

        double confidence = degraded ? 0.4d : (usedLlm ? 0.85d : 0.8d);
        return ChartGenResult.builder()
                .chartType(suggestion.getType())
                .config(config)
                .confidence(confidence)
                .degraded(degraded)
                .build();
    }

    private ChartSuggestion withAxesForType(ChartType type, ChartDataset ds) {
        if (type == ChartType.TABLE) {
            return new ChartSuggestion(type, ChartAxes.builder().build());
        }
        List<String> numeric = ds.numericFieldNames();
        List<String> cat = ds.categoricalFieldNames();
        String catField = cat.isEmpty()
                ? (ds.columnCount() > 0 ? ds.getColumns().get(0).getName() : null)
                : cat.get(0);
        List<String> values = numeric.isEmpty() ? new ArrayList<String>() : numeric;
        return new ChartSuggestion(type, ChartAxes.builder().categoryField(catField).valueFields(values).build());
    }
}
