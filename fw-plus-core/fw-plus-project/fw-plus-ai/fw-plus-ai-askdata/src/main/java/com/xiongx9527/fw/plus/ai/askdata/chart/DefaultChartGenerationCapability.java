package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 {@link ChartGenerationCapability} 实现（§6.38 / §6.20）：决策类型 → 截断行数 → 构造配置 →
 * 校验（形态 + 字段存在性）→（可选）降级。缺模型时走 {@link ChartTypeGuesser} 启发式；
 * 提供模型时优先采用其决策（封堵风险登记 A.2.1：SSR 重新猜类型丢弃模型配置）。
 */
public class DefaultChartGenerationCapability implements ChartGenerationCapability {

    private static final Logger log = LoggerFactory.getLogger(DefaultChartGenerationCapability.class);

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
            throw new IllegalArgumentException(I18nMessageResolver.resolveKey("askdata.contract.input_null"));
        }
        input.validate();
        ChartDataset ds = input.getData();
        // A.1.2：图表实际绘制不应吃下全量结果集，超出行上限先截断。
        if (ds.rowCount() > ChartDataset.MAX_CHART_ROWS) {
            ds = ds.limit(ChartDataset.MAX_CHART_ROWS);
            log.debug("chart-generate rows truncated to {}", ChartDataset.MAX_CHART_ROWS);
        }

        ChartSuggestion suggestion;
        boolean usedLlm = false;
        String source;
        if (input.getPreferredType() != null) {
            suggestion = withAxesForType(input.getPreferredType(), ds);
            source = "preferred";
        } else if (llm != null) {
            try {
                String raw = llm.complete(promptBuilder.buildSystem(), promptBuilder.buildUser(input));
                ChartSuggestion parsed = parser.parse(raw);
                if (parsed != null && parsed.getType() != ChartType.UNKNOWN) {
                    suggestion = parsed;
                    usedLlm = true;
                    source = "llm";
                } else {
                    suggestion = guesser.guess(ds);
                    source = "guesser";
                }
            } catch (RuntimeException e) {
                suggestion = guesser.guess(ds);
                source = "guesser";
            }
        } else {
            suggestion = guesser.guess(ds);
            source = "guesser";
        }

        Map<String, Object> config = builder.build(suggestion, ds, input.getQuestion());
        boolean degraded = false;
        // 形态校验 + 字段存在性校验（A.2.2）+ 数据集完整性（扩展 VERIFY：矩形 / 长度一致），任一不过即降级为表格。
        boolean valid = validator.isValid(config) && validator.isValid(suggestion, ds)
                && validator.isDatasetConsistent(ds);
        if (!valid) {
            if (degradePolicy.isEnabled()) {
                suggestion = new ChartSuggestion(ChartType.TABLE, ChartAxes.builder().build());
                config = builder.build(suggestion, ds, input.getQuestion());
                degraded = true;
            } else {
                degraded = true;
            }
        }

        double confidence = degraded ? 0.4d : (usedLlm ? 0.85d : 0.8d);
        // §6.37 全链路日志：只记元数据，不记行数据 / 模型原文，避免泄露业务数据。
        log.info("chart-generate type={} degraded={} rows={} source={}",
                suggestion.getType(), degraded, ds.rowCount(), source);
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
