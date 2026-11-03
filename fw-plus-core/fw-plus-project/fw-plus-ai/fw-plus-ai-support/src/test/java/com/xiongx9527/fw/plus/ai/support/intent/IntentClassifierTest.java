package com.xiongx9527.fw.plus.ai.support.intent;

import com.xiongx9527.fw.plus.ai.base.intent.Intent;
import com.xiongx9527.fw.plus.ai.base.intent.IntentPort;
import com.xiongx9527.fw.plus.ai.base.intent.IntentRequest;
import com.xiongx9527.fw.plus.ai.base.intent.IntentResult;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class IntentClassifierTest {

    private static IntentRequest req(String text) {
        return IntentRequest.builder()
                .text(text)
                .candidates(Arrays.asList(
                        Intent.of("sql", "查询 销售 数据 SQL 报表"),
                        Intent.of("chart", "图表 可视化 渲染 柱状图"),
                        Intent.of("export", "导出 Excel 文件")))
                .build();
    }

    @Test
    public void regexPicksKeywordIntent() {
        IntentResult r = new RegexIntentClassifier().classify(req("帮我查一下销售数据"));
        assertNotNull(r.getTop());
        assertEquals("sql", r.getTop().getRef());
        assertTrue(r.getTop().getScore() > 0.0);
    }

    @Test
    public void hybridCombinesScores() {
        IntentPort regex = new RegexIntentClassifier();
        // 第二个分类器用空结果不影响（这里直接复用 regex 仅做合并演示）
        IntentPort hybrid = new HybridIntentClassifier(Arrays.asList(regex, regex),
                Arrays.asList(0.7, 0.3));
        IntentResult r = hybrid.classify(req("导出一份 Excel 文件"));
        assertEquals("export", r.getTop().getRef());
    }
}
