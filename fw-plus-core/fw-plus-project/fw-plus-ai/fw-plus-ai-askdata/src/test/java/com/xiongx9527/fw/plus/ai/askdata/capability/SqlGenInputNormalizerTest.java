package com.xiongx9527.fw.plus.ai.askdata.capability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.xiongx9527.fw.plus.ai.askdata.schema.SchemaContext;
import com.xiongx9527.fw.plus.ai.askdata.schema.TableSchema;
import java.util.Collections;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

public class SqlGenInputNormalizerTest {

    private SqlGenInput base(String question) {
        SchemaContext schema = SchemaContext.builder()
                .tables(Collections.singletonList(TableSchema.builder().name("orders").build()))
                .build();
        return SqlGenInput.builder().question(question).schemaContext(schema)
                .dialectHint("mysql").build();
    }

    @Test
    void foldsInternalWhitespaceAndTrims() {
        SqlGenInput out = SqlGenInputNormalizer.normalize(base("  统计   订单\n\t数  "));
        assertEquals("统计 订单 数", out.getQuestion());
    }

    @Test
    void preprocessorAppliedBeforeFold() {
        Function<String, String> pre = q -> q.replace("今天", "2026-11-03");
        SqlGenInput out = SqlGenInputNormalizer.normalize(base("  今天 的 订单 "),
                SqlGenInputNormalizer.DEFAULT_MAX_LENGTH, pre);
        assertEquals("2026-11-03 的 订单", out.getQuestion());
    }

    @Test
    void lengthCapThrowsWhenExceeded() {
        String longQ = repeat("a", 50);
        assertThrows(IllegalArgumentException.class,
                () -> SqlGenInputNormalizer.normalize(base(longQ), 10, null));
    }

    @Test
    void zeroMaxMeansNoLimit() {
        String longQ = repeat("a", 50);
        SqlGenInput out = SqlGenInputNormalizer.normalize(base(longQ), 0, null);
        assertEquals(50, out.getQuestion().length());
    }

    @Test
    void returnsInputWhenQuestionNull() {
        SqlGenInput in = base(null);
        assertSame(in, SqlGenInputNormalizer.normalize(in));
    }

    @Test
    void copiesOtherFields() {
        SqlGenInput in = base("  q  ");
        SqlGenInput out = SqlGenInputNormalizer.normalize(in);
        assertEquals("mysql", out.getDialectHint());
        assertTrue(out.getSchemaContext().containsTable("orders"));
        assertEquals("q", out.getQuestion());
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) {
            sb.append(s);
        }
        return sb.toString();
    }
}
