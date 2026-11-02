package com.xiongx9527.fw.plus.ai.askdata.chart;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ChartConfigValidatorTest {

    private ChartDataset ds() {
        return ChartDataset.builder()
                .columns(Arrays.asList(new ChartColumn("region", "VARCHAR"),
                        new ChartColumn("amount", "DOUBLE")))
                .rows(Collections.<List<Object>>emptyList())
                .build();
    }

    private ChartSuggestion bar(String cat, List<String> vals) {
        return new ChartSuggestion(ChartType.BAR,
                ChartAxes.builder().categoryField(cat).valueFields(vals).build());
    }

    @Test
    void validWhenFieldsExist() {
        ChartConfigValidator v = new ChartConfigValidator();
        assertTrue(v.isValid(bar("region", Arrays.asList("amount")), ds()));
    }

    @Test
    void invalidWhenCategoryFieldMissing() {
        ChartConfigValidator v = new ChartConfigValidator();
        assertFalse(v.isValid(bar("ghost", Arrays.asList("amount")), ds()));
    }

    @Test
    void invalidWhenValueFieldMissing() {
        ChartConfigValidator v = new ChartConfigValidator();
        assertFalse(v.isValid(bar("region", Arrays.asList("ghost")), ds()));
    }

    @Test
    void tableAndUnknownAlwaysValid() {
        ChartConfigValidator v = new ChartConfigValidator();
        assertTrue(v.isValid(new ChartSuggestion(ChartType.TABLE, ChartAxes.builder().build()), ds()));
        assertTrue(v.isValid(new ChartSuggestion(ChartType.UNKNOWN, null), ds()));
    }
}
