package com.xiongx9527.fw.plus.ai.askdata.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import com.xiongx9527.fw.plus.ai.askdata.llm.LlmChatPort;
import com.xiongx9527.fw.plus.ai.askdata.llm.StaticLlmChatPort;
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
        LlmChatPort port = new StaticLlmChatPort(
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
        LlmChatPort port = new StaticLlmChatPort(
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
}
