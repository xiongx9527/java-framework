package com.xiongx9527.fw.plus.ai.askdata.chart;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.base.model.LlmResponse;
import com.xiongx9527.fw.plus.common.i18n.I18nMessageResolver;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 默认 {@link ChartGenerationCapability} 实现（§6.38 / §6.20）：决策类型 → 截断行数 → 构造配置 →
 * 校验（形态 + 字段存在性）→（可选）降级。缺模型时走 {@link ChartTypeGuesser} 启发式；
 * 提供模型时优先采用其决策（封堵风险登记 A.2.1：SSR 重新猜类型丢弃模型配置）。
 */
public class DefaultChartGenerationCapability implements ChartGenerationCapability {

    private static final Logger log = LoggerFactory.getLogger(DefaultChartGenerationCapability.class);

    /** G2 模式解析模型输出用（仅解析，不依赖 Spring 容器）。 */
    private static final ObjectMapper G2_MAPPER = new ObjectMapper();

    private final LlmPort llm;
    private final ChartConfigBuilder builder;
    private final ChartTypeGuesser guesser;
    private final ChartConfigValidator validator;
    private final ChartLlmParser parser;
    private final ChartDegradePolicy degradePolicy;
    private final ChartPromptBuilder promptBuilder;

    public DefaultChartGenerationCapability(LlmPort llm) {
        this(llm, new ChartConfigBuilder(), new ChartTypeGuesser(), new ChartConfigValidator(),
                new ChartLlmParser(), ChartDegradePolicy.builder().build(), new ChartPromptBuilder());
    }

