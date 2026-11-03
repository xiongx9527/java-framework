package com.xiongx9527.fw.plus.ai.support.retrieval;

import com.xiongx9527.fw.plus.ai.base.store.Document;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * 加权关键字打分：按「字段权重」判定命中程度。
 *
 * <p>命中规则（每个关键词取所有字段里的最高分）：</p>
 * <ul>
 *   <li>字段值完全等于关键词 → 字段权重（如表名 1.0）</li>
 *   <li>字段值包含关键词 → 字段权重 × {@code substringFactor}（默认 0.7）</li>
 * </ul>
 *
 * <p>最终分 = 各关键词最高分的平均值，落在 0~1。字段与权重由调用方配置，
 * 因此同一套打分能用于「表名 &gt; 注释 &gt; 字段名」这类领域口径。</p>
 */
public class WeightedKeywordScorer implements KeywordScorer {

    /** 子串命中相对完全命中的折扣。 */
    public static final double DEFAULT_SUBSTRING_FACTOR = 0.7;

    private final Map<String, Field> fields = new LinkedHashMap<String, Field>();

    private final double substringFactor;

    public WeightedKeywordScorer(double substringFactor) {
        this.substringFactor = substringFactor <= 0 || substringFactor > 1
                ? DEFAULT_SUBSTRING_FACTOR : substringFactor;
    }

    public WeightedKeywordScorer() {
        this(DEFAULT_SUBSTRING_FACTOR);
    }

    /** 增加一个参与匹配的字段。 */
    public WeightedKeywordScorer field(String name, double weight, Function<Document, String> valueOf) {
        if (name != null && valueOf != null) {
            fields.put(name, new Field(weight, valueOf));
        }
        return this;
    }

    @Override
    public double score(List<String> keywords, Document document) {
        if (keywords == null || keywords.isEmpty() || document == null || fields.isEmpty()) {
            return 0.0;
        }
        List<String> normalized = new ArrayList<String>();
        for (String keyword : keywords) {
            if (keyword != null && !keyword.trim().isEmpty()) {
                normalized.add(keyword.trim().toLowerCase(Locale.ROOT));
            }
        }
        if (normalized.isEmpty()) {
            return 0.0;
        }
        double total = 0.0;
        for (String keyword : normalized) {
            total += bestScore(keyword, document);
        }
        return Math.min(1.0, total / normalized.size());
    }

    private double bestScore(String keyword, Document document) {
        double best = 0.0;
        for (Field field : fields.values()) {
            String value = field.valueOf.apply(document);
            if (value == null || value.isEmpty()) {
                continue;
            }
            String lower = value.toLowerCase(Locale.ROOT);
            if (lower.equals(keyword)) {
                best = Math.max(best, field.weight);
            } else if (lower.contains(keyword)) {
                best = Math.max(best, field.weight * substringFactor);
            }
        }
        return best;
    }

    private static final class Field {

        private final double weight;

        private final Function<Document, String> valueOf;

        private Field(double weight, Function<Document, String> valueOf) {
            this.weight = weight;
            this.valueOf = valueOf;
        }
    }
}
