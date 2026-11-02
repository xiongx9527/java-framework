package com.xiongx9527.fw.plus.ai.askdata.chart;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ChartTypeGuesserTest {

    private ChartDataset dataset(String catType, Object[] catVals, String numType, Object[] numVals) {
        int rows = Math.min(catVals.length, numVals.length);
        List<List<Object>> data = new ArrayList<>();
        for (int i = 0; i < rows; i++) {
            data.add(Arrays.asList(catVals[i], numVals[i]));
        }
        return ChartDataset.builder()
                .columns(Arrays.asList(new ChartColumn("c", catType), new ChartColumn("v", numType)))
                .rows(data)
                .build();
    }

    @Test
    void pieWhenFewCategories() {
        ChartDataset ds = dataset("VARCHAR", new Object[]{"a", "b", "c"},
                "DOUBLE", new Object[]{1.0, 2.0, 3.0});
        assertEquals(ChartType.PIE, new ChartTypeGuesser().guess(ds).getType());
    }

    @Test
    void barWhenManyCategories() {
        ChartDataset ds = dataset("VARCHAR",
                new Object[]{"a", "b", "c", "d", "e", "f", "g", "h", "i", "j"},
                "DOUBLE", new Object[]{1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0, 8.0, 9.0, 10.0});
        assertEquals(ChartType.BAR, new ChartTypeGuesser().guess(ds).getType());
    }

    @Test
    void scatterWhenTwoNumeric() {
        ChartDataset ds = ChartDataset.builder()
                .columns(Arrays.asList(new ChartColumn("x", "DOUBLE"), new ChartColumn("y", "DOUBLE")))
                .rows(Arrays.asList(Arrays.asList(1.0, 2.0), Arrays.asList(3.0, 4.0)))
                .build();
        assertEquals(ChartType.SCATTER, new ChartTypeGuesser().guess(ds).getType());
    }

    @Test
    void tableWhenNoNumeric() {
        ChartDataset ds = dataset("VARCHAR", new Object[]{"a", "b"},
                "VARCHAR", new Object[]{"x", "y"});
        assertEquals(ChartType.TABLE, new ChartTypeGuesser().guess(ds).getType());
    }
}