    public DefaultChartGenerationCapability(LlmPort llm, ChartConfigBuilder builder,
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

        // G2 模式：要求模型直接产出 G2 风格配置，能力层跳过 ECharts 构造 / 校验，原样透传该 Map。
        if (input.getChartFormat() == ChartFormat.G2) {
            return generateG2(input, ds);
        }

        ChartSuggestion suggestion;
        boolean usedLlm = false;
        String source;
        String rawOutput = "";
        if (input.getPreferredType() != null) {
            suggestion = withAxesForType(input.getPreferredType(), ds);
            source = "preferred";
        } else if (llm != null) {
            try {
                LlmResponse response = llm.complete(promptBuilder.buildSystem(), promptBuilder.buildUser(input));
                String raw = response == null ? null : response.getContent();
                rawOutput = raw == null ? "" : raw;
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
                .rawOutput(rawOutput)
                .confidence(confidence)
                .degraded(degraded)
                .build();
    }

    /**
     * G2 模式生成：让模型直接产出 G2 配置 JSON（{@code type/title/columns/axis}），能力层仅做解析与最小兜底，
     * 不构造 ECharts 配置、不跑 ECharts 校验。模型不可用 / 产出非法 JSON 时退化为确定性 G2 表格配置。
     */
    private ChartGenResult generateG2(ChartGenInput input, ChartDataset ds) {
        Map<String, Object> config;
        ChartType type;
        boolean degraded;
        double confidence;
        String rawOutput = "";
        if (llm != null) {
            try {
                LlmResponse response = llm.complete(promptBuilder.buildSystem(), promptBuilder.buildUser(input));
                String raw = response == null ? null : response.getContent();
                rawOutput = raw == null ? "" : raw;
                Map<String, Object> parsed = parseG2(raw);
                if (parsed != null && parsed.get("type") != null) {
                    // A：模型常把 name/value 写反（name 填英文别名、value 填中文显示名），
                    // 按数据集真实列名校正，保证 value 始终是数据行的键。
                    alignG2Fields(parsed, ds);
                    config = parsed;
                    type = ChartType.fromHint(String.valueOf(parsed.get("type")));
                    degraded = false;
                    confidence = 0.85d;
                } else {
                    config = fallbackG2(ds);
                    type = ChartType.TABLE;
                    degraded = true;
                    confidence = 0.4d;
                }
            } catch (RuntimeException e) {
                config = fallbackG2(ds);
                type = ChartType.TABLE;
                degraded = true;
                confidence = 0.4d;
            }
        } else {
            // G2 模式依赖模型产出；无模型时退化为确定性 G2 表格配置，保证仍有可渲染结果。
            config = fallbackG2(ds);
            type = ChartType.TABLE;
            degraded = true;
            confidence = 0.4d;
        }
        // §6.37 全链路日志：只记元数据，不记行数据 / 模型原文。
        log.info("chart-generate[g2] type={} degraded={} rows={}", type, degraded, ds.rowCount());
        return ChartGenResult.builder()
                .chartType(type)
                .config(config)
                .rawOutput(rawOutput)
                .confidence(confidence)
                .degraded(degraded)
                .build();
    }

    /**
     * 校正模型产出的 G2 字段映射：前端用 {@code value} 作为数据行的键，故 value 必须是数据集真实列名。
     * 模型（尤其小模型/受 SQL 中 AS 中文别名影响）常把 name/value 写反（name=英文别名、value=中文显示名）；
     * 此处对每个 {name,value} 节点：若 value 不在真实列名而 name 在，则交换二者。
     * 不修改任何字段都在列名集合内的正确配置，也不猜测两边都不匹配的节点。
     */
    @SuppressWarnings("unchecked")
    private static void alignG2Fields(Map<String, Object> config, ChartDataset ds) {
        Set<String> actual = new HashSet<String>();
        if (ds.getColumns() != null) {
            for (ChartColumn c : ds.getColumns()) {
                actual.add(c.getName());
            }
        }
        if (actual.isEmpty()) {
            return;
        }
        // columns: [{name,value}]
        Object columns = config.get("columns");
        if (columns instanceof List) {
            for (Object item : (List<Object>) columns) {
                alignEntry(item, actual);
            }
        }
        // axis: {x:{name,value}, y:{...}|[...], series:{...}, multi-quota:{name,value:[...]}}
        Object axisObj = config.get("axis");
        if (axisObj instanceof Map) {
            Map<String, Object> axis = (Map<String, Object>) axisObj;
            for (String key : new String[]{"x", "y", "series"}) {
                Object node = axis.get(key);
                if (node instanceof List) {
                    for (Object item : (List<Object>) node) {
                        alignEntry(item, actual);
                    }
                } else {
                    alignEntry(node, actual);
                }
            }
        }
    }

    /** 单个 {name,value} 节点：value 非真实列名而 name 是时交换。 */
    @SuppressWarnings("unchecked")
    private static void alignEntry(Object node, Set<String> actualColumns) {
        if (!(node instanceof Map)) {
            return;
        }
        Map<String, Object> entry = (Map<String, Object>) node;
        Object name = entry.get("name");
        Object value = entry.get("value");
        if (name == null || value == null) {
            return;
        }
        String ns = String.valueOf(name);
        String vs = String.valueOf(value);
        if (!actualColumns.contains(vs) && actualColumns.contains(ns)) {
            entry.put("name", value);
            entry.put("value", name);
        }
    }

    /** 解析模型输出的 G2 配置 JSON（容忍 ```json 围栏），并剔除内联 data（数据由前端另行下发）。 */
    private static Map<String, Object> parseG2(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return null;
        }
        String json = stripFences(raw);
        if (json.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> map = G2_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {
            });
            map.remove("data");
            return map;
        } catch (Exception e) {
            log.info("chart-generate[g2] parse failed: {}", e.getMessage());
            return null;
        }
    }

    /** 去掉模型输出可能携带的 ```json ... ``` 围栏。 */
    private static String stripFences(String raw) {
        String s = raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl >= 0) {
                s = s.substring(nl + 1);
            }
            int last = s.lastIndexOf("```");
            if (last >= 0) {
                s = s.substring(0, last);
            }
        }
        return s.trim();
    }

    /** 确定性兜底：依据数据集列构造最小 G2 表格配置（columns + 空 axis）。 */
    private static Map<String, Object> fallbackG2(ChartDataset ds) {
        Map<String, Object> chart = new LinkedHashMap<>();
        chart.put("type", "table");
        chart.put("title", "");
        List<Map<String, Object>> columns = new ArrayList<>();
        if (ds.getColumns() != null) {
            for (ChartColumn c : ds.getColumns()) {
                Map<String, Object> col = new LinkedHashMap<>();
                col.put("name", c.getName());
                col.put("value", c.getName());
                columns.add(col);
            }
        }
        chart.put("columns", columns);
        chart.put("axis", new LinkedHashMap<String, Object>());
        return chart;
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
