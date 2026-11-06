package com.xiongx9527.fw.plus.ai.askdata.chart;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartColumn;
import com.xiongx9527.fw.plus.ai.askdata.chart.data.ChartDataset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ChartPromptBuilderTest {

    private ChartDataset ds() {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(Arrays.asList("east", 10.0));
        return ChartDataset.builder()
                .columns(Arrays.asList(new ChartColumn("region", "VARCHAR"),
                        new ChartColumn("amount", "DOUBLE")))
                .rows(rows)
                .build();
    }

    @Test
    void includesSqlAndMSchemaWhenProvided() {
        ChartPromptBuilder builder = new ChartPromptBuilder();
        String sql = "SELECT region, amount FROM t";
        String mSchema = "table t(region VARCHAR, amount DOUBLE)";
        ChartGenInput input = ChartGenInput.builder()
                .question("趋势")
                .data(ds())
                .sql(sql)
                .mSchema(mSchema)
                .build();
        String user = builder.buildUser(input);
        assertTrue(user.contains(sql), "prompt should contain provided sql");
        assertTrue(user.contains(mSchema), "prompt should contain provided m-schema");
    }

    @Test
    void omitsSqlAndMSchemaWhenAbsent() {
        ChartPromptBuilder builder = new ChartPromptBuilder();
        ChartGenInput input = ChartGenInput.builder().question("q").data(ds()).build();
        String user = builder.buildUser(input);
        assertTrue(!user.contains("SELECT region, amount FROM t"),
                "sql value should be absent when not provided");
    }
}
