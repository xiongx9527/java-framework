package com.xiongx9527.fw.plus.ai.askdata.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import com.xiongx9527.fw.plus.ai.base.model.LlmPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmPort;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class DefaultChartGenerationCapabilityTest {

    private ChartDataset ds() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("east", 10.0));
        rows.add(Arrays.asList("west", 20.0));
        rows.add(Arrays.asList("north", 5.0));
        return ChartDataset.builder()
                .columns(Arrays.asList(new ChartColumn("region", "VARCHAR"),
                        new ChartColumn("amount", "DOUBLE")))
                .rows(rows)
                .build();
    }

    @Test
    void heuristicPicksPieForFewCategories() {
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder().build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("各区域金额").data(ds()).build());
        assertEquals(ChartType.PIE, r.getChartType());
        assertTrue(r.getConfidence() > 0);
        assertFalse(r.isDegraded());
    }

    @Test
    void preferredTypeWins() {
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder().build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ds())
                .preferredType(ChartType.BAR).build());
        assertEquals(ChartType.BAR, r.getChartType());
    }

    @Test
    void llmDecisionUsedWhenProvided() {
        LlmPort port = new StaticLlmPort(
                "{\"chartType\":\"line\",\"categoryField\":\"region\",\"valueFields\":[\"amount\"]}");
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder().llm(port).build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("趋势").data(ds()).build());
        assertEquals(ChartType.LINE, r.getChartType());
    }

    @Test
    void degradesToTableWhenValidatorFails() {
        ChartConfigValidator failing = new ChartConfigValidator() {
            @Override
            public boolean isValid(Map<String, Object> config) {
                return false;
            }
        };
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder()
                .validator(failing)
                .build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ds()).build());
        assertEquals(ChartType.TABLE, r.getChartType());
        assertTrue(r.isDegraded());
    }

    @Test
    void degradesWhenLlmHallucinatesField() {
        LlmPort port = new StaticLlmPort(
                "{\"chartType\":\"bar\",\"categoryField\":\"ghost\",\"valueFields\":[\"amount\"]}");
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder().llm(port).build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ds()).build());
        assertEquals(ChartType.TABLE, r.getChartType());
        assertTrue(r.isDegraded());
    }

    @Test
    void degradesToTableOnRaggedDataset() {
        // 扩展 VERIFY：非矩形（残缺行）数据集会让 x 轴 / 系列长度不一致，必须降级为表格。
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("east", 10.0));
        rows.add(Arrays.asList("west"));
        ChartDataset ragged = ChartDataset.builder()
                .columns(Arrays.asList(new ChartColumn("region", "VARCHAR"),
                        new ChartColumn("amount", "DOUBLE")))
                .rows(rows)
                .build();
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder().build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ragged).build());
        assertEquals(ChartType.TABLE, r.getChartType());
        assertTrue(r.isDegraded());
    }

    @Test
    void missingDataThrows() {
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder().build();
        assertThrows(ChartGenerationException.class,
                () -> cap.generate(ChartGenInput.builder().question("q").build()));
    }

    @Test
    void g2ModeUsesLlmConfigDirectly() {
        String g2 = "{\"type\":\"column\",\"title\":\"各区域金额\",\"columns\":[{\"name\":\"region\",\"value\":\"region\"}],"
                + "\"axis\":{\"x\":{\"name\":\"region\",\"value\":\"region\"},\"y\":{\"name\":\"amount\",\"value\":\"amount\"}}}";
        LlmPort port = new StaticLlmPort(g2);
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder()
                .llm(port).chartFormat(ChartFormat.G2).build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("各区域金额").data(ds())
                .chartFormat(ChartFormat.G2).build());
        assertEquals(ChartType.BAR, r.getChartType());
        assertFalse(r.isDegraded());
        assertEquals("column", r.getConfig().get("type"));
        assertEquals(g2, r.getRawOutput());
    }

    @Test
    @SuppressWarnings("unchecked")
    void g2ModeAlignsSwappedNameValueAgainstRealColumns() {
        // 模型把 name/value 写反：name=英文别名(真实列名)，value=中文显示名(非真实列名)
        String g2 = "{\"type\":\"column\",\"title\":\"t\",\"columns\":[{\"name\":\"region\",\"value\":\"区域\"}],"
                + "\"axis\":{\"x\":{\"name\":\"region\",\"value\":\"区域\"},\"y\":{\"name\":\"amount\",\"value\":\"金额\"}}}";
        LlmPort port = new StaticLlmPort(g2);
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder()
                .llm(port).chartFormat(ChartFormat.G2).build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ds())
                .chartFormat(ChartFormat.G2).build());
        Map<String, Object> axis = (Map<String, Object>) r.getConfig().get("axis");
        Map<String, Object> x = (Map<String, Object>) axis.get("x");
        Map<String, Object> y = (Map<String, Object>) axis.get("y");
        // 校正后 value 必须是数据行真实键，name 是显示名
        assertEquals("region", x.get("value"));
        assertEquals("区域", x.get("name"));
        assertEquals("amount", y.get("value"));
        assertEquals("金额", y.get("name"));
        Map<String, Object> col = ((List<Map<String, Object>>) r.getConfig().get("columns")).get(0);
        assertEquals("region", col.get("value"));
    }

    @Test
    void g2ModeStripsCodeFence() {
        String g2 = "```json\n{\"type\":\"pie\",\"title\":\"t\",\"columns\":[],\"axis\":{}}\n```";
        LlmPort port = new StaticLlmPort(g2);
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder()
                .llm(port).chartFormat(ChartFormat.G2).build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ds())
                .chartFormat(ChartFormat.G2).build());
        assertEquals(ChartType.PIE, r.getChartType());
        assertEquals("pie", r.getConfig().get("type"));
    }

    @Test
    void g2ModeFallsBackOnInvalidJson() {
        LlmPort port = new StaticLlmPort("完全不是 JSON 的废话");
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder()
                .llm(port).chartFormat(ChartFormat.G2).build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ds())
                .chartFormat(ChartFormat.G2).build());
        assertEquals(ChartType.TABLE, r.getChartType());
        assertTrue(r.isDegraded());
        assertEquals("table", r.getConfig().get("type"));
    }

    @Test
    void g2ModeFallsBackWhenNoLlm() {
        ChartGenerationCapability cap = ChartGenerationCapabilityBuilder.builder()
                .chartFormat(ChartFormat.G2).build();
        ChartGenResult r = cap.generate(ChartGenInput.builder().question("q").data(ds())
                .chartFormat(ChartFormat.G2).build());
        assertEquals(ChartType.TABLE, r.getChartType());
        assertTrue(r.isDegraded());
        assertEquals("table", r.getConfig().get("type"));
    }
}
